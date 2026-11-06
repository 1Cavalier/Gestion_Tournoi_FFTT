package com.pingmanager.ui.ecran;

import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.ProjetTournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ecran.PresentationGestionTournois.Actuel;
import com.pingmanager.ui.ecran.PresentationGestionTournois.Etape;
import com.pingmanager.ui.ecran.PresentationGestionTournois.Fiche;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Vue de l'accueil « Gestion des tournois » (B033) : tournoi actuel, gros
 * bouton de création, frise verticale des étapes et historique des tournois
 * passés. Vue mince : tout ce qui est décidé (répartition, ordre, libellés)
 * vient de {@link PresentationGestionTournois}.
 */
final class AccueilGestionTournois {

    private final ContexteApplication contexte;
    private final Consumer<Node> poser;
    private final Node clubs;
    private final Runnable creer;
    private final Consumer<Path> ouvrir;
    private final Consumer<Path> jourJ;

    /**
     * @param poser  pose un contenu (déjà défilant ou non) au centre de la page
     * @param clubs  ligne « Club : … » de l'en-tête
     * @param creer  ouvre le formulaire de création
     * @param ouvrir ouvre un tournoi (complétion, import ou choix du mode)
     * @param jourJ  ouvre le jour J d'un tournoi
     */
    AccueilGestionTournois(ContexteApplication contexte, Consumer<Node> poser, Node clubs,
                           Runnable creer, Consumer<Path> ouvrir, Consumer<Path> jourJ) {
        this.contexte = contexte;
        this.poser = poser;
        this.clubs = clubs;
        this.creer = creer;
        this.ouvrir = ouvrir;
        this.jourJ = jourJ;
    }

    /** Pose l'accueil (tournoi actuel + frise). */
    void afficher() {
        PresentationGestionTournois p = presentation();

        Button creerTournoi = Ui.boutonPrimaire("+  " + PresentationGestionTournois.BOUTON_CREER);
        creerTournoi.getStyleClass().add("bouton-creer-tournoi");
        creerTournoi.setOnAction(e -> creer.run());
        Label aideCreer = Ui.aide(PresentationGestionTournois.AIDE_CREER);
        VBox blocCreer = new VBox(6, creerTournoi, aideCreer);
        blocCreer.getStyleClass().add("bloc-creer-tournoi");

        Button historique = Ui.boutonLien(p.lienHistorique() + "  →");
        historique.setOnAction(e -> afficherHistorique());

        VBox titres = new VBox(4, Ui.titre(PresentationGestionTournois.TITRE), clubs, historique);
        HBox entete = Ui.enteteAvecActions(titres, blocCreer);

        poser.accept(Ui.page(1040, entete, sectionActuel(p), frise(p)));
    }

    private PresentationGestionTournois presentation() {
        List<Fiche> fiches = new ArrayList<>();
        for (ProjetTournoi projet : contexte.tournoisRecents()) {
            try {
                Tournoi t = contexte.lireTournoi(projet.getFichier());
                if (contexte.estTournoiDuCompte(t)) { // seulement les tournois des clubs du compte
                    fiches.add(Fiche.depuis(t, projet.getFichier()));
                }
            } catch (RuntimeException ignore) {
                // Fichier déplacé, supprimé ou illisible : on ne l'affiche pas.
            }
        }
        return new PresentationGestionTournois(fiches, LocalDate.now());
    }

    // ------------------------------------------------------------------
    // Tournoi actuel
    // ------------------------------------------------------------------

    private Node sectionActuel(PresentationGestionTournois p) {
        Optional<Actuel> actuel = p.actuel();
        if (actuel.isEmpty()) {
            VBox carte = Ui.carte(PresentationGestionTournois.TITRE_ACTUEL,
                    Ui.etatVide(PresentationGestionTournois.ACTUEL_VIDE, null));
            carte.getStyleClass().add("carte-actuel-vide");
            return carte;
        }
        Actuel a = actuel.get();
        Fiche f = a.fiche();
        Label nom = Ui.label(f.nom(), "en-cours-nom");
        Label etat = f.jourJEntame()
                ? Ui.pastille(PresentationGestionTournois.PASTILLE_JOUR_J, StatutPastille.INFO)
                : Ui.pastille(StatutPastille.libelleTournoi(f.statut()), StatutPastille.pourTournoi(f.statut()));
        VBox infos = new VBox(6, Ui.ligne(10, nom, etat), Ui.discret(PresentationGestionTournois.detailActuel(f)));
        HBox.setHgrow(infos, Priority.ALWAYS);

        Button suivre = Ui.boutonPrimaire(PresentationGestionTournois.BOUTON_JOUR_J);
        suivre.setOnAction(e -> jourJ.accept(f.fichier()));
        Button details = Ui.boutonSecondaire(PresentationGestionTournois.libelleAction(f));
        details.setOnAction(e -> ouvrir.accept(f.fichier()));

        VBox entete = new VBox(2, Ui.titreCarte(PresentationGestionTournois.TITRE_ACTUEL),
                Ui.label(a.surtitre(), "surtitre"));
        VBox carte = Ui.carte(entete, Ui.ligne(16, Ui.blocDate(f.debut()), infos, details, suivre));
        carte.getStyleClass().add("carte-en-cours");
        return carte;
    }

    // ------------------------------------------------------------------
    // Frise verticale
    // ------------------------------------------------------------------

    private Node frise(PresentationGestionTournois p) {
        VBox frise = new VBox(0);
        frise.getStyleClass().add("frise");
        List<Etape> etapes = p.frise();
        for (int i = 0; i < etapes.size(); i++) {
            frise.getChildren().add(etape(p, etapes.get(i), i == etapes.size() - 1));
        }
        VBox entete = new VBox(2, Ui.titreCarte(PresentationGestionTournois.TITRE_FRISE),
                Ui.aide(PresentationGestionTournois.AIDE_FRISE));
        return Ui.carte(entete, frise);
    }

