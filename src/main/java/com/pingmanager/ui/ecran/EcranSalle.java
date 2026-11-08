package com.pingmanager.ui.ecran;

import com.pingmanager.core.AnnonceRencontre;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.ResultatPropositionSalle;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.core.TableauDeBordSalle.CarteTable;
import com.pingmanager.core.TableauDeBordSalle.Engagement;
import com.pingmanager.core.TableauDeBordSalle.JoueurEnAttente;
import com.pingmanager.core.TableauDeBordSalle.MatchSalle;
import com.pingmanager.model.ElementPlan;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.Poule;
import com.pingmanager.model.SalleTournoi;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Écran principal du juge-arbitre le jour J (B011, B037) : à gauche les
 * « Matchs proposés » de tous les tableaux triés par l'ordonnanceur, en couleur
 * (rouge, orange, vert : {@link com.pingmanager.core.ReglePrioriteSalle}), puis
 * les joueurs en attente avec leur chrono (un clic montre pourquoi et leur
 * poule ou leur tour, nom surligné) ; au centre le bandeau « Où en sont les tableaux »
 * (fourni par {@link EcranJourJ}) et une carte par table (libre ou occupée,
 * match en cours, durée écoulée).
 *
 * <p>Gestes du juge-arbitre, en peu de clics :</p>
 * <ul>
 *   <li>« Proposer » (Alt+P) remplit les tables libres selon l'ordonnanceur ;</li>
 *   <li>« Table N » sur un match proposé l'envoie en un clic sur la première
 *       table libre ; glisser un match sur une table libre, ou « Autre table… »
 *       (bouton ou clic droit) ; « Envoyer le prochain » sur une table libre ;</li>
 *   <li>« Terminer » sur une table ouvre la saisie du score (composants des
 *       poules et du tableau final) ; à l'enregistrement la table se libère et
 *       tout se met à jour ;</li>
 *   <li>« Annuler » sur une table occupée corrige une erreur d'envoi.</li>
 * </ul>
 *
 * <p>Toute la logique est dans {@link ServiceSalle} (cœur) et
 * {@link PresentationSalle} ; chaque action réussie enregistre le tournoi
 * aussitôt. Après un score, « Envoyer … à la table N » (Entrée) enchaîne le
 * match suivant sur la table libérée, sans changer de page. Le bandeau est
 * reconstruit à chaque action : tout se met à jour en direct. Les durées se
 * rafraîchissent toutes les 20 secondes.</p>
 */
final class EcranSalle {

    private static final Duration FREQUENCE_CHRONO = Duration.seconds(20);

    private final Tournoi tournoi;
    private final Runnable enregistrer;
    private final Runnable onRetour;
    private final String libelleRetour;
    private final String titre;
    private final Supplier<Node> bandeau;
    private final ServiceSalle salle;
    private final SaisieResultatMatch saisiePoule;
    private final ServicePhaseFinale servicePhaseFinale = new ServicePhaseFinale();

    private final BorderPane racine = new BorderPane();
    private final ScrollPane defilement;
    private final Map<Label, LocalDateTime> chronos = new IdentityHashMap<>();
    /** Chronos d'attente des joueurs (texte nu, rouge au-delà du seuil). */
    private final Map<Label, LocalDateTime> chronosJoueurs = new IdentityHashMap<>();
    private Timeline minuterie;

    private TableauDeBordSalle bord;
    private Match enSaisie;
    private String messageSucces;
    private String messageErreur;
    /** Table libérée par le dernier score saisi : propose d'y enchaîner le match suivant. */
    private int tableLiberee;
    /** Licence du joueur en attente dont on montre « pourquoi », ou null. */
    private String joueurDetail;
    /** Panneau « Lancer un match » ouvert ? */
    private boolean lancerManuel;
    private String rechercheLancer = "";
    /** Tables dont l'annulation attend la confirmation. */
    private final Set<Integer> annulationsAConfirmer = new HashSet<>();
    /** Lancement suspendu le temps de choisir comment annoncer la rencontre. */
    private Consumer<ModeAnnonce> lancementEnAttente;
    /** Annonces du dernier lancement, affichées à l'écran. */
    private List<AnnonceRencontre> annonces = List.of();
    /** Ouvre un tableau (poules ou arbre) par son nom, ou null. */
    private Consumer<String> ouvrirTableau;
    /** Salle du plan affichée (B024), ou -1 pour toutes. */
    private int salleAffichee = -1;
    private final Horloge horloge;
    /** Convocations par SMS (B019), ou null si non branchées. */
    private com.pingmanager.core.ServiceConvocationsSms sms;
    /** Ouvre les réglages des SMS (B019), ou null. */
    private Runnable ouvrirSms;

    EcranSalle(Tournoi tournoi, Runnable enregistrer, Runnable onRetour, Horloge horloge) {
        this(tournoi, enregistrer, onRetour, "← Jour J", "Salle et tables", null, horloge);
    }

