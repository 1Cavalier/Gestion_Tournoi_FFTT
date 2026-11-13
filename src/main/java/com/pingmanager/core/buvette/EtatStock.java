package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.Ingredient;

/**
 * État du stock d'un ingrédient pendant un tournoi (B046), en portions.
 *
 * @param ingredient  l'ingrédient (ou un ingrédient « inconnu » s'il a disparu du catalogue)
 * @param depart      stock de départ et reports
 * @param achats      portions achetées
 * @param ventes      portions vendues (positif)
 * @param pertesDons  portions perdues ou données (positif)
 * @param ecartsInventaire écarts d'inventaire et corrections, signés
 * @param restant     stock théorique restant
 * @param seuil       seuil d'alerte, ou -1
 * @param achatsPayesCentimes montant payé pour les achats
 */
public record EtatStock(Ingredient ingredient, long depart, long achats, long ventes, long pertesDons,
                        long ecartsInventaire, long restant, long seuil, long achatsPayesCentimes) {

    /** @return vrai si un seuil est fixé et que le stock restant est en dessous. */
    public boolean sousLeSeuil() {
        return seuil >= 0 && restant < seuil;
    }

    /** Équivalent en unités d'achat entières (rendement minimal), arrondi à l'inférieur. */
    public long unitesAchatEntieres() {
        return restant <= 0 ? 0 : restant / ingredient.getRendementMin();
    }
}
