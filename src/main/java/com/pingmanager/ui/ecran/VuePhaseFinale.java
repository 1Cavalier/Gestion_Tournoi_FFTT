package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.ResultatConstructionPhaseFinale;
import com.pingmanager.core.ResultatSaisieScore;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ecran.DispositionArbre.Carte;
import com.pingmanager.ui.ecran.DispositionArbre.Disposition;
import com.pingmanager.ui.ecran.DispositionArbre.EtatCarte;
import com.pingmanager.ui.ecran.DispositionArbre.Point;
import com.pingmanager.ui.ecran.DispositionArbre.TitreColonne;
import com.pingmanager.ui.ecran.DispositionArbre.Trait;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.CaseTableau;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.LignePodium;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.TypeCase;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polyline;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Vue « Phase finale » d'un tableau, ouverte depuis la page Jour J : second
 * tirage (construction du tableau final, graine affichée et modifiable) une fois
 * les poules terminées, puis <b>arbre des rencontres</b> — une colonne par tour,
 * cartes de match reliées par des traits au tour suivant, vainqueur en gras,
 * exempts visibles, match en cours mis en évidence, petite finale sous la
 * finale — et podium.
 *
 * <p>Un clic sur un match ouvre la saisie du score (panneau à droite) ; après
 * chaque résultat, l'arbre, le podium et l'état sont redessinés aussitôt et le
 * tournoi est enregistré. Toute la logique est hors JavaFX et testée :
 * {@link ServicePhaseFinale} (cœur), {@link PresentationPhaseFinale},
 * {@link DispositionArbre} (coordonnées des cartes et des traits) et
 * {@link SaisieScorePhaseFinale}.</p>
 */
public final class VuePhaseFinale {

    /** Hauteur maximale de la zone visible de l'arbre (au-delà, il défile). */
    private static final double HAUTEUR_ARBRE_MAX = 640;

    private final ContexteApplication contexte;
    private final Tournoi tournoi;
    private final Tableau tableau;
    private final Path fichier;
    private final Runnable onRetour;
    private final ServicePhaseFinale service = new ServicePhaseFinale();

    private final BorderPane racine = new BorderPane();
    private Match selectionne;
    private String messageSucces;
    private String messageErreur;
    /** Graine proposée pour le tirage, stable d'un rafraîchissement à l'autre. */
    private final long graineProposee = PresentationJourJ.nouvelleGraine(new Random());

    // Positions de défilement, conservées quand la vue est redessinée.
    private ScrollPane defilementPage;
    private ScrollPane defilementArbre;
    private double positionPage;
    private double positionArbreH;
    private double positionArbreV;

    private VuePhaseFinale(ContexteApplication contexte, Tournoi tournoi, Tableau tableau, Path fichier,
                           Runnable onRetour) {
        this.contexte = contexte;
        this.tournoi = tournoi;
        this.tableau = Objects.requireNonNull(tableau, "tableau");
        this.fichier = fichier;
        this.onRetour = onRetour;
    }

    /**
     * Construit la vue « Phase finale » d'un tableau.
     *
     * @param contexte contexte de l'application (enregistrement du tournoi)
     * @param tournoi  le tournoi qui contient le tableau
     * @param tableau  le tableau dont on suit la phase finale
     * @param fichier  fichier du tournoi, où chaque modification est enregistrée ;
     *                 {@code null} pour ne rien enregistrer
     * @param onRetour retour à la page Jour J ({@code null} : pas de bouton retour)
     * @return le nœud racine de la vue (il gère son propre défilement)
     */
    public static Parent creer(ContexteApplication contexte, Tournoi tournoi, Tableau tableau, Path fichier,
                               Runnable onRetour) {
        VuePhaseFinale vue = new VuePhaseFinale(contexte, tournoi, tableau, fichier, onRetour);
        vue.rafraichir();
        return vue.racine;
    }

    /** Comme {@link #creer(ContexteApplication, Tournoi, Tableau, Path, Runnable)}, sans bouton retour. */
    public static Parent creer(ContexteApplication contexte, Tournoi tournoi, Tableau tableau, Path fichier) {
        return creer(contexte, tournoi, tableau, fichier, null);
    }

    // ==================================================================
    // Rendu
    // ==================================================================

