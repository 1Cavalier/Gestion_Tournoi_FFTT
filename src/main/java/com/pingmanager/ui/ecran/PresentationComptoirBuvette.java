package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.CommandeComptoir;
import com.pingmanager.core.buvette.JaugeStock;
import com.pingmanager.core.buvette.NiveauStock;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Logique d'affichage du comptoir de la buvette (B026), testée hors JavaFX :
 * grille des articles en vente par rayon, commande en cours et total.
 */
final class PresentationComptoirBuvette {

    static final String AUCUN_ARTICLE = "Aucun produit en vente : ajoutez des articles dans l'onglet « Articles » "
            + "(catalogue de base ou création), avec un prix de vente.";
    static final String COMMANDE_VIDE = "Touchez un article pour l'ajouter à la commande.";

    /**
     * Bouton d'un article.
     *
     * @param id        article
     * @param nom       nom
     * @param prix      « 1,00 € » ou « Prix à fixer »
     * @param vendable  faux si le prix n'est pas fixé (bouton grisé)
     * @param quantite  portions déjà dans la commande (0 : aucune)
     * @param alerte    « À recompter », « Épuisé », « Stock bas » ou vide
     */
    record BoutonArticle(String id, String nom, String prix, boolean vendable, int quantite, String alerte) {
    }

    /** Rayon du comptoir et ses boutons. */
    record Rayon(String titre, List<BoutonArticle> boutons) {
    }

    /** Ligne de la commande en cours : « 2 × Café … 2,00 € ». */
    record LigneCommande(String id, String nom, int quantite, String montant) {
    }

    private final StockBuvette stock;
    private final CommandeComptoir commande;

    PresentationComptoirBuvette(StockBuvette stock, CommandeComptoir commande) {
        this.stock = stock;
        this.commande = commande;
    }

    /** Rayons non vides, dans l'ordre (boissons chaudes, froides, salé, sucré, autres). */
    List<Rayon> rayons() {
        List<ArticleBuvette> enVente = ServiceVentesBuvette.articlesEnVente(stock);
        List<Rayon> res = new ArrayList<>();
        for (RayonBuvette r : RayonBuvette.rayonsProduits()) {
            List<BoutonArticle> boutons = new ArrayList<>();
            for (ArticleBuvette a : enVente) {
                if (a.getRayon() == r) {
                    boolean vendable = a.getPrixVenteCentimes() > 0;
                    boutons.add(new BoutonArticle(a.getId(), a.getNom(),
                            vendable ? Centimes.euros(a.getPrixVenteCentimes()) : "Prix à fixer", vendable,
                            commande.quantite(a.getId()), alerte(a)));
                }
            }
            if (!boutons.isEmpty()) {
                res.add(new Rayon(r.getLibelle(), boutons));
            }
        }
        return res;
    }

    /** Alerte de stock du produit, ou de l'un de ses contenants (le plus grave). */
    String alerte(ArticleBuvette produit) {
        List<ArticleBuvette> concernes = new ArrayList<>();
        concernes.add(produit);
        for (LienContenant l : produit.getContenants()) {
            stock.article(l.idContenant()).ifPresent(concernes::add);
        }
        String res = "";
        for (ArticleBuvette a : concernes) {
            if (stock.estARecompter(a.getId())) {
                return "À recompter";
            }
            if (a.getStock() == 0) {
                res = "Épuisé";
            } else if (res.isEmpty() && JaugeStock.de(a).niveau() == NiveauStock.URGENT) {
                res = "Stock bas";
            }
        }
        return res;
    }

    boolean aucunArticle() {
        return ServiceVentesBuvette.articlesEnVente(stock).isEmpty();
    }

    List<LigneCommande> lignes() {
        List<LigneCommande> res = new ArrayList<>();
        for (Map.Entry<String, Integer> e : commande.quantites().entrySet()) {
            stock.article(e.getKey()).ifPresent(a -> res.add(new LigneCommande(a.getId(), a.getNom(), e.getValue(),
                    Centimes.euros(a.getPrixVenteCentimes() * e.getValue()))));
        }
        return res;
    }

    long totalCentimes() {
        long t = 0;
        for (Map.Entry<String, Integer> e : commande.quantites().entrySet()) {
            t += stock.article(e.getKey()).map(ArticleBuvette::getPrixVenteCentimes).orElse(0L) * e.getValue();
        }
        return t;
    }

    int nombreArticles() {
        int n = 0;
        for (int q : commande.quantites().values()) {
            n += q;
        }
        return n;
    }

    /** « Valider · 3,50 € » (ou « Valider » si la commande est vide). */
    String libelleValider() {
        return commande.estVide() ? "Valider" : "Valider · " + Centimes.euros(totalCentimes());
    }

    /** « 3 articles » / « 1 article ». */
    String resume() {
        int n = nombreArticles();
        return n == 0 ? "Commande vide" : n + (n > 1 ? " articles" : " article");
    }
}
