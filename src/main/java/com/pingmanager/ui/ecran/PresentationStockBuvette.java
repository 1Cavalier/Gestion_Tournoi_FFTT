package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.CalculCoutBuvette;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.CoutPortion;
import com.pingmanager.core.buvette.JaugeStock;
import com.pingmanager.core.buvette.NiveauStock;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.StockBuvette;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Logique d'affichage de la page « Buvette » (B026), testée hors JavaFX :
 * liste « À racheter », articles par famille avec leur jauge et leurs coûts,
 * derniers mouvements.
 */
final class PresentationStockBuvette {

    static final String MESSAGE_VIDE = "Aucun article pour l'instant : ajoutez vos produits (café, boissons, "
            + "sandwichs…) et vos contenants (gobelets, touillettes…) avec le formulaire ci-dessous.";
    static final String RIEN_A_RACHETER = "Rien à racheter : tous les stocks sont au-dessus de leur seuil d'alerte.";

    /** Classe CSS de la jauge selon le niveau. */
    static String classeJauge(NiveauStock n) {
        if (n == NiveauStock.URGENT) {
            return "jauge-stock-rouge";
        }
        if (n == NiveauStock.BAS) {
            return "jauge-stock-orange";
        }
        return "jauge-stock-vert";
    }

    /** Pastille du niveau. */
    static StatutPastille statut(NiveauStock n) {
        if (n == NiveauStock.URGENT) {
            return StatutPastille.ERREUR;
        }
        if (n == NiveauStock.BAS) {
            return StatutPastille.ATTENTION;
        }
        return StatutPastille.SUCCES;
    }

    /** Ligne de la liste « À racheter ». */
    record LigneARacheter(String id, String nom, String detail) {
    }

    /**
     * Ligne d'un article.
     *
     * @param id          identifiant
     * @param nom         nom
     * @param famille     famille
     * @param stock       « 120 doses »
     * @param niveau      niveau du stock
     * @param classeJauge classe CSS de la jauge
     * @param remplissage remplissage de la jauge (0 à 1)
     * @param detail      seuil, contenants, coûts
     * @param retirable   faux pour un contenant qui sert encore à un produit
     * @param aRecompter  vrai si le stock a manqué à une sortie ou une vente
     */
    record LigneArticle(String id, String nom, FamilleArticle famille, String stock, NiveauStock niveau,
                        String classeJauge, double remplissage, String detail, boolean retirable,
                        boolean aRecompter) {
    }

    /** Groupe d'articles d'une famille. */
    record Section(FamilleArticle famille, String titre, List<LigneArticle> lignes) {
    }

    private final StockBuvette stock;

    PresentationStockBuvette(StockBuvette stock) {
        this.stock = stock;
    }

    boolean vide() {
        return stock.getArticles().isEmpty();
    }

    List<LigneARacheter> aRacheter() {
        List<LigneARacheter> res = new ArrayList<>();
        for (JaugeStock j : JaugeStock.aRacheter(stock)) {
            ArticleBuvette a = j.article();
            res.add(new LigneARacheter(a.getId(), a.getNom(), "Il reste " + quantite(a.getStock(), a.getUnite())
                    + " · seuil d'alerte " + a.getSeuilAlerte()));
        }
        return res;
    }

    /** Les deux familles, produits d'abord (une famille vide reste affichée) ; sans les articles archivés. */
    List<Section> sections() {
        List<Section> res = new ArrayList<>();
        for (FamilleArticle f : FamilleArticle.values()) {
            List<LigneArticle> lignes = new ArrayList<>();
            for (ArticleBuvette a : stock.articlesActifs(f)) {
                lignes.add(ligne(a));
            }
            res.add(new Section(f, f.getLibellePluriel(), lignes));
        }
        return res;
    }

