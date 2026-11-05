package com.pingmanager.ui.ecran;

import com.pingmanager.core.Horloge;
import com.pingmanager.core.ResultatPropositionSalle;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.core.TableauDeBordSalle.CarteTable;
import com.pingmanager.core.TableauDeBordSalle.MatchBloque;
import com.pingmanager.core.TableauDeBordSalle.MatchSalle;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vue « Salle » du jour J : une carte par table (libre ou occupée, match en
 * cours, durée écoulée) et, à droite, les matchs prêts de tous les tableaux
 * triés par priorité, puis les matchs bloqués avec leur motif.
 *
 * <p>Gestes du juge-arbitre, en peu de clics :</p>
 * <ul>
 *   <li>« Proposer » (Alt+P) remplit les tables libres selon l'ordonnanceur ;</li>
 *   <li>glisser un match prêt sur une table libre, ou « Envoyer à… » (bouton ou
 *       clic droit) puis la table ; « Envoyer le prochain » sur une table libre ;</li>
 *   <li>« Terminer » sur une table ouvre la saisie du score (composants des
 *       poules et du tableau final) ; à l'enregistrement la table se libère et
 *       tout se met à jour ;</li>
 *   <li>« Annuler l'affectation » corrige une erreur d'envoi.</li>
 * </ul>
 *
 * <p>Toute la logique est dans {@link ServiceSalle} (cœur) et
 * {@link PresentationSalle} ; chaque action réussie enregistre le tournoi
 * aussitôt. Les durées se rafraîchissent toutes les 20 secondes.</p>
 */
final class EcranSalle {

    private static final Duration FREQUENCE_CHRONO = Duration.seconds(20);

    private final Tournoi tournoi;
    private final Runnable enregistrer;
    private final Runnable onRetour;
    private final ServiceSalle salle;
    private final SaisieResultatMatch saisiePoule;
    private final ServicePhaseFinale servicePhaseFinale = new ServicePhaseFinale();

    private final BorderPane racine = new BorderPane();
    private final ScrollPane defilement;
    private final Map<Label, LocalDateTime> chronos = new IdentityHashMap<>();
    private Timeline minuterie;

    private TableauDeBordSalle bord;
    private Match enSaisie;
    private String messageSucces;
    private String messageErreur;

    EcranSalle(Tournoi tournoi, Runnable enregistrer, Runnable onRetour, Horloge horloge) {
        this.tournoi = tournoi;
        this.enregistrer = enregistrer;
        this.onRetour = onRetour;
        this.salle = new ServiceSalle(horloge);
        this.saisiePoule = new SaisieResultatMatch(horloge);
        this.defilement = Ui.defilant(new VBox());
        racine.setCenter(defilement);
        racine.getStyleClass().add("salle");
    }

    Node build() {
        afficher();
        demarrerMinuterie();
        return racine;
    }

    private boolean ferme() {
        return tournoi.getStatut() == StatutTournoi.ANNULE;
    }

    // ==================================================================
    // Page
    // ==================================================================

