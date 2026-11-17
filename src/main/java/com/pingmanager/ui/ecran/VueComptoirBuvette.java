package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ResultatVente;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.model.buvette.VenteBuvette;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Onglet « Comptoir » de la page Buvette (B026, mode « Stock et ventes ») :
 * une grille de gros boutons, un par article en vente, rangés par rayon ; un
 * appui ajoute une portion à la commande en cours. « Valider » enregistre la
 * vente (prix figé, moyen de paiement) et sort le stock du produit et de ses
 * contenants. La dernière vente s'annule d'un geste.
 *
 * <p>Pensé pour une tablette posée sur le bar : boutons de 150 × 96 au moins,
 * commande à droite sur grand écran, sous la grille quand l'écran est étroit.
 * Logique d'affichage dans {@link PresentationComptoirBuvette}.</p>
 */
final class VueComptoirBuvette {

    /** En dessous de cette largeur, la commande passe sous la grille. */
    static final double LARGEUR_DEUX_COLONNES = 860;

    private final PageBuvette page;

    VueComptoirBuvette(PageBuvette page) {
        this.page = page;
    }

    Node construire() {
        StockBuvette stock = page.stock();
        PresentationComptoirBuvette p = new PresentationComptoirBuvette(stock, page.commande);
        VBox grille = new VBox(16);
        grille.getStyleClass().add("buvette-comptoir-grille");
        if (!page.avertissementComptoir.isEmpty()) {
            grille.getChildren().add(Ui.encartAlerte("Stock insuffisant : pensez à recompter",
                    page.avertissementComptoir));
        }
        if (p.aucunArticle()) {
            Button articles = Ui.boutonPrimaire("Ajouter des articles");
            articles.setOnAction(e -> {
                page.vueCatalogue = true;
                page.ouvrir(PageBuvette.ONGLET_ARTICLES);
            });
            grille.getChildren().add(Ui.carteNue(Ui.etatVide(PresentationComptoirBuvette.AUCUN_ARTICLE, articles)));
        }
        for (PresentationComptoirBuvette.Rayon r : p.rayons()) {
            FlowPane boutons = new FlowPane(10, 10);
            boutons.getStyleClass().add("buvette-comptoir-rayon");
            boutons.prefWrapLengthProperty().bind(grille.widthProperty());
            for (PresentationComptoirBuvette.BoutonArticle b : r.boutons()) {
                boutons.getChildren().add(bouton(b));
            }
            grille.getChildren().add(new VBox(8, Ui.titreSection(r.titre()), boutons));
        }
        VBox commande = commande(p);
        return disposition(grille, commande);
    }

    /** Grille et commande côte à côte sur grand écran, l'une sous l'autre sinon. */
    private static Node disposition(VBox grille, VBox commande) {
        StackPane conteneur = new StackPane();
        HBox large = new HBox(20);
        VBox etroit = new VBox(20);
        HBox.setHgrow(grille, Priority.ALWAYS);
        commande.setMinWidth(320);
        commande.setPrefWidth(360);
        commande.setMaxWidth(380);
        Runnable composer = () -> {
            boolean deux = conteneur.getWidth() <= 0 || conteneur.getWidth() >= LARGEUR_DEUX_COLONNES;
            if (deux && !conteneur.getChildren().contains(large)) {
                etroit.getChildren().clear();
                commande.setMaxWidth(380);
                large.getChildren().setAll(grille, commande);
                conteneur.getChildren().setAll(large);
            } else if (!deux && !conteneur.getChildren().contains(etroit)) {
                large.getChildren().clear();
                commande.setMaxWidth(Double.MAX_VALUE);
                etroit.getChildren().setAll(commande, grille);
                conteneur.getChildren().setAll(etroit);
            }
        };
        conteneur.widthProperty().addListener((o, a, n) -> composer.run());
        composer.run();
        return conteneur;
    }

    private Node bouton(PresentationComptoirBuvette.BoutonArticle b) {
        Label nom = new Label(b.nom());
        nom.getStyleClass().add("buvette-bouton-nom");
        nom.setWrapText(true);
        Label prix = new Label(b.prix());
        prix.getStyleClass().add("buvette-bouton-prix");
        VBox contenu = new VBox(4, nom, prix);
        contenu.setAlignment(Pos.TOP_LEFT);
        if (b.quantite() > 0) {
            contenu.getStyleClass().add("buvette-bouton-contenu-compte"); // place pour la pastille du nombre
        }
        if (!b.alerte().isEmpty()) {
            contenu.getChildren().add(Ui.pastille(b.alerte(), "À recompter".equals(b.alerte())
                    || "Épuisé".equals(b.alerte()) ? StatutPastille.ERREUR : StatutPastille.ATTENTION));
        }
        StackPane graphique = new StackPane(contenu);
        StackPane.setAlignment(contenu, Pos.TOP_LEFT);
        if (b.quantite() > 0) {
            Label q = new Label(String.valueOf(b.quantite()));
            q.getStyleClass().add("buvette-bouton-quantite");
            StackPane.setAlignment(q, Pos.TOP_RIGHT);
            graphique.getChildren().add(q);
        }
        Button bouton = new Button();
        bouton.setGraphic(graphique);
        bouton.getStyleClass().add("buvette-bouton-article");
        if (b.quantite() > 0) {
            bouton.getStyleClass().add("buvette-bouton-article-choisi");
        }
        bouton.setDisable(!b.vendable());
        bouton.setMnemonicParsing(false);
        graphique.prefWidthProperty().bind(bouton.widthProperty().subtract(28));
        bouton.setOnAction(e -> {
            page.commande.ajouter(b.id());
            page.message.setText("");
            page.afficher();
        });
        return bouton;
    }

