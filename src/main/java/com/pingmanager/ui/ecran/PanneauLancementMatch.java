package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeLancements;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Tournoi;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface « Lancer un match » du jour J (B057), ouverte par la carte du même
 * nom à la place de l'écran : le juge-arbitre envoie un match sur une table en
 * quelques secondes. De haut en bas :
 *
 * <ol>
 *   <li>en-tête : « ← Retour jour J » à gauche, le titre au centre ;</li>
 *   <li>bandeau de proposition : « Proposer un match », la zone du match
 *       proposé (joueurs, tableau et échelon, table libre modifiable), puis
 *       « Lancer » et « Autre » (l'un sous l'autre ; sous la zone sur petite
 *       largeur) ;</li>
 *   <li>la liste des matchs à lancer (N°, joueurs, table, tableau avec
 *       « Voir plus », « Lancer »), du plus urgent au moins urgent.</li>
 * </ol>
 *
 * <p>Pendant la phase de poules, la proposition et les lignes peuvent porter une
 * <b>poule entière</b> : ses joueurs dans l'ordre de classement (numéro, nom,
 * points, sur deux lignes au plus), le nombre de matchs et la durée prévue, la
 * table ; « Lancer la poule » l'envoie en bloc. « Voir plus » montre la poule
 * (joueurs, ordre des matchs I.301, classement).</p>
 *
 * <p>L'ordre, la proposition suivante, les tables libres et les refus viennent
 * du cœur ({@link com.pingmanager.core.ServiceSalle#listeLancements}) ; le
 * lancement lui-même passe par l'écran de la salle (annonce, SMS, feuille,
 * enregistrement), qui repasse par le cœur et ses vérifications. Les choix
 * (proposition, tables) sont gardés d'un affichage à l'autre. Logique
 * d'affichage : {@link PresentationLancementMatch} ; textes :
 * {@link TextesLancementMatch}.</p>
 */
final class PanneauLancementMatch {

    /** Largeur en dessous de laquelle « Lancer » et « Autre » passent sous la zone de proposition. */
    static final double LARGEUR_BANDEAU_ETROIT = 820;
    /** Largeur en dessous de laquelle la liste passe d'un tableau à une carte par match. */
    static final double LARGEUR_LISTE_ETROITE = 700;

    /** Gestes délégués à l'écran de la salle. */
    interface Actions {
        /** Envoie le match sur la table : annonce, SMS, feuille et enregistrement ; le cœur vérifie de nouveau. */
        void lancer(Match match, int table);

        /**
         * Envoie la poule entière sur la table (phase de poules) : annonce de tous ses
         * joueurs, SMS, feuille de poule et enregistrement ; le cœur vérifie de nouveau.
         */
        void lancerPoule(com.pingmanager.model.Poule poule, int table);

        /** Option « feuille de match à chaque lancement » (B016), mémorisée pour le tournoi. */
        void feuilleALEnvoi(boolean imprimer);

        /** Revient à l'écran du jour J. */
        void fermer();

        /** Redessine l'écran (après un choix qui change l'interface). */
        void rafraichir();
    }

    private final Tournoi tournoi;
    private final Actions actions;

    /** « Proposer un match » a été appuyé et la proposition n'a pas encore été lancée. */
    private boolean demande;
    /** Match proposé dans le bandeau, ou null. */
    private Match propose;
    /** Table choisie dans le bandeau, ou null (table pré-choisie par le cœur). */
    private Integer tableBandeau;
    /** Table choisie sur une ligne de la liste, à la place de la table pré-choisie. */
    private final Map<Match, Integer> choixTables = new IdentityHashMap<>();
    /** Match dont la poule ou le tour est ouvert (« Voir plus »), ou null. */
    private Match voirPlus;
    /** Situation de la salle au dernier affichage, pour se redessiner quand elle change ailleurs. */
    private String empreinte = "";

    PanneauLancementMatch(Tournoi tournoi, Actions actions) {
        this.tournoi = tournoi;
        this.actions = actions;
    }

    /** Remet l'interface dans son état d'ouverture : zone vide, tables pré-choisies, liste visible. */
    void reinitialiser() {
        demande = false;
        propose = null;
        tableBandeau = null;
        choixTables.clear();
        voirPlus = null;
    }

    /** Après un lancement : la zone se vide si c'était le match proposé ; les choix obsolètes s'effacent. */
    void apresLancement(Match match, boolean reussi) {
        if (reussi && match == propose) {
            demande = false;
            propose = null;
            tableBandeau = null;
        }
        if (reussi) {
            choixTables.remove(match);
        }
    }

    /**
     * La salle a-t-elle changé depuis le dernier affichage (match terminé
     * ailleurs, table libérée, joueur de nouveau libre) ? Jamais pendant la
     * consultation d'une poule ou d'un arbre (« Voir plus ») : rien n'y est perdu.
     */
    boolean aChange(ListeLancements liste) {
        return voirPlus == null && !empreinte.equals(PresentationLancementMatch.empreinte(liste));
    }

    // ==================================================================
    // En-tête
    // ==================================================================

    /** « ← Retour jour J » à gauche, « Lancer un match » au centre. */
    Node entete() {
        Button retour = Ui.boutonLien(TextesLancementMatch.RETOUR);
        retour.getStyleClass().add("lancement-retour");
        retour.setCancelButton(true);
        retour.setOnAction(e -> actions.fermer());
        Label titre = Ui.titre(TextesLancementMatch.TITRE);
        titre.getStyleClass().add("lancement-titre");
        GridPane g = new GridPane();
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(i == 1 ? 50 : 25);
            g.getColumnConstraints().add(c);
        }
        g.add(retour, 0, 0);
        g.add(titre, 1, 0);
        GridPane.setHalignment(titre, HPos.CENTER);
        GridPane.setHalignment(retour, HPos.LEFT);
        g.getStyleClass().add("lancement-entete");
        return g;
    }

    // ==================================================================
    // Corps : bandeau et liste, ou la vue « Voir plus »
    // ==================================================================

    Node corps(ListeLancements liste, int nombreDeTables, boolean ferme, boolean feuilleALEnvoi) {
        empreinte = PresentationLancementMatch.empreinte(liste);
        // Choix de tables qui ne servent plus (match lancé ailleurs) : oubliés.
        choixTables.keySet().removeIf(m -> liste.jouable(m).isEmpty());
        if (demande && propose == null) {
            // Rien n'était jouable à la demande : dès qu'un match le devient, il s'affiche.
            propose = liste.propositionApres(null).map(l -> l.match().match()).orElse(null);
        }
        if (voirPlus != null) {
            VBox v = new VBox(16, vuePlus(liste));
            v.getStyleClass().add("lancement-match");
            return v;
        }
        CheckBox feuille = new CheckBox(TextesLancementMatch.FEUILLE_A_L_ENVOI);
        feuille.setSelected(feuilleALEnvoi);
        feuille.setDisable(ferme);
        feuille.setOnAction(e -> actions.feuilleALEnvoi(feuille.isSelected()));
        feuille.getStyleClass().add("lancement-feuille");
        VBox v = new VBox(20, bandeau(liste, nombreDeTables, ferme), liste(liste, nombreDeTables, ferme), feuille);
        v.getStyleClass().add("lancement-match");
        return v;
    }

    // ------------------------------------------------------------------
    // Bandeau de proposition
    // ------------------------------------------------------------------

    private Node bandeau(ListeLancements liste, int nombreDeTables, boolean ferme) {
        PresentationLancementMatch.Bandeau b = PresentationLancementMatch.bandeau(liste, demande, propose, ferme,
                nombreDeTables);
        if (demande && propose != null && b.ligne() == null) {
            // La proposition n'est plus jouable (lancée depuis la liste, joueur parti ailleurs) : zone vide.
            demande = false;
            propose = null;
            tableBandeau = null;
        }

        Button proposer = Ui.boutonSecondaire(TextesLancementMatch.PROPOSER);
        proposer.getStyleClass().addAll("lancement-proposer", "lancement-tactile");
        proposer.setMaxHeight(Double.MAX_VALUE);
        proposer.setOnAction(e -> {
            demande = true;
            tableBandeau = null;
            propose = liste.propositionApres(null).map(l -> l.match().match()).orElse(null);
            actions.rafraichir();
        });

        Button lancer = Ui.boutonPrimaire(TextesLancementMatch.LANCER);
        lancer.getStyleClass().addAll("lancement-bouton-lancer", "lancement-tactile");
        lancer.setMaxWidth(Double.MAX_VALUE);
        lancer.setDisable(!b.lancerActif());
        Button autre = Ui.boutonSecondaire(TextesLancementMatch.AUTRE);
        autre.getStyleClass().add("lancement-tactile");
        autre.setMaxWidth(Double.MAX_VALUE);
        autre.setDisable(!b.autreActif());
        autre.setTooltip(new Tooltip(TextesLancementMatch.AUTRE_AIDE));
        autre.setOnAction(e -> {
            tableBandeau = null;
            propose = liste.propositionApres(propose).map(l -> l.match().match()).orElse(null);
            actions.rafraichir();
        });

        Node zone;
        if (b.ligne() == null) {
            Label texte = Ui.label(b.texte(), "lancement-zone-texte");
            texte.setWrapText(true);
            texte.setMinHeight(Region.USE_PREF_SIZE);
            zone = texte;
        } else {
            ListeLancements.Ligne l = b.ligne();
            Match m = l.match().match();
            Integer table = PresentationLancementMatch.tableRetenue(l, tableBandeau);
            Node choixTable;
            if (l.estPoule()) {
                lancer.setText(TextesLancementMatch.LANCER_POULE);
            }
            if (b.lancerActif()) {
                ComboBox<Integer> tables = comboTables(l, table, t -> tableBandeau = t);
                choixTable = tables;
                lancer.setOnAction(e -> {
                    Integer t = tables.getValue();
                    if (t != null) {
                        lancerLigne(l, t);
                    }
                });
            } else {
                choixTable = Ui.pastille(b.motifLancer(), StatutPastille.ATTENTION);
                lancer.setTooltip(new Tooltip(b.motifLancer()));
            }
            FlowPane champs;
            if (l.estPoule()) {
                VBox joueurs = new VBox(2);
                PresentationLancementMatch.joueursPoule(l.poule()).forEach(s -> joueurs.getChildren()
                        .add(valeur(s, true)));
                champs = new FlowPane(24, 12,
                        champ(TextesLancementMatch.TABLEAU, valeur(PresentationLancementMatch.tableauEtEchelon(l),
                                false)),
                        champ(TextesLancementMatch.JOUEURS, joueurs),
                        champ(TextesLancementMatch.MATCHS, valeur(PresentationLancementMatch.matchsEtDuree(
                                l.poule()), false)),
                        champ(TextesLancementMatch.TABLE, choixTable));
            } else {
                champs = new FlowPane(24, 12,
                        champ(TextesLancementMatch.JOUEUR_1, valeur(PresentationSalle.nom(m.getJoueurA()), true)),
                        champ(TextesLancementMatch.JOUEUR_2, valeur(PresentationSalle.nom(m.getJoueurB()), true)),
                        champ(TextesLancementMatch.TABLEAU, valeur(PresentationLancementMatch.tableauEtEchelon(l),
                                false)),
                        champ(TextesLancementMatch.TABLE, choixTable));
            }
            champs.setAlignment(Pos.CENTER_LEFT);
            champs.setPrefWrapLength(560);
            Label rang = Ui.label(PresentationLancementMatch.rang(liste, m), "lancement-rang");
            zone = new VBox(8, champs, rang);
        }
        VBox cadreZone = new VBox(zone);
        cadreZone.getStyleClass().add("lancement-zone");
        if (b.ligne() != null) {
            cadreZone.getStyleClass().add("lancement-zone-pleine");
        }
        cadreZone.setAlignment(Pos.CENTER_LEFT);
        cadreZone.setMinWidth(0);

        VBox colonneBoutons = new VBox(8, lancer, autre);
        colonneBoutons.getStyleClass().add("lancement-boutons");
        HBox ligneBoutons = new HBox(8);
        ligneBoutons.getStyleClass().add("lancement-boutons");

        VBox carte = new VBox();
        carte.getStyleClass().addAll("carte", "lancement-bandeau");
        boolean[] etroit = {false};
        Runnable composer = () -> {
            boolean e = carte.getWidth() > 0 && carte.getWidth() < LARGEUR_BANDEAU_ETROIT;
            if (e == etroit[0] && !carte.getChildren().isEmpty()) {
                return;
            }
            etroit[0] = e;
            colonneBoutons.getChildren().clear();
            ligneBoutons.getChildren().clear();
            carte.getChildren().clear();
            if (e) {
                HBox.setHgrow(lancer, Priority.ALWAYS);
                HBox.setHgrow(autre, Priority.ALWAYS);
                ligneBoutons.getChildren().addAll(lancer, autre);
                proposer.setMaxWidth(Double.MAX_VALUE);
                carte.getChildren().add(new VBox(12, proposer, cadreZone, ligneBoutons));
            } else {
                colonneBoutons.getChildren().addAll(lancer, autre);
                proposer.setMaxWidth(Region.USE_PREF_SIZE);
                HBox.setHgrow(cadreZone, Priority.ALWAYS);
                HBox ligne = new HBox(16, proposer, cadreZone, colonneBoutons);
                ligne.setAlignment(Pos.CENTER_LEFT);
                carte.getChildren().add(ligne);
            }
        };
        carte.widthProperty().addListener((o, a, n) -> composer.run());
        composer.run();
        // Le geste suivant a le focus : « Lancer » quand un match est proposé, sinon « Proposer un match ».
        Platform.runLater(b.lancerActif() ? lancer::requestFocus : proposer::requestFocus);
        return carte;
    }

    /** Lance la ligne : la poule entière pour une ligne de poule, sinon le match. */
    private void lancerLigne(ListeLancements.Ligne l, int table) {
        if (l.estPoule()) {
            actions.lancerPoule(l.poule().poule(), table);
        } else {
            actions.lancer(l.match().match(), table);
        }
    }

    private static VBox champ(String libelle, Node valeur) {
        Label l = Ui.label(libelle, "lancement-champ-libelle");
        VBox v = new VBox(4, l, valeur);
        v.getStyleClass().add("lancement-champ");
        return v;
    }

    private static Label valeur(String texte, boolean joueur) {
        Label l = Ui.label(texte, joueur ? "lancement-joueur" : "lancement-valeur");
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    /** Liste des tables libres qui peuvent recevoir ce match, la table retenue choisie. */
    private static ComboBox<Integer> comboTables(ListeLancements.Ligne l, Integer table, Consumer<Integer> choix) {
        ComboBox<Integer> tables = new ComboBox<>();
        tables.getItems().setAll(l.tablesPossibles());
        tables.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer t) {
                return t == null ? "" : PresentationLancementMatch.libelleTable(t);
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        });
        tables.setValue(table);
        tables.getStyleClass().addAll("lancement-table", "lancement-tactile");
        tables.valueProperty().addListener((o, avant, apres) -> {
            if (apres != null) {
                choix.accept(apres);
            }
        });
        return tables;
    }

    // ------------------------------------------------------------------
    // Liste des matchs à lancer
    // ------------------------------------------------------------------

    private Node liste(ListeLancements liste, int nombreDeTables, boolean ferme) {
        List<ListeLancements.Ligne> jouables = liste.getJouables();
        VBox contenu = new VBox(0);
        VBox carte = Ui.carte(Ui.titreAvecCompteur(TextesLancementMatch.LISTE_TITRE, jouables.size()),
                Ui.aide(TextesLancementMatch.LISTE_AIDE), contenu);
        carte.getStyleClass().add("lancement-liste");
        if (jouables.isEmpty()) {
            contenu.getChildren().add(Ui.etatVideCompact(TextesLancementMatch.LISTE_VIDE));
            return carte;
        }
        boolean[] etroit = {false};
        boolean[] compose = {false};
        Runnable composer = () -> {
            boolean e = carte.getWidth() > 0 && carte.getWidth() < LARGEUR_LISTE_ETROITE;
            if (compose[0] && e == etroit[0]) {
                return;
            }
            compose[0] = true;
            etroit[0] = e;
            contenu.getChildren().setAll(e ? cartesMatchs(jouables, nombreDeTables, ferme)
                    : List.of(grilleMatchs(jouables, nombreDeTables, ferme)));
        };
        carte.widthProperty().addListener((o, a, n) -> composer.run());
        composer.run();
        return carte;
    }

    /** Grande largeur : une ligne par match, six colonnes. */
    private Node grilleMatchs(List<ListeLancements.Ligne> jouables, int nombreDeTables, boolean ferme) {
        GridPane g = new GridPane();
        g.getStyleClass().add("lancement-grille");
        double[] parts = {6, 20, 20, 15, 23, 16};
        for (double p : parts) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(p);
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            g.getColumnConstraints().add(c);
        }
        String[] entetes = {TextesLancementMatch.COLONNE_NUMERO, TextesLancementMatch.JOUEUR_1,
                TextesLancementMatch.JOUEUR_2, TextesLancementMatch.TABLE, TextesLancementMatch.TABLEAU,
                TextesLancementMatch.COLONNE_LANCER};
        for (int c = 0; c < entetes.length; c++) {
            Label l = Ui.enteteColonne(entetes[c]);
            l.getStyleClass().add("lancement-entete-colonne");
            l.setMaxWidth(Double.MAX_VALUE);
            g.add(l, c, 0);
        }
        int rang = 1;
        for (ListeLancements.Ligne l : jouables) {
            Cellules c = cellules(l, nombreDeTables, ferme);
            List<Node> noeuds = l.estPoule()
                    // Poule : joueurs (deux lignes au plus) puis matchs et durée, sur les deux colonnes « Joueur ».
                    ? List.of(c.numero, new VBox(4, c.joueurA, c.joueurB), c.table, c.tableau, c.lancer)
                    : List.of(c.numero, c.joueurA, c.joueurB, c.table, c.tableau, c.lancer);
            for (int k = 0; k < noeuds.size(); k++) {
                int col = l.estPoule() && k >= 2 ? k + 1 : k;
                Node noeud = noeuds.get(k);
                // Chaque cellule dans un cadre : filet au-dessus, contenu aligné à gauche, jamais étiré.
                VBox cellule = new VBox(noeud);
                cellule.setAlignment(Pos.CENTER_LEFT);
                cellule.setFillWidth(false);
                cellule.setMinWidth(0);
                cellule.setMaxWidth(Double.MAX_VALUE);
                cellule.setMaxHeight(Double.MAX_VALUE);
                cellule.getStyleClass().add("lancement-cellule");
                if (rang == 1) {
                    cellule.getStyleClass().add("lancement-cellule-premiere");
                }
                if (noeud instanceof Label texte) {
                    texte.setMaxWidth(Double.MAX_VALUE);
                    cellule.setFillWidth(true);
                }
                if (col == 4 || (l.estPoule() && col == 1)) {
                    cellule.setFillWidth(true);
                }
                if (l.estPoule()) {
                    cellule.getStyleClass().add("lancement-cellule-poule");
                }
                GridPane.setValignment(cellule, VPos.CENTER);
                GridPane.setFillHeight(cellule, true);
                g.add(cellule, col, rang, l.estPoule() && col == 1 ? 2 : 1, 1);
            }
            rang++;
        }
        return g;
    }

    /** Petite largeur : une carte par match, rien de tronqué. */
    private List<Node> cartesMatchs(List<ListeLancements.Ligne> jouables, int nombreDeTables, boolean ferme) {
        List<Node> res = new ArrayList<>();
        boolean premiere = true;
        for (ListeLancements.Ligne l : jouables) {
            Cellules c = cellules(l, nombreDeTables, ferme);
            HBox haut = new HBox(8, c.numero, c.tableau);
            HBox.setHgrow(c.tableau, Priority.ALWAYS);
            haut.setAlignment(Pos.TOP_LEFT);
            VBox joueurs = new VBox(2, c.joueurA, c.joueurB);
            FlowPane bas = new FlowPane(12, 8, c.table, c.lancer);
            bas.setAlignment(Pos.CENTER_LEFT);
            VBox v = new VBox(8, haut, joueurs, bas);
            v.getStyleClass().add("lancement-carte-match");
            if (premiere) {
                v.getStyleClass().add("lancement-carte-match-premiere");
                premiere = false;
            }
            res.add(v);
        }
        return res;
    }

    private record Cellules(Label numero, Label joueurA, Label joueurB, Node table, VBox tableau, VBox lancer) {
    }

    private Cellules cellules(ListeLancements.Ligne l, int nombreDeTables, boolean ferme) {
        Match m = l.match().match();
        Label numero = Ui.label(l.numero() > 0 ? String.valueOf(l.numero()) : "", "lancement-numero");
        Label a;
        Label b;
        if (l.estPoule()) {
            a = valeur(PresentationLancementMatch.joueursPouleCompact(l.poule()), true);
            b = valeur(PresentationLancementMatch.matchsEtDuree(l.poule()), false);
            b.getStyleClass().add("lancement-poule-detail");
        } else {
            a = valeur(PresentationSalle.nom(m.getJoueurA()), true);
            b = valeur(PresentationSalle.nom(m.getJoueurB()), true);
        }

        Label tableau = valeur(PresentationLancementMatch.tableauEtEchelon(l), false);
        Button plus = Ui.boutonLien(TextesLancementMatch.VOIR_PLUS);
        plus.getStyleClass().add("lancement-voir-plus");
        plus.setOnAction(e -> {
            voirPlus = m;
            actions.rafraichir();
        });
        VBox celluleTableau = new VBox(2, tableau, plus);
        celluleTableau.setAlignment(Pos.CENTER_LEFT);

        Button lancer = Ui.boutonPrimaire(l.estPoule() ? TextesLancementMatch.LANCER_POULE : TextesLancementMatch.LANCER);
        lancer.getStyleClass().addAll("lancement-bouton-lancer", "lancement-tactile");
        String motif = PresentationLancementMatch.motifLigne(l, ferme, nombreDeTables);
        VBox celluleLancer = new VBox(4, lancer);
        celluleLancer.setAlignment(Pos.CENTER_LEFT);
        Node table;
        if (motif.isEmpty()) {
            Integer retenue = PresentationLancementMatch.tableRetenue(l, choixTables.get(m));
            ComboBox<Integer> tables = comboTables(l, retenue, t -> choixTables.put(m, t));
            table = tables;
            lancer.setOnAction(e -> {
                Integer t = tables.getValue();
                if (t != null) {
                    lancerLigne(l, t);
                }
            });
        } else {
            table = Ui.label("—", "lancement-sans-table");
            lancer.setDisable(true);
            Label raison = Ui.label(motif, "lancement-motif");
            raison.setWrapText(true);
            raison.setMinHeight(Region.USE_PREF_SIZE);
            celluleLancer.getChildren().add(raison);
        }
        return new Cellules(numero, a, b, table, celluleTableau, celluleLancer);
    }

    // ------------------------------------------------------------------
    // « Voir plus » : poule ou arbre, en lecture seule
    // ------------------------------------------------------------------

    private Node vuePlus(ListeLancements liste) {
        Button fermer = Ui.boutonSecondaire(TextesLancementMatch.FERMER);
        fermer.getStyleClass().add("lancement-tactile");
        fermer.setCancelButton(true);
        fermer.setOnAction(e -> {
            voirPlus = null;
            actions.rafraichir();
        });
        var situation = PresentationLancementMatch.situer(tournoi, voirPlus);
        if (situation.isEmpty()) {
            return Ui.carte(Ui.enteteAvecActions(Ui.titreCarte(TextesLancementMatch.TITRE), fermer),
                    Ui.etatVideCompact(TextesLancementMatch.VUE_INTROUVABLE));
        }
        PresentationLancementMatch.Situation s = situation.get();
        Label titre = Ui.titreCarte(PresentationLancementMatch.titreSituation(s));
        Node entete = Ui.enteteAvecActions(titre, fermer);
        VBox carte;
        if (s.estPoule()) {
            carte = Ui.carte(entete, vuePoule(s));
        } else {
            ScrollPane arbre = VuePhaseFinale.arbreLectureSeule(s.tableau().getPhaseFinale());
            arbre.getStyleClass().add("lancement-arbre");
            arbre.setPrefViewportHeight(520);
            double position = PresentationLancementMatch.positionTour(s);
            Platform.runLater(() -> arbre.setHvalue(position));
            carte = Ui.carte(entete, Ui.aide(TextesLancementMatch.VUE_ARBRE_AIDE), VuePhaseFinale.legendeArbre(),
                    arbre);
        }
        carte.getStyleClass().add("lancement-vue-plus");
        Ui.apparition(carte);
        return carte;
    }

    /** Joueurs, matchs (résultats) et classement de la poule, sans saisie. */
    private static Node vuePoule(PresentationLancementMatch.Situation s) {
        VBox joueurs = new VBox(0);
        boolean premiere = true;
        for (Joueur j : s.poule().getJoueurs()) {
            HBox l = Ui.ligne(12, valeur(PresentationPhaseFinale.nomJoueur(j), true), Ui.espace(),
                    Ui.discret(PresentationPhaseFinale.detailJoueur(j)));
            l.getStyleClass().add(premiere ? "lancement-vue-ligne-premiere" : "lancement-vue-ligne");
            premiere = false;
            joueurs.getChildren().add(l);
        }
        VBox matchs = new VBox(0);
        premiere = true;
        for (PresentationSuiviTournoi.LigneMatch m : PresentationSuiviTournoi.matchsPoule(s.poule())) {
            Label adversaires = valeur(m.joueurA() + "  –  " + m.joueurB(), false);
            HBox.setHgrow(adversaires, Priority.ALWAYS);
            adversaires.setMaxWidth(Double.MAX_VALUE);
            HBox l = Ui.ligne(12, Ui.discret(m.situation()), adversaires,
                    Ui.label(m.table().isEmpty() ? m.resultat() : m.table(), "lancement-vue-resultat"));
            l.getStyleClass().add(premiere ? "lancement-vue-ligne-premiere" : "lancement-vue-ligne");
            premiere = false;
            matchs.getChildren().add(l);
        }
        VBox classement = new VBox(0);
        premiere = true;
        for (PresentationSuiviTournoi.LignePoule c : PresentationSuiviTournoi.classement(s.tableau(), s.poule(),
                null)) {
            Label joueur = valeur(c.joueur(), true);
            HBox.setHgrow(joueur, Priority.ALWAYS);
            joueur.setMaxWidth(Double.MAX_VALUE);
            HBox l = Ui.ligne(12, Ui.label(c.rang(), "lancement-vue-rang"), joueur, Ui.discret(c.bilan()),
                    Ui.discret(c.qualification()));
            l.getStyleClass().add(premiere ? "lancement-vue-ligne-premiere" : "lancement-vue-ligne");
            premiere = false;
            classement.getChildren().add(l);
        }
        List<Node> blocs = List.of(
                new VBox(8, Ui.sousTitre(TextesLancementMatch.VUE_JOUEURS), joueurs),
                new VBox(8, Ui.sousTitre(TextesLancementMatch.VUE_MATCHS), matchs),
                new VBox(8, Ui.sousTitre(TextesLancementMatch.VUE_CLASSEMENT), classement));
        return Ui.grilleAdaptative(3, 300, blocs);
    }
}
