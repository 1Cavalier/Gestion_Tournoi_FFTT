package com.pingmanager.ui.ecran;

import com.pingmanager.core.GenerateurSsml;
import com.pingmanager.core.ResultatSaisiePrononciation;
import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.ValidationPrononciation;
import com.pingmanager.model.PrononciationClub;
import com.pingmanager.model.PrononciationJoueur;
import com.pingmanager.ui.ContexteApplication;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Saisie de la prononciation phonétique d'un joueur (et, au jour J, de son
 * club) pour les appels au micro (B018) : champs en API, aide cliquable
 * (symboles du français, exemples), « Écouter » et « Enregistrer ».
 *
 * <p>Utilisé par « Mon profil » (partie « Appel ») et par l'écran
 * « Prononciation des noms » du jour J. La règle (validation, registre) est dans
 * le cœur ; ce composant ne fait que l'afficher.</p>
 */
final class FormulairePrononciation {

    private final ContexteApplication contexte;
    private final String licence;
    private final String prenomEcrit;
    private final String nomEcrit;
    private final String numeroClub;
    private final String nomClub;
    private final ParametresVoix voix;
    private final Runnable apresEnregistrement;

    private final TextField champPrenom = Ui.champ("prononciation du prénom");
    private final TextField champNom = Ui.champ("prononciation du nom");
    private final TextField champClub = Ui.champ("prononciation du club");
    private final Label message = Ui.message();
    private final Button ecouter = Ui.boutonSecondaire("Écouter");
    private TextField champActif = champNom;

    /**
     * @param numeroClub numéro du club (null si inconnu)
     * @param nomClub    nom du club : null pour ne pas proposer le club (profil)
     * @param voix       voix et débit pour l'écoute (null = voix par défaut)
     */
    FormulairePrononciation(ContexteApplication contexte, String licence, String prenomEcrit, String nomEcrit,
                            String numeroClub, String nomClub, ParametresVoix voix, Runnable apresEnregistrement) {
        this.contexte = contexte;
        this.licence = licence;
        this.prenomEcrit = prenomEcrit == null ? "" : prenomEcrit;
        this.nomEcrit = nomEcrit == null ? "" : nomEcrit;
        this.numeroClub = numeroClub == null || numeroClub.isBlank() ? null : numeroClub;
        this.nomClub = nomClub;
        this.voix = voix == null ? ParametresVoix.PAR_DEFAUT : voix;
        this.apresEnregistrement = apresEnregistrement == null ? () -> { } : apresEnregistrement;
    }

    Node build() {
        PrononciationJoueur p = contexte.prononciations().joueur(licence).orElse(null);
        champPrenom.setText(p == null ? "" : p.prenom());
        champNom.setText(p == null ? "" : p.nom());

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        int ligne = 0;
        ligne = rangee(g, ligne, "Prénom", prenomEcrit, champPrenom);
        ligne = rangee(g, ligne, "Nom", nomEcrit, champNom);
        boolean avecClub = nomClub != null && !nomClub.isBlank();
        if (avecClub) {
            if (numeroClub != null) {
                champClub.setText(contexte.prononciations().club(numeroClub).map(PrononciationClub::ipa).orElse(""));
                rangee(g, ligne, "Club", nomClub, champClub);
            } else {
                g.add(Ui.discret("Club"), 0, ligne);
                Label aide = Ui.aide(PresentationPrononciation.CLUB_SANS_NUMERO);
                g.add(aide, 1, ligne);
            }
        }
        for (TextField t : List.of(champPrenom, champNom, champClub)) {
            t.getStyleClass().add("prononciation-champ");
            t.focusedProperty().addListener((o, a, n) -> {
                if (n) {
                    champActif = t;
                }
            });
            GridPane.setHgrow(t, Priority.ALWAYS);
        }

        Button enregistrer = Ui.boutonPrimaire("Enregistrer");
        enregistrer.setOnAction(e -> enregistrer(avecClub && numeroClub != null));
        ecouter.setOnAction(e -> ecouter(avecClub && numeroClub != null));
        ecouter.setTooltip(new Tooltip("Faire dire le nom par la voix des annonces, avec cette prononciation."));

        VBox aide = construireAide();
        Ui.cacher(aide);
        Hyperlink basculer = new Hyperlink("Aide : symboles et exemples");
        basculer.setOnAction(e -> {
            if (aide.isVisible()) {
                Ui.cacher(aide);
            } else {
                Ui.montrer(aide);
            }
        });

        VBox racine = new VBox(10, g, Ui.actions(enregistrer, ecouter), message, basculer, aide);
        return racine;
    }

