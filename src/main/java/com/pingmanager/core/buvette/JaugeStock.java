package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.StockBuvette;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Jauge du stock d'un article de la buvette (B026) : niveau (vert, orange,
 * rouge) et remplissage entre 0 et 1.
 *
 * <p>La jauge est pleine à {@value #MULTIPLE_PLEIN} fois le seuil d'alerte
 * (ou dès qu'il reste du stock si aucun seuil n'est fixé).</p>
 *
 * @param article     l'article
 * @param niveau      suffisant, bas ou à racheter
 * @param remplissage part de la jauge remplie, entre 0 et 1
 */
public record JaugeStock(ArticleBuvette article, NiveauStock niveau, double remplissage) {

    /** La jauge est pleine à ce multiple du seuil d'alerte. */
    public static final int MULTIPLE_PLEIN = 3;

    /** Jauge d'un article. */
    public static JaugeStock de(ArticleBuvette a) {
        long stock = a.getStock();
        long seuil = a.getSeuilAlerte();
        double remplissage;
        if (seuil <= 0) {
            remplissage = stock > 0 ? 1.0 : 0.0;
        } else {
            remplissage = Math.min(1.0, (double) stock / (MULTIPLE_PLEIN * seuil));
        }
        return new JaugeStock(a, NiveauStock.de(stock, seuil), remplissage);
    }

    /**
     * Liste « À racheter » : les articles au niveau {@link NiveauStock#URGENT},
     * les plus vides d'abord (stock rapporté au seuil), puis par nom.
     */
    public static List<JaugeStock> aRacheter(StockBuvette stock) {
        List<JaugeStock> res = new ArrayList<>();
        for (ArticleBuvette a : stock.getArticles()) {
            JaugeStock j = de(a);
            if (j.niveau() == NiveauStock.URGENT) {
                res.add(j);
            }
        }
        res.sort(Comparator.comparingDouble(JaugeStock::ratio).thenComparing(j -> j.article().getNom()));
        return res;
    }

    /** Stock rapporté au seuil (0 si vide). */
    private double ratio() {
        long seuil = article.getSeuilAlerte();
        return seuil <= 0 ? article.getStock() : (double) article.getStock() / seuil;
    }
}
