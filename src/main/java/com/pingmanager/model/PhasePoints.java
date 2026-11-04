package com.pingmanager.model;

/** Phase de la saison dont les points classement servent aux inscriptions. */
public enum PhasePoints {
    PREMIERE("1re phase"),
    DEUXIEME("2e phase");

    private final String libelle;

    PhasePoints(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
