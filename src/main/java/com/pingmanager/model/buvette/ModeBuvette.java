package com.pingmanager.model.buvette;

/**
 * Réglage de la buvette pour un tournoi (B026) : suivre seulement le stock, ou
 * enregistrer aussi les ventes au comptoir pour tenir les comptes du club.
 *
 * <p>Ce n'est pas une caisse enregistreuse : c'est un suivi interne du club.</p>
 */
public enum ModeBuvette {
    /** Comportement d'origine : les sorties de stock se saisissent à la main. */
    STOCK_SEUL("Stock seulement", "Les sorties de stock se saisissent à la main, aucune vente n'est enregistrée."),
    /** Écran de comptoir : chaque vente est enregistrée et sort le stock. */
    STOCK_ET_VENTES("Stock et ventes", "Chaque vente du comptoir est enregistrée (prix, moyen de paiement) "
            + "et sort du stock le produit et ses contenants.");

    private final String libelle;
    private final String explication;

    ModeBuvette(String libelle, String explication) {
        this.libelle = libelle;
        this.explication = explication;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getExplication() {
        return explication;
    }

    /** Relecture tolérante : {@link #STOCK_SEUL} si absent ou inconnu (anciens fichiers). */
    public static ModeBuvette lire(String nom) {
        for (ModeBuvette m : values()) {
            if (m.name().equals(nom)) {
                return m;
            }
        }
        return STOCK_SEUL;
    }
}
