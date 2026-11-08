package com.pingmanager.core;

/** Raison pour laquelle aucun export de résultats ne peut être produit. */
public enum MotifRefusExport {
    /** Le tableau demandé n'appartient pas au tournoi. */
    TABLEAU_INCONNU("Ce tableau n'appartient pas au tournoi."),
    /** Le tableau demandé n'est pas terminé (finale ou petite finale à jouer). */
    TABLEAU_NON_TERMINE("Le tableau n'est pas terminé : finale (et petite finale) à jouer avant l'export."),
    /** Aucun tableau du tournoi n'est terminé. */
    AUCUN_TABLEAU_TERMINE("Aucun tableau n'est terminé : rien à exporter.");

    private final String libelle;

    MotifRefusExport(String libelle) {
        this.libelle = libelle;
    }

    /** @return message destiné à l'utilisateur. */
    public String getLibelle() {
        return libelle;
    }
}
