package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.ArticleCatalogue;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Logique d'affichage du catalogue de base à cocher (B026), testée hors
 * JavaFX : articles par rayon, composition, coûts et prix indicatifs (« à
 * adapter ») et repère « déjà dans vos articles ».
 */
final class PresentationCatalogueBuvette {

    static final String AVERTISSEMENT = "Coûts et prix indicatifs, à adapter : ce sont des ordres de grandeur. "
            + "Chaque article coché devient une copie que vous modifiez librement ; le catalogue ne change pas.";

    /**
     * Ligne du catalogue.
     *
     * @param id          article du catalogue
     * @param nom         nom proposé
     * @param detail      composition, coût et prix indicatifs
     * @param dejaAuClub  vrai si le club l'a déjà (copie ou même nom)
     */
    record Ligne(String id, String nom, String detail, boolean dejaAuClub) {
    }

    /** Rayon du catalogue. */
    record Rayon(RayonBuvette rayon, String titre, List<Ligne> lignes) {
    }

    private final CatalogueBuvette catalogue;
    private final StockBuvette stock;

    PresentationCatalogueBuvette(CatalogueBuvette catalogue, StockBuvette stock) {
        this.catalogue = catalogue;
        this.stock = stock;
    }

    List<Rayon> rayons() {
        List<Rayon> res = new ArrayList<>();
        for (RayonBuvette r : RayonBuvette.values()) {
            List<Ligne> lignes = new ArrayList<>();
            for (ArticleCatalogue a : catalogue.rayon(r)) {
                lignes.add(new Ligne(a.id(), a.nom(), detail(a), dejaAuClub(a)));
            }
            if (!lignes.isEmpty()) {
                res.add(new Rayon(r, r.getLibelle(), lignes));
            }
        }
        return res;
    }

    boolean dejaAuClub(ArticleCatalogue c) {
        for (ArticleBuvette a : stock.getArticles()) {
            if (a.getIdCatalogue().equals(c.id()) || (cle(a.getNom()).equals(cle(c.nom()))
                    && a.getFamille() == c.famille())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Produit : « Avec gobelet carton, touillette, sucre · coût indicatif 0,096 € + emballage 0,068 €
     * · prix conseillé 1,00 € » ; contenant : « 4,90 € les 100 · 0,049 € l'unité ».
     */
    String detail(ArticleCatalogue a) {
        BigDecimal brut = unitaire(a);
        if (!a.estProduit()) {
            return Centimes.euros(a.prixLotCentimes()) + " les " + a.unitesParLot() + " · "
                    + Centimes.eurosPrecis(brut) + " l'unité (indicatif)";
        }
        BigDecimal emballage = BigDecimal.ZERO;
        List<String> noms = new ArrayList<>();
        for (LienContenant l : a.composition()) {
            ArticleCatalogue c = catalogue.article(l.idContenant()).orElseThrow();
            emballage = emballage.add(unitaire(c).multiply(BigDecimal.valueOf(l.quantite())));
            noms.add((l.quantite() > 1 ? l.quantite() + " × " : "") + c.nom().toLowerCase(Locale.FRENCH));
        }
        List<String> p = new ArrayList<>();
        p.add("1 " + a.unite());
        if (!noms.isEmpty()) {
            p.set(0, p.get(0) + " + " + String.join(" + ", noms));
        }
        String cout = "coût indicatif " + Centimes.eurosPrecis(brut);
        if (emballage.signum() > 0) {
            cout += " + emballage " + Centimes.eurosPrecis(emballage) + " = " + Centimes.eurosPrecis(brut.add(emballage));
        }
        p.add(cout);
        p.add("prix conseillé " + Centimes.euros(a.prixVenteCentimes()));
        return String.join(" · ", p);
    }

    private static BigDecimal unitaire(ArticleCatalogue a) {
        return BigDecimal.valueOf(a.prixLotCentimes()).divide(BigDecimal.valueOf(a.unitesParLot()), 6,
                RoundingMode.HALF_UP);
    }

    private static String cle(String nom) {
        return StockBuvette.slug(nom);
    }
}
