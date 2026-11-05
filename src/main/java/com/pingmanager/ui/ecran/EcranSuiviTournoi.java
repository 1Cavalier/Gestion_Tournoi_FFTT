package com.pingmanager.ui.ecran;

import com.pingmanager.core.SuiviTournoi;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.EntreeTableau;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.LigneMatch;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.LignePoule;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.ProchainePartie;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Fiche d'un tournoi en <b>mode suivi</b> (onglet Tournois et « Mes tournois »),
 * dès le lancement du premier tableau : barre des tableaux avec leur étape, et
 * pour le tableau choisi les matchs en cours avec leur table, le podium, l'arbre
 * du tableau final et les poules avec leur classement en direct. Le joueur
 * connecté et inscrit voit ses tableaux en premier et sa prochaine partie en
 * tête.
 *
 * <p><b>Lecture seule</b> : aucune saisie ni envoi sur une table (c'est le
 * juge-arbitre qui pilote depuis la page Jour J). Le fichier du tournoi est relu
 * par le bouton « Actualiser » et toutes les 30 s tant que l'écran est affiché.
 * Toute la logique est dans {@link PresentationSuiviTournoi} (testée).</p>
 */
final class EcranSuiviTournoi {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Runnable onFiche;
    private final BorderPane racine = new BorderPane();
    private final Timeline minuterie;

    private Tournoi tournoi;
    private String nomTableau;
    private String messageErreur;
    private LocalTime derniereLecture = LocalTime.now();
    private ScrollPane defilementPage;

