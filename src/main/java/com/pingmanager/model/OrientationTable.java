package com.pingmanager.model;

/**
 * Orientation d'une table sur le plan de la salle (B024) : sens du filet vu
 * d'en haut. Une table occupe toujours 2 × 2 cases ; l'orientation ne sert qu'au
 * dessin du plan. Pas de {@code switch} sur cette énumération (voir CLAUDE.md).
 */
public enum OrientationTable {
    /** Les joueurs se font face de gauche à droite : filet vertical sur le plan. */
    HORIZONTALE("Horizontale"),
    /** Les joueurs se font face de haut en bas : filet horizontal sur le plan. */
    VERTICALE("Verticale");

    private final String libelle;

    OrientationTable(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return l'autre orientation (bouton « Tourner »). */
    public OrientationTable tournee() {
        return this == HORIZONTALE ? VERTICALE : HORIZONTALE;
    }
}
