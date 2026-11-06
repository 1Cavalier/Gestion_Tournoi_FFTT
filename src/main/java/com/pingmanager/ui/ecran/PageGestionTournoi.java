package com.pingmanager.ui.ecran;

import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.nio.file.Path;
import java.util.List;

/**
 * Accueil de la gestion des tournois (réservé aux comptes rattachés à un club).
 *
 * <p>Trois temps : l'accueil ({@link AccueilGestionTournois} : tournoi actuel et
 * frise des étapes <b>brouillon → homologué → publié</b>), un formulaire de
 * création d'un brouillon, et l'écran de complétion d'un tournoi. Tout passe par le
 * {@link ContexteApplication} ; la présentation vient de {@code theme.css}.</p>
 */
public final class PageGestionTournoi {

    private final ContexteApplication contexte;
    private final BorderPane racine = new BorderPane();

    public PageGestionTournoi(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    public Parent build() {
        afficherAccueil();
        return racine;
    }

    private void poser(Node contenu) {
        Node page = Ui.defilant(contenu);
        racine.setCenter(page);
        Ui.apparition(page);
    }

    // ==================================================================
    // Accueil : tournoi actuel et frise des étapes (B033)
    // ==================================================================

    private void afficherAccueil() {
        new AccueilGestionTournois(contexte, this::poser, clubsGeres(), this::afficherFormulaire,
                this::afficherCompletion, this::afficherJourJ).afficher();
    }

    private void afficherJourJ(Path fichier) {
        racine.setCenter(new EcranJourJ(contexte, fichier, this::afficherAccueil,
                PresentationGestionTournois.RETOUR).build());
    }

    // ==================================================================
    // Formulaire de création d'un brouillon
    // ==================================================================

    private void afficherFormulaire() {
        poser(new FormulaireNouveauTournoi(contexte, this::afficherCompletion, this::afficherAccueil).build());
    }

    // ==================================================================
    // Écran de complétion
    // ==================================================================

    private void afficherCompletion(Path fichier) {
        com.pingmanager.model.ModePreparation mode = contexte.lireTournoi(fichier).getModePreparation();
        if (mode == com.pingmanager.model.ModePreparation.A_CHOISIR) {
            racine.setCenter(new EcranChoixPreparation(contexte, fichier, () -> afficherCompletion(fichier),
                    this::afficherAccueil).build());
        } else if (mode == com.pingmanager.model.ModePreparation.IMPORT) {
            racine.setCenter(new EcranTournoiImporte(contexte, fichier, this::afficherAccueil,
                    () -> afficherCompletion(fichier)).build());
        } else {
            // L'écran de complétion gère son propre défilement : pas de ScrollPane imbriqué.
            racine.setCenter(new EcranCompletion(contexte, fichier, this::afficherAccueil).build());
        }
    }

    // ==================================================================
    // En-tête : club(s) gérés
    // ==================================================================

    private Label clubsGeres() {
        List<com.pingmanager.model.AccesClub> acces = contexte.accesCourants();
        StringBuilder sb = new StringBuilder(acces.size() > 1 ? "Clubs : " : "Club : ");
        for (int i = 0; i < acces.size(); i++) {
            String num = acces.get(i).getNumeroClub();
            String nom = contexte.clubParNumero(num)
                    .map(com.pingmanager.model.Club::getNom).orElse("Club " + num);
            sb.append(nom).append(" (").append(num).append(")");
            if (i < acces.size() - 1) {
                sb.append(", ");
            }
        }
        return Ui.discret(sb.toString());
    }
}
