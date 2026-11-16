package com.pingmanager.model.buvette;

/**
 * Famille d'un article de la buvette (B026) : ce qui se consomme ou ce qui
 * l'accompagne.
 */
public enum FamilleArticle {
    /** Ce qui se consomme : café, boissons, sandwichs, gâteaux… */
    PRODUIT("Produits", "Produit"),
    /** Ce qui accompagne : gobelets, touillettes, serviettes, assiettes en carton… */
    CONTENANT("Contenants et consommables", "Contenant ou consommable");

    private final String libellePluriel;
    private final String libelle;

    FamilleArticle(String libellePluriel, String libelle) {
        this.libellePluriel = libellePluriel;
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getLibellePluriel() {
        return libellePluriel;
    }

    /** Relecture tolérante d'un nom de constante ({@link #PRODUIT} si inconnu). */
    public static FamilleArticle lire(String nom) {
        for (FamilleArticle f : values()) {
            if (f.name().equals(nom)) {
                return f;
            }
        }
        return PRODUIT;
    }
}
