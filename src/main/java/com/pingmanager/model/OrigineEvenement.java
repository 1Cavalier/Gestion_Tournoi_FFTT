package com.pingmanager.model;

/**
 * Origine d'une date de l'agenda du compte : saisie à la main, ajoutée
 * automatiquement à l'inscription à un tournoi (et retirée à la désinscription
 * complète), ou copiée du calendrier fédéral par le titulaire (B045).
 *
 * <p>Les anciens fichiers n'ont que {@code PERSONNEL} et {@code TOURNOI} ; une
 * valeur inconnue est relue comme date personnelle.</p>
 */
public enum OrigineEvenement {
    /** Date ajoutée par le titulaire du compte. */
    PERSONNEL,
    /** Tournoi auquel le joueur est inscrit (ou en liste d'attente). */
    TOURNOI,
    /**
     * Repère du calendrier fédéral copié dans l'agenda (« Ajouter à mon agenda ») :
     * modifiable et retirable comme une date personnelle.
     */
    FEDERAL
}
