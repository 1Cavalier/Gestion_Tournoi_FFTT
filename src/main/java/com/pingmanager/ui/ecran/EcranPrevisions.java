package com.pingmanager.ui.ecran;

import com.pingmanager.core.ComparaisonStrategies;
import com.pingmanager.core.ResultatSimulation;
import com.pingmanager.core.ScenarioSimulation;
import com.pingmanager.core.SimulateurTournoi;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
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
 * clair, enregistrée aussitôt), lance une simulation accélérée de la journée et
 * la compare à une autre stratégie.
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
        TextField graine = Ui.champ("1");
        graine.setText("1");
        graine.setPrefColumnCount(8);
        CheckBox completer = new CheckBox("Compléter les tableaux jusqu'à leurs places maximales (joueurs fictifs)");
        completer.setWrapText(true);

        ComboBox<StrategieOrdonnancement> comparer = new ComboBox<>();
        comparer.getItems().add(null);
        comparer.getItems().addAll(StrategieOrdonnancement.values());
        comparer.setCellFactory(l -> celluleStrategie());
        comparer.setButtonCell(celluleStrategie());
        comparer.getSelectionModel().selectFirst();

        Button lancer = Ui.boutonPrimaire("Lancer la simulation");
        lancer.setDefaultButton(true);
        lancer.setOnAction(e -> simuler(tables.getText(), graine.getText(), completer.isSelected(),
                comparer.getValue()));

        HBox reglages = Ui.ligne(16, Ui.champEtiquete("Tables", tables), Ui.champEtiquete("Graine", graine),
                Ui.champEtiquete("Comparer avec", comparer));
        reglages.setAlignment(Pos.BOTTOM_LEFT);
        Label avertissement = Ui.aide(PresentationPrevisions.AVERTISSEMENT
                + " Une même graine redonne exactement le même résultat.");
        avertissement.setWrapText(true);
        return Ui.carte("Simuler la journée", reglages, completer, avertissement, Ui.actions(lancer),
                messageSimulation);
    }

    private static ListCell<StrategieOrdonnancement> celluleStrategie() {
        return new ListCell<>() {
            @Override
            protected void updateItem(StrategieOrdonnancement s, boolean vide) {
                super.updateItem(s, vide);
                setText(vide ? null : (s == null ? "Aucune comparaison" : s.getLibelle()));
            }
        };
    }

    private void simuler(String saisieTables, String saisieGraine, boolean completer,
                         StrategieOrdonnancement autre) {
        Integer tables = PresentationPrevisions.lireTables(saisieTables);
        if (tables == null) {
            Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_TABLES);
            return;
        }
        Long graine = PresentationPrevisions.lireGraine(saisieGraine);
        if (graine == null) {
            Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_GRAINE);
            return;
        }
        messageSimulation.setText("");
        ScenarioSimulation scenario = SimulateurTournoi.scenarioDepuis(tournoi, tables, graine, completer);
        StrategieOrdonnancement choisie = tournoi.getStrategieOrdonnancement();
        ResultatSimulation resultat = simulateur.simuler(scenario, choisie);
        zoneResultat.getChildren().setAll(carteResultat(resultat));
        if (autre != null) {
            ComparaisonStrategies c = new ComparaisonStrategies(resultat, simulateur.simuler(scenario, autre));
            zoneResultat.getChildren().add(carteComparaison(c));
        }
        Ui.apparition(zoneResultat);
    }

    private Node carteResultat(ResultatSimulation r) {
        Label titre = Ui.titreCarte("Prévision avec « " + r.strategie().getLibelle() + " »");
        titre.setWrapText(true);
        if (r.tableaux().isEmpty()) {
            return Ui.carte(titre, Ui.etatVideCompact(PresentationPrevisions.synthese(r)));
        }
        List<PresentationPrevisions.Chiffre> chiffres = PresentationPrevisions.chiffres(r);
        Node[] tuiles = new Node[chiffres.size()];
        for (int i = 0; i < chiffres.size(); i++) {
            PresentationPrevisions.Chiffre c = chiffres.get(i);
            VBox tuile = Ui.tuileChiffre(c.valeur(), c.libelle());
            if (c.alerte()) {
                tuile.getChildren().get(0).getStyleClass().add("prevision-alerte");
            }
            tuiles[i] = tuile;
        }
        Label synthese = Ui.discret(PresentationPrevisions.synthese(r));
        synthese.setWrapText(true);

        GridPane grille = grille(6);
        String[] entetes = {"Tableau", "Début", "Fin prévue", "Fin souhaitée", "Attente moy.", "État"};
        for (int i = 0; i < entetes.length; i++) {
            grille.add(Ui.enteteColonne(entetes[i]), i, 0);
        }
        List<PresentationPrevisions.LigneTableau> lignes = PresentationPrevisions.lignes(r);
        for (int k = 0; k < lignes.size(); k++) {
            PresentationPrevisions.LigneTableau l = lignes.get(k);
            int rang = k + 1;
            Label nom = Ui.gras(l.nom());
            nom.setWrapText(true);
            grille.add(nom, 0, rang);
            grille.add(Ui.texte(l.debut()), 1, rang);
            grille.add(Ui.texte(l.fin()), 2, rang);
            grille.add(Ui.discret(l.finSouhaitee()), 3, rang);
            grille.add(Ui.texte(l.attente()), 4, rang);
            grille.add(Ui.pastille(l.etat(), l.statut()), 5, rang);
        }
        return Ui.carte(titre, Ui.rangeeTuiles(tuiles), synthese, grille);
    }

    private Node carteComparaison(ComparaisonStrategies c) {
        Label verdict = Ui.texte(PresentationPrevisions.verdict(c));
        verdict.setWrapText(true);
        GridPane grille = grille(3);
        grille.add(Ui.enteteColonne("Indicateur"), 0, 0);
        Label a = Ui.enteteColonne(c.premiere().strategie().getLibelle());
        a.setWrapText(true);
        Label b = Ui.enteteColonne(c.seconde().strategie().getLibelle());
        b.setWrapText(true);
        grille.add(a, 1, 0);
        grille.add(b, 2, 0);
        List<PresentationPrevisions.LigneComparaison> lignes = PresentationPrevisions.comparaison(c);
        for (int k = 0; k < lignes.size(); k++) {
            PresentationPrevisions.LigneComparaison l = lignes.get(k);
            int rang = k + 1;
            Label indicateur = Ui.texte(l.indicateur());
            indicateur.setWrapText(true);
            grille.add(indicateur, 0, rang);
            grille.add(valeur(l.premiere(), l.meilleure() == 1), 1, rang);
            grille.add(valeur(l.seconde(), l.meilleure() == 2), 2, rang);
        }
        Label legende = Ui.aide("En gras : la meilleure valeur. Mêmes poules, mêmes résultats et mêmes durées "
                + "de parties : seul l'ordre de lancement change.");
        legende.setWrapText(true);
        return Ui.carte("Comparaison de deux stratégies", verdict, grille, legende);
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
