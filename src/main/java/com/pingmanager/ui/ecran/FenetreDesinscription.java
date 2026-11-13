package com.pingmanager.ui.ecran;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.MotifRetrait;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * B043 : fenêtre « Désinscrire… » du poste de pointage : le motif (décision du
 * joueur proposée en premier, puis les autres motifs), une précision obligatoire
 * pour « Autre », et la confirmation. Textes dans {@link PresentationRetraits}.
 */
final class FenetreDesinscription {

    /** Le choix confirmé. */
    record Choix(MotifRetrait motif, String precision) {
    }

    private FenetreDesinscription() {
    }

    /**
     * Ouvre la fenêtre (modale) au-dessus de {@code proprietaire}.
     *
     * @return le motif confirmé, ou vide si l'on renonce
     */
    static Optional<Choix> demander(Node proprietaire, Joueur joueur, String tableau) {
        Dialog<ButtonType> fenetre = new Dialog<>();
        fenetre.setTitle(PresentationRetraits.TITRE_FENETRE);
        fenetre.setHeaderText(PresentationPostePointage.confirmationDesistement(joueur, tableau));
        if (proprietaire != null && proprietaire.getScene() != null) {
            fenetre.initOwner(proprietaire.getScene().getWindow());
            // Même feuille de style et même thème (clair ou sombre) que la fenêtre principale.
            fenetre.getDialogPane().getStylesheets().setAll(proprietaire.getScene().getStylesheets());
            fenetre.getDialogPane().getStyleClass().addAll(proprietaire.getScene().getRoot().getStyleClass());
        }
        fenetre.getDialogPane().getStyleClass().add("fenetre-desinscription");

        ToggleGroup groupe = new ToggleGroup();
        VBox motifs = new VBox(8);
        motifs.getStyleClass().add("desinscription-motifs");
        for (MotifRetrait m : PresentationRetraits.motifsProposes()) {
            RadioButton b = new RadioButton(m.getLibelle());
            b.setUserData(m);
            b.setToggleGroup(groupe);
            b.getStyleClass().add("desinscription-motif");
            if (m.estDecisionDuJoueur()) {
                b.setSelected(true);
                b.getStyleClass().add("desinscription-motif-joueur");
            }
            motifs.getChildren().add(b);
        }
        TextField precision = Ui.champ(PresentationRetraits.PRECISION);
        precision.getStyleClass().add("desinscription-precision");

        Label titre = Ui.label("Motif du retrait", "desinscription-titre");
        VBox contenu = new VBox(12, titre, motifs, precision);
        contenu.getStyleClass().add("desinscription-contenu");
        fenetre.getDialogPane().setContent(contenu);

        ButtonType confirmer = new ButtonType(PresentationRetraits.CONFIRMER, ButtonBar.ButtonData.OK_DONE);
        ButtonType renoncer = new ButtonType("Renoncer", ButtonBar.ButtonData.CANCEL_CLOSE);
        fenetre.getDialogPane().getButtonTypes().setAll(confirmer, renoncer);
        Node boutonConfirmer = fenetre.getDialogPane().lookupButton(confirmer);
        boutonConfirmer.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !PresentationRetraits.confirmable(motif(groupe), precision.getText()),
                groupe.selectedToggleProperty(), precision.textProperty()));

        Optional<ButtonType> r = fenetre.showAndWait();
        if (r.isEmpty() || r.get() != confirmer) {
            return Optional.empty();
        }
        return Optional.of(new Choix(motif(groupe), precision.getText()));
    }

    private static MotifRetrait motif(ToggleGroup groupe) {
        return groupe.getSelectedToggle() == null ? null : (MotifRetrait) groupe.getSelectedToggle().getUserData();
    }
}
