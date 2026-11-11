package com.pingmanager.ui.ecran;

import com.pingmanager.core.ScenarioSimulation;
import com.pingmanager.core.SimulateurTournoi;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ui.ContexteApplication;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Écran « Prévisions et stratégie » (B025), ouvert depuis la deuxième version
 * de l'écran du tournoi : le club choisit sa stratégie principale (expliquée en
 * clair, enregistrée aussitôt) et lance une simulation statistique de la
 * journée : le tournoi est rejoué de nombreuses fois (durées de match tirées au
 * hasard entre un minimum et un maximum) avec chacune des trois stratégies.
 *
 * <p>Mots, chiffres et couleurs viennent de {@link PresentationPrevisions}
 * (testée sans JavaFX) ; le calcul, de {@link SimulateurTournoi}.</p>
 */
final class EcranPrevisions {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Tournoi tournoi;
    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    private final BorderPane racine = new BorderPane();
    private final VBox zoneResultat = new VBox(16);
    private final Label messageChoix = Ui.message();
    private final Label messageSimulation = Ui.message();
    private final List<VBox> cartesChoix = new ArrayList<>();

    EcranPrevisions(ContexteApplication contexte, Path fichier, Runnable onRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.tournoi = contexte.chargerTournoi(fichier);
    }

    Node build() {
        Button retour = Ui.boutonLien("← Retour au tournoi");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre(PresentationPrevisions.TITRE);
        Label sousTitre = Ui.discret(tournoi.getNom() == null ? "" : tournoi.getNom());
        VBox contenu = Ui.page(1040, new VBox(6, retour, titre, sousTitre), carteStrategie(), carteSimulation(),
                zoneResultat);
        racine.setCenter(Ui.defilant(contenu));
        return racine;
    }

    // ------------------------------------------------------------------
    // Choix de la stratégie
    // ------------------------------------------------------------------