    private void afficher() {
        double position = defilement.getVvalue();
        chronos.clear();
        bord = salle.tableauDeBord(tournoi);

        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> {
            arreterMinuterie();
            onRetour.run();
        });
        Label titre = Ui.titre("Salle et tables");
        Label nom = Ui.discret(tournoi.getNom() == null ? "" : tournoi.getNom());
        VBox contenu = Ui.page(1200, new VBox(6, retour, titre, nom), tuiles(), barreActions());

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
        if (ferme()) {
            contenu.getChildren().add(Ui.encartInfo("Ce tournoi est annulé : la salle est fermée."));
        }
        if (!bord.getAnomalies().isEmpty()) {
            contenu.getChildren().add(carteAnomalies());
        }
        contenu.getChildren().add(grilleTables());
        defilement.setContent(contenu);
        javafx.application.Platform.runLater(() -> defilement.setVvalue(position));
        racine.setRight(enSaisie == null ? colonneMatchs() : panneauSaisie(enSaisie));
    }

    private Node tuiles() {
        return new FlowPane(12, 12,
                Ui.tuile(String.valueOf(bord.nombreTablesLibres()),
                        bord.nombreTablesLibres() > 1 ? "tables libres" : "table libre"),
                Ui.tuile(String.valueOf(bord.nombreTablesOccupees()),
                        bord.nombreTablesOccupees() > 1 ? "matchs en cours" : "match en cours"),
                Ui.tuile(String.valueOf(bord.getPrets().size()),
                        bord.getPrets().size() > 1 ? "matchs prêts" : "match prêt"),
                Ui.tuile(String.valueOf(bord.getBloques().size()),
                        bord.getBloques().size() > 1 ? "matchs bloqués" : "match bloqué"));
    }

    private Node barreActions() {
        Button proposer = Ui.boutonPrimaire("_Proposer");
        proposer.setMnemonicParsing(true);
        proposer.setDisable(ferme() || bord.getNombreDeTables() == 0);
        proposer.setTooltip(new Tooltip("Alt+P"));
        proposer.setOnAction(e -> proposer());
        Label aide = Ui.aide("« Proposer » remplit les tables libres avec les matchs prêts, du plus prioritaire au "
                + "moins prioritaire. Un joueur n'est jamais envoyé sur deux tables. Pour choisir vous-même, glissez "
                + "un match prêt sur une table libre, ou utilisez « Envoyer à… ».");
        aide.setMaxWidth(720);
        HBox barre = Ui.ligne(16, proposer, aide);
        return barre;
    }

    // ==================================================================
    // Grille des tables
    // ==================================================================

    private Node grilleTables() {
        if (bord.getNombreDeTables() == 0) {
            return Ui.carte("Tables", Ui.texte("Le nombre de tables du tournoi n'est pas renseigné. Indiquez-le dans "
                    + "« Informations sportives et techniques » pour afficher la salle."));
        }
        FlowPane grille = new FlowPane(12, 12);
        grille.getStyleClass().add("salle-grille");
        for (CarteTable c : bord.getTables()) {
            grille.getChildren().add(carteTable(c));
        }
        return grille;
    }

    private Node carteTable(CarteTable c) {
        Label numero = new Label(String.valueOf(c.numero()));
        numero.getStyleClass().add("salle-numero");
        Label etat = Ui.pastille(PresentationSalle.etat(c), c.estLibre() ? Ui.ETAT_OK : Ui.ETAT_RECOMMANDE);
        VBox carte = new VBox(8, Ui.ligne(10, numero, Ui.espace(), etat));
        carte.getStyleClass().addAll("salle-table", c.estLibre() ? "salle-table-libre" : "salle-table-occupee");
        carte.setPrefWidth(236);
        carte.setMinWidth(236);
        carte.setMaxWidth(236);

        if (c.estLibre()) {
            carte.getChildren().add(Ui.aide("Glissez un match prêt ici."));
            Button prochain = Ui.boutonSecondaire("Envoyer le prochain");
            if (bord.getPrets().isEmpty() || ferme()) {
                prochain.setDisable(true);
            } else {
                MatchSalle premier = bord.getPrets().get(0);
                prochain.setTooltip(new Tooltip(PresentationSalle.joueurs(premier.match()) + "\n"
                        + PresentationSalle.contexte(premier)));
                prochain.setOnAction(e -> affecter(premier.match(), c.numero()));
            }
            carte.getChildren().add(prochain);
            accepterDepot(carte, c.numero());
        } else {
            MatchSalle m = c.match();
            Label contexte = Ui.discret(PresentationSalle.contexte(m));
            Label a = joueur(PresentationSalle.nom(m.match().getJoueurA()));
            Label b = joueur(PresentationSalle.nom(m.match().getJoueurB()));
            Label duree = new Label();
            duree.getStyleClass().add("salle-duree");
            LocalDateTime debut = m.match().getInstantLancement();
            if (debut != null) {
                chronos.put(duree, debut);
                majChrono(duree, debut);
            }
            Button terminer = Ui.boutonPrimaire("Terminer");
            terminer.setDisable(ferme());
            terminer.setTooltip(new Tooltip("Saisir le score : la table se libère à l'enregistrement."));
            terminer.setOnAction(e -> {
                enSaisie = m.match();
                afficher();
            });
            Button annuler = Ui.boutonLien("Annuler l'affectation");
            annuler.setTooltip(new Tooltip("Erreur d'envoi : la table redevient libre et le match revient dans "
                    + "les matchs prêts."));
            annuler.setOnAction(e -> annulerAffectation(m.match()));
            carte.getChildren().addAll(contexte, a, b, duree, Ui.ligne(8, terminer, annuler));
        }
        return carte;
    }

    private static Label joueur(String nom) {
        Label l = new Label(nom);
        l.getStyleClass().add("salle-joueur");
        l.setWrapText(true);
        return l;
    }

    private void majChrono(Label l, LocalDateTime debut) {
        java.time.Duration ecoule = java.time.Duration.between(debut, salle.maintenant());
        l.setText("depuis " + PresentationSalle.duree(ecoule));
        l.getStyleClass().remove("salle-duree-longue");
        if (PresentationSalle.estLongue(ecoule)) {
            l.getStyleClass().add("salle-duree-longue");
        }
    }

    /** Une table libre accepte un match prêt glissé depuis la colonne de droite. */
    private void accepterDepot(VBox carte, int table) {
        carte.setOnDragOver(e -> {
            Dragboard d = e.getDragboard();
            if (d.hasString() && PresentationSalle.lireCleGlisser(d.getString()) >= 0) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        carte.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) {
                carte.getStyleClass().add("salle-table-survol");
            }
        });
        carte.setOnDragExited(e -> carte.getStyleClass().remove("salle-table-survol"));
        carte.setOnDragDropped(e -> {
            int rang = PresentationSalle.lireCleGlisser(e.getDragboard().getString());
            boolean ok = rang >= 0 && rang < bord.getPrets().size();
            e.setDropCompleted(ok);
            e.consume();
            if (ok) {
                Match m = bord.getPrets().get(rang).match();
                javafx.application.Platform.runLater(() -> affecter(m, table));
            }
        });
    }

    private Node carteAnomalies() {
        VBox lignes = new VBox(0);
        for (MatchSalle m : bord.getAnomalies()) {
            Button regulariser = Ui.boutonSecondaire("Remettre à jouer");
            regulariser.setOnAction(e -> annulerAffectation(m.match()));
            Integer table = m.match().getNumeroTable();
            VBox infos = new VBox(2, Ui.gras(PresentationSalle.joueurs(m.match())),
                    Ui.discret(PresentationSalle.contexte(m) + (table == null ? " · sans table" : " · table " + table)));
            lignes.getChildren().add(Ui.ligneListe(infos, Ui.espace(), regulariser));
        }
        return Ui.carte(Ui.titreAvecCompteur("Matchs à régulariser", bord.getAnomalies().size()),
                Ui.aide("Ces matchs sont notés « en cours » mais sans table valable (table inexistante, déjà prise, "
                        + "ou joueur sur une autre table). Remettez-les à jouer, puis renvoyez-les sur une table."),
                lignes);
    }

    // ==================================================================
    // Colonne des matchs prêts et bloqués
    // ==================================================================

    private Node colonneMatchs() {
        VBox prets = new VBox(0);
        List<MatchSalle> liste = bord.getPrets();
        for (int i = 0; i < liste.size(); i++) {
            prets.getChildren().add(lignePret(liste.get(i), i));
        }
        if (liste.isEmpty()) {
            prets.getChildren().add(Ui.discret("Aucun match prêt. Les matchs des poules lancées et du tableau final "
                    + "apparaissent ici dès que leurs deux joueurs sont libres."));
        }
        VBox colonne = new VBox(16, Ui.carte(Ui.titreAvecCompteur("Matchs prêts", liste.size()),
                Ui.aide("Du plus prioritaire au moins prioritaire. Glissez un match sur une table libre."), prets));

        if (!bord.getBloques().isEmpty()) {
            VBox bloques = new VBox(0);
            for (MatchBloque b : bord.getBloques()) {
                bloques.getChildren().add(ligneBloque(b));
            }
            colonne.getChildren().add(Ui.carte(Ui.titreAvecCompteur("Bloqués", bord.getBloques().size()),
                    Ui.aide("Un de leurs joueurs est déjà sur une table."), bloques));
        }
        if (bord.getEnAttenteDesJoueurs() > 0) {
            int n = bord.getEnAttenteDesJoueurs();
            colonne.getChildren().add(Ui.aide(n + (n > 1 ? " matchs du tableau final attendent" : " match du tableau "
                    + "final attend") + " le résultat du tour précédent."));
        }
        colonne.getStyleClass().add("salle-colonne");
        colonne.setPrefWidth(380);
        colonne.setMaxWidth(380);
        ScrollPane s = Ui.defilant(colonne);
        s.getStyleClass().add("salle-cote");
        BorderPane.setMargin(s, new Insets(24, 24, 24, 0));
        return s;
    }

    private Node lignePret(MatchSalle m, int rang) {
        Label joueurs = Ui.gras(PresentationSalle.joueurs(m.match()));
        joueurs.setWrapText(true);
        VBox infos = new VBox(2, Ui.discret(PresentationSalle.contexte(m)), joueurs);
        if (!m.explication().isEmpty()) {
            infos.getChildren().add(Ui.aide("Priorité : " + m.explication()));
        }
        MenuButton envoyer = new MenuButton("Envoyer à…");
        envoyer.getStyleClass().add("salle-envoyer");
        envoyer.getItems().addAll(itemsEnvoi(m.match()));
        envoyer.setDisable(ferme() || bord.tablesLibres().isEmpty());
        VBox ligne = new VBox(6, infos, envoyer);
        ligne.getStyleClass().add("salle-pret");
        ligne.setCursor(Cursor.OPEN_HAND);

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(itemsEnvoi(m.match()));
        ligne.setOnContextMenuRequested(e -> {
            if (!menu.getItems().isEmpty()) {
                menu.show(ligne, e.getScreenX(), e.getScreenY());
            }
            e.consume();
        });
        ligne.setOnDragDetected(e -> {
            if (ferme()) {
                return;
            }
            Dragboard d = ligne.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent contenu = new ClipboardContent();
            contenu.putString(PresentationSalle.cleGlisser(rang));
            d.setContent(contenu);
            e.consume();
        });
        return ligne;
    }

    /** « Envoyer à la table N » pour chaque table libre. */
    private List<MenuItem> itemsEnvoi(Match m) {
        List<MenuItem> items = new ArrayList<>();
        if (ferme()) {
            return items;
        }
        for (Integer table : bord.tablesLibres()) {
            MenuItem item = new MenuItem("Envoyer à la table " + table);
            item.setOnAction(e -> affecter(m, table));
            items.add(item);
        }
        return items;
    }

    private Node ligneBloque(MatchBloque b) {
        Label joueurs = Ui.texte(PresentationSalle.joueurs(b.match().match()));
        VBox ligne = new VBox(4, Ui.discret(PresentationSalle.contexte(b.match())), joueurs,
                Ui.pastille(PresentationSalle.motifBlocage(b), Ui.ETAT_OPTIONNEL));
        ligne.getStyleClass().addAll("salle-pret", "salle-bloque");
        return ligne;
    }

    // ==================================================================
    // Terminer : saisie du score avec les composants existants
    // ==================================================================

    private Node panneauSaisie(Match m) {
        Integer table = m.getNumeroTable();
        Label titre = Ui.titreCarte(table == null ? "Résultat" : "Table " + table + " · résultat");
        Runnable fermer = () -> {
            enSaisie = null;
            afficher();
        };
        Node editeur;
        Tableau tableau = tableauDePoule(m);
        if (tableau != null) {
            VBox carte = Ui.carte(titre, Ui.gras(PresentationSalle.joueurs(m)),
                    EditeurScorePoule.creer(tableau, m, saisiePoule, fermer, this::apresResultat));
            carte.setPrefWidth(380);
            carte.setMaxWidth(380);
            editeur = carte;
        } else {
            tableau = tableauFinal(m);
            if (tableau == null) {
                enSaisie = null;
                messageErreur = "Ce match n'appartient plus à aucun tableau.";
                return colonneMatchs();
            }
            editeur = new VBox(12, titre,
                    EditeurScorePhaseFinale.creer(tableau, m, servicePhaseFinale, fermer, this::apresResultat));
        }
        ScrollPane s = Ui.defilant(editeur);
        s.getStyleClass().add("salle-cote");
        BorderPane.setMargin(s, new Insets(24, 24, 24, 0));
        return s;
    }

    private void apresResultat(Match m) {
        ResultatAffectationTable r = salle.libererApresResultat(tournoi, m);
        enSaisie = null;
        apresAction(r.estReussi(), PresentationSalle.message(r, bord.getNombreDeTables()));
    }

    private Tableau tableauDePoule(Match m) {
        for (Tableau t : tournoi.getTableaux()) {
            for (Poule p : t.getPoules()) {
                if (p.getMatchs().contains(m)) {
                    return t;
                }
            }
        }
        return null;
    }

    private Tableau tableauFinal(Match m) {
        for (Tableau t : tournoi.getTableaux()) {
            if (t.aPhaseFinale()) {
                for (Tour tour : t.getPhaseFinale().getTours()) {
                    if (tour.getMatchs().contains(m)) {
                        return t;
                    }
                }
            }
        }
        return null;
    }

    // ==================================================================
    // Actions
    // ==================================================================

    private void proposer() {
        ResultatPropositionSalle r = salle.proposer(tournoi);
        apresAction(r.aLance(), PresentationSalle.messageProposition(r));
    }

    private void affecter(Match m, int table) {
        ResultatAffectationTable r = salle.affecter(tournoi, m, table);
        apresAction(r.estReussi(), PresentationSalle.message(r, bord.getNombreDeTables()));
    }

    private void annulerAffectation(Match m) {
        ResultatAffectationTable r = salle.annulerAffectation(tournoi, m);
        apresAction(r.estReussi(), PresentationSalle.message(r, bord.getNombreDeTables()));
    }

    /** Enregistre aussitôt une action réussie, puis redessine toute la salle. */
    private void apresAction(boolean reussi, String texte) {
        if (reussi) {
            try {
                enregistrer.run();
                messageSucces = texte;
            } catch (RuntimeException ex) {
                messageErreur = texte + " Attention : l'enregistrement du tournoi a échoué (" + ex.getMessage()
                        + ").";
            }
        } else {
            messageErreur = texte;
        }
        afficher();
    }

    // ==================================================================
    // Rafraîchissement des durées
    // ==================================================================

    private void demarrerMinuterie() {
        minuterie = new Timeline(new KeyFrame(FREQUENCE_CHRONO,
                e -> chronos.forEach(this::majChrono)));
        minuterie.setCycleCount(Timeline.INDEFINITE);
        minuterie.play();
        // La minuterie s'arrête quand la vue quitte l'écran.
        racine.sceneProperty().addListener((o, avant, apres) -> {
            if (apres == null) {
                arreterMinuterie();
            }
        });
    }

    private void arreterMinuterie() {
        if (minuterie != null) {
            minuterie.stop();
        }
    }
}
