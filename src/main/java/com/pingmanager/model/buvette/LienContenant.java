package com.pingmanager.model.buvette;

/**
 * Contenant ou consommable utilisé à chaque portion d'un produit (B026) :
 * « un café = un gobelet + une touillette ».
 *
 * @param idContenant identifiant de l'article contenant
 * @param quantite    nombre d'unités utilisées par portion (au moins 1)
 */
public record LienContenant(String idContenant, int quantite) {

    public LienContenant {
        if (idContenant == null || idContenant.isBlank()) {
            throw new IllegalArgumentException("contenant obligatoire");
        }
        if (quantite < 1) {
            throw new IllegalArgumentException("quantité au moins 1");
        }
        idContenant = idContenant.trim();
    }
}
