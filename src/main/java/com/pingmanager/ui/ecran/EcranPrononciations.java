package com.pingmanager.ui.ecran;

import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.PrononciationJoueur;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Jour J, « Prononciation des noms » (B018) : le juge-arbitre choisit un
 * joueur du tournoi et saisit ou corrige la prononciation de son nom et de son
 * club. Les prononciations vont dans le registre de l'application (réutilisé
 * d'un tournoi à l'autre), pas dans le tournoi.
 */
final class EcranPrononciations {

    private final ContexteApplication contexte;
    private final Tournoi tournoi;
    private final Runnable onRetour;

    private final TextField recherche = Ui.champ("rechercher un joueur, une licence, un club");
    private final ListView<Joueur> liste = new ListView<>();
    private final VBox zoneFormulaire = new VBox(12);

    EcranPrononciations(ContexteApplication contexte, Tournoi tournoi, Runnable onRetour) {
        this.contexte = contexte;
        this.tournoi = tournoi;
        this.onRetour = onRetour;
    }

    Node build() {
        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> onRetour.run());

        liste.getStyleClass().add("prononciation-liste");
        liste.setPrefHeight(460);
        liste.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Joueur j, boolean vide) {
                super.updateItem(j, vide);
                if (vide || j == null) {
                    setText(null);
                    return;
                }
                PrononciationJoueur p = contexte.prononciations().joueur(j.getNumeroLicence()).orElse(null);
                setText(j.getNom() + " " + j.getPrenom() + (j.getClub().isBlank() ? "" : " · " + j.getClub())
                        + (p == null ? "" : "  —  " + PresentationPrononciation.resume(p)));
            }
        });
        liste.getSelectionModel().selectedItemProperty().addListener((o, a, j) -> afficherFormulaire(j));
        recherche.textProperty().addListener((o, a, n) -> remplir());
        remplir();
        afficherFormulaire(null);

        VBox gauche = new VBox(8, recherche, liste);
        gauche.setPrefWidth(380);
        gauche.setMinWidth(300);
        VBox droite = Ui.carte("Prononciation", zoneFormulaire);
        HBox.setHgrow(droite, Priority.ALWAYS);
        HBox corps = Ui.ligne(20, gauche, droite);
        corps.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        VBox page = Ui.page(1080, retour, Ui.titre(PresentationPrononciation.TITRE_JOUR_J),
                Ui.aide(PresentationPrononciation.AIDE_JOUR_J), corps);
        return Ui.defilant(page);
    }

    private void remplir() {
        Joueur choisi = liste.getSelectionModel().getSelectedItem();
        List<List<Joueur>> inscrits = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            inscrits.add(t.getInscrits());
        }
        liste.getItems().setAll(PresentationPrononciation.joueurs(inscrits, recherche.getText()));
        if (choisi != null && liste.getItems().contains(choisi)) {
            liste.getSelectionModel().select(choisi);
        }
    }

    private void afficherFormulaire(Joueur j) {
        if (j == null) {
            zoneFormulaire.getChildren().setAll(Ui.aide(liste.getItems().isEmpty()
                    ? "Aucun joueur inscrit dans ce tournoi."
                    : "Choisissez un joueur dans la liste."));
            return;
        }
        ReglagesAnnoncesVocales r = tournoi.getAnnoncesVocales();
        String club = j.getClub() == null || j.getClub().isBlank() ? null : j.getClub().trim();
        zoneFormulaire.getChildren().setAll(new FormulairePrononciation(contexte, j.getNumeroLicence(),
                j.getPrenom(), com.pingmanager.core.TexteAnnonceVocale.nomPropre(j.getNom().trim()),
                contexte.numeroClubDuJoueur(j.getNumeroLicence()).orElse(null), club,
                new ParametresVoix(r.getVoix(), r.getDebit()), liste::refresh).build());
    }
}
