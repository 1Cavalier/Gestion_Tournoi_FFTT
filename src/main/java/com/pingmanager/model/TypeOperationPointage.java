package com.pingmanager.model;

/** Nature d'une {@link OperationPointage} faite sur un poste de pointage (B041). */
public enum TypeOperationPointage {
    /** Le joueur est présent pour ce tableau. */
    POINTER,
    /** Correction : le pointage est retiré. */
    DEPOINTER,
    /** L'engagement est réglé ; la valeur porte le {@link ModeReglement}. */
    MARQUER_PAYE,
    /** Correction : le paiement est annulé. */
    MARQUER_NON_PAYE,
    /**
     * Désinscription : le joueur quitte le tableau ; la valeur porte la précision (ou
     * le libellé du motif), le motif est un {@link MotifRetrait} (B043).
     */
    DESINSCRIRE,
    /** B043 : non pointé à la clôture du pointage, place libérée (jamais un choix du joueur). */
    NON_POINTE,
    /** B043 : un retrait (désinscription ou non pointé) est annulé, le joueur est réinscrit. */
    ANNULER_RETRAIT
}
