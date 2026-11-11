package com.pingmanager.ui.ecran;

import com.pingmanager.core.ScenarioSimulation;
import com.pingmanager.core.SimulateurTournoi;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;
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
        TextField appel = champCourt(PresentationPrevisions.APPEL);
        TextField rappelPourcent = champCourt(PresentationPrevisions.RAPPEL_POURCENT);
        TextField rappelMinutes = champCourt(PresentationPrevisions.RAPPEL_MINUTES);
        TextField echauffement = champCourt(PresentationPrevisions.ECHAUFFEMENT);
        TextField pause = champCourt(PresentationPrevisions.PAUSE_SECONDES);
        TextField tempsMort = champCourt(PresentationPrevisions.TEMPS_MORT_POURCENT);
        TextField feuille = champCourt(PresentationPrevisions.FEUILLE);
        // Une heure de fin visée par journée (les journées sont simulées séparément).
        java.util.Map<java.time.LocalDate, TextField> finsVisees = new java.util.LinkedHashMap<>();
        HBox lignesFins = Ui.ligne(16);
        lignesFins.setAlignment(Pos.BOTTOM_LEFT);
        for (java.util.Map.Entry<java.time.LocalDate, List<java.time.LocalDateTime>> j : finsTheoriquesParJour().entrySet()) {
            TextField f = champCourt(PresentationPrevisions.finViseeProposee(j.getValue()));
            f.setPrefColumnCount(5);
            finsVisees.put(j.getKey(), f);
            lignesFins.getChildren().add(Ui.champEtiquete("Fin visée – " + PresentationPrevisions.libelleJour(j.getKey()), f));
        }
        CheckBox completer = new CheckBox("Compléter les tableaux jusqu'à leurs places maximales (joueurs fictifs)");
        completer.setWrapText(true);

        Button lancer = Ui.boutonPrimaire("Simuler les trois stratégies");
        lancer.setDefaultButton(true);
        lancer.setOnAction(e -> {
            PresentationPrevisions.ReglageAnnexes annexes = PresentationPrevisions.lireTempsAnnexes(appel.getText(),
                    rappelPourcent.getText(), rappelMinutes.getText(), echauffement.getText(), pause.getText(),
                    tempsMort.getText(), feuille.getText());
            if (annexes.erreur() != null) {
                Ui.erreur(messageSimulation, annexes.erreur());
                return;
            }
            java.util.Map<java.time.LocalDate, java.time.LocalTime> fins = new java.util.HashMap<>();
            for (java.util.Map.Entry<java.time.LocalDate, TextField> f : finsVisees.entrySet()) {
                java.time.LocalTime h = PresentationPrevisions.lireFinVisee(f.getValue().getText());
                if (h == null) {
                    Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_FIN_VISEE);
                    return;
                }
                fins.put(f.getKey(), h);
            }
            simuler(lancer, tables.getText(), tirages.getText(), moyenne.getText(), minimum.getText(),
                    maximum.getText(), graine.getText(), completer.isSelected(), annexes.temps(), fins);
        });

        HBox reglages = Ui.ligne(16, Ui.champEtiquete("Tables", tables), Ui.champEtiquete("Tirages", tirages),
                Ui.champEtiquete("Graine", graine));
        reglages.setAlignment(Pos.BOTTOM_LEFT);
        HBox durees = Ui.ligne(16, Ui.champEtiquete("Durée moyenne du jeu (min)", moyenne),
                Ui.champEtiquete("Minimum (min)", minimum), Ui.champEtiquete("Maximum (min)", maximum));
        durees.setAlignment(Pos.BOTTOM_LEFT);
        Label aideDurees = Ui.aide("Durée du jeu seul, pour un match en cinq manches ; trois manches : deux tiers, sept manches : "
                + "quatre tiers. La moyenne doit être plus proche du minimum que du maximum.");
        aideDurees.setWrapText(true);
        HBox autour1 = Ui.ligne(16, Ui.champEtiquete("Appel (min)", appel),
                Ui.champEtiquete("Joueur à rappeler (%)", rappelPourcent),
                Ui.champEtiquete("Retard d'un rappel (min)", rappelMinutes),
                Ui.champEtiquete("Échauffement (min)", echauffement));
        autour1.setAlignment(Pos.BOTTOM_LEFT);
        HBox autour2 = Ui.ligne(16, Ui.champEtiquete("Pause entre manches (s)", pause),
                Ui.champEtiquete("Temps mort pris (% par joueur)", tempsMort),
                Ui.champEtiquete("Retour de la feuille (min)", feuille));
        autour2.setAlignment(Pos.BOTTOM_LEFT);
        Label aideAutour = Ui.aide("Temps autour du match, ajoutés à la durée du jeu : la table et les joueurs "
                + "restent occupés. Échauffement de 2 min avant la partie (3.4.3.1), pause d'une minute au plus "
                + "entre deux manches (3.4.4.1.1), un temps mort d'une minute au plus par joueur (3.4.4.2). "
                + "Un joueur qui n'entend pas son nom doit être rappelé.");
        aideAutour.setWrapText(true);
        Label avertissement = Ui.aide(PresentationPrevisions.AVERTISSEMENT_STATISTIQUE
                + " Une même graine redonne exactement les mêmes statistiques.");
        avertissement.setWrapText(true);
        Label aideFins = Ui.aide("Chaque journée est simulée seule. Un tableau « déborde » s'il finit après "
                + "l'heure visée de sa journée : changez-la pour voir l'effet sur le risque.");
        aideFins.setWrapText(true);
        return Ui.carte("Simuler les journées", reglages, lignesFins, aideFins, durees, aideDurees, Ui.titreCarte("Temps autour du match"),
                autour1, autour2, aideAutour, completer, avertissement,
                Ui.actions(lancer), messageSimulation);
    }

    /** Journées du tournoi (jour de début des matchs) et fins théoriques de leurs tableaux. */
    private java.util.SortedMap<java.time.LocalDate, List<java.time.LocalDateTime>> finsTheoriquesParJour() {
        java.util.SortedMap<java.time.LocalDate, List<java.time.LocalDateTime>> jours = new java.util.TreeMap<>();
        for (ScenarioSimulation.TableauSimule t : SimulateurTournoi.scenarioDepuis(tournoi, 1, 1, false).getTableaux()) {
            jours.computeIfAbsent(t.debut().toLocalDate(), k -> new ArrayList<>()).add(t.finSouhaitee());
        }
        return jours;
    }

    private static TextField champCourt(String valeur) {
        TextField t = Ui.champ(valeur);
        t.setText(valeur);
        t.setPrefColumnCount(4);
        return t;
    }

    private static TextField champMinutes(int valeur) {
        TextField t = Ui.champ(String.valueOf(valeur));
        t.setText(String.valueOf(valeur));
        t.setPrefColumnCount(4);
        return t;
    }

    private void simuler(Button lancer, String saisieTables, String saisieTirages, String saisieMoyenne,
                         String saisieMinimum, String saisieMaximum, String saisieGraine, boolean completer,
                         TempsAnnexesMatch annexes,
                         java.util.Map<java.time.LocalDate, java.time.LocalTime> finsVisees) {
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
                .loiDuree(durees.loi()).tempsAnnexes(annexes).heureLimite(base.getHeureLimite());
        base.getTableaux().forEach(b::tableau);
        ScenarioSimulation scenario = b.build();
        StrategieOrdonnancement choisie = tournoi.getStrategieOrdonnancement();
        // Une journée à la fois, chacune avec son heure de fin visée.
        java.util.SortedMap<java.time.LocalDate, ScenarioSimulation> jours = new java.util.TreeMap<>();
        for (java.util.Map.Entry<java.time.LocalDate, ScenarioSimulation> j : scenario.parJour().entrySet()) {
            java.time.LocalTime fin = finsVisees.getOrDefault(j.getKey(),
                    PresentationPrevisions.lireFinVisee(PresentationPrevisions.FIN_VISEE_PAR_DEFAUT));
            jours.put(j.getKey(), j.getValue().avecFinVisee(fin));
        }

        // Plusieurs centaines de tournois rejoués : calcul hors du fil de l'interface.
        lancer.setDisable(true);
        Ui.succes(messageSimulation, "Simulation en cours (" + jours.size() + " journée(s), " + tirages
                + " tirages × 3 stratégies)…");
        Task<java.util.SortedMap<java.time.LocalDate, List<StatistiquesSimulation>>> calcul = new Task<>() {
            @Override
            protected java.util.SortedMap<java.time.LocalDate, List<StatistiquesSimulation>> call() {
                java.util.SortedMap<java.time.LocalDate, List<StatistiquesSimulation>> r = new java.util.TreeMap<>();
                for (java.util.Map.Entry<java.time.LocalDate, ScenarioSimulation> j : jours.entrySet()) {
                    r.put(j.getKey(), simulateur.comparerToutes(j.getValue(), tirages));
                }
                return r;
            }
        };
        calcul.setOnSucceeded(e -> {
            lancer.setDisable(false);
            messageSimulation.setText("");
            afficherJournees(calcul.getValue(), choisie, durees.loi());
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
        for (int i = 1; i <= stats.size(); i++) {
            grille.getColumnConstraints().get(i).setMinWidth(170);
            grille.getColumnConstraints().get(i).setPercentWidth(20);
        }
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

    /** Une journée à la fois (boutons s'il y en a plusieurs) : comparaison, puis tableau par tableau. */
    private void afficherJournees(java.util.SortedMap<java.time.LocalDate, List<StatistiquesSimulation>> parJour,
                                  StrategieOrdonnancement choisie, LoiDureeMatch loi) {
        zoneResultat.getChildren().clear();
        if (parJour.isEmpty()) {
            zoneResultat.getChildren().add(Ui.carte(Ui.etatVideCompact("Aucun tableau à simuler.")));
            return;
        }
        VBox detail = new VBox(16);
        if (parJour.size() > 1) {
            ToggleGroup groupe = new ToggleGroup();
            List<ToggleButton> boutons = new ArrayList<>();
            for (java.time.LocalDate jour : parJour.keySet()) {
                ToggleButton b = new ToggleButton(PresentationPrevisions.libelleJour(jour));
                b.setUserData(jour);
                boutons.add(b);
            }
            groupe.selectedToggleProperty().addListener((obs, avant, apres) -> {
                if (apres == null) {
                    avant.setSelected(true); // toujours une journée affichée
                    return;
                }
                List<StatistiquesSimulation> stats = parJour.get((java.time.LocalDate) apres.getUserData());
                detail.getChildren().setAll(carteStrategies(stats, choisie, loi), carteParTableau(stats, choisie));
            });
            zoneResultat.getChildren().addAll(Ui.segment(groupe, boutons.toArray(new ToggleButton[0])), detail);
            boutons.get(0).setSelected(true);
        } else {
            List<StatistiquesSimulation> stats = parJour.values().iterator().next();
            detail.getChildren().setAll(carteStrategies(stats, choisie, loi), carteParTableau(stats, choisie));
            zoneResultat.getChildren().add(detail);
        }
    }

    /**
     * Chaque tableau, les trois stratégies côte à côte : un indicateur à la fois
     * (boutons), une seule valeur par case, la meilleure en gras.
     */
    private Node carteParTableau(List<StatistiquesSimulation> stats, StrategieOrdonnancement choisie) {
        if (stats.isEmpty() || stats.get(0).tableaux().isEmpty()) {
            return Ui.carte(Ui.titreCarte("Par tableau"), Ui.etatVideCompact("Aucun tableau simulé."));
        }
        VBox detail = new VBox(8);
        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        for (PresentationPrevisions.IndicateurTableau ind : PresentationPrevisions.IndicateurTableau.values()) {
            ToggleButton b = new ToggleButton(ind.libelle());
            b.setUserData(ind);
            boutons.add(b);
        }
        groupe.selectedToggleProperty().addListener((obs, avant, apres) -> {
            if (apres == null) {
                avant.setSelected(true); // toujours un indicateur affiché
                return;
            }
            PresentationPrevisions.IndicateurTableau ind = (PresentationPrevisions.IndicateurTableau) apres.getUserData();
            Label explication = Ui.aide(ind.explication());
            explication.setWrapText(true);
            detail.getChildren().setAll(explication, grilleIndicateur(stats, choisie, ind));
        });
        HBox choix = Ui.segment(groupe, boutons.toArray(new ToggleButton[0]));
        boutons.get(0).setSelected(true);
        return Ui.carte(Ui.titreCarte("Par tableau : les trois stratégies côte à côte"), choix, detail);
    }

    private Node grilleIndicateur(List<StatistiquesSimulation> stats, StrategieOrdonnancement choisie,
                                  PresentationPrevisions.IndicateurTableau ind) {
        GridPane grille = grille(stats.size() + 2);
        // Largeurs minimales : heures sur une ligne, colonnes des stratégies égales.
        grille.getColumnConstraints().get(1).setMinWidth(90);
        for (int i = 2; i < stats.size() + 2; i++) {
            grille.getColumnConstraints().get(i).setMinWidth(170);
            grille.getColumnConstraints().get(i).setPercentWidth(22);
        }
        grille.add(Ui.enteteColonne("Tableau"), 0, 0);
        grille.add(Ui.enteteColonne("Début prévu"), 1, 0);
        for (int i = 0; i < stats.size(); i++) {
            StrategieOrdonnancement s = stats.get(i).strategie();
            Label e = Ui.enteteColonne(s.getLibelle() + (s == choisie ? " (votre choix)" : ""));
            e.setWrapText(true);
            grille.add(e, i + 2, 0);
        }
        List<PresentationPrevisions.LigneIndicateur> lignes = PresentationPrevisions.grilleIndicateur(stats, ind);
        for (int k = 0; k < lignes.size(); k++) {
            PresentationPrevisions.LigneIndicateur l = lignes.get(k);
            int rang = k + 1;
            Label nom = Ui.gras(l.nom());
            nom.setWrapText(true);
            grille.add(nom, 0, rang);
            grille.add(Ui.discret(l.debutPrevu()), 1, rang);
            for (int i = 0; i < l.valeurs().size(); i++) {
                Node v;
                if (l.statuts() != null) {
                    Label p = Ui.pastille(l.valeurs().get(i), l.statuts().get(i));
                    if (l.meilleure() == i) {
                        p.getStyleClass().add("prevision-meilleure");
                    }
                    v = p;
                } else {
                    v = valeur(l.valeurs().get(i), l.meilleure() == i);
                }
                grille.add(v, i + 2, rang);
            }
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
