package com.pingmanager.core;

/**
 * Les six étapes d'un tableau le jour J, dans l'ordre de la frise de l'écran
 * du juge-arbitre. Calculées par {@link CalculEtapeTableau} à partir de l'état
 * réel du tableau ; l'interface ne fait que les afficher.
 */
public enum EtapeTableau {
    AVANT_POINTAGE(1, "Avant pointage", "Avant"),
    POINTAGE_OUVERT(2, "Pointage ouvert", "Pointage"),
    POINTAGE_FERME(3, "Pointage fermé", "Fermé"),
    POULES(4, "Phase de poules", "Poules"),
    TABLEAU_FINAL(5, "Élimination directe", "Élim."),
    TERMINE(6, "Tableau terminé", "Terminé");

    private final int numero;
    private final String libelle;
    private final String libelleCourt;

    EtapeTableau(int numero, String libelle, String libelleCourt) {
        this.numero = numero;
        this.libelle = libelle;
        this.libelleCourt = libelleCourt;
    }

    /** Rang dans la frise, de 1 à 6. */
    public int numero() {
        return numero;
    }

    /** Libellé complet : « Pointage ouvert ». */
    public String libelle() {
        return libelle;
    }

    /** Libellé court, pour une frise étroite : « Pointage ». */
    public String libelleCourt() {
        return libelleCourt;
    }

    /** Cette étape est-elle déjà passée quand le tableau est à l'étape {@code courante} ? */
    public boolean estAvant(EtapeTableau courante) {
        return numero < courante.numero;
    }
}
