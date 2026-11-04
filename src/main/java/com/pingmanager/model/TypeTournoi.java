package com.pingmanager.model;

/**
 * Type d'un tournoi, tel que déclaré dans la demande d'homologation FFTT
 * (champ obligatoire du formulaire SPID).
 *
 * <p>Les valeurs pourront être ajustées à la nomenclature officielle exacte ;
 * elles suffisent à porter l'information exigée pour l'homologation.</p>
 */
public enum TypeTournoi {

    /** Tournoi promotionnel. */
    PROMOTIONNEL,
    /** Tournoi national. */
    NATIONAL,
    /** Tournoi international. */
    INTERNATIONAL,
    /** Autre catégorie. */
    AUTRE
}
