package com.pingmanager.model;

/**
 * Mode de restriction d'accès d'un {@link Tableau} selon le classement (points)
 * des joueurs.
 *
 * <p>Un tableau applique l'un de ces trois modes :</p>
 * <ul>
 *   <li>{@link #TRANCHE} : seuls les joueurs dont les points sont compris entre
 *       une borne basse et une borne haute (incluses) peuvent s'inscrire
 *       (ex. « tableau 1000 à 1299 points »).</li>
 *   <li>{@link #POINT_MAX} : seul un plafond de points est vérifié ; tout joueur
 *       à ce plafond ou en dessous peut s'inscrire (ex. « tableau &lt;= 1500 points »).</li>
 *   <li>{@link #TOUTES_SERIES} : aucune limite de points (« Toutes Séries »).</li>
 * </ul>
 *
 * <p>Dans les modes {@link #POINT_MAX} et {@link #TOUTES_SERIES}, l'organisateur
 * peut en plus exclure les meilleurs joueurs français par leur <b>rang national</b>
 * (numérotation), et non par leurs points : voir
 * {@link ConfigurationTableau#getExclusionRangNational()}. Cette exclusion n'a pas
 * de sens en mode {@link #TRANCHE}, où la sélection se fait déjà par une fourchette
 * de points.</p>
 */
public enum ModeLimiteClassement {

    /** Fourchette fermée [pointsMin, pointsMax]. */
    TRANCHE,

    /** Plafond de points : points &lt;= pointsMax. */
    POINT_MAX,

    /** Aucune limite de points. */
    TOUTES_SERIES
}
