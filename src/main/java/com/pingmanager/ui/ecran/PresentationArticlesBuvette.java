package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.CalculCoutBuvette;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.CoutPortion;
import com.pingmanager.core.buvette.ServiceArticlesBuvette;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Logique d'affichage de la liste et de la fiche des articles du club
 * (B026), testée hors JavaFX : articles en vente par rayon, articles
 * retirés de la vente, coûts et marge d'une portion.
 */
final class PresentationArticlesBuvette {

    static final String AUCUN_ARTICLE = "Votre club n'a pas encore d'article. Le plus simple : choisissez dans le "
            + "catalogue de base ce que vous vendez, puis adaptez prix et coûts. Vous pouvez aussi créer un article.";

    /**
     * Ligne de la liste.
     *
     * @param id          article
     * @param nom         nom
     * @param detail      composition, coût, prix et marge (produit) ou lot et usage (contenant)
     * @param origine     « Du catalogue » ou « Créé par le club »
     * @param prixAFixer  vrai pour un produit sans prix de vente
     * @param supprimable vrai si l'article n'a aucun historique (sinon : archiver)
     */
    record Ligne(String id, String nom, String detail, String origine, boolean prixAFixer, boolean supprimable) {
    }

    /** Rayon de la liste. */
    record Rayon(String titre, List<Ligne> lignes) {
    }

    private final StockBuvette stock;

    PresentationArticlesBuvette(StockBuvette stock) {
        this.stock = stock;
    }

    boolean vide() {
        return stock.getArticles().isEmpty();
    }

    /** Articles en vente, par rayon (rayons vides omis). */
    List<Rayon> rayons() {
        List<Rayon> res = new ArrayList<>();
        List<ArticleBuvette> actifs = new ArrayList<>(stock.articlesActifs(FamilleArticle.PRODUIT));
        actifs.addAll(stock.articlesActifs(FamilleArticle.CONTENANT));
        for (RayonBuvette r : RayonBuvette.values()) {
            List<Ligne> lignes = new ArrayList<>();
            for (ArticleBuvette a : actifs) {
                if (a.getRayon() == r) {
                    lignes.add(ligne(a));
                }
            }
            if (!lignes.isEmpty()) {
                res.add(new Rayon(r.getLibelle(), lignes));
            }
        }
        return res;
    }

    /** Articles retirés de la vente. */
    List<Ligne> archives() {
        List<Ligne> res = new ArrayList<>();
        for (ArticleBuvette a : stock.articlesArchives()) {
            res.add(ligne(a));
        }
        return res;
    }

    Ligne ligne(ArticleBuvette a) {
        List<String> p = new ArrayList<>();
        if (a.estProduit()) {
            p.add(composition(a));
            Optional<CoutPortion> c = CalculCoutBuvette.portion(stock, a);
            c.ifPresent(cp -> p.add("coût " + Centimes.eurosPrecis(cp.complet())));
            if (a.getPrixVenteCentimes() > 0) {
                p.add("vendu " + Centimes.euros(a.getPrixVenteCentimes()));
                c.ifPresent(cp -> p.add("marge " + Centimes.eurosPrecis(marge(a, cp))));
            }
        } else {
            p.add(Centimes.euros(a.getPrixLotCentimes()) + " les " + a.getUnitesParLot() + " ("
                    + Centimes.eurosPrecis(CalculCoutBuvette.coutUnitaire(a)) + " l'unité)");
            List<String> n = new ArrayList<>();
            stock.produitsUtilisant(a.getId()).forEach(u -> n.add(u.getNom()));
            if (!n.isEmpty()) {
                p.add("avec " + String.join(", ", n));
            }
        }
        return new Ligne(a.getId(), a.getNom(), String.join(" · ", p),
                a.getIdCatalogue().isEmpty() ? "Créé par le club" : "Du catalogue",
                a.estProduit() && a.getPrixVenteCentimes() == 0, !ServiceArticlesBuvette.aUnHistorique(stock, a.getId()));
    }

    /** « 1 dose + gobelet, touillette » (« 2 × serviette » si plus d'une). */
    String composition(ArticleBuvette produit) {
        List<String> res = new ArrayList<>();
        for (LienContenant l : produit.getContenants()) {
            stock.article(l.idContenant()).ifPresent(c -> res.add((l.quantite() > 1 ? l.quantite() + " × " : "")
                    + c.getNom().toLowerCase()));
        }
        return "1 " + produit.getUnite() + (res.isEmpty() ? "" : " + " + String.join(", ", res));
    }

    /** Marge d'une portion (prix de vente − coût complet), en centimes exacts. */
    static BigDecimal marge(ArticleBuvette a, CoutPortion c) {
        return BigDecimal.valueOf(a.getPrixVenteCentimes()).subtract(c.complet());
    }

    /**
     * Résumé des coûts dans la fiche : « Produit 0,096 € + emballage 0,068 € = 0,164 € la portion ·
     * vendu 1,00 € · marge 0,836 € ».
     */
    String resumeCouts(ArticleBuvette a) {
        Optional<CoutPortion> c = CalculCoutBuvette.portion(stock, a);
        if (c.isEmpty()) {
            return "Coût d'une unité : " + Centimes.eurosPrecis(CalculCoutBuvette.coutUnitaire(a)) + ".";
        }
        String s = "Produit brut " + Centimes.eurosPrecis(c.get().brut()) + " + emballage "
                + Centimes.eurosPrecis(c.get().emballage()) + " = portion complète "
                + Centimes.eurosPrecis(c.get().complet());
        if (a.getPrixVenteCentimes() > 0) {
            s += " · vendu " + Centimes.euros(a.getPrixVenteCentimes()) + " · marge "
                    + Centimes.eurosPrecis(marge(a, c.get()));
        }
        return s + ".";
    }
}
