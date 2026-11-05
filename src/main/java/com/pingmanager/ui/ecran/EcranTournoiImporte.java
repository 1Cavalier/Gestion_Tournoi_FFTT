package com.pingmanager.ui.ecran;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

/**
 * Vue d'ensemble d'un tournoi <b>préparé hors de PingManager</b> (mode import) : les
 * informations générales, l'import du classeur (tableaux + joueurs), les tableaux
 * obtenus (horaires modifiables) et les inscriptions. Pas d'homologation ni de
 * publication dans l'application : le club les a faites de son côté.
 */
final class EcranTournoiImporte {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Runnable onChangementMode;
    private final BorderPane racine = new BorderPane();
    private Tournoi tournoi;

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
        Button retour = Ui.boutonLien("← Gestion des tournois");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre(tournoi.getNom());
        titre.setWrapText(true);
        HBox ligneTitre = Ui.ligne(10, titre, Ui.pastille("Tournoi importé", Ui.ETAT_RECOMMANDE));
        VBox page = Ui.page(860, new VBox(6, retour, ligneTitre,
                        Ui.discret("Préparé hors de PingManager : importez les tableaux et les joueurs, vérifiez, "
                                + "puis gérez le tournoi en salle comme les autres.")),
                carteEtapes(), carteInformations(), carteImport(), carteTableaux(), carteChangerMode());
        if (EcranJourJ.estPropose(tournoi)) {
            page.getChildren().add(2, EcranJourJ.carteAcces(tournoi,
                    () -> racine.setCenter(new EcranJourJ(contexte, fichier, this::afficher,
                            "← Tournoi importé").build())));
        }
        racine.setCenter(Ui.defilant(page));
    }

    /** Trois étapes avec leur état : informations, import, horaires. */
    private Node carteEtapes() {
        boolean infos = tournoi.getDateDebut() != null && tournoi.getAdresse() != null;
        boolean importe = RemplissageTableaux.nombreDeParticipants(tournoi) > 0;
        boolean horaires = !tournoi.getTableaux().isEmpty() && tournoi.getTableaux().stream()
                .allMatch(t -> t.getHoraires() != null && t.getHoraires().getDebutMatchs() != null);
        return Ui.carte("Avancement",
                etape("1. Informations du tournoi", infos, "Dates et lieu renseignés.", "Dates ou lieu à compléter."),
                etape("2. Tableaux et joueurs importés", importe,
                        tournoi.getTableaux().size() + " tableaux, " + RemplissageTableaux.nombreDeParticipants(tournoi)
                                + " joueurs.", "Importez le classeur (feuilles Tableaux et Joueurs)."),
                etape("3. Horaires des tableaux", horaires, "Chaque tableau a son jour et son heure.",
                        "Au moins un tableau sans horaire : « Gérer les tableaux » → « Horaires »."));
    }

    private static Node etape(String nom, boolean ok, String siOk, String sinon) {
        Label p = ok ? Ui.pastille("Fait", Ui.ETAT_OK) : Ui.pastille("À faire", Ui.ETAT_ATTENTION);
        p.setMinWidth(70);
        VBox t = new VBox(2, Ui.gras(nom), Ui.discret(ok ? siOk : sinon));
        HBox.setHgrow(t, Priority.ALWAYS);
        HBox l = Ui.ligne(14, p, t);
        l.getStyleClass().add("recap-ligne");
        return l;
    }

    private Node carteInformations() {
        Button modifier = Ui.boutonSecondaire("Modifier");
        modifier.setOnAction(e -> {
            FormulairesSection.Referentiel ref = new FormulairesSection.Referentiel(
                    contexte.clubsGeres(), contexte::correspondantParDefaut);
            racine.setCenter(Ui.defilant(FormulairesSection.construire(0, "Général", tournoi, ref,
                    () -> {
                        contexte.enregistrerTournoi(tournoi, fichier);
                        afficher();
                    }, this::afficher)));
        });
        String dates = BlocsInscriptions.periode(tournoi);
        String lieu = tournoi.getAdresse() == null ? "Lieu à préciser" : tournoi.getAdresse().toString();
        String niveau = tournoi.getNiveau() == null ? "" : tournoi.getNiveau().getLibelle();
        return Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Informations"), modifier),
                Ui.texte(dates), Ui.texte(lieu), Ui.discret(niveau + (tournoi.getNumeroHomologation() == null || tournoi.getNumeroHomologation().isBlank() ? ""
                        : " · homologation " + tournoi.getNumeroHomologation())));
    }

    private Node carteImport() {
        Button importer = Ui.boutonPrimaire("Importer le classeur…");
        importer.setOnAction(e -> racine.setCenter(new EcranImportJoueurs(contexte, fichier, this::afficher,
                () -> racine.setCenter(new VueInscriptionsClub(contexte, fichier, this::afficher).build())).build()));
        Button voir = Ui.boutonSecondaire("Voir les inscriptions");
        voir.setDisable(tournoi.getTableaux().isEmpty());
        voir.setOnAction(e -> racine.setCenter(new VueInscriptionsClub(contexte, fichier, this::afficher).build()));
        return Ui.carte("Tableaux et joueurs",
                Ui.texte("Remplissez le modèle Excel : la feuille « Tableaux » (nom, points imposés, public, jour, "
                        + "heure, places) puis la feuille « Joueurs » (nom, prénom, points, club, catégorie, "
                        + "tableaux). Le modèle se télécharge depuis l'écran d'import. Vous pouvez réimporter "
                        + "un classeur complété : les joueurs déjà présents ne sont pas dupliqués."),
                Ui.ligne(10, importer, voir));
    }

    private Node carteTableaux() {
        VBox lignes = new VBox();
        for (RemplissageTableaux.EtatTableau e : RemplissageTableaux.parTableau(tournoi)) {
            Tableau t = e.tableau();
            String h = BlocsInscriptions.horaire(t);
            VBox id = new VBox(2, Ui.gras(t.getNom()), Ui.discret((h.isEmpty() ? "Horaire à poser" : h) + " · "
                    + com.pingmanager.core.reglement.LibellesTableau.points(t.getConfiguration())));
            id.setPrefWidth(320);
            VBox j = BlocsInscriptions.jauge(e);
            HBox.setHgrow(j, Priority.ALWAYS);
            lignes.getChildren().add(Ui.ligneListe(id, j));
        }
        if (lignes.getChildren().isEmpty()) {
            lignes.getChildren().add(Ui.discret("Aucun tableau : ils seront créés par l'import."));
        }
        Button gerer = Ui.boutonSecondaire("Gérer les tableaux");
        gerer.setDisable(tournoi.getTableaux().isEmpty());
        gerer.setOnAction(e -> racine.setCenter(new EcranTableaux(tournoi,
                () -> contexte.enregistrerTournoi(tournoi, fichier), this::afficher).build()));
        return Ui.carte(Ui.enteteAvecActions(Ui.titreAvecCompteur("Tableaux", tournoi.getTableaux().size()), gerer),
                lignes);
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
