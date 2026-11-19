package com.pingmanager.ui.ecran;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.diffusion.DiffusionSuivi;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.print.PrinterJob;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;

/**
 * Page « Jour J » d'un tournoi (Gestion des tournois → tournoi → « Jour J »).
 *
 * <p>Écran principal unique du juge-arbitre ({@link EcranSalle}) : bandeau,
 * trois cartes, liste des tableaux, salle. Chaque tableau a sa ligne avec la
 * frise des six étapes (étape calculée par
 * {@link com.pingmanager.core.CalculEtapeTableau}) et les actions de l'étape
 * courante, qui ouvrent les vues secondaires :</p>
 * <ul>
 *   <li>ouvrir le pointage (confirmation sous la ligne) ;</li>
 *   <li>pointer, clôturer, puis lancer le tirage au sort
 *       ({@link EcranPointageTableau}) ;</li>
 *   <li>suivre les poules et saisir les scores ({@link EcranPoulesTableau}) ;</li>
 *   <li>tirer le tableau final et suivre l'arbre ({@link VuePhaseFinale}).</li>
 * </ul>
 *
 * <p>Le tournoi est relu depuis son fichier à chaque retour sur l'écran principal ; chaque
 * action est enregistrée aussitôt.</p>
 */
final class EcranJourJ {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final String libelleRetour;
    private Tournoi tournoi;
    /** Journée montrée dans le bandeau (indice), ou null : la journée du jour. */
    private Integer jourAffiche;

    private final BorderPane racine = new BorderPane();
    /** Rouvrir l'interface des annonces au prochain affichage (après un geste du suivi sur téléphone). */
    /** Panneau à rouvrir au prochain affichage : « annonces », « strategie », « lancement » ou vide. */
    private String rouvrir = "";
    /** B021 : message du dernier démarrage du suivi sur téléphone (refus), vide sinon. */
    private String messageTelephone = "";

    EcranJourJ(ContexteApplication contexte, Path fichier, Runnable onRetour) {
        this(contexte, fichier, onRetour, "← Vue d'ensemble du tournoi");
    }

    EcranJourJ(ContexteApplication contexte, Path fichier, Runnable onRetour, String libelleRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.libelleRetour = libelleRetour;
    }

    /**
     * Le jour J est-il proposé pour ce tournoi ? Oui s'il est publié, importé (préparé
     * hors de PingManager), ou si un tableau a déjà quitté la phase d'inscription ;
     * jamais pour un tournoi annulé ou sans tableau.
     */
    static boolean estPropose(Tournoi t) {
        if (t.getStatut() == StatutTournoi.ANNULE || t.getTableaux().isEmpty()) {
            return false;
        }
        return t.getStatut() == StatutTournoi.PUBLIE
                || t.getModePreparation() == com.pingmanager.model.ModePreparation.IMPORT
                || t.getTableaux().stream().anyMatch(x -> x.getStatut() != StatutTableau.INSCRIPTION);
    }

