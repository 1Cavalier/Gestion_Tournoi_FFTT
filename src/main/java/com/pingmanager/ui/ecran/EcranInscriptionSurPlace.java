package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceInscriptionSurPlace;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Jour J, « Inscrire sur place » : le juge-arbitre cherche un joueur (du tournoi, ou un
 * licencié par son nom ou sa licence), voit les tableaux dont il est éliminé, et l'inscrit à
 * un tableau qui n'a pas encore commencé ({@link ServiceInscriptionSurPlace}). Les règles du
 * tournoi s'appliquent, dont la clause « nouveau tableau après élimination ».
 */
final class EcranInscriptionSurPlace {

    private final ContexteApplication contexte;
    private final Tournoi tournoi;
    private final Runnable enregistrer;
    private final Runnable onRetour;
    private final ServiceInscriptionSurPlace service = new ServiceInscriptionSurPlace();

    private final TextField recherche = Ui.champ("nom, prénom, licence ou club");
    private final ListView<Joueur> liste = new ListView<>();
    private final VBox zone = new VBox(12);
    private final Label message = Ui.message();
    private final CheckBox paye = new CheckBox("Engagement réglé à la table");

    EcranInscriptionSurPlace(ContexteApplication contexte, Tournoi tournoi, Runnable enregistrer, Runnable onRetour) {
        this.contexte = contexte;
        this.tournoi = tournoi;
        this.enregistrer = enregistrer;
        this.onRetour = onRetour;
    }

    Node build() {
        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> onRetour.run());
        paye.setSelected(true);

        liste.setPrefHeight(460);
        liste.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Joueur j, boolean vide) {
                super.updateItem(j, vide);
                if (vide || j == null) {
                    setText(null);
                    return;
                }
                boolean nouveau = PresentationInscriptionSurPlace.participant(tournoi, j.getNumeroLicence()) == null;
                setText(j.getNom() + " " + j.getPrenom() + " · " + j.getPoints() + " pts"
                        + (j.getClub() == null || j.getClub().isBlank() ? "" : " · " + j.getClub())
                        + (nouveau ? "  (licencié, pas encore inscrit)" : ""));
            }
        });
        liste.getSelectionModel().selectedItemProperty().addListener((o, a, j) -> afficherJoueur(j));
        recherche.textProperty().addListener((o, a, n) -> remplir());
        remplir();
        afficherJoueur(null);

        VBox gauche = new VBox(8, recherche, liste);
        gauche.setPrefWidth(380);
        gauche.setMinWidth(300);
        VBox droite = Ui.carte("Tableaux qui n'ont pas commencé", message, zone);
        HBox.setHgrow(droite, Priority.ALWAYS);
        HBox corps = Ui.ligne(20, gauche, droite);
        corps.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        Ui.cacher(message);

        VBox page = Ui.page(1080, retour, Ui.titre(PresentationInscriptionSurPlace.TITRE),
                Ui.aide(PresentationInscriptionSurPlace.regle(tournoi)), corps,
                // Point 21 (RGPD) : à lire ou remettre au joueur inscrit sur place.
                Ui.aide(com.pingmanager.core.MentionInformation.INSCRIPTION));
        return Ui.defilant(page);
    }

    private void remplir() {
        Joueur choisi = liste.getSelectionModel().getSelectedItem();
        String texte = recherche.getText() == null ? "" : recherche.getText().trim();
        List<com.pingmanager.model.FicheLicence> licencies = texte.length() >= 2
                ? contexte.rechercherLicences(texte, 20) : List.of();
        liste.getItems().setAll(PresentationInscriptionSurPlace.joueurs(tournoi, texte, licencies));
        if (choisi != null && liste.getItems().contains(choisi)) {
            liste.getSelectionModel().select(choisi);
        }
    }

    private void afficherJoueur(Joueur j) {
        if (j == null) {
            zone.getChildren().setAll(Ui.aide(liste.getItems().isEmpty()
                    ? "Aucun joueur trouvé : tapez un nom ou un numéro de licence."
                    : "Choisissez un joueur dans la liste."));
            return;
        }
        zone.getChildren().setAll(Ui.gras(j.getNom() + " " + j.getPrenom()),
                Ui.discret(PresentationInscriptionSurPlace.situation(tournoi, j)), paye);
        Map<Tableau, String> verif = PresentationInscriptionSurPlace.trier(service.verifier(tournoi, j));
        if (verif.isEmpty()) {
            zone.getChildren().add(Ui.aide("Tous les tableaux ont commencé : plus d'inscription possible."));
            return;
        }
        verif.forEach((tab, refus) -> zone.getChildren().add(ligne(j, tab, refus)));
    }

    private Node ligne(Joueur j, Tableau tab, String refus) {
        VBox texte = new VBox(2, Ui.gras(tab.getNom()), Ui.discret(PresentationInscriptionSurPlace.detail(tab)));
        if (refus != null) {
            Label motif = new Label(refus);
            motif.setWrapText(true);
            motif.getStyleClass().add("motif-refus");
            texte.getChildren().add(motif);
        }
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox l = Ui.ligne(12, texte, espace);
        if (refus == null) {
            Button inscrire = Ui.boutonPrimaire("Inscrire");
            inscrire.setOnAction(e -> inscrire(j, tab));
            l.getChildren().add(inscrire);
        }
        return l;
    }

    private void inscrire(Joueur j, Tableau tab) {
        // Un licencié pas encore inscrit devient un participant : on garde l'instance du tournoi s'il y est.
        Joueur present = PresentationInscriptionSurPlace.participant(tournoi, j.getNumeroLicence());
        Joueur cible = present != null ? present : j;
        ServiceInscriptionSurPlace.Resultat r = service.inscrire(tournoi, cible, tab, paye.isSelected());
        if (r.accepte()) {
            enregistrer.run();
            Ui.succes(message, cible.getNom() + " " + cible.getPrenom() + " est inscrit au tableau " + tab.getNom()
                    + (paye.isSelected() ? ", engagement réglé." : ", engagement à régler."));
            remplir();
            liste.getSelectionModel().select(cible);
            afficherJoueur(cible);
        } else {
            Ui.erreur(message, r.motif());
        }
    }
}
