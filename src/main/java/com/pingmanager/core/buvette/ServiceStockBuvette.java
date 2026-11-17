package com.pingmanager.core.buvette;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.NatureMouvement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.StockBuvette;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Mises à jour du stock de la buvette (B026) : entrée (achat, réassort),
 * sortie et recomptage. Sortir une portion d'un produit sort aussi ses
 * contenants (un café sort une dose, un gobelet et une touillette).
 *
 * <p>Le stock ne devient jamais négatif : s'il manque des unités, l'article
 * est ramené à 0, signalé « à recompter » ({@link StockBuvette#estARecompter})
 * et le résultat demande de recompter (le comptage réel était faux ; refuser
 * la sortie bloquerait le bénévole au comptoir). Le recomptage lève le
 * signalement.</p>
 *
 * <p>Point d'entrée unique des sorties : le jour où un joueur commandera
 * depuis l'application, le retrait de sa commande appellera
 * {@link #sortir(StockBuvette, String, long, OrigineMouvement, String, String)}
 * avec son origine et sa référence.</p>
 */
public final class ServiceStockBuvette {

    private final Horloge horloge;

    public ServiceStockBuvette(Horloge horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    /** Entrée de {@code quantite} unités d'un article (seul l'article bouge, jamais ses contenants). */
    public ResultatMouvementStock entrer(StockBuvette stock, String idArticle, long quantite, String auteur) {
        Optional<ArticleBuvette> a = stock.article(idArticle);
        if (a.isEmpty()) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.INCONNU);
        }
        if (quantite < 1 || quantite > ServiceArticlesBuvette.QUANTITE_MAX) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.QUANTITE_INVALIDE);
        }
        int op = stock.prochaineOperation();
        MouvementStock m = appliquer(stock, a.get(), NatureMouvement.ENTREE, quantite, op, horloge.maintenant(),
                OrigineMouvement.SAISIE_MANUELLE, "", auteur);
        return new ResultatMouvementStock(ResultatMouvementStock.Motif.ENREGISTRE, List.of(m),
                a.get().getNom() + " : +" + quantite + " (" + m.stockApres() + " en stock).", List.of());
    }

    /** Sortie saisie à la main au comptoir. */
    public ResultatMouvementStock sortir(StockBuvette stock, String idArticle, long quantite, String auteur) {
        return sortir(stock, idArticle, quantite, OrigineMouvement.SAISIE_MANUELLE, "", auteur);
    }

    /**
     * Sortie de {@code quantite} unités d'un article. Pour un produit, chaque
     * portion sort aussi ses contenants (quantité par portion × portions) ; un
     * lien vers un contenant disparu est ignoré.
     *
     * @param origine   d'où vient la sortie
     * @param reference référence de l'origine (vide pour une saisie à la main)
     */
    public ResultatMouvementStock sortir(StockBuvette stock, String idArticle, long quantite,
                                         OrigineMouvement origine, String reference, String auteur) {
        Optional<ArticleBuvette> a = stock.article(idArticle);
        if (a.isEmpty()) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.INCONNU);
        }
        if (quantite < 1 || quantite > ServiceArticlesBuvette.QUANTITE_MAX) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.QUANTITE_INVALIDE);
        }
        Objects.requireNonNull(origine, "origine");
        int op = stock.prochaineOperation();
        Instant t = horloge.maintenant();
        List<MouvementStock> mouvements = new ArrayList<>();
        List<String> epuises = new ArrayList<>();
        ArticleBuvette produit = a.get();
        mouvements.add(sortie(stock, produit, quantite, op, t, origine, reference, auteur, epuises));
        for (LienContenant l : produit.getContenants()) {
            Optional<ArticleBuvette> c = stock.article(l.idContenant());
            if (c.isPresent()) {
                mouvements.add(sortie(stock, c.get(), quantite * l.quantite(), op, t, origine, reference, auteur,
                        epuises));
            }
        }
        String unite = produit.estProduit() ? (quantite > 1 ? " portions sorties" : " portion sortie")
                : (quantite > 1 ? " sortis" : " sorti");
        String message = produit.getNom() + " : " + quantite + unite
                + (mouvements.size() > 1 ? ", avec " + (mouvements.size() - 1)
                + (mouvements.size() > 2 ? " contenants." : " contenant.") : ".");
        return new ResultatMouvementStock(ResultatMouvementStock.Motif.ENREGISTRE, mouvements, message, epuises);
    }

    private static MouvementStock sortie(StockBuvette stock, ArticleBuvette a, long quantite, int op, Instant t,
                                         OrigineMouvement origine, String reference, String auteur,
                                         List<String> epuises) {
        ArticleBuvette actuel = stock.article(a.getId()).orElse(a); // un contenant cité deux fois
        if (actuel.getStock() < quantite) {
            epuises.add(actuel.getNom());
            stock.marquerARecompter(actuel.getId());
        }
        return appliquer(stock, actuel, NatureMouvement.SORTIE, -Math.min(quantite, actuel.getStock()), op, t,
                origine, reference, auteur);
    }

    /** Recomptage : le stock devient {@code compte} ; la différence est journalisée. */
    public ResultatMouvementStock recompter(StockBuvette stock, String idArticle, long compte, String auteur) {
        Optional<ArticleBuvette> a = stock.article(idArticle);
        if (a.isEmpty()) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.INCONNU);
        }
        if (compte < 0 || compte > ServiceArticlesBuvette.QUANTITE_MAX) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.COMPTE_INVALIDE);
        }
        long ecart = compte - a.get().getStock();
        stock.recompte(idArticle);
        MouvementStock m = appliquer(stock, a.get(), NatureMouvement.RECOMPTAGE, ecart, stock.prochaineOperation(),
                horloge.maintenant(), OrigineMouvement.SAISIE_MANUELLE, "", auteur);
        String detail = ecart == 0 ? "aucun écart" : "écart de " + (ecart > 0 ? "+" : "") + ecart;
        return new ResultatMouvementStock(ResultatMouvementStock.Motif.ENREGISTRE, List.of(m),
                a.get().getNom() + " recompté : " + compte + " en stock (" + detail + ").", List.of());
    }

    /**
     * Remet en stock des quantités sorties auparavant (annulation d'une vente),
     * en une seule opération du journal. Une quantité nulle ou un article
     * disparu est ignoré.
     *
     * @return les lignes ajoutées au journal
     */
    List<MouvementStock> remettre(StockBuvette stock, List<com.pingmanager.model.buvette.SortieVente> sorties,
                                  OrigineMouvement origine, String reference, String auteur) {
        int op = stock.prochaineOperation();
        Instant t = horloge.maintenant();
        List<MouvementStock> res = new ArrayList<>();
        for (com.pingmanager.model.buvette.SortieVente sv : sorties) {
            Optional<ArticleBuvette> a = stock.article(sv.idArticle());
            if (a.isPresent() && sv.quantite() > 0) {
                res.add(appliquer(stock, a.get(), NatureMouvement.ENTREE, sv.quantite(), op, t, origine, reference,
                        auteur));
            }
        }
        return res;
    }

    /** Recomptage saisi à l'écran (« 12 »). */
    public ResultatMouvementStock recompter(StockBuvette stock, String idArticle, String saisie, String auteur) {
        var compte = ServiceArticlesBuvette.entier(saisie, -1);
        if (compte.isEmpty() || compte.getAsLong() < 0) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.COMPTE_INVALIDE);
        }
        return recompter(stock, idArticle, compte.getAsLong(), auteur);
    }

    private static MouvementStock appliquer(StockBuvette stock, ArticleBuvette a, NatureMouvement nature,
                                            long variation, int op, Instant t, OrigineMouvement origine,
                                            String reference, String auteur) {
        long apres = a.getStock() + variation;
        stock.enregistrer(a.avecStock(apres));
        MouvementStock m = new MouvementStock(op, t, a.getId(), nature, variation, apres, origine, reference, auteur);
        stock.journaliser(m);
        return m;
    }
}
