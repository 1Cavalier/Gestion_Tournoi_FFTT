package com.pingmanager.ui.ecran;

import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * B043 : les pages du groupe de navigation « Tournoi en cours ».
 *
 * <ul>
 *   <li><b>Gestion</b> : la page du jour J du tournoi en cours, ouverte
 *       directement ({@link EcranJourJ} : salle, matchs, feuilles, suivi,
 *       phase finale…), avec un retour vers la fiche du tournoi (ses onglets
 *       habituels) et réciproquement ;</li>
 *   <li><b>Tableau</b> : les tableaux groupés par jour avec leur statut ; un clic
 *       ouvre l'écran existant (pointage et tirage, poules, arbre) ;</li>
 *   <li><b>Pointage</b> : le poste de pointage ({@link EcranPostePointage}).</li>
 * </ul>
 *
 * <p>Aucun écran existant n'est refait : ces pages ne font que les ouvrir sur
 * le tournoi choisi ({@link SelectionTournoiEnCours}).</p>
 */
final class PagesTournoiEnCours {

    private final ContexteApplication contexte;
    private final SelectionTournoiEnCours selection;
    /** Reconstruit la page courante (après un changement de tournoi). */
    private final Runnable reafficher;

    PagesTournoiEnCours(ContexteApplication contexte, SelectionTournoiEnCours selection, Runnable reafficher) {
        this.contexte = contexte;
        this.selection = selection;
        this.reafficher = reafficher;
    }

    // ------------------------------------------------------------------
    // Gestion
    // ------------------------------------------------------------------

    Parent gestion() {
        BorderPane racine = cadre();
        Path fichier = selection.fichier();
        if (fichier == null) {
            return vide();
        }
        afficherJourJ(racine, fichier);
        return racine;
    }

    private void afficherJourJ(BorderPane racine, Path fichier) {
        racine.setCenter(new EcranJourJ(contexte, fichier, () -> afficherFiche(racine, fichier),
                PresentationTournoiEnCours.RETOUR_FICHE).build());
    }

    /** La fiche du tournoi, comme depuis « Gestion des tournois » (mêmes onglets). */
    private void afficherFiche(BorderPane racine, Path fichier) {
        contexte.effacerNumerosSmsSiTermine(fichier, java.time.LocalDateTime.now()); // B019
        Runnable retour = () -> afficherJourJ(racine, fichier);
        ModePreparation mode = contexte.lireTournoi(fichier).getModePreparation();
        if (mode == ModePreparation.A_CHOISIR) {
            racine.setCenter(new EcranChoixPreparation(contexte, fichier, () -> afficherFiche(racine, fichier),
                    retour).build());
        } else if (mode == ModePreparation.IMPORT) {
            racine.setCenter(new EcranTournoiImporte(contexte, fichier, retour,
                    () -> afficherFiche(racine, fichier)).build());
        } else {
            racine.setCenter(new EcranCompletion(contexte, fichier, retour).build());
        }
    }

    // ------------------------------------------------------------------
    // Tableau
    // ------------------------------------------------------------------

    Parent tableaux() {
        BorderPane racine = cadre();
        if (selection.fichier() == null) {
            return vide();
        }
        afficherListeTableaux(racine);
        return racine;
    }

    private void afficherListeTableaux(BorderPane racine) {
        Path fichier = selection.fichier();
        Tournoi tournoi;
        try {
            tournoi = contexte.chargerTournoi(fichier);
        } catch (RuntimeException e) {
            racine.setCenter(Ui.page(1040, Ui.titre(PresentationTournoiEnCours.TITRE_TABLEAUX),
                    Ui.etatVide("Lecture du tournoi impossible : " + e.getMessage(), null)));
            return;
        }
        List<Node> blocs = new ArrayList<>();
        blocs.add(Ui.enteteAvecActions(new VBox(4, Ui.titre(PresentationTournoiEnCours.TITRE_TABLEAUX),
                Ui.discret(tournoi.getNom()))));
        List<PresentationAvancement.Journee> journees = PresentationAvancement.journees(tournoi);
        if (journees.isEmpty()) {
            blocs.add(Ui.etatVideCompact("Aucun tableau dans ce tournoi."));
        }
        LocalDate aujourdhui = LocalDate.now();
        for (PresentationAvancement.Journee j : journees) {
            VBox lignes = new VBox();
            lignes.getStyleClass().add("encours-tableaux");
            boolean premiere = true;
            for (PresentationAvancement.Ligne l : j.lignes()) {
                lignes.getChildren().add(ligneTableau(racine, tournoi, fichier, l, premiere));
                premiere = false;
            }
            String titre = j.jour() == null ? "Jour non défini (tableaux sans horaire)"
                    : PresentationAvancement.titreJour(j, aujourdhui);
            blocs.add(Ui.carte(titre, lignes));
        }
        racine.setCenter(Ui.defilant(Ui.page(1040, blocs.toArray(new Node[0]))));
    }