    private void rafraichir() {
        memoriserDefilement();
        DiagnosticPhaseFinale diagnostic = service.diagnostiquer(tableau);
        VBox contenu = new VBox(20);
        contenu.getStyleClass().addAll("page", "pf-page");
        contenu.getChildren().add(entete(diagnostic));
        if (messageSucces != null || messageErreur != null) {
            Label message = Ui.message();
            if (messageErreur != null) {
                Ui.erreur(message, messageErreur);
            } else {
                Ui.succes(message, messageSucces);
            }
            contenu.getChildren().add(message);
            messageSucces = null;
            messageErreur = null;
        }
        defilementArbre = null;
        if (diagnostic.estConstruite()) {
            TableauElimination elim = tableau.getPhaseFinale();
            contenu.getChildren().addAll(carteResume(diagnostic, elim), carteArbre(elim), cartePodium(elim));
        } else {
            contenu.getChildren().add(carteConstruction(diagnostic));
        }
        defilementPage = Ui.defilant(contenu);
        racine.setCenter(defilementPage);
        racine.setRight(selectionne == null ? null : panneauSaisie(selectionne));
        restaurerDefilement();
    }

    private void memoriserDefilement() {
        if (defilementPage != null) {
            positionPage = defilementPage.getVvalue();
        }
        if (defilementArbre != null) {
            positionArbreH = defilementArbre.getHvalue();
            positionArbreV = defilementArbre.getVvalue();
        }
    }

    private void restaurerDefilement() {
        ScrollPane page = defilementPage;
        ScrollPane arbre = defilementArbre;
        double p = positionPage;
        double h = positionArbreH;
        double v = positionArbreV;
        Platform.runLater(() -> {
            page.setVvalue(p);
            if (arbre != null) {
                arbre.setHvalue(h);
                arbre.setVvalue(v);
            }
        });
    }

    private Node entete(DiagnosticPhaseFinale diagnostic) {
        String sous = tournoi == null || tournoi.getNom() == null ? "" : tournoi.getNom();
        VBox titres = new VBox(4);
        if (onRetour != null) {
            Button retour = Ui.boutonLien("← Jour J");
            retour.setOnAction(e -> onRetour.run());
            titres.getChildren().add(retour);
        }
        titres.getChildren().addAll(Ui.titre("Tableau final — " + tableau.getNom()), Ui.discret(sous));
        String etat = diagnostic.getEtat() == DiagnosticPhaseFinale.Etat.TERMINEE ? Ui.ETAT_OK
                : diagnostic.estConstruite() || diagnostic.peutConstruire() ? Ui.ETAT_RECOMMANDE : Ui.ETAT_OPTIONNEL;
        return Ui.enteteAvecActions(titres, Ui.pastille(PresentationPhaseFinale.titreEtat(diagnostic), etat));
    }

    private static Node rappel(String texte) {
        Label l = Ui.texte(texte);
        VBox v = new VBox(l);
        v.getStyleClass().add("rappel-regles");
        return v;
    }

    // ------------------------------------------------------------------
    // Avant le tirage : construire le tableau final
    // ------------------------------------------------------------------

    private Node carteConstruction(DiagnosticPhaseFinale diagnostic) {
        VBox corps = new VBox(12, Ui.texte(PresentationPhaseFinale.messageEtat(diagnostic)));
        if (diagnostic.peutConstruire()) {
            TextField graine = Ui.champ("ex. 141126");
            graine.setText(String.valueOf(graineProposee));
            graine.getStyleClass().add("graine-tirage");
            VBox champGraine = Ui.champEtiquete("Graine du tirage", graine);
            champGraine.setMaxWidth(360);
            Label erreur = Ui.message();
            Button tirer = Ui.boutonPrimaire("Tirer le tableau final");
            tirer.setDefaultButton(true);
            tirer.setOnAction(e -> construire(graine, erreur));
            corps.getChildren().addAll(
                    rappel(PresentationPhaseFinale.textePetiteFinaleAvantTirage(
                            tableau.getConfiguration().avecPetiteFinale())),
                    champGraine,
                    Ui.aide("IX.109 : le juge-arbitre est responsable du tirage au sort. Annoncez la graine : avec "
                            + "la même graine et les mêmes qualifiés, PingManager refait exactement le même tableau. "
                            + "Pour refaire un tirage déjà publié, saisissez sa graine."),
                    erreur,
                    Ui.actions(tirer));
            Platform.runLater(tirer::requestFocus);
        }
        return Ui.carte("Construire le tableau final", corps);
    }

