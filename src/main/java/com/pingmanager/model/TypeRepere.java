package com.pingmanager.model;

/**
 * Repère posé sur le plan d'une salle (B024, cahier des charges v0, section 6) :
 * table d'arbitrage, entrée, sortie, WC… Chaque type a une taille par défaut sur
 * la grille (en cases) et un sigle court pour les petites cases. Pas de
 * {@code switch} sur cette énumération (voir CLAUDE.md).
 */
public enum TypeRepere {
    TABLE_ARBITRAGE("Table d'arbitrage", "JA", 2, 1),
    TABLE_DE_MARQUE("Table de marque", "M", 2, 1),
    ENTREE("Entrée", "E", 1, 1),
    SORTIE("Sortie", "S", 1, 1),
    SORTIE_SECOURS("Sortie de secours", "SS", 1, 1),
    WC("WC", "WC", 1, 1),
    VESTIAIRES("Vestiaires", "V", 2, 1),
    BUVETTE("Buvette", "B", 2, 1),
    ESCALIER("Escalier", "Esc", 1, 1),
    ECHAUFFEMENT("Échauffement", "Éch", 2, 2),
    PUBLIC("Public", "P", 2, 1),
    AUTRE("Autre", "?", 1, 1);

    private final String libelle;
    private final String sigle;
    private final int largeurParDefaut;
    private final int hauteurParDefaut;

    TypeRepere(String libelle, String sigle, int largeurParDefaut, int hauteurParDefaut) {
        this.libelle = libelle;
        this.sigle = sigle;
        this.largeurParDefaut = largeurParDefaut;
        this.hauteurParDefaut = hauteurParDefaut;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getSigle() {
        return sigle;
    }

    public int getLargeurParDefaut() {
        return largeurParDefaut;
    }

    public int getHauteurParDefaut() {
        return hauteurParDefaut;
    }
}
