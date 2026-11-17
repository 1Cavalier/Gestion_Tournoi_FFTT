package com.pingmanager.ui.ecran;

import com.pingmanager.core.GenerateurSsml;
import com.pingmanager.core.HistoriqueAnnonces;
import com.pingmanager.core.SyntheseVocale;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DiffuseurAnnonces;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Panneau « Annonces » du jour J (B056), ouvert par la carte du même nom à la
 * place de l'écran : le choix du mode en quatre options, puis un bloc « Voix »
 * et un bloc « SMS » (chacun avec son explication, ses réglages et son test ;
 * replié et atténué s'il ne sert pas avec le mode choisi), l'historique des
 * dernières annonces et le suivi sur téléphone.
 *
 * <p>Les réglages sont ceux d'avant (mêmes champs du tournoi, mêmes valeurs) :
 * chaque changement passe par l'écran hôte, qui enregistre et réaffiche. Mots :
 * {@link TextesPanneauxJourJ} ; choix d'affichage : {@link PresentationPanneauxJourJ}.
 * L'instance est gardée par l'écran hôte : l'état replié ou déplié survit aux
 * rafraîchissements.</p>
 */
final class PanneauAnnonces {

    private static final PseudoClass SELECTIONNEE = PseudoClass.getPseudoClass("selected");
    private static final int[] RAPPELS = {0, 3, 5, 10};
    private static final int[] DEBITS = {-3, 0, 3};

    /** Actions confiées à l'écran hôte (enregistrement, navigation). */
    interface Actions {
        /** Mémorise le mode ({@code null} : demander à chaque match), enregistre et réaffiche. */
        void choisirMode(ModeAnnonce mode);

        /** Applique un réglage, enregistre, affiche le message et réaffiche. */
        void regler(Runnable changement, String message);

        /** Réannonce une rencontre à la voix (bouton « Réannoncer »). */
        void reannoncer(Match match, int table);

        /** Réaffiche le panneau (après un dépliage), sans rien enregistrer. */
        void rafraichir();

        /** Ferme le panneau et revient au jour J. */
        void fermer();
    }

    private final Tournoi tournoi;
    private final DiffuseurAnnonces voix;
    private final Actions actions;
    private Runnable ouvrirSms;
    private Runnable ouvrirPrononciations;
    private Supplier<Node> suiviTelephone;
    private boolean ferme;

    private boolean voixDepliee;
    private boolean smsDeplie;

    PanneauAnnonces(Tournoi tournoi, DiffuseurAnnonces voix, Actions actions) {
        this.tournoi = tournoi;
        this.voix = voix;
        this.actions = actions;
    }

    PanneauAnnonces avecSms(Runnable ouvrir) {
        this.ouvrirSms = ouvrir;
        return this;
    }

    PanneauAnnonces avecPrononciations(Runnable ouvrir) {
        this.ouvrirPrononciations = ouvrir;
        return this;
    }

    PanneauAnnonces avecSuiviTelephone(Supplier<Node> carte) {
        this.suiviTelephone = carte;
        return this;
    }

    /** Tournoi annulé : réglages consultables mais figés. */
    PanneauAnnonces ferme(boolean ferme) {
        this.ferme = ferme;
        return this;
    }

    private boolean voixDisponible() {
        return voix != null && voix.service().voixDisponible();
    }

    /** @return le panneau entier : en-tête, mode, Voix, SMS, historique, suivi sur téléphone. */
    Node build() {
        VBox panneau = new VBox(20, enTete(), carteMode());
        Node blocVoix = blocVoix();
        Node blocSms = blocSms();
        panneau.getChildren().add(Ui.grilleAdaptative(2, 420, List.of(blocVoix, blocSms)));
        panneau.getChildren().add(carteHistorique());
        if (suiviTelephone != null) {
            panneau.getChildren().add(suiviTelephone.get());
        }
        panneau.getStyleClass().addAll("panneau-jourj", "panneau-annonces");
        return panneau;
    }

    // ------------------------------------------------------------------
    // En-tête
    // ------------------------------------------------------------------

    private Node enTete() {
        return enTete(TextesPanneauxJourJ.ANNONCES_TITRE, TextesPanneauxJourJ.ANNONCES_PHRASE, actions::fermer);
    }

    /** En-tête commun aux deux panneaux : titre, phrase, bouton « Fermer ». */
    static Node enTete(String titre, String phrase, Runnable fermer) {
        Button bouton = Ui.boutonSecondaire(TextesPanneauxJourJ.FERMER);
        bouton.setCancelButton(true);
        bouton.setOnAction(e -> fermer.run());
        bouton.setMinWidth(Region.USE_PREF_SIZE);
        Label t = Ui.titre(titre);
        Label p = Ui.label(phrase, "panneau-jourj-phrase");
        p.setWrapText(true);
        p.setMinHeight(Region.USE_PREF_SIZE);
        VBox textes = new VBox(4, t, p);
        HBox.setHgrow(textes, Priority.ALWAYS);
        HBox ligne = new HBox(16, textes, bouton);
        ligne.setAlignment(Pos.TOP_LEFT);
        ligne.getStyleClass().add("panneau-jourj-entete");
        return ligne;
    }

    // ------------------------------------------------------------------
    // Choix du mode
    // ------------------------------------------------------------------

    private Node carteMode() {
        List<Node> options = new ArrayList<>();
        for (PresentationPanneauxJourJ.OptionMode o : PresentationPanneauxJourJ.optionsMode(tournoi, voixDisponible())) {
            options.add(optionMode(o));
        }
        VBox carte = Ui.carte(Ui.titreCarte(TextesPanneauxJourJ.MODE_TITRE), Ui.grilleAdaptative(4, 200, options));
        if (tournoi.getModeAnnonce() == null) {
            carte.getChildren().add(Ui.encartInfo(TextesPanneauxJourJ.MODE_AUCUN_CHOISI));
        } else {
            Button demander = Ui.boutonLien(TextesPanneauxJourJ.MODE_DEMANDER);
            demander.setDisable(ferme);
            demander.setOnAction(e -> actions.choisirMode(null));
            carte.getChildren().add(demander);
        }
        carte.getStyleClass().add("panneau-annonces-modes");
        return carte;
    }

    /** Une option : carte cliquable et atteignable au clavier, annoncée comme bouton radio. */
    private Node optionMode(PresentationPanneauxJourJ.OptionMode o) {
        Label rond = new Label();
        rond.getStyleClass().add("panneau-option-rond");
        rond.setMinSize(18, 18);
        rond.setMaxSize(18, 18);
        Label nom = Ui.label(o.nom(), "panneau-option-nom");
        nom.setWrapText(true);
        nom.setMinHeight(Region.USE_PREF_SIZE);
        HBox entete = Ui.ligne(10, rond, nom);
        entete.setAlignment(Pos.CENTER_LEFT);
        Label phrase = texte(o.phrase(), "panneau-option-phrase");
        VBox carte = new VBox(8, entete, phrase);
        for (String r : o.remarques()) {
            carte.getChildren().add(texte(r, "panneau-option-remarque"));
        }
        Region pousse = new Region();
        VBox.setVgrow(pousse, Priority.ALWAYS);
        carte.getChildren().add(pousse);
        if (o.active()) {
            carte.getChildren().add(Ui.pastille(TextesPanneauxJourJ.MODE_ACTUEL, StatutPastille.INFO));
        }
        carte.getStyleClass().add("panneau-option");
        carte.pseudoClassStateChanged(SELECTIONNEE, o.active());
        carte.setMaxWidth(Double.MAX_VALUE);
        carte.setMaxHeight(Double.MAX_VALUE);
        carte.setAccessibleRole(AccessibleRole.RADIO_BUTTON);
        carte.setAccessibleText(o.nom() + ". " + o.phrase() + (o.active() ? " " + TextesPanneauxJourJ.MODE_ACTUEL : ""));
        if (!ferme) {
            carte.setFocusTraversable(true);
            carte.setOnMouseClicked(e -> choisir(o));
            carte.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    choisir(o);
                }
            });
        }
        return carte;
    }

    private void choisir(PresentationPanneauxJourJ.OptionMode o) {
        if (!o.active()) {
            actions.choisirMode(o.mode());
        }
    }

    // ------------------------------------------------------------------
    // Blocs Voix et SMS
    // ------------------------------------------------------------------

    /**
     * Cadre d'un bloc : titre, pastille d'état, explication ; le contenu suit
     * s'il est utilisé ou déplié. Un bloc inutilisé est atténué et replié, avec
     * un bouton pour préparer ses réglages quand même.
     */
    private VBox bloc(String titre, boolean utilise, boolean deplie, Runnable basculer, String explication,
                      Supplier<List<Node>> contenu) {
        Label pastille = Ui.pastille(utilise ? TextesPanneauxJourJ.BLOC_ACTIF : TextesPanneauxJourJ.BLOC_INACTIF,
                utilise ? StatutPastille.SUCCES : StatutPastille.NEUTRE);
        Label t = Ui.titreCarte(titre);
        t.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane entete = new FlowPane(10, 6, t, pastille);
        entete.setAlignment(Pos.CENTER_LEFT);
        VBox carte = Ui.carte(entete, texte(explication, "panneau-bloc-explication"));
        if (utilise || deplie) {
            carte.getChildren().addAll(contenu.get());
        }
        if (!utilise) {
            Button b = Ui.boutonLien(deplie ? TextesPanneauxJourJ.BLOC_REPLIER : TextesPanneauxJourJ.BLOC_DEPLIER);
            b.setOnAction(e -> basculer.run());
            carte.getChildren().add(b);
            carte.getStyleClass().add("panneau-bloc-inactif");
        }
        carte.getStyleClass().add("panneau-bloc");
        carte.setMaxHeight(Double.MAX_VALUE);
        return carte;
    }

    private Node blocVoix() {
        boolean utilise = PresentationPanneauxJourJ.voixUtilisee(tournoi.getModeAnnonce());
        VBox carte = bloc(TextesPanneauxJourJ.VOIX_TITRE, utilise, voixDepliee, () -> {
            voixDepliee = !voixDepliee;
            actions.rafraichir();
        }, TextesPanneauxJourJ.VOIX_EXPLICATION, this::reglagesVoix);
        carte.getStyleClass().add("panneau-bloc-voix");
        return carte;
    }

    private List<Node> reglagesVoix() {
        List<Node> noeuds = new ArrayList<>();
        if (!voixDisponible()) {
            noeuds.add(Ui.encartInfo(TextesPanneauxJourJ.VOIX_INDISPONIBLE));
        }
        ReglagesAnnoncesVocales r = tournoi.getAnnoncesVocales();
        CheckBox auto = caseACocher(TextesPanneauxJourJ.VOIX_AUTOMATIQUE, r.isAnnonceAutomatique(),
                v -> actions.regler(() -> r.setAnnonceAutomatique(v), TextesPanneauxJourJ.voixAutomatique(v)));
        CheckBox contexte = caseACocher(TextesPanneauxJourJ.VOIX_TABLEAU_TOUR, r.isAvecTableauEtTour(),
                v -> actions.regler(() -> r.setAvecTableauEtTour(v), TextesPanneauxJourJ.voixTableauTour(v)));
        noeuds.add(new VBox(2, auto, Ui.aide(TextesPanneauxJourJ.VOIX_AUTOMATIQUE_AIDE)));
        noeuds.add(contexte);

        List<String> repetitions = new ArrayList<>();
        for (int n = 1; n <= ReglagesAnnoncesVocales.DIFFUSIONS_MAX; n++) {
            repetitions.add(PresentationSalle.libelleDiffusions(n));
        }
        ComboBox<String> repetition = liste(repetitions, r.getDiffusions() - 1, i -> actions.regler(
                () -> r.setDiffusions(i + 1), TextesPanneauxJourJ.voixReglage(repetitions.get(i))));

        List<Integer> minutes = new ArrayList<>();
        for (int m : RAPPELS) {
            minutes.add(m);
        }
        if (!minutes.contains(r.getRappelMinutes())) {
            minutes.add(r.getRappelMinutes());
        }
        List<String> rappels = new ArrayList<>();
        minutes.forEach(m -> rappels.add(PresentationSalle.libelleRappel(m)));
        ComboBox<String> rappel = liste(rappels, minutes.indexOf(r.getRappelMinutes()), i -> actions.regler(
                () -> r.setRappelMinutes(minutes.get(i)), TextesPanneauxJourJ.voixReglage(rappels.get(i))));

        List<String> debits = new ArrayList<>();
        int debitActuel = 1;
        for (int i = 0; i < DEBITS.length; i++) {
            debits.add(PresentationSalle.libelleDebit(DEBITS[i]));
            if (Integer.signum(DEBITS[i]) == Integer.signum(r.getDebit())) {
                debitActuel = i;
            }
        }
        ComboBox<String> debit = liste(debits, debitActuel, i -> actions.regler(
                () -> r.setDebit(DEBITS[i]), TextesPanneauxJourJ.voixReglage(TextesPanneauxJourJ.VOIX_DEBIT + " "
                        + debits.get(i).toLowerCase())));

        List<String> noms = new ArrayList<>();
        noms.add(TextesPanneauxJourJ.VOIX_PAR_DEFAUT);
        if (voix != null) {
            noms.addAll(voix.voixConnues());
        }
        if (r.getVoix() != null && !noms.contains(r.getVoix())) {
            noms.add(r.getVoix());
        }
        ComboBox<String> choixVoix = liste(noms, r.getVoix() == null ? 0 : noms.indexOf(r.getVoix()),
                i -> actions.regler(() -> r.setVoix(i == 0 ? null : noms.get(i)),
                        TextesPanneauxJourJ.voixReglage(noms.get(i))));

        noeuds.add(Ui.grilleAdaptative(2, 180, List.of(
                Ui.champEtiquete(TextesPanneauxJourJ.VOIX_REPETITION, repetition),
                Ui.champEtiquete(TextesPanneauxJourJ.VOIX_RAPPEL, rappel),
                Ui.champEtiquete(TextesPanneauxJourJ.VOIX_DEBIT, debit),
                Ui.champEtiquete(TextesPanneauxJourJ.VOIX_CHOIX, choixVoix))));

        Label message = Ui.message();
        Button tester = Ui.boutonSecondaire(TextesPanneauxJourJ.VOIX_TESTER);
        tester.setOnAction(e -> testerVoix(tester, message));
        FlowPane boutons = new FlowPane(8, 8, tester);
        if (ouvrirPrononciations != null) {
            Button prononciations = Ui.boutonLien(TextesPanneauxJourJ.VOIX_PRONONCIATION);
            prononciations.setOnAction(e -> ouvrirPrononciations.run());
            boutons.getChildren().add(prononciations);
        }
        noeuds.add(boutons);
        noeuds.add(message);
        for (javafx.scene.control.Control c : List.of(auto, contexte, repetition, rappel, debit, choixVoix)) {
            c.setDisable(ferme);
        }
        return noeuds;
    }

    private void testerVoix(Button tester, Label message) {
        if (!voixDisponible()) {
            Ui.erreur(message, PresentationPrononciation.VOIX_INDISPONIBLE);
            return;
        }
        ReglagesAnnoncesVocales r = tournoi.getAnnoncesVocales();
        tester.setDisable(true);
        Ui.succes(message, TextesPanneauxJourJ.VOIX_TEST_EN_COURS);
        voix.ecouter(List.of(new GenerateurSsml.Morceau(TextesPanneauxJourJ.VOIX_TEST_PHRASE, null)),
                new SyntheseVocale.ParametresVoix(r.getVoix(), r.getDebit()), res -> Platform.runLater(() -> {
                    tester.setDisable(false);
                    String m = PresentationPrononciation.messageEcoute(res);
                    if (m.isEmpty()) {
                        Ui.cacher(message);
                    } else {
                        Ui.erreur(message, m);
                    }
                }));
    }

    private Node blocSms() {
        boolean utilise = PresentationPanneauxJourJ.smsUtilise(tournoi.getModeAnnonce());
        VBox carte = bloc(TextesPanneauxJourJ.SMS_TITRE, utilise, smsDeplie, () -> {
            smsDeplie = !smsDeplie;
            actions.rafraichir();
        }, TextesPanneauxJourJ.SMS_EXPLICATION, this::reglagesSms);
        // Bandeau de simulation toujours visible, juste sous le titre.
        Label simulation = Ui.encartInfo(TextesPanneauxJourJ.SMS_SIMULATION);
        simulation.getStyleClass().add("panneau-sms-simulation");
        carte.getChildren().add(1, simulation);
        carte.getStyleClass().add("panneau-bloc-sms");
        return carte;
    }

    private List<Node> reglagesSms() {
        List<Node> noeuds = new ArrayList<>();
        ConvocationsSms c = tournoi.getConvocationsSms();
        CheckBox auto = caseACocher(TextesPanneauxJourJ.SMS_AUTOMATIQUE, c.isEnvoiAutomatique(),
                v -> actions.regler(() -> c.setEnvoiAutomatique(v), TextesPanneauxJourJ.smsAutomatique(v)));
        auto.setDisable(ferme);
        noeuds.add(auto);
        noeuds.add(ligneValeur(TextesPanneauxJourJ.SMS_EXPEDITEUR,
                com.pingmanager.core.ServiceConvocationsSms.expediteur(tournoi)));
        noeuds.add(ligneValeur(TextesPanneauxJourJ.SMS_NUMEROS,
                TextesPanneauxJourJ.nombreNumeros(c.getContacts().size())));
        noeuds.add(ligneValeur(TextesPanneauxJourJ.SMS_ENVOIS,
                TextesPanneauxJourJ.envois(c.nombreEnvoyes(), c.getLimiteEnvois())));

        VBox exemple = new VBox(4, Ui.aide(TextesPanneauxJourJ.SMS_EXEMPLE),
                texte(PresentationPanneauxJourJ.exempleSms(), "panneau-sms-exemple"));
        exemple.getStyleClass().add("panneau-sms-exemple-cadre");
        Ui.cacher(exemple);
        Button tester = Ui.boutonSecondaire(TextesPanneauxJourJ.SMS_TESTER);
        tester.setOnAction(e -> Ui.afficherSi(exemple, !exemple.isVisible()));
        FlowPane boutons = new FlowPane(8, 8, tester);
        if (ouvrirSms != null) {
            Button journal = Ui.boutonLien(TextesPanneauxJourJ.SMS_JOURNAL);
            journal.setOnAction(e -> ouvrirSms.run());
            boutons.getChildren().add(journal);
        }
        noeuds.add(boutons);
        noeuds.add(exemple);
        return noeuds;
    }

    // ------------------------------------------------------------------
    // Historique
    // ------------------------------------------------------------------

    private Node carteHistorique() {
        VBox lignes = new VBox(0);
        List<HistoriqueAnnonces.Entree> entrees = voix == null ? List.of() : voix.historique().dernieres();
        if (entrees.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact(TextesPanneauxJourJ.HISTORIQUE_VIDE));
        }
        boolean premiere = true;
        for (HistoriqueAnnonces.Entree e : entrees) {
            Label heure = Ui.label(PresentationPanneauxJourJ.heure(e.instant(), ZoneId.systemDefault()),
                    "panneau-historique-heure");
            heure.setMinWidth(Region.USE_PREF_SIZE);
            Label texte = texte(e.annonce().texte(), "panneau-historique-texte");
            HBox.setHgrow(texte, Priority.ALWAYS);
            texte.setMaxWidth(Double.MAX_VALUE);
            Label etat = Ui.pastille(e.dite() ? TextesPanneauxJourJ.HISTORIQUE_DITE
                    : TextesPanneauxJourJ.HISTORIQUE_A_LIRE, e.dite() ? StatutPastille.SUCCES : StatutPastille.INFO);
            etat.setMinWidth(Region.USE_PREF_SIZE);
            Button reannoncer = Ui.boutonSecondaire(TextesPanneauxJourJ.HISTORIQUE_REANNONCER);
            reannoncer.setMinWidth(Region.USE_PREF_SIZE);
            reannoncer.setDisable(ferme);
            reannoncer.setOnAction(ev -> actions.reannoncer(e.annonce().match(), e.annonce().table()));
            HBox ligne = new HBox(12, heure, texte, etat, reannoncer);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("panneau-historique-ligne");
            if (premiere) {
                ligne.getStyleClass().add("panneau-historique-premiere");
                premiere = false;
            }
            lignes.getChildren().add(ligne);
        }
        VBox carte = Ui.carte(Ui.titreCarte(TextesPanneauxJourJ.HISTORIQUE_TITRE),
                Ui.aide(TextesPanneauxJourJ.HISTORIQUE_AIDE), lignes);
        carte.getStyleClass().add("panneau-historique");
        return carte;
    }

    // ------------------------------------------------------------------
    // Petites briques
    // ------------------------------------------------------------------

    static Label texte(String s, String classe) {
        Label l = Ui.label(s, classe);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static CheckBox caseACocher(String libelle, boolean cochee, Consumer<Boolean> action) {
        CheckBox c = new CheckBox(libelle);
        c.setWrapText(true);
        c.setSelected(cochee);
        c.setOnAction(e -> action.accept(c.isSelected()));
        return c;
    }

    private static ComboBox<String> liste(List<String> valeurs, int choisi, Consumer<Integer> action) {
        ComboBox<String> c = new ComboBox<>();
        c.getItems().setAll(valeurs);
        if (choisi >= 0 && choisi < valeurs.size()) {
            c.getSelectionModel().select(choisi);
        }
        c.setMaxWidth(Double.MAX_VALUE);
        c.setOnAction(e -> {
            int i = c.getSelectionModel().getSelectedIndex();
            if (i >= 0 && i != choisi) {
                action.accept(i);
            }
        });
        return c;
    }

    private static Node ligneValeur(String libelle, String valeur) {
        return texte(libelle + " : " + valeur, "panneau-valeur");
    }
}
