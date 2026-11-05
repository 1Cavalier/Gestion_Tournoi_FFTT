package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatSaisieMatch;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Saisie du résultat d'une partie de poule : score manche par manche (2.11.1),
 * ou issue non normale (I.303 a), et effacement d'un résultat erroné.
 *
 * <p>Composant partagé par l'écran des poules et par la salle (bouton
 * « Terminer » d'une table). Le cœur ({@link SaisieResultatMatch}) valide et
 * enregistre ; un refus s'affiche sous les champs avec son motif, une réussite
 * est signalée à l'appelant, qui enregistre le tournoi et rafraîchit son
 * écran.</p>
 */
final class EditeurScorePoule {

    private EditeurScorePoule() {
    }

    /**
     * @param annuler             action du bouton « Annuler »
     * @param apresEnregistrement appelé quand le cœur a accepté la saisie (score,
     *                            issue ou effacement), avec le match concerné
     */
    static VBox creer(Tableau tableau, Match m, SaisieResultatMatch saisie, Runnable annuler,
                      Consumer<Match> apresEnregistrement) {
        int nbManches = 2 * m.getNombreManchesGagnantes() - 1;
        List<TextField> champs = new ArrayList<>();
        HBox ligneChamps = Ui.ligne(8);
        Label score = Ui.gras("");
        Label erreur = Ui.message();
        for (int i = 0; i < nbManches; i++) {
            TextField c = Ui.champ(String.valueOf(i + 1));
            c.getStyleClass().add("champ-manche");
            c.setPrefColumnCount(4);
            if (i < m.getManches().size() && m.getIssue() == IssueMatch.NORMALE) {
                c.setText(com.pingmanager.core.NotationManche.ecrire(m.getManches().get(i)));
            }
            champs.add(c);
            ligneChamps.getChildren().add(c);
        }
        Runnable majScore = () -> {
            PresentationJourJ.LectureManches l = PresentationJourJ.lireManches(textes(champs));
            for (int i = 0; i < champs.size(); i++) {
                champs.get(i).getStyleClass().remove(Ui.EN_ERREUR);
            }
            if (l.premiereIllisible() > 0) {
                Ui.marquerErreur(champs.get(l.premiereIllisible() - 1));
            }
            if (l.premiereInvalide() > 0) {
                Ui.marquerErreur(champs.get(l.premiereInvalide() - 1));
            }
            score.setText(l.manches().isEmpty() ? "" : "Manches : " + PresentationJourJ.manchesGagnees(l.manches()));
        };
        champs.forEach(c -> c.textProperty().addListener((o, a, n) -> majScore.run()));
        ligneChamps.getChildren().add(score);
        majScore.run();

        Button valider = Ui.boutonPrimaire("Enregistrer le score");
        valider.setOnAction(e -> {
            PresentationJourJ.LectureManches l = PresentationJourJ.lireManches(textes(champs));
            if (l.premiereIllisible() > 0) {
                Ui.erreur(erreur, "Manche " + l.premiereIllisible() + " illisible : tapez par exemple 11-9, ou 9 "
                        + "(-9 si le second joueur gagne la manche).");
                return;
            }
            enregistrerResultat(m, apresEnregistrement, saisie.enregistrerScore(tableau, m, l.manches()), erreur);
        });
        champs.forEach(c -> c.setOnAction(e -> valider.fire()));
        Button annulerBouton = Ui.boutonSecondaire("Annuler");
        annulerBouton.setOnAction(e -> annuler.run());

        // Issue non normale (I.303 a).
        ComboBox<IssueMatch> issue = new ComboBox<>();
        issue.getItems().addAll(PresentationJourJ.ISSUES_NON_NORMALES);
        issue.setConverter(convertisseur(PresentationJourJ::libelleIssue));
        issue.setPromptText("Issue…");
        ComboBox<Joueur> fautif = new ComboBox<>();
        fautif.getItems().addAll(m.getJoueurA(), m.getJoueurB());
        fautif.setConverter(convertisseur(Joueur::getNomComplet));
        fautif.setPromptText("Joueur concerné…");
        if (m.getIssue() != IssueMatch.NORMALE && m.getStatut() == StatutMatch.TERMINE) {
            issue.setValue(m.getIssue());
            fautif.setValue(m.getJoueurFautif());
        }
        Button declarer = Ui.boutonSecondaire("Enregistrer l'issue");
        declarer.setOnAction(e -> {
            // Le cœur refuse une issue absente (NORMALE) ou un joueur non choisi, avec le motif précis.
            IssueMatch choisie = issue.getValue() == null ? IssueMatch.NORMALE : issue.getValue();
            enregistrerResultat(m, apresEnregistrement, saisie.enregistrerIssue(tableau, m, choisie, fautif.getValue()),
                    erreur);
        });

        VBox editeur = new VBox(10,
                Ui.libelleChamp("Score, manche par manche"),
                ligneChamps,
                Ui.aide("Tapez 11-9, ou seulement les points du perdant : 9 si " + m.getJoueurA().getNomComplet()
                        + " gagne la manche, -9 si c'est " + m.getJoueurB().getNomComplet()
                        + ". Une manche se gagne à 11, avec 2 points d'écart après 10-10 (2.11.1)."),
                Ui.actions(valider, annulerBouton),
                Ui.libelleChamp("Ou issue non normale"),
                Ui.ligne(8, issue, fautif, declarer),
                Ui.aide("Forfait, abandon, refus de jouer ou disqualification : le joueur concerné marque 0 point "
                        + "au classement de la poule et son adversaire gagne le match (I.303 a)."));
        if (m.getStatut() == StatutMatch.TERMINE) {
            Button effacer = Ui.boutonLien("Effacer le résultat");
            effacer.getStyleClass().add("lien-danger");
            effacer.setOnAction(e -> enregistrerResultat(m, apresEnregistrement, saisie.effacer(tableau, m), erreur));
            editeur.getChildren().add(Ui.actions(effacer));
        }
        editeur.getChildren().add(erreur);
        editeur.getStyleClass().add("editeur-match");
        Platform.runLater(() -> champs.get(0).requestFocus());
        return editeur;
    }

    private static void enregistrerResultat(Match m, Consumer<Match> apresEnregistrement, ResultatSaisieMatch r,
                                            Label erreur) {
        if (!r.estEnregistre()) {
            Ui.erreur(erreur, PresentationJourJ.motifSaisie(r));
            return;
        }
        apresEnregistrement.accept(m);
    }

    private static List<String> textes(List<TextField> champs) {
        List<String> t = new ArrayList<>();
        for (TextField c : champs) {
            t.add(c.getText());
        }
        return t;
    }

    private static <T> StringConverter<T> convertisseur(java.util.function.Function<T, String> f) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : f.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
