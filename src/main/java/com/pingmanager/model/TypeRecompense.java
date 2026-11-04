package com.pingmanager.model;

/**
 * Nature d'une {@link Recompense} attribuée à une place d'un tableau.
 *
 * <ul>
 *   <li>{@link #ARGENT} : dotation en euros (cas le plus fréquent).</li>
 *   <li>{@link #REDUCTION_POURCENTAGE} : réduction exprimée en pourcentage brut.
 *       La base sur laquelle s'applique cette réduction sera précisée
 *       ultérieurement ; à ce stade, seul le pourcentage est stocké.</li>
 *   <li>{@link #AUTRE} : récompense décrite librement (coupe, lot, bon…).</li>
 * </ul>
 */
public enum TypeRecompense {
    ARGENT,
    REDUCTION_POURCENTAGE,
    /** Part (en %) des droits d'engagement perçus sur le tableau (ex. 50 % au vainqueur). */
    PART_INSCRIPTIONS,
    AUTRE
}