    /**
     * @param bandeau bloc posé au-dessus des tables, reconstruit à chaque
     *                rafraîchissement (bandeau d'avancement des tableaux), ou null
     */
    EcranSalle(Tournoi tournoi, Runnable enregistrer, Runnable onRetour, String libelleRetour, String titre,
               Supplier<Node> bandeau, Horloge horloge) {
        this.tournoi = tournoi;
        this.enregistrer = enregistrer;
        this.onRetour = onRetour;
        this.libelleRetour = libelleRetour;
        this.titre = titre;
        this.bandeau = bandeau;
        this.salle = new ServiceSalle(horloge);
        this.horloge = horloge;
        this.saisiePoule = new SaisieResultatMatch(horloge);
        this.defilement = Ui.defilant(new VBox());
        racine.setCenter(defilement);
        racine.getStyleClass().add("salle");
    }

    /** Permet d'ouvrir les poules ou l'arbre d'un tableau depuis « pourquoi ». */
    EcranSalle surOuvrirTableau(Consumer<String> ouvrir) {
        this.ouvrirTableau = ouvrir;
        return this;
    }

    /**
     * Branche les convocations par SMS (B019) : envoi automatique au lancement
     * selon l'annonce choisie, et bouton vers les réglages.
     */
    EcranSalle avecSms(com.pingmanager.core.ServiceConvocationsSms service, Runnable ouvrirReglages) {
        this.sms = service;
        this.ouvrirSms = ouvrirReglages;
        return this;
    }

    /** SMS au lancement d'un match (B019) : résumé à ajouter au message, vide si rien n'est parti. */
    private String smsApresLancement(Match m, int table, ModeAnnonce mode) {
        if (sms == null) {
            return "";
        }
        LocalDateTime maintenant = LocalDateTime.ofInstant(horloge.maintenant(), java.time.ZoneId.systemDefault());
        String resume = sms.apresLancement(tournoi, m, table, mode, maintenant).resume();
        return resume.isEmpty() ? "" : " SMS : " + resume;
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
        double position = lancementEnAttente != null ? 0 : defilement.getVvalue();
        chronos.clear();
        chronosJoueurs.clear();
        bord = salle.tableauDeBord(tournoi);

        Button retour = Ui.boutonLien(libelleRetour);
        retour.setOnAction(e -> {
            arreterMinuterie();
            onRetour.run();
        });
        VBox contenu = Ui.page(Double.MAX_VALUE, retour, entete());

        if (messageSucces != null || messageErreur != null) {
            Label message = Ui.message(messageErreur, messageSucces);
            HBox ligneMessage = Ui.ligne(16, message);
            Node suite = boutonSuite();
            if (suite != null && messageErreur == null) {
                ligneMessage.getChildren().add(suite);
            }
            contenu.getChildren().add(ligneMessage);
            messageSucces = null;
            messageErreur = null;
        }
        for (AnnonceRencontre a : annonces) {
            contenu.getChildren().add(encartAnnonce(a));
        }
        annonces = List.of();
        if (lancementEnAttente != null) {
            contenu.getChildren().add(choixAnnonce());
        }
        tableLiberee = 0;
        if (ferme()) {
            contenu.getChildren().add(Ui.encartInfo("Ce tournoi est annulé : la salle est fermée."));
        }
        if (!bord.getAnomalies().isEmpty()) {
            contenu.getChildren().add(carteAnomalies());
        }
        if (bandeau != null) {
            contenu.getChildren().add(bandeau.get());
        }
        Label intertitreTables = Ui.sousTitre("Salle");
        intertitreTables.getStyleClass().add("jourj-intertitre");
        contenu.getChildren().addAll(intertitreTables, grilleTables());
        defilement.setContent(contenu);
        javafx.application.Platform.runLater(() -> defilement.setVvalue(position));
        racine.setLeft(colonneGauche());
        if (enSaisie == null) {
            Ui.apparition(racine.getLeft());
        }
    }

    /** Colonne de gauche : saisie du score, « Lancer un match », « pourquoi », ou matchs proposés. */
    private Node colonneGauche() {
        if (enSaisie != null) {
            return panneauSaisie(enSaisie);
        }
        if (lancerManuel) {
            return cote(panneauLancer());
        }
        if (joueurDetail != null) {
            JoueurEnAttente j = bord.joueurEnAttente(joueurDetail);
            if (j != null) {
                return cote(panneauJoueur(j));
            }
            joueurDetail = null; // il joue ou n'a plus rien à jouer
        }
        return colonneMatchs();
    }

