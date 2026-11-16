package com.pingmanager.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Trace d'un tirage au sort effectué (poules ou tableau final) : quand il a été
 * fait et sous la responsabilité de quel juge-arbitre.
 *
 * <p>IX.109 : le juge-arbitre est le responsable direct du tirage au sort.
 * IX.111 : le règlement du tournoi indique la date, l'heure et le lieu du tirage
 * au sort public ; la trace permet de rapprocher le tirage réellement fait de
 * celui annoncé. Manuel JA, I-6.4 : un joueur placé par tirage au sort ne peut
 * plus être déplacé.</p>
 *
 * <p>La graine du générateur aléatoire n'en fait pas partie : c'est un
 * mécanisme technique interne (reprise après incident, tests), conservé à part
 * dans {@link Tableau}. Aucun texte fédéral ne l'impose.</p>
 *
 * <p>Value-class immuable.</p>
 *
 * @param effectueLe  instant du tirage (obligatoire)
 * @param jugeArbitre « Prénom Nom » du juge-arbitre principal au moment du
 *                    tirage, ou {@code null} si aucun n'était désigné
 */
public record TraceTirage(Instant effectueLe, String jugeArbitre) {

    public TraceTirage {
        Objects.requireNonNull(effectueLe, "effectueLe");
        if (jugeArbitre != null && jugeArbitre.isBlank()) {
            jugeArbitre = null;
        }
        if (jugeArbitre != null) {
            jugeArbitre = jugeArbitre.strip();
        }
    }
}