    private Node ligneTableau(BorderPane racine, Tournoi tournoi, Path fichier, PresentationAvancement.Ligne l,
                              boolean premiere) {
        Tableau t = l.tableau();
        Label nom = Ui.label(t.getNom(), "encours-tableau-nom");
        Label detail = Ui.discret(PresentationJourJ.horaires(t) + " · " + l.detail());
        VBox infos = new VBox(2, nom, detail);
        HBox.setHgrow(infos, Priority.ALWAYS);
        Label statut = Ui.pastille(PresentationTournoiEnCours.statut(t), PresentationTournoiEnCours.pastille(t));
        Button ouvrir = Ui.boutonSecondaire(l.libelleBouton());
        VBox bloc = new VBox();
        ouvrir.setDisable(l.vue() == PresentationAvancement.Detail.OUVRIR_POINTAGE
                && tournoi.getStatut() == StatutTournoi.ANNULE);
        ouvrir.setOnAction(e -> ouvrirTableau(racine, tournoi, fichier, l, bloc));
        HBox ligne = Ui.ligne(16, infos, statut, ouvrir);
        ligne.getStyleClass().add("encours-tableau");
        if (premiere) {
            ligne.getStyleClass().add("encours-tableau-premier");
        }
        bloc.getChildren().add(ligne);
        return bloc;
    }

    private void ouvrirTableau(BorderPane racine, Tournoi tournoi, Path fichier, PresentationAvancement.Ligne l,
                               VBox bloc) {
        Tableau t = l.tableau();
        Runnable enregistrer = () -> contexte.enregistrerTournoi(tournoi, fichier);
        Runnable retour = () -> afficherListeTableaux(racine);
        PresentationAvancement.Detail d = l.vue();
        if (d == PresentationAvancement.Detail.OUVRIR_POINTAGE) {
            confirmerOuverture(racine, tournoi, fichier, t, bloc);
        } else if (d == PresentationAvancement.Detail.POINTAGE) {
            afficherPointageTableau(racine, tournoi, fichier, t);
        } else if (d == PresentationAvancement.Detail.POULES) {
            racine.setCenter(new EcranPoulesTableau(tournoi, t, enregistrer, retour, false,
                    () -> racine.setCenter(VuePhaseFinale.creer(contexte, tournoi, t, fichier, retour))).build());
        } else {
            racine.setCenter(VuePhaseFinale.creer(contexte, tournoi, t, fichier, retour));
        }
    }

    private void afficherPointageTableau(BorderPane racine, Tournoi tournoi, Path fichier, Tableau t) {
        Runnable enregistrer = () -> contexte.enregistrerTournoi(tournoi, fichier);
        Runnable retour = () -> afficherListeTableaux(racine);
        racine.setCenter(new EcranPointageTableau(tournoi, t, enregistrer, retour,
                tab -> racine.setCenter(new EcranPoulesTableau(tournoi, tab, enregistrer, retour, true,
                        () -> racine.setCenter(VuePhaseFinale.creer(contexte, tournoi, tab, fichier, retour)))
                        .build())).build());
    }

    /** Ouvrir le pointage ne se défait pas : confirmation sous la ligne (comme au jour J). */
    private void confirmerOuverture(BorderPane racine, Tournoi tournoi, Path fichier, Tableau t, VBox bloc) {
        if (bloc.getChildren().stream().anyMatch(n -> n.getStyleClass().contains("encart-confirmation"))) {
            return;
        }
        Button confirmer = Ui.boutonPrimaire("Ouvrir le pointage de " + t.getNom());
        Button renoncer = Ui.boutonSecondaire("Annuler");
        VBox encart = Ui.encartConfirmation("Le pointage du tableau « " + t.getNom() + " » sera ouvert : les joueurs "
                + "pourront être pointés et payer sur place. Cette étape ne se referme pas.", confirmer, renoncer);
        confirmer.setOnAction(e -> {
            try {
                t.ouvrirPointage();
            } catch (IllegalStateException refus) {
                afficherListeTableaux(racine);
                return;
            }
            contexte.enregistrerTournoi(tournoi, fichier);
            afficherPointageTableau(racine, tournoi, fichier, t);
        });
        renoncer.setOnAction(e -> bloc.getChildren().remove(encart));
        bloc.getChildren().add(encart);
    }

    // ------------------------------------------------------------------
    // Pointage
    // ------------------------------------------------------------------

    Parent pointage() {
        if (selection.fichier() == null) {
            return vide();
        }
        BorderPane racine = cadre();
        racine.setCenter(new EcranPostePointage(contexte, selection.fichier()).build());
        return racine;
    }

    // ------------------------------------------------------------------

    /** Page avec, en haut, le sélecteur de tournoi s'il y en a plusieurs en cours. */
    private BorderPane cadre() {
        BorderPane racine = new BorderPane();
        racine.getStyleClass().add("encours-page");
        Node choix = selection.selecteur(reafficher);
        if (choix != null) {
            HBox bandeau = Ui.ligne(10, Ui.discret("Tournoi en cours :"), choix);
            bandeau.getStyleClass().add("encours-bandeau");
            racine.setTop(bandeau);
        }
        return racine;
    }

    private static Parent vide() {
        return Ui.page(1040, Ui.titre(PresentationTournoiEnCours.GROUPE),
                Ui.etatVide(PresentationTournoiEnCours.AUCUN, null));
    }
}
