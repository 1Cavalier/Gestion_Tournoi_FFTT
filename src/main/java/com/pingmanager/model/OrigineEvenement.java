package com.pingmanager.model;

/**
 * Origine d'une date de l'agenda du compte : saisie à la main, ou ajoutée
 * automatiquement à l'inscription à un tournoi (et retirée à la désinscription
 * complète).
 */
public enum OrigineEvenement {
    /** Date ajoutée par le titulaire du compte. */
    PERSONNEL,
    /** Tournoi auquel le joueur est inscrit (ou en liste d'attente). */
    TOURNOI
}
