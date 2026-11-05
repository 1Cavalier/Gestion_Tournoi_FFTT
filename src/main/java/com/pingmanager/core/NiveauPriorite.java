package com.pingmanager.core;

/**
 * Niveau de priorité d'un match proposé dans la salle, affiché en couleur sur
 * l'écran du jour J (cahier des charges v0, section 7) : rouge, orange, vert.
 * Calculé par {@link ReglePrioriteSalle}.
 *
 * <p>Pas de {@code switch} sur cette énumération : le libellé est porté par la
 * constante.</p>
 */
public enum NiveauPriorite {

    /** Rouge : à lancer tout de suite (attente longue). */
    URGENT("Urgent"),
    /** Orange : à lancer bientôt (match important, attente qui s'allonge, prochain selon la stratégie). */
    IMPORTANT("Important"),
    /** Vert : peut attendre. */
    NORMAL("Normal");

    private final String libelle;

    NiveauPriorite(String libelle) {
        this.libelle = libelle;
    }

    /** « Urgent », « Important », « Normal ». */
    public String getLibelle() {
        return libelle;
    }
}
