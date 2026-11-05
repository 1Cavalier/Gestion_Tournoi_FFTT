package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.ResultatConstructionPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.CaseTableau;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.Colonne;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.LignePodium;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.TypeCase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vue « Phase finale » d'un tableau : construction du tableau final une fois
 * les poules terminées (second tirage), affichage tour par tour avec les
 * exempts, saisie des scores (panneau à droite), progression automatique des
 * vainqueurs, petite finale et podium.
 *
 * <p>Toute la logique est dans {@link ServicePhaseFinale} (cœur) et
 * {@link PresentationPhaseFinale} / {@link SaisieScorePhaseFinale} (sans
 * JavaFX, testées) ; cette classe ne fait que poser les composants. Chaque
 * modification réussie enregistre le tournoi, comme les autres écrans.</p>
 *
 * <p>Point d'entrée : {@link #creer(ContexteApplication, Tournoi, Tableau, Path)}.</p>
 */
public final class VuePhaseFinale {

    /** Hauteur fixe d'une case : l'alignement des colonnes en dépend. */
    private static final double HAUTEUR_CASE = 64;
    /** Écart vertical entre deux cases du premier tour. */
    private static final double ECART_CASES = 12;
    private static final double LARGEUR_CASE = 236;

    /**
     * Graines des tirages faits pendant la session, par fichier et tableau. La
     * graine n'est pas (encore) enregistrée dans le fichier du tournoi : voir
     * QUESTIONS.md.
     */
    private static final Map<String, Long> GRAINES = new ConcurrentHashMap<>();

    private final ContexteApplication contexte;
    private final Tournoi tournoi;
    private final Tableau tableau;
    private final Path fichier;
    private final ServicePhaseFinale service = new ServicePhaseFinale();

    private final BorderPane racine = new BorderPane();
    private Match selectionne;
    private String messageSucces;
    private String messageErreur;

    private VuePhaseFinale(ContexteApplication contexte, Tournoi tournoi, Tableau tableau, Path fichier) {
        this.contexte = contexte;
        this.tournoi = tournoi;
        this.tableau = Objects.requireNonNull(tableau, "tableau");
        this.fichier = fichier;
    }

    /**
     * Construit la vue « Phase finale » d'un tableau.
     *
     * @param contexte contexte de l'application (enregistrement du tournoi)
     * @param tournoi  le tournoi qui contient le tableau
     * @param tableau  le tableau dont on suit la phase finale
     * @param fichier  fichier du tournoi, où chaque modification est enregistrée ;
     *                 {@code null} pour un tableau d'essai (rien n'est enregistré)
     * @return le nœud racine de la vue (il gère son propre défilement)
     */
    public static Parent creer(ContexteApplication contexte, Tournoi tournoi, Tableau tableau, Path fichier) {
        VuePhaseFinale vue = new VuePhaseFinale(contexte, tournoi, tableau, fichier);
        vue.rafraichir();
        return vue.racine;
    }

    // ==================================================================
    // Rendu
    // ==================================================================

    private void rafraichir() {
        DiagnosticPhaseFinale diagnostic = service.diagnostiquer(tableau);
        VBox contenu = new VBox(20);
        contenu.getStyleClass().add("page");
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
        if (fichier == null) {
            contenu.getChildren().add(rappel("Tableau d'essai : les poules ont été jouées au hasard et rien "
                    + "n'est enregistré. Le vrai tableau n'est pas modifié."));
        }
        if (diagnostic.estConstruite()) {
            TableauElimination elim = tableau.getPhaseFinale();
            contenu.getChildren().addAll(carteResume(diagnostic, elim), cartePodium(elim), carteTableau(elim));
            Match pf = PresentationPhaseFinale.matchPetiteFinale(elim);
            if (pf != null) {
                contenu.getChildren().add(cartePetiteFinale(pf));
            }
        } else {
            contenu.getChildren().add(carteConstruction(diagnostic));
        }
        racine.setCenter(Ui.defilant(contenu));
        racine.setRight(selectionne == null ? null : panneauSaisie(selectionne));
    }

    private Node entete(DiagnosticPhaseFinale diagnostic) {
        String sous = tournoi == null || tournoi.getNom() == null ? "" : tournoi.getNom();
        VBox titres = new VBox(4, Ui.titre("Phase finale — " + tableau.getNom()), Ui.discret(sous));
        String etat = diagnostic.getEtat() == DiagnosticPhaseFinale.Etat.TERMINEE ? Ui.ETAT_OK
                : diagnostic.estConstruite() || diagnostic.peutConstruire() ? Ui.ETAT_RECOMMANDE : Ui.ETAT_OPTIONNEL;
        return Ui.enteteAvecActions(titres, Ui.pastille(PresentationPhaseFinale.titreEtat(diagnostic), etat));
    }

