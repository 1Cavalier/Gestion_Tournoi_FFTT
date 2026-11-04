package com.pingmanager.ordonnancement;

/**
 * Phase d'un match dans le déroulé d'un tableau, utilisée pour prioriser les
 * lancements « par vagues » : on privilégie l'écoulement des poules, puis le
 * premier tour du tableau à élimination, puis les tours suivants.
 *
 * <p>L'ordre de déclaration reflète la priorité décroissante (POULE en premier).</p>
 */
public enum PhaseMatch {
    /** Match de poule. */
    POULE,
    /** Premier tour du tableau à élimination directe (64es, 32es…). */
    PREMIER_TOUR_TABLEAU,
    /** Tour suivant du tableau à élimination. */
    TOUR_SUIVANT_TABLEAU
}
