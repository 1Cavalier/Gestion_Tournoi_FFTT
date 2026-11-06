package com.pingmanager.model;

/**
 * Manière dont un match se conclut, au sens du règlement FFTT (article I.303).
 *
 * <p>La distinction est importante pour le classement en poule : une défaite
 * <b>jouée normalement</b> rapporte 1 point, mais une absence à l'appel, un
 * abandon, un refus de jouer ou une disqualification font marquer <b>0 point</b>
 * au joueur concerné.</p>
 */
public enum IssueMatch {

    /** Match joué jusqu'à son terme ; le perdant a marqué 1 point (poule). */
    NORMALE,

    /**
     * Un joueur est absent à l'appel de son nom : il est déclaré forfait pour ce
     * match et marque 0 point (I.303).
     */
    FORFAIT,

    /**
     * Un joueur abandonne en cours de match (blessure, maladie, autre) : il
     * marque 0 point (I.303).
     */
    ABANDON,

    /** Un joueur refuse de jouer : 0 point (I.303). */
    REFUS_DE_JOUER,

    /**
     * Un joueur est disqualifié (comportement, carton rouge du juge-arbitre) :
     * 0 point (I.303).
     */
    DISQUALIFICATION;

}
