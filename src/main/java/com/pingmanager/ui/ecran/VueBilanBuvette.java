package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.BilanBuvette;
import com.pingmanager.core.buvette.ExportCsvBuvette;
import com.pingmanager.core.buvette.ResultatVente;
import com.pingmanager.model.buvette.VenteBuvette;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Onglet « Bilan » de la page Buvette (B026) : pour le tournoi ou une
 * journée, nombre de ventes, recette, coût des articles vendus et marge ;
 * détail par article et par moyen de paiement ; historique des ventes
 * (annulées comprises, avec annulation d'une vente) ; export CSV du bilan et
 * de l'historique. Logique d'affichage dans {@link PresentationBilanBuvette}.
 *
 * <p>Suivi interne du club : ce bilan n'est pas un document de caisse.</p>
 */
final class VueBilanBuvette {

    private final PageBuvette page;

    VueBilanBuvette(PageBuvette page) {
        this.page = page;
    }

    Node construire() {
        List<VenteBuvette> ventes = page.ventes();
        PresentationBilanBuvette p = new PresentationBilanBuvette(ventes, page.zone);
        VBox v = new VBox(20);
        if (p.vide()) {
            v.getChildren().add(Ui.carteNue(Ui.etatVideCompact(PresentationBilanBuvette.AUCUNE_VENTE)));
            return v;
        }
        // Période : tout le tournoi ou une journée.
        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        boolean trouvee = false;
        for (PresentationBilanBuvette.Periode per : p.periodes()) {
            trouvee |= java.util.Objects.equals(per.jour(), page.periodeBilan);
        }
        if (!trouvee) {
            page.periodeBilan = null;
        }
        for (PresentationBilanBuvette.Periode per : p.periodes()) {
            ToggleButton b = new ToggleButton(per.libelle());
            b.setSelected(java.util.Objects.equals(per.jour(), page.periodeBilan));
            b.setOnAction(e -> {
                page.periodeBilan = per.jour();
                page.annulationAConfirmer = 0;
                page.afficher();
            });
            boutons.add(b);
        }
        Button exportBilan = Ui.boutonSecondaire("Exporter le bilan (CSV)");
        Button exportHistorique = Ui.boutonSecondaire("Exporter l'historique (CSV)");
        BilanBuvette b = p.bilan(page.periodeBilan);
        exportBilan.setOnAction(e -> exporter("Exporter le bilan de la buvette",
                PresentationBilanBuvette.nomFichier("buvette-bilan", page.referenceTournoi(), page.periodeBilan),
                ExportCsvBuvette.bilan(b, page.nomTournoi())));
        exportHistorique.setOnAction(e -> exporter("Exporter l'historique des ventes",
                PresentationBilanBuvette.nomFichier("buvette-ventes", page.referenceTournoi(), null),
                ExportCsvBuvette.historique(ventes, page.zone)));
        javafx.scene.layout.FlowPane entete = new javafx.scene.layout.FlowPane(12, 10,
                Ui.segment(groupe, boutons.toArray(new ToggleButton[0])), exportBilan, exportHistorique);
        entete.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().add(entete);

        List<Node> tuiles = new ArrayList<>();
        for (PresentationBilanBuvette.Chiffre c : PresentationBilanBuvette.chiffres(b)) {
            tuiles.add(Ui.tuileChiffre(c.valeur(), c.libelle()));
        }
        v.getChildren().add(Ui.grilleAdaptative(4, 170, tuiles));
        v.getChildren().add(Ui.aide(PresentationBilanBuvette.complement(b)
                + ". Coût = produit + emballage, figé au moment de chaque vente. Suivi interne du club, "
                + "pas un document de caisse."));

        Node articles = tableau(List.of("Article", "Quantité", "Recette", "Coût", "Marge"),
                PresentationBilanBuvette.detailArticles(b), "Aucune vente sur cette période.");
        Node moyens = tableau(List.of("Paiement", "Ventes", "Recette"), PresentationBilanBuvette.detailMoyens(b), "");
        VBox carteArticles = Ui.carte("Par article", articles);
        VBox carteMoyens = Ui.carte("Par moyen de paiement", moyens);
        v.getChildren().add(Ui.grilleAdaptative(2, 360, List.of(carteArticles, carteMoyens)));
        v.getChildren().add(historique(p));
        return v;
    }

    /** Tableau simple : première colonne à gauche, chiffres alignés à droite. */
    private static Node tableau(List<String> entetes, List<List<String>> lignes, String vide) {
        if (lignes.isEmpty()) {
            return Ui.discret(vide);
        }
        GridPane g = new GridPane();
        g.getStyleClass().add("buvette-tableau");
        g.setHgap(16);
        g.setVgap(6);
        for (int c = 0; c < entetes.size(); c++) {
            ColumnConstraints cc = new ColumnConstraints();
            if (c == 0) {
                cc.setHgrow(Priority.ALWAYS);
            } else {
                cc.setHalignment(HPos.RIGHT);
            }
            g.getColumnConstraints().add(cc);
            g.add(Ui.enteteColonne(entetes.get(c)), c, 0);
        }
        for (int r = 0; r < lignes.size(); r++) {
            for (int c = 0; c < lignes.get(r).size(); c++) {
                Label l = c == 0 ? Ui.texte(lignes.get(r).get(c)) : Ui.gras(lignes.get(r).get(c));
                l.setWrapText(c == 0);
                g.add(l, c, r + 1);
            }
        }
        return g;
    }

    private Node historique(PresentationBilanBuvette p) {
        List<PresentationBilanBuvette.LigneHistorique> lignes = p.historique(page.periodeBilan);
        VBox liste = new VBox(4);
        boolean premiere = true;
        for (PresentationBilanBuvette.LigneHistorique l : lignes) {
            HBox droite = new HBox(8);
            droite.setAlignment(Pos.CENTER_RIGHT);
            if (!l.avertissement().isEmpty()) {
                droite.getChildren().add(Ui.pastille("Stock insuffisant", StatutPastille.ATTENTION));
            }
            if (!l.annulable()) {
                droite.getChildren().add(Ui.pastille("Annulée", StatutPastille.NEUTRE));
            } else {
                Button annuler = Ui.boutonLien("Annuler");
                annuler.getStyleClass().add("lien-danger");
                annuler.setOnAction(e -> {
                    page.annulationAConfirmer = l.numero();
                    page.afficher();
                });
                droite.getChildren().add(annuler);
            }
            String detail = l.articles() + " · " + l.total()
                    + (l.etat().isEmpty() ? "" : " · " + l.etat())
                    + (l.avertissement().isEmpty() ? "" : " · " + l.avertissement());
            HBox ligne = Ui.ligneElement(null, l.titre(), detail, droite);
            ligne.getStyleClass().add(l.annulable() ? "buvette-vente" : "buvette-vente-annulee");
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
            if (page.annulationAConfirmer == l.numero() && l.annulable()) {
                Button oui = Ui.boutonPrimaire("Annuler la vente n° " + l.numero());
                oui.setOnAction(e -> {
                    ResultatVente r = page.serviceVentes.annuler(page.stock(), page.referenceTournoi(), l.numero(),
                            page.auteur());
                    page.annulationAConfirmer = 0;
                    page.enregistrer(r.accepte(), r.explication());
                });
                Button non = Ui.boutonSecondaire("Garder la vente");
                non.setOnAction(e -> {
                    page.annulationAConfirmer = 0;
                    page.afficher();
                });
                liste.getChildren().add(Ui.encartConfirmation("Annuler la vente n° " + l.numero() + " ("
                        + l.total() + ") ? Le stock sera remis ; la vente restera visible ici, marquée annulée.",
                        oui, non));
            }
        }
        if (lignes.isEmpty()) {
            liste.getChildren().add(Ui.discret("Aucune vente sur cette période."));
        }
        return Ui.carte(Ui.titreAvecCompteur("Historique des ventes", lignes.size()), liste);
    }

    private void exporter(String titre, String nomFichier, String contenu) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titre);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tableur (CSV)", "*.csv"));
        fc.setInitialFileName(nomFichier);
        Node n = page.message;
        File f = fc.showSaveDialog(n.getScene() == null ? null : n.getScene().getWindow());
        if (f == null) {
            return;
        }
        Path cible = f.getName().toLowerCase().endsWith(".csv") ? f.toPath()
                : f.toPath().resolveSibling(f.getName() + ".csv");
        try {
            Files.writeString(cible, contenu, StandardCharsets.UTF_8);
            Ui.succes(page.message, "Export enregistré : " + cible.getFileName() + ".");
        } catch (IOException echec) {
            Ui.erreur(page.message, "Export impossible : " + echec.getMessage());
        }
    }
}