    private Node etape(PresentationGestionTournois p, Etape etape, boolean derniere) {
        // Repère à gauche : le numéro dans une pastille ronde, puis le trait vertical.
        Label numero = Ui.label(String.valueOf(etape.numero()), "frise-numero");
        numero.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        Region trait = new Region();
        trait.getStyleClass().add("frise-trait");
        VBox.setVgrow(trait, Priority.ALWAYS);
        VBox repere = new VBox(6, numero, trait);
        repere.getStyleClass().add("frise-repere");

        HBox titre = Ui.ligne(10, Ui.label(etape.libelle().toUpperCase(java.util.Locale.FRENCH), "surtitre"),
                Ui.pastille(String.valueOf(etape.tournois().size()), etape.estVide()
                        ? StatutPastille.NEUTRE : StatutPastille.pourTournoi(etape.statut())));
        VBox contenu = new VBox(4, titre, Ui.label(etape.titre(), "frise-etape-titre"), Ui.aide(etape.aide()));
        contenu.getStyleClass().add("frise-contenu");
        HBox.setHgrow(contenu, Priority.ALWAYS);

        VBox liste = new VBox(0);
        liste.getStyleClass().add("frise-liste");
        if (etape.estVide()) {
            // Étape vide : une simple mention discrète, pour que l'œil aille aux étapes remplies.
            liste.getChildren().add(Ui.label(PresentationGestionTournois.ETAPE_VIDE, "frise-vide"));
        } else {
            for (int i = 0; i < etape.tournois().size(); i++) {
                HBox l = ligneTournoi(p, etape.tournois().get(i));
                if (i == 0) {
                    l.getStyleClass().add("ligne-element-premiere");
                }
                liste.getChildren().add(l);
            }
        }
        contenu.getChildren().add(liste);

        HBox ligne = new HBox(16, repere, contenu);
        ligne.getStyleClass().add("frise-etape");
        ligne.getStyleClass().add(etape.estVide() ? "frise-etape-vide" : "frise-etape-active");
        if (derniere) {
            ligne.getStyleClass().add("frise-etape-derniere");
        }
        return ligne;
    }

    private HBox ligneTournoi(PresentationGestionTournois p, Fiche f) {
        List<Node> droite = new ArrayList<>();
        if (p.estActuel(f)) {
            droite.add(Ui.pastille(PresentationGestionTournois.PASTILLE_ACTUEL, StatutPastille.SUCCES));
        }
        String mode = PresentationGestionTournois.pastilleMode(f);
        if (mode != null) {
            droite.add(Ui.pastille(mode, PresentationGestionTournois.PASTILLE_MODE.equals(mode)
                    ? StatutPastille.ATTENTION : StatutPastille.INFO));
        }
        if (f.jourJEntame()) {
            droite.add(Ui.pastille(PresentationGestionTournois.PASTILLE_JOUR_J, StatutPastille.INFO));
        }
        Button action = Ui.boutonSecondaire(PresentationGestionTournois.libelleAction(f));
        action.setOnAction(e -> ouvrir.accept(f.fichier()));
        droite.add(action);
        return cliquable(Ui.ligneElement(Ui.blocDate(f.debut()), f.nom(),
                PresentationGestionTournois.detailCourt(f), droite.toArray(new Node[0])), f);
    }

    private HBox cliquable(HBox ligne, Fiche f) {
        ligne.getStyleClass().add("ligne-tournoi-cliquable");
        ligne.setOnMouseClicked(e -> ouvrir.accept(f.fichier()));
        // Un clic sur un bouton de la ligne ne doit pas ouvrir le tournoi une seconde fois.
        for (Node n : ligne.getChildren()) {
            if (n instanceof Button) {
                n.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);
            }
        }
        return ligne;
    }

    // ------------------------------------------------------------------
    // Historique
    // ------------------------------------------------------------------

    /** Pose la liste des tournois passés du club. */
    void afficherHistorique() {
        PresentationGestionTournois p = presentation();
        Button retour = Ui.boutonLien(PresentationGestionTournois.RETOUR);
        retour.setOnAction(e -> afficher());

        VBox liste = new VBox(0);
        List<Fiche> passes = p.historique();
        if (passes.isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact(PresentationGestionTournois.HISTORIQUE_VIDE));
        }
        for (int i = 0; i < passes.size(); i++) {
            Fiche f = passes.get(i);
            Button ouvrirTournoi = Ui.boutonSecondaire("Consulter");
            ouvrirTournoi.setOnAction(e -> ouvrir.accept(f.fichier()));
            Label etat = Ui.pastille(PresentationGestionTournois.libelleHistorique(f),
                    f.statut() == com.pingmanager.model.StatutTournoi.ANNULE
                            ? StatutPastille.ERREUR : StatutPastille.NEUTRE);
            HBox l = cliquable(Ui.ligneElement(Ui.blocDate(f.debut()), f.nom(),
                    PresentationGestionTournois.detailCourt(f), etat, ouvrirTournoi), f);
            if (i == 0) {
                l.getStyleClass().add("ligne-element-premiere");
            }
            liste.getChildren().add(l);
        }
        VBox entete = new VBox(2, Ui.titreCarte(PresentationGestionTournois.TITRE_HISTORIQUE),
                Ui.aide(PresentationGestionTournois.AIDE_HISTORIQUE));
        poser.accept(Ui.page(1040, retour, Ui.titre(PresentationGestionTournois.TITRE_HISTORIQUE),
                Ui.carte(entete, liste)));
    }
}
