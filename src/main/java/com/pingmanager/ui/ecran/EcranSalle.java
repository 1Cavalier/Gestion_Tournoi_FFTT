package com.pingmanager.ui.ecran;

import com.pingmanager.core.AnnonceRencontre;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.ListeLancements;
import com.pingmanager.core.ResultatPropositionSalle;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.core.TableauDeBordSalle.CarteTable;
import com.pingmanager.core.TableauDeBordSalle.MatchSalle;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Poule;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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
 * Écran principal du juge-arbitre le jour J, de haut en bas : bandeau « Jour J »
 * et nom du tournoi ; trois cartes carrées ({@link TableauDeBordJourJ}) :
 * « Lancer un match » (ouvre à gauche l'{@link InterfaceLancementMatch} :
 * matchs proposés avec leur table et l'attente des joueurs, choix manuel),
 * « Annonces » (annonce des
 * rencontres, voix, SMS, prononciation, suivi sur téléphone) et « Stratégie en
 * place » ({@link SelecteurStrategie}) ; la liste des tableaux avec leur frise
 * (fournie par {@link EcranJourJ}) ; enfin la salle, une carte par table.
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
    private Timeline minuterie;

    private TableauDeBordSalle bord;
    private Match enSaisie;
    private String messageSucces;
    private String messageErreur;
    /** Table libérée par le dernier score saisi : propose d'y enchaîner le match suivant. */
    private int tableLiberee;
    /** Interface « Lancer un match » (B055) : garde l'onglet, la recherche et les tables choisies. */
    private final InterfaceLancementMatch lancement = new InterfaceLancementMatch(new ActionsLancement());
    /** Tables dont l'annulation attend la confirmation. */
    private final Set<Integer> annulationsAConfirmer = new HashSet<>();
    /** Lancement suspendu le temps de choisir comment annoncer la rencontre. */
    private Consumer<ModeAnnonce> lancementEnAttente;
    /** Annonces du dernier lancement, affichées à l'écran. */
    private List<AnnonceRencontre> annonces = List.of();
    /** Ouvre un tableau (poules ou arbre) par son nom, ou null. */
    private Consumer<String> ouvrirTableau;
    private final Horloge horloge;
    /** Convocations par SMS (B019), ou null si non branchées. */
    private com.pingmanager.core.ServiceConvocationsSms sms;
    /** Ouvre les réglages des SMS (B019), ou null. */
    private Runnable ouvrirSms;
    /** Annonces vocales (B017), ou null si non branchées. */
    private com.pingmanager.ui.infra.DiffuseurAnnonces voix;
    /** Ouvre l'écran « Prononciation des noms » (B018), ou null. */
    private Runnable ouvrirPrononciations;
    /** Interface « Lancer un match » ouverte (colonne de gauche) ? */
    private boolean lancementOuvert;
    /** Panneau ouvert sous les cartes : {@link #PANNEAU_ANNONCES}, {@link #PANNEAU_STRATEGIE} ou vide. */
    private String panneau = "";
    private static final String PANNEAU_ANNONCES = "annonces";
    private static final String PANNEAU_STRATEGIE = "strategie";
    /** Carte « Suivi sur téléphone » posée dans les annonces (fournie par le jour J), ou null. */
    private Supplier<Node> suiviTelephone;
    /**
     * Annonces non dites (voix indisponible) : zone gardée d'un affichage à
     * l'autre, complétée sans redessiner la page (une saisie de score en cours
     * n'est jamais perdue).
     */
    private final VBox zoneAnnoncesVocales = new VBox(8);

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

    /** Permet d'ouvrir les poules ou l'arbre d'un tableau depuis l'interface « Lancer un match ». */
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

    /** Carte « Suivi sur téléphone » (B021), rangée dans l'interface des annonces. */
    EcranSalle avecSuiviTelephone(Supplier<Node> carte) {
        this.suiviTelephone = carte;
        return this;
    }

    /** Branche l'écran « Prononciation des noms » (B018), ouvert depuis le menu « Voix ». */
    EcranSalle avecPrononciations(Runnable ouvrir) {
        this.ouvrirPrononciations = ouvrir;
        return this;
    }

    /**
     * Branche les annonces vocales (B017) : annonce automatique au lancement
     * selon l'annonce choisie, bouton « Annoncer » sur chaque table, rappels.
     */
    EcranSalle avecAnnoncesVocales(com.pingmanager.ui.infra.DiffuseurAnnonces diffuseur) {
        this.voix = diffuseur;
        diffuseur.surDiffusion(r -> Platform.runLater(() -> recevoirAnnonce(r)));
        return this;
    }

    /** Annonce traitée par la voix : si elle n'a pas été dite, elle s'affiche (repli silencieux). */
    private void recevoirAnnonce(com.pingmanager.core.ResultatAnnonceVocale r) {
        if (!r.estAffichee() || racine.getScene() == null) {
            return;
        }
        Label texte = new Label(PresentationSalle.texteRepli(r));
        texte.setWrapText(true);
        texte.getStyleClass().add("jourj-annonce-texte");
        Button fermer = Ui.boutonLien("Fermer");
        VBox encart = new VBox(4, texte, fermer);
        encart.getStyleClass().add("jourj-annonce");
        fermer.setOnAction(e -> zoneAnnoncesVocales.getChildren().remove(encart));
        zoneAnnoncesVocales.getChildren().add(0, encart);
        // Trois annonces au plus : les plus anciennes ont déjà été lues au micro.
        while (zoneAnnoncesVocales.getChildren().size() > 3) {
            zoneAnnoncesVocales.getChildren().remove(3);
        }
    }

    /** Annonce vocale au lancement d'un match (B017) : phrase à ajouter au message, vide si rien n'est demandé. */
    private String voixApresLancement(Match m, int table, ModeAnnonce mode) {
        if (voix == null || !com.pingmanager.core.ServiceAnnoncesVocales.annonceAutomatique(tournoi, mode)) {
            return "";
        }
        com.pingmanager.core.ResultatDemandeAnnonce r = voix.service().apresLancement(tournoi, m, table, mode);
        voix.diffuser();
        return " " + PresentationSalle.messageDemandeAnnonce(r, voix.service().voixDisponible());
    }

    /**
     * « Appel vocal » seul avec l'annonce automatique : la voix (ou, à défaut,
     * l'encart « À lire au micro ») suffit, pas d'encart d'annonce en double.
     */
    private boolean annonceVocaleSeule(ModeAnnonce mode) {
        return voix != null && mode == ModeAnnonce.APPEL
                && com.pingmanager.core.ServiceAnnoncesVocales.annonceAutomatique(tournoi, mode);
    }

    /** Bouton « Annoncer » d'une table occupée. */
    private void annoncerTable(Match m, int table) {
        com.pingmanager.core.ResultatDemandeAnnonce r = voix.service().annoncer(tournoi, m, table);
        voix.diffuser();
        String texte = PresentationSalle.messageDemandeAnnonce(r, voix.service().voixDisponible());
        if (r.acceptee()) {
            messageSucces = texte;
        } else {
            messageErreur = texte;
        }
        afficher();
    }

    /** Rappels arrivés à échéance : seulement si le match est toujours sur sa table. */
    private void verifierRappels() {
        if (voix == null) {
            return;
        }
        // Le tournoi a pu être relu depuis le fichier : on regarde la salle actuelle, pas l'objet mémorisé.
        if (voix.service().programmerRappelsDus((m, t) -> bord.getTables().stream().anyMatch(c -> c.numero() == t
                && !c.estLibre() && c.match().match().getStatut() == StatutMatch.EN_COURS
                && com.pingmanager.core.ServiceAnnoncesVocales.memeRencontre(c.match().match(), m))) > 0) {
            voix.diffuser();
        }
    }

    private void oublierAnnonce(Match m) {
        if (voix != null) {
            voix.service().oublier(m);
        }
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
        bord = salle.tableauDeBord(tournoi);

        Button retour = Ui.boutonLien(libelleRetour);
        retour.setOnAction(e -> {
            arreterMinuterie();
            onRetour.run();
        });
        VBox contenu = Ui.page(Double.MAX_VALUE, retour, TableauDeBordJourJ.bandeau(titre, tournoi.getNom()));

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
        contenu.getChildren().add(zoneAnnoncesVocales);
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
        contenu.getChildren().add(cartes());
        if (PANNEAU_ANNONCES.equals(panneau)) {
            contenu.getChildren().add(panneauAnnonces());
        } else if (PANNEAU_STRATEGIE.equals(panneau)) {
            contenu.getChildren().add(panneauStrategie());
        }
        if (bandeau != null) {
            contenu.getChildren().add(bandeau.get());
        }
        Label intertitreTables = Ui.sousTitre("Salle");
        intertitreTables.getStyleClass().add("jourj-intertitre");
        Label chiffres = new Label(PresentationSalle.resumeSalle(bord));
        chiffres.getStyleClass().add("jourj-chiffres");
        chiffres.setWrapText(true);
        contenu.getChildren().addAll(intertitreTables, chiffres, grilleTables());
        defilement.setContent(contenu);
        javafx.application.Platform.runLater(() -> defilement.setVvalue(position));
        Node gauche = colonneGauche();
        boolean apparition = gauche != null && racine.getLeft() == null;
        racine.setLeft(gauche);
        if (enSaisie == null && apparition) {
            Ui.apparition(gauche);
        }
    }

    /**
     * Colonne de gauche : saisie du score, ou l'interface « Lancer un match »
     * ({@link InterfaceLancementMatch}) quand elle est ouverte ; rien sinon (la
     * page prend toute la largeur).
     */
    private Node colonneGauche() {
        if (enSaisie != null) {
            return panneauSaisie(enSaisie);
        }
        return lancementOuvert ? colonneLancement() : null;
    }

    // ==================================================================
    // Trois cartes : Lancer un match, Annonces, Stratégie en place
    // ==================================================================

    /** Les trois cartes carrées, côte à côte (en colonne sur petite largeur). */
    private Node cartes() {
        Button lancer = Ui.boutonPrimaire(lancementOuvert ? "Fermer" : "_Lancer un match");
        lancer.setMnemonicParsing(true);
        lancer.setTooltip(new Tooltip("Alt+L · matchs proposés avec leur table et l'attente des joueurs, "
                + "« Proposer » et choix manuel du match et de la table."));
        VBox carteLancer = TableauDeBordJourJ.carte(Ui.icone(Icones.RAQUETTE, "icone"),
                PresentationTableauDeBordJourJ.CARTE_LANCER, PresentationTableauDeBordJourJ.valeurLancer(bord),
                PresentationTableauDeBordJourJ.detailLancer(bord), lancer, lancementOuvert, this::basculerLancement);

        boolean voixDispo = voix != null && voix.service().voixDisponible();
        Button annonces = Ui.boutonSecondaire(PANNEAU_ANNONCES.equals(panneau) ? "Fermer" : "Régler les annonces");
        VBox carteAnnonces = TableauDeBordJourJ.carte(Ui.icone(Icones.ANNONCE, "icone"),
                PresentationTableauDeBordJourJ.CARTE_ANNONCES,
                PresentationTableauDeBordJourJ.etatAnnonces(tournoi.getModeAnnonce()),
                PresentationTableauDeBordJourJ.detailAnnonces(tournoi.getModeAnnonce(), tournoi.getAnnoncesVocales(),
                        voixDispo, tournoi.getConvocationsSms().isEnvoiAutomatique()),
                annonces, PANNEAU_ANNONCES.equals(panneau), () -> basculerPanneau(PANNEAU_ANNONCES));

        Button strategie = Ui.boutonSecondaire(PANNEAU_STRATEGIE.equals(panneau) ? "Fermer" : "Changer de stratégie");
        VBox carteStrategie = TableauDeBordJourJ.carte(Ui.icone(Icones.STRATEGIE, "icone"),
                PresentationTableauDeBordJourJ.CARTE_STRATEGIE, PresentationTableauDeBordJourJ.valeurStrategie(tournoi),
                PresentationTableauDeBordJourJ.detailStrategie(tournoi), strategie, PANNEAU_STRATEGIE.equals(panneau),
                () -> basculerPanneau(PANNEAU_STRATEGIE));
        return TableauDeBordJourJ.cartes(List.of(carteLancer, carteAnnonces, carteStrategie));
    }

    /** Ouvre l'interface des annonces sous les cartes (avant {@link #build()}). */
    void ouvrirAnnonces() {
        panneau = PANNEAU_ANNONCES;
    }

    private void basculerLancement() {
        lancementOuvert = !lancementOuvert;
        if (lancementOuvert) {
            lancement.reinitialiser();
        }
        afficher();
    }

    private void basculerPanneau(String nom) {
        panneau = nom.equals(panneau) ? "" : nom;
        afficher();
    }

    /** Choix d'annonce des rencontres, mémorisé pour ce tournoi. */
    private Node menuAnnonce() {
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
        return annonce;
    }

    /**
     * Interface des annonces (sous les cartes) : annonce des rencontres, voix,
     * convocations par SMS, prononciation des noms, suivi sur téléphone.
     */
    private Node panneauAnnonces() {
        Button fermer = Ui.boutonLien("Fermer");
        fermer.setOnAction(e -> basculerPanneau(PANNEAU_ANNONCES));
        FlowPane reglages = new FlowPane(10, 8, menuAnnonce());
        reglages.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (voix != null) {
            reglages.getChildren().add(menuVoix());
        }
        if (ouvrirSms != null) {
            Button boutonSms = Ui.boutonSecondaire("Convocations par SMS…");
            boutonSms.setTooltip(new Tooltip("Réglages, numéros consentis, journal des envois."));
            boutonSms.setOnAction(e -> {
                arreterMinuterie();
                ouvrirSms.run();
            });
            reglages.getChildren().add(boutonSms);
        }
        if (ouvrirPrononciations != null) {
            Button prononciations = Ui.boutonSecondaire("Prononciation des noms…");
            prononciations.setOnAction(e -> {
                arreterMinuterie();
                ouvrirPrononciations.run();
            });
            reglages.getChildren().add(prononciations);
        }
        VBox carte = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte(PresentationTableauDeBordJourJ.CARTE_ANNONCES),
                fermer), Ui.aide("Annonce de chaque rencontre envoyée sur une table : voix de l'ordinateur, SMS aux "
                        + "joueurs, les deux, ou rien. Réglages mémorisés pour ce tournoi."), reglages);
        carte.getStyleClass().add("jourj-panneau");
        VBox panneauEntier = new VBox(16, carte);
        if (suiviTelephone != null) {
            panneauEntier.getChildren().add(suiviTelephone.get());
        }
        return panneauEntier;
    }

    /**
     * Sélecteur de stratégie à trois choix ({@link SelecteurStrategie}, le même
     * que sur la vue du tournoi) : le changement s'applique aux prochains matchs
     * proposés, jamais aux matchs déjà sur une table.
     */
    private Node panneauStrategie() {
        Button fermer = Ui.boutonLien("Fermer");
        fermer.setOnAction(e -> basculerPanneau(PANNEAU_STRATEGIE));
        Node selecteur = new SelecteurStrategie(tournoi, t -> enregistrer.run())
                .surChangement(s -> {
                    messageSucces = PresentationTableauDeBordJourJ.messageChangementStrategie(tournoi);
                    afficher();
                })
                .build();
        VBox carte = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte(PresentationTableauDeBordJourJ.CARTE_STRATEGIE),
                fermer), Ui.aide(PresentationTableauDeBordJourJ.MESSAGE_STRATEGIE), selecteur);
        carte.getStyleClass().add("jourj-panneau");
        return carte;
    }

    /** Réglages des annonces vocales (B017), mémorisés pour ce tournoi. */
    private Node menuVoix() {
        ReglagesAnnoncesVocales r = tournoi.getAnnoncesVocales();
        boolean dispo = voix.service().voixDisponible();
        MenuButton menu = new MenuButton(PresentationSalle.libelleVoix(r, dispo));
        menu.getStyleClass().add("jourj-annonce-choix");
        menu.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        menu.setDisable(ferme());
        menu.setTooltip(new Tooltip(dispo
                ? "Annonces vocales : l'ordinateur dit « Prénom Nom, club, contre …, table N ». Réglages mémorisés "
                        + "pour ce tournoi."
                : "La synthèse vocale n'est disponible que sous Windows : les annonces s'affichent à l'écran pour "
                        + "être lues au micro."));

        CheckMenuItem auto = new CheckMenuItem("Annonce automatique au lancement");
        auto.setSelected(r.isAnnonceAutomatique());
        auto.setOnAction(e -> reglerVoix(() -> r.setAnnonceAutomatique(auto.isSelected()), auto.isSelected()
                ? "Annonce vocale automatique : chaque match envoyé sur une table est annoncé (si l'annonce "
                        + "choisie comprend l'appel vocal)."
                : "Annonce vocale automatique coupée : bouton « Annoncer » sur chaque table."));
        CheckMenuItem contexte = new CheckMenuItem("Dire le tableau et le tour");
        contexte.setSelected(r.isAvecTableauEtTour());
        contexte.setOnAction(e -> reglerVoix(() -> r.setAvecTableauEtTour(contexte.isSelected()),
                contexte.isSelected() ? "Les annonces commencent par le tableau et le tour."
                        : "Les annonces ne disent plus le tableau ni le tour."));

        Menu repetitions = new Menu("Répétition");
        ToggleGroup gRep = new ToggleGroup();
        for (int n = 1; n <= ReglagesAnnoncesVocales.DIFFUSIONS_MAX; n++) {
            int valeur = n;
            String libelle = PresentationSalle.libelleDiffusions(n);
            repetitions.getItems().add(radio(libelle, gRep, r.getDiffusions() == n,
                    () -> reglerVoix(() -> r.setDiffusions(valeur), "Annonces : " + libelle.toLowerCase() + ".")));
        }
        Menu rappel = new Menu("Rappel si la table reste vide");
        ToggleGroup gRap = new ToggleGroup();
        for (int minutes : new int[] {0, 3, 5, 10}) {
            String libelle = PresentationSalle.libelleRappel(minutes);
            rappel.getItems().add(radio(libelle, gRap, r.getRappelMinutes() == minutes,
                    () -> reglerVoix(() -> r.setRappelMinutes(minutes), "Annonces : " + libelle.toLowerCase()
                            + (minutes > 0 ? " (« Joueurs à la table » sur la table l'annule)." : "."))));
        }
        Menu debit = new Menu("Débit");
        ToggleGroup gDeb = new ToggleGroup();
        for (int d : new int[] {-3, 0, 3}) {
            String libelle = PresentationSalle.libelleDebit(d);
            debit.getItems().add(radio(libelle, gDeb, Integer.signum(r.getDebit()) == Integer.signum(d),
                    () -> reglerVoix(() -> r.setDebit(d), "Débit de la voix : " + libelle.toLowerCase() + ".")));
        }
        Menu choixVoix = new Menu("Voix");
        ToggleGroup gVoix = new ToggleGroup();
        choixVoix.getItems().add(radio("Voix par défaut du système", gVoix, r.getVoix() == null,
                () -> reglerVoix(() -> r.setVoix(null), "Voix par défaut du système.")));
        for (String nom : voix.voixConnues()) {
            choixVoix.getItems().add(radio(nom, gVoix, nom.equals(r.getVoix()),
                    () -> reglerVoix(() -> r.setVoix(nom), "Voix choisie : " + nom + ".")));
        }
        menu.getItems().addAll(auto, contexte, new SeparatorMenuItem(), repetitions, rappel, debit, choixVoix);
        if (ouvrirPrononciations != null) {
            MenuItem prononciations = new MenuItem("Prononciation des noms…");
            prononciations.setOnAction(e -> ouvrirPrononciations.run());
            menu.getItems().addAll(new SeparatorMenuItem(), prononciations);
        }
        return menu;
    }

    private static RadioMenuItem radio(String libelle, ToggleGroup groupe, boolean choisi, Runnable action) {
        RadioMenuItem item = new RadioMenuItem(libelle);
        item.setToggleGroup(groupe);
        item.setSelected(choisi);
        item.setOnAction(e -> action.run());
        return item;
    }

    private void reglerVoix(Runnable changement, String message) {
        changement.run();
        apresAction(true, message);
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
        FlowPane grille = new FlowPane(12, 12);
        grille.getStyleClass().add("salle-grille");
        // Autant de colonnes que la largeur disponible le permet.
        grille.prefWrapLengthProperty().bind(defilement.widthProperty().subtract(64));
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
                afficher();
            });
            Button feuilleMatch = Ui.boutonLien("Feuille");
            feuilleMatch.setTooltip(new Tooltip("Imprimer la feuille de match de cette table."));
            feuilleMatch.setOnAction(e -> {
                ImpressionJourJ.Message msg = ImpressionJourJ.imprimer(
                        ImpressionJourJ.feuilleMatch(tournoi, m.match()), false);
                if (msg.erreur()) {
                    messageErreur = msg.texte();
                } else {
                    messageSucces = msg.texte();
                }
                afficher();
            });
            carte.getChildren().addAll(new VBox(0, tableau, tour), a, b, duree, terminer);
            HBox liens = Ui.ligne(8, feuilleMatch);
            if (voix != null) {
                Button annoncer = Ui.boutonLien("Annoncer");
                annoncer.setDisable(ferme());
                annoncer.setTooltip(new Tooltip("Annoncer (ou annoncer de nouveau) cette rencontre dans la salle."));
                annoncer.setOnAction(e -> annoncerTable(m.match(), c.numero()));
                liens.getChildren().add(annoncer);
                Optional<java.time.Instant> rappel = voix.service().rappelPrevu(m.match());
                if (rappel.isPresent()) {
                    Button presents = Ui.boutonLien("Joueurs à la table");
                    presents.setTooltip(new Tooltip(PresentationSalle.infoRappel(rappel.get(),
                            java.time.ZoneId.systemDefault())));
                    presents.setOnAction(e -> {
                        voix.service().oublier(m.match());
                        messageSucces = "Table " + c.numero() + " : joueurs présents, pas de rappel.";
                        afficher();
                    });
                    carte.getChildren().add(liens);
                    liens = Ui.ligne(8, presents);
                }
            }
            carte.getChildren().add(liens);
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
    // Interface « Lancer un match » (B055)
    // ==================================================================

    /** Interface « Lancer un match », recalculée par le cœur à chaque affichage. */
    private Node colonneLancement() {
        ListeLancements liste = salle.listeLancements(tournoi);
        VBox colonne = lancement.construire(liste, bord.getNombreDeTables(), ferme(),
                tournoi.imprimerFeuilleALEnvoi(), this::cleGlisser);
        return cote(colonne);
    }

    /** Donnée glissée d'un match vers une table libre (rang dans les matchs prêts), ou null. */
    private String cleGlisser(Match m) {
        List<MatchSalle> prets = bord.getPrets();
        for (int i = 0; i < prets.size(); i++) {
            if (prets.get(i).match() == m) {
                return PresentationSalle.cleGlisser(i);
            }
        }
        return null;
    }

    /** Colonne de gauche défilante, largeur fixe. */
    private Node cote(VBox colonne) {
        colonne.getStyleClass().add("salle-colonne");
        colonne.setPrefWidth(420);
        colonne.setMaxWidth(420);
        ScrollPane s = Ui.defilant(colonne);
        s.getStyleClass().add("salle-cote");
        s.setMinWidth(440);
        BorderPane.setMargin(s, new Insets(24, 0, 24, 24));
        return s;
    }

    /** Gestes de l'interface « Lancer un match », branchés sur la salle. */
    private final class ActionsLancement implements InterfaceLancementMatch.Actions {
        @Override
        public void lancer(Match match, int table) {
            affecter(match, table);
        }

        @Override
        public void proposer() {
            EcranSalle.this.proposer();
        }

        @Override
        public void fermer() {
            basculerLancement();
        }

        @Override
        public void feuilleALEnvoi(boolean imprimer) {
            tournoi.setImprimerFeuilleALEnvoi(imprimer);
            apresAction(true, imprimer
                    ? "La feuille de match sera imprimée à chaque envoi sur une table."
                    : "Les feuilles de match ne s'impriment plus à l'envoi (bouton « Feuille » sur chaque table).");
        }

        @Override
        public void rafraichir() {
            afficher();
        }

        @Override
        public void ouvrirTableau(String nom) {
            if (ouvrirTableau != null) {
                arreterMinuterie();
                ouvrirTableau.accept(nom);
            }
        }
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
                + "envoyés aux joueurs qui ont donné leur numéro. Appel vocal : l'ordinateur annonce la rencontre dans "
                + "la salle (menu « Voix », Windows) ; sans voix, l'annonce s'affiche ici pour être lue au micro."),
                choix, retenir);
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
                return lancementOuvert ? colonneLancement() : null;
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
        oublierAnnonce(m);
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
                    smsEnvoyes.append(voixApresLancement(l.match().match(), l.table(), mode));
                }
                annonces = annonceVocaleSeule(mode) ? List.of() : liste;
                List<Match> lances = new ArrayList<>();
                r.getLancements().forEach(l -> lances.add(l.match().match()));
                apresAction(true, PresentationSalle.messageProposition(r) + feuillesALEnvoi(lances) + smsEnvoyes);
                return;
            }
            apresAction(false, PresentationSalle.messageProposition(r));
        });
    }

    private void affecter(Match m, int table) {
        lancerAvecAnnonce(mode -> {
            String contexte = contexteDe(m);
            ResultatAffectationTable r = salle.affecter(tournoi, m, table);
            String smsEnvoyes = "";
            if (r.estReussi()) {
                annonces = annonceVocaleSeule(mode) ? List.of()
                        : List.of(AnnonceRencontre.preparer(mode, m, table, contexte));
                smsEnvoyes = smsApresLancement(m, table, mode) + voixApresLancement(m, table, mode);
            }
            String texte = PresentationSalle.message(r, bord.getNombreDeTables());
            if (r.estReussi()) {
                texte += feuillesALEnvoi(List.of(m));
            }
            apresAction(r.estReussi(), texte + smsEnvoyes);
        });
    }

    /**
     * B016 : si l'option est cochée, imprime les feuilles des matchs qui viennent
     * d'être envoyés ; rend la phrase à ajouter au message (vide sinon). Un
     * échec d'impression n'annule jamais l'envoi.
     */
    private String feuillesALEnvoi(List<Match> envoyes) {
        if (!tournoi.imprimerFeuilleALEnvoi() || envoyes.isEmpty()) {
            return "";
        }
        ImpressionJourJ.Message msg = ImpressionJourJ.imprimer(ImpressionJourJ.feuillesMatchs(tournoi, envoyes), true);
        return (msg.erreur() ? " Feuille non imprimée : " : " ") + msg.texte();
    }

    private void annulerAffectation(Match m) {
        ResultatAffectationTable r = salle.annulerAffectation(tournoi, m);
        if (r.estReussi()) {
            oublierAnnonce(m);
        }
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
            lancement.rafraichirAttentes(salle.maintenant());
            verifierRappels();
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
