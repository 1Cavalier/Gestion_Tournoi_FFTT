package com.pingmanager.model.buvette;

import java.util.List;
import java.util.Objects;

/**
 * Article du catalogue de base de la buvette (B026), fourni avec
 * l'application en lecture seule. Les coûts et prix sont <b>indicatifs, à
 * adapter</b> : le club en fait une copie ({@link ArticleBuvette}) qu'il
 * modifie librement, sans jamais toucher au catalogue.
 *
 * @param id                identifiant stable dans le catalogue (« cafe »)
 * @param nom               nom proposé
 * @param rayon             rayon (sa famille s'en déduit)
 * @param unite             unité comptée en stock (« dose », « gobelet »)
 * @param prixLotCentimes   prix indicatif d'un lot, en centimes
 * @param unitesParLot      nombre d'unités dans ce lot
 * @param prixVenteCentimes prix de vente indicatif d'une portion (0 pour un contenant)
 * @param seuilAlerte       seuil d'alerte proposé
 * @param composition       contenants utilisés par portion (identifiants du catalogue)
 */
public record ArticleCatalogue(String id, String nom, RayonBuvette rayon, String unite, long prixLotCentimes,
                               int unitesParLot, long prixVenteCentimes, long seuilAlerte,
                               List<LienContenant> composition) {

    public ArticleCatalogue {
        if (id == null || id.isBlank() || nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("identifiant et nom obligatoires");
        }
        Objects.requireNonNull(rayon, "rayon");
        if (unite == null || unite.isBlank()) {
            throw new IllegalArgumentException("unité obligatoire");
        }
        if (prixLotCentimes < 0 || unitesParLot < 1 || prixVenteCentimes < 0 || seuilAlerte < 0) {
            throw new IllegalArgumentException("montants et quantités positifs");
        }
        composition = rayon.famille() == FamilleArticle.PRODUIT && composition != null
                ? List.copyOf(composition) : List.of();
    }

    public FamilleArticle famille() {
        return rayon.famille();
    }

    public boolean estProduit() {
        return famille() == FamilleArticle.PRODUIT;
    }
}