    static Node rappel(String texte) {
        Label l = Ui.texte(texte);
        VBox v = new VBox(l);
        v.getStyleClass().add("rappel-regles");
        return v;
    }

    // ------------------------------------------------------------------
    // Avant le tirage
    // ------------------------------------------------------------------

    private Node carteConstruction(DiagnosticPhaseFinale diagnostic) {
        VBox corps = new VBox(12, Ui.texte(PresentationPhaseFinale.messageEtat(diagnostic)));
        if (diagnostic.peutConstruire()) {
            TextField graine = Ui.champ("Laisser vide pour un tirage au hasard");
            VBox champGraine = Ui.champEtiquete("Graine du tirage (facultatif)", graine);
            champGraine.setMaxWidth(360);
            Label erreur = Ui.message();
            Button tirer = Ui.boutonPrimaire("Tirer le tableau final");
            tirer.setDefaultButton(true);
            tirer.setOnAction(e -> construire(graine, erreur));
            corps.getChildren().addAll(
                    champGraine,
                    Ui.aide("Pour refaire un tirage déjà publié, saisissez sa graine : le même tirage sera "
                            + "reproduit à l'identique."),
                    erreur,
                    Ui.actions(tirer));
        }
        return Ui.carte("Tableau final", corps);
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
        GRAINES.put(cleGraine(), r.getGraine());
        messageSucces = "Tableau final tiré (graine " + r.getGraine() + ").";
        enregistrerPuisRafraichir();
    }

    private String cleGraine() {
        String origine = fichier == null ? "essai@" + System.identityHashCode(tableau) : fichier.toAbsolutePath().toString();
        return origine + "|" + tableau.getNom();
    }

    // ------------------------------------------------------------------
    // Tableau construit
    // ------------------------------------------------------------------

