package com.pingmanager.ui.ecran;

import com.pingmanager.core.SaisiePoule;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Tableau;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Saisie d'une <b>poule entière d'un coup</b>, au retour de la feuille de poule
 * (salle du jour J) : tous ses matchs dans l'ordre de la feuille (I.301), une
 * ligne de manches par match, les résultats déjà saisis pré-remplis et
 * modifiables, une issue anormale possible. « Valider la poule » enregistre tout
 * en une fois par le cœur ({@link SaisiePoule}) ; le classement de la poule
 * s'affiche ensuite. Logique d'affichage : {@link PresentationSaisiePoule}.
 */
final class PanneauSaisiePoule {

    private final Tableau tableau;
    private final Poule poule;
    private final SaisiePoule saisie;
    private final Runnable fermer;
    private final Consumer<SaisiePoule.Resultat> apresValidation;
    /** Après une validation réussie : le classement remplace la saisie. */
    private boolean valide;
    private final Integer table;

    PanneauSaisiePoule(Tableau tableau, Poule poule, SaisieResultatMatch saisieMatch, Runnable fermer,
                       Consumer<SaisiePoule.Resultat> apresValidation) {
        this.tableau = tableau;
        this.poule = poule;
        this.table = poule.getNumeroTable();
        this.saisie = new SaisiePoule(saisieMatch);
        this.fermer = fermer;
        this.apresValidation = apresValidation;
    }

    /** La table de la poule au moment où la saisie a été ouverte (null si elle n'était pas lancée). */
    Integer table() {
        return table;
    }

    Poule poule() {
        return poule;
    }

    private record LigneChamps(Match match, List<TextField> manches, ComboBox<IssueMatch> issue,
                               ComboBox<Joueur> fautif) {
    }

    Node build() {
        String titre = tableau.getNom() + " · Poule " + poule.getNom()
                + (table == null ? "" : " · table " + table) + " — saisie de la poule";
        Button fermerBouton = Ui.boutonSecondaire(valide ? "Fermer" : "Annuler");
        fermerBouton.setCancelButton(true);
        fermerBouton.setOnAction(e -> fermer.run());
        Node entete = Ui.enteteAvecActions(Ui.titreCarte(titre), fermerBouton);
        VBox carte;
        if (valide) {
            carte = Ui.carte(entete, classement());
        } else {
            carte = Ui.carte(entete, Ui.aide("Recopiez la feuille de poule : une ligne par partie, dans l'ordre où "
                    + "elles se sont jouées. Tapez 11-9, ou seulement les points du perdant (9, ou -9 si le second "
                    + "joueur gagne). Les résultats déjà saisis sont repris ; une ligne vide reste à saisir."),
                    formulaire());
        }
        carte.getStyleClass().add("saisie-poule");
        return carte;
    }

