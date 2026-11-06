package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatTransition;
import com.pingmanager.core.ServiceCompletudeTournoi;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.core.ServiceCompletudeTournoi.Item;
import com.pingmanager.core.ServiceCompletudeTournoi.Section;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

/**
 * Écran de complétion d'un tournoi : vue d'ensemble des quatre sections (état
 * de chaque champ + niveau d'exigence) et, par section, un bouton « Modifier »
 * qui ouvre un formulaire éditable ({@link FormulairesSection}).
 *
 * <p>Lecture via {@link ServiceCompletudeTournoi} ; écriture via le contexte
 * (persistance immédiate). Code couleur expliqué par une légende : rond =
 * dépendance (homologation, publication, recommandé, optionnel), badge à droite
 * = état (vert rempli, orange obligatoire à remplir, gris facultatif).</p>
 */
final class EcranCompletion {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private Tournoi tournoi;
    private final ServiceCompletudeTournoi completude = new ServiceCompletudeTournoi();
    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();
    private final ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();
    private PresentationPreparationTournoi presentation;

    private final BorderPane racine = new BorderPane();

    EcranCompletion(ContexteApplication contexte, Path fichier, Runnable onRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.tournoi = contexte.chargerTournoi(fichier);
    }

    Node build() {
        afficherApercu();
        return racine;
    }

    private void poser(Node contenu) {
        racine.setCenter(Ui.defilant(contenu));
    }

    // ------------------------------------------------------------------
    // Vue d'ensemble
    // ------------------------------------------------------------------

    private void afficherApercu() {
        // Relu à chaque retour : une inscription en ligne ou un import a pu modifier le fichier
        // entre-temps (toutes les sections enregistrent avant de revenir ici).
        tournoi = contexte.chargerTournoi(fichier);
        presentation = new PresentationPreparationTournoi(tournoi, completude, verrous);
        boolean supervision = contexte.estUltraAdmin() && !contexte.estTournoiDuCompte(tournoi);
        Button retour = Ui.boutonLien(supervision ? "← Tous les tournois" : "← Gestion des tournois");
        retour.setOnAction(e -> onRetour.run());

        String nomTournoi = (tournoi.getNom() == null || tournoi.getNom().isBlank())
                ? "Sans titre" : tournoi.getNom();
        Label titre = Ui.titre(nomTournoi);
        titre.setWrapText(true);
        HBox ligneTitre = Ui.ligne(12, titre, Ui.badge(libelleStatut(tournoi.getStatut())));

        List<Section> sections = completude.sections(tournoi);
        EnumSet<Niveau> presentes = EnumSet.noneOf(Niveau.class);
        sections.forEach(sec -> sec.items().forEach(it -> presentes.add(it.niveau())));

        VBox contenu = Ui.page(1040, new VBox(6, retour, ligneTitre), Ui.carte("Avancement du dossier", parcours()),
                recap(sections), carteInscriptions(),
                Ui.legende(presentes));
        if (EcranJourJ.estPropose(tournoi)) {
            contenu.getChildren().add(2, EcranJourJ.carteAcces(tournoi,
                    () -> racine.setCenter(new EcranJourJ(contexte, fichier, this::afficherApercu).build())));
        }
        if (supervision) {
            Label sup = new Label("Mode supervision : vous intervenez sur le tournoi du club "
                    + (tournoi.getClubOrganisateur() == null ? "" : tournoi.getClubOrganisateur())
                    + ". Vos modifications sont enregistrées directement dans son dossier.");
            sup.setWrapText(true);
            sup.getStyleClass().addAll("rappel-attention", "rappel-doux");
            contenu.getChildren().add(1, sup);
        }

        presentation.titreRefus().ifPresent(titreRefus ->
                contenu.getChildren().add(1, carteRefus(titreRefus)));

        Label titreDossier = Ui.sousTitre(presentation.titreDossier());
        titreDossier.getStyleClass().add("titre-dossier");
        if (presentation.deuxiemeVersion()) {
            // Deuxième version (B035) : les nouvelles parties en haut, le dossier et le règlement en bas.
            if (presentation.afficheSalle()) {
                contenu.getChildren().add(cartePlanSalle());
            }
            if (presentation.afficheStrategie()) {
                contenu.getChildren().add(carteStrategie());
            }
            if (presentation.afficheRemplissageEtFinances()) {
                contenu.getChildren().add(carteRemplissageFinances());
            }
            if (!presentation.cartesBientot().isEmpty()) {
                contenu.getChildren().add(cartesAVenir());
            }
            contenu.getChildren().addAll(carteEtapes(), titreDossier);
            for (int i = 0; i < sections.size(); i++) {
                contenu.getChildren().add(carteSection(i, sections.get(i)));
            }
        } else {
            contenu.getChildren().add(titreDossier);
            for (int i = 0; i < sections.size(); i++) {
                contenu.getChildren().add(carteSection(i, sections.get(i)));
            }
            contenu.getChildren().add(carteEtapes());
        }
        // Tant qu'aucun joueur n'est inscrit, le club peut basculer vers l'import (tournoi préparé ailleurs).
        if (com.pingmanager.core.RemplissageTableaux.nombreDeParticipants(tournoi) == 0
                && tournoi.getStatut() != StatutTournoi.PUBLIE) {
            Button versImport = Ui.boutonLien("Tournoi déjà préparé ailleurs ? Passer à l'import des tableaux et des joueurs");
            versImport.setOnAction(e -> {
                tournoi.setModePreparation(com.pingmanager.model.ModePreparation.IMPORT);
                contexte.enregistrerTournoi(tournoi, fichier);
                onRetour.run();
            });
            contenu.getChildren().add(versImport);
        }
        poser(contenu);
    }