    private void construire(TextField champGraine, Label erreur) {
        PresentationPhaseFinale.LectureGraine lecture =
                PresentationPhaseFinale.lireGraine(champGraine.getText(), System.nanoTime());
        if (!lecture.estValide()) {
            Ui.marquerErreur(champGraine);
            Ui.erreur(erreur, lecture.erreur());
            return;
        }
        ResultatConstructionPhaseFinale r = service.construire(tableau, lecture.graine());
        if (!r.estConstruit()) {
            Ui.erreur(erreur, PresentationPhaseFinale.messageEtat(r.getDiagnostic()));
            return;
        }
        messageSucces = "Tableau final tiré avec la graine " + r.getGraine()
                + ". Annoncez-la et affichez le tableau.";
        enregistrerPuisRafraichir();
    }

    // ------------------------------------------------------------------
    // Tableau construit : résumé, arbre, podium
    // ------------------------------------------------------------------

    private Node carteResume(DiagnosticPhaseFinale diagnostic, TableauElimination elim) {
        VBox corps = new VBox(8,
                Ui.gras(PresentationPhaseFinale.resume(elim)),
                Ui.aide(PresentationPhaseFinale.texteGraine(tableau.getGraineTableauFinal())),
                Ui.texte(PresentationPhaseFinale.messageEtat(diagnostic)));
        String notePf = PresentationPhaseFinale.notePetiteFinale(tableau.getConfiguration().avecPetiteFinale(), elim);
        if (!notePf.isEmpty()) {
            corps.getChildren().add(Ui.aide(notePf));
        }
        return Ui.carte("Tableau final", corps);
    }

    private Node cartePodium(TableauElimination elim) {
        PodiumPhaseFinale podium = ServicePhaseFinale.podium(elim);
        VBox corps = new VBox(0);
        for (LignePodium ligne : PresentationPhaseFinale.lignesPodium(podium)) {
            Label place = Ui.badge(ligne.place());
            place.getStyleClass().add("pf-place");
            Label nom = Ui.gras(ligne.joueur());
            Label detail = Ui.discret(ligne.detail());
            HBox h = Ui.ligneListe(place, nom, detail);
            h.getStyleClass().add("pf-ligne-podium");
            corps.getChildren().add(h);
        }
        corps.getChildren().add(Ui.aide(PresentationPhaseFinale.notePodium(podium)));
        return Ui.carte("Podium", corps);
    }

    /** L'arbre des rencontres, dessiné aux coordonnées de {@link DispositionArbre}. */
    private Node carteArbre(TableauElimination elim) {
        Disposition d = DispositionArbre.calculer(elim);
        Pane dessin = new Pane();
        dessin.getStyleClass().add("arbre-dessin");
        dessin.setMinSize(d.largeur(), d.hauteur());
        dessin.setPrefSize(d.largeur(), d.hauteur());
        dessin.setMaxSize(d.largeur(), d.hauteur());

        for (Trait t : d.traits()) {
            Polyline ligne = new Polyline();
            for (Point pt : t.points()) {
                ligne.getPoints().addAll(pt.x(), pt.y());
            }
            ligne.getStyleClass().add("arbre-trait");
            if (t.parcouru()) {
                ligne.getStyleClass().add("arbre-trait-parcouru");
            }
            ligne.setMouseTransparent(true);
            dessin.getChildren().add(ligne);
        }
        for (TitreColonne titre : d.titres()) {
            dessin.getChildren().add(titreColonne(titre.libelle(), titre.x(), 0, titre.largeur()));
        }
        for (Carte c : d.cartes()) {
            dessin.getChildren().add(carte(c));
        }
        if (d.petiteFinale() != null) {
            Carte pf = d.petiteFinale();
            dessin.getChildren().addAll(
                    titreColonne(DispositionArbre.TITRE_PETITE_FINALE, pf.x(), d.yTitrePetiteFinale(), pf.largeur()),
                    carte(pf));
        }

        ScrollPane defilement = new ScrollPane(dessin);
        defilement.getStyleClass().add("arbre-defilement");
        defilement.setPannable(true);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        defilement.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        defilement.setPrefViewportHeight(DispositionArbre.hauteurVisible(d, HAUTEUR_ARBRE_MAX));
        defilement.setPrefViewportWidth(d.largeur() + 16);
        defilementArbre = defilement;

        return Ui.carte("Arbre des rencontres",
                Ui.aide("Cliquez sur un match à jouer (cadre bleu) pour saisir son score ; le vainqueur avance "
                        + "aussitôt au tour suivant. Un joueur « Exempt » passe ce tour sans adversaire. "
                        + "Glissez l'arbre ou utilisez les barres pour le parcourir."),
                legende(), defilement);
    }

