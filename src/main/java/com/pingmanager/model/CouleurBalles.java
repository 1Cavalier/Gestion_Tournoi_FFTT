package com.pingmanager.model;

/** Couleur des balles (règles du jeu 2.3.3 : blanche ou orange, et mate). */
public enum CouleurBalles {
    BLANCHE("Blanche"),
    ORANGE("Orange");

    private final String libelle;

    CouleurBalles(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
