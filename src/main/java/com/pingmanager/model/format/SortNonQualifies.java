package com.pingmanager.model.format;

/**
 * Sort réservé aux joueurs d'une poule qui ne sont pas qualifiés pour le
 * tableau principal.
 *
 * <p>Le format actuel (poule de 3, 2 qualifiés) élimine le 3e. D'autres formats,
 * ajoutés plus tard, pourront rediriger les non-qualifiés vers une épreuve de
 * consolante ou des parties de classement.</p>
 */
public enum SortNonQualifies {
    /** Les non-qualifiés sont éliminés de l'épreuve. */
    ELIMINE,
    /** Les non-qualifiés basculent vers un tableau de consolante (à venir). */
    CONSOLANTE,
    /** Les non-qualifiés disputent des parties de classement (à venir). */
    PARTIES_DE_CLASSEMENT
}
