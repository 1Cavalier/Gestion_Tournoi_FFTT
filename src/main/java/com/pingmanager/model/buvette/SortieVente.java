package com.pingmanager.model.buvette;

/**
 * Quantité réellement sortie du stock par une vente (B026), pour un article
 * (le produit ou l'un de ses contenants). L'annulation de la vente remet
 * exactement ces quantités : si le stock manquait au moment de la vente, il
 * n'est pas gonflé à l'annulation.
 *
 * @param idArticle article sorti
 * @param quantite  unités réellement sorties (0 si l'article était déjà épuisé)
 */
public record SortieVente(String idArticle, long quantite) {

    public SortieVente {
        if (idArticle == null || idArticle.isBlank()) {
            throw new IllegalArgumentException("article obligatoire");
        }
        if (quantite < 0) {
            throw new IllegalArgumentException("quantité positive ou nulle");
        }
    }
}
