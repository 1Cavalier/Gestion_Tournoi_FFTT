package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Coûts de la buvette (B026) : coût unitaire d'un article (prix du lot divisé
 * par le nombre d'unités du lot) et coût d'une portion d'un produit, séparé en
 * coût brut, coût de l'emballage et coût complet.
 *
 * <p>Calculs en centimes exacts ({@link BigDecimal}, 6 décimales) : l'arrondi
 * n'intervient qu'à l'affichage ({@link Centimes#eurosPrecis(BigDecimal)}).</p>
 */
public final class CalculCoutBuvette {

    /** Décimales gardées sur les centimes (1 millionième de centime). */
    static final int ECHELLE = 6;

    private CalculCoutBuvette() {
    }

    /** Coût d'une unité de l'article, en centimes exacts. */
    public static BigDecimal coutUnitaire(ArticleBuvette a) {
        return BigDecimal.valueOf(a.getPrixLotCentimes())
                .divide(BigDecimal.valueOf(a.getUnitesParLot()), ECHELLE, RoundingMode.HALF_UP);
    }

    /**
     * Coût d'une portion d'un produit : son coût unitaire (brut) et celui de ses
     * contenants, multipliés par leur quantité (emballage). Un lien vers un
     * article absent du stock est ignoré.
     *
     * @return vide si l'article n'est pas un produit
     */
    public static Optional<CoutPortion> portion(StockBuvette stock, ArticleBuvette produit) {
        if (!produit.estProduit()) {
            return Optional.empty();
        }
        BigDecimal emballage = BigDecimal.ZERO.setScale(ECHELLE);
        for (LienContenant l : produit.getContenants()) {
            Optional<ArticleBuvette> c = stock.article(l.idContenant());
            if (c.isPresent()) {
                emballage = emballage.add(coutUnitaire(c.get()).multiply(BigDecimal.valueOf(l.quantite())));
            }
        }
        return Optional.of(new CoutPortion(coutUnitaire(produit), emballage));
    }
}
