package com.pingmanager.model.buvette;

/**
 * Rayon d'un article de la buvette (B026) : sert à ranger le catalogue de base,
 * la liste des articles du club et la grille du comptoir.
 */
public enum RayonBuvette {
    BOISSONS_CHAUDES("Boissons chaudes"),
    BOISSONS_FROIDES("Boissons froides"),
    SALE("Salé"),
    SUCRE("Sucré"),
    /** Produit créé par le club qui n'entre dans aucun autre rayon. */
    AUTRE("Autres produits"),
    /** Gobelets, touillettes, serviettes… : le seul rayon de la famille « contenant ». */
    CONTENANTS("Contenants et consommables");

    private final String libelle;

    RayonBuvette(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return la famille des articles de ce rayon. */
    public FamilleArticle famille() {
        return this == CONTENANTS ? FamilleArticle.CONTENANT : FamilleArticle.PRODUIT;
    }

    /** @return les rayons des produits, dans l'ordre d'affichage. */
    public static RayonBuvette[] rayonsProduits() {
        return new RayonBuvette[]{BOISSONS_CHAUDES, BOISSONS_FROIDES, SALE, SUCRE, AUTRE};
    }

    /**
     * Rayon cohérent avec la famille : un contenant est toujours au rayon
     * {@link #CONTENANTS} ; un produit sans rayon (ou au rayon des contenants)
     * va dans {@link #AUTRE}.
     */
    public static RayonBuvette accorder(RayonBuvette rayon, FamilleArticle famille) {
        if (famille == FamilleArticle.CONTENANT) {
            return CONTENANTS;
        }
        return rayon == null || rayon == CONTENANTS ? AUTRE : rayon;
    }

    /** Relecture tolérante d'un nom de constante ({@code null} si inconnu ou vide). */
    public static RayonBuvette lire(String nom) {
        for (RayonBuvette r : values()) {
            if (r.name().equals(nom)) {
                return r;
            }
        }
        return null;
    }
}