    /**
     * En-tête compact : titre et nom du tournoi, chiffres de la salle sur une
     * ligne ; à droite, le choix d'annonce mémorisé, « Lancer un match » (Alt+L)
     * et « Proposer » (Alt+P).
     */
    private Node entete() {
        Label t = Ui.titre(titre);
        Label nom = Ui.discret(tournoi.getNom() == null ? "" : tournoi.getNom());
        Label chiffres = new Label(PresentationSalle.resumeSalle(bord));
        chiffres.getStyleClass().add("jourj-chiffres");
        chiffres.setWrapText(true);
        VBox gauche = new VBox(4, t, nom);

        Button proposer = Ui.boutonPrimaire("_Proposer");
        proposer.setMnemonicParsing(true);
        proposer.setDisable(ferme() || bord.getNombreDeTables() == 0);
        proposer.setTooltip(new Tooltip("Alt+P · remplit toutes les tables libres avec les matchs proposés, du plus "
                + "prioritaire au moins prioritaire. Un joueur n'est jamais envoyé sur deux tables."));
        proposer.setOnAction(e -> proposer());

        Button lancer = Ui.boutonSecondaire("_Lancer un match");
        lancer.setMnemonicParsing(true);
        lancer.setDisable(ferme() || bord.getNombreDeTables() == 0);
        lancer.setTooltip(new Tooltip("Alt+L · choisir soi-même le match et la table, sans passer par les matchs "
                + "proposés."));
        lancer.setOnAction(e -> {
            enSaisie = null;
            joueurDetail = null;
            lancerManuel = true;
            afficher();
        });

        MenuButton annonce = new MenuButton(PresentationSalle.libelleAnnonce(tournoi.getModeAnnonce()));
        annonce.getStyleClass().add("jourj-annonce-choix");
        annonce.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        annonce.setTooltip(new Tooltip("Comment annoncer une rencontre envoyée sur une table. Choix mémorisé pour "
                + "ce tournoi, modifiable ici à tout moment."));
        for (ModeAnnonce m : ModeAnnonce.values()) {
            MenuItem item = new MenuItem(m.getLibelle());
            item.setOnAction(e -> memoriserAnnonce(m));
            annonce.getItems().add(item);
        }
        MenuItem demander = new MenuItem("Demander à chaque lancement");
        demander.setOnAction(e -> memoriserAnnonce(null));
        annonce.getItems().addAll(new SeparatorMenuItem(), demander);

        HBox actions = Ui.ligne(10, annonce, lancer, proposer);
        if (ouvrirSms != null) {
            Button boutonSms = Ui.boutonSecondaire("SMS…");
            boutonSms.setTooltip(new Tooltip("Convocations par SMS : réglages, numéros consentis, journal des envois."));
            boutonSms.setOnAction(e -> {
                arreterMinuterie();
                ouvrirSms.run();
            });
            actions.getChildren().add(0, boutonSms);
        }
        HBox haut = Ui.ligne(16, gauche, Ui.espace(), actions);
        haut.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        return new VBox(6, haut, chiffres);
    }

    /**
     * Après un score : « Envoyer A – B à la table N » pour enchaîner le match
     * suivant sur la table libérée (touche Entrée), ou null s'il n'y a rien à
     * enchaîner.
     */
    private Node boutonSuite() {
        if (ferme()) {
            return null;
        }
        Optional<MatchSalle> suite = PresentationSalle.suiteSurTable(bord, tableLiberee);
        if (suite.isEmpty()) {
            return null;
        }
        int table = tableLiberee;
        Match m = suite.get().match();
        Button envoyer = Ui.boutonPrimaire("Envoyer " + PresentationSalle.joueurs(m) + " à la table " + table);
        envoyer.setDefaultButton(true);
        envoyer.setTooltip(new Tooltip("Entrée · " + PresentationSalle.contexte(suite.get())));
        envoyer.setOnAction(e -> affecter(m, table));
        javafx.application.Platform.runLater(envoyer::requestFocus);
        return envoyer;
    }

    // ==================================================================
    // Grille des tables
    // ==================================================================

    private Node grilleTables() {
        if (bord.getNombreDeTables() == 0) {
            return Ui.carte("Tables", Ui.texte("Le nombre de tables du tournoi n'est pas renseigné. Indiquez-le dans "
                    + "« Informations sportives et techniques » pour afficher la salle."));
        }
        if (tournoi.aUnPlanDeSalle()) {
            return planDesSalles();
        }
        FlowPane grille = new FlowPane(12, 12);
        grille.getStyleClass().add("salle-grille");
        // Autant de colonnes que la largeur disponible le permet.
        grille.prefWrapLengthProperty().bind(defilement.widthProperty().subtract(64));
        for (CarteTable c : bord.getTables()) {
            grille.getChildren().add(carteTable(c));
        }
        return grille;
    }

    // ==================================================================
    // Plan réel des salles (B024)
    // ==================================================================

    /** Largeur d'une colonne de table (une table = 2 colonnes, une carte de 220 px). */
    private static final double COLONNE_TABLE = 115;
    /** Taille d'une case d'allée ou de repère. */
    private static final double CASE_ETROITE = 26;

    /**
     * Les salles telles que le club les a dessinées : mini-carte (choix de la
     * salle, occupation par étage) puis le plan, chaque table à sa place ; les
     * tables du tournoi absentes du plan s'affichent à part.
     */
    private Node planDesSalles() {
        List<SalleTournoi> salles = tournoi.getSalles();
        if (salleAffichee >= salles.size()) {
            salleAffichee = -1;
        }
        VBox bloc = new VBox(16);
        if (salles.size() > 1) {
            bloc.getChildren().add(miniCarteSalles(salles));
        }
        for (int i = 0; i < salles.size(); i++) {
            if (salleAffichee < 0 || salleAffichee == i) {
                SalleTournoi s = salles.get(i);
                Label titreSalle = Ui.gras(s.getNom() + " · " + PresentationPlanSalle.occupation(s, bord));
                bloc.getChildren().add(new VBox(8, titreSalle, planSalle(s)));
            }
        }
        List<Integer> horsPlan = PresentationPlanSalle.tablesHorsPlan(salles, bord.getNombreDeTables());
        if (!horsPlan.isEmpty() && salleAffichee < 0) {
            FlowPane autres = new FlowPane(12, 12);
            autres.getStyleClass().add("salle-grille");
            autres.prefWrapLengthProperty().bind(defilement.widthProperty().subtract(64));
            horsPlan.forEach(n -> autres.getChildren().add(carteTable(bord.table(n))));
            bloc.getChildren().add(new VBox(8, Ui.gras("Tables hors plan"), autres));
        }
        return bloc;
    }