    private static int rangee(GridPane g, int ligne, String libelle, String ecrit, TextField champ) {
        Label l = Ui.discret(ecrit.isBlank() ? libelle : libelle + " (« " + ecrit + " »)");
        g.add(l, 0, ligne);
        g.add(champ, 1, ligne);
        return ligne + 1;
    }

    private VBox construireAide() {
        FlowPane symboles = new FlowPane(6, 6);
        for (PresentationPrononciation.Symbole s : PresentationPrononciation.SYMBOLES) {
            Button b = new Button(s.symbole());
            b.getStyleClass().add("prononciation-symbole");
            b.setFocusTraversable(false);
            b.setTooltip(new Tooltip(s.infobulle()));
            b.setOnAction(e -> inserer(s.symbole()));
            symboles.getChildren().add(b);
        }
        FlowPane exemples = new FlowPane(12, 6);
        for (PresentationPrononciation.Exemple ex : PresentationPrononciation.EXEMPLES) {
            Hyperlink h = new Hyperlink(ex.libelle());
            h.getStyleClass().add("prononciation-exemple");
            h.setFocusTraversable(false);
            h.setTooltip(new Tooltip("Insérer « " + ex.ipa() + " »"));
            h.setOnAction(e -> inserer(ex.ipa()));
            exemples.getChildren().add(h);
        }
        VBox v = new VBox(8, Ui.aide(PresentationPrononciation.AIDE_SAISIE), symboles,
                Ui.discret("Exemples (cliquer pour insérer) :"), exemples);
        v.getStyleClass().add("prononciation-aide");
        return v;
    }

    private void inserer(String symbole) {
        TextField t = champActif;
        PresentationPrononciation.Insertion i = PresentationPrononciation.inserer(t.getText(),
                t.getSelection().getStart(), t.getSelection().getEnd(), symbole);
        t.setText(i.texte());
        t.requestFocus();
        t.positionCaret(i.curseur());
    }

    private void enregistrer(boolean club) {
        ResultatSaisiePrononciation r = contexte.prononciations().definirJoueur(licence, champPrenom.getText(),
                champNom.getText());
        if (!r.valide()) {
            Ui.erreur(message, r.motif());
            return;
        }
        if (club) {
            ResultatSaisiePrononciation rc = contexte.prononciations().definirClub(numeroClub, nomClub,
                    champClub.getText());
            if (!rc.valide()) {
                contexte.sauvegarder();
                Ui.erreur(message, "Prénom et nom enregistrés, mais pas le club : " + rc.motif());
                return;
            }
        }
        contexte.sauvegarder();
        Ui.succes(message, r.estEffacement() ? "Prononciation effacée : la voix lira le nom écrit."
                : "Prononciation enregistrée : elle servira aux annonces de tous les tournois.");
        apresEnregistrement.run();
    }

    private void ecouter(boolean club) {
        for (TextField t : club ? List.of(champPrenom, champNom, champClub) : List.of(champPrenom, champNom)) {
            ResultatSaisiePrononciation v = ValidationPrononciation.verifier(t.getText());
            if (!v.valide()) {
                Ui.erreur(message, v.motif());
                return;
            }
        }
        if (!contexte.annoncesVocales().service().voixDisponible()) {
            Ui.erreur(message, PresentationPrononciation.VOIX_INDISPONIBLE);
            return;
        }
        List<GenerateurSsml.Morceau> morceaux = new ArrayList<>();
        if (!prenomEcrit.isBlank()) {
            morceaux.add(new GenerateurSsml.Morceau(prenomEcrit, champPrenom.getText()));
        }
        morceaux.add(new GenerateurSsml.Morceau(nomEcrit, champNom.getText()));
        if (club) {
            morceaux.add(new GenerateurSsml.Morceau(nomClub, champClub.getText()));
        }
        ecouter.setDisable(true);
        Ui.succes(message, "Écoute en cours…");
        contexte.annoncesVocales().ecouter(morceaux, voix, r -> Platform.runLater(() -> {
            ecouter.setDisable(false);
            String m = PresentationPrononciation.messageEcoute(r);
            if (m.isEmpty()) {
                Ui.cacher(message);
            } else {
                Ui.erreur(message, m);
            }
        }));
    }
}
