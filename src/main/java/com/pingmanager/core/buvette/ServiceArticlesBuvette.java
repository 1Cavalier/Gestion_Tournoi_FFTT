package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Création, modification et retrait des articles de la buvette (B026), avec
 * contrôle de la saisie. Ne connaît ni l'écran ni l'enregistrement.
 */
public final class ServiceArticlesBuvette {

    /** Stock, seuil et unités par lot maximaux acceptés à la saisie. */
    public static final long QUANTITE_MAX = 1_000_000;

    /**
     * Ajoute (id {@code null}) ou modifie un article. En modification, le stock
     * n'est pas repris de la saisie : il ne change que par entrée, sortie ou
     * recomptage (pour que chaque changement soit dans le journal).
     */
    public ResultatArticleBuvette enregistrer(StockBuvette stock, SaisieArticle s) {
        ArticleBuvette existant = null;
        if (s.id() != null) {
            existant = stock.article(s.id()).orElse(null);
            if (existant == null) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.INCONNU);
            }
        }
        String nom = s.nom() == null ? "" : s.nom().trim().replaceAll("\\s+", " ");
        if (nom.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.NOM_OBLIGATOIRE);
        }
        for (ArticleBuvette a : stock.getArticles()) {
            if (!a.getId().equals(s.id()) && cle(a.getNom()).equals(cle(nom))) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.NOM_EN_DOUBLE, a.getNom());
            }
        }
        FamilleArticle famille = s.famille() == null ? FamilleArticle.PRODUIT : s.famille();
        if (existant != null && famille == FamilleArticle.PRODUIT && !existant.estProduit()) {
            List<ArticleBuvette> utilisateurs = stock.produitsUtilisant(existant.getId());
            if (!utilisateurs.isEmpty()) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.FAMILLE_VERROUILLEE,
                        noms(utilisateurs));
            }
        }
        OptionalLong prix = vide(s.prixLot()) ? OptionalLong.of(0) : Centimes.lire(s.prixLot());
        if (prix.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.PRIX_INVALIDE);
        }
        OptionalLong lot = entier(s.unitesParLot(), 1);
        if (lot.isEmpty() || lot.getAsLong() < 1) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.LOT_INVALIDE);
        }
        long quantiteStock;
        if (existant != null) {
            quantiteStock = existant.getStock();
        } else {
            OptionalLong q = entier(s.stock(), 0);
            if (q.isEmpty()) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.STOCK_INVALIDE);
            }
            quantiteStock = q.getAsLong();
        }
        OptionalLong seuil = entier(s.seuil(), 0);
        if (seuil.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.SEUIL_INVALIDE);
        }
        List<LienContenant> liens = new ArrayList<>();
        if (famille == FamilleArticle.PRODUIT) {
            for (Map.Entry<String, Integer> e : s.contenants().entrySet()) {
                Optional<ArticleBuvette> c = stock.article(e.getKey());
                if (c.isEmpty() || c.get().estProduit() || e.getValue() == null || e.getValue() < 1) {
                    return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.CONTENANT_INVALIDE);
                }
            }
            // Ordre stable : celui des contenants dans le stock.
            for (ArticleBuvette c : stock.articles(FamilleArticle.CONTENANT)) {
                Integer q = s.contenants().get(c.getId());
                if (q != null) {
                    liens.add(new LienContenant(c.getId(), q));
                }
            }
        }
        ArticleBuvette a = ArticleBuvette.builder()
                .id(existant == null ? stock.idLibre(nom) : existant.getId())
                .nom(nom)
                .famille(famille)
                .unite(s.unite())
                .lot(prix.getAsLong(), (int) lot.getAsLong())
                .stock(quantiteStock)
                .seuilAlerte(seuil.getAsLong())
                .contenants(liens)
                .build();
        stock.enregistrer(a);
        return new ResultatArticleBuvette(existant == null ? ResultatArticleBuvette.Motif.AJOUTE
                : ResultatArticleBuvette.Motif.MODIFIE, a, nom);
    }

    /** Retire un article ; refusé pour un contenant qui sert encore à un produit. */
    public ResultatArticleBuvette retirer(StockBuvette stock, String id) {
        Optional<ArticleBuvette> a = stock.article(id);
        if (a.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.INCONNU);
        }
        List<ArticleBuvette> utilisateurs = stock.produitsUtilisant(id);
        if (!utilisateurs.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.CONTENANT_UTILISE, noms(utilisateurs));
        }
        stock.retirer(id);
        return new ResultatArticleBuvette(ResultatArticleBuvette.Motif.RETIRE, null, a.get().getNom());
    }

    private static String noms(List<ArticleBuvette> articles) {
        List<String> n = new ArrayList<>();
        articles.forEach(a -> n.add(a.getNom()));
        return String.join(", ", n);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    /** Entier positif ou nul (espaces tolérés), {@code defaut} si vide ; vide si illisible. */
    static OptionalLong entier(String saisie, long defaut) {
        if (vide(saisie)) {
            return OptionalLong.of(defaut);
        }
        String t = saisie.replace(" ", "").replace(" ", "").trim();
        if (!t.matches("\\d{1,7}")) {
            return OptionalLong.empty();
        }
        long v = Long.parseLong(t);
        return v > QUANTITE_MAX ? OptionalLong.empty() : OptionalLong.of(v);
    }

    /** Clé de comparaison des noms : sans accents, casse ni ponctuation. */
    static String cle(String nom) {
        return StockBuvette.slug(nom);
    }
}
