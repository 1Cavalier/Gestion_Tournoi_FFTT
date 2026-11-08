package com.pingmanager.model;

/**
 * Nature d'une convocation par SMS (B019). Pas de {@code switch} sur cette
 * énumération : le libellé est porté par la constante.
 */
public enum TypeConvocation {

    /** Match de poule : « Tableau C, poule B ». */
    MATCH_POULE("Match de poule"),
    /** Tour d'un tableau à élimination : « Tableau C, 8es de finale ». */
    TOUR_TABLEAU("Tour de tableau"),
    /** Match de classement : « Tableau C, match de classement (places 17 à 32) ». */
    MATCH_CLASSEMENT("Match de classement"),
    /** Convocation pour arbitrer une partie. */
    ARBITRAGE("Arbitrage");

    private final String libelle;

    TypeConvocation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
