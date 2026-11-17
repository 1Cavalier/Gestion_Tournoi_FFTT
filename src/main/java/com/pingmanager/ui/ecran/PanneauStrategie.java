package com.pingmanager.ui.ecran;

import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panneau « Stratégie » du jour J (B056), ouvert par la carte « Stratégie en
 * place » à la place de l'écran : une carte par stratégie, de même taille
 * (quand la choisir, ce qu'elle fait en salle, avantage, inconvénient), un
 * bouton explicite avec confirmation pour en changer, puis une zone à part
 * « Comparer les stratégies » qui ouvre les prévisions chiffrées.
 *
 * <p>L'enregistrement passe par {@link PresentationSelecteurStrategie#choisir}
 * (le même que le sélecteur de la vue du tournoi, inchangé) : un refus remet la
 * stratégie précédente. Mots : {@link TextesPanneauxJourJ}. L'instance est
 * gardée par l'écran hôte : la confirmation en cours survit aux
 * rafraîchissements.</p>
 */
final class PanneauStrategie {

    private static final PseudoClass SELECTIONNEE = PseudoClass.getPseudoClass("selected");

    private final Tournoi tournoi;
    private final PresentationSelecteurStrategie.Enregistreur enregistreur;
    private final Runnable fermer;
    private final Runnable rafraichir;
    private Consumer<PresentationSelecteurStrategie.ResultatChoixStrategie> apresChoix = r -> { };
    private Runnable comparer;
    private boolean ferme;

    /** Stratégie dont le changement attend une confirmation, ou {@code null}. */
    private StrategieOrdonnancement aConfirmer;

    PanneauStrategie(Tournoi tournoi, PresentationSelecteurStrategie.Enregistreur enregistreur, Runnable fermer,
                     Runnable rafraichir) {
        this.tournoi = tournoi;
        this.enregistreur = enregistreur;
        this.fermer = fermer;
        this.rafraichir = rafraichir;
    }

    /** Appelé après chaque choix (enregistré ou refusé), avant le rafraîchissement. */
    PanneauStrategie apresChoix(Consumer<PresentationSelecteurStrategie.ResultatChoixStrategie> action) {
        this.apresChoix = action == null ? r -> { } : action;
        return this;
    }

    /** Ouvre la comparaison chiffrée ; sans action, la zone ne montre pas de bouton. */
    PanneauStrategie avecComparaison(Runnable action) {
        this.comparer = action;
        return this;
    }

    /** Tournoi annulé : cartes consultables, boutons inactifs. */
    PanneauStrategie ferme(boolean ferme) {
        this.ferme = ferme;
        return this;
    }

    Node build() {
        List<Node> cartes = new ArrayList<>();
        for (PresentationPanneauxJourJ.CarteStrategie c : PresentationPanneauxJourJ.cartesStrategie(tournoi)) {
            cartes.add(carte(c));
        }
        VBox panneau = new VBox(20,
                PanneauAnnonces.enTete(TextesPanneauxJourJ.STRATEGIE_TITRE, TextesPanneauxJourJ.STRATEGIE_PHRASE,
                        fermer),
                Ui.grilleAdaptative(3, 280, cartes),
                zoneComparer());
        panneau.getStyleClass().addAll("panneau-jourj", "panneau-strategie");
        return panneau;
    }

    private Node carte(PresentationPanneauxJourJ.CarteStrategie c) {
        Label nom = PanneauAnnonces.texte(c.nom(), "panneau-strategie-nom");
        VBox carte = new VBox(12, nom);
        if (c.marque() != null) {
            FlowPane marque = new FlowPane(Ui.pastille(c.marque(), StatutPastille.INFO));
            carte.getChildren().add(marque);
        }
        VBox quand = new VBox(4, intertitre(TextesPanneauxJourJ.STRATEGIE_QUAND),
                ligne(TextesPanneauxJourJ.STRATEGIE_TOURNOI, c.typeTournoi()),
                ligne(TextesPanneauxJourJ.STRATEGIE_TABLES, c.tables()),
                ligne(TextesPanneauxJourJ.STRATEGIE_OBJECTIF, c.objectif()));
        carte.getChildren().addAll(quand,
                rubrique(TextesPanneauxJourJ.STRATEGIE_EN_SALLE, c.enSalle(), "panneau-strategie-salle"),
                rubrique(TextesPanneauxJourJ.STRATEGIE_AVANTAGE, c.avantage(), "panneau-strategie-avantage"),
                rubrique(TextesPanneauxJourJ.STRATEGIE_INCONVENIENT, c.inconvenient(),
                        "panneau-strategie-inconvenient"));
        Region pousse = new Region();
        VBox.setVgrow(pousse, Priority.ALWAYS);
        carte.getChildren().add(pousse);

        if (c.strategie() == aConfirmer && !ferme) {
            Button oui = Ui.boutonPrimaire(TextesPanneauxJourJ.STRATEGIE_OUI);
            oui.setOnAction(e -> appliquer(c.strategie()));
            Button non = Ui.boutonLien(TextesPanneauxJourJ.STRATEGIE_ANNULER);
            non.setOnAction(e -> {
                aConfirmer = null;
                rafraichir.run();
            });
            carte.getChildren().add(Ui.encartConfirmation(
                    TextesPanneauxJourJ.confirmationChangement(c.strategie()), oui, non));
        } else {
            Button bouton = c.active() && !c.boutonActif() ? Ui.boutonSecondaire(c.bouton())
                    : Ui.boutonPrimaire(c.bouton());
            bouton.setMaxWidth(Double.MAX_VALUE);
            bouton.setDisable(ferme || !c.boutonActif());
            bouton.setOnAction(e -> {
                if (c.aConfirmer()) {
                    aConfirmer = c.strategie();
                    rafraichir.run();
                } else {
                    appliquer(c.strategie());
                }
            });
            carte.getChildren().add(bouton);
        }
        carte.getStyleClass().addAll("carte", "panneau-strategie-carte");
        carte.pseudoClassStateChanged(SELECTIONNEE, c.active());
        carte.setMaxWidth(Double.MAX_VALUE);
        carte.setMaxHeight(Double.MAX_VALUE);
        return carte;
    }

    private void appliquer(StrategieOrdonnancement s) {
        aConfirmer = null;
        apresChoix.accept(PresentationSelecteurStrategie.choisir(tournoi, s, enregistreur));
        rafraichir.run();
    }

    private Node zoneComparer() {
        VBox carte = Ui.carte(Ui.titreCarte(TextesPanneauxJourJ.COMPARER_TITRE),
                PanneauAnnonces.texte(TextesPanneauxJourJ.COMPARER_AIDE, "panneau-bloc-explication"));
        if (comparer != null) {
            Button voir = Ui.boutonSecondaire(TextesPanneauxJourJ.COMPARER_BOUTON);
            voir.setOnAction(e -> comparer.run());
            carte.getChildren().add(Ui.actions(voir));
        }
        carte.getStyleClass().add("panneau-strategie-comparer");
        return carte;
    }

    private static Label intertitre(String s) {
        Label l = Ui.label(s, "panneau-strategie-intertitre");
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    private static Node ligne(String libelle, String valeur) {
        return PanneauAnnonces.texte(libelle + " : " + valeur, "panneau-strategie-texte");
    }

    private static Node rubrique(String titre, String texte, String classe) {
        VBox v = new VBox(4, intertitre(titre), PanneauAnnonces.texte(texte, "panneau-strategie-texte"));
        v.getStyleClass().add(classe);
        return v;
    }
}
