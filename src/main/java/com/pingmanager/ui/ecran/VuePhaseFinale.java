package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.ResultatConstructionPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
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
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polyline;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

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
            contenu.getChildren().add(Ui.message(messageErreur, messageSucces));
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

    static Node rappel(String texte) {
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
        if (ServicePhaseFinale.estTerminee(elim)) {
            corps.getChildren().add(blocExportResultats());
        }
        return Ui.carte("Podium", corps);
    }

    /**
     * B023 : export des résultats du tableau terminé (parties.csv, classements.csv),
     * format provisoire en attendant le format fédéral (IX.107).
     */
    private Node blocExportResultats() {
        Label message = Ui.discret("");
        message.setWrapText(true);
        Button exporter = Ui.boutonSecondaire("Exporter les résultats (CSV)…");
        exporter.setOnAction(e -> {
            com.pingmanager.core.ResultatPreparationExport prep =
                    new com.pingmanager.core.ServiceResultatsFederaux().construire(tournoi, tableau);
            if (!prep.estExportable()) {
                message.setText(prep.getMotif().getLibelle());
                return;
            }
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Dossier où enregistrer les résultats du tableau « " + tableau.getNom() + " »");
            java.io.File dossier = dc.showDialog(racine.getScene() == null ? null : racine.getScene().getWindow());
            if (dossier == null) {
                return;
            }
            com.pingmanager.importation.ExportResultatsCsv export = new com.pingmanager.importation.ExportResultatsCsv();
            String prefixe = "resultats-" + tableau.getNom().replaceAll("[^\\p{L}\\p{N}]+", "-") + "-";
            StringBuilder sb = new StringBuilder();
            try {
                for (com.pingmanager.core.FichierExporte f : export.produire(prep.getResultats().orElseThrow())) {
                    Path cible = dossier.toPath().resolve(prefixe + f.getNom());
                    java.nio.file.Files.write(cible, f.getContenu());
                    sb.append(sb.length() == 0 ? "Enregistré : " : ", ").append(cible.getFileName());
                }
                sb.append(" (").append(export.nomFormat()).append(").");
                prep.getAvertissements().forEach(a -> sb.append("\n").append(a));
                message.setText(sb.toString());
            } catch (java.io.IOException ex) {
                message.setText("Export impossible : " + ex.getMessage());
            }
        });
        return new VBox(6, exporter, message);
    }

    /** L'arbre des rencontres, dessiné aux coordonnées de {@link DispositionArbre}. */
    private Node carteArbre(TableauElimination elim) {
        ScrollPane defilement = dessinArbre(elim, selectionne, m -> {
            selectionne = m;
            rafraichir();
        });
        defilementArbre = defilement;

        return Ui.carte("Arbre des rencontres",
                Ui.aide("Cliquez sur un match à jouer (cadre bleu) pour saisir son score ; le vainqueur avance "
                        + "aussitôt au tour suivant. Un joueur « Exempt » passe ce tour sans adversaire. "
                        + "Glissez l'arbre ou utilisez les barres pour le parcourir."),
                legende(), defilement);
    }

    /**
     * Arbre des rencontres en <b>lecture seule</b> (suivi public du tournoi) :
     * mêmes cartes, traits et numéros de table, sans clic ni saisie.
     */
    static ScrollPane arbreLectureSeule(TableauElimination elim) {
        return dessinArbre(elim, null, null);
    }

    /** Légende des états des cartes de l'arbre. */
    static Node legendeArbre() {
        return legende();
    }

    /**
     * Dessine l'arbre dans une zone défilante.
     *
     * @param selectionne match mis en évidence ({@code null} : aucun)
     * @param surClic     action au clic sur un match saisissable ({@code null} : lecture seule)
     */
    private static ScrollPane dessinArbre(TableauElimination elim, Match selectionne, Consumer<Match> surClic) {
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
            dessin.getChildren().add(carte(c, selectionne, surClic));
        }
        if (d.petiteFinale() != null) {
            Carte pf = d.petiteFinale();
            dessin.getChildren().addAll(
                    titreColonne(DispositionArbre.TITRE_PETITE_FINALE, pf.x(), d.yTitrePetiteFinale(), pf.largeur()),
                    carte(pf, selectionne, surClic));
        }

        ScrollPane defilement = new ScrollPane(dessin);
        defilement.getStyleClass().add("arbre-defilement");
        defilement.setPannable(true);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        defilement.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        defilement.setPrefViewportHeight(DispositionArbre.hauteurVisible(d, HAUTEUR_ARBRE_MAX));
        defilement.setPrefViewportWidth(d.largeur() + 16);
        return defilement;
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

    private static Node carte(Carte c, Match selectionne, Consumer<Match> surClic) {
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
            if (surClic != null && PresentationPhaseFinale.estSaisissable(m)) {
                noeud.setCursor(Cursor.HAND);
                noeud.setOnMouseClicked(e -> surClic.accept(m));
            }
        } else {
            String detail = PresentationPhaseFinale.detailJoueur(contenu.joueurExempte());
            Tooltip.install(noeud, new Tooltip(PresentationPhaseFinale.AIDE_EXEMPT
                    + (detail.isEmpty() ? "" : "\n" + detail)));
        }
        return noeud;
    }

    private static Region carteMatch(Match m) {
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

    private static HBox ligneJoueur(Match m, boolean joueurA) {
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
