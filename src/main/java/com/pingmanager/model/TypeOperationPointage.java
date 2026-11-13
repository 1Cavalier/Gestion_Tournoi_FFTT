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
    /** Désistement : le joueur quitte le tableau ; la valeur porte le motif. */
    DESINSCRIRE
}
