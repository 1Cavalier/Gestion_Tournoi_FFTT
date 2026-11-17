package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.CatalogueBase;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ResultatAjoutCatalogue;
import com.pingmanager.core.buvette.SaisieArticle;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Onglet « Articles » de la page Buvette (B026) : la liste des articles du
 * club par rayon, la fiche d'un article (création ou modification) et le
 * catalogue de base à cocher. Logique d'affichage dans
 * {@link PresentationArticlesBuvette} et {@link PresentationCatalogueBuvette}.
 */
final class VueArticlesBuvette {

    private final PageBuvette page;

    VueArticlesBuvette(PageBuvette page) {
        this.page = page;
    }

    Node construire() {
        if (page.vueCatalogue) {
            return catalogue();
        }
        StockBuvette stock = page.stock();
        PresentationArticlesBuvette p = new PresentationArticlesBuvette(stock);
        VBox v = new VBox(20);
        Button catalogue = p.vide() ? Ui.boutonPrimaire("Choisir dans le catalogue")
                : Ui.boutonSecondaire("Choisir dans le catalogue");
        catalogue.setOnAction(e -> {
            page.vueCatalogue = true;
            page.articleEdite = null;
            page.afficher();
        });
        Button creer = Ui.boutonSecondaire("Créer un article");
        creer.setOnAction(e -> {
            page.articleEdite = PageBuvette.NOUVEL_ARTICLE;
            page.afficher();
        });
        v.getChildren().add(Ui.enteteAvecActions(Ui.aide("Les articles de votre club servent d'un tournoi à "
                + "l'autre. Chacun se modifie librement ; le catalogue de base ne change jamais."), catalogue, creer));
        if (page.articleEdite != null) {
            v.getChildren().add(fiche(stock, new PresentationStockBuvette(stock), p));
        }
        if (p.vide()) {
            v.getChildren().add(Ui.carteNue(Ui.etatVideCompact(PresentationArticlesBuvette.AUCUN_ARTICLE)));
        }
        for (PresentationArticlesBuvette.Rayon r : p.rayons()) {
            v.getChildren().add(carteRayon(stock, r.titre(), r.lignes(), false));
        }
        List<PresentationArticlesBuvette.Ligne> archives = p.archives();
        if (!archives.isEmpty()) {
            v.getChildren().add(carteRayon(stock, "Retirés de la vente", archives, true));
        }
        return v;
    }