    private Node miniCarteSalles(List<SalleTournoi> salles) {
        HBox etages = new HBox(16);
        etages.getStyleClass().add("plan-mini-carte");
        for (PresentationPlanSalle.EtageMiniCarte etage : PresentationPlanSalle.miniCarte(salles, salleAffichee)) {
            FlowPane tuiles = new FlowPane(8, 8);
            for (PresentationPlanSalle.SalleMiniCarte m : etage.salles()) {
                VBox tuile = new VBox(2, Ui.gras(m.nom()),
                        Ui.discret(PresentationPlanSalle.occupation(salles.get(m.index()), bord)));
                tuile.getStyleClass().add("plan-mini-salle");
                if (m.courante()) {
                    tuile.getStyleClass().add("plan-mini-salle-active");
                }
                tuile.setOnMouseClicked(e -> {
                    salleAffichee = m.index();
                    afficher();
                });
                tuiles.getChildren().add(tuile);
            }
            etages.getChildren().add(new VBox(4, Ui.discret(etage.libelle()), tuiles));
        }
        VBox bloc = new VBox(8, etages);
        if (salleAffichee >= 0) {
            Button toutes = Ui.boutonLien("Voir toutes les salles");
            toutes.setOnAction(e -> {
                salleAffichee = -1;
                afficher();
            });
            bloc.getChildren().add(toutes);
        }
        return bloc;
    }

    private Node planSalle(SalleTournoi s) {
        PlanSalle plan = s.getPlan();
        boolean[] colonnes = PresentationPlanSalle.avecTable(plan, true);
        boolean[] lignes = PresentationPlanSalle.avecTable(plan, false);
        GridPane g = new GridPane();
        g.getStyleClass().addAll("plan-grille", "plan-grille-jourj");
        for (boolean table : colonnes) {
            double l = table ? COLONNE_TABLE : CASE_ETROITE;
            g.getColumnConstraints().add(new ColumnConstraints(l, l, l));
        }
        for (boolean table : lignes) {
            RowConstraints r = new RowConstraints();
            if (table) {
                r.setMinHeight(60);
            } else {
                r.setMinHeight(CASE_ETROITE);
                r.setPrefHeight(CASE_ETROITE);
                r.setMaxHeight(CASE_ETROITE);
            }
            g.getRowConstraints().add(r);
        }
        for (PresentationPlanSalle.Tuile t : PresentationPlanSalle.tuiles(plan)) {
            Node noeud;
            if (t.element() instanceof ElementPlan.TablePlan table) {
                CarteTable c = table.numero() <= bord.getNombreDeTables() ? bord.table(table.numero()) : null;
                if (c != null) {
                    noeud = carteTable(c);
                } else {
                    Label hors = Ui.discret("Table " + table.numero() + "\nhors compétition");
                    hors.getStyleClass().addAll("plan-element", "plan-table-hors");
                    hors.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    noeud = hors;
                }
            } else {
                Label l = new Label(t.texte());
                l.getStyleClass().addAll(t.classes());
                l.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                Tooltip.install(l, new Tooltip(t.infobulle()));
                noeud = l;
            }
            g.add(noeud, t.colonne(), t.ligne(), t.largeur(), t.hauteur());
        }
        return g;
    }

