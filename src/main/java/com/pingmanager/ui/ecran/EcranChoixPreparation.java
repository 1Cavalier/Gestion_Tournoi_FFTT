package com.pingmanager.ui.ecran;

import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

/**
 * Choix <b>imposé</b> juste après la création d'un brouillon : préparer tout le tournoi
 * dans PingManager, ou importer les tableaux et les joueurs d'un tournoi préparé ailleurs.
 * Tant que le choix n'est pas fait, le tournoi rouvre sur cet écran.
 */
final class EcranChoixPreparation {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable apresChoix;
    private final Runnable onRetour;

    EcranChoixPreparation(ContexteApplication contexte, Path fichier, Runnable apresChoix, Runnable onRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.apresChoix = apresChoix;
        this.onRetour = onRetour;
    }

    Node build() {
        Tournoi t = contexte.chargerTournoi(fichier);
        Button retour = Ui.boutonLien("← Gestion des tournois");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre("Comment préparez-vous ce tournoi ?");
        Label sous = Ui.discret(t.getNom() + " — brouillon créé. Choisissez une méthode pour continuer ; "
                + "vous pourrez en changer plus tard tant qu'aucun joueur n'est inscrit.");
        sous.setWrapText(true);

        Node complet = choix("Tout préparer dans PingManager", "Préparation complète",
                "Le tournoi est rédigé de bout en bout dans l'application.",
                new String[] {"Général, tableaux, règles d'inscription, juge-arbitre, matériel",
                        "Règlement et feuille d'engagement générés en PDF",
                        "Demande d'homologation puis publication",
                        "Inscriptions en ligne des joueurs, carte des tournois"},
                "Choisir la préparation complète", ModePreparation.COMPLETE);
        Node importe = choix("Importer les tableaux et les joueurs", "Tournoi préparé ailleurs",
                "Le club a déjà son règlement, ses tableaux et ses inscrits : il les importe pour gérer "
                        + "le tournoi en salle.",
                new String[] {"Classeur Excel modèle à deux feuilles : Tableaux et Joueurs",
                        "Tableaux avec leurs points imposés, jour et heure",
                        "Joueurs : nom, prénom, points, club (obligatoires), catégorie, tableaux",
                        "Gestion du jour J identique aux autres tournois"},
                "Choisir l'import", ModePreparation.IMPORT);
        HBox deux = new HBox(20, complet, importe);
        HBox.setHgrow(complet, Priority.ALWAYS);
        HBox.setHgrow(importe, Priority.ALWAYS);
        VBox page = Ui.page(980, new VBox(6, retour, titre, sous), deux);
        return Ui.defilant(page);
    }

    private Node choix(String titre, String etiquette, String texte, String[] points, String bouton,
                       ModePreparation mode) {
        Label badge = Ui.pastille(etiquette, mode == ModePreparation.COMPLETE ? Ui.ETAT_OK : Ui.ETAT_RECOMMANDE);
        Label t = new Label(titre);
        t.getStyleClass().add("choix-titre");
        t.setWrapText(true);
        VBox liste = new VBox(6);
        for (String p : points) {
            Label l = Ui.texte("•  " + p);
            liste.getChildren().add(l);
        }
        Button b = Ui.boutonPrimaire(bouton);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            Tournoi tournoi = contexte.chargerTournoi(fichier);
            tournoi.setModePreparation(mode);
            contexte.enregistrerTournoi(tournoi, fichier);
            apresChoix.run();
        });
        VBox espace = new VBox();
        VBox.setVgrow(espace, Priority.ALWAYS);
        VBox c = new VBox(14, badge, t, Ui.texte(texte), liste, espace, b);
        c.getStyleClass().addAll("carte", "carte-choix");
        c.setAlignment(Pos.TOP_LEFT);
        c.setPrefWidth(440);
        c.setMinHeight(330);
        return c;
    }
}
