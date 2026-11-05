package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatSaisieScore;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panneau de saisie du résultat d'un match du tableau final : score manche par
 * manche ou issue non normale, avec correction d'un match déjà joué.
 *
 * <p>Composant partagé par la vue « Phase finale » et par la salle (bouton
 * « Terminer » d'une table). La validation et la progression du tableau sont
 * faites par {@link ServicePhaseFinale} ; une réussite est signalée à
 * l'appelant, qui enregistre le tournoi et rafraîchit son écran.</p>
 */
final class EditeurScorePhaseFinale {

    private EditeurScorePhaseFinale() {
    }

    /**
     * @param fermer              action du bouton « Fermer »
     * @param apresEnregistrement appelé quand le cœur a accepté le résultat
     */
    static VBox creer(Tableau tableau, Match m, ServicePhaseFinale service, Runnable fermer,
                      Consumer<Match> apresEnregistrement) {
        Label titre = Ui.titreCarte("Saisie du résultat");
        Label adversaires = Ui.gras(PresentationPhaseFinale.nomJoueur(m.getJoueurA())
                + "  contre  " + PresentationPhaseFinale.nomJoueur(m.getJoueurB()));
        adversaires.setWrapText(true);
        Label format = Ui.discret(PresentationPhaseFinale.formatMatch(m));

        ComboBox<IssueMatch> issue = new ComboBox<>();
        issue.getItems().addAll(IssueMatch.values());
        issue.setConverter(new StringConverter<>() {
            @Override
            public String toString(IssueMatch i) {
                return i == null ? "" : i == IssueMatch.NORMALE ? "Match joué (score)" : PresentationPhaseFinale.libelleIssue(i);
            }

            @Override
            public IssueMatch fromString(String s) {
                return null;
            }
        });
        boolean dejaJoue = m.getStatut() == StatutMatch.TERMINE;
        issue.setValue(dejaJoue ? m.getIssue() : IssueMatch.NORMALE);
        issue.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Joueur> fautif = new ComboBox<>();
        fautif.getItems().addAll(m.getJoueurA(), m.getJoueurB());
        fautif.setConverter(new StringConverter<>() {
            @Override
            public String toString(Joueur j) {
                return j == null ? "" : PresentationPhaseFinale.nomJoueur(j);
            }

            @Override
            public Joueur fromString(String s) {
                return null;
            }
        });
        fautif.setPromptText("Choisir…");
        if (dejaJoue && m.getJoueurFautif() != null) {
            fautif.setValue(m.getJoueurFautif());
        }
        fautif.setMaxWidth(Double.MAX_VALUE);
        VBox blocFautif = Ui.champEtiquete("Joueur absent, qui abandonne ou disqualifié", fautif);

        GridPane grille = new GridPane();
        grille.getStyleClass().add("pf-grille-score");
        grille.setHgap(8);
        grille.setVgap(6);
        grille.add(Ui.enteteColonne(abrege(m.getJoueurA())), 1, 0);
        grille.add(Ui.enteteColonne(abrege(m.getJoueurB())), 2, 0);
        List<String[]> initiales = SaisieScorePhaseFinale.valeursInitiales(m);
        List<TextField[]> champs = new ArrayList<>();
        for (int i = 0; i < initiales.size(); i++) {
            TextField a = champPoints(initiales.get(i)[0]);
            TextField b = champPoints(initiales.get(i)[1]);
            champs.add(new TextField[] {a, b});
            grille.add(Ui.discret("Manche " + (i + 1)), 0, i + 1);
            grille.add(a, 1, i + 1);
            grille.add(b, 2, i + 1);
        }
        VBox blocScore = new VBox(6, grille,
                Ui.aide("Une manche se gagne à 11 points, avec deux points d'écart après 10-10 (2.11.1). "
                        + "Laissez vides les manches non jouées."));

        Runnable majIssue = () -> {
            boolean normale = issue.getValue() == null || issue.getValue() == IssueMatch.NORMALE;
            if (normale) {
                Ui.montrer(blocScore);
                Ui.cacher(blocFautif);
            } else {
                Ui.cacher(blocScore);
                Ui.montrer(blocFautif);
            }
        };
        issue.valueProperty().addListener((o, a, n) -> majIssue.run());
        majIssue.run();

        Label message = Ui.message();
        Button enregistrer = Ui.boutonPrimaire(dejaJoue ? "Corriger le résultat" : "Enregistrer le résultat");
        enregistrer.setDefaultButton(true);
        enregistrer.setOnAction(e -> enregistrerResultat(tableau, m, service, apresEnregistrement, issue.getValue(),
                fautif.getValue(), champs, message));
        Button fermerBouton = Ui.boutonSecondaire("Fermer");
        fermerBouton.setCancelButton(true);
        fermerBouton.setOnAction(e -> fermer.run());

        VBox panneau = new VBox(14, titre, adversaires, format,
                Ui.champEtiquete("Résultat", issue), blocScore, blocFautif, message, Ui.actions(enregistrer, fermerBouton));
        if (dejaJoue) {
            panneau.getChildren().add(3, VuePhaseFinale.rappel("Ce match est déjà joué : vous corrigez son résultat."));
        }
        panneau.getStyleClass().addAll("carte", "pf-panneau-saisie");
        panneau.setPrefWidth(340);
        panneau.setMaxWidth(340);
        return panneau;
    }

    private static TextField champPoints(String valeur) {
        TextField t = Ui.champ(null);
        t.setText(valeur);
        t.getStyleClass().add("pf-champ-points");
        t.setPrefColumnCount(3);
        t.setMaxWidth(72);
        return t;
    }

    private static String abrege(Joueur j) {
        String nom = PresentationPhaseFinale.nomJoueur(j);
        return nom.length() > 16 ? nom.substring(0, 15) + "…" : nom;
    }

    private static void enregistrerResultat(Tableau tableau, Match m, ServicePhaseFinale service,
                                            Consumer<Match> apresEnregistrement, IssueMatch issue, Joueur fautif,
                                            List<TextField[]> champs, Label message) {
        ResultatSaisieScore r;
        if (issue == null || issue == IssueMatch.NORMALE) {
            List<String[]> lignes = new ArrayList<>();
            for (TextField[] c : champs) {
                lignes.add(new String[] {c[0].getText(), c[1].getText()});
            }
            SaisieScorePhaseFinale.Lecture lecture = SaisieScorePhaseFinale.lire(lignes);
            if (!lecture.estValide()) {
                Ui.erreur(message, lecture.erreur());
                return;
            }
            r = service.saisirScore(tableau, m, lecture.manches());
            if (!r.estValide() && r.getNumeroManche() > 0 && r.getNumeroManche() <= champs.size()) {
                Ui.marquerErreur(champs.get(r.getNumeroManche() - 1)[0]);
                Ui.marquerErreur(champs.get(r.getNumeroManche() - 1)[1]);
            }
        } else {
            r = service.declarerIssue(tableau, m, issue, fautif);
        }
        if (!r.estValide()) {
            Ui.erreur(message, SaisieScorePhaseFinale.messageRefus(r, m));
            return;
        }
        apresEnregistrement.accept(m);
    }
}
