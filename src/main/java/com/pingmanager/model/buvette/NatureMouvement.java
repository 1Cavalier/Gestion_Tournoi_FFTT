package com.pingmanager.model.buvette;

/** Nature d'un mouvement de stock de la buvette (B026). */
public enum NatureMouvement {
    ENTREE("Entrée"),
    SORTIE("Sortie"),
    RECOMPTAGE("Recomptage");

    private final String libelle;

    NatureMouvement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Relecture tolérante : {@code null} si le nom est inconnu (ligne écartée). */
    public static NatureMouvement lire(String nom) {
        for (NatureMouvement n : values()) {
            if (n.name().equals(nom)) {
                return n;
            }
        }
        return null;
    }
}
