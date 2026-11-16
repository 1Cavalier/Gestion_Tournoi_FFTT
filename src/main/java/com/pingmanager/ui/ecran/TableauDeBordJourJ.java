package com.pingmanager.ui.ecran;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Briques JavaFX de l'écran du jour J du juge-arbitre : bandeau « Jour J »,
 * cartes carrées côte à côte (en colonne sur petite largeur) et frise des six
 * étapes d'un tableau (libellés complets, courts ou numéros seuls selon la
 * place). Mots et choix : {@link PresentationTableauDeBordJourJ}.
 */
final class TableauDeBordJourJ {

    /** Largeur minimale d'une carte pour garder les trois côte à côte. */
    static final double LARGEUR_MIN_CARTE = 230;
    /** Côté maximal d'une carte carrée (au-delà, la carte s'élargit sans grandir). */
    static final double HAUTEUR_MAX_CARTE = 240;

    /** Hauteur de la rangée des ronds (le rond courant, plus grand, y tient). */
    private static final double HAUTEUR_FRISE = 32;

    private TableauDeBordJourJ() {
    }

    /** Bandeau : la mention « Jour J », puis le nom du tournoi en titre. */
    static Node bandeau(String mention, String nomTournoi) {
        Label m = Ui.label(mention, "jourj-bandeau-mention");
        Label nom = Ui.titre(nomTournoi == null || nomTournoi.isBlank() ? "Tournoi" : nomTournoi);
        nom.getStyleClass().add("jourj-bandeau-nom");
        nom.setWrapText(true);
        VBox v = new VBox(2, m, nom);
        v.getStyleClass().add("jourj-bandeau");
        return v;
    }

    /**
     * Une carte du tableau de bord : titre, valeur en grand, détail, bouton.
     * Toute la carte se clique (hors bouton) ; {@code ouverte} la marque comme
     * l'interface affichée.
     */
    static VBox carte(Node icone, String titre, String valeur, String detail, Button bouton, boolean ouverte,
                      Runnable action) {
        Label t = Ui.label(titre, "jourj-carte-titre");
        HBox entete = Ui.ligne(8);
        if (icone != null) {
            entete.getChildren().add(icone);
        }
        entete.getChildren().add(t);
        Label v = Ui.label(valeur, "jourj-carte-valeur");
        v.setWrapText(true);
        v.setMinHeight(Region.USE_PREF_SIZE);
        Label d = Ui.label(detail, "jourj-carte-detail");
        d.setWrapText(true);
        d.setMinHeight(Region.USE_PREF_SIZE);
        Region pousse = new Region();
        VBox.setVgrow(pousse, Priority.ALWAYS);
        bouton.setMaxWidth(Double.MAX_VALUE);
        bouton.setOnAction(e -> action.run());
        VBox carte = new VBox(8, entete, v, d, pousse, bouton);
        carte.getStyleClass().addAll("carte", "jourj-carte");
        if (ouverte) {
            carte.getStyleClass().add("jourj-carte-ouverte");
        }
        carte.setCursor(Cursor.HAND);
        carte.setOnMouseClicked(e -> {
            if (!dansUnBouton(e.getTarget(), carte)) {
                action.run();
            }
        });
        return carte;
    }

    /**
     * Les cartes côte à côte, de même taille et carrées (côté borné à
     * {@link #HAUTEUR_MAX_CARTE}) ; en colonne, pleine largeur, quand la place
     * manque.
     */
    static GridPane cartes(List<VBox> cartes) {
        GridPane g = Ui.grilleAdaptative(cartes.size(), LARGEUR_MIN_CARTE, cartes);
        g.getStyleClass().add("jourj-cartes");
        Runnable carrer = () -> {
            int n = DispositionGrille.colonnes(g.getWidth(), g.getHgap(), cartes.size(), LARGEUR_MIN_CARTE);
            double cote = n > 1 ? Math.min(HAUTEUR_MAX_CARTE, (g.getWidth() - (n - 1) * g.getHgap()) / n) : -1;
            for (VBox c : cartes) {
                c.setMinHeight(n > 1 ? cote : Region.USE_COMPUTED_SIZE);
                c.setPrefHeight(n > 1 ? cote : Region.USE_COMPUTED_SIZE);
            }
        };
        g.widthProperty().addListener((o, a, b) -> carrer.run());
        carrer.run();
        return g;
    }