    private Node formulaire() {
        VBox lignes = new VBox(0);
        List<LigneChamps> champs = new ArrayList<>();
        int numero = 1;
        TextField premier = null;
        for (Match m : poule.getMatchs()) {
            if (m.estBye()) {
                continue;
            }
            PresentationSaisiePoule.Valeurs v = PresentationSaisiePoule.valeurs(m);
            Label libelle = Ui.label(PresentationSaisiePoule.libelle(numero, poule, m), "saisie-poule-partie");
            libelle.setWrapText(true);
            libelle.setMinHeight(Region.USE_PREF_SIZE);
            libelle.setPrefWidth(260);
            libelle.setMinWidth(200);
            HBox manches = Ui.ligne(6);
            List<TextField> tf = new ArrayList<>();
            for (int i = 0; i < v.manches().size(); i++) {
                TextField c = Ui.champ(String.valueOf(i + 1));
                c.getStyleClass().add("champ-manche");
                c.setPrefColumnCount(4);
                c.setText(v.manches().get(i));
                tf.add(c);
                manches.getChildren().add(c);
                if (premier == null) {
                    premier = c;
                }
            }
            Label apercu = Ui.label("", "saisie-poule-apercu");
            Runnable maj = () -> apercu.setText(PresentationSaisiePoule.apercu(textes(tf)));
            tf.forEach(c -> c.textProperty().addListener((o, a, n) -> maj.run()));
            maj.run();
            manches.getChildren().add(apercu);
            ComboBox<IssueMatch> issue = new ComboBox<>();
            issue.getItems().add(IssueMatch.NORMALE);
            issue.getItems().addAll(PresentationJourJ.ISSUES_NON_NORMALES);
            issue.setConverter(convertisseur(i -> i == IssueMatch.NORMALE ? "Score" : PresentationJourJ.libelleIssue(i)));
            issue.setValue(v.issue() == null ? IssueMatch.NORMALE : v.issue());
            ComboBox<Joueur> fautif = new ComboBox<>();
            fautif.getItems().addAll(m.getJoueurA(), m.getJoueurB());
            fautif.setConverter(convertisseur(Joueur::getNomComplet));
            fautif.setPromptText("Joueur concerné…");
            fautif.setValue(v.fautif());
            fautif.disableProperty().bind(issue.valueProperty().isEqualTo(IssueMatch.NORMALE));
            FlowPane ligne = new FlowPane(12, 8, libelle, manches, Ui.ligne(8, issue, fautif));
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add(numero == 1 ? "saisie-poule-ligne-premiere" : "saisie-poule-ligne");
            lignes.getChildren().add(ligne);
            champs.add(new LigneChamps(m, tf, issue, fautif));
            numero++;
        }
        Label erreur = Ui.message();
        Button valider = Ui.boutonPrimaire("Valider la poule");
        valider.setDefaultButton(true);
        valider.setOnAction(e -> valider(champs, erreur));
        if (premier != null) {
            TextField f = premier;
            Platform.runLater(f::requestFocus);
        }
        VBox v = new VBox(12, lignes, Ui.actions(valider), erreur);
        VBox.setVgrow(lignes, Priority.NEVER);
        return v;
    }

    private void valider(List<LigneChamps> champs, Label erreur) {
        List<SaisiePoule.Partie> parties = new ArrayList<>();
        for (int i = 0; i < champs.size(); i++) {
            LigneChamps l = champs.get(i);
            PresentationSaisiePoule.Lecture lecture = PresentationSaisiePoule.lire(i + 1, l.match(),
                    textes(l.manches()), l.issue().getValue(), l.fautif().getValue());
            if (lecture.erreur() != null) {
                Ui.erreur(erreur, lecture.erreur());
                return;
            }
            parties.add(lecture.partie());
        }
        SaisiePoule.Resultat r = saisie.enregistrer(tableau, poule, parties);
        if (!r.estEnregistre()) {
            Ui.erreur(erreur, PresentationSaisiePoule.refus(r));
            return;
        }
        valide = true;
        apresValidation.accept(r);
    }

    /** Classement de la poule après la validation (I.303). */
    private Node classement() {
        VBox lignes = new VBox(0);
        boolean premiere = true;
        for (PresentationSuiviTournoi.LignePoule c : PresentationSuiviTournoi.classement(tableau, poule, null)) {
            Label joueur = Ui.label(c.joueur(), "lancement-joueur");
            HBox.setHgrow(joueur, Priority.ALWAYS);
            joueur.setMaxWidth(Double.MAX_VALUE);
            HBox l = Ui.ligne(12, Ui.label(c.rang(), "lancement-vue-rang"), joueur, Ui.discret(c.bilan()),
                    Ui.discret(c.qualification()));
            l.getStyleClass().add(premiere ? "lancement-vue-ligne-premiere" : "lancement-vue-ligne");
            premiere = false;
            lignes.getChildren().add(l);
        }
        return new VBox(8, Ui.sousTitre("Classement de la poule"), lignes);
    }

    private static List<String> textes(List<TextField> champs) {
        List<String> t = new ArrayList<>();
        champs.forEach(c -> t.add(c.getText()));
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
