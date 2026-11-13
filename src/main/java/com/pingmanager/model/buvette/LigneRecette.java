package com.pingmanager.model.buvette;

/**
 * Ligne de recette d'un produit (B046) : combien de portions d'un ingrédient
 * sont consommées par produit vendu (« 1 sandwich = 1 portion de baguette,
 * 1 tranche de jambon, 1 noisette de beurre »).
 *
 * @param idIngredient identifiant de l'ingrédient du catalogue
 * @param quantite     nombre de portions consommées (au moins 1)
 */
public record LigneRecette(String idIngredient, int quantite) {

    public LigneRecette {
        if (idIngredient == null || idIngredient.isBlank()) {
            throw new IllegalArgumentException("ingrédient obligatoire");
        }
        if (quantite < 1) {
            throw new IllegalArgumentException("quantité au moins 1");
        }
        idIngredient = idIngredient.trim();
    }
}
