package com.pingmanager.model.buvette;

/** Unité dans laquelle un produit est vendu au comptoir (B046). */
public enum UniteVente {
    GOBELET("gobelet"),
    CANETTE("canette"),
    BOUTEILLE("bouteille"),
    PART("part"),
    PORTION("portion"),
    PIECE("pièce"),
    SACHET("sachet");

    private final String libelle;

    UniteVente(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Relecture tolérante d'un nom de constante ({@link #PORTION} si inconnu). */
    public static UniteVente lire(String nom) {
        for (UniteVente u : values()) {
            if (u.name().equals(nom)) {
                return u;
            }
        }
        return PORTION;
    }
}
