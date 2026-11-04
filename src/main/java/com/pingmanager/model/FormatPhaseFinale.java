package com.pingmanager.model;

/**
 * Format de la phase qui suit les poules dans un tableau.
 *
 * <p>Configurable par tableau, comme demandé :</p>
 * <ul>
 *   <li>{@link #ELIMINATION_DIRECTE} : les qualifiés de poule s'affrontent
 *       dans un arbre à élimination directe jusqu'à la finale.</li>
 *   <li>{@link #AUCUNE} : le classement final découle uniquement des poules
 *       (pas de phase à élimination).</li>
 * </ul>
 *
 * <p>Prévu pour être étendu (poules de classement, double élimination, etc.).</p>
 */
public enum FormatPhaseFinale {
    ELIMINATION_DIRECTE,
    AUCUNE
}
