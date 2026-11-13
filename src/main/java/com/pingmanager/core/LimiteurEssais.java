package com.pingmanager.core;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Limite d'essais (README section 5, point 15) pour la connexion et pour les codes à
 * 6 chiffres : après {@value #SEUIL_PAR_DEFAUT} échecs consécutifs pour une même clé
 * (un email, un email et un club…), la clé est bloquée un moment, et le blocage double
 * à chaque nouvel échec : 1 min, 2 min, 4 min… jusqu'à 1 h au plus. Une réussite remet
 * le compteur à zéro.
 *
 * <p>Le temps passe par le port {@link Horloge} (tests à horloge contrôlée). L'état vit
 * en mémoire, le temps de la session de l'application : un redémarrage le remet à zéro
 * (question en section 6 du README).</p>
 */
public final class LimiteurEssais {

    /** Nombre d'échecs consécutifs avant le premier blocage. */
    public static final int SEUIL_PAR_DEFAUT = 5;
    /** Durée du premier blocage. */
    public static final Duration BLOCAGE_INITIAL_PAR_DEFAUT = Duration.ofMinutes(1);
    /** Durée maximale d'un blocage. */
    public static final Duration BLOCAGE_MAXIMAL_PAR_DEFAUT = Duration.ofHours(1);

    private final Horloge horloge;
    private final int seuil;
    private final Duration blocageInitial;
    private final Duration blocageMaximal;
    private final Map<String, Etat> etats = new HashMap<>();

    private static final class Etat {
        int echecs;
        Instant bloqueJusqua;
    }

    public LimiteurEssais(Horloge horloge) {
        this(horloge, SEUIL_PAR_DEFAUT, BLOCAGE_INITIAL_PAR_DEFAUT, BLOCAGE_MAXIMAL_PAR_DEFAUT);
    }

    public LimiteurEssais(Horloge horloge, int seuil, Duration blocageInitial, Duration blocageMaximal) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        if (seuil < 1) {
            throw new IllegalArgumentException("Le seuil doit valoir au moins 1.");
        }
        if (blocageInitial.isNegative() || blocageInitial.isZero()
                || blocageMaximal.compareTo(blocageInitial) < 0) {
            throw new IllegalArgumentException("Durées de blocage incohérentes.");
        }
        this.seuil = seuil;
        this.blocageInitial = blocageInitial;
        this.blocageMaximal = blocageMaximal;
    }

    private static String normaliser(String cle) {
        return cle == null ? "" : cle.trim().toLowerCase(Locale.ROOT);
    }

    /** @return le temps d'attente restant avant un nouvel essai ({@link Duration#ZERO} si libre). */
    public synchronized Duration attenteRestante(String cle) {
        Etat e = etats.get(normaliser(cle));
        if (e == null || e.bloqueJusqua == null) {
            return Duration.ZERO;
        }
        Duration reste = Duration.between(horloge.maintenant(), e.bloqueJusqua);
        return reste.isNegative() ? Duration.ZERO : reste;
    }

    /** @return {@code true} si la clé est bloquée en ce moment. */
    public boolean estBloque(String cle) {
        return !attenteRestante(cle).isZero();
    }

    /** Enregistre un échec ; à partir du seuil, bloque la clé pour une durée croissante. */
    public synchronized void enregistrerEchec(String cle) {
        Etat e = etats.computeIfAbsent(normaliser(cle), k -> new Etat());
        e.echecs++;
        if (e.echecs >= seuil) {
            int rang = Math.min(e.echecs - seuil, 30);
            Duration duree = blocageInitial.multipliedBy(1L << rang);
            if (duree.compareTo(blocageMaximal) > 0) {
                duree = blocageMaximal;
            }
            e.bloqueJusqua = horloge.maintenant().plus(duree);
        }
    }

    /** Une réussite efface l'historique d'échecs de la clé. */
    public synchronized void enregistrerReussite(String cle) {
        etats.remove(normaliser(cle));
    }

    /** @return le nombre d'échecs consécutifs enregistrés pour la clé. */
    public synchronized int echecs(String cle) {
        Etat e = etats.get(normaliser(cle));
        return e == null ? 0 : e.echecs;
    }

    /** @return l'attente en clair, arrondie à la minute supérieure (« 1 min », « 4 min »). */
    public static String formaterAttente(Duration attente) {
        long secondes = Math.max(0, attente.getSeconds() + (attente.getNano() > 0 ? 1 : 0));
        long minutes = Math.max(1, (secondes + 59) / 60);
        return minutes + " min";
    }

    /** @return le message d'un refus pour trop d'essais. */
    public static String messageBlocage(Duration attente) {
        return "Trop d'essais infructueux : réessayez dans " + formaterAttente(attente) + ".";
    }
}
