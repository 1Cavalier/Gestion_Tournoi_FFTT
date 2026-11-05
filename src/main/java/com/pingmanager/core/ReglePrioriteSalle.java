package com.pingmanager.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Règle de couleur des matchs proposés (cahier des charges v0, section 7) :
 * rouge, orange ou vert selon l'attente, l'importance du match et la stratégie
 * d'ordonnancement.
 *
 * <ul>
 *   <li><b>Rouge</b> (urgent) : le match attend depuis {@link #ATTENTE_LONGUE}
 *       ou plus, ou c'est un match important qui attend depuis
 *       {@link #ATTENTE_MOYENNE} ou plus ;</li>
 *   <li><b>Orange</b> (important) : match important (demi-finale, finale, joueur
 *       attendu dans un autre tableau), attente d'au moins
 *       {@link #ATTENTE_MOYENNE}, ou match que la stratégie de l'ordonnanceur
 *       enverrait maintenant (il fait partie des premiers proposés, autant que
 *       de tables libres) ;</li>
 *   <li><b>Vert</b> (normal) : sinon.</li>
 * </ul>
 *
 * <p>Ce n'est pas une règle fédérale : le juge-arbitre programme les parties
 * « en fonction de l'horaire retenu et des tables disponibles » (3.3.1.2.2) et
 * fixe « l'horaire des parties » (IX.109). La couleur n'est qu'une aide ; les
 * seuils sont des choix d'organisation (question ouverte dans le journal B037).
 * Aucun temps de repos minimal n'est imposé par les textes fournis.</p>
 */
public final class ReglePrioriteSalle {

    /** Attente à partir de laquelle un match passe au rouge. */
    public static final Duration ATTENTE_LONGUE = Duration.ofMinutes(20);

    /** Attente à partir de laquelle un match passe à l'orange (au rouge s'il est important). */
    public static final Duration ATTENTE_MOYENNE = Duration.ofMinutes(10);

    /** Niveau et raisons lisibles (« attend depuis 25 min », « finale »…). */
    public record Evaluation(NiveauPriorite niveau, List<String> raisons) {
        public Evaluation {
            Objects.requireNonNull(niveau, "niveau");
            raisons = List.copyOf(raisons);
        }

        /** Raisons séparées par des virgules, vide si aucune. */
        public String motif() {
            return String.join(", ", raisons);
        }
    }

    private ReglePrioriteSalle() {
    }

    /**
     * Évalue un match proposé.
     *
     * @param attente            depuis quand le match est prêt (null : inconnue, comptée nulle)
     * @param importance         raison de l'importance du match (« finale », « X joue aussi B »…),
     *                           ou null / vide si le match n'a rien de particulier
     * @param choisiParStrategie le match fait-il partie de ceux que l'ordonnanceur enverrait
     *                           maintenant sur les tables libres ?
     */
    public static Evaluation evaluer(Duration attente, String importance, boolean choisiParStrategie) {
        Duration a = attente == null || attente.isNegative() ? Duration.ZERO : attente;
        boolean important = importance != null && !importance.isBlank();
        boolean longue = a.compareTo(ATTENTE_LONGUE) >= 0;
        boolean moyenne = a.compareTo(ATTENTE_MOYENNE) >= 0;

        List<String> raisons = new ArrayList<>();
        if (moyenne) {
            raisons.add("attend depuis " + a.toMinutes() + " min");
        }
        if (important) {
            raisons.add(importance);
        }
        if (longue || (important && moyenne)) {
            return new Evaluation(NiveauPriorite.URGENT, raisons);
        }
        if (important || moyenne || choisiParStrategie) {
            if (choisiParStrategie && raisons.isEmpty()) {
                raisons.add("prochain à envoyer");
            }
            return new Evaluation(NiveauPriorite.IMPORTANT, raisons);
        }
        return new Evaluation(NiveauPriorite.NORMAL, raisons);
    }
}
