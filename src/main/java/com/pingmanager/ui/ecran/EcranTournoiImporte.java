package com.pingmanager.ui.ecran;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.reglement.ServiceReglementTournoi;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Vue d'ensemble d'un tournoi <b>préparé hors de PingManager</b> (mode import). Trois
 * étapes le rendent opérationnel (décision de Quentin, 11 novembre 2026) : déposer le
 * règlement, importer le classeur (tableaux + joueurs), vérifier l'import. Un bandeau
 * mène ensuite au jour J. Logique d'état dans {@link PresentationTournoiImporte}.
 */
final class EcranTournoiImporte {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Runnable onChangementMode;
    private final BorderPane racine = new BorderPane();
    private final ServiceReglementTournoi reglements = new ServiceReglementTournoi();
    private Tournoi tournoi;
    private PresentationTournoiImporte presentation;

    EcranTournoiImporte(ContexteApplication contexte, Path fichier, Runnable onRetour, Runnable onChangementMode) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.onChangementMode = onChangementMode;
    }

    Node build() {
        afficher();
        return racine;
    }

    private void afficher() {
        tournoi = contexte.chargerTournoi(fichier);
        presentation = new PresentationTournoiImporte(tournoi);
        Button retour = Ui.boutonLien("← Gestion des tournois");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre(tournoi.getNom());
        titre.setWrapText(true);
        HBox ligneTitre = Ui.ligne(10, titre, Ui.badge("Brouillon"),
                Ui.pastille("Tournoi importé", StatutPastille.INFO));
        Label infos = Ui.discret(BlocsInscriptions.periode(tournoi)
                + (tournoi.getAdresse() == null ? "" : " · " + tournoi.getAdresse())
                + " · Préparé hors de PingManager : trois étapes suffisent pour le rendre opérationnel.");
        infos.setWrapText(true);
        VBox page = Ui.page(Double.MAX_VALUE, new VBox(6, retour, ligneTitre, infos), sectionEtapes(),
                carteTableaux(), carteChangerMode());
        racine.setCenter(Ui.defilant(page));
    }

    private void ouvrirJourJ() {
        racine.setCenter(new EcranJourJ(contexte, fichier, this::afficher, "← Tournoi importé").build());
    }

    // ------------------------------------------------------------------
    // Les trois étapes
    // ------------------------------------------------------------------

    private Node sectionEtapes() {
        Label titre = Ui.titreSection("Rendre le tournoi opérationnel");
        HBox.setHgrow(titre, Priority.ALWAYS);
        ProgressBar barre = new ProgressBar(presentation.etapesFaites() / (double) PresentationTournoiImporte.NOMBRE_ETAPES);
        barre.getStyleClass().addAll("jauge", "jauge-dossier");
        barre.setPrefWidth(200);
        Label compteur = new Label(presentation.etapesFaites() + " / " + PresentationTournoiImporte.NOMBRE_ETAPES);
        compteur.getStyleClass().add("dossier-compteur");
        HBox progression = Ui.ligne(14, barre, compteur);
        progression.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane entete = new FlowPane(24, 12, titre, progression);
        titre.prefWidthProperty().bind(javafx.beans.binding.Bindings.max(260,
                entete.widthProperty().subtract(progression.widthProperty()).subtract(40)));

        javafx.scene.layout.GridPane grille = Ui.grilleAdaptative(3, 300,
                List.of(etapeReglement(), etapeImport(), etapeVerification()));
        VBox carte = new VBox(18, entete, grille, bandeauJourJ());
        carte.getStyleClass().addAll("carte", "section-dossier");
        return carte;
    }

    /** Étape 1 : déposer le PDF du règlement (le règlement reste celui du club). */
    private Node etapeReglement() {
        boolean fait = presentation.reglementDepose();
        Button choisir = fait ? Ui.boutonSecondaire("Remplacer") : Ui.boutonPrimaire("Choisir le PDF…");
        choisir.setOnAction(e -> choisirReglement());
        Node contenu;
        if (fait) {
            String emplacement = tournoi.getReglementDocument().getEmplacement();
            Label nom = Ui.gras(Path.of(emplacement).getFileName().toString());
            nom.setTooltip(new javafx.scene.control.Tooltip(emplacement));
            HBox.setHgrow(nom, Priority.ALWAYS);
            nom.setMaxWidth(Double.MAX_VALUE);
            HBox depose = Ui.ligne(10, Ui.iconeCadree(Icones.DOCUMENT, "icone"), nom, choisir);
            depose.getStyleClass().add("fichier-depose");
            contenu = depose;
        } else {
            choisir.setMaxWidth(Double.MAX_VALUE);
            contenu = choisir;
        }
        return carteEtape(1, fait, false, "Règlement du tournoi",
                "Le PDF du règlement que vous avez rédigé. Il accompagne le tournoi et reste consultable.",
                contenu);
    }

    private void choisirReglement() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Règlement du tournoi");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        Path dossier = fichier.toAbsolutePath().getParent();
        if (dossier != null && Files.isDirectory(dossier)) {
            fc.setInitialDirectory(dossier.toFile());
        }
        File f = fc.showOpenDialog(racine.getScene() == null ? null : racine.getScene().getWindow());
        if (f == null) {
            return;
        }
        reglements.deposer(tournoi, new DocumentReference("Règlement", f.getAbsolutePath()));
        contexte.enregistrerTournoi(tournoi, fichier);
        afficher();
    }

    /** Étape 2 : importer le classeur (feuilles Tableaux et Joueurs). */
    private Node etapeImport() {
        boolean fait = presentation.importFait();
        Button importer = fait ? Ui.boutonSecondaire("Réimporter un classeur…")
                : Ui.boutonPrimaire("Importer le classeur…");
        importer.setMaxWidth(Double.MAX_VALUE);
        importer.setOnAction(e -> racine.setCenter(new EcranImportJoueurs(contexte, fichier, this::afficher,
                () -> racine.setCenter(new VueInscriptionsClub(contexte, fichier, this::afficher).build())).build()));
        int joueurs = RemplissageTableaux.nombreDeParticipants(tournoi);
        int tableaux = tournoi.getTableaux().size();
        String texte = fait
                ? tableaux + (tableaux > 1 ? " tableaux, " : " tableau, ") + joueurs
                + (joueurs > 1 ? " joueurs importés." : " joueur importé.")
                + " Un nouveau classeur complète l'import sans dupliquer les joueurs."
                : "Un seul classeur : la feuille « Tableaux » puis la feuille « Joueurs ». Excel, LibreOffice, "
                + "CSV ou JSON. Le modèle se télécharge depuis l'écran d'import.";
        return carteEtape(2, fait, false, "Tableaux et joueurs", texte, importer);
    }

    /** Étape 3 : vérifier l'import (dates, horaires des tableaux). */
    private Node etapeVerification() {
        boolean fait = presentation.verificationFaite();
        boolean active = presentation.importFait() && !fait;
        VBox points = new VBox(6);
        for (String p : presentation.pointsAVerifier()) {
            Label l = new Label(p);
            l.setWrapText(true);
            l.getStyleClass().addAll("rappel-attention", "rappel-doux");
            l.setMaxWidth(Double.MAX_VALUE);
            points.getChildren().add(l);
        }
        Button horaires = Ui.boutonSecondaire("Gérer les tableaux");
        horaires.setDisable(tournoi.getTableaux().isEmpty());
        horaires.setOnAction(e -> racine.setCenter(new EcranTableaux(tournoi,
                () -> contexte.enregistrerTournoi(tournoi, fichier), this::afficher).build()));
        Button infos = Ui.boutonLien("Dates et lieu");
        infos.setOnAction(e -> modifierInformations());
        FlowPane actions = new FlowPane(10, 8, horaires, infos);
        VBox contenu = new VBox(10, points, actions);
        String texte = fait ? "Dates du tournoi et horaire de chaque tableau : tout est en place."
                : "PingManager contrôle ce que le jour J demande : les dates du tournoi et l'horaire de "
                + "chaque tableau.";
        return carteEtape(3, fait, active, "Vérifier l'import", texte, contenu);
    }

    private void modifierInformations() {
        FormulairesSection.Referentiel ref = new FormulairesSection.Referentiel(
                contexte.clubsGeres(), contexte::correspondantParDefaut);
        racine.setCenter(Ui.defilant(FormulairesSection.construire(0, "Général", tournoi, ref,
                () -> contexte.enregistrerTournoi(tournoi, fichier),
                () -> {
                    contexte.enregistrerTournoi(tournoi, fichier);
                    afficher();
                }, this::afficher)));
    }

    /**
     * Carte d'une étape. L'étape « en cours » est la première non faite (ou celle
     * indiquée par {@code forceEnCours}) ; les suivantes restent neutres.
     */
    private VBox carteEtape(int numero, boolean faite, boolean forceEnCours, String titre, String texte, Node contenu) {
        boolean enCours = !faite && (forceEnCours || numero == premiereNonFaite());
        Label rond = new Label(faite ? "✓" : String.valueOf(numero));
        rond.getStyleClass().add("etape-numero");
        rond.setMinSize(34, 34);
        Label pastille = faite ? Ui.pastille("Fait", Ui.CADRE_OK)
                : enCours ? Ui.pastille("À faire", Ui.CADRE_MANQUANT) : Ui.pastille("À venir", Ui.CADRE_FACULTATIF);
        HBox entete = Ui.ligne(10, rond, Ui.espace(), pastille);
        Label t = new Label(titre);
        t.getStyleClass().add("etape-titre");
        t.setWrapText(true);
        Label explication = Ui.discret(texte);
        explication.setWrapText(true);
        Region pousse = new Region();
        VBox.setVgrow(pousse, Priority.ALWAYS);
        VBox v = new VBox(12, entete, t, explication, pousse, contenu);
        v.getStyleClass().addAll("etape-officielle", faite ? "etape-faite" : enCours ? "etape-en-cours" : "etape-a-venir");
        return v;
    }

    private int premiereNonFaite() {
        if (!presentation.reglementDepose()) {
            return 1;
        }
        if (!presentation.importFait()) {
            return 2;
        }
        return presentation.verificationFaite() ? 0 : 3;
    }

    /** Bandeau final : accès au jour J dès que des tableaux existent, et ce qu'il reste à faire. */
    private Node bandeauJourJ() {
        Label titre = new Label(presentation.pret() ? "Prêt pour le jour J" : "Ensuite : le jour J");
        titre.getStyleClass().add("bandeau-jour-j-titre");
        Label phrase = new Label(presentation.phraseJourJ());
        phrase.setWrapText(true);
        VBox textes = new VBox(4, titre, phrase);
        HBox.setHgrow(textes, Priority.ALWAYS);
        Button ouvrir = new Button("Ouvrir le jour J →");
        ouvrir.setDisable(!EcranJourJ.estPropose(tournoi));
        ouvrir.setOnAction(e -> ouvrirJourJ());
        ouvrir.setMinWidth(Region.USE_PREF_SIZE);
        HBox bandeau = new HBox(16, textes, ouvrir);
        bandeau.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bandeau.getStyleClass().add("bandeau-jour-j");
        return bandeau;
    }

    // ------------------------------------------------------------------
    // Ce qui a été importé
    // ------------------------------------------------------------------

    private Node carteTableaux() {
        VBox lignes = new VBox();
        for (RemplissageTableaux.EtatTableau e : RemplissageTableaux.parTableau(tournoi)) {
            Tableau t = e.tableau();
            String h = BlocsInscriptions.horaire(t);
            VBox id = new VBox(2, Ui.gras(t.getNom()), Ui.discret((h.isEmpty() ? "Horaire à poser" : h) + " · "
                    + com.pingmanager.core.reglement.LibellesTableau.points(t.getConfiguration())));
            id.setPrefWidth(320);
            id.setMinWidth(180);
            VBox j = BlocsInscriptions.jauge(e);
            HBox.setHgrow(j, Priority.ALWAYS);
            lignes.getChildren().add(Ui.ligneListe(id, j));
        }
        if (lignes.getChildren().isEmpty()) {
            lignes.getChildren().add(Ui.discret("Aucun tableau pour l'instant : ils seront créés par l'import."));
        }
        Button voir = Ui.boutonSecondaire("Voir les inscriptions");
        voir.setDisable(tournoi.getTableaux().isEmpty());
        voir.setOnAction(e -> racine.setCenter(new VueInscriptionsClub(contexte, fichier, this::afficher).build()));
        return Ui.carte(Ui.enteteAvecActions(Ui.titreAvecCompteur("Ce qui a été importé", tournoi.getTableaux().size()),
                voir), lignes);
    }

    /** Retour à la préparation complète, tant qu'aucun joueur n'est inscrit. */
    private Node carteChangerMode() {
        boolean vide = RemplissageTableaux.nombreDeParticipants(tournoi) == 0;
        Button changer = Ui.boutonLien("Préparer plutôt ce tournoi entièrement dans PingManager");
        changer.setDisable(!vide);
        changer.setOnAction(e -> {
            tournoi.setModePreparation(ModePreparation.COMPLETE);
            contexte.enregistrerTournoi(tournoi, fichier);
            onChangementMode.run();
        });
        return new VBox(4, changer, Ui.aide(vide ? "Le choix de méthode reste possible tant qu'aucun joueur n'est inscrit."
                : "Des joueurs sont déjà importés : la méthode est fixée."));
    }
}
