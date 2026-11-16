package com.pingmanager.ui.ecran;

import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sélecteur de stratégie d'ordonnancement à trois choix : une option
 * sélectionnable par stratégie (nom, phrase d'explication, marque de la
 * stratégie active), un message, et le bouton « En savoir plus » qui ouvre
 * l'explication complète et les statistiques de comparaison.
 *
 * <p>Composant réutilisable : vue du tournoi homologué ou publié
 * ({@link EcranCompletion}) et, à terme, écran du jour J. L'appelant fournit
 * le tournoi et la façon de l'enregistrer ; mots et enregistrement viennent de
 * {@link PresentationSelecteurStrategie} (testée sans JavaFX).</p>
 */
final class SelecteurStrategie {

    private static final PseudoClass SELECTIONNEE = PseudoClass.getPseudoClass("selected");

    private final Tournoi tournoi;
    private final PresentationSelecteurStrategie.Enregistreur enregistreur;
    private Runnable enSavoirPlus;
    private Consumer<StrategieOrdonnancement> surChangement = s -> { };

    private final VBox zoneOptions = new VBox();
    private final Label message = Ui.message();

    SelecteurStrategie(Tournoi tournoi, PresentationSelecteurStrategie.Enregistreur enregistreur) {
        this.tournoi = tournoi;
        this.enregistreur = enregistreur;
    }

    /** Action du bouton « En savoir plus » ; sans action, le bouton n'est pas affiché. */
    SelecteurStrategie enSavoirPlus(Runnable action) {
        this.enSavoirPlus = action;
        return this;
    }

    /** Appelé après chaque changement enregistré (par exemple pour rafraîchir l'écran hôte). */
    SelecteurStrategie surChangement(Consumer<StrategieOrdonnancement> action) {
        this.surChangement = action == null ? s -> { } : action;
        return this;
    }

    /** @return le sélecteur : les trois options, le message, puis « En savoir plus ». */
    Node build() {
        afficherOptions();
        VBox racine = new VBox(12, zoneOptions, message);
        racine.getStyleClass().add("selecteur-strategie");
        if (enSavoirPlus != null) {
            Button plus = Ui.boutonSecondaire(PresentationSelecteurStrategie.BOUTON_EN_SAVOIR_PLUS);
            plus.getStyleClass().add("selecteur-strategie-plus");
            plus.setOnAction(e -> enSavoirPlus.run());
            racine.getChildren().add(Ui.actions(plus));
        }
        return racine;
    }

    private void afficherOptions() {
        List<Node> options = new ArrayList<>();
        for (PresentationSelecteurStrategie.Option o : PresentationSelecteurStrategie.options(tournoi)) {
            options.add(option(o));
        }
        zoneOptions.getChildren().setAll(Ui.grilleAdaptative(3, 240, options));
    }

    /**
     * Une option : carte cliquable et atteignable au clavier (Tab, puis Espace ou
     * Entrée), annoncée comme bouton radio. L'option active porte la pseudo-classe
     * {@code :selected} (fond teinté, contour et rond bleus) et sa marque.
     */
    private VBox option(PresentationSelecteurStrategie.Option o) {
        Label rond = new Label();
        rond.getStyleClass().add("option-strategie-rond");
        rond.setMinSize(18, 18);
        rond.setMaxSize(18, 18);
        Label nom = Ui.label(o.nom(), "option-strategie-nom");
        nom.setWrapText(true);
        nom.setMinHeight(Region.USE_PREF_SIZE);
        HBox entete = Ui.ligne(10, rond, nom);
        entete.setAlignment(Pos.CENTER_LEFT);
        Label phrase = Ui.label(o.phrase(), "option-strategie-phrase");
        phrase.setWrapText(true);
        phrase.setMinHeight(Region.USE_PREF_SIZE);
        VBox carte = new VBox(8, entete, phrase);
        if (o.marque() != null) {
            Region pousse = new Region();
            VBox.setVgrow(pousse, Priority.ALWAYS);
            Label marque = Ui.pastille(o.marque(), PresentationSelecteurStrategie.MARQUE_CHOISIE.equals(o.marque())
                    ? StatutPastille.INFO : StatutPastille.NEUTRE);
            carte.getChildren().addAll(pousse, marque);
        }
        carte.getStyleClass().add("option-strategie");
        carte.pseudoClassStateChanged(SELECTIONNEE, o.active());
        carte.setMaxWidth(Double.MAX_VALUE);
        carte.setMaxHeight(Double.MAX_VALUE);
        carte.setFocusTraversable(true);
        carte.setAccessibleRole(AccessibleRole.RADIO_BUTTON);
        carte.setAccessibleText(o.nom() + ". " + o.phrase() + (o.marque() == null ? "" : " " + o.marque() + "."));
        carte.setOnMouseClicked(e -> choisir(o.strategie()));
        carte.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                e.consume();
                choisir(o.strategie());
            }
        });
        return carte;
    }

    private void choisir(StrategieOrdonnancement s) {
        PresentationSelecteurStrategie.ResultatChoixStrategie r =
                PresentationSelecteurStrategie.choisir(tournoi, s, enregistreur);
        // Toujours reconstruire depuis le tournoi : un refus laisse la stratégie précédente marquée.
        afficherOptions();
        if (!r.enregistre()) {
            Ui.erreur(message, r.message());
            return;
        }
        if (!r.inchange()) {
            Ui.succes(message, r.message());
            surChangement.accept(s);
        }
    }
}