    private static Node legende() {
        HBox h = new HBox(16);
        h.getStyleClass().add("arbre-legende");
        for (EtatCarte e : List.of(EtatCarte.A_JOUER, EtatCarte.EN_COURS, EtatCarte.TERMINE,
                EtatCarte.EN_ATTENTE, EtatCarte.EXEMPT)) {
            Region pastille = new Region();
            pastille.getStyleClass().addAll("arbre-legende-pastille", DispositionArbre.classeEtat(e));
            Label l = Ui.discret(DispositionArbre.libelleEtat(e));
            h.getChildren().add(Ui.ligne(6, pastille, l));
        }
        return h;
    }

    private static Label titreColonne(String texte, double x, double y, double largeur) {
        Label l = new Label(texte);
        l.getStyleClass().add("arbre-titre-colonne");
        l.setAlignment(Pos.CENTER);
        l.setMinWidth(largeur);
        l.setPrefWidth(largeur);
        l.setMaxWidth(largeur);
        l.relocate(x, y);
        return l;
    }

    private Node carte(Carte c) {
        CaseTableau contenu = c.contenu();
        EtatCarte etat = DispositionArbre.etat(contenu);
        Region noeud = contenu.type() == TypeCase.EXEMPT ? carteExempt(contenu.joueurExempte()) : carteMatch(c.match());
        noeud.getStyleClass().addAll("arbre-carte", DispositionArbre.classeEtat(etat));
        if (c.petiteFinale()) {
            noeud.getStyleClass().add("arbre-petite-finale");
        }
        noeud.setMinSize(c.largeur(), c.hauteur());
        noeud.setPrefSize(c.largeur(), c.hauteur());
        noeud.setMaxSize(c.largeur(), c.hauteur());
        noeud.relocate(c.x(), c.y());

        if (contenu.type() == TypeCase.MATCH) {
            Match m = c.match();
            if (m == selectionne) {
                noeud.getStyleClass().add("arbre-selectionne");
            }
            StringBuilder bulle = new StringBuilder(PresentationPhaseFinale.etatMatch(m))
                    .append(" · ").append(PresentationPhaseFinale.formatMatch(m));
            String detail = PresentationPhaseFinale.detailScore(m);
            if (!detail.isEmpty()) {
                bulle.append("\n").append(detail);
            }
            Tooltip.install(noeud, new Tooltip(bulle.toString()));
            if (PresentationPhaseFinale.estSaisissable(m)) {
                noeud.setCursor(Cursor.HAND);
                noeud.setOnMouseClicked(e -> {
                    selectionne = m;
                    rafraichir();
                });
            }
        } else {
            String detail = PresentationPhaseFinale.detailJoueur(contenu.joueurExempte());
            Tooltip.install(noeud, new Tooltip(PresentationPhaseFinale.AIDE_EXEMPT
                    + (detail.isEmpty() ? "" : "\n" + detail)));
        }
        return noeud;
    }