    private Node carteStrategie() {
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.STRATEGIE, false),
                Ui.titreCarte("Stratégie principale du club"));
        entete.setAlignment(Pos.CENTER_LEFT);
        Label aide = Ui.aide("Elle décide de l'ordre des matchs proposés en salle le jour J (et de leurs couleurs "
                + "de priorité). Vous pouvez en changer à tout moment.");
        aide.setWrapText(true);
        ToggleGroup groupe = new ToggleGroup();
        VBox liste = new VBox(10);
        for (PresentationPrevisions.ChoixStrategie c : PresentationPrevisions.choix(tournoi)) {
            RadioButton radio = new RadioButton(c.libelle());
            radio.getStyleClass().add("choix-strategie-titre");
            radio.setToggleGroup(groupe);
            radio.setSelected(c.selectionnee());
            radio.setUserData(c.strategie());
            Label explication = Ui.texte(c.explication());
            explication.setWrapText(true);
            Label contrepartie = Ui.discret(c.contrepartie());
            contrepartie.setWrapText(true);
            VBox carte = new VBox(6, radio, explication, contrepartie);
            carte.getStyleClass().add("choix-strategie");
            carte.setUserData(c.strategie());
            if (c.selectionnee()) {
                carte.getStyleClass().add("choix-strategie-active");
            }
            carte.setOnMouseClicked(e -> radio.setSelected(true));
            cartesChoix.add(carte);
            liste.getChildren().add(carte);
        }
        Label phrase = Ui.discret(PresentationPrevisions.phraseStrategie(tournoi));
        groupe.selectedToggleProperty().addListener((obs, avant, apres) -> {
            if (apres == null) {
                return;
            }
            StrategieOrdonnancement s = (StrategieOrdonnancement) apres.getUserData();
            tournoi.setStrategieOrdonnancement(s);
            contexte.enregistrerTournoi(tournoi, fichier);
            for (VBox carte : cartesChoix) {
                carte.getStyleClass().remove("choix-strategie-active");
                if (carte.getUserData() == s) {
                    carte.getStyleClass().add("choix-strategie-active");
                }
            }
            phrase.setText(PresentationPrevisions.phraseStrategie(tournoi));
            Ui.succes(messageChoix, PresentationPrevisions.messageChoixEnregistre(s));
        });
        return Ui.carte(entete, aide, liste, phrase, messageChoix);
    }

    // ------------------------------------------------------------------
    // Simulation
    // ------------------------------------------------------------------

    private Node carteSimulation() {
        TextField tables = Ui.champ("16");
        tables.setText(String.valueOf(PresentationPrevisions.tablesProposees(tournoi)));
        tables.setPrefColumnCount(5);
        TextField tirages = Ui.champ(String.valueOf(PresentationPrevisions.TIRAGES_PAR_DEFAUT));
        tirages.setText(String.valueOf(PresentationPrevisions.TIRAGES_PAR_DEFAUT));
        tirages.setPrefColumnCount(5);
        TextField moyenne = champMinutes(PresentationPrevisions.DUREE_MOYENNE);
        TextField minimum = champMinutes(PresentationPrevisions.DUREE_MINIMUM);
        TextField maximum = champMinutes(PresentationPrevisions.DUREE_MAXIMUM);
        TextField graine = Ui.champ("1");
        graine.setText("1");
        graine.setPrefColumnCount(8);
        CheckBox completer = new CheckBox("Compléter les tableaux jusqu'à leurs places maximales (joueurs fictifs)");
        completer.setWrapText(true);

        Button lancer = Ui.boutonPrimaire("Simuler les trois stratégies");
        lancer.setDefaultButton(true);
        lancer.setOnAction(e -> simuler(lancer, tables.getText(), tirages.getText(), moyenne.getText(),
                minimum.getText(), maximum.getText(), graine.getText(), completer.isSelected()));

        HBox reglages = Ui.ligne(16, Ui.champEtiquete("Tables", tables), Ui.champEtiquete("Tirages", tirages),
                Ui.champEtiquete("Graine", graine));
        reglages.setAlignment(Pos.BOTTOM_LEFT);
        HBox durees = Ui.ligne(16, Ui.champEtiquete("Durée moyenne d'un match (min)", moyenne),
                Ui.champEtiquete("Minimum (min)", minimum), Ui.champEtiquete("Maximum (min)", maximum));
        durees.setAlignment(Pos.BOTTOM_LEFT);
        Label aideDurees = Ui.aide("Pour un match en cinq manches ; trois manches : deux tiers, sept manches : "
                + "quatre tiers. La moyenne doit être plus proche du minimum que du maximum.");
        aideDurees.setWrapText(true);
        Label avertissement = Ui.aide(PresentationPrevisions.AVERTISSEMENT_STATISTIQUE
                + " Une même graine redonne exactement les mêmes statistiques.");
        avertissement.setWrapText(true);
        return Ui.carte("Simuler la journée", reglages, durees, aideDurees, completer, avertissement,
                Ui.actions(lancer), messageSimulation);
    }

    private static TextField champMinutes(int valeur) {
        TextField t = Ui.champ(String.valueOf(valeur));
        t.setText(String.valueOf(valeur));
        t.setPrefColumnCount(4);
        return t;
    }

    private void simuler(Button lancer, String saisieTables, String saisieTirages, String saisieMoyenne,
                         String saisieMinimum, String saisieMaximum, String saisieGraine, boolean completer) {
        Integer tables = PresentationPrevisions.lireTables(saisieTables);
        if (tables == null) {
            Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_TABLES);
            return;
        }
        Integer tirages = PresentationPrevisions.lireTirages(saisieTirages);
        if (tirages == null) {
            Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_TIRAGES);
            return;
        }
        PresentationPrevisions.ReglageDurees durees =
                PresentationPrevisions.lireDurees(saisieMoyenne, saisieMinimum, saisieMaximum);
        if (durees.erreur() != null) {
            Ui.erreur(messageSimulation, durees.erreur());
            return;
        }
        Long graine = PresentationPrevisions.lireGraine(saisieGraine);
        if (graine == null) {
            Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_GRAINE);
            return;
        }
        ScenarioSimulation base = SimulateurTournoi.scenarioDepuis(tournoi, tables, graine, completer);
        ScenarioSimulation.Builder b = ScenarioSimulation.builder().nombreDeTables(tables).graine(graine)
                .loiDuree(durees.loi()).heureLimite(base.getHeureLimite());
        base.getTableaux().forEach(b::tableau);
        ScenarioSimulation scenario = b.build();
        StrategieOrdonnancement choisie = tournoi.getStrategieOrdonnancement();

        // Plusieurs centaines de tournois rejoués : calcul hors du fil de l'interface.
        lancer.setDisable(true);
        Ui.succes(messageSimulation, "Simulation en cours (" + tirages + " tirages × 3 stratégies)…");
        Task<List<StatistiquesSimulation>> calcul = new Task<>() {
            @Override
            protected List<StatistiquesSimulation> call() {
                return simulateur.comparerToutes(scenario, tirages);
            }
        };
        calcul.setOnSucceeded(e -> {
            lancer.setDisable(false);
            messageSimulation.setText("");
            List<StatistiquesSimulation> stats = calcul.getValue();
            zoneResultat.getChildren().setAll(carteStrategies(stats, choisie, durees.loi()),
                    carteParTableau(stats, choisie));
            Ui.apparition(zoneResultat);
        });
        calcul.setOnFailed(e -> {
            lancer.setDisable(false);
            Ui.erreur(messageSimulation, "La simulation a échoué : " + calcul.getException().getMessage());
        });
        Thread fil = new Thread(calcul, "simulation-previsions");
        fil.setDaemon(true);
        fil.start();
    }

    private Node carteStrategies(List<StatistiquesSimulation> stats, StrategieOrdonnancement choisie,
                                 LoiDureeMatch loi) {
        Label verdict = Ui.texte(PresentationPrevisions.verdictStrategies(stats, choisie));
        verdict.setWrapText(true);
        GridPane grille = grille(stats.size() + 1);
        grille.add(Ui.enteteColonne("Indicateur"), 0, 0);
        for (int i = 0; i < stats.size(); i++) {
            StrategieOrdonnancement s = stats.get(i).strategie();
            Label entete = Ui.enteteColonne(s.getLibelle() + (s == choisie ? " (votre choix)" : ""));
            entete.setWrapText(true);
            grille.add(entete, i + 1, 0);
        }
        List<PresentationPrevisions.LigneStrategies> lignes = PresentationPrevisions.lignesStrategies(stats);
        for (int k = 0; k < lignes.size(); k++) {
            PresentationPrevisions.LigneStrategies l = lignes.get(k);
            Label indicateur = Ui.texte(l.indicateur());
            indicateur.setWrapText(true);
            grille.add(indicateur, 0, k + 1);
            for (int i = 0; i < l.valeurs().size(); i++) {
                grille.add(valeur(l.valeurs().get(i), l.meilleure() == i), i + 1, k + 1);
            }
        }
        Label synthese = Ui.discret(PresentationPrevisions.syntheseStatistique(stats, loi));
        synthese.setWrapText(true);
        Label legende = Ui.aide("En gras : la meilleure valeur. Un tableau a vraiment commencé quand sa dernière "
                + "poule a commencé. Cas rare : 1 tirage sur 20 finit plus tôt (ou plus tard) que cette heure.");
        legende.setWrapText(true);
        return Ui.carte(Ui.titreCarte("Comparaison des stratégies"), verdict, grille, synthese, legende);
    }

    /** Détail par tableau, une stratégie à la fois (choix par boutons, celle du club d'abord). */
    private Node carteParTableau(List<StatistiquesSimulation> stats, StrategieOrdonnancement choisie) {
        VBox detail = new VBox();
        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        for (StatistiquesSimulation s : stats) {
            ToggleButton b = new ToggleButton(s.strategie().getLibelle());
            b.setUserData(s);
            boutons.add(b);
        }
        HBox choix = Ui.segment(groupe, boutons.toArray(new ToggleButton[0]));
        groupe.selectedToggleProperty().addListener((obs, avant, apres) -> {
            if (apres == null) {
                avant.setSelected(true); // toujours une stratégie affichée
                return;
            }
            detail.getChildren().setAll(grilleTableaux((StatistiquesSimulation) apres.getUserData()));
        });
        for (ToggleButton b : boutons) {
            if (((StatistiquesSimulation) b.getUserData()).strategie() == choisie) {
                b.setSelected(true);
            }
        }
        if (groupe.getSelectedToggle() == null && !boutons.isEmpty()) {
            boutons.get(0).setSelected(true);
        }
        return Ui.carte(Ui.titreCarte("Par tableau"), choix, detail);
    }

    private Node grilleTableaux(StatistiquesSimulation s) {
        List<PresentationPrevisions.LigneTableauStat> lignes = PresentationPrevisions.lignesTableaux(s);
        if (lignes.isEmpty()) {
            return Ui.etatVideCompact("Aucun tableau simulé.");
        }
        List<String> entetes = PresentationPrevisions.COLONNES_TABLEAUX;
        GridPane grille = grille(entetes.size());
        for (int i = 0; i < entetes.size(); i++) {
            Label e = Ui.enteteColonne(entetes.get(i));
            e.setWrapText(true);
            grille.add(e, i, 0);
        }
        for (int k = 0; k < lignes.size(); k++) {
            PresentationPrevisions.LigneTableauStat l = lignes.get(k);
            int rang = k + 1;
            Label nom = Ui.gras(l.nom());
            nom.setWrapText(true);
            grille.add(nom, 0, rang);
            grille.add(Ui.texte(l.debut()), 1, rang);
            grille.add(Ui.texte(l.finMoyenne()), 2, rang);
            grille.add(Ui.texte(l.finTard()), 3, rang);
            grille.add(Ui.discret(l.finSouhaitee()), 4, rang);
            grille.add(Ui.texte(l.attente()), 5, rang);
            grille.add(Ui.pastille(l.risque(), l.statut()), 6, rang);
        }
        return grille;
    }

    private static Label valeur(String texte, boolean meilleure) {
        Label l = meilleure ? Ui.gras(texte) : Ui.texte(texte);
        if (meilleure) {
            l.getStyleClass().add("prevision-meilleure");
        }
        return l;
    }

    private static GridPane grille(int colonnes) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.getStyleClass().add("prevision-grille");
        for (int i = 0; i < colonnes; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(i == 0 ? Priority.ALWAYS : Priority.SOMETIMES);
            g.getColumnConstraints().add(col);
        }
        return g;
    }
}
