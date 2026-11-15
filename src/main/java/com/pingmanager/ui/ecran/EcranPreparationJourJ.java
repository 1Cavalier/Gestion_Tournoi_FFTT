package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListePointage;
import com.pingmanager.core.PrevisionTournoi;
import com.pingmanager.core.ReglagesPrevision;
import com.pingmanager.core.ServicePreparationJourJ;
import com.pingmanager.core.ServicePreparationJourJ.Acces;
import com.pingmanager.core.SimulateurTournoi;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.Onglet;
import com.pingmanager.ui.infra.ImpressionFeuilles;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Étape « J-7 : préparer le jour J » (B051) : le jour J tel qu'il se présentera,
 * en <b>mode préparation</b>. Mêmes composants et classes de style que l'écran
 * du jour J ({@link EcranSalle}, {@link PagesTournoiEnCours},
 * {@link EcranPostePointage}), mais tout est en lecture seule : rien ne se
 * lance, aucun pointage, aucun score, et le fichier du tournoi n'est jamais
 * enregistré d'ici.
 *
 * <p>Vue mince : les mots, l'ordre et les états viennent de
 * {@link PresentationPreparationJourJ}, les alertes et l'accès de
 * {@link ServicePreparationJourJ}. La prévision se calcule à la demande, hors
 * du fil JavaFX.</p>
 */
final class EcranPreparationJourJ {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Acces acces;
    private final BorderPane racine = new BorderPane();
    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    private Tournoi tournoi;
    private Onglet onglet = Onglet.GENERAL;
    private final VBox zoneOnglet = new VBox(16);
    /** Dernière prévision calculée (gardée en changeant d'onglet), ou {@code null}. */
    private PrevisionTournoi prevision;
    private StrategieOrdonnancement strategiePrevue;

    EcranPreparationJourJ(ContexteApplication contexte, Path fichier, Acces acces, Runnable onRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.acces = acces;
        this.onRetour = onRetour;
        racine.getStyleClass().addAll("salle", "preparation-jourj");
    }

