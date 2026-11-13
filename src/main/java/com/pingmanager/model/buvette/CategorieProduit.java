package com.pingmanager.model.buvette;

/**
 * Catégorie d'un produit de la buvette (B046), pour regrouper la carte.
 *
 * <p>Les boissons alcoolisées forment une catégorie à part, jamais proposée par
 * défaut : le club doit l'activer à la main (voir
 * {@link CatalogueBuvette#isAlcoolActive()}).</p>
 */
public enum CategorieProduit {
    BOISSONS_FROIDES("Boissons froides"),
    BOISSONS_CHAUDES("Boissons chaudes"),
    SANDWICHS("Sandwichs et salé"),
    SNACKS("Snacks"),
    DESSERTS("Desserts et sucré"),
    ALCOOLISEES("Boissons alcoolisées"),
    AUTRE("Autre");

    private final String libelle;

    CategorieProduit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return vrai pour la catégorie soumise à activation manuelle. */
    public boolean estAlcoolisee() {
        return this == ALCOOLISEES;
    }

    /** Relecture tolérante d'un nom de constante ({@link #AUTRE} si inconnu). */
    public static CategorieProduit lire(String nom) {
        for (CategorieProduit c : values()) {
            if (c.name().equals(nom)) {
                return c;
            }
        }
        return AUTRE;
    }
}
