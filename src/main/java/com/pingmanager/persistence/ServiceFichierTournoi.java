package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Chef d'orchestre de la persistance côté application : relie le document de
 * tournoi, l'autosave et les données d'application en un cycle de vie cohérent,
 * tel qu'une interface l'attendra.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li><b>Démarrage</b> : charge le dépôt applicatif (clubs, comptes, accès,
 *       tournois récents) depuis le dossier de données par défaut.</li>
 *   <li><b>Nouveau / ouvrir</b> : ouvre un document de tournoi ; avant d'ouvrir,
 *       détecte un éventuel <b>autosave à reprendre</b> et laisse l'appelant
 *       décider (callback), sans jamais trancher à sa place.</li>
 *   <li><b>Session de travail</b> : démarre l'autosave périodique (15 min par
 *       défaut) sur la session courante, fournie à la demande par l'appelant.</li>
 *   <li><b>Enregistrer / enregistrer-sous</b> : écrit le document (atomique), met
 *       à jour les récents, sauvegarde le dépôt applicatif.</li>
 *   <li><b>Fermer</b> : arrête proprement l'autosave (marqueur de fermeture
 *       propre) et persiste le dépôt applicatif.</li>
 * </ul>
 *
 * <p>Le service ne connaît rien du métier : il reçoit un fournisseur d'état de
 * session ({@link Supplier}&lt;{@link SessionSalle}&gt;) et signale les moments
 * clés via un {@link Ecouteur} — ce qui le rend directement pilotable par une
 * future interface. Il vit dans la couche de persistance ; le cœur n'en dépend
 * pas (règle d'or).</p>
 */
public final class ServiceFichierTournoi implements AutoCloseable {

    /**
     * Callbacks émis aux moments clés. Toutes les méthodes ont une implémentation
     * vide par défaut : un appelant n'implémente que ce qui l'intéresse.
     */
    public interface Ecouteur {
        /**
         * Une reprise sur incident a été détectée pour le document qu'on s'apprête
         * à ouvrir. L'appelant décide : {@code decision.reprendre()} pour charger
         * l'autosave, {@code decision.ignorer()} pour l'écarter et ouvrir le
         * document normal. Tant qu'aucune des deux n'est appelée, l'ouverture est
         * suspendue.
         */
        default void repriseDetectee(RepriseDecision decision) {
            decision.ignorer();
        }

        /** Un document vient d'être ouvert ou créé (chemin courant, ou vide si non enregistré). */
        default void documentOuvert(Optional<Path> fichier) {
        }

        /** Le document courant vient d'être enregistré à cet emplacement. */
        default void documentEnregistre(Path fichier) {
        }

        /** Le document courant a été fermé. */
        default void documentFerme() {
        }
    }

    /** Remise à l'appelant lors d'une reprise détectée : à lui de trancher. */
    public interface RepriseDecision {
        /** Reprendre : la session interrompue (autosave) devient la session courante. */
        void reprendre();

        /** Ignorer : écarter l'autosave et poursuivre avec le document normal. */
        void ignorer();
    }

    private final Path dossierDonnees;
    private final Duration frequenceAutosave;
    private final Ecouteur ecouteur;

    private DepotApplication application;
    private Path fichierCourant;             // null tant que non enregistré
    private ServiceAutosave autosaveCourant; // null hors session
    private boolean repriseEnAttente;        // garde-fou du protocole de décision

    /**
     * @param dossierDonnees    dossier des données applicatives (dépôt clubs/comptes)
     * @param frequenceAutosave intervalle de l'autosave (15 min conseillé)
     * @param ecouteur          récepteur des événements (jamais nul ; utiliser un
     *                          écouteur vide si rien à observer)
     */
    public ServiceFichierTournoi(Path dossierDonnees, Duration frequenceAutosave, Ecouteur ecouteur) {
        this.dossierDonnees = Objects.requireNonNull(dossierDonnees, "dossierDonnees").toAbsolutePath();
        this.frequenceAutosave = Objects.requireNonNull(frequenceAutosave, "frequenceAutosave");
        this.ecouteur = Objects.requireNonNull(ecouteur, "ecouteur");
    }

    /** Construit un service avec les réglages par défaut (dossier ~/.pingmanager, 15 min). */
    public static ServiceFichierTournoi parDefaut(Ecouteur ecouteur) {
        return new ServiceFichierTournoi(
                dossierDonneesParDefaut(),
                ServiceAutosave.FREQUENCE_PAR_DEFAUT,
                ecouteur);
    }

    /**
     * Emplacement stable et discret des données applicatives :
     * {@code $XDG_DATA_HOME/pingmanager} si défini, sinon {@code ~/.pingmanager}.
     */
    public static Path dossierDonneesParDefaut() {
        String xdg = System.getenv("XDG_DATA_HOME");
        Path base = (xdg != null && !xdg.isBlank())
                ? Path.of(xdg)
                : Path.of(System.getProperty("user.home", "."), ".pingmanager");
        return base.resolve("pingmanager");
    }

    // ==================================================================
    // Démarrage
    // ==================================================================

    /**
     * Démarre l'application : charge le dépôt applicatif (ou en crée un vide au
     * premier lancement). À appeler une fois avant tout le reste.
     *
     * @return le dépôt applicatif chargé (clubs, comptes, accès, récents)
     */
    public DepotApplication demarrer() {
        application = DepotApplication.charger(cheminDepotApplication());
        return application;
    }

    /** @return le dépôt applicatif courant (après {@link #demarrer()}). */
    public DepotApplication application() {
        exigerDemarre();
        return application;
    }

    /** @return la liste des tournois récents, du plus récent au plus ancien. */
    public List<ProjetTournoi> recents() {
        exigerDemarre();
        return application.tousLesRecents();
    }

    // ==================================================================
    // Nouveau / ouvrir
    // ==================================================================

    /**
     * Crée un nouveau tournoi en session, non encore enregistré sur disque.
     * L'autosave ne démarre qu'une fois le fournisseur d'état fourni via
     * {@link #activerAutosave(Supplier)} (généralement après le premier
     * enregistrement, quand un chemin existe).
     */
    public SessionSalle nouveau(Tournoi tournoi, RegistreDisciplinaire discipline) {
        exigerDemarre();
        fermerSessionCouranteSiBesoin();
        fichierCourant = null;
        SessionSalle session = new SessionSalle(tournoi, discipline, null, null);
        ecouteur.documentOuvert(Optional.empty());
        return session;
    }

    /**
     * Ouvre un tournoi depuis un fichier. Si un autosave non fermé proprement
     * existe pour ce fichier, l'écouteur est sollicité pour décider de reprendre
     * ou d'ignorer ; sinon le document est chargé directement.
     *
     * @return la session ouverte (issue du document, ou de l'autosave si reprise)
     */
    public SessionSalle ouvrir(Path fichier) {
        exigerDemarre();
        Objects.requireNonNull(fichier, "fichier");
        fermerSessionCouranteSiBesoin();

        Path abs = fichier.toAbsolutePath();
        ServiceAutosave.Reprise reprise = ServiceAutosave.detecterReprise(abs);

        SessionSalle session;
        if (reprise != null) {
            session = resoudreReprise(abs, reprise);
        } else {
            session = chargerDocument(abs);
        }

        fichierCourant = abs;
        noterRecent(abs, session.getTournoi());
        ecouteur.documentOuvert(Optional.of(abs));
        return session;
    }

    private SessionSalle resoudreReprise(Path fichier, ServiceAutosave.Reprise reprise) {
        // Protocole : on remet la décision à l'appelant via un objet à double
        // détente. Un seul des deux chemins peut être emprunté ; par sécurité, si
        // l'écouteur ne tranche pas, on considère la reprise ignorée (défaut).
        final SessionSalle[] resultat = new SessionSalle[1];
        repriseEnAttente = true;
        RepriseDecision decision = new RepriseDecision() {
            @Override
            public void reprendre() {
                if (!repriseEnAttente) {
                    return;
                }
                repriseEnAttente = false;
                resultat[0] = reprise.charger();
            }

            @Override
            public void ignorer() {
                if (!repriseEnAttente) {
                    return;
                }
                repriseEnAttente = false;
                ServiceAutosave.ignorerReprise(fichier);
                resultat[0] = chargerDocument(fichier);
            }
        };
        ecouteur.repriseDetectee(decision);
        if (repriseEnAttente) {
            // L'écouteur n'a rien décidé : on applique le défaut sûr (ignorer).
            decision.ignorer();
        }
        return resultat[0];
    }

    private SessionSalle chargerDocument(Path fichier) {
        TournoiMapper.DocumentCharge charge = DepotTournoi.charger(fichier);
        // Un document ne porte pas l'état vivant d'ordonnancement : session de
        // préparation (l'ordonnancement sera rebâti par le métier au jour J).
        return new SessionSalle(charge.getTournoi(), charge.getDiscipline(), null, null);
    }

    // ==================================================================
    // Autosave de la session courante
    // ==================================================================

    /**
     * Active l'autosave périodique sur la session courante. À appeler une fois
     * qu'un chemin de document existe (après un premier enregistrement), en
     * fournissant un accès à l'état vivant à sauvegarder.
     *
     * @param fournisseurEtat fournit l'instantané de session à chaque tick
     * @throws IllegalStateException si aucun document n'est encore enregistré
     */
    public void activerAutosave(Supplier<SessionSalle> fournisseurEtat) {
        exigerDemarre();
        Objects.requireNonNull(fournisseurEtat, "fournisseurEtat");
        if (fichierCourant == null) {
            throw new IllegalStateException(
                    "Enregistrez le tournoi avant d'activer l'autosave (aucun chemin de fichier).");
        }
        arreterAutosave();
        Path autosave = ServiceAutosave.autosavePour(fichierCourant);
        autosaveCourant = new ServiceAutosave(autosave, fournisseurEtat, frequenceAutosave);
        autosaveCourant.demarrer();
    }

    /** @return true si l'autosave tourne pour la session courante. */
    public boolean autosaveActif() {
        return autosaveCourant != null;
    }

    private void arreterAutosave() {
        if (autosaveCourant != null) {
            autosaveCourant.close(); // pose le marqueur de fermeture propre
            autosaveCourant = null;
        }
    }

    // ==================================================================
    // Enregistrer / enregistrer-sous
    // ==================================================================

    /**
     * Enregistre le document courant à son emplacement connu.
     *
     * @throws IllegalStateException si le document n'a pas encore d'emplacement
     *                               (utiliser {@link #enregistrerSous})
     */
    public void enregistrer(Tournoi tournoi, RegistreDisciplinaire discipline) {
        exigerDemarre();
        if (fichierCourant == null) {
            throw new IllegalStateException("Aucun emplacement : utilisez enregistrerSous(...).");
        }
        ecrireDocument(tournoi, discipline, fichierCourant);
    }

    /**
     * Enregistre le document courant vers un nouvel emplacement, qui devient
     * l'emplacement courant. Bascule aussi l'autosave (s'il tournait) vers le
     * nouveau fichier.
     */
    public void enregistrerSous(Tournoi tournoi, RegistreDisciplinaire discipline, Path cible) {
        exigerDemarre();
        Objects.requireNonNull(cible, "cible");
        Path abs = cible.toAbsolutePath();
        boolean autosaveTournait = autosaveActif();
        Supplier<SessionSalle> fournisseur = null;
        if (autosaveTournait) {
            fournisseur = autosaveCourant.fournisseur();
            arreterAutosave();
        }
        ecrireDocument(tournoi, discipline, abs);
        fichierCourant = abs;
        if (autosaveTournait) {
            activerAutosave(fournisseur);
        }
    }

    private void ecrireDocument(Tournoi tournoi, RegistreDisciplinaire discipline, Path cible) {
        DepotTournoi.sauvegarder(tournoi, discipline, cible);
        noterRecent(cible, tournoi);
        sauvegarderApplication();
        ecouteur.documentEnregistre(cible);
    }

    /** @return l'emplacement du document courant, ou vide si non encore enregistré. */
    public Optional<Path> fichierCourant() {
        return Optional.ofNullable(fichierCourant);
    }

    // ==================================================================
    // Fermeture
    // ==================================================================

    /** Ferme la session courante proprement (autosave arrêté + marqueur), sans quitter l'application. */
    public void fermerDocument() {
        if (fichierCourant == null && autosaveCourant == null) {
            return;
        }
        arreterAutosave();
        fichierCourant = null;
        ecouteur.documentFerme();
    }

    private void fermerSessionCouranteSiBesoin() {
        if (autosaveCourant != null || fichierCourant != null) {
            fermerDocument();
        }
    }

    /**
     * Ferme l'application : ferme la session courante et persiste le dépôt
     * applicatif. Idempotent.
     */
    @Override
    public void close() {
        arreterAutosave();
        if (application != null) {
            sauvegarderApplication();
        }
        fichierCourant = null;
    }

    // ==================================================================
    // Interne
    // ==================================================================

    private void noterRecent(Path fichier, Tournoi tournoi) {
        application.noterRecent(new ProjetTournoi(fichier, tournoi.getNom(), LocalDateTime.now()));
        sauvegarderApplication();
    }

    private void sauvegarderApplication() {
        application.sauvegarder(cheminDepotApplication());
    }

    private Path cheminDepotApplication() {
        return dossierDonnees.resolve(DepotApplication.NOM_FICHIER);
    }

    private void exigerDemarre() {
        if (application == null) {
            throw new IllegalStateException("Appelez demarrer() avant d'utiliser le service.");
        }
    }
}
