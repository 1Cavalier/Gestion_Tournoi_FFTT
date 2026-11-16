package com.pingmanager.core.buvette;

/**
 * Niveau du stock d'un article de la buvette (B026), affiché par une jauge.
 *
 * <ul>
 *   <li>{@link #URGENT} (rouge) : stock au seuil d'alerte ou en dessous, ou
 *       stock vide : à racheter d'urgence ;</li>
 *   <li>{@link #BAS} (orange) : stock au plus deux fois le seuil ;</li>
 *   <li>{@link #SUFFISANT} (vert) : au-delà.</li>
 * </ul>
 */
public enum NiveauStock {
    SUFFISANT("Suffisant"),
    BAS("Bas"),
    URGENT("À racheter");

    /** Le stock est « bas » jusqu'à ce multiple du seuil d'alerte. */
    public static final int MULTIPLE_BAS = 2;

    private final String libelle;

    NiveauStock(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Niveau d'un stock pour un seuil d'alerte donné. */
    public static NiveauStock de(long stock, long seuil) {
        if (stock <= 0 || stock <= seuil) {
            return URGENT;
        }
        if (stock <= MULTIPLE_BAS * seuil) {
            return BAS;
        }
        return SUFFISANT;
    }
}