    private Node carteTable(CarteTable c) {
        Label numero = new Label(String.valueOf(c.numero()));
        numero.getStyleClass().add("salle-numero");
        Label etat = Ui.pastille(PresentationSalle.etat(c), c.estLibre() ? Ui.ETAT_OK : Ui.ETAT_RECOMMANDE);
        VBox carte = new VBox(8, Ui.ligne(10, numero, Ui.espace(), etat));
        carte.getStyleClass().addAll("salle-table", c.estLibre() ? "salle-table-libre" : "salle-table-occupee");
        carte.setPrefWidth(220);
        carte.setMinWidth(220);
        carte.setMaxWidth(220);

        if (c.estLibre()) {
            carte.getChildren().add(Ui.aide("Glissez un match proposé ici."));
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
            Label tableau = Ui.discret(m.tableau());
            Label tour = Ui.discret(m.tour());
            Label a = joueur(PresentationSalle.nom(m.match().getJoueurA()));
            Label b = joueur(PresentationSalle.nom(m.match().getJoueurB()));
            Label duree = new Label();
            duree.getStyleClass().add("salle-duree");
            LocalDateTime debut = m.match().getInstantLancement();
            if (debut != null) {
                chronos.put(duree, debut);
                majChrono(duree, debut);
            }
            Button terminer = Ui.boutonPrimaire(PresentationSalle.libelleTerminer(tournoi, m.match()));
            terminer.getStyleClass().add("jourj-terminer");
            terminer.setMaxWidth(Double.MAX_VALUE);
            terminer.setDisable(ferme());
            terminer.setTooltip(new Tooltip("Saisir le score : la table se libère à l'enregistrement."));
            terminer.setOnAction(e -> {
                enSaisie = m.match();
                lancerManuel = false;
                joueurDetail = null;
                afficher();
            });
            carte.getChildren().addAll(new VBox(0, tableau, tour), a, b, duree, terminer);
            if (annulationsAConfirmer.contains(c.numero())) {
                Button oui = Ui.boutonSecondaire("Oui, annuler");
                oui.getStyleClass().add("jourj-danger");
                Button non = Ui.boutonLien("Non");
                oui.setOnAction(e -> {
                    annulationsAConfirmer.remove(c.numero());
                    annulerAffectation(m.match());
                });
                non.setOnAction(e -> {
                    annulationsAConfirmer.remove(c.numero());
                    afficher();
                });
                carte.getChildren().add(Ui.encartConfirmation(PresentationSalle.confirmationAnnulation(c.numero()),
                        oui, non));
            } else {
                Button annuler = Ui.boutonLien("Annuler");
                annuler.getStyleClass().addAll("lien-danger", "jourj-annuler");
                annuler.setDisable(ferme());
                annuler.setTooltip(new Tooltip("Erreur d'envoi : la table redevient libre et le match revient dans "
                        + "les matchs proposés (confirmation demandée)."));
                annuler.setOnAction(e -> {
                    annulationsAConfirmer.add(c.numero());
                    afficher();
                });
                carte.getChildren().add(annuler);
            }
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

    private void majChronoJoueur(Label l, LocalDateTime depuis) {
        java.time.Duration attente = java.time.Duration.between(depuis, salle.maintenant());
        l.setText(PresentationSalle.duree(attente));
        l.getStyleClass().remove("jourj-chrono-long");
        if (attente.compareTo(com.pingmanager.core.ReglePrioriteSalle.ATTENTE_LONGUE) >= 0) {
            l.getStyleClass().add("jourj-chrono-long");
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
            prets.getChildren().add(Ui.etatVideCompact("Aucun match à proposer. Les parties des poules lancées et du "
                    + "tableau final apparaissent ici dès que leurs deux joueurs sont libres."));
        }
        VBox colonne = new VBox(16, Ui.carte(Ui.titreAvecCompteur("Matchs proposés", liste.size()),
                Ui.aide("Tous tableaux confondus, du plus prioritaire au moins prioritaire. Rouge : attend depuis "
                        + "longtemps ; orange : important ou prochain à envoyer ; vert : peut attendre. « Table N » "
                        + "envoie le match sur la première table libre ; vous pouvez aussi le glisser sur une table."),
                prets));

        List<JoueurEnAttente> attente = bord.getJoueursEnAttente();
        VBox joueurs = new VBox(0);
        for (JoueurEnAttente j : attente) {
            joueurs.getChildren().add(ligneJoueur(j));
        }
        if (attente.isEmpty()) {
            joueurs.getChildren().add(Ui.etatVideCompact("Personne n'attend : tous les joueurs qui ont encore un "
                    + "match sont sur une table."));
        }
        colonne.getChildren().add(Ui.carte(Ui.titreAvecCompteur("Joueurs en attente", attente.size()),
                Ui.aide("Du plus long chrono au plus court. Cliquez sur un joueur pour savoir pourquoi il ne joue pas."),
                joueurs));
        return cote(colonne);
    }

    /** Colonne de gauche défilante, largeur fixe. */
    private Node cote(VBox colonne) {
        colonne.getStyleClass().add("salle-colonne");
        colonne.setPrefWidth(360);
        colonne.setMaxWidth(360);
        ScrollPane s = Ui.defilant(colonne);
        s.getStyleClass().add("salle-cote");
        s.setMinWidth(380);
        BorderPane.setMargin(s, new Insets(24, 0, 24, 24));
        return s;
    }

    private Node lignePret(MatchSalle m, int rang) {
        Label joueurs = Ui.gras(PresentationSalle.joueurs(m.match()));
        joueurs.setWrapText(true);
        Label niveau = Ui.pastille(m.niveau().getLibelle(), PresentationSalle.pastillePriorite(m.niveau()));
        niveau.setTooltip(new Tooltip(PresentationSalle.detailPriorite(m)));
        VBox infos = new VBox(2, Ui.ligne(8, Ui.discret(PresentationSalle.contexte(m)), Ui.espace(), niveau), joueurs);
        if (!m.motifNiveau().isEmpty()) {
            infos.getChildren().add(Ui.aide(m.motifNiveau()));
        }
        Optional<Integer> libre = PresentationSalle.premiereTableLibre(bord);
        Button direct = Ui.boutonPrimaire(libre.map(n -> "Table " + n).orElse("Aucune table libre"));
        direct.getStyleClass().add("jourj-envoi-direct");
        direct.setDisable(ferme() || libre.isEmpty());
        libre.ifPresent(n -> {
            direct.setTooltip(new Tooltip("Envoyer ce match à la table " + n + " (première table libre)"));
            direct.setOnAction(e -> affecter(m.match(), n));
        });
        MenuButton envoyer = new MenuButton("Autre table…");
        envoyer.getStyleClass().add("salle-envoyer");
        envoyer.getItems().addAll(itemsEnvoi(m.match()));
        envoyer.setDisable(ferme() || bord.tablesLibres().isEmpty());
        VBox ligne = new VBox(6, infos, Ui.ligne(8, direct, envoyer));
        ligne.getStyleClass().addAll("salle-pret", PresentationSalle.classePriorite(m.niveau()));
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

    /** Un joueur en attente : nom, motif court, chrono ; un clic montre pourquoi. */
    private Node ligneJoueur(JoueurEnAttente j) {
        Label nom = Ui.gras(PresentationSalle.nom(j.joueur()));
        Label motif = Ui.discret(PresentationSalle.motifCourt(j));
        motif.setWrapText(true);
        Label chrono = new Label(PresentationSalle.chrono(j));
        chrono.getStyleClass().add("jourj-chrono");
        if (PresentationSalle.attenteLongue(j)) {
            chrono.getStyleClass().add("jourj-chrono-long");
        }
        if (j.depuis() != null) {
            chronosJoueurs.put(chrono, j.depuis());
        }
        VBox infos = new VBox(2, nom, motif);
        HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);
        HBox ligne = Ui.ligne(10, infos, chrono);
        ligne.getStyleClass().addAll("salle-pret", "jourj-joueur-attente");
        ligne.setCursor(Cursor.HAND);
        Tooltip.install(ligne, new Tooltip("Pourquoi " + PresentationSalle.nom(j.joueur()) + " ne joue pas ?"));
        ligne.setOnMouseClicked(e -> {
            joueurDetail = j.joueur().getNumeroLicence();
            afficher();
        });
        return ligne;
    }

    /**
     * « Pourquoi » un joueur ne joue pas : une phrase par match à jouer, puis sa
     * poule ou son tour avec son nom surligné.
     */
    private VBox panneauJoueur(JoueurEnAttente j) {
        Button retour = Ui.boutonLien("← Matchs proposés");
        retour.setOnAction(e -> {
            joueurDetail = null;
            afficher();
        });
        Label chrono = new Label(j.attente() == null ? "Chrono pas encore commencé"
                : "En attente depuis " + PresentationSalle.chrono(j));
        chrono.getStyleClass().add("jourj-chrono");
        if (PresentationSalle.attenteLongue(j)) {
            chrono.getStyleClass().add("jourj-chrono-long");
        }
        VBox carte = Ui.carte(Ui.titreCarte("Pourquoi " + PresentationSalle.nom(j.joueur()) + " ne joue pas"),
                chrono);
        List<String> phrases = PresentationSalle.pourquoi(j, bord);
        Optional<Integer> libre = PresentationSalle.premiereTableLibre(bord);
        // Une phrase par match, puis la poule (ou le tour) une seule fois, nom surligné.
        Map<String, VBox> blocs = new java.util.LinkedHashMap<>();
        Map<String, Engagement> premiers = new java.util.HashMap<>();
        for (int i = 0; i < j.engagements().size(); i++) {
            Engagement e = j.engagements().get(i);
            String cle = e.match().tableau() + " | " + e.groupe();
            premiers.putIfAbsent(cle, e);
            VBox bloc = blocs.computeIfAbsent(cle, k -> {
                VBox v = new VBox(8);
                v.getStyleClass().add("jourj-pourquoi");
                return v;
            });
            Label phrase = Ui.texte(phrases.get(i));
            phrase.setWrapText(true);
            bloc.getChildren().add(phrase);
            if (e.raison() == TableauDeBordSalle.RaisonAttente.PRET && libre.isPresent() && !ferme()) {
                int table = libre.get();
                Button envoyer = Ui.boutonPrimaire("Envoyer à la table " + table);
                envoyer.setOnAction(x -> affecter(e.match().match(), table));
                bloc.getChildren().add(envoyer);
            }
        }
        for (Map.Entry<String, VBox> entree : blocs.entrySet()) {
            VBox bloc = entree.getValue();
            Engagement e = premiers.get(entree.getKey());
            bloc.getChildren().add(groupe(e, j));
            HBox actions = Ui.ligne(8);
            if (ouvrirTableau != null) {
                Button voir = Ui.boutonSecondaire("Voir le tableau");
                voir.setTooltip(new Tooltip("Ouvrir les poules ou l'arbre de « " + e.match().tableau() + " »"));
                voir.setOnAction(x -> {
                    arreterMinuterie();
                    ouvrirTableau.accept(e.match().tableau());
                });
                actions.getChildren().add(voir);
            }
            if (!actions.getChildren().isEmpty()) {
                bloc.getChildren().add(actions);
            }
            carte.getChildren().add(bloc);
        }
        return new VBox(12, retour, carte);
    }

    /** Poule ou tour d'un engagement, le joueur surligné. */
    private Node groupe(Engagement e, JoueurEnAttente j) {
        VBox lignes = new VBox(2);
        Label titre = Ui.discret(e.match().tableau() + " · " + e.groupe());
        titre.getStyleClass().add("jourj-groupe-titre");
        lignes.getChildren().add(titre);
        for (PresentationSalle.LigneGroupe l : PresentationSalle.lignesGroupe(e, j.joueur())) {
            Label a = nomGroupe(l.joueurA(), l.surligneA());
            Label b = nomGroupe(l.joueurB(), l.surligneB());
            Label etat = Ui.discret(l.etat());
            HBox ligne = Ui.ligne(6, a, Ui.discret("–"), b, Ui.espace(), etat);
            ligne.getStyleClass().add("jourj-groupe-ligne");
            if (l.concerne()) {
                ligne.getStyleClass().add("jourj-groupe-concerne");
            }
            lignes.getChildren().add(ligne);
        }
        lignes.getStyleClass().add("jourj-groupe");
        return lignes;
    }

    private static Label nomGroupe(String nom, boolean surligne) {
        Label l = new Label(nom);
        l.getStyleClass().add("texte");
        if (surligne) {
            l.getStyleClass().add("jourj-surligne");
        }
        return l;
    }

    /**
     * « Lancer un match » : tous les matchs à jouer dont les deux joueurs sont
     * connus, cherchés par nom de joueur ou de tableau ; les matchs bloqués
     * restent visibles avec leur motif.
     */
    private VBox panneauLancer() {
        Button fermer = Ui.boutonLien("← Matchs proposés");
        fermer.setOnAction(e -> {
            lancerManuel = false;
            rechercheLancer = "";
            afficher();
        });
        TextField recherche = Ui.champ("Nom d'un joueur ou d'un tableau");
        recherche.setText(rechercheLancer);
        VBox resultats = new VBox(0);
        Runnable remplir = () -> {
            resultats.getChildren().clear();
            List<PresentationSalle.Lancable> liste = PresentationSalle.lancables(bord, rechercheLancer);
            for (PresentationSalle.Lancable l : liste) {
                resultats.getChildren().add(ligneLancable(l));
            }
            if (liste.isEmpty()) {
                resultats.getChildren().add(Ui.etatVideCompact(rechercheLancer.isBlank()
                        ? "Aucun match à jouer dont les deux joueurs sont connus."
                        : "Aucun match ne correspond à « " + rechercheLancer.trim() + " »."));
            }
        };
        recherche.textProperty().addListener((o, avant, apres) -> {
            rechercheLancer = apres == null ? "" : apres;
            remplir.run();
        });
        remplir.run();
        javafx.application.Platform.runLater(recherche::requestFocus);
        VBox carte = Ui.carte(Ui.titreCarte("Lancer un match"),
                Ui.aide("Choisissez le match et la table vous-même. Un joueur déjà sur une table ne peut pas être "
                        + "envoyé sur une autre : ses matchs restent grisés avec le motif."),
                recherche, resultats);
        return new VBox(12, fermer, carte);
    }

    private Node ligneLancable(PresentationSalle.Lancable l) {
        MatchSalle m = l.match();
        Label joueurs = Ui.gras(PresentationSalle.joueurs(m.match()));
        joueurs.setWrapText(true);
        VBox ligne = new VBox(6, Ui.discret(PresentationSalle.contexte(m)), joueurs);
        ligne.getStyleClass().add("salle-pret");
        if (!l.possible()) {
            ligne.getStyleClass().add("salle-bloque");
            ligne.getChildren().add(Ui.pastille(l.refus(), StatutPastille.NEUTRE));
            return ligne;
        }
        Optional<Integer> libre = PresentationSalle.premiereTableLibre(bord);
        Button direct = Ui.boutonPrimaire(libre.map(n -> "Table " + n).orElse("Aucune table libre"));
        direct.getStyleClass().add("jourj-envoi-direct");
        direct.setDisable(ferme() || libre.isEmpty());
        libre.ifPresent(n -> direct.setOnAction(e -> affecter(m.match(), n)));
        MenuButton autre = new MenuButton("Autre table…");
        autre.getStyleClass().add("salle-envoyer");
        autre.getItems().addAll(itemsEnvoi(m.match()));
        autre.setDisable(ferme() || bord.tablesLibres().isEmpty());
        ligne.getChildren().add(Ui.ligne(8, direct, autre));
        return ligne;
    }

    // ==================================================================
    // Annonce de la rencontre
    // ==================================================================

    /**
     * Lance une action d'envoi sur une table avec la façon d'annoncer mémorisée,
     * ou, s'il n'y en a pas, après avoir demandé « Comment voulez-vous annoncer
     * la rencontre ? ».
     */
    private void lancerAvecAnnonce(Consumer<ModeAnnonce> action) {
        if (ferme()) {
            return;
        }
        ModeAnnonce mode = tournoi.getModeAnnonce();
        if (mode != null) {
            action.accept(mode);
            return;
        }
        lancementEnAttente = action;
        afficher();
    }

    /** Question posée au premier lancement : rien, SMS, appel vocal, les deux ; choix à retenir. */
    private Node choixAnnonce() {
        Label question = Ui.titreCarte("Comment voulez-vous annoncer la rencontre ?");
        CheckBox retenir = new CheckBox("Retenir ce choix pour ce tournoi (modifiable en haut de la page)");
        retenir.setSelected(true);
        HBox choix = Ui.ligne(8);
        for (ModeAnnonce m : ModeAnnonce.values()) {
            Button b = m == ModeAnnonce.AUCUNE ? Ui.boutonSecondaire(m.getLibelle()) : Ui.boutonPrimaire(m.getLibelle());
            b.setOnAction(e -> {
                Consumer<ModeAnnonce> action = lancementEnAttente;
                lancementEnAttente = null;
                if (retenir.isSelected()) {
                    tournoi.setModeAnnonce(m);
                    // Enregistré tout de suite : le choix retenu ne doit pas dépendre de la réussite du lancement.
                    try {
                        enregistrer.run();
                    } catch (RuntimeException ex) {
                        messageErreur = "Attention : l'enregistrement du choix d'annonce a échoué (" + ex.getMessage()
                                + ").";
                    }
                }
                if (action != null) {
                    action.accept(m);
                } else {
                    afficher();
                }
            });
            choix.getChildren().add(b);
        }
        Button renoncer = Ui.boutonLien("Ne pas lancer");
        renoncer.setOnAction(e -> {
            lancementEnAttente = null;
            afficher();
        });
        choix.getChildren().addAll(Ui.espace(), renoncer);
        VBox encart = new VBox(10, question, Ui.aide("SMS : messages simulés pour l'instant (bouton « SMS… »), "
                + "envoyés aux joueurs qui ont donné leur numéro. L'appel vocal arrive bientôt : l'annonce s'affiche "
                + "ici pour être lue au micro."), choix, retenir);
        encart.getStyleClass().addAll("encart-confirmation", "jourj-annonce-question");
        return encart;
    }

    /** Annonce affichée après un lancement, lisible de loin. */
    private Node encartAnnonce(AnnonceRencontre a) {
        Label texte = new Label(PresentationSalle.texteAnnonce(a));
        texte.setWrapText(true);
        texte.getStyleClass().add("jourj-annonce-texte");
        VBox v = new VBox(texte);
        v.getStyleClass().add("jourj-annonce");
        return v;
    }

    private void memoriserAnnonce(ModeAnnonce mode) {
        tournoi.setModeAnnonce(mode);
        apresAction(true, mode == null
                ? "Annonce des rencontres : demandée à chaque lancement."
                : "Annonce des rencontres : " + mode.getLibelle() + ". Choix mémorisé pour ce tournoi.");
    }

    /** Tableau et tour d'un match, pour l'annonce (vide s'il n'est plus dans la salle). */
    private String contexteDe(Match m) {
        for (MatchSalle x : bord.getPrets()) {
            if (x.match() == m) {
                return PresentationSalle.contexte(x);
            }
        }
        for (TableauDeBordSalle.MatchBloque b : bord.getBloques()) {
            if (b.match().match() == m) {
                return PresentationSalle.contexte(b.match());
            }
        }
        return "";
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
        s.setMinWidth(380);
        BorderPane.setMargin(s, new Insets(24, 0, 24, 24));
        return s;
    }

    private void apresResultat(Match m) {
        ResultatAffectationTable r = salle.libererApresResultat(tournoi, m);
        enSaisie = null;
        boolean reussi = r.estReussi();
        apresActionSansAffichage(reussi, PresentationSalle.message(r, bord.getNombreDeTables()));
        if (reussi && messageErreur == null) {
            tableLiberee = r.getNumeroTable();
        }
        afficher();
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
        lancerAvecAnnonce(mode -> {
            ResultatPropositionSalle r = salle.proposer(tournoi);
            if (r.aLance()) {
                List<AnnonceRencontre> liste = new ArrayList<>();
                StringBuilder smsEnvoyes = new StringBuilder();
                for (ResultatPropositionSalle.Lancement l : r.getLancements()) {
                    liste.add(AnnonceRencontre.preparer(mode, l.match().match(), l.table(),
                            PresentationSalle.contexte(l.match())));
                    smsEnvoyes.append(smsApresLancement(l.match().match(), l.table(), mode));
                }
                annonces = liste;
                apresAction(true, PresentationSalle.messageProposition(r) + smsEnvoyes);
                return;
            }
            apresAction(r.aLance(), PresentationSalle.messageProposition(r));
        });
    }

    private void affecter(Match m, int table) {
        lancerAvecAnnonce(mode -> {
            String contexte = contexteDe(m);
            ResultatAffectationTable r = salle.affecter(tournoi, m, table);
            String smsEnvoyes = "";
            if (r.estReussi()) {
                annonces = List.of(AnnonceRencontre.preparer(mode, m, table, contexte));
                lancerManuel = false;
                rechercheLancer = "";
                joueurDetail = null;
                smsEnvoyes = smsApresLancement(m, table, mode);
            }
            apresAction(r.estReussi(), PresentationSalle.message(r, bord.getNombreDeTables()) + smsEnvoyes);
        });
    }

    private void annulerAffectation(Match m) {
        ResultatAffectationTable r = salle.annulerAffectation(tournoi, m);
        apresAction(r.estReussi(), PresentationSalle.message(r, bord.getNombreDeTables()));
    }

    /** Enregistre aussitôt une action réussie, puis redessine toute la salle. */
    private void apresAction(boolean reussi, String texte) {
        apresActionSansAffichage(reussi, texte);
        afficher();
    }

    private void apresActionSansAffichage(boolean reussi, String texte) {
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
    }

    // ==================================================================
    // Rafraîchissement des durées
    // ==================================================================

    private void demarrerMinuterie() {
        minuterie = new Timeline(new KeyFrame(FREQUENCE_CHRONO, e -> {
            chronos.forEach(this::majChrono);
            chronosJoueurs.forEach(this::majChronoJoueur);
        }));
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
