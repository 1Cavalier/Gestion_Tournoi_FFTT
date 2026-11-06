package com.pingmanager.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Sauvegarde automatique périodique d'une session, pour la reprise après un
 * arrêt non prévu (coupure de courant, plantage, batterie).
 *
 * <p>Fonctionnement. Une fois démarré, le service écrit l'état courant de la
 * session dans un <b>fichier d'autosave dédié</b>, distinct du document que
 * l'organisateur enregistre volontairement, à intervalle régulier (15 minutes
 * par défaut : suffisamment rare pour rester discret, suffisamment fréquent
 * pour limiter la perte en cas d'incident). Un <b>marqueur de fermeture propre</b>
 * placé à côté indique qu'une session s'est terminée normalement.</p>
 *
 * <p>Au lancement suivant, {@link #detecterReprise(Path)} regarde s'il existe un
 * autosave non accompagné de son marqueur de fermeture propre : c'est le signe
 * qu'une session ne s'est pas terminée normalement, et qu'on peut proposer de
 * reprendre là où elle en était.</p>
 *
 * <p>Cette classe vit dans la couche de persistance et ne connaît rien du
 * fonctionnement métier : elle reçoit un fournisseur d'état de session et se
 * contente de l'écrire.</p>
 */
public final class ServiceAutosave implements AutoCloseable {

    /** Fréquence par défaut de l'autosave : stable mais sécurisé. */
    public static final Duration FREQUENCE_PAR_DEFAUT = Duration.ofMinutes(15);

    /** Suffixe du fichier d'autosave, dérivé du fichier de document. */
    public static final String SUFFIXE_AUTOSAVE = ".autosave.json";

    /** Suffixe du marqueur de fermeture propre. */
    private static final String SUFFIXE_MARQUEUR = ".cloture";

    private final Path fichierAutosave;
    private final Path marqueurCloture;
    private final Supplier<SessionSalle> fournisseurEtat;
    private final Duration frequence;

    private ScheduledExecutorService planificateur;
    private ScheduledFuture<?> tache;
    private volatile boolean actif;

    /**
     * @param fichierAutosave chemin du fichier d'autosave (généralement le
     *                        document suffixé par {@link #SUFFIXE_AUTOSAVE})
     * @param fournisseurEtat fournit l'instantané de session à écrire à chaque tick
     * @param frequence       intervalle entre deux sauvegardes automatiques
     */
    public ServiceAutosave(Path fichierAutosave, Supplier<SessionSalle> fournisseurEtat,
                           Duration frequence) {
        this.fichierAutosave = Objects.requireNonNull(fichierAutosave, "fichierAutosave");
        this.fournisseurEtat = Objects.requireNonNull(fournisseurEtat, "fournisseurEtat");
        this.frequence = Objects.requireNonNull(frequence, "frequence");
        this.marqueurCloture = fichierAutosave.resolveSibling(
                fichierAutosave.getFileName() + SUFFIXE_MARQUEUR);
    }

    /** Construit le chemin d'autosave associé à un document de tournoi. */
    public static Path autosavePour(Path document) {
        return document.resolveSibling(document.getFileName() + SUFFIXE_AUTOSAVE);
    }

    /**
     * Démarre l'autosave périodique. Efface un éventuel marqueur de fermeture
     * propre laissé par une session précédente (on rouvre une session active) et
     * écrit immédiatement un premier instantané.
     */
    public synchronized void demarrer() {
        if (actif) {
            return;
        }
        actif = true;
        supprimerMarqueur();
        sauvegarderMaintenant(); // premier point de reprise sans attendre le tick
        planificateur = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pingmanager-autosave");
            t.setDaemon(true);
            return t;
        });
        long secondes = Math.max(1, frequence.toSeconds());
        tache = planificateur.scheduleAtFixedRate(
                this::sauvegarderSansEchec, secondes, secondes, TimeUnit.SECONDS);
    }

    /**
     * Écrit un instantané immédiatement (par exemple à une transition importante),
     * en plus des sauvegardes périodiques.
     */
    public void sauvegarderMaintenant() {
        SessionSalle etat = fournisseurEtat.get();
        if (etat != null) {
            DepotSession.sauvegarder(etat, fichierAutosave);
        }
    }

    /** @return le fournisseur d'état de session confié à ce service. */
    public Supplier<SessionSalle> fournisseur() {
        return fournisseurEtat;
    }

    private void sauvegarderSansEchec() {
        try {
            sauvegarderMaintenant();
        } catch (RuntimeException e) {
            // L'autosave ne doit jamais faire tomber l'application : on avale
            // l'erreur (un tick raté sera rattrapé au suivant).
        }
    }

    /**
     * Arrête proprement l'autosave et pose le marqueur de fermeture propre :
     * la prochaine ouverture ne proposera pas de reprise pour cette session.
     */
    @Override
    public synchronized void close() {
        if (!actif) {
            return;
        }
        actif = false;
        if (tache != null) {
            tache.cancel(false);
        }
        if (planificateur != null) {
            planificateur.shutdownNow();
        }
        // Écrit un dernier instantané, puis marque la fermeture comme propre.
        sauvegarderSansEchec();
        poserMarqueur();
    }

    // ------------------------------------------------------------------
    // Détection de reprise
    // ------------------------------------------------------------------

    /**
     * Décrit une reprise possible détectée au démarrage.
     */
    public static final class Reprise {
        private final Path fichierAutosave;

        Reprise(Path fichierAutosave) {
            this.fichierAutosave = fichierAutosave;
        }

        /** Charge la session interrompue à reprendre. */
        public SessionSalle charger() {
            return DepotSession.charger(fichierAutosave);
        }
    }

    /**
     * Vérifie s'il existe un autosave à reprendre pour un document donné.
     *
     * @param document le document de tournoi que l'on s'apprête à ouvrir
     * @return une reprise possible, ou {@code null} si aucun incident détecté
     */
    public static Reprise detecterReprise(Path document) {
        Path autosave = autosavePour(document);
        Path marqueur = autosave.resolveSibling(autosave.getFileName() + SUFFIXE_MARQUEUR);
        boolean autosaveExiste = Files.exists(autosave);
        boolean fermeturePropre = Files.exists(marqueur);
        if (autosaveExiste && !fermeturePropre) {
            return new Reprise(autosave);
        }
        return null;
    }

    /**
     * Écarte une reprise détectée : supprime l'autosave et son marqueur
     * (l'utilisateur a choisi de ne pas reprendre, ou la reprise est faite).
     */
    public static void ignorerReprise(Path document) {
        Path autosave = autosavePour(document);
        Path marqueur = autosave.resolveSibling(autosave.getFileName() + SUFFIXE_MARQUEUR);
        supprimerSiPresent(autosave);
        supprimerSiPresent(marqueur);
    }

    // ------------------------------------------------------------------
    // Marqueur de fermeture propre
    // ------------------------------------------------------------------

    private void poserMarqueur() {
        try {
            Files.writeString(marqueurCloture,
                    "cloture-propre " + java.time.LocalDateTime.now(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Sans marqueur, la prochaine ouverture proposera une reprise inutile :
            // gênant mais pas dangereux. On n'échoue pas la fermeture pour autant.
        }
    }

    private void supprimerMarqueur() {
        supprimerSiPresent(marqueurCloture);
    }

    private static void supprimerSiPresent(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de supprimer " + p, e);
        }
    }
}
