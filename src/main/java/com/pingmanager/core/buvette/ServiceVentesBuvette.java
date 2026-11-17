package com.pingmanager.core.buvette;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.buvette.AnnulationVente;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.OrigineVente;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.SortieVente;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.model.buvette.VenteBuvette;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ventes de la buvette pendant un tournoi (B026), en mode « Stock et
 * ventes » : enregistrement (prix et coût figés, stock sorti pour le produit
 * et ses contenants) et annulation (stock remis, vente gardée et marquée
 * annulée).
 *
 * <p>Stock insuffisant : la vente passe quand même (au comptoir, c'est la
 * réalité qui compte), avec un avertissement, et l'article est signalé « à
 * recompter ».</p>
 *
 * <p>Suivi interne du club, pas une caisse enregistreuse. Le jour où un
 * joueur commandera en ligne (B026a), la commande retirée au comptoir
 * passera par {@link #vendre(StockBuvette, String, Map, MoyenPaiement, OrigineVente, String, String)}
 * avec son origine et sa référence.</p>
 */
public final class ServiceVentesBuvette {

    private final Horloge horloge;
    private final ServiceStockBuvette serviceStock;

    public ServiceVentesBuvette(Horloge horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.serviceStock = new ServiceStockBuvette(horloge);
    }

    /** Règle la buvette d'un tournoi (« Stock seulement » ou « Stock et ventes »). */
    public BuvetteTournoi regler(StockBuvette stock, String referenceTournoi, String nomTournoi, ModeBuvette mode) {
        BuvetteTournoi t = stock.tournoiOuCree(referenceTournoi, nomTournoi);
        t.setMode(Objects.requireNonNull(mode, "mode"));
        return t;
    }

    /** @return le réglage du tournoi ({@link ModeBuvette#STOCK_SEUL} s'il n'a jamais été réglé). */
    public static ModeBuvette mode(StockBuvette stock, String referenceTournoi) {
        return stock.tournoi(referenceTournoi).map(BuvetteTournoi::getMode).orElse(ModeBuvette.STOCK_SEUL);
    }

    /** Articles proposés au comptoir : les produits non archivés, rangés par rayon. */
    public static List<ArticleBuvette> articlesEnVente(StockBuvette stock) {
        List<ArticleBuvette> res = new ArrayList<>();
        List<ArticleBuvette> produits = stock.articlesActifs(FamilleArticle.PRODUIT);
        for (RayonBuvette r : RayonBuvette.rayonsProduits()) {
            for (ArticleBuvette a : produits) {
                if (a.getRayon() == r) {
                    res.add(a);
                }
            }
        }
        return res;
    }

    /** Vente au comptoir de la commande en cours. */
    public ResultatVente vendre(StockBuvette stock, String referenceTournoi, CommandeComptoir commande,
                                MoyenPaiement moyen, String auteur) {
        return vendre(stock, referenceTournoi, commande.quantites(), moyen, OrigineVente.COMPTOIR, "", auteur);
    }

    /**
     * Enregistre une vente : vérifie toute la commande d'abord (rien n'est
     * sorti si une ligne est refusée), puis sort le stock de chaque produit et
     * de ses contenants.
     *
     * @param quantites        article → portions, dans l'ordre de la commande
     * @param origine          d'où vient la vente
     * @param referenceOrigine référence de l'origine (vide au comptoir)
     */
    public ResultatVente vendre(StockBuvette stock, String referenceTournoi, Map<String, Integer> quantites,
                                MoyenPaiement moyen, OrigineVente origine, String referenceOrigine, String auteur) {
        Objects.requireNonNull(moyen, "moyen de paiement");
        Objects.requireNonNull(origine, "origine");
        Optional<BuvetteTournoi> bt = stock.tournoi(referenceTournoi);
        if (bt.isEmpty() || bt.get().getMode() != ModeBuvette.STOCK_ET_VENTES) {
            return ResultatVente.refus(ResultatVente.Motif.MODE_STOCK_SEUL);
        }
        if (quantites == null || quantites.isEmpty()) {
            return ResultatVente.refus(ResultatVente.Motif.COMMANDE_VIDE);
        }
        List<LigneVente> lignes = new ArrayList<>();
        for (Map.Entry<String, Integer> e : quantites.entrySet()) {
            Optional<ArticleBuvette> a = stock.article(e.getKey());
            if (a.isEmpty()) {
                return ResultatVente.refus(ResultatVente.Motif.ARTICLE_INCONNU);
            }
            ArticleBuvette art = a.get();
            if (art.estArchive()) {
                return ResultatVente.refus(ResultatVente.Motif.ARTICLE_ARCHIVE, art.getNom());
            }
            if (!art.estProduit()) {
                return ResultatVente.refus(ResultatVente.Motif.PAS_UN_PRODUIT, art.getNom());
            }
            if (art.getPrixVenteCentimes() <= 0) {
                return ResultatVente.refus(ResultatVente.Motif.PRIX_MANQUANT, art.getNom());
            }
            Integer q = e.getValue();
            if (q == null || q < 1 || q > CommandeComptoir.QUANTITE_MAX) {
                return ResultatVente.refus(ResultatVente.Motif.QUANTITE_INVALIDE);
            }
            BigDecimal cout = CalculCoutBuvette.portion(stock, art).orElseThrow().complet();
            lignes.add(new LigneVente(art.getId(), art.getNom(), q, art.getPrixVenteCentimes(), cout));
        }
        BuvetteTournoi t = bt.get();
        int numero = t.prochainNumero();
        String reference = reference(t, numero);
        List<SortieVente> sorties = new ArrayList<>();
        Set<String> manquants = new LinkedHashSet<>();
        for (LigneVente l : lignes) {
            ResultatMouvementStock r = serviceStock.sortir(stock, l.idArticle(), l.quantite(), OrigineMouvement.VENTE,
                    reference, auteur);
            for (MouvementStock m : r.mouvements()) {
                sorties.add(new SortieVente(m.idArticle(), -m.variation()));
            }
            manquants.addAll(r.epuises());
        }
        VenteBuvette v = new VenteBuvette(numero, t.getReferenceTournoi(), horloge.maintenant(), lignes, moyen, origine,
                referenceOrigine, auteur, sorties, new ArrayList<>(manquants), null);
        t.ajouter(v);
        int n = v.nombreArticles();
        String message = "Vente n° " + numero + " enregistrée : " + n + (n > 1 ? " articles, " : " article, ")
                + Centimes.euros(v.totalCentimes()) + " (" + moyen.getLibelle().toLowerCase() + ").";
        return new ResultatVente(ResultatVente.Motif.ENREGISTREE, v, message, new ArrayList<>(manquants));
    }

    /** Annule la plus récente vente encore valable du tournoi (le geste « oups » du comptoir). */
    public ResultatVente annulerDerniere(StockBuvette stock, String referenceTournoi, String auteur) {
        Optional<VenteBuvette> v = stock.tournoi(referenceTournoi).flatMap(BuvetteTournoi::derniereVenteValable);
        if (v.isEmpty()) {
            return ResultatVente.refus(ResultatVente.Motif.AUCUNE_VENTE);
        }
        return annuler(stock, referenceTournoi, v.get().numero(), auteur);
    }

    /**
     * Annule une vente : remet en stock exactement ce qu'elle avait sorti et la
     * garde dans l'historique, marquée annulée (elle ne compte plus au bilan).
     */
    public ResultatVente annuler(StockBuvette stock, String referenceTournoi, int numero, String auteur) {
        Optional<BuvetteTournoi> bt = stock.tournoi(referenceTournoi);
        Optional<VenteBuvette> v = bt.flatMap(t -> t.vente(numero));
        if (v.isEmpty()) {
            return ResultatVente.refus(ResultatVente.Motif.VENTE_INCONNUE);
        }
        if (v.get().estAnnulee()) {
            return ResultatVente.refus(ResultatVente.Motif.DEJA_ANNULEE);
        }
        BuvetteTournoi t = bt.get();
        serviceStock.remettre(stock, v.get().sorties(), OrigineMouvement.ANNULATION_VENTE, reference(t, numero),
                auteur);
        VenteBuvette annulee = v.get().annulee(new AnnulationVente(horloge.maintenant(), auteur));
        t.remplacer(annulee);
        return new ResultatVente(ResultatVente.Motif.ANNULEE, annulee, "Vente n° " + numero + " annulée ("
                + Centimes.euros(annulee.totalCentimes()) + ") : le stock est remis, la vente reste dans "
                + "l'historique.", List.of());
    }

    /** Référence d'une vente dans le journal de stock : « tournoi#12 ». */
    static String reference(BuvetteTournoi t, int numero) {
        return t.getReferenceTournoi() + "#" + numero;
    }
}