    private Node carteRayon(StockBuvette stock, String titre, List<PresentationArticlesBuvette.Ligne> lignes,
                            boolean archives) {
        VBox liste = new VBox(4);
        boolean premiere = true;
        for (PresentationArticlesBuvette.Ligne l : lignes) {
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);
            if (l.prixAFixer()) {
                actions.getChildren().add(Ui.pastille("Prix à fixer", StatutPastille.ATTENTION));
            }
            actions.getChildren().add(Ui.pastille(l.origine(), StatutPastille.NEUTRE));
            if (archives) {
                Button remettre = Ui.boutonLien("Remettre en vente");
                remettre.setOnAction(e -> page.annoncer(page.serviceArticles.remettreEnVente(stock, l.id())));
                actions.getChildren().add(remettre);
            } else {
                Button modifier = Ui.boutonLien("Modifier");
                modifier.setOnAction(e -> {
                    page.articleEdite = l.id();
                    page.afficher();
                });
                Button retirer = Ui.boutonLien("Retirer de la vente");
                retirer.setOnAction(e -> page.annoncer(page.serviceArticles.archiver(stock, l.id())));
                actions.getChildren().addAll(modifier, retirer);
            }
            if (l.supprimable()) {
                Button supprimer = Ui.boutonLien("Supprimer");
                supprimer.getStyleClass().add("lien-danger");
                supprimer.setOnAction(e -> page.annoncer(page.serviceArticles.supprimer(stock, l.id())));
                actions.getChildren().add(supprimer);
            }
            HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(), actions);
            ligne.getStyleClass().add("buvette-article");
            if (l.id().equals(page.articleEdite)) {
                ligne.getStyleClass().add("ligne-element-choisie");
            }
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        VBox carte = Ui.carte(Ui.titreAvecCompteur(titre, lignes.size()), liste);
        if (archives) {
            carte.getStyleClass().add("buvette-archives");
        }
        return carte;
    }

    // ==================================================================
    // Fiche d'un article
    // ==================================================================

    private Node fiche(StockBuvette stock, PresentationStockBuvette ps, PresentationArticlesBuvette pa) {
        ArticleBuvette existant = page.articleEdite.isEmpty() ? null : stock.article(page.articleEdite).orElse(null);
        TextField nom = Ui.champ("ex. Café, Gobelet");
        ComboBox<FamilleArticle> famille = Ui.combo(FamilleArticle.values(),
                existant == null ? FamilleArticle.PRODUIT : existant.getFamille());
        ComboBox<RayonBuvette> rayon = Ui.combo(RayonBuvette.rayonsProduits(),
                existant == null || !existant.estProduit() ? RayonBuvette.AUTRE : existant.getRayon());
        TextField unite = Ui.champ("ex. dose, gobelet, canette");
        TextField prixLot = Ui.champ("ex. 4,50");
        TextField unitesParLot = Ui.champ("ex. 100");
        TextField prixVente = Ui.champ("ex. 1,00");
        TextField quantiteStock = Ui.champ("ex. 200");
        TextField seuil = Ui.champ("ex. 30");
        if (existant != null) {
            nom.setText(existant.getNom());
            unite.setText(existant.getUnite());
            prixLot.setText(Centimes.nombre(existant.getPrixLotCentimes()));
            unitesParLot.setText(String.valueOf(existant.getUnitesParLot()));
            if (existant.getPrixVenteCentimes() > 0) {
                prixVente.setText(Centimes.nombre(existant.getPrixVenteCentimes()));
            }
            quantiteStock.setText(String.valueOf(existant.getStock()));
            quantiteStock.setDisable(true);
            seuil.setText(String.valueOf(existant.getSeuilAlerte()));
        }

        // Contenants utilisés par portion (produits seulement).
        VBox zoneContenants = new VBox(6);
        Map<String, CheckBox> coches = new LinkedHashMap<>();
        Map<String, TextField> quantites = new LinkedHashMap<>();
        List<ArticleBuvette> disponibles = new java.util.ArrayList<>(ps.contenantsDisponibles());
        if (existant != null) {
            for (LienContenant lien : existant.getContenants()) {
                stock.article(lien.idContenant()).filter(ArticleBuvette::estArchive).ifPresent(disponibles::add);
            }
        }
        zoneContenants.getChildren().add(Ui.libelleChamp("Ce que l'on met avec, à chaque portion"));
        if (disponibles.isEmpty()) {
            zoneContenants.getChildren().add(Ui.aide("Ajoutez d'abord vos contenants (famille « Contenant ou "
                    + "consommable ») pour les lier à ce produit."));
        }
        FlowPane cases = new FlowPane(16, 8);
        for (ArticleBuvette c : disponibles) {
            CheckBox coche = new CheckBox(c.getNom());
            TextField q = Ui.champ("1");
            q.setPrefColumnCount(2);
            q.getStyleClass().add("buvette-quantite");
            if (existant != null) {
                for (LienContenant lien : existant.getContenants()) {
                    if (lien.idContenant().equals(c.getId())) {
                        coche.setSelected(true);
                        q.setText(String.valueOf(lien.quantite()));
                    }
                }
            }
            q.disableProperty().bind(coche.selectedProperty().not());
            coches.put(c.getId(), coche);
            quantites.put(c.getId(), q);
            HBox h = new HBox(6, coche, q, Ui.discret("par portion"));
            h.setAlignment(Pos.CENTER_LEFT);
            cases.getChildren().add(h);
        }
        zoneContenants.getChildren().add(cases);
        VBox champRayon = Ui.champEtiquete("Rayon", rayon);
        VBox champPrixVente = Ui.champEtiquete("Prix de vente (€)", prixVente);
        Runnable majFamille = () -> {
            boolean produit = famille.getValue() == FamilleArticle.PRODUIT;
            Ui.afficherSi(zoneContenants, produit);
            Ui.afficherSi(champRayon, produit);
            Ui.afficherSi(champPrixVente, produit);
        };
        famille.valueProperty().addListener((o, a, n) -> majFamille.run());
        majFamille.run();

        Supplier<Map<String, Integer>> liens = () -> {
            Map<String, Integer> res = new LinkedHashMap<>();
            for (Map.Entry<String, CheckBox> e : coches.entrySet()) {
                if (e.getValue().isSelected()) {
                    String t = quantites.get(e.getKey()).getText().trim();
                    res.put(e.getKey(), t.isEmpty() ? 1 : t.matches("\\d{1,4}") ? Integer.parseInt(t) : 0);
                }
            }
            return res;
        };
        Button enregistrer = Ui.boutonPrimaire(existant == null ? "Ajouter l'article" : "Enregistrer");
        enregistrer.setOnAction(e -> page.annoncer(page.serviceArticles.enregistrer(stock, new SaisieArticle(
                existant == null ? null : existant.getId(), nom.getText(), famille.getValue(), unite.getText(),
                prixLot.getText(), unitesParLot.getText(), quantiteStock.getText(), seuil.getText(),
                liens.get(), prixVente.getText(),
                famille.getValue() == FamilleArticle.PRODUIT ? rayon.getValue() : RayonBuvette.CONTENANTS))));
        Button fermer = Ui.boutonSecondaire("Fermer");
        fermer.setOnAction(e -> {
            page.articleEdite = null;
            page.afficher();
        });
        FlowPane champs = new FlowPane(12, 12,
                Ui.champEtiquete("Nom", nom), Ui.champEtiquete("Famille", famille), champRayon,
                Ui.champEtiquete("Unité comptée", unite), Ui.champEtiquete("Prix d'un lot (€)", prixLot),
                Ui.champEtiquete("Unités par lot", unitesParLot), champPrixVente,
                Ui.champEtiquete(existant == null ? "Stock de départ" : "Stock (onglet Stock)", quantiteStock),
                Ui.champEtiquete("Seuil d'alerte", seuil));
        VBox corps = new VBox(12, champs,
                Ui.aide("Le coût se saisit comme vous achetez : « 4,50 € les 100 gobelets ». "
                        + "Pour un produit, une unité de stock = une portion servie."),
                zoneContenants);
        if (existant != null) {
            Label couts = Ui.texte(pa.resumeCouts(existant));
            couts.getStyleClass().add("buvette-resume-couts");
            corps.getChildren().add(couts);
            if (!existant.getIdCatalogue().isEmpty()) {
                corps.getChildren().add(Ui.aide("Copié du catalogue de base : vos changements ne modifient que "
                        + "l'article de votre club."));
            }
        }
        corps.getChildren().add(Ui.actions(enregistrer, fermer));
        VBox carte = Ui.carte(existant == null ? "Créer un article" : "Fiche « " + existant.getNom() + " »", corps);
        carte.getStyleClass().add("buvette-fiche");
        return carte;
    }

    // ==================================================================
    // Catalogue de base à cocher
    // ==================================================================

    private Node catalogue() {
        StockBuvette stock = page.stock();
        PresentationCatalogueBuvette p = new PresentationCatalogueBuvette(CatalogueBase.catalogue(), stock);
        VBox v = new VBox(20);
        Label compteur = Ui.discret("");
        Runnable majCompteur = () -> compteur.setText(page.cochesCatalogue.isEmpty() ? "Rien de coché"
                : page.cochesCatalogue.size() + (page.cochesCatalogue.size() > 1 ? " articles cochés" : " article coché"));
        majCompteur.run();
        Button ajouter = Ui.boutonPrimaire("Ajouter à mes articles");
        ajouter.setOnAction(e -> {
            ResultatAjoutCatalogue r = page.serviceArticles.ajouterDepuisCatalogue(stock, CatalogueBase.catalogue(),
                    page.cochesCatalogue);
            if (r.accepte()) {
                page.cochesCatalogue.clear();
                page.vueCatalogue = false;
            }
            page.enregistrer(r.accepte(), r.explication());
        });
        Button retour = Ui.boutonSecondaire("Retour à mes articles");
        retour.setOnAction(e -> {
            page.vueCatalogue = false;
            page.afficher();
        });
        v.getChildren().add(Ui.encartInfo(PresentationCatalogueBuvette.AVERTISSEMENT));
        for (PresentationCatalogueBuvette.Rayon r : p.rayons()) {
            VBox liste = new VBox(2);
            boolean premiere = true;
            for (PresentationCatalogueBuvette.Ligne l : r.lignes()) {
                CheckBox coche = new CheckBox();
                coche.getStyleClass().add("buvette-coche-catalogue");
                if (l.dejaAuClub()) {
                    coche.setSelected(true);
                    coche.setDisable(true);
                } else {
                    coche.setSelected(page.cochesCatalogue.contains(l.id()));
                    coche.selectedProperty().addListener((o, a, n) -> {
                        if (n) {
                            page.cochesCatalogue.add(l.id());
                        } else {
                            page.cochesCatalogue.remove(l.id());
                        }
                        majCompteur.run();
                    });
                }
                HBox ligne = l.dejaAuClub()
                        ? Ui.ligneElement(coche, l.nom(), l.detail(), Ui.pastille("Déjà dans vos articles",
                        StatutPastille.SUCCES))
                        : Ui.ligneElement(coche, l.nom(), l.detail(), Ui.pastille("Indicatif, à adapter",
                        StatutPastille.NEUTRE));
                ligne.getStyleClass().add("buvette-ligne-catalogue");
                if (!l.dejaAuClub()) {
                    ligne.setOnMouseClicked(e -> {
                        if (e.getTarget() != coche) {
                            coche.setSelected(!coche.isSelected());
                        }
                    });
                }
                if (premiere) {
                    ligne.getStyleClass().add("ligne-element-premiere");
                    premiere = false;
                }
                liste.getChildren().add(ligne);
            }
            v.getChildren().add(Ui.carte(Ui.titreAvecCompteur(r.titre(), r.lignes().size()), liste));
        }
        HBox barre = new HBox(12, compteur, Ui.espace(), retour, ajouter);
        barre.setAlignment(Pos.CENTER_LEFT);
        barre.getStyleClass().addAll("carte", "buvette-barre-catalogue");
        v.getChildren().add(1, Ui.aide("Cochez ce que votre club vend. Un produit entraîne ses contenants "
                + "(un café ajoute aussi gobelet, touillette et sucre)."));
        v.getChildren().add(barre);
        return v;
    }
}
