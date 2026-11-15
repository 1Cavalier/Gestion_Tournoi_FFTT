package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.PrevisionTournoi;
import com.pingmanager.core.PrevisionTournoi.EffectifTableau;
import com.pingmanager.core.PrevisionTournoi.PrevisionJournee;
import com.pingmanager.core.ReglagesPrevision;
import com.pingmanager.core.RemplissageSimulation;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Écran « Prévisions et stratégie » (B025, refait en B050), ouvert depuis
 * l'écran du tournoi. De haut en bas : une micro-description, les trois
 * stratégies en cartes côte à côte (bouton « Choisir cette stratégie » sous
 * chacune, choix enregistré aussitôt), puis les prévisions : explication du
 * calcul, réglages par groupes (tables, durées, fin visée jour et heure, temps
 * autour du match, remplissage des tableaux) et statistiques des trois
 * stratégies côte à côte, par thème.
 *
 * <p>Mots, chiffres et lectures de saisie viennent de
 * {@link PresentationPrevisions} (testée sans JavaFX) ; le calcul, de
 * {@link SimulateurTournoi#prevoir}, lancé hors du fil de l'interface.</p>
 */
final class EcranPrevisions {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Tournoi tournoi;
    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    private final BorderPane racine = new BorderPane();
    private final GridPane zoneCartes = new GridPane();
    private final VBox zoneResultat = new VBox(16);
    private final Label messageChoix = Ui.message();
    private final Label messageSimulation = Ui.message();

    /** Dernière prévision affichée (pour remettre en avant la stratégie après un nouveau choix). */
    private PrevisionTournoi dernierePrevision;
    private LoiDureeMatch derniereLoi;

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
        Label micro = Ui.label(PresentationPrevisions.MICRO_DESCRIPTION, "prevision-micro");
        micro.setWrapText(true);
        for (int i = 0; i < StrategieOrdonnancement.values().length; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / StrategieOrdonnancement.values().length);
            zoneCartes.getColumnConstraints().add(col);
        }
        RowConstraints rangCartes = new RowConstraints();
        rangCartes.setVgrow(Priority.ALWAYS);
        zoneCartes.getRowConstraints().add(rangCartes);
        zoneCartes.setHgap(16);
        zoneCartes.setVgap(10);
        afficherCartes();
        VBox contenu = Ui.page(1040, new VBox(6, retour, titre, sousTitre, micro), zoneCartes, messageChoix,
                Ui.titreSection(PresentationPrevisions.TITRE_PREVISIONS), carteExplication(), carteReglages(),
                zoneResultat);
        racine.setCenter(Ui.defilant(contenu));
        return racine;
    }

    // ------------------------------------------------------------------
    // Les trois cartes de stratégie
    // ------------------------------------------------------------------

    private void afficherCartes() {
        zoneCartes.getChildren().clear();
        List<PresentationPrevisions.CarteStrategie> cartes = PresentationPrevisions.cartes(tournoi);
        for (int i = 0; i < cartes.size(); i++) {
            PresentationPrevisions.CarteStrategie c = cartes.get(i);
            Label nom = Ui.label(c.nom(), "carte-strategie-nom");
            nom.setWrapText(true);
            VBox carte = new VBox(10, nom);
            carte.getStyleClass().add("carte-strategie");
            if (c.marque() != null) {
                carte.getChildren().add(Ui.pastille(c.marque(), c.choisie() ? StatutPastille.INFO : StatutPastille.NEUTRE));
            }
            carte.getChildren().addAll(
                    rubrique(PresentationPrevisions.RUBRIQUE_LANCEMENT, c.lancement()),
                    rubrique(PresentationPrevisions.RUBRIQUE_TOURNOI, c.pourQuelTournoi()),
                    rubrique(PresentationPrevisions.RUBRIQUE_POINT_FAIBLE, c.pointFaible()));
            if (c.choisie()) {
                carte.getStyleClass().add("carte-strategie-choisie");
            }
            carte.setMaxHeight(Double.MAX_VALUE);
            Button choisir = c.choisie() ? Ui.boutonSecondaire(PresentationPrevisions.MARQUE_CHOISIE)
                    : Ui.boutonPrimaire(PresentationPrevisions.BOUTON_CHOISIR);
            choisir.setDisable(c.choisie());
            choisir.setMaxWidth(Double.MAX_VALUE);
            choisir.setOnAction(e -> choisir(c.strategie()));
            zoneCartes.add(carte, i, 0);
            zoneCartes.add(choisir, i, 1);
        }
    }

    private static VBox rubrique(String titre, String texte) {
        Label t = Ui.label(titre, "carte-strategie-rubrique");
        Label l = Ui.texte(texte);
        l.setWrapText(true);
        return new VBox(2, t, l);
    }

    /** Enregistre aussitôt le choix ; un refus d'écriture remet l'état précédent et dit pourquoi. */
    private void choisir(StrategieOrdonnancement s) {
        StrategieOrdonnancement avant = tournoi.strategieOrdonnancementChoisie()
                ? tournoi.getStrategieOrdonnancement() : null;
        tournoi.setStrategieOrdonnancement(s);
        try {
            contexte.enregistrerTournoi(tournoi, fichier);
        } catch (DroitRefuseException refus) {
            tournoi.setStrategieOrdonnancement(avant);
            Ui.erreur(messageChoix, refus.getMessage());
            return;
        }
        afficherCartes();
        Ui.succes(messageChoix, PresentationPrevisions.messageChoixEnregistre(s));
        if (dernierePrevision != null) {
            afficherPrevision(dernierePrevision, derniereLoi);
        }
    }

    // ------------------------------------------------------------------
    // Prévisions : explication
    // ------------------------------------------------------------------

    private Node carteExplication() {
        VBox paragraphes = new VBox(10);
        for (PresentationPrevisions.Explication e : PresentationPrevisions.explications()) {
            Label titre = Ui.gras(e.titre());
            Label texte = Ui.texte(e.texte());
            texte.setWrapText(true);
            paragraphes.getChildren().add(new VBox(2, titre, texte));
        }
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.STRATEGIE, false),
                Ui.titreCarte(PresentationPrevisions.TITRE_EXPLICATION));
        entete.setAlignment(Pos.CENTER_LEFT);
        VBox carte = Ui.carte(entete, paragraphes);
        carte.getStyleClass().add("prevision-explication");
        return carte;
    }

    // ------------------------------------------------------------------
    // Prévisions : réglages
    // ------------------------------------------------------------------

    /** Une journée : le jour de sa fin visée (le jour même ou le lendemain) et l'heure. */
    private record ChampsFinVisee(LocalDate journee, ComboBox<String> jour, TextField heure) {
        boolean lendemain() {
            return jour.getSelectionModel().getSelectedIndex() == 1;
        }
    }

    private Node carteReglages() {
        TextField tables = champCourt(String.valueOf(PresentationPrevisions.tablesProposees(tournoi)));
        TextField moyenne = champCourt(String.valueOf(PresentationPrevisions.DUREE_MOYENNE));
        TextField minimum = champCourt(String.valueOf(PresentationPrevisions.DUREE_MINIMUM));
        TextField maximum = champCourt(String.valueOf(PresentationPrevisions.DUREE_MAXIMUM));

        HBox durees = Ui.ligne(16, Ui.champEtiquete("Durée moyenne du jeu (min)", moyenne),
                Ui.champEtiquete("Minimum (min)", minimum), Ui.champEtiquete("Maximum (min)", maximum));
        durees.setAlignment(Pos.BOTTOM_LEFT);
        Label aideDurees = aide("Durée du jeu seul, pour un match en cinq manches ; trois manches : deux tiers, "
                + "sept manches : quatre tiers. La moyenne doit être plus proche du minimum que du maximum.");

        List<ChampsFinVisee> fins = new ArrayList<>();
        Node groupeFins = groupeFinsVisees(fins);
        Map<String, TextField> annexes = new LinkedHashMap<>();
        Node groupeAnnexes = groupeAnnexes(annexes);
        ToggleGroup choixRemplissage = new ToggleGroup();
        TextField autreTaux = champCourt("");
        Map<String, TextField> tauxParTableau = new LinkedHashMap<>();
        Node groupeRemplissage = groupeRemplissage(choixRemplissage, autreTaux, tauxParTableau);

        Button lancer = Ui.boutonPrimaire("Simuler les trois stratégies");
        lancer.setDefaultButton(true);
        lancer.setOnAction(e -> {
            ReglagesPrevision reglages = lireReglages(tables, moyenne, minimum, maximum, fins, annexes,
                    choixRemplissage, autreTaux, tauxParTableau);
            if (reglages != null) {
                simuler(lancer, reglages);
            }
        });
        return Ui.carte("Réglages de la simulation",
                Ui.grilleAdaptative(2, 420, List.of(
                        groupe("Tables", Ui.champEtiquete("Nombre de tables", tables)),
                        groupe("Durée d'un match", durees, aideDurees))),
                groupeFins, groupeAnnexes, groupeRemplissage, Ui.actions(lancer), messageSimulation);
    }

    private Node groupeFinsVisees(List<ChampsFinVisee> fins) {
        VBox lignes = new VBox(12);
        for (PresentationPrevisions.FinProposee f : PresentationPrevisions.finsProposees(
                SimulateurTournoi.finsViseesProposees(tournoi))) {
            LocalDate journee = f.journee();
            ComboBox<String> jour = new ComboBox<>();
            jour.getItems().addAll(PresentationPrevisions.libelleChoixJour(journee, false),
                    PresentationPrevisions.libelleChoixJour(journee, true));
            jour.getSelectionModel().select(f.lendemain() ? 1 : 0);
            TextField heure = champCourt(f.heure());
            ChampsFinVisee champs = new ChampsFinVisee(journee, jour, heure);
            fins.add(champs);
            Label apercu = Ui.discret("");
            apercu.setWrapText(true);
            Runnable maj = () -> apercu.setText(PresentationPrevisions.apercuFinVisee(journee, heure.getText(),
                    champs.lendemain()));
            jour.getSelectionModel().selectedIndexProperty().addListener((obs, a, b) -> maj.run());
            heure.textProperty().addListener((obs, a, b) -> maj.run());
            maj.run();
            HBox saisie = Ui.ligne(16, Ui.champEtiquete("Jour de fin", jour), Ui.champEtiquete("Heure de fin", heure));
            saisie.setAlignment(Pos.BOTTOM_LEFT);
            lignes.getChildren().add(new VBox(6, Ui.gras(PresentationPrevisions.libelleJour(journee)), saisie, apercu));
        }
        if (fins.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact("Aucune journée à simuler : aucun tableau n'a de format."));
        }
        return groupe("Fin visée de chaque journée", lignes, aide(PresentationPrevisions.AIDE_FIN_VISEE));
    }

    private Node groupeAnnexes(Map<String, TextField> champs) {
        List<PresentationPrevisions.GroupeAnnexes> groupes = PresentationPrevisions.groupesAnnexes();
        List<Label> moyennes = new ArrayList<>();
        List<Node> blocs = new ArrayList<>();
        Label total = Ui.label("", "prevision-total");
        total.setWrapText(true);
        Runnable maj = () -> {
            PresentationPrevisions.ResumeAnnexes resume = PresentationPrevisions.resumeAnnexes(saisies(champs));
            for (int i = 0; i < moyennes.size(); i++) {
                moyennes.get(i).setText(i < resume.moyennes().size() ? resume.moyennes().get(i) : "");
            }
            total.setText(resume.total());
        };
        for (PresentationPrevisions.GroupeAnnexes g : groupes) {
            HBox ligne = Ui.ligne(16);
            ligne.setAlignment(Pos.BOTTOM_LEFT);
            for (PresentationPrevisions.ChampAnnexe c : g.champs()) {
                TextField champ = champCourt(c.valeur());
                champ.textProperty().addListener((obs, a, b) -> maj.run());
                champs.put(c.cle(), champ);
                ligne.getChildren().add(Ui.champEtiquete(c.libelle(), champ));
            }
            Label moyenne = Ui.discret(g.moyenne());
            moyennes.add(moyenne);
            blocs.add(new VBox(6, Ui.gras(g.nom()), ligne, moyenne));
        }
        maj.run();
        return groupe("Temps autour du match", Ui.grilleAdaptative(groupes.size(), 300, blocs), total,
                aide(PresentationPrevisions.AIDE_ANNEXES));
    }

    private Node groupeRemplissage(ToggleGroup choix, TextField autreTaux, Map<String, TextField> tauxParTableau) {
        List<ToggleButton> boutons = new ArrayList<>();
        for (PresentationPrevisions.ChoixRemplissage c : PresentationPrevisions.ChoixRemplissage.values()) {
            ToggleButton b = new ToggleButton(c.libelle());
            b.setUserData(c);
            boutons.add(b);
        }
        VBox champAutre = Ui.champEtiquete("Taux global (%)", autreTaux);
        Ui.cacher(champAutre);
        HBox global = Ui.ligne(16, Ui.champEtiquete("Remplissage de tous les tableaux",
                Ui.segment(choix, boutons.toArray(new ToggleButton[0]))), champAutre);
        global.setAlignment(Pos.BOTTOM_LEFT);

        // Une ligne par tableau : taux propre facultatif et aperçu de ce qui sera joué.
        List<EffectifTableau> depart = SimulateurTournoi.effectifs(tournoi, RemplissageSimulation.INSCRITS_REELS);
        List<PresentationPrevisions.LigneRemplissage> lignes = PresentationPrevisions.lignesRemplissage(depart);
        GridPane grille = grille(7);
        String[] entetes = {"Tableau", "Inscrits", "Places", "Taux propre (%)", "Taux appliqué", "Joueurs simulés",
                "Effet"};
        for (int i = 0; i < entetes.length; i++) {
            grille.add(Ui.enteteColonne(entetes[i]), i, 0);
        }
        Map<String, Label[]> apercus = new LinkedHashMap<>();
        Label resume = Ui.discret("");
        resume.setWrapText(true);
        Runnable maj = () -> {
            PresentationPrevisions.ReglageRemplissage r = lireRemplissage(choix, autreTaux, tauxParTableau);
            if (r.erreur() != null) {
                resume.setText(r.erreur());
                return;
            }
            List<EffectifTableau> effectifs = SimulateurTournoi.effectifs(tournoi, r.remplissage());
            for (PresentationPrevisions.LigneRemplissage l : PresentationPrevisions.lignesRemplissage(effectifs)) {
                Label[] cases = apercus.get(l.nom());
                if (cases == null) {
                    continue;
                }
                cases[0].setText(l.taux());
                cases[1].setText(l.simules());
                cases[2].setText(l.effet());
                cases[2].getStyleClass().removeIf(c -> c.startsWith("pastille-"));
                cases[2].getStyleClass().add(l.statut().classeCss());
            }
            resume.setText(PresentationPrevisions.resumeRemplissage(effectifs));
        };
        for (int k = 0; k < lignes.size(); k++) {
            PresentationPrevisions.LigneRemplissage l = lignes.get(k);
            int rang = k + 1;
            Label nom = Ui.gras(l.nom());
            nom.setWrapText(true);
            grille.add(nom, 0, rang);
            grille.add(Ui.texte(l.inscrits()), 1, rang);
            grille.add(Ui.texte(l.capacite()), 2, rang);
            if (l.reglable()) {
                TextField taux = champCourt("");
                taux.setPromptText(PresentationPrevisions.INVITE_TAUX_TABLEAU);
                taux.textProperty().addListener((obs, a, b) -> maj.run());
                tauxParTableau.put(l.nom(), taux);
                grille.add(taux, 3, rang);
            } else {
                grille.add(Ui.discret("—"), 3, rang);
            }
            Label[] cases = {Ui.texte(l.taux()), Ui.gras(l.simules()), Ui.pastille(l.effet(), l.statut())};
            apercus.put(l.nom(), cases);
            for (int i = 0; i < cases.length; i++) {
                grille.add(cases[i], 4 + i, rang);
            }
        }
        choix.selectedToggleProperty().addListener((obs, avant, apres) -> {
            if (apres == null) {
                avant.setSelected(true); // toujours un choix global
                return;
            }
            Ui.afficherSi(champAutre, apres.getUserData() == PresentationPrevisions.ChoixRemplissage.AUTRE);
            maj.run();
        });
        autreTaux.textProperty().addListener((obs, a, b) -> maj.run());
        boutons.get(0).setSelected(true);
        Node tableaux = lignes.isEmpty() ? Ui.etatVideCompact("Aucun tableau à simuler.") : grille;
        return groupe("Remplissage des tableaux", global, tableaux, resume,
                aide(PresentationPrevisions.AIDE_REMPLISSAGE));
    }

    private static PresentationPrevisions.ReglageRemplissage lireRemplissage(ToggleGroup choix, TextField autreTaux,
                                                                             Map<String, TextField> tauxParTableau) {
        PresentationPrevisions.ChoixRemplissage c = choix.getSelectedToggle() == null
                ? PresentationPrevisions.ChoixRemplissage.INSCRITS_REELS
                : (PresentationPrevisions.ChoixRemplissage) choix.getSelectedToggle().getUserData();
        return PresentationPrevisions.lireRemplissage(c, autreTaux.getText(), saisies(tauxParTableau));
    }

    /** @return les réglages lus à l'écran, ou {@code null} après avoir affiché l'erreur de saisie. */
    private ReglagesPrevision lireReglages(TextField tables, TextField moyenne, TextField minimum, TextField maximum,
                                           List<ChampsFinVisee> fins, Map<String, TextField> annexes,
                                           ToggleGroup choixRemplissage, TextField autreTaux,
                                           Map<String, TextField> tauxParTableau) {
        Integer nbTables = PresentationPrevisions.lireTables(tables.getText());
        if (nbTables == null) {
            Ui.erreur(messageSimulation, PresentationPrevisions.ERREUR_TABLES);
            return null;
        }
        PresentationPrevisions.ReglageDurees durees = PresentationPrevisions.lireDurees(moyenne.getText(),
                minimum.getText(), maximum.getText());
        if (durees.erreur() != null) {
            Ui.erreur(messageSimulation, durees.erreur());
            return null;
        }
        PresentationPrevisions.ReglageAnnexes temps = PresentationPrevisions.lireTempsAnnexes(saisies(annexes));
        if (temps.erreur() != null) {
            Ui.erreur(messageSimulation, temps.erreur());
            return null;
        }
        PresentationPrevisions.ReglageRemplissage remplissage = lireRemplissage(choixRemplissage, autreTaux,
                tauxParTableau);
        if (remplissage.erreur() != null) {
            Ui.erreur(messageSimulation, remplissage.erreur());
            return null;
        }
        ReglagesPrevision.Builder b = ReglagesPrevision.builder().nombreDeTables(nbTables).loiDuree(durees.loi())
                .tempsAnnexes(temps.temps()).remplissage(remplissage.remplissage());
        for (ChampsFinVisee f : fins) {
            PresentationPrevisions.ReglageFinVisee fin = PresentationPrevisions.lireFinVisee(f.journee(),
                    f.heure().getText(), f.lendemain());
            if (fin.erreur() != null) {
                Ui.erreur(messageSimulation, fin.erreur());
                return null;
            }
            b.finVisee(f.journee(), fin.instant());
        }
        return b.build();
    }

    private static Map<String, String> saisies(Map<String, TextField> champs) {
        Map<String, String> saisies = new LinkedHashMap<>();
        champs.forEach((cle, champ) -> saisies.put(cle, champ.getText()));
        return saisies;
    }

    /** Un groupe de réglages : un titre et son contenu, dans un cadre. */
    private static VBox groupe(String titre, Node... contenu) {
        VBox v = new VBox(10, Ui.label(titre, "prevision-groupe-titre"));
        v.getChildren().addAll(contenu);
        v.getStyleClass().add("prevision-groupe");
        return v;
    }

    private static Label aide(String texte) {
        Label l = Ui.aide(texte);
        l.setWrapText(true);
        return l;
    }

    private static TextField champCourt(String valeur) {
        TextField t = Ui.champ(valeur);
        t.setText(valeur);
        t.setPrefColumnCount(5);
        return t;
    }

    // ------------------------------------------------------------------
    // Simulation
    // ------------------------------------------------------------------

    private void simuler(Button lancer, ReglagesPrevision reglages) {
        // Chaque journée rejouée cent fois par stratégie : calcul hors du fil de l'interface.
        lancer.setDisable(true);
        Ui.succes(messageSimulation, PresentationPrevisions.messageSimulationEnCours());
        Task<PrevisionTournoi> calcul = new Task<>() {
            @Override
            protected PrevisionTournoi call() {
                return simulateur.prevoir(tournoi, reglages);
            }
        };
        calcul.setOnSucceeded(e -> {
            lancer.setDisable(false);
            messageSimulation.setText("");
            afficherPrevision(calcul.getValue(), reglages.getLoiDuree());
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

    // ------------------------------------------------------------------
    // Résultats
    // ------------------------------------------------------------------

    /** Une journée à la fois (boutons s'il y en a plusieurs) : statistiques par thème, puis tableau par tableau. */
    private void afficherPrevision(PrevisionTournoi prevision, LoiDureeMatch loi) {
        dernierePrevision = prevision;
        derniereLoi = loi;
        StrategieOrdonnancement choisie = tournoi.getStrategieOrdonnancement();
        zoneResultat.getChildren().clear();
        if (prevision.journees().isEmpty()) {
            zoneResultat.getChildren().add(Ui.carte(Ui.etatVideCompact("Aucun tableau à simuler.")));
            return;
        }
        VBox detail = new VBox(16);
        if (prevision.journees().size() > 1) {
            ToggleGroup groupe = new ToggleGroup();
            List<ToggleButton> boutons = new ArrayList<>();
            for (LocalDate jour : prevision.journees().keySet()) {
                ToggleButton b = new ToggleButton(PresentationPrevisions.libelleJour(jour));
                b.setUserData(jour);
                boutons.add(b);
            }
            groupe.selectedToggleProperty().addListener((obs, avant, apres) -> {
                if (apres == null) {
                    avant.setSelected(true); // toujours une journée affichée
                    return;
                }
                afficherJournee(detail, prevision.journees().get((LocalDate) apres.getUserData()), choisie, loi);
            });
            zoneResultat.getChildren().addAll(Ui.segment(groupe, boutons.toArray(new ToggleButton[0])), detail);
            boutons.get(0).setSelected(true);
        } else {
            afficherJournee(detail, prevision.journees().values().iterator().next(), choisie, loi);
            zoneResultat.getChildren().add(detail);
        }
    }

    private void afficherJournee(VBox detail, PrevisionJournee journee, StrategieOrdonnancement choisie,
                                 LoiDureeMatch loi) {
        detail.getChildren().setAll(carteStatistiques(journee, choisie, loi),
                carteParTableau(journee.strategies(), choisie));
    }

    /** Les trois stratégies côte à côte, thème par thème ; la colonne de la stratégie choisie est mise en avant. */
    private Node carteStatistiques(PrevisionJournee journee, StrategieOrdonnancement choisie, LoiDureeMatch loi) {
        List<StatistiquesSimulation> stats = journee.strategies();
        int colonneChoisie = PresentationPrevisions.colonneChoisie(stats, choisie);
        Label verdict = Ui.texte(PresentationPrevisions.verdictStrategies(stats, choisie));
        verdict.setWrapText(true);
        GridPane grille = grille(stats.size() + 1);
        grille.getStyleClass().add("prevision-comparaison");
        grille.setHgap(0);
        grille.setVgap(0);
        for (int i = 1; i <= stats.size(); i++) {
            grille.getColumnConstraints().get(i).setMinWidth(170);
            grille.getColumnConstraints().get(i).setPercentWidth(22);
        }
        grille.add(cellule(Ui.enteteColonne("Indicateur"), false), 0, 0);
        for (int i = 0; i < stats.size(); i++) {
            Label entete = Ui.enteteColonne(stats.get(i).strategie().getLibelle());
            entete.setWrapText(true);
            VBox tete = new VBox(4, entete);
            if (i == colonneChoisie) {
                tete.getChildren().add(Ui.pastille(PresentationPrevisions.MARQUE_COLONNE_CHOISIE, StatutPastille.INFO));
            }
            grille.add(cellule(tete, i == colonneChoisie), i + 1, 0);
        }
        int rang = 1;
        for (PresentationPrevisions.GroupeStatistiques g : PresentationPrevisions.groupesStatistiques(journee)) {
            Label aideTheme = Ui.aide(g.aide());
            aideTheme.setWrapText(true);
            VBox theme = new VBox(2, Ui.label(g.titre(), "prevision-theme"), aideTheme);
            theme.getStyleClass().add("prevision-theme-bloc");
            grille.add(theme, 0, rang++, stats.size() + 1, 1);
            for (PresentationPrevisions.LigneStrategies l : g.lignes()) {
                Label indicateur = Ui.texte(l.indicateur());
                indicateur.setWrapText(true);
                grille.add(cellule(indicateur, false), 0, rang);
                for (int i = 0; i < l.valeurs().size(); i++) {
                    grille.add(cellule(valeur(l.valeurs().get(i), l.meilleure() == i), i == colonneChoisie), i + 1,
                            rang);
                }
                rang++;
            }
        }
        Label synthese = Ui.discret(PresentationPrevisions.syntheseStatistique(stats, loi) + " "
                + PresentationPrevisions.resumeRemplissage(journee.effectifs()));
        synthese.setWrapText(true);
        Label legende = aide("En bleu et en gras : la meilleure valeur de la ligne, quand il y en a une.");
        return Ui.carte(Ui.titreCarte("Comparaison des stratégies"), verdict, grille, synthese, legende);
    }

    /** Case de la comparaison : la colonne de la stratégie choisie porte un fond teinté continu. */
    private static StackPane cellule(Node contenu, boolean choisie) {
        StackPane c = new StackPane(contenu);
        c.setAlignment(Pos.CENTER_LEFT);
        c.getStyleClass().add("prevision-cellule");
        if (choisie) {
            c.getStyleClass().add("prevision-cellule-choisie");
        }
        return c;
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
            detail.getChildren().setAll(aide(ind.explication()), grilleIndicateur(stats, choisie, ind));
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
            Label e = Ui.enteteColonne(s.getLibelle() + (s == choisie ? " (votre stratégie)" : ""));
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
        l.setWrapText(true);
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