    /**
     * Frise des six étapes : un rond numéroté par étape, relié au suivant, et
     * son libellé dessous ; l'étape courante en bleu plein. Libellés complets,
     * courts, ou numéros seuls selon la largeur disponible (l'infobulle garde
     * toujours le libellé complet).
     */
    static Node frise(List<PresentationTableauDeBordJourJ.Pas> pas) {
        HBox frise = new HBox(0);
        frise.getStyleClass().add("jourj-frise");
        frise.setMinWidth(0);
        frise.setMaxWidth(Double.MAX_VALUE);
        List<Label> libelles = new ArrayList<>();
        for (int i = 0; i < pas.size(); i++) {
            PresentationTableauDeBordJourJ.Pas p = pas.get(i);
            Label rond = new Label(String.valueOf(p.numero()));
            rond.getStyleClass().add("jourj-frise-rond");
            rond.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            Region traitAvant = trait(i > 0, p.etat() != PresentationTableauDeBordJourJ.EtatPas.A_VENIR);
            Region traitApres = trait(i < pas.size() - 1,
                    p.etat() == PresentationTableauDeBordJourJ.EtatPas.PASSE);
            HBox haut = new HBox(0, traitAvant, rond, traitApres);
            haut.setAlignment(Pos.CENTER);
            // Même hauteur pour toutes les étapes : les traits restent alignés malgré le rond courant plus grand.
            haut.setMinHeight(HAUTEUR_FRISE);
            haut.setPrefHeight(HAUTEUR_FRISE);
            Label libelle = Ui.label(p.libelle(), "jourj-frise-libelle");
            libelle.setMinWidth(0);
            libelle.setAlignment(Pos.CENTER);
            libelle.setMaxWidth(Double.MAX_VALUE);
            libelles.add(libelle);
            VBox etape = new VBox(4, haut, libelle);
            etape.setAlignment(Pos.TOP_CENTER);
            etape.setMinWidth(0);
            etape.setPrefWidth(0);
            etape.getStyleClass().addAll("jourj-frise-pas", p.classe());
            HBox.setHgrow(etape, Priority.ALWAYS);
            etape.setMaxWidth(Double.MAX_VALUE);
            Tooltip.install(etape, new Tooltip(p.numero() + ". " + p.libelle()));
            frise.getChildren().add(etape);
        }
        Runnable adapter = () -> {
            PresentationTableauDeBordJourJ.ModeFrise mode = PresentationTableauDeBordJourJ.modeFrise(frise.getWidth());
            for (int i = 0; i < pas.size(); i++) {
                String texte = pas.get(i).texte(mode);
                Label l = libelles.get(i);
                l.setText(texte);
                l.setVisible(!texte.isEmpty());
                l.setManaged(!texte.isEmpty());
            }
        };
        frise.widthProperty().addListener((o, a, b) -> adapter.run());
        adapter.run();
        return frise;
    }

    /** Trait de liaison entre deux ronds (invisible avant le premier et après le dernier). */
    private static Region trait(boolean present, boolean parcouru) {
        Region r = new Region();
        r.getStyleClass().add("jourj-frise-trait");
        if (parcouru) {
            r.getStyleClass().add("jourj-frise-trait-parcouru");
        }
        r.setVisible(present);
        r.setMinWidth(4);
        r.setPrefHeight(2);
        r.setMaxHeight(2);
        HBox.setHgrow(r, Priority.ALWAYS);
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }

    /**
     * Ligne « à gauche / à droite » qui passe sur deux rangées quand la place
     * manque (en dessous de {@code seuil} pixels) : la partie de droite descend
     * sous celle de gauche au lieu de la recouvrir.
     */
    static VBox ligneAdaptative(Region gauche, Region droite, double seuil) {
        VBox conteneur = new VBox(8);
        conteneur.setMinWidth(0);
        conteneur.setMaxWidth(Double.MAX_VALUE);
        gauche.setMinWidth(0);
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox.setHgrow(gauche, Priority.SOMETIMES);
        boolean[] etroit = {false};
        Runnable composer = () -> {
            HBox rangee = new HBox(12);
            rangee.setAlignment(Pos.CENTER_LEFT);
            if (etroit[0]) {
                conteneur.getChildren().setAll(gauche, droite);
            } else {
                rangee.getChildren().setAll(gauche, espace, droite);
                conteneur.getChildren().setAll(rangee);
            }
        };
        conteneur.widthProperty().addListener((o, avant, apres) -> {
            boolean e = apres.doubleValue() > 0 && apres.doubleValue() < seuil;
            if (e != etroit[0]) {
                etroit[0] = e;
                composer.run();
            }
        });
        composer.run();
        return conteneur;
    }

    static boolean dansUnBouton(Object cible, Node limite) {
        Node n = cible instanceof Node noeud ? noeud : null;
        while (n != null && n != limite) {
            if (n instanceof ButtonBase) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }
}
