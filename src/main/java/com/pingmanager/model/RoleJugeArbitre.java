package com.pingmanager.model;

/** Rôle d'un juge-arbitre dans un tournoi. */
public enum RoleJugeArbitre {

    /** Le juge-arbitre désigné du tournoi (IX.104, IX.109) : un seul. */
    PRINCIPAL("Juge-arbitre principal"),
    /** Juge-arbitre adjoint : l'assiste, sans exigence de grade propre. */
    ADJOINT("Juge-arbitre adjoint");

    private final String libelle;

    RoleJugeArbitre(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
