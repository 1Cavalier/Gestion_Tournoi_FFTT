package com.pingmanager.model;

/**
 * Cycle de vie d'un match au cours du tournoi.
 */
public enum StatutMatch {
    /** Créé mais pas encore prêt (ex. en attente des résultats de poule). */
    EN_ATTENTE,
    /** Prêt à être joué, peut être affecté à une table. */
    A_JOUER,
    /** En cours sur une table. */
    EN_COURS,
    /** Terminé, score enregistré. */
    TERMINE
}
