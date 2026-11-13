package com.pingmanager.model.buvette;

/**
 * Nature d'un mouvement de stock de la buvette (B046). Les quantités sont des
 * portions, signées : positives pour une entrée, négatives pour une sortie.
 */
public enum TypeMouvementStock {
    STOCK_DEPART("Stock de départ"),
    ACHAT("Achat"),
    REPORT("Report d'un autre tournoi"),
    VENTE("Vente"),
    PERTE("Perte"),
    DON("Don"),
    INVENTAIRE("Écart d'inventaire"),
    CORRECTION("Correction");

    private final String libelle;

    TypeMouvementStock(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Relecture tolérante : {@code null} si le nom est inconnu (ligne écartée). */
    public static TypeMouvementStock lire(String nom) {
        for (TypeMouvementStock t : values()) {
            if (t.name().equals(nom)) {
                return t;
            }
        }
        return null;
    }
}