    private VBox commande(PresentationComptoirBuvette p) {
        VBox lignes = new VBox(6);
        if (page.commande.estVide()) {
            lignes.getChildren().add(Ui.discret(PresentationComptoirBuvette.COMMANDE_VIDE));
        }
        for (PresentationComptoirBuvette.LigneCommande l : p.lignes()) {
            Button moins = Ui.boutonSecondaire("−");
            moins.getStyleClass().add("buvette-pas");
            moins.setOnAction(e -> {
                page.commande.retirerUn(l.id());
                page.afficher();
            });
            Button plus = Ui.boutonSecondaire("+");
            plus.getStyleClass().add("buvette-pas");
            plus.setOnAction(e -> {
                page.commande.ajouter(l.id());
                page.afficher();
            });
            Label q = Ui.gras(String.valueOf(l.quantite()));
            q.getStyleClass().add("buvette-commande-quantite");
            Label nom = Ui.texte(l.nom());
            nom.setWrapText(true);
            HBox.setHgrow(nom, Priority.ALWAYS);
            nom.setMaxWidth(Double.MAX_VALUE);
            Label montant = Ui.gras(l.montant());
            montant.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            HBox ligne = new HBox(8, moins, q, plus, nom, montant);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("buvette-commande-ligne");
            lignes.getChildren().add(ligne);
        }
        Label total = new Label(Centimes.euros(p.totalCentimes()));
        total.getStyleClass().add("buvette-total");
        HBox ligneTotal = new HBox(8, Ui.discret(p.resume()), Ui.espace(), total);
        ligneTotal.setAlignment(Pos.BASELINE_LEFT);

        ToggleGroup groupe = new ToggleGroup();
        ToggleButton[] moyens = new ToggleButton[MoyenPaiement.values().length];
        int i = 0;
        for (MoyenPaiement m : MoyenPaiement.values()) {
            ToggleButton t = new ToggleButton(m.getLibelle());
            t.getStyleClass().add("buvette-moyen");
            t.setSelected(m == page.moyen);
            t.setOnAction(e -> {
                page.moyen = m;
                t.setSelected(true);
            });
            t.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(t, Priority.ALWAYS);
            moyens[i++] = t;
        }
        HBox segment = Ui.segment(groupe, moyens);
        segment.getStyleClass().add("buvette-moyens");

        Button valider = Ui.boutonPrimaire(p.libelleValider());
        valider.getStyleClass().add("buvette-valider");
        valider.setMaxWidth(Double.MAX_VALUE);
        valider.setDisable(page.commande.estVide());
        valider.setOnAction(e -> valider());
        Button vider = Ui.boutonLien("Vider la commande");
        vider.setDisable(page.commande.estVide());
        vider.setOnAction(e -> {
            page.commande.vider();
            page.afficher();
        });

        VBox carte = Ui.carte("Commande en cours", lignes, Ui.filet(), ligneTotal,
                Ui.libelleChamp("Paiement"), segment, valider, vider);
        carte.getStyleClass().add("buvette-commande");
        derniereVente().ifPresent(v -> {
            Button annuler = Ui.boutonSecondaire("Annuler la dernière vente");
            annuler.getStyleClass().add("buvette-annuler-derniere");
            annuler.setMaxWidth(Double.MAX_VALUE);
            annuler.setOnAction(e -> {
                ResultatVente r = page.serviceVentes.annulerDerniere(page.stock(), page.referenceTournoi(),
                        page.auteur());
                page.avertissementComptoir = "";
                page.enregistrer(r.accepte(), r.explication());
            });
            carte.getChildren().addAll(Ui.filet(), Ui.discret("Dernière vente : n° " + v.numero() + " · "
                    + Centimes.euros(v.totalCentimes()) + " · " + v.moyen().getLibelle().toLowerCase()), annuler);
        });
        return carte;
    }

    private Optional<VenteBuvette> derniereVente() {
        return page.stock().tournoi(page.referenceTournoi()).flatMap(BuvetteTournoi::derniereVenteValable);
    }

    private void valider() {
        ResultatVente r = page.serviceVentes.vendre(page.stock(), page.referenceTournoi(), page.commande, page.moyen,
                page.auteur());
        if (r.accepte()) {
            page.commande.vider();
            page.moyen = MoyenPaiement.ESPECES;
            page.avertissementComptoir = r.avecAvertissement()
                    ? "La vente n° " + r.vente().numero() + " est enregistrée, mais le stock de "
                    + String.join(", ", r.aRecompter()) + " était insuffisant (ramené à 0). Recomptez dans "
                    + "l'onglet « Stock »." : "";
        }
        page.enregistrer(r.accepte(), r.message());
    }
}