    /** Carte d'accès au jour J, posée dans la vue d'ensemble d'un tournoi. */
    static Node carteAcces(Tournoi t, Runnable ouvrir) {
        int pointage = 0;
        int lances = 0;
        for (Tableau x : t.getTableaux()) {
            if (x.getStatut() == StatutTableau.POINTAGE) {
                pointage++;
            } else if (x.getStatut() == StatutTableau.LANCE || x.getStatut() == StatutTableau.PHASE_FINALE) {
                lances++;
            }
        }
        Button ouvrirJourJ = Ui.boutonPrimaire("Ouvrir le jour J");
        ouvrirJourJ.setOnAction(e -> ouvrir.run());
        String etat = pointage == 0 && lances == 0
                ? "Aucun tableau en pointage pour l'instant."
                : pointage + (pointage > 1 ? " tableaux en pointage" : " tableau en pointage") + " · " + lances
                        + (lances > 1 ? " tableaux lancés." : " tableau lancé.");
        return Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Jour J"), ouvrirJourJ), Ui.discret(etat),
                Ui.aide("Pointage tableau par tableau, liste d'attente et repêchage, tirage au sort des poules, "
                        + "saisie des scores et classement des poules en direct."));
    }

    Node build() {
        afficherListe();
        // B021 : quitter le jour J (la page sort de la fenêtre) arrête le suivi sur téléphone.
        racine.sceneProperty().addListener((obs, avant, apres) -> {
            if (avant != null && apres == null) {
                contexte.diffusionSuivi().arreter();
            }
        });
        return racine;
    }

    private void enregistrer() {
        contexte.enregistrerTournoi(tournoi, fichier);
        publierSuivi();
    }

    /** B021 : nouvelle photographie pour les téléphones (fil JavaFX ; sans effet si le suivi est fermé). */
    private void publierSuivi() {
        contexte.diffusionSuivi().publier(tournoi);
    }

    // ------------------------------------------------------------------
    // Écran principal : matchs proposés, tables, avancement des tableaux
    // ------------------------------------------------------------------

    /**
     * Écran principal du juge-arbitre (B011), relu depuis le fichier : tout se
     * fait sur cette page (envoi sur une table, saisie du score, match suivant) ;
     * les écrans de pointage, de poules et l'arbre s'ouvrent depuis le bandeau
     * « Où en sont les tableaux » et y reviennent.
     */
    private void afficherListe() {
        tournoi = contexte.chargerTournoi(fichier);
        publierSuivi();
        // B019 : numéros de portable effacés dès le lendemain du dernier jour.
        if (contexte.convocationsSms().effacerSiTermine(tournoi, java.time.LocalDateTime.now()) > 0) {
            enregistrer();
        }
        EcranSalle principal = new EcranSalle(tournoi, this::enregistrer, onRetour, libelleRetour, "Jour J",
                this::bandeauTableaux, Horloge.systeme()).surOuvrirTableau(this::ouvrirTableau)
                .avecSms(contexte.convocationsSms(), () -> depuisAnnonces(this::afficherSms))
                .avecAnnoncesVocales(contexte.annoncesVocales())
                .avecPrononciations(() -> depuisAnnonces(this::afficherPrononciations))
                .avecSuiviTelephone(this::carteTelephone)
                .avecComparaisonStrategies(this::afficherComparaison);
        if ("annonces".equals(rouvrir)) {
            principal.ouvrirAnnonces();
        } else if ("strategie".equals(rouvrir)) {
            principal.ouvrirStrategie();
        } else if ("lancement".equals(rouvrir)) {
            principal.ouvrirLancement();
        }
        rouvrir = "";
        racine.setCenter(principal.build());
    }

    /** Écran ouvert depuis le panneau « Annonces » : son retour rouvre le panneau. */
    private void depuisAnnonces(Runnable ouvrir) {
        rouvrir = "annonces";
        ouvrir.run();
    }

    /**
     * B056 : comparaison chiffrée des stratégies (écran des prévisions, calculs
     * inchangés) ; le retour rouvre le panneau « Stratégie ».
     */
    private void afficherComparaison() {
        racine.setCenter(new EcranPrevisions(contexte, fichier, () -> {
            rouvrir = "strategie";
            afficherListe();
        }).build());
    }

    /** Inscription sur place à un tableau qui n'a pas commencé ; retour à l'écran principal. */
    private void afficherInscriptionSurPlace() {
        racine.setCenter(new EcranInscriptionSurPlace(contexte, tournoi, this::enregistrer, this::afficherListe).build());
    }

    /** B018 : prononciation des noms des joueurs et des clubs ; retour à l'écran principal. */
    private void afficherPrononciations() {
        racine.setCenter(new EcranPrononciations(contexte, tournoi, this::afficherListe).build());
    }

    /** Convocations par SMS (B019) : réglages, numéros consentis, journal ; retour à l'écran principal. */
    private void afficherSms() {
        racine.setCenter(new EcranConvocationsSms(tournoi, contexte.convocationsSms(), contexte.smsSimule(),
                this::enregistrer, this::afficherListe, Horloge.systeme()).build());
    }

    /** Ouvre les poules ou l'arbre d'un tableau désigné par son nom (depuis « pourquoi »). */
    private void ouvrirTableau(String nom) {
        for (PresentationAvancement.Journee j : PresentationAvancement.journees(tournoi)) {
            for (PresentationAvancement.Ligne l : j.lignes()) {
                if (l.tableau().getNom().equals(nom)) {
                    if (l.vue() == PresentationAvancement.Detail.POULES) {
                        afficherPoules(l.tableau(), false);
                    } else {
                        afficherTableauFinal(l.tableau());
                    }
                    return;
                }
            }
        }
        afficherListe();
    }

    /**
     * Liste des tableaux : une ligne par tableau avec sa frise en six étapes
     * (étape calculée dans le cœur, {@link com.pingmanager.core.CalculEtapeTableau})
     * et les actions de l'étape courante ; un clic sur la ligne ouvre l'action
     * principale. Reconstruite par l'écran principal après chaque action, donc
     * toujours à jour.
     */
    private Node bandeauTableaux() {
        VBox lignes = new VBox(0);
        if (tournoi.getTableaux().isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact(
                    "Aucun tableau dans ce tournoi. Créez-les dans « Tableaux et inscriptions »."));
        }
        List<PresentationAvancement.Journee> journees = PresentationAvancement.journees(tournoi);
        boolean jours = PresentationAvancement.afficherJours(journees);
        java.time.LocalDate aujourdhui = java.time.LocalDate.now();
        // B037 : la journée du jour par défaut, « jour suivant » pour voir la suite.
        int index = jourAffiche == null
                ? PresentationAvancement.indexJourParDefaut(journees, aujourdhui)
                : PresentationAvancement.borner(jourAffiche, journees);
        boolean premiere = true;
        for (int i = 0; i < journees.size(); i++) {
            if (jours && i != index) {
                continue;
            }
            for (PresentationAvancement.Ligne l : journees.get(i).lignes()) {
                lignes.getChildren().add(ligneTableau(PresentationTableauDeBordJourJ.ligne(l.tableau()), premiere));
                premiere = false;
            }
        }
        javafx.scene.layout.FlowPane actions = new javafx.scene.layout.FlowPane(8, 8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        if (jours) {
            Label jour = Ui.discret(PresentationAvancement.titreJour(journees.get(index), aujourdhui));
            jour.getStyleClass().add("avancement-jour");
            Button precedent = Ui.boutonSecondaire("← Jour précédent");
            precedent.setDisable(index == 0);
            precedent.setOnAction(e -> changerJour(index - 1));
            Button suivant = Ui.boutonSecondaire("Jour suivant →");
            suivant.setDisable(index >= journees.size() - 1);
            suivant.setOnAction(e -> changerJour(index + 1));
            precedent.getStyleClass().add("avancement-action");
            suivant.getStyleClass().add("avancement-action");
            actions.getChildren().addAll(jour, precedent, suivant);
        }
        Button surPlace = Ui.boutonSecondaire("Inscrire sur place…");
        surPlace.getStyleClass().add("avancement-action");
        surPlace.setTooltip(new Tooltip("Inscrire un joueur, par exemple éliminé d'un tableau, à un tableau "
                + "qui n'a pas encore commencé."));
        surPlace.setOnAction(e -> afficherInscriptionSurPlace());
        actions.getChildren().add(surPlace);
        // B052 : pas d'inscription avant la publication (ancien fichier), raison affichée.
        if (PresentationInscriptionsClub.avantPublication(tournoi)) {
            surPlace.setDisable(true);
            actions.getChildren().add(0, VueInscriptionsClub.raisonAvantPublication());
        }
        Label titreTableaux = Ui.titreCarte(PresentationTableauDeBordJourJ.TITRE_TABLEAUX);
        titreTableaux.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox gaucheEntete = Ui.ligne(10, titreTableaux);
        actions.setAlignment(jours ? javafx.geometry.Pos.CENTER_LEFT : javafx.geometry.Pos.CENTER_RIGHT);
        Node entete = TableauDeBordJourJ.ligneAdaptative(gaucheEntete, actions, 640);
        VBox carte = Ui.carte(entete, lignes);
        carte.getStyleClass().addAll("avancement", "jourj-tableaux");
        if (tournoi.getStatut() == StatutTournoi.ANNULE) {
            carte.getChildren().add(Ui.encartInfo("Ce tournoi est annulé : le pointage et les tirages sont fermés."));
        }
        // Reconstruit à chaque rafraîchissement de l'écran principal : la photographie suit.
        publierSuivi();
        return carte;
    }

    // ------------------------------------------------------------------
    // B021 : suivi sur téléphone (serveur web local en lecture seule)
    // ------------------------------------------------------------------

    /** Carte « Suivi sur téléphone » : port, ouverture, adresse en grand, arrêt. */
    private Node carteTelephone() {
        DiffusionSuivi d = contexte.diffusionSuivi();
        VBox carte;
        if (d.estDemarre()) {
            Label adresse = Ui.label(d.url(), "suivi-telephone-adresse");
            Button grand = Ui.boutonSecondaire("Afficher en grand et imprimer");
            grand.setOnAction(e -> afficherAdresseEnGrand());
            Button arreter = Ui.boutonSecondaire("Arrêter le suivi");
            arreter.setOnAction(e -> {
                d.arreter();
                messageTelephone = "";
                rouvrir = "annonces";
                afficherListe();
            });
            carte = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Suivi sur téléphone"), grand, arreter),
                    Ui.discret("Ouvert : joueurs et public, reliés au Wi-Fi de la salle, ouvrent cette adresse."),
                    adresse,
                    Ui.aide("Consultation seule : poules, matchs en cours et à venir avec la table, tableaux finaux "
                            + "et « Ma prochaine partie ». Seuls le nom, le club et les points des joueurs sont "
                            + "visibles. Le suivi s'arrête en quittant le jour J."));
        } else {
            TextField port = Ui.champ("Port");
            port.setText(String.valueOf(d.port()));
            port.setPrefColumnCount(6);
            Button ouvrir = Ui.boutonPrimaire("Ouvrir le suivi sur téléphone");
            Runnable demarrer = () -> {
                int p = DiffusionSuivi.lirePort(port.getText());
                DiffusionSuivi.ResultatDemarrage r = d.demarrer(p, tournoi);
                messageTelephone = r.estDemarre() ? "" : r.message();
                rouvrir = "annonces";
                afficherListe();
            };
            ouvrir.setOnAction(e -> demarrer.run());
            port.setOnAction(e -> demarrer.run());
            carte = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Suivi sur téléphone"), ouvrir),
                    Ui.discret("Les joueurs et le public suivent le tournoi sur leur téléphone, sans application, "
                            + "par le Wi-Fi de la salle."),
                    Ui.ligne(8, Ui.discret("Port"), port));
            if (!messageTelephone.isEmpty()) {
                carte.getChildren().add(Ui.encartInfo(messageTelephone));
            }
        }
        carte.getStyleClass().add("suivi-telephone");
        return carte;
    }

    /** Adresse du suivi en grand, à montrer ou à imprimer pour l'afficher dans la salle. */
    private void afficherAdresseEnGrand() {
        DiffusionSuivi d = contexte.diffusionSuivi();
        if (!d.estDemarre()) {
            afficherListe();
            return;
        }
        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> afficherListe());
        Button imprimer = Ui.boutonPrimaire("Imprimer");
        Label etat = Ui.discret("");
        Node affiche = afficheTelephone(d.url());
        imprimer.setOnAction(e -> etat.setText(imprimer(affiche)));
        VBox page = new VBox(16, Ui.ligne(10, retour, Ui.espace(), etat, imprimer), affiche);
        page.getStyleClass().add("suivi-telephone-page");
        racine.setCenter(page);
    }

    /** L'affiche elle-même : adresse en très grand et mode d'emploi. */
    private Node afficheTelephone(String url) {
        Label titre = Ui.label("Suivez le tournoi sur votre téléphone", "suivi-telephone-affiche-titre");
        Label nom = Ui.label(tournoi.getNom(), "suivi-telephone-affiche-tournoi");
        Label etape1 = Ui.label("1. Connectez-vous au Wi-Fi de la salle.", "suivi-telephone-affiche-etape");
        Label etape2 = Ui.label("2. Ouvrez cette adresse dans votre navigateur :", "suivi-telephone-affiche-etape");
        Label adresse = Ui.label(url, "suivi-telephone-affiche-adresse");
        adresse.setWrapText(true);
        Label aide = Ui.label("Poules, matchs en cours avec la table, tableaux finaux et « Ma prochaine partie » "
                + "(recherche par nom ou licence).", "suivi-telephone-affiche-aide");
        aide.setWrapText(true);
        VBox affiche = new VBox(18, titre, nom, etape1, etape2, adresse, aide);
        affiche.getStyleClass().add("suivi-telephone-affiche");
        return affiche;
    }

    /** Imprime l'affiche ; renvoie un message court pour le juge-arbitre. */
    private String imprimer(Node affiche) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return "Aucune imprimante disponible.";
        }
        if (!job.showPrintDialog(racine.getScene() == null ? null : racine.getScene().getWindow())) {
            return "";
        }
        // Réduite si besoin pour tenir dans la zone imprimable (l'affiche à l'écran n'est pas modifiée).
        javafx.print.PageLayout mise = job.getJobSettings().getPageLayout();
        double largeur = affiche.getBoundsInParent().getWidth();
        double hauteur = affiche.getBoundsInParent().getHeight();
        double echelle = Math.min(1, Math.min(mise.getPrintableWidth() / Math.max(1, largeur),
                mise.getPrintableHeight() / Math.max(1, hauteur)));
        javafx.scene.transform.Scale reduction = new javafx.scene.transform.Scale(echelle, echelle);
        affiche.getTransforms().add(reduction);
        boolean ok;
        try {
            ok = job.printPage(affiche) && job.endJob();
            if (!ok) {
                job.cancelJob();
            }
        } finally {
            affiche.getTransforms().remove(reduction);
        }
        return ok ? "Affiche envoyée à l'imprimante." : "L'impression a échoué.";
    }

    private void changerJour(int index) {
        jourAffiche = index;
        afficherListe();
    }

    /**
     * Une ligne de tableau : nom, pastille d'étape, parties en jeu et actions de
     * l'étape ; la frise des six étapes ; l'avancement de l'étape et sa jauge.
     */
    private Node ligneTableau(PresentationTableauDeBordJourJ.LigneTableau l, boolean premiere) {
        Tableau t = l.tableau();
        PresentationAvancement.Ligne av = l.avancement();
        Button nom = Ui.boutonLien(t.getNom());
        nom.getStyleClass().add("avancement-nom");
        nom.setTooltip(new Tooltip(PresentationJourJ.horaires(t)));
        Label etape = Ui.pastille(l.libelleEtape(), StatutPastille.INFO);
        etape.getStyleClass().add("avancement-etape");
        javafx.scene.layout.FlowPane titre = new javafx.scene.layout.FlowPane(10, 6, nom, etape);
        titre.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        String enJeu = PresentationAvancement.libelleEnJeu(av.enJeu());
        if (!enJeu.isEmpty()) {
            titre.getChildren().add(Ui.pastille(enJeu, StatutPastille.NEUTRE));
        }
        titre.setPrefWrapLength(420);

        boolean ferme = tournoi.getStatut() == StatutTournoi.ANNULE;
        VBox bloc = new VBox(10);
        HBox boutons = Ui.ligne(8);
        boutons.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        for (PresentationTableauDeBordJourJ.Action a : l.actions()) {
            Button b = a.principale() ? Ui.boutonPrimaire(a.libelle()) : Ui.boutonSecondaire(a.libelle());
            b.getStyleClass().add("avancement-action");
            b.setDisable(ferme && (a.vue() == PresentationAvancement.Detail.OUVRIR_POINTAGE
                    || l.etape().etape() == com.pingmanager.core.EtapeTableau.POINTAGE_FERME
                    || "Tirer le tableau final".equals(a.libelle())));
            b.setOnAction(e -> ouvrirDetail(t, a.vue(), bloc));
            boutons.getChildren().add(b);
        }
        Node haut = TableauDeBordJourJ.ligneAdaptative(titre, boutons, 560);

        Label detail = new Label(av.detail());
        detail.getStyleClass().add("avancement-detail");
        detail.setWrapText(true);
        detail.setMinWidth(0);
        HBox.setHgrow(detail, Priority.ALWAYS);
        detail.setMaxWidth(Double.MAX_VALUE);
        ProgressBar jauge = new ProgressBar(Math.max(0, av.jauge()));
        jauge.getStyleClass().addAll("jauge", "avancement-jauge");
        jauge.setPrefWidth(120);
        jauge.setMinWidth(60);
        jauge.setPrefHeight(8);
        jauge.setVisible(av.aJauge());
        jauge.setManaged(av.aJauge());
        if (av.aJauge()) {
            Tooltip.install(jauge, new Tooltip(av.pourcentage()));
        }
        HBox bas = Ui.ligne(12, detail, jauge);

        bloc.getChildren().addAll(haut, TableauDeBordJourJ.frise(l.frise()), bas);
        bloc.getStyleClass().addAll("avancement-ligne", "jourj-ligne-tableau");
        if (premiere) {
            bloc.getStyleClass().add("avancement-ligne-premiere");
        }
        PresentationAvancement.Detail principale = l.vue();
        nom.setOnAction(e -> ouvrirDetail(t, principale, bloc));
        // Toute la ligne est cliquable (hors boutons, qui ont leur propre action).
        bloc.setCursor(Cursor.HAND);
        bloc.setOnMouseClicked(e -> {
            if (!TableauDeBordJourJ.dansUnBouton(e.getTarget(), bloc)
                    && !(ferme && principale == PresentationAvancement.Detail.OUVRIR_POINTAGE)) {
                ouvrirDetail(t, principale, bloc);
            }
        });
        return bloc;
    }

    private void ouvrirDetail(Tableau t, PresentationAvancement.Detail d, VBox bloc) {
        if (d == PresentationAvancement.Detail.OUVRIR_POINTAGE) {
            if (tournoi.getStatut() != StatutTournoi.ANNULE) {
                confirmerOuverture(t, bloc);
            }
        } else if (d == PresentationAvancement.Detail.POINTAGE) {
            afficherPointage(t);
        } else if (d == PresentationAvancement.Detail.POULES) {
            afficherPoules(t, false);
        } else {
            afficherTableauFinal(t);
        }
    }

    /** Ouvrir le pointage ne se défait pas : confirmation en ligne, sous le tableau. */
    private void confirmerOuverture(Tableau t, VBox bloc) {
        if (bloc.getChildren().stream().anyMatch(n -> n.getStyleClass().contains("encart-confirmation"))) {
            return;
        }
        Button confirmer = Ui.boutonPrimaire("Ouvrir le pointage de " + t.getNom());
        Button renoncer = Ui.boutonSecondaire("Annuler");
        VBox encart = Ui.encartConfirmation("Le pointage du tableau « " + t.getNom() + " » sera ouvert : les joueurs "
                + "pourront être pointés et payer sur place. Cette étape ne se referme pas.", confirmer, renoncer);
        confirmer.setOnAction(e -> ouvrirPointage(t));
        renoncer.setOnAction(e -> bloc.getChildren().remove(encart));
        bloc.getChildren().add(encart);
    }

    private void ouvrirPointage(Tableau t) {
        try {
            t.ouvrirPointage();
        } catch (IllegalStateException refus) {
            afficherListe();
            return;
        }
        enregistrer();
        afficherPointage(t);
    }

    // ------------------------------------------------------------------
    // Écrans d'un tableau
    // ------------------------------------------------------------------

    private void afficherPointage(Tableau t) {
        racine.setCenter(new EcranPointageTableau(tournoi, t, this::enregistrer, this::afficherListe,
                tab -> afficherPoules(tab, true)).build());
    }

    private void afficherPoules(Tableau t, boolean apresTirage) {
        racine.setCenter(new EcranPoulesTableau(tournoi, t, this::enregistrer, this::afficherListe,
                apresTirage, () -> afficherTableauFinal(t)).avecLancement(this::ouvrirLancement).build());
    }

    /**
     * Tableau final : second tirage si les poules viennent de se terminer, puis
     * arbre des rencontres et saisie des scores ({@link VuePhaseFinale}).
     */
    private void afficherTableauFinal(Tableau t) {
        racine.setCenter(VuePhaseFinale.creer(contexte, tournoi, t, fichier, this::afficherListe,
                this::ouvrirLancement));
    }

    /** Raccourci « Lancer ce match » d'un match non lancé : la salle, interface « Lancer un match » ouverte. */
    private void ouvrirLancement() {
        rouvrir = "lancement";
        afficherListe();
    }
}
