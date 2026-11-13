package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatAgenda;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Petite fenêtre d'ajout ou de modification d'une date personnelle de l'agenda
 * (B045) : titre, date, heure facultative, note. La validation est celle de
 * {@link ServiceAgenda} ; un refus s'affiche dans la fenêtre, qui reste ouverte.
 */
final class FenetreDateAgenda {

    private FenetreDateAgenda() {
    }

    /**
     * Ouvre la fenêtre (modale). Si {@code existante} est fournie, la date est
     * modifiée ; sinon une date est ajoutée (au jour {@code jourPropose} s'il est fourni).
     *
     * @return le résultat accepté (date ajoutée ou modifiée), ou vide si l'on renonce
     */
    static Optional<ResultatAgenda> ouvrir(Node proprietaire, ServiceAgenda service, AgendaCompte agenda,
                                           EvenementAgenda existante, LocalDate jourPropose) {
        Dialog<ButtonType> fenetre = new Dialog<>();
        fenetre.setTitle(existante == null ? "Ajouter une date" : "Modifier la date");
        if (proprietaire != null && proprietaire.getScene() != null) {
            fenetre.initOwner(proprietaire.getScene().getWindow());
            // Même feuille de style et même thème (clair ou sombre) que la fenêtre principale.
            fenetre.getDialogPane().getStylesheets().setAll(proprietaire.getScene().getStylesheets());
            fenetre.getDialogPane().getStyleClass().addAll(proprietaire.getScene().getRoot().getStyleClass());
        }
        fenetre.getDialogPane().getStyleClass().add("agenda-fenetre");

        TextField titre = Ui.champ("Entraînement, stage, réunion…");
        DatePicker date = new DatePicker();
        date.setPromptText("jj/mm/aaaa");
        date.setMaxWidth(Double.MAX_VALUE);
        TextField heure = Ui.champ("Facultatif, par exemple 18h30");
        TextArea note = new TextArea();
        note.setPromptText("Facultatif");
        note.setPrefRowCount(3);
        note.setWrapText(true);
        if (existante != null) {
            titre.setText(existante.getTitre());
            date.setValue(existante.getDate());
            heure.setText(existante.getHeure() == null ? "" : Formats.heure(existante.getHeure()));
            note.setText(existante.getNote() == null ? "" : existante.getNote());
        } else {
            date.setValue(jourPropose);
        }
        Label message = Ui.message();
        message.getStyleClass().add("agenda-fenetre-message");

        VBox contenu = new VBox(6,
                Ui.libelleChamp("Titre"), titre,
                Ui.libelleChamp("Date"), date,
                Ui.libelleChamp("Heure"), heure,
                Ui.libelleChamp("Note"), note,
                message);
        contenu.getStyleClass().add("agenda-fenetre-contenu");
        fenetre.getDialogPane().setContent(contenu);

        ButtonType enregistrer = new ButtonType(existante == null ? "Ajouter" : "Enregistrer",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType renoncer = new ButtonType("Renoncer", ButtonBar.ButtonData.CANCEL_CLOSE);
        fenetre.getDialogPane().getButtonTypes().setAll(enregistrer, renoncer);

        ResultatAgenda[] accepte = {null};
        Node bouton = fenetre.getDialogPane().lookupButton(enregistrer);
        bouton.addEventFilter(ActionEvent.ACTION, ev -> {
            LocalTime h;
            try {
                h = Saisie.lireHeure(heure.getText(), "Heure");
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
                ev.consume();
                return;
            }
            String n = note.getText() == null || note.getText().isBlank() ? null : note.getText().trim();
            ResultatAgenda r = existante == null
                    ? service.ajouterDate(agenda, titre.getText(), date.getValue(), h, n)
                    : service.modifierDate(agenda, existante.getId(), titre.getText(), date.getValue(),
                    existante.estSurPlusieursJours() ? existante.getDateFin() : null, h, n);
            if (!r.estAccepte()) {
                Ui.erreur(message, r.message());
                ev.consume();
                return;
            }
            accepte[0] = r;
        });
        Optional<ButtonType> choix = fenetre.showAndWait();
        if (choix.isEmpty() || choix.get() != enregistrer || accepte[0] == null) {
            return Optional.empty();
        }
        return Optional.of(accepte[0]);
    }
}