    private Node carteResume(DiagnosticPhaseFinale diagnostic, TableauElimination elim) {
        VBox corps = new VBox(8,
                Ui.gras(PresentationPhaseFinale.resume(elim)),
                Ui.aide(PresentationPhaseFinale.texteGraine(GRAINES.get(cleGraine()))),
                Ui.texte(PresentationPhaseFinale.messageEtat(diagnostic)));
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

    private Node carteTableau(TableauElimination elim) {
        List<Colonne> colonnes = PresentationPhaseFinale.colonnes(elim);
        HBox arbre = new HBox(24);
        arbre.getStyleClass().add("pf-arbre");
        for (int i = 0; i < colonnes.size(); i++) {
            arbre.getChildren().add(colonne(colonnes.get(i), i));
        }
        ScrollPane defilement = new ScrollPane(arbre);
        defilement.getStyleClass().add("pf-defilement");
        defilement.setFitToHeight(true);
        defilement.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return Ui.carte("Tableau", Ui.aide("Les matchs à jouer sont encadrés : cliquez dessus pour saisir le "
                + "score. Un joueur « Exempt » n'a pas d'adversaire à ce tour."), defilement);
    }

    private Node colonne(Colonne colonne, int niveau) {
        double[] alignement = PresentationPhaseFinale.alignement(niveau, HAUTEUR_CASE, ECART_CASES);
        VBox cases = new VBox(alignement[1]);
        cases.setPadding(new Insets(alignement[0], 0, 0, 0));
        for (CaseTableau c : colonne.cases()) {
            cases.getChildren().add(caseTableau(c));
        }
        Label titre = Ui.enteteColonne(colonne.libelle());
        titre.getStyleClass().add("pf-titre-colonne");
        titre.setMinHeight(24);
        titre.setPrefHeight(24);
        VBox v = new VBox(10, titre, cases);
        v.setMinWidth(LARGEUR_CASE);
        return v;
    }

    private Node caseTableau(CaseTableau c) {
        if (c.type() == TypeCase.MATCH) {
            return caseMatch(c.match());
        }
        if (c.type() == TypeCase.EXEMPT) {
            return caseExempt(c.joueurExempte());
        }
        Region vide = new Region();
        dimensionner(vide);
        return vide;
    }

    private static void dimensionner(Region r) {
        r.setMinHeight(HAUTEUR_CASE);
        r.setPrefHeight(HAUTEUR_CASE);
        r.setMaxHeight(HAUTEUR_CASE);
        r.setMinWidth(LARGEUR_CASE);
        r.setPrefWidth(LARGEUR_CASE);
        r.setMaxWidth(LARGEUR_CASE);
    }

    private Node caseExempt(Joueur joueur) {
        Label nom = new Label(PresentationPhaseFinale.nomJoueur(joueur));
        nom.getStyleClass().add("pf-nom");
        HBox.setHgrow(nom, Priority.ALWAYS);
        nom.setMaxWidth(Double.MAX_VALUE);
        Label badge = Ui.badge(PresentationPhaseFinale.LIBELLE_EXEMPT);
        HBox ligne = Ui.ligne(6, nom, badge);
        Label aide = new Label(PresentationPhaseFinale.AIDE_EXEMPT);
        aide.getStyleClass().add("pf-aide-case");
        VBox v = new VBox(4, ligne, aide);
        v.getStyleClass().addAll("pf-case", "pf-exempt");
        v.setAlignment(Pos.CENTER_LEFT);
        dimensionner(v);
        String detail = PresentationPhaseFinale.detailJoueur(joueur);
        if (!detail.isEmpty()) {
            Tooltip.install(v, new Tooltip(detail));
        }
        return v;
    }

    private Node caseMatch(Match m) {
        VBox v = new VBox(2, ligneJoueur(m, true), ligneJoueur(m, false));
        v.getStyleClass().add("pf-case");
        v.setAlignment(Pos.CENTER_LEFT);
        if (m.getStatut() == StatutMatch.TERMINE) {
            v.getStyleClass().add("pf-termine");
        } else if (PresentationPhaseFinale.estSaisissable(m)) {
            v.getStyleClass().add("pf-a-jouer");
        } else {
            v.getStyleClass().add("pf-attente");
        }
        if (m == selectionne) {
            v.getStyleClass().add("pf-selectionne");
        }
        dimensionner(v);

        StringBuilder bulle = new StringBuilder(PresentationPhaseFinale.etatMatch(m))
                .append(" · ").append(PresentationPhaseFinale.formatMatch(m));
        String detail = PresentationPhaseFinale.detailScore(m);
        if (!detail.isEmpty()) {
            bulle.append("\n").append(detail);
        }
        Tooltip.install(v, new Tooltip(bulle.toString()));

        if (PresentationPhaseFinale.estSaisissable(m)) {
            v.setCursor(Cursor.HAND);
            v.setOnMouseClicked(e -> {
                selectionne = m;
                rafraichir();
            });
        }
        return v;
    }

    private static Node ligneJoueur(Match m, boolean joueurA) {
        Joueur j = joueurA ? m.getJoueurA() : m.getJoueurB();
        Label nom = new Label(PresentationPhaseFinale.nomJoueur(j));
        nom.getStyleClass().add("pf-nom");
        if (!PresentationPhaseFinale.estConnu(j)) {
            nom.getStyleClass().add("pf-inconnu");
        }
        nom.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nom, Priority.ALWAYS);
        Label manches = new Label(PresentationPhaseFinale.manchesDe(m, joueurA));
        manches.getStyleClass().add("pf-manches");
        HBox h = Ui.ligne(6, nom, manches);
        h.getStyleClass().add("pf-ligne-joueur");
        if (PresentationPhaseFinale.estVainqueur(m, joueurA)) {
            h.getStyleClass().add("pf-vainqueur");
        }
        return h;
    }

    private Node cartePetiteFinale(Match pf) {
        return Ui.carte("Petite finale (3e place)",
                Ui.aide("Les deux perdants des demi-finales se rencontrent pour la 3e place."),
                caseMatch(pf));
    }

    // ==================================================================
    // Saisie du score (panneau de droite)
    // ==================================================================

    private Node panneauSaisie(Match m) {
        VBox panneau = EditeurScorePhaseFinale.creer(tableau, m, service, () -> {
            selectionne = null;
            rafraichir();
        }, joue -> {
            selectionne = null;
            messageSucces = PresentationPhaseFinale.messageResultat(joue, tableau.getPhaseFinale());
            enregistrerPuisRafraichir();
        });
        ScrollPane defilant = Ui.defilant(panneau);
        defilant.getStyleClass().add("pf-cote");
        BorderPane.setMargin(defilant, new Insets(24, 24, 24, 0));
        return defilant;
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