    LigneArticle ligne(ArticleBuvette a) {
        JaugeStock j = JaugeStock.de(a);
        List<String> parts = new ArrayList<>();
        parts.add("Seuil d'alerte " + a.getSeuilAlerte());
        if (a.estProduit() && a.getPrixVenteCentimes() > 0) {
            parts.add("Vendu " + Centimes.euros(a.getPrixVenteCentimes()));
        }
        if (a.estProduit()) {
            String c = texteContenants(a);
            if (!c.isEmpty()) {
                parts.add("Par portion : " + c);
            }
            parts.add(texteCouts(a));
        } else {
            parts.add(Centimes.eurosPrecis(CalculCoutBuvette.coutUnitaire(a)) + " l'unité");
            List<ArticleBuvette> utilisateurs = stock.produitsUtilisant(a.getId());
            if (!utilisateurs.isEmpty()) {
                List<String> n = new ArrayList<>();
                utilisateurs.forEach(p -> n.add(p.getNom()));
                parts.add("Utilisé par " + String.join(", ", n));
            }
        }
        return new LigneArticle(a.getId(), a.getNom(), a.getFamille(), quantite(a.getStock(), a.getUnite()),
                j.niveau(), classeJauge(j.niveau()), j.remplissage(), String.join(" · ", parts),
                stock.produitsUtilisant(a.getId()).isEmpty(), stock.estARecompter(a.getId()));
    }

    /** « 1 gobelet, 1 touillette » (vide si aucun contenant). */
    String texteContenants(ArticleBuvette produit) {
        List<String> res = new ArrayList<>();
        for (LienContenant l : produit.getContenants()) {
            stock.article(l.idContenant()).ifPresent(c -> res.add(l.quantite() + " " + c.getNom().toLowerCase()));
        }
        return String.join(", ", res);
    }

    /** « Produit 0,064 € + emballage 0,048 € = 0,112 € la portion ». */
    String texteCouts(ArticleBuvette produit) {
        Optional<CoutPortion> c = CalculCoutBuvette.portion(stock, produit);
        if (c.isEmpty()) {
            return "";
        }
        return "Produit " + Centimes.eurosPrecis(c.get().brut()) + " + emballage "
                + Centimes.eurosPrecis(c.get().emballage()) + " = " + Centimes.eurosPrecis(c.get().complet())
                + " la portion";
    }

    /** Contenants proposés dans le formulaire d'un produit (ceux en vente). */
    List<ArticleBuvette> contenantsDisponibles() {
        return stock.articlesActifs(FamilleArticle.CONTENANT);
    }

    /** @return vrai si au moins un article est en vente (hors archivés). */
    boolean aDesArticlesActifs() {
        return !stock.articlesActifs(FamilleArticle.PRODUIT).isEmpty()
                || !stock.articlesActifs(FamilleArticle.CONTENANT).isEmpty();
    }

    /** Les {@code n} derniers mouvements, du plus récent au plus ancien. */
    List<String> derniersMouvements(int n, ZoneId zone) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(zone);
        List<MouvementStock> journal = stock.getJournal();
        List<String> res = new ArrayList<>();
        for (int i = journal.size() - 1; i >= 0 && res.size() < n; i--) {
            MouvementStock m = journal.get(i);
            String nom = stock.article(m.idArticle()).map(ArticleBuvette::getNom).orElse(m.idArticle());
            String v = m.variation() > 0 ? "+" + m.variation() : String.valueOf(m.variation());
            String origine = m.origine() == com.pingmanager.model.buvette.OrigineMouvement.SAISIE_MANUELLE ? ""
                    : " (" + m.origine().getLibelle().toLowerCase() + " n° "
                    + m.reference().substring(m.reference().lastIndexOf('#') + 1) + ")";
            res.add(f.format(m.horodatage()) + " · " + m.nature().getLibelle() + origine + " · " + nom + " " + v
                    + " (reste " + m.stockApres() + ") · " + m.auteur());
        }
        return res;
    }

    /** « 1 gobelet », « 12 gobelets » : pluriel simple (+ s), sauf mot déjà en -s ou -x. */
    static String quantite(long n, String unite) {
        return n + " " + (n > 1 && !unite.endsWith("s") && !unite.endsWith("x") ? unite + "s" : unite);
    }
}
