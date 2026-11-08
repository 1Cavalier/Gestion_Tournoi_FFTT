package com.pingmanager.core.feuilles;

/**
 * Pourquoi une feuille ne peut pas être préparée. Le libellé est porté par la
 * constante (pas de {@code switch} sur cette énumération).
 */
public enum MotifRefusFeuille {

    /** Le match est un exempt (bye) : il n'y a pas de partie à jouer. */
    MATCH_EXEMPT("Ce match est un exempt : il n'y a pas de partie à jouer, donc pas de feuille."),
    /** Un des joueurs n'est pas encore connu (tour précédent non joué). */
    JOUEURS_A_DETERMINER("Les deux joueurs ne sont pas encore connus : le tour précédent n'est pas joué."),
    /** Le match n'appartient à aucun tableau du tournoi. */
    MATCH_INTROUVABLE("Ce match n'appartient à aucun tableau du tournoi."),
    /** La poule n'a aucune partie (tableau non lancé). */
    POULE_SANS_PARTIE("Cette poule n'a pas encore de parties : le tableau n'est pas lancé."),
    /** Le tableau final n'est pas construit. */
    PAS_DE_TABLEAU_FINAL("Le tableau final n'est pas encore construit (second tirage au sort).");

    private final String libelle;

    MotifRefusFeuille(String libelle) {
        this.libelle = libelle;
    }

    /** Explication en français, à afficher telle quelle. */
    public String getLibelle() {
        return libelle;
    }
}