    private Region carteMatch(Match m) {
        VBox lignes = new VBox(2, ligneJoueur(m, true), ligneJoueur(m, false));
        lignes.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lignes, Priority.ALWAYS);
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        String table = DispositionArbre.libelleTable(m);
        if (!table.isEmpty()) {
            // Numéro de table en gros, lisible de loin.
            Label numero = new Label(String.valueOf(m.getNumeroTable()));
            numero.getStyleClass().add("arbre-numero-table");
            numero.setMinWidth(Region.USE_PREF_SIZE);
            Tooltip.install(numero, new Tooltip(table));
            h.getChildren().add(numero);
        }
        h.getChildren().add(lignes);
        return h;
    }

    private static Region carteExempt(Joueur joueur) {
        Label nom = new Label(PresentationPhaseFinale.nomJoueur(joueur));
        nom.getStyleClass().add("arbre-nom");
        nom.setTextOverrun(OverrunStyle.ELLIPSIS);
        nom.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nom, Priority.ALWAYS);
        Label badge = new Label(PresentationPhaseFinale.LIBELLE_EXEMPT);
        badge.getStyleClass().add("arbre-badge-exempt");
        badge.setMinWidth(Region.USE_PREF_SIZE);
        HBox ligne = Ui.ligne(6, nom, badge);
        Label aide = new Label(PresentationPhaseFinale.AIDE_EXEMPT);
        aide.getStyleClass().add("arbre-aide");
        VBox v = new VBox(2, ligne, aide);
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    private static Node ligneJoueur(Match m, boolean joueurA) {
        Joueur j = joueurA ? m.getJoueurA() : m.getJoueurB();
        Label nom = new Label(PresentationPhaseFinale.nomJoueur(j));
        nom.getStyleClass().add("arbre-nom");
        nom.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (!PresentationPhaseFinale.estConnu(j)) {
            nom.getStyleClass().add("arbre-inconnu");
        }
        nom.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nom, Priority.ALWAYS);
        Label manches = new Label(PresentationPhaseFinale.manchesDe(m, joueurA));
        manches.getStyleClass().add("arbre-manches");
        manches.setMinWidth(Region.USE_PREF_SIZE);
        HBox h = Ui.ligne(6, nom, manches);
        h.getStyleClass().add("arbre-ligne-joueur");
        if (PresentationPhaseFinale.estVainqueur(m, joueurA)) {
            h.getStyleClass().add("arbre-vainqueur");
        }
        return h;
    }

    // ==================================================================
    // Saisie du score (panneau de droite)
    // ==================================================================

    private Node panneauSaisie(Match m) {
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
        if (!champs.isEmpty()) {
            // Saisie au clavier dès l'ouverture : Tab passe d'un champ à l'autre, Entrée enregistre.
            Platform.runLater(champs.get(0)[0]::requestFocus);
        }

        Label message = Ui.message();
        Button enregistrer = Ui.boutonPrimaire(dejaJoue ? "Corriger le résultat" : "Enregistrer le résultat");
        enregistrer.setDefaultButton(true);
        enregistrer.setOnAction(e -> enregistrerResultat(m, issue.getValue(), fautif.getValue(), champs, message));
        Button fermer = Ui.boutonSecondaire("Fermer");
        fermer.setCancelButton(true);
        fermer.setOnAction(e -> {
            selectionne = null;
            rafraichir();
        });

        VBox panneau = new VBox(14, titre, adversaires, format,
                Ui.champEtiquete("Résultat", issue), blocScore, blocFautif, message, Ui.actions(enregistrer, fermer));
        if (dejaJoue) {
            panneau.getChildren().add(3, rappel("Ce match est déjà joué : vous corrigez son résultat."));
        }
        panneau.getStyleClass().addAll("carte", "pf-panneau-saisie");
        panneau.setPrefWidth(340);
        panneau.setMaxWidth(340);
        ScrollPane defilant = Ui.defilant(panneau);
        defilant.getStyleClass().add("pf-cote");
        BorderPane.setMargin(defilant, new Insets(24, 24, 24, 0));
        return defilant;
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

    private void enregistrerResultat(Match m, IssueMatch issue, Joueur fautif,
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
        selectionne = null;
        messageSucces = PresentationPhaseFinale.messageResultat(m, tableau.getPhaseFinale());
        enregistrerPuisRafraichir();
    }

    // ==================================================================
    // Enregistrement
    // ==================================================================

    private void enregistrerPuisRafraichir() {
        if (fichier != null && contexte != null && tournoi != null) {
            try {
                contexte.enregistrerTournoi(tournoi, fichier);
            } catch (RuntimeException ex) {
                messageSucces = null;
                messageErreur = "Le résultat est saisi mais l'enregistrement du tournoi a échoué : "
                        + ex.getMessage();
            }
        }
        rafraichir();
    }
}