    /**
     * Carte d'accès posée dans la vue d'ensemble du tournoi, après « Remplissage
     * et finances » ; {@code null} si l'étape n'est pas proposée à ce statut.
     */
    static Node carteAcces(Acces acces, Runnable ouvrir) {
        PresentationPreparationJourJ.CarteAcces c = PresentationPreparationJourJ.carte(acces);
        if (!c.visible()) {
            return null;
        }
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.AGENDA, !c.ouverte()),
                Ui.titreCarte(PresentationPreparationJourJ.TITRE_CARTE), Ui.pastille(c.pastille(), c.statut()));
        entete.setAlignment(Pos.CENTER_LEFT);
        Label etat = Ui.gras(c.etat());
        etat.setWrapText(true);
        Label aide = Ui.discret(PresentationPreparationJourJ.AIDE_CARTE);
        aide.setWrapText(true);
        VBox carte = Ui.carte(entete, etat, aide);
        carte.getStyleClass().add("carte-preparation-jourj");
        if (c.ouverte()) {
            Button bouton = Ui.boutonSecondaire(PresentationPreparationJourJ.BOUTON_OUVRIR);
            bouton.setOnAction(e -> ouvrir.run());
            carte.getChildren().add(Ui.actions(bouton));
        }
        return carte;
    }

    Node build() {
        afficher();
        return racine;
    }

    // ------------------------------------------------------------------
    // Page
    // ------------------------------------------------------------------

    /** Relit le tournoi (lecture seule) et reconstruit la page sur l'onglet courant. */
    private void afficher() {
        tournoi = contexte.chargerTournoi(fichier);
        if (strategiePrevue != tournoi.getStrategieOrdonnancement()) {
            prevision = null; // la stratégie a changé : le résumé ne vaut plus
        }
        Button retour = Ui.boutonLien("← Vue d'ensemble du tournoi");
        retour.setOnAction(e -> onRetour.run());

        Label bandeau = Ui.encartInfo(PresentationPreparationJourJ.BANDEAU);
        bandeau.getStyleClass().add("preparation-bandeau");

        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        for (Onglet o : Onglet.values()) {
            ToggleButton b = new ToggleButton(o.libelle());
            b.setSelected(o == onglet);
            b.setOnAction(e -> {
                // Un clic sur l'onglet déjà choisi ne le désélectionne pas.
                b.setSelected(true);
                onglet = o;
                afficherOnglet();
            });
            boutons.add(b);
        }
        HBox onglets = Ui.segment(groupe, boutons.toArray(new ToggleButton[0]));
        onglets.getStyleClass().add("preparation-onglets");

        VBox contenu = Ui.page(Double.MAX_VALUE, retour, entete(), bandeau, onglets, zoneOnglet);
        racine.setCenter(Ui.defilant(contenu));
        afficherOnglet();
    }

    /** En-tête comme au jour J : titre, nom du tournoi, puis dates et « Préparation — J-n ». */
    private Node entete() {
        Label titre = Ui.titre(PresentationPreparationJourJ.TITRE);
        Label nom = Ui.discret(tournoi.getNom() == null ? "" : tournoi.getNom());
        Label dates = new Label(PresentationPreparationJourJ.dates(tournoi) + " · "
                + PresentationPreparationJourJ.sousTitre(acces));
        dates.getStyleClass().add("jourj-chiffres");
        dates.setWrapText(true);
        return new VBox(4, titre, nom, dates);
    }

    private void afficherOnglet() {
        zoneOnglet.getChildren().clear();
        if (onglet == Onglet.GENERAL) {
            zoneOnglet.getChildren().addAll(chiffres(), carteAlertes(), carteStrategie());
        } else if (onglet == Onglet.TABLEAUX) {
            zoneOnglet.getChildren().addAll(tableaux());
        } else if (onglet == Onglet.POINTAGE) {
            zoneOnglet.getChildren().addAll(pointage());
        } else if (onglet == Onglet.SALLE) {
            zoneOnglet.getChildren().addAll(salle());
        } else {
            zoneOnglet.getChildren().add(impressions());
        }
        Ui.apparition(zoneOnglet);
    }

    // ------------------------------------------------------------------
    // Général
    // ------------------------------------------------------------------

    private Node chiffres() {
        List<PresentationPreparationJourJ.Chiffre> chiffres = PresentationPreparationJourJ.chiffres(tournoi);
        Node[] tuiles = new Node[chiffres.size()];
        for (int i = 0; i < tuiles.length; i++) {
            tuiles[i] = Ui.tuileChiffre(chiffres.get(i).valeur(), chiffres.get(i).libelle());
        }
        return Ui.rangeeTuiles(tuiles);
    }

    private Node carteAlertes() {
        List<PresentationPreparationJourJ.LigneAlerte> lignes =
                PresentationPreparationJourJ.alertes(ServicePreparationJourJ.alertes(tournoi));
        VBox liste = new VBox();
        liste.getStyleClass().add("preparation-alertes");
        if (lignes.isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact(PresentationPreparationJourJ.AUCUNE_ALERTE));
        }
        for (PresentationPreparationJourJ.LigneAlerte l : lignes) {
            Label pastille = Ui.pastille(l.pastille(), l.statut());
            pastille.setMinWidth(Region.USE_PREF_SIZE);
            HBox ligne = Ui.ligneElement(null, l.titre(), l.detail(), pastille);
            ligne.getStyleClass().add("preparation-alerte");
            if (liste.getChildren().isEmpty()) {
                ligne.getStyleClass().add("ligne-element-premiere");
            }
            liste.getChildren().add(ligne);
        }
        return Ui.carte(Ui.ligne(10, Ui.titreCarte("Alertes"),
                Ui.discret(PresentationPreparationJourJ.resumeAlertes(lignes))), liste);
    }

    /** Stratégie choisie (B050), résumé de sa prévision à la demande, lien vers l'écran Stratégie. */
    private Node carteStrategie() {
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.STRATEGIE, false),
                Ui.titreCarte(PresentationPrevisions.TITRE_CARTE));
        entete.setAlignment(Pos.CENTER_LEFT);
        Label phrase = Ui.gras(PresentationPrevisions.phraseStrategie(tournoi));
        phrase.setWrapText(true);
        Label explication = Ui.discret(tournoi.getStrategieOrdonnancement().getExplication());
        explication.setWrapText(true);

        VBox resume = new VBox(4);
        resume.getStyleClass().add("preparation-prevision");
        Label message = Ui.message();
        Button calculer = Ui.boutonSecondaire(PresentationPreparationJourJ.BOUTON_PREVISION);
        if (prevision != null) {
            remplirResume(resume);
        } else {
            Label aide = Ui.discret(PresentationPreparationJourJ.PREVISION_A_LA_DEMANDE);
            aide.setWrapText(true);
            resume.getChildren().add(aide);
        }
        calculer.setOnAction(e -> calculerPrevision(calculer, message, resume));
        Button strategie = Ui.boutonLien(PresentationPreparationJourJ.LIEN_STRATEGIE);
        strategie.setOnAction(e -> racine.setCenter(new EcranPrevisions(contexte, fichier, this::afficher).build()));
        return Ui.carte(entete, phrase, explication, resume, message, Ui.actions(calculer, strategie));
    }

    private void remplirResume(VBox resume) {
        resume.getChildren().clear();
        for (String ligne : PresentationPreparationJourJ.resumePrevision(prevision, strategiePrevue)) {
            Label l = Ui.texte(ligne);
            l.setWrapText(true);
            resume.getChildren().add(l);
        }
    }

    /** Chaque journée rejouée de nombreuses fois : calcul hors du fil de l'interface, sur une copie relue. */
    private void calculerPrevision(Button calculer, Label message, VBox resume) {
        calculer.setDisable(true);
        Ui.succes(message, PresentationPreparationJourJ.PREVISION_EN_COURS);
        // Copie relue du fichier : le tournoi affiché n'est pas partagé avec le fil de calcul.
        Tournoi copie = contexte.chargerTournoi(fichier);
        StrategieOrdonnancement strategie = copie.getStrategieOrdonnancement();
        ReglagesPrevision reglages = ReglagesPrevision.builder()
                .nombreDeTables(PresentationPrevisions.tablesProposees(copie)).build();
        Task<PrevisionTournoi> calcul = new Task<>() {
            @Override
            protected PrevisionTournoi call() {
                return simulateur.prevoir(copie, reglages);
            }
        };
        calcul.setOnSucceeded(e -> {
            prevision = calcul.getValue();
            strategiePrevue = strategie;
            calculer.setDisable(false);
            message.setText("");
            remplirResume(resume);
        });
        calcul.setOnFailed(e -> {
            calculer.setDisable(false);
            Ui.erreur(message, "La prévision a échoué : " + calcul.getException().getMessage());
        });
        Thread fil = new Thread(calcul, "prevision-preparation-jour-j");
        fil.setDaemon(true);
        fil.start();
    }

    // ------------------------------------------------------------------
    // Tableaux
    // ------------------------------------------------------------------

    private List<Node> tableaux() {
        List<Node> blocs = new ArrayList<>();
        List<PresentationPreparationJourJ.JourneeTableaux> journees = PresentationPreparationJourJ.journees(tournoi);
        if (journees.isEmpty()) {
            blocs.add(Ui.etatVideCompact("Aucun tableau dans ce tournoi."));
        }
        for (PresentationPreparationJourJ.JourneeTableaux j : journees) {
            VBox lignes = new VBox();
            lignes.getStyleClass().add("encours-tableaux");
            boolean premiere = true;
            for (PresentationPreparationJourJ.LigneTableau l : j.lignes()) {
                lignes.getChildren().add(ligneTableau(l, premiere));
                premiere = false;
            }
            blocs.add(Ui.carte(j.titre(), lignes));
        }
        return blocs;
    }

    /** Comme la page « Tableau » du tournoi en cours, sans bouton : rien ne s'ouvre en préparation. */
    private static Node ligneTableau(PresentationPreparationJourJ.LigneTableau l, boolean premiere) {
        Label nom = Ui.label(l.nom(), "encours-tableau-nom");
        VBox infos = new VBox(2, nom, Ui.discret(l.horaires()));
        HBox.setHgrow(infos, Priority.ALWAYS);
        ProgressBar jauge = new ProgressBar(Math.max(0, l.jauge()));
        jauge.getStyleClass().addAll("jauge", "avancement-jauge");
        if (l.jauge() >= 1) {
            jauge.getStyleClass().add("jauge-complete");
        }
        jauge.setPrefWidth(120);
        jauge.setMinWidth(120);
        jauge.setPrefHeight(8);
        jauge.setVisible(l.jauge() >= 0);
        Label statut = Ui.pastille(l.etat(), l.statut());
        statut.setMinWidth(Region.USE_PREF_SIZE);
        HBox ligne = Ui.ligne(16, infos, jauge, compteur(l.inscrits()), compteur(l.attente()), compteur(l.payes()),
                statut);
        ligne.getStyleClass().add("encours-tableau");
        if (premiere) {
            ligne.getStyleClass().add("encours-tableau-premier");
        }
        return ligne;
    }

    private static Label compteur(String texte) {
        Label l = Ui.label(texte, "preparation-compteur");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    // ------------------------------------------------------------------
    // Pointage (aperçu)
    // ------------------------------------------------------------------

    private List<Node> pointage() {
        List<ListePointage.Ligne> lignes = PresentationPreparationJourJ.apercuPointage(tournoi);
        int total = com.pingmanager.core.RemplissageTableaux.nombreDeParticipants(tournoi);
        Label aide = Ui.encartInfo(PresentationPreparationJourJ.AIDE_POINTAGE);
        VBox liste = new VBox();
        liste.getStyleClass().addAll("pointage-liste", "preparation-apercu");
        boolean premiere = true;
        for (ListePointage.Ligne l : lignes) {
            liste.getChildren().add(ligneJoueur(l, premiere));
            premiere = false;
        }
        if (lignes.isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact("Aucun joueur inscrit pour l'instant."));
        }
        return List.of(aide, Ui.carte(Ui.ligne(10, Ui.titreCarte(PresentationPostePointage.TITRE),
                Ui.discret(PresentationPreparationJourJ.comptePointage(lignes.size(), Math.max(total, lignes.size())))),
                liste));
    }

    /** La ligne du poste de pointage, cases inactives et sans action. */
    private static Node ligneJoueur(ListePointage.Ligne l, boolean premiere) {
        Joueur j = l.joueur();
        VBox bloc = new VBox();
        bloc.getStyleClass().add("pointage-joueur");
        if (premiere) {
            bloc.getStyleClass().add("pointage-joueur-premier");
        }
        boolean premier = true;
        for (ListePointage.Engagement e : l.engagements()) {
            CheckBox pointe = new CheckBox("Pointé");
            pointe.getStyleClass().addAll("pointage-case", "pointage-case-pointe");
            pointe.setSelected(e.pointe());
            pointe.setDisable(true);
            CheckBox paye = new CheckBox("Payé");
            paye.getStyleClass().addAll("pointage-case", "pointage-case-paye");
            paye.setSelected(e.paye());
            paye.setDisable(true);
            HBox cases = Ui.ligne(16, pointe, paye);
            cases.getStyleClass().add("pointage-cases");

            Label nom = Ui.label(premier ? PresentationPostePointage.nomAffiche(j) : "", "pointage-nom");
            Label detail = Ui.label(premier ? PresentationPostePointage.detail(j) : "", "pointage-detail");
            VBox identite = new VBox(2, nom, detail);
            identite.getStyleClass().add("pointage-identite");
            if (!premier) {
                identite.getStyleClass().add("pointage-identite-suite");
            }
            HBox.setHgrow(identite, Priority.ALWAYS);

            FlowPane etiquettes = new FlowPane(6, 6);
            etiquettes.getStyleClass().add("pointage-etiquettes");
            etiquettes.getChildren().add(
                    Ui.pastille(PresentationPostePointage.libelleTableau(e.tableau()), StatutPastille.INFO));
            if (e.jour() != null) {
                Label jour = Ui.pastille(PresentationPostePointage.jour(e.jour()), StatutPastille.NEUTRE);
                jour.getStyleClass().add("pointage-pastille-jour");
                etiquettes.getChildren().add(jour);
            }
            if (e.jourNonDefini()) {
                etiquettes.getChildren().add(
                        Ui.pastille(PresentationPostePointage.JOUR_NON_DEFINI, StatutPastille.ATTENTION));
            }
            if (e.payeEnLigne()) {
                etiquettes.getChildren().add(Ui.pastille("Payé en ligne", StatutPastille.SUCCES));
            }
            Label montant = Ui.label(PresentationPostePointage.montant(e), "pointage-montant");
            HBox ligne = Ui.ligne(16, cases, identite, etiquettes, montant);
            ligne.getStyleClass().add("pointage-engagement");
            if (!premier) {
                ligne.getStyleClass().add("pointage-engagement-suite");
            }
            bloc.getChildren().add(ligne);
            premier = false;
        }
        return bloc;
    }

    // ------------------------------------------------------------------
    // Salle (aperçu)
    // ------------------------------------------------------------------

    private List<Node> salle() {
        List<Node> blocs = new ArrayList<>();
        blocs.add(Ui.encartInfo(PresentationPreparationJourJ.AIDE_SALLE));
        VBox ordre = new VBox(12);
        List<PresentationPreparationJourJ.JourneeSalle> journees = PresentationPreparationJourJ.ordrePrevu(tournoi);
        if (journees.isEmpty()) {
            ordre.getChildren().add(Ui.etatVideCompact("Aucun tableau avec un horaire de début."));
        }
        for (PresentationPreparationJourJ.JourneeSalle j : journees) {
            VBox lignes = new VBox();
            lignes.getStyleClass().add("encours-tableaux");
            boolean premiere = true;
            for (PresentationPreparationJourJ.Passage p : j.passages()) {
                Label rang = Ui.label(String.valueOf(p.rang()), "salle-numero");
                rang.setMinWidth(Region.USE_PREF_SIZE);
                HBox ligne = Ui.ligne(16, rang,
                        new VBox(2, Ui.label(p.nom(), "encours-tableau-nom"), Ui.discret(p.detail())));
                ligne.getStyleClass().add("encours-tableau");
                if (premiere) {
                    ligne.getStyleClass().add("encours-tableau-premier");
                }
                premiere = false;
                lignes.getChildren().add(ligne);
            }
            ordre.getChildren().add(new VBox(6, Ui.gras(j.titre()), lignes));
        }
        blocs.add(Ui.carte("Ordre prévu des tableaux", ordre));

        Label intertitre = Ui.sousTitre("Salle");
        intertitre.getStyleClass().add("jourj-intertitre");
        Label resume = new Label(PresentationPreparationJourJ.resumeSalle(tournoi));
        resume.getStyleClass().add("jourj-chiffres");
        blocs.add(new VBox(4, intertitre, resume));
        if (tournoi.getNombreDeTables() <= 0) {
            blocs.add(Ui.carte("Tables", Ui.texte(PresentationPreparationJourJ.SALLE_SANS_TABLE)));
            return blocs;
        }
        FlowPane grille = new FlowPane(12, 12);
        grille.getStyleClass().add("salle-grille");
        for (int n = 1; n <= tournoi.getNombreDeTables(); n++) {
            Label numero = new Label(String.valueOf(n));
            numero.getStyleClass().add("salle-numero");
            VBox carte = new VBox(8, Ui.ligne(10, numero, Ui.espace(), Ui.pastille("Libre", Ui.ETAT_OK)));
            carte.getStyleClass().addAll("salle-table", "salle-table-libre");
            carte.setPrefWidth(220);
            carte.setMinWidth(220);
            carte.setMaxWidth(220);
            grille.getChildren().add(carte);
        }
        blocs.add(grille);
        return blocs;
    }

    // ------------------------------------------------------------------
    // À imprimer / à préparer
    // ------------------------------------------------------------------

    private Node impressions() {
        Label message = Ui.message();
        VBox liste = new VBox();
        liste.getStyleClass().add("preparation-impressions");
        for (PresentationPreparationJourJ.Impression i : PresentationPreparationJourJ.impressions()) {
            Label pastille = Ui.pastille(i.disponibilite().libelle(), i.disponibilite().statut());
            pastille.setMinWidth(Region.USE_PREF_SIZE);
            HBox ligne = Ui.ligneElement(Ui.iconeTuile(Icones.DOCUMENT, !i.aBouton()), i.titre(), i.description(),
                    pastille);
            // Pas de bouton mort : seul ce qui existe déjà en a un.
            if (i.aBouton()) {
                Button ouvrir = Ui.boutonSecondaire(i.libelleBouton());
                ouvrir.setOnAction(e -> ouvrirDocument(i.document(), message));
                ligne.getChildren().add(ouvrir);
            }
            ligne.getStyleClass().add("preparation-impression");
            if (liste.getChildren().isEmpty()) {
                ligne.getStyleClass().add("ligne-element-premiere");
            }
            liste.getChildren().add(ligne);
        }
        return Ui.carte(PresentationPreparationJourJ.Onglet.IMPRESSIONS.libelle(), liste, message);
    }

    /** Prépare le PDF dans un fichier temporaire puis l'ouvre ; le tournoi n'est pas modifié. */
    private void ouvrirDocument(PresentationPreparationJourJ.Document document, Label message) {
        boolean reglement = document == PresentationPreparationJourJ.Document.REGLEMENT;
        Path temporaire = null;
        try {
            temporaire = Files.createTempFile("pingmanager-preparation", ".pdf");
            if (reglement) {
                VueReglementFige.ecrireReglement(tournoi, contexte.documents(), temporaire);
            } else {
                VueReglementFige.ecrireFeuille(tournoi, temporaire);
            }
            ImpressionFeuilles.Resultat r = ImpressionFeuilles.ouvrir(Files.readAllBytes(temporaire),
                    reglement ? "reglement" : "feuille-engagement", false);
            if (r.estReussi()) {
                Ui.succes(message, r.message());
            } else {
                Ui.erreur(message, r.message());
            }
        } catch (IOException | RuntimeException e) {
            Ui.erreur(message, "Le PDF n'a pas pu être préparé (" + e.getMessage() + ").");
        } finally {
            if (temporaire != null) {
                try {
                    Files.deleteIfExists(temporaire);
                } catch (IOException ignore) {
                    // fichier temporaire : sans conséquence
                }
            }
        }
    }
}