    /**
     * @param onRetour retour à la liste des tournois
     * @param onFiche  ouverture de la fiche complète (informations, documents, inscrits)
     */
    EcranSuiviTournoi(ContexteApplication contexte, Path fichier, Runnable onRetour, Runnable onFiche) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.onFiche = onFiche;
        minuterie = new Timeline(new KeyFrame(
                Duration.seconds(PresentationSuiviTournoi.INTERVALLE_ACTUALISATION_SECONDES), e -> actualiser()));
        minuterie.setCycleCount(Animation.INDEFINITE);
        // La relecture automatique ne tourne que tant que l'écran est affiché.
        racine.sceneProperty().addListener((o, avant, apres) -> {
            if (apres == null) {
                minuterie.stop();
            } else {
                minuterie.play();
            }
        });
    }

    /** Construit l'écran à partir du tournoi déjà chargé. */
    Node build(Tournoi charge) {
        tournoi = charge;
        derniereLecture = LocalTime.now();
        afficher();
        return racine;
    }

    // ------------------------------------------------------------------
    // Relecture
    // ------------------------------------------------------------------

    private void actualiser() {
        try {
            tournoi = contexte.chargerTournoi(fichier);
            derniereLecture = LocalTime.now();
            messageErreur = null;
        } catch (RuntimeException ex) {
            messageErreur = PresentationSuiviTournoi.erreurRelecture(ex.getMessage());
        }
        afficher();
    }

    private String licence() {
        return contexte.ficheLicenceLiee().map(FicheLicence::getNumeroLicence).orElse(null);
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    private void afficher() {
        double position = defilementPage == null ? 0 : defilementPage.getVvalue();
        String licence = licence();
        List<EntreeTableau> entrees = PresentationSuiviTournoi.tableaux(tournoi, licence);
        Tableau affiche = PresentationSuiviTournoi.tableauAffiche(entrees, nomTableau);
        nomTableau = affiche == null ? null : affiche.getNom();

        VBox page = Ui.page(1200, entete());
        page.getStyleClass().add("suivi-page");
        if (messageErreur != null) {
            Label message = Ui.message();
            Ui.erreur(message, messageErreur);
            page.getChildren().add(message);
        }
        Node moi = carteProchainePartie(licence);
        if (moi != null) {
            page.getChildren().add(moi);
        }
        page.getChildren().add(barreTableaux(entrees, affiche));
        if (affiche != null) {
            page.getChildren().addAll(detailTableau(affiche, licence));
        }
        defilementPage = Ui.defilant(page);
        racine.setCenter(defilementPage);
        ScrollPane d = defilementPage;
        Platform.runLater(() -> d.setVvalue(position));
    }

    private Node entete() {
        Button retour = Ui.boutonLien("← Retour aux tournois");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre(tournoi.getNom());
        titre.setWrapText(true);
        Label enCours = Ui.badge("En cours");
        enCours.getStyleClass().add("badge-en-cours");
        HBox badges = Ui.ligne(8, enCours, Ui.pastille("Suivi en direct · lecture seule", Ui.ETAT_RECOMMANDE));
        Label miseAJour = Ui.discret(PresentationSuiviTournoi.texteMiseAJour(derniereLecture));
        VBox titres = new VBox(6, retour, titre, badges, miseAJour);

        Button actualiser = Ui.boutonSecondaire("Actualiser");
        actualiser.setOnAction(e -> actualiser());
        Button fiche = Ui.boutonLien("Informations du tournoi");
        fiche.setOnAction(e -> onFiche.run());
        return Ui.enteteAvecActions(titres, fiche, actualiser);
    }

    /** La prochaine partie du joueur connecté, ou un mot s'il n'a rien à jouer ; {@code null} sinon. */
    private Node carteProchainePartie(String licence) {
        Optional<ProchainePartie> p = PresentationSuiviTournoi.prochainePartie(tournoi, licence);
        if (p.isEmpty()) {
            String texte = PresentationSuiviTournoi.texteSansProchainePartie(tournoi, licence);
            return texte.isEmpty() ? null : Ui.carteNue(Ui.discret(texte));
        }
        ProchainePartie partie = p.get();
        Label table = Ui.badge(partie.table());
        table.getStyleClass().add("suivi-table");
        Label adversaire = Ui.gras(partie.adversaire());
        adversaire.setWrapText(true);
        VBox c = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte(partie.titre()), table),
                adversaire, Ui.discret(partie.detail()));
        c.getStyleClass().addAll("carte-moi", "suivi-prochaine-partie");
        if (partie.enCours()) {
            c.getStyleClass().add("suivi-en-cours");
        }
        return c;
    }

    /** Barre des tableaux : un bouton par tableau (nom + étape), le tableau affiché mis en avant. */
    private Node barreTableaux(List<EntreeTableau> entrees, Tableau affiche) {
        FlowPane barre = new FlowPane(8, 8);
        barre.getStyleClass().add("suivi-barre-tableaux");
        for (EntreeTableau e : entrees) {
            Label nom = Ui.gras(e.nom());
            Label etape = Ui.label(e.etape(), "suivi-etape");
            etape.getStyleClass().add(e.classeEtape());
            VBox contenu = new VBox(4, nom, etape);
            if (e.estAMoi()) {
                Label mien = Ui.badge("Mon tableau");
                mien.getStyleClass().add("suivi-mon-tableau");
                contenu.getChildren().add(0, mien);
            }
            Button b = new Button();
            b.setGraphic(contenu);
            b.getStyleClass().add("suivi-onglet");
            if (e.tableau() == affiche) {
                b.getStyleClass().add("suivi-onglet-actif");
            }
            b.setOnAction(ev -> {
                nomTableau = e.nom();
                defilementPage = null;
                afficher();
            });
            barre.getChildren().add(b);
        }
        return barre;
    }

    // ------------------------------------------------------------------
    // Tableau choisi
    // ------------------------------------------------------------------

    private static Label nomSansRetour(String nom, boolean vainqueur) {
        Label l = Ui.texte(nom);
        l.setWrapText(false);
        l.setMinWidth(Region.USE_PREF_SIZE);
        if (vainqueur) {
            l.getStyleClass().add("suivi-vainqueur");
        }
        return l;
    }

    private List<Node> detailTableau(Tableau t, String licence) {
        Label etape = Ui.label(PresentationSuiviTournoi.etape(t), "suivi-etape");
        etape.getStyleClass().add(PresentationSuiviTournoi.classeEtape(t));
        Node titre = Ui.ligne(10, Ui.sousTitre(t.getNom()), etape);
        if (!SuiviTournoi.estCommence(t)) {
            return List.of(titre, Ui.carteNue(Ui.discret(PresentationSuiviTournoi.texteNonCommence(t))));
        }
        VBox bloc = new VBox(20, titre, carteMatchsEnCours(t));
        if (PresentationSuiviTournoi.afficherPodium(t)) {
            bloc.getChildren().add(cartePodium(t));
        }
        if (t.aPhaseFinale()) {
            bloc.getChildren().add(Ui.carte("Tableau final",
                    Ui.aide("Lecture seule : les résultats sont saisis par le juge-arbitre. "
                            + "Glissez l'arbre ou utilisez les barres pour le parcourir."),
                    VuePhaseFinale.legendeArbre(), VuePhaseFinale.arbreLectureSeule(t.getPhaseFinale())));
        } else {
            bloc.getChildren().add(Ui.aide(PresentationSuiviTournoi.texteSansArbre(t)));
        }
        if (!t.getPoules().isEmpty()) {
            FlowPane poules = new FlowPane(16, 16);
            poules.getStyleClass().add("suivi-poules");
            for (Poule p : t.getPoules()) {
                poules.getChildren().add(cartePoule(t, p, licence));
            }
            bloc.getChildren().addAll(Ui.titreCarte("Poules"), poules);
        }
        return List.of(bloc);
    }

    private Node carteMatchsEnCours(Tableau t) {
        List<LigneMatch> lignes = PresentationSuiviTournoi.matchsEnCours(t);
        VBox corps = new VBox(0);
        if (lignes.isEmpty()) {
            corps.getChildren().add(Ui.discret(PresentationSuiviTournoi.aucunMatchEnCours()));
        }
        for (LigneMatch l : lignes) {
            corps.getChildren().add(ligneMatch(l));
        }
        return Ui.carte(Ui.titreAvecCompteur("Matchs en cours", lignes.size()), corps);
    }

    private Node cartePodium(Tableau t) {
        VBox corps = new VBox(0);
        for (PresentationPhaseFinale.LignePodium ligne : PresentationSuiviTournoi.podium(t)) {
            Label place = Ui.badge(ligne.place());
            place.getStyleClass().add("pf-place");
            HBox h = Ui.ligneListe(place, Ui.gras(ligne.joueur()), Ui.discret(ligne.detail()));
            h.getStyleClass().add("pf-ligne-podium");
            corps.getChildren().add(h);
        }
        return Ui.carte("Podium", corps);
    }

    private Node cartePoule(Tableau t, Poule p, String licence) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(6);
        g.getStyleClass().add("suivi-classement");
        int ligne = 0;
        for (LignePoule l : PresentationSuiviTournoi.classement(t, p, licence)) {
            Label rang = Ui.label(l.rang(), "suivi-rang");
            rang.getStyleClass().add(l.classe());
            Label nom = Ui.gras(l.joueur());
            if (l.estMoi()) {
                nom.getStyleClass().add("suivi-moi");
            }
            VBox joueur = new VBox(1, nom, Ui.discret(l.detail()));
            GridPane.setHgrow(joueur, Priority.ALWAYS);
            g.add(rang, 0, ligne);
            g.add(joueur, 1, ligne);
            Label bilan = Ui.discret(l.bilan());
            bilan.setMinWidth(Region.USE_PREF_SIZE);
            g.add(bilan, 2, ligne);
            if (!l.qualification().isEmpty()) {
                Label q = Ui.badge(l.qualification());
                q.getStyleClass().add(l.classe());
                g.add(q, 3, ligne);
            }
            ligne++;
        }
        VBox matchs = new VBox(0);
        for (LigneMatch l : PresentationSuiviTournoi.matchsPoule(p)) {
            matchs.getChildren().add(ligneMatch(l));
        }
        VBox c = Ui.carte(PresentationSuiviTournoi.titrePoule(p), g, matchs);
        c.getStyleClass().add("suivi-poule");
        c.setPrefWidth(540);
        return c;
    }

    /**
     * Une ligne de match en lecture seule : situation, joueurs (vainqueur en
     * gras) avec le détail des manches dessous, résultat ou état, table.
     */
    private static Node ligneMatch(LigneMatch l) {
        Label situation = Ui.discret(l.situation());
        situation.getStyleClass().add("suivi-situation");
        situation.setMinWidth(Region.USE_PREF_SIZE);
        Label a = nomSansRetour(l.joueurA(), l.vainqueurA());
        Label b = nomSansRetour(l.joueurB(), l.vainqueurB());
        VBox joueurs = new VBox(2, Ui.ligne(6, a, Ui.discret("–"), b));
        if (!l.detail().isEmpty()) {
            Label detail = Ui.discret(l.detail());
            detail.getStyleClass().add("suivi-detail-score");
            joueurs.getChildren().add(detail);
        }
        HBox.setHgrow(joueurs, Priority.ALWAYS);
        Label resultat = Ui.discret(l.resultat());
        resultat.setMinWidth(Region.USE_PREF_SIZE);
        HBox h = Ui.ligneListe(situation, joueurs, resultat);
        h.setAlignment(Pos.CENTER_LEFT);
        if (!l.table().isEmpty()) {
            Label table = Ui.badge(l.table());
            table.getStyleClass().add("suivi-table");
            h.getChildren().add(table);
        }
        if (l.enCours()) {
            h.getStyleClass().add("suivi-ligne-en-cours");
        }
        return h;
    }
}