    /**
     * Carte « Inscriptions » : remplissage global et accès à la prévisualisation
     * (par tableau, par joueur). Visible dès qu'il y a des tableaux.
     */
    private Node carteInscriptions() {
        int inscrits = 0;
        int places = 0;
        int attente = 0;
        for (com.pingmanager.model.Tableau t : tournoi.getTableaux()) {
            inscrits += t.getInscrits().size();
            attente += t.getFileAttente().size();
            places += Math.max(0, t.getConfiguration().getPlacesMax());
        }
        int participants = com.pingmanager.core.RemplissageTableaux.nombreDeParticipants(tournoi);
        Button voir = Ui.boutonPrimaire("Voir les inscriptions");
        voir.setOnAction(e -> racine.setCenter(new VueInscriptionsClub(contexte, fichier,
                this::afficherApercu).build()));
        Button importer = Ui.boutonSecondaire("Importer des joueurs…");
        importer.setOnAction(e -> racine.setCenter(new EcranImportJoueurs(contexte, fichier, this::afficherApercu,
                () -> racine.setCenter(new VueInscriptionsClub(contexte, fichier, this::afficherApercu).build())).build()));
        String phrase = participants == 0
                ? (tournoi.getStatut() == StatutTournoi.PUBLIE ? "Aucune inscription pour l'instant."
                : "Les inscriptions ouvriront à la publication du tournoi.")
                : participants + (participants > 1 ? " joueurs" : " joueur") + " · " + inscrits + " / " + places
                + " places occupées" + (attente > 0 ? " · " + attente + " en liste d'attente" : "");
        javafx.scene.control.ProgressBar barre = new javafx.scene.control.ProgressBar(
                places == 0 ? 0 : Math.min(1, inscrits / (double) places));
        barre.getStyleClass().add("jauge");
        barre.setMaxWidth(Double.MAX_VALUE);
        return Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Inscriptions"), importer, voir), barre, Ui.discret(phrase),
                Ui.aide("Tournoi préparé hors de PingManager ? « Importer des joueurs » crée les tableaux et les "
                        + "inscrits à partir d'un fichier Excel, LibreOffice, CSV ou JSON."));
    }

    /**
     * Tuiles en tête de la vue d'ensemble : ce qui manque pour l'homologation,
     * pour la publication, puis la complétude globale du dossier.
     */
    private Node recap(List<Section> sections) {
        int h = completude.bloquantsHomologation(tournoi);
        int p = completude.bloquantsPublication(tournoi);
        int renseignes = 0;
        int total = 0;
        for (Section sec : sections) {
            for (Item it : sec.items()) {
                total++;
                if (it.renseigne()) {
                    renseignes++;
                }
            }
        }
        javafx.scene.control.ProgressBar barre = new javafx.scene.control.ProgressBar(
                total == 0 ? 0 : renseignes / (double) total);
        barre.getStyleClass().addAll("jauge", "completion-jauge");
        barre.setMaxWidth(Double.MAX_VALUE);
        VBox globale = Ui.tuileChiffre(renseignes + " / " + total, "Éléments du dossier renseignés", barre);
        globale.getStyleClass().add("tuile-cle-accent");
        return Ui.rangeeTuiles(
                tuileRecap("Demande d'homologation", h,
                        "Tout est prêt pour demander l'homologation.",
                        "à renseigner avant de demander l'homologation."),
                tuileRecap("Publication", p,
                        "Tout est prêt pour publier et ouvrir les inscriptions.",
                        "à renseigner avant de publier."),
                globale);
    }

    /** Frise des quatre étapes : faite (coche), en cours (mise en avant), à venir (grisée). */
    private Node parcours() {
        HBox frise = new HBox(6);
        frise.getStyleClass().add("parcours");
        List<PresentationPreparationTournoi.Etape> etapes = presentation.parcours();
        for (int i = 0; i < etapes.size(); i++) {
            PresentationPreparationTournoi.Etape e = etapes.get(i);
            Label numero = new Label(e.etat() == PresentationPreparationTournoi.EtatEtape.FAITE
                    ? "\u2713" : String.valueOf(e.numero()));
            numero.getStyleClass().add("parcours-numero");
            Label titre = new Label(e.titre());
            titre.getStyleClass().add("parcours-titre");
            titre.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            VBox etape = new VBox(6, numero, titre);
            etape.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            etape.getStyleClass().addAll("parcours-etape", e.etat().classeCss());
            frise.getChildren().add(etape);
            if (i < etapes.size() - 1) {
                // Le trait relie les étapes et prend la place libre ; il est coloré une fois l'étape franchie.
                javafx.scene.layout.Region trait = new javafx.scene.layout.Region();
                trait.getStyleClass().add("parcours-trait");
                if (e.etat() == PresentationPreparationTournoi.EtatEtape.FAITE) {
                    trait.getStyleClass().add("parcours-trait-fait");
                }
                trait.setMinWidth(16);
                HBox.setHgrow(trait, Priority.ALWAYS);
                frise.getChildren().add(trait);
            }
        }
        return frise;
    }

    /** Rappel du dernier refus d'homologation, en tête du brouillon. */
    private Node carteRefus(String titreRefus) {
        Label titre = Ui.gras(titreRefus);
        Label motif = new Label("Motif : " + presentation.motifRefus().orElse(""));
        motif.setWrapText(true);
        Label conseil = Ui.discret(presentation.conseilApresRefus());
        conseil.setWrapText(true);
        VBox v = new VBox(6, titre, motif, conseil);
        v.getStyleClass().add("carte-refus-homologation");
        return v;
    }

    /** Deuxième version : les parties qui arrivent (salle, stratégie, remplissage et finances). */
    private Node cartesAVenir() {
        // Grille à colonnes égales : les cartes ont la même largeur et la même hauteur.
        javafx.scene.layout.GridPane tuiles = new javafx.scene.layout.GridPane();
        tuiles.setHgap(16);
        tuiles.getStyleClass().add("cartes-a-venir");
        List<PresentationPreparationTournoi.CarteAVenir> cartes = presentation.cartesBientot();
        for (int i = 0; i < cartes.size(); i++) {
            PresentationPreparationTournoi.CarteAVenir c = cartes.get(i);
            VBox carte = Ui.carteBientot(iconeAVenir(c.tache()), c.titre(), c.description(), c.tache());
            carte.setMaxWidth(Double.MAX_VALUE);
            carte.setMaxHeight(Double.MAX_VALUE);
            javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
            col.setPercentWidth(100.0 / cartes.size());
            col.setHgrow(Priority.ALWAYS);
            tuiles.getColumnConstraints().add(col);
            tuiles.add(carte, i, 0);
        }
        VBox bloc = new VBox(10, Ui.sousTitre("Pour la suite de la préparation"), tuiles);
        return bloc;
    }

    /**
     * Carte « Plan de la salle » (B024) : salles et tables placées, alertes de
     * cohérence, accès à l'éditeur de plan.
     */
    private Node cartePlanSalle() {
        com.pingmanager.core.ServicePlanSalle service = new com.pingmanager.core.ServicePlanSalle();
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.SALLE, false),
                Ui.titreCarte(PresentationPlanSalle.TITRE_CARTE));
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label phrase = Ui.gras(PresentationPlanSalle.phraseCarte(tournoi, service));
        phrase.setWrapText(true);
        VBox carte = Ui.carte(entete, phrase);
        if (tournoi.aUnPlanDeSalle()) {
            for (String alerte : PresentationPlanSalle.alertes(service.verifier(tournoi))) {
                Label l = Ui.discret(alerte);
                l.setWrapText(true);
                carte.getChildren().add(l);
            }
        } else {
            Label aide = Ui.discret("Placez les tables, les allées et les repères (table d'arbitrage, entrée, WC…), "
                    + "sur une ou plusieurs salles, et réservez des tables à un tableau.");
            aide.setWrapText(true);
            carte.getChildren().add(aide);
        }
        Button ouvrir = Ui.boutonSecondaire(tournoi.aUnPlanDeSalle() ? "Modifier le plan" : "Préparer le plan");
        ouvrir.setOnAction(e -> racine.setCenter(
                new EcranPlanSalle(contexte, fichier, this::afficherApercu).build()));
        carte.getChildren().add(Ui.actions(ouvrir));
        return carte;
    }

    /**
     * Carte « Stratégie d'ordonnancement » (B025) : stratégie appliquée et son
     * explication, accès à l'écran « Prévisions et stratégie ».
     */
    private Node carteStrategie() {
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.STRATEGIE, false),
                Ui.titreCarte(PresentationPrevisions.TITRE_CARTE));
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label phrase = Ui.gras(PresentationPrevisions.phraseStrategie(tournoi));
        phrase.setWrapText(true);
        Label explication = Ui.discret(tournoi.getStrategieOrdonnancement().getExplication());
        explication.setWrapText(true);
        Button ouvrir = Ui.boutonSecondaire("Choisir la stratégie et prévoir la journée");
        ouvrir.setOnAction(e -> racine.setCenter(
                new EcranPrevisions(contexte, fichier, this::afficherApercu).build()));
        return Ui.carte(entete, phrase, explication, Ui.actions(ouvrir));
    }

    /**
     * Carte « Remplissage et finances » (B036) : chiffres clés (remplissage,
     * encaissé, dotation promise, solde), jauge globale et une ligne par tableau.
     * Les mots et les couleurs viennent de {@link PresentationRemplissageFinances}.
     */
    private Node carteRemplissageFinances() {
        PresentationRemplissageFinances p = new PresentationRemplissageFinances(
                new com.pingmanager.core.ServiceRemplissageFinances().calculer(tournoi));
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.FINANCES, false),
                Ui.titreCarte(PresentationRemplissageFinances.TITRE));
        VBox carte = Ui.carte(entete);
        carte.getStyleClass().add("carte-finances");
        if (p.vide()) {
            carte.getChildren().add(Ui.etatVideCompact(p.messageVide()));
            return carte;
        }
        List<PresentationRemplissageFinances.Chiffre> chiffres = p.chiffres();
        Node[] tuiles = new Node[chiffres.size()];
        for (int i = 0; i < chiffres.size(); i++) {
            PresentationRemplissageFinances.Chiffre c = chiffres.get(i);
            VBox tuile = Ui.tuileChiffre(c.valeur(), c.libelle());
            if (!c.tonalite().classeCss().isEmpty()) {
                tuile.getChildren().get(0).getStyleClass().add(c.tonalite().classeCss());
            }
            tuiles[i] = tuile;
        }
        javafx.scene.control.ProgressBar jauge = new javafx.scene.control.ProgressBar(p.jaugeGlobale());
        jauge.getStyleClass().add("jauge");
        jauge.setMaxWidth(Double.MAX_VALUE);
        Label remplissage = Ui.discret(p.phraseRemplissage());
        remplissage.setWrapText(true);
        Label solde = Ui.discret(p.phraseSolde());
        solde.setWrapText(true);
        VBox lignes = new VBox();
        lignes.getStyleClass().add("finances-lignes");
        List<PresentationRemplissageFinances.Ligne> ls = p.lignes();
        for (int i = 0; i < ls.size(); i++) {
            lignes.getChildren().add(ligneFinances(ls.get(i), i == 0));
        }
        carte.getChildren().addAll(Ui.rangeeTuiles(tuiles), jauge, remplissage, solde, lignes);
        return carte;
    }

    /** Une ligne de tableau : nom, jauge, remplissage, attente, argent, dotation. */
    private static Node ligneFinances(PresentationRemplissageFinances.Ligne l, boolean premiere) {
        Label nom = Ui.gras(l.nom());
        nom.getStyleClass().add("finances-nom");
        nom.setMinWidth(150);
        nom.setPrefWidth(150);
        Node jauge;
        if (l.jauge() < 0) {
            Region vide = new Region();
            vide.setPrefWidth(110);
            vide.setMinWidth(110);
            jauge = vide;
        } else {
            javafx.scene.control.ProgressBar barre = new javafx.scene.control.ProgressBar(l.jauge());
            barre.getStyleClass().add("jauge");
            if (l.jauge() >= 1) {
                barre.getStyleClass().add("jauge-complete");
            }
            barre.setPrefWidth(110);
            barre.setMinWidth(110);
            jauge = barre;
        }
        Label remplissage = Ui.texte(l.remplissage());
        remplissage.getStyleClass().add("finances-remplissage");
        remplissage.setMinWidth(Region.USE_PREF_SIZE);
        HBox ligne = Ui.ligne(12, nom, jauge, remplissage);
        if (!l.attente().isEmpty()) {
            Label attente = Ui.pastille(l.attente(), StatutPastille.INFO);
            attente.setMinWidth(Region.USE_PREF_SIZE);
            ligne.getChildren().add(attente);
        }
        Label argent = Ui.discret(l.argent());
        argent.setMinWidth(Region.USE_PREF_SIZE);
        argent.setPrefWidth(170);
        Label dotation = Ui.discret(l.dotation());
        dotation.setWrapText(true);
        dotation.setPrefWidth(280);
        dotation.setMinWidth(200);
        dotation.getStyleClass().add("finances-dotation");
        ligne.getChildren().addAll(Ui.espace(), argent, dotation);
        ligne.getStyleClass().add("finances-ligne");
        if (premiere) {
            ligne.getStyleClass().add("finances-ligne-premiere");
        }
        return ligne;
    }

    /** Icône de chaque carte à venir, d'après sa tâche (chaîne de if, pas de switch). */
    private static String iconeAVenir(String tache) {
        if ("B024".equals(tache)) {
            return Icones.SALLE;
        }
        if ("B025".equals(tache)) {
            return Icones.STRATEGIE;
        }
        if ("B036".equals(tache)) {
            return Icones.FINANCES;
        }
        return null;
    }

    private static Node tuileRecap(String etape, int manquants, String siComplet, String siManquant) {
        Label pastille = manquants == 0
                ? Ui.pastille("Complet", Ui.ETAT_OK)
                : Ui.pastille(manquants + (manquants > 1 ? " manquants" : " manquant"),
                        Ui.ETAT_ATTENTION);
        String phrase = manquants == 0 ? siComplet
                : (manquants > 1 ? manquants + " éléments obligatoires " : "1 élément obligatoire ")
                        + siManquant;
        Label titre = Ui.gras(etape);
        titre.setWrapText(true);
        Label texte = Ui.discret(phrase);
        texte.setWrapText(true);
        VBox v = new VBox(6, titre, Ui.ligne(0, pastille), texte);
        v.getStyleClass().addAll("tuile-cle", "completion-etape");
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    // ------------------------------------------------------------------
    // Demande d'homologation, publication
    // ------------------------------------------------------------------

    /**
     * Deux étapes en bas de la vue d'ensemble : « Demander l'homologation »
     * (brouillon → demande d'homologation) puis « Publier le tournoi » (demande ou
     * homologué → publié). Chaque bouton n'est actif que si les conditions sont
     * réunies ; sinon, la raison est donnée en dessous.
     */
    private Node carteEtapes() {
        StatutTournoi st = tournoi.getStatut();
        Label message = Ui.message();
        int h = completude.bloquantsHomologation(tournoi);
        int p = completude.bloquantsPublication(tournoi);

        // --- Étape 1 : demande d'homologation ---
        VBox etape1;
        if (st == StatutTournoi.BROUILLON) {
            Button demander = Ui.boutonPrimaire("Demander l'homologation");
            demander.setDisable(h > 0);
            demander.setOnAction(e -> {
                ResultatTransition r = cycle.demanderHomologation(tournoi);
                if (r.estEffectuee()) {
                    contexte.enregistrerTournoi(tournoi, fichier);
                    afficherApercu();
                } else {
                    Ui.erreur(message, "Dossier incomplet : " + String.join(", ", r.getManquants()) + ".");
                }
            });
            etape1 = etape("1. Demande d'homologation",
                    h == 0 ? "Le dossier est complet : vous pouvez faire la demande. Le tournoi passe à l'état "
                            + "« Demande d'homologation » en attendant le numéro."
                            : manquants(h) + " avant de pouvoir demander l'homologation (voir les sections ci-dessus).",
                    demander);
        } else if (st == StatutTournoi.DEMANDE_HOMOLOGATION) {
            Button retirer = Ui.boutonLien("Retirer la demande");
            retirer.setOnAction(e -> {
                cycle.retirerDemande(tournoi);
                contexte.enregistrerTournoi(tournoi, fichier);
                afficherApercu();
            });
            etape1 = etape("1. Demande d'homologation",
                    "Demande faite : le dossier d'homologation n'est plus modifiable. Enregistrez la réponse "
                            + "dès réception : acceptée (avec le numéro délivré) ou refusée (avec le motif).",
                    null, Ui.pastille("Demandée", Ui.ETAT_OK));
            etape1.getChildren().add(reponseHomologation(message, retirer));
        } else {
            etape1 = etape("1. Demande d'homologation",
                    st == StatutTournoi.ANNULE ? "Tournoi annulé." : "Homologation obtenue"
                            + (tournoi.getNumeroHomologation() != null ? " (n° " + tournoi.getNumeroHomologation() + ")."
                            : "."),
                    null, Ui.pastille(st == StatutTournoi.ANNULE ? "Annulé" : "Fait", Ui.ETAT_OK));
        }

        // --- Étape 2 : publication ---
        VBox etape2;
        if (st == StatutTournoi.PUBLIE) {
            etape2 = etape("2. Publication", "Le tournoi est publié : les inscriptions sont ouvertes.",
                    null, Ui.pastille("Publié", Ui.ETAT_OK));
        } else {
            boolean statutOk = st == StatutTournoi.DEMANDE_HOMOLOGATION || st == StatutTournoi.HOMOLOGUE;
            Button publier = Ui.boutonPrimaire("Publier le tournoi");
            publier.setDisable(!statutOk || p > 0);
            publier.setOnAction(e -> {
                ResultatTransition r = cycle.publierDepuisDemande(tournoi);
                if (r.estEffectuee()) {
                    contexte.enregistrerTournoi(tournoi, fichier);
                    afficherApercu();
                } else {
                    Ui.erreur(message, "Publication impossible : " + String.join(", ", r.getManquants()) + ".");
                }
            });
            String raison = st == StatutTournoi.BROUILLON || st == StatutTournoi.ANNULE
                    ? "Faites d'abord la demande d'homologation."
                    : p > 0 ? manquants(p) + " avant de publier (dont le numéro d'homologation s'il n'est pas "
                            + "encore saisi)."
                    : "Tout est prêt : la publication ouvre les inscriptions.";
            etape2 = etape("2. Publication", raison, publier);
        }
        HBox etapes = Ui.ligne(16, etape1, etape2);
        etapes.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        for (VBox e : List.of(etape1, etape2)) {
            HBox.setHgrow(e, Priority.ALWAYS);
            e.setPrefWidth(0);
            e.setMaxWidth(Double.MAX_VALUE);
        }
        return Ui.carte("Homologation et publication", etapes, message);
    }

    /**
     * Réponse à la demande : « Homologation acceptée » (numéro délivré) ou
     * « Homologation refusée » (motif obligatoire, retour au brouillon).
     */
    private Node reponseHomologation(Label message, Button retirer) {
        TextField numero = Ui.champ("n° délivré par la FFTT");
        numero.getStyleClass().add("champ-numero-homologation");
        if (tournoi.getNumeroHomologation() != null) {
            numero.setText(tournoi.getNumeroHomologation());
        }
        Button accepter = Ui.boutonPrimaire("Homologation acceptée");
        accepter.setOnAction(e -> {
            ResultatTransition r = cycle.homologuer(tournoi, numero.getText());
            if (r.estEffectuee()) {
                contexte.enregistrerTournoi(tournoi, fichier);
                afficherApercu();
            } else {
                Ui.erreur(message, PresentationPreparationTournoi.messageTransition(r));
            }
        });
        Button refuser = Ui.boutonSecondaire("Homologation refusée");
        refuser.getStyleClass().add("bouton-refus");

        TextArea motif = new TextArea();
        motif.setPromptText("Motif du refus, tel que communiqué (obligatoire)");
        motif.setPrefRowCount(3);
        motif.setWrapText(true);
        Button confirmer = Ui.boutonPrimaire("Enregistrer le refus");
        Button renoncer = Ui.boutonLien("Annuler");
        VBox saisieRefus = new VBox(8, Ui.champEtiquete("Motif du refus", motif),
                Ui.discret("Le tournoi revient au brouillon : le dossier redevient modifiable."),
                Ui.actions(confirmer, renoncer));
        saisieRefus.getStyleClass().add("saisie-refus");
        Ui.cacher(saisieRefus);
        refuser.setOnAction(e -> {
            Ui.montrer(saisieRefus);
            motif.requestFocus();
        });
        renoncer.setOnAction(e -> {
            motif.clear();
            Ui.cacher(saisieRefus);
            Ui.cacher(message);
        });
        confirmer.setOnAction(e -> {
            ResultatTransition r = cycle.refuserHomologation(tournoi, motif.getText());
            if (r.estEffectuee()) {
                contexte.enregistrerTournoi(tournoi, fichier);
                afficherApercu();
            } else {
                Ui.erreur(message, PresentationPreparationTournoi.messageTransition(r));
                Ui.marquerErreur(motif);
            }
        });
        retirer.getStyleClass().add("lien-aligne-bouton");
        HBox ligne = Ui.ligne(10, Ui.champEtiquete("Numéro d'homologation", numero), accepter, refuser,
                Ui.espace(), retirer);
        ligne.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        return new VBox(10, ligne, saisieRefus);
    }

    private static String manquants(int n) {
        return n > 1 ? n + " éléments obligatoires à renseigner" : "1 élément obligatoire à renseigner";
    }

    private static VBox etape(String titre, String texte, Button action, Label... pastilles) {
        Label t = Ui.discret(texte);
        t.setWrapText(true);
        HBox entete = Ui.ligne(10, Ui.gras(titre));
        for (Label pa : pastilles) {
            entete.getChildren().add(pa);
        }
        VBox v = new VBox(6, entete, t);
        if (action != null) {
            Region pousse = new Region();
            VBox.setVgrow(pousse, Priority.ALWAYS);
            v.getChildren().addAll(pousse, Ui.actions(action));
        }
        v.getStyleClass().addAll("bloc-regle", "completion-etape-dossier");
        return v;
    }

    private static final String[] ICONES_SECTIONS = {Icones.FICHE, Icones.TABLEAUX, Icones.RAQUETTE, Icones.DOCUMENT};

    private Node carteSection(int index, Section s) {
        javafx.scene.layout.GridPane lignes = new javafx.scene.layout.GridPane();
        lignes.getStyleClass().add("completion-items");
        for (int c = 0; c < 2; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(50);
            lignes.getColumnConstraints().add(cc);
        }
        int rang = 0;
        int renseignes = 0;
        // Tournoi publié : le règlement est figé, sa ligne laisse place à un lien de lecture.
        boolean reglementFige = index == 3 && !tournoi.reglementModifiable();
        for (Item it : s.items()) {
            // Deux colonnes : les éléments se lisent de gauche à droite, ligne par ligne.
            Node cellule = reglementFige && it.libelle().startsWith("Règlement du tournoi")
                    ? lienReglementFige() : ligneItem(it);
            lignes.add(cellule, rang % 2, rang / 2);
            rang++;
            if (it.renseigne()) {
                renseignes++;
            }
        }

        Button modifier = Ui.boutonSecondaire(index == 1 ? "Gérer les tableaux" : index >= 2 ? "Gérer" : "Modifier");
        modifier.setOnAction(e -> afficherFormulaire(index, s.titre()));

        Label compte = Ui.discret(renseignes + " / " + s.items().size() + " renseignés");
        Node icone = Ui.iconeTuile(ICONES_SECTIONS[Math.min(index, ICONES_SECTIONS.length - 1)], false);
        HBox titreSection = Ui.ligne(12, icone, new VBox(2, Ui.titreCarte(s.titre()), compte));
        String verrou = presentation.mentionVerrouSection(index);
        if (verrou != null) {
            Label pastille = Ui.pastille(verrou, Ui.CADRE_FACULTATIF);
            pastille.getStyleClass().add("pastille-verrou");
            pastille.setGraphic(Ui.iconeCadree(Icones.CADENAS, "icone-verrou"));
            pastille.setGraphicTextGap(6);
            pastille.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            pastille.setTooltip(new javafx.scene.control.Tooltip(presentation.motifVerrouSection(index)));
            titreSection.getChildren().add(pastille);
        }
        Node entete = Ui.enteteAvecActions(titreSection, modifier);
        javafx.scene.control.ProgressBar barre = new javafx.scene.control.ProgressBar(
                s.items().isEmpty() ? 0 : renseignes / (double) s.items().size());
        barre.getStyleClass().addAll("jauge", "completion-jauge");
        barre.setMaxWidth(Double.MAX_VALUE);
        return Ui.carte(entete, barre, lignes);
    }

    /** Lien vers le règlement figé d'un tournoi publié (lecture seule, téléchargements). */
    private Node lienReglementFige() {
        Path dossier = fichier.toAbsolutePath().getParent();
        Runnable enregistrer = () -> contexte.enregistrerTournoi(tournoi, fichier);
        return VueReglementFige.lien(tournoi, dossier, enregistrer, () -> racine.setCenter(
                VueReglementFige.page(tournoi, dossier, enregistrer, this::afficherApercu, "← Vue d'ensemble")));
    }

    /** Une ligne : rond de dépendance, libellé, et état à droite (même code que les cadres). */
    private static Node ligneItem(Item it) {
        Label libelle = new Label(it.libelle());
        libelle.setWrapText(true);
        HBox l = Ui.ligne(10, Ui.puceDependance(it.niveau()), libelle, Ui.espace(),
                Ui.badgeEtat(it.niveau(), it.renseigne()));
        l.getStyleClass().add("completion-item");
        return l;
    }

    // ------------------------------------------------------------------
    // Formulaire d'une section
    // ------------------------------------------------------------------

    private void afficherFormulaire(int index, String titreSection) {
        if (index == 1) {
            // Tableaux et inscriptions : écran dédié (échéances + grille + formulaire
            // d'un tableau). Il gère son propre défilement.
            racine.setCenter(new EcranTableaux(tournoi,
                    () -> contexte.enregistrerTournoi(tournoi, fichier),
                    this::afficherApercu).build());
            return;
        }
        if (index == 2) {
            // Informations sportives et techniques : écran dédié (JA, arbitres, matériel, rappels).
            racine.setCenter(new EcranSportif(tournoi, contexte::rechercherLicences,
                    () -> contexte.enregistrerTournoi(tournoi, fichier),
                    this::afficherApercu).build());
            return;
        }
        if (index == 3) {
            // Complémentaire : règlement (déposé ou généré), image, conditions, site web.
            racine.setCenter(new EcranComplementaire(tournoi, fichier.toAbsolutePath().getParent(),
                    () -> contexte.enregistrerTournoi(tournoi, fichier),
                    this::afficherApercu).build());
            return;
        }
        FormulairesSection.Referentiel ref = new FormulairesSection.Referentiel(
                contexte.clubsGeres(), contexte::correspondantParDefaut);
        Node form = FormulairesSection.construire(
                index, titreSection, tournoi, ref,
                () -> contexte.enregistrerTournoi(tournoi, fichier),
                () -> {                        // à l'enregistrement
                    contexte.enregistrerTournoi(tournoi, fichier);
                    afficherApercu();
                },
                this::afficherApercu);         // à l'annulation
        poser(form);
    }

    private static String libelleStatut(StatutTournoi statut) {
        // Pas de switch sur l'énumération (classe annexe générée par javac).
        if (statut == StatutTournoi.DEMANDE_HOMOLOGATION) {
            return "Demande d'homologation";
        }
        if (statut == StatutTournoi.HOMOLOGUE) {
            return "Homologué";
        }
        if (statut == StatutTournoi.PUBLIE) {
            return "Publié";
        }
        return statut == StatutTournoi.ANNULE ? "Annulé" : "Brouillon";
    }
}
