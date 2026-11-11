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
        Label infos = Ui.discret(BlocsInscriptions.periode(tournoi)
                + (tournoi.getAdresse() == null ? "" : " · " + tournoi.getAdresse()));
        infos.setWrapText(true);

        List<Section> sections = completude.sections(tournoi);

        // Disposition validée par Quentin (11 novembre 2026) : en-tête, puis le dossier
        // (légende + quatre blocs en grille), puis les trois étapes officielles. Plus de
        // frise ni de tuiles de résumé : les étapes disent elles-mêmes ce qui manque.
        VBox contenu = Ui.page(Double.MAX_VALUE, new VBox(6, retour, ligneTitre, infos));
        contenu.getStyleClass().add("page-dossier");
        if (supervision) {
            Label sup = new Label("Mode supervision : vous intervenez sur le tournoi du club "
                    + (tournoi.getClubOrganisateur() == null ? "" : tournoi.getClubOrganisateur())
                    + ". Vos modifications sont enregistrées directement dans son dossier.");
            sup.setWrapText(true);
            sup.getStyleClass().addAll("rappel-attention", "rappel-doux");
            contenu.getChildren().add(sup);
        }
        presentation.titreRefus().ifPresent(titreRefus -> contenu.getChildren().add(carteRefus(titreRefus)));
        if (EcranJourJ.estPropose(tournoi)) {
            contenu.getChildren().add(EcranJourJ.carteAcces(tournoi,
                    () -> racine.setCenter(new EcranJourJ(contexte, fichier, this::afficherApercu).build())));
        }

        if (presentation.deuxiemeVersion()) {
            // Homologué ou publié (B035) : les inscriptions et les nouvelles parties en haut,
            // les étapes puis le dossier et le règlement en bas.
            contenu.getChildren().add(carteInscriptions());
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
            contenu.getChildren().addAll(sectionEtapes(), sectionDossier(sections));
        } else {
            // Brouillon ou demande en cours : pas encore d'inscriptions, donc pas de carte dédiée.
            contenu.getChildren().addAll(sectionDossier(sections), sectionEtapes());
        }
        // Tant qu'aucun joueur n'est inscrit, le club peut basculer vers l'import (tournoi préparé ailleurs).
        if (com.pingmanager.core.RemplissageTableaux.nombreDeParticipants(tournoi) == 0
                && tournoi.getStatut() != StatutTournoi.PUBLIE) {
            Button versImport = Ui.boutonLien("Tournoi déjà préparé ailleurs ? Déposer le règlement et importer "
                    + "les tableaux et les joueurs");
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

    // ------------------------------------------------------------------
    // Préparation du dossier : légende et quatre blocs en grille
    // ------------------------------------------------------------------

    /** Nombre d'éléments montrés par bloc ; le reste est dans le formulaire de la section. */
    static final int ELEMENTS_PAR_BLOC = 5;

    private static final String[] ICONES_SECTIONS = {Icones.FICHE, Icones.TABLEAUX, Icones.RAQUETTE, Icones.DOCUMENT};

    /**
     * Section « Préparation du dossier » : titre et progression globale, légende sur une
     * bande, puis les quatre blocs (Général, Tableaux, Informations sportives,
     * Complémentaire) sur 4, 2 ou 1 colonne selon la largeur de la fenêtre.
     */
    private Node sectionDossier(List<Section> sections) {
        EnumSet<Niveau> presentes = EnumSet.noneOf(Niveau.class);
        int renseignes = 0;
        int total = 0;
        for (Section sec : sections) {
            for (Item it : sec.items()) {
                presentes.add(it.niveau());
                total++;
                if (it.renseigne()) {
                    renseignes++;
                }
            }
        }
        Label titre = Ui.titreSection(presentation.titreDossier());
        Label aide = Ui.discret("Complétez chaque bloc. Les éléments « à remplir » bloquent l'étape suivante.");
        aide.setWrapText(true);
        VBox titres = new VBox(4, titre, aide);
        HBox.setHgrow(titres, Priority.ALWAYS);

        javafx.scene.control.ProgressBar barre = new javafx.scene.control.ProgressBar(
                total == 0 ? 0 : renseignes / (double) total);
        barre.getStyleClass().addAll("jauge", "jauge-dossier");
        barre.setPrefWidth(220);
        Label compteur = new Label(renseignes + " / " + total);
        compteur.getStyleClass().add("dossier-compteur");
        HBox progression = Ui.ligne(14, barre, compteur);
        progression.setMinWidth(Region.USE_PREF_SIZE);

        javafx.scene.layout.FlowPane entete = new javafx.scene.layout.FlowPane(24, 12, titres, progression);
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        // Le titre prend la place libre ; la progression reste à droite tant qu'elle tient sur la ligne.
        titres.prefWidthProperty().bind(javafx.beans.binding.Bindings.max(260,
                entete.widthProperty().subtract(progression.widthProperty()).subtract(40)));

        List<Node> blocs = new java.util.ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            blocs.add(blocSection(i, sections.get(i)));
        }
        javafx.scene.layout.GridPane grille = Ui.grilleAdaptative(4, 250, blocs);
        grille.getStyleClass().add("grille-dossier");

        VBox carte = new VBox(18, entete, Ui.legendeCompacte(presentes), grille);
        carte.getStyleClass().addAll("carte", "section-dossier");
        return carte;
    }

    /** Un bloc du dossier : icône, état, jauge, premiers éléments (manquants d'abord), accès. */
    private Node blocSection(int index, Section s) {
        int renseignes = 0;
        int obligatoiresManquants = 0;
        for (Item it : s.items()) {
            if (it.renseigne()) {
                renseignes++;
            } else if (Ui.estObligatoire(it.niveau())) {
                obligatoiresManquants++;
            }
        }
        int nonRenseignes = s.items().size() - renseignes;
        Label etat;
        if (obligatoiresManquants > 0) {
            etat = Ui.pastille(obligatoiresManquants + " à remplir", Ui.CADRE_MANQUANT);
        } else if (nonRenseignes == 0) {
            etat = Ui.pastille("Complet", Ui.CADRE_OK);
        } else {
            etat = Ui.pastille(nonRenseignes + (nonRenseignes > 1 ? " facultatifs" : " facultatif"),
                    Ui.CADRE_FACULTATIF);
        }
        Label titre = Ui.titreCarte(s.titre());
        titre.setWrapText(true);
        HBox pastilles = Ui.ligne(6, etat);
        String verrou = presentation.mentionVerrouSection(index);
        if (verrou != null) {
            Label pastille = Ui.pastille(verrou, Ui.CADRE_FACULTATIF);
            pastille.getStyleClass().add("pastille-verrou");
            pastille.setGraphic(Ui.iconeCadree(Icones.CADENAS, "icone-verrou"));
            pastille.setGraphicTextGap(6);
            pastille.setTooltip(new javafx.scene.control.Tooltip(presentation.motifVerrouSection(index)));
            pastilles.getChildren().add(pastille);
        }
        VBox titres = new VBox(6, titre, pastilles);
        HBox.setHgrow(titres, Priority.ALWAYS);
        HBox entete = new HBox(12, Ui.iconeTuile(ICONES_SECTIONS[Math.min(index, ICONES_SECTIONS.length - 1)], false),
                titres);

        javafx.scene.control.ProgressBar barre = new javafx.scene.control.ProgressBar(
                s.items().isEmpty() ? 0 : renseignes / (double) s.items().size());
        barre.getStyleClass().addAll("jauge", "completion-jauge");
        barre.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(barre, Priority.ALWAYS);
        Label compte = Ui.discret(renseignes + " / " + s.items().size());
        compte.setMinWidth(Region.USE_PREF_SIZE);
        HBox jauge = Ui.ligne(10, barre, compte);

        // Tournoi publié : le règlement est figé, sa ligne laisse place à un lien de lecture.
        boolean reglementFige = index == 3 && !tournoi.reglementModifiable();
        VBox lignes = new VBox();
        List<Item> montres = elementsMontres(s.items());
        for (Item it : montres) {
            lignes.getChildren().add(reglementFige && it.libelle().startsWith("Règlement du tournoi")
                    ? lienReglementFige() : ligneItem(it));
        }
        int autres = s.items().size() - montres.size();

        Button ouvrir = Ui.boutonSecondaire(index == 1 ? "Gérer les tableaux" : "Ouvrir");
        ouvrir.setOnAction(e -> afficherFormulaire(index, s.titre()));
        Label reste = Ui.discret(autres > 0 ? "+ " + autres + (autres > 1 ? " autres éléments" : " autre élément") : "");
        reste.setWrapText(true);
        HBox pied = Ui.ligne(8, reste, Ui.espace(), ouvrir);
        Region pousse = new Region();
        VBox.setVgrow(pousse, Priority.ALWAYS);

        VBox bloc = new VBox(14, entete, jauge, lignes, pousse, pied);
        bloc.getStyleClass().add("bloc-dossier");
        return bloc;
    }

    /**
     * Les éléments affichés dans un bloc : d'abord les obligatoires non renseignés (ce qui
     * bloque), puis les autres dans l'ordre de la section, {@link #ELEMENTS_PAR_BLOC} au plus.
     */
    static List<Item> elementsMontres(List<Item> items) {
        List<Item> tries = new java.util.ArrayList<>();
        for (Item it : items) {
            if (!it.renseigne() && Ui.estObligatoire(it.niveau())) {
                tries.add(it);
            }
        }
        for (Item it : items) {
            if (!tries.contains(it)) {
                tries.add(it);
            }
        }
        return tries.subList(0, Math.min(ELEMENTS_PAR_BLOC, tries.size()));
    }

    // ------------------------------------------------------------------
    // Homologation et publication : trois étapes officielles
    // ------------------------------------------------------------------

    /** État d'affichage d'une étape officielle. */
    private enum EtatOfficiel {
        FAITE("etape-faite", "Terminée"),
        EN_COURS("etape-en-cours", "Étape en cours"),
        A_VENIR("etape-a-venir", "À venir");

        private final String classe;
        private final String surtitre;

        EtatOfficiel(String classe, String surtitre) {
            this.classe = classe;
            this.surtitre = surtitre;
        }
    }

    /**
     * Section « Homologation et publication » : trois grandes cartes ordonnées
     * (demande, réponse de la FFTT, publication). Chacune dit son état, ce qui manque et
     * porte son action ; une étape se débloque quand la précédente est validée.
     */
    private Node sectionEtapes() {
        StatutTournoi st = tournoi.getStatut();
        Label message = Ui.message();
        int h = completude.bloquantsHomologation(tournoi);
        int p = completude.bloquantsPublication(tournoi);
        boolean annule = st == StatutTournoi.ANNULE;

        // --- 1. Demande d'homologation ---
        Node etape1;
        if (st == StatutTournoi.BROUILLON) {
            Button demander = Ui.boutonPrimaire("Demander l'homologation");
            demander.setDisable(h > 0);
            demander.setMaxWidth(Double.MAX_VALUE);
            demander.setOnAction(e -> {
                ResultatTransition r = cycle.demanderHomologation(tournoi);
                if (r.estEffectuee()) {
                    contexte.enregistrerTournoi(tournoi, fichier);
                    afficherApercu();
                } else {
                    Ui.erreur(message, "Dossier incomplet : " + String.join(", ", r.getManquants()) + ".");
                }
            });
            etape1 = carteEtape(1, EtatOfficiel.EN_COURS, "Demande d'homologation",
                    h == 0 ? Ui.pastille("Prêt à envoyer", Ui.CADRE_OK) : Ui.pastille(manquantsCourt(h), Ui.CADRE_MANQUANT),
                    h == 0 ? "Le dossier est complet. Faites la demande auprès de la FFTT, puis indiquez-le ici : "
                            + "le dossier d'homologation ne sera plus modifiable."
                            : manquants(h) + " avant de pouvoir demander l'homologation (voir les blocs ci-dessus).",
                    demander);
        } else if (st == StatutTournoi.DEMANDE_HOMOLOGATION) {
            Button retirer = Ui.boutonLien("Retirer la demande");
            retirer.setOnAction(e -> {
                cycle.retirerDemande(tournoi);
                contexte.enregistrerTournoi(tournoi, fichier);
                afficherApercu();
            });
            etape1 = carteEtape(1, EtatOfficiel.FAITE, "Demande d'homologation",
                    Ui.pastille("Demandée", Ui.CADRE_OK),
                    "Demande faite : le dossier d'homologation n'est plus modifiable tant qu'elle est en cours.",
                    retirer);
        } else {
            etape1 = carteEtape(1, annule ? EtatOfficiel.A_VENIR : EtatOfficiel.FAITE, "Demande d'homologation",
                    Ui.pastille(annule ? "Annulé" : "Fait", annule ? Ui.CADRE_FACULTATIF : Ui.CADRE_OK),
                    annule ? "Tournoi annulé." : "Demande faite et acceptée.", null);
        }

        // --- 2. Réponse de la FFTT ---
        Node etape2;
        if (st == StatutTournoi.DEMANDE_HOMOLOGATION) {
            etape2 = carteEtape(2, EtatOfficiel.EN_COURS, "Homologation obtenue",
                    Ui.pastille("En attente de la FFTT", Ui.CADRE_FACULTATIF),
                    "Dès réception de la réponse, enregistrez-la : acceptée avec le numéro délivré, "
                            + "ou refusée avec le motif.",
                    reponseHomologation(message));
        } else if (st == StatutTournoi.HOMOLOGUE || st == StatutTournoi.PUBLIE) {
            etape2 = carteEtape(2, EtatOfficiel.FAITE, "Homologation obtenue", Ui.pastille("Fait", Ui.CADRE_OK),
                    "Homologation obtenue" + (tournoi.getNumeroHomologation() != null
                            ? " : n° " + tournoi.getNumeroHomologation() + "." : "."), null);
        } else {
            TextField numero = Ui.champ("n° délivré par la FFTT");
            numero.setDisable(true);
            etape2 = carteEtape(2, EtatOfficiel.A_VENIR, "Homologation obtenue", Ui.pastille("À venir", Ui.CADRE_FACULTATIF),
                    "Après la demande, saisissez ici le numéro d'homologation délivré par la FFTT. "
                            + "C'est en général le dernier élément qui manque pour publier.",
                    Ui.champEtiquete("Numéro d'homologation", numero));
        }

        // --- 3. Publication ---
        Node etape3;
        if (st == StatutTournoi.PUBLIE) {
            etape3 = carteEtape(3, EtatOfficiel.FAITE, "Publication", Ui.pastille("Publié", Ui.CADRE_OK),
                    "Le tournoi est publié : les inscriptions en ligne sont ouvertes.", null);
        } else {
            boolean statutOk = st == StatutTournoi.DEMANDE_HOMOLOGATION || st == StatutTournoi.HOMOLOGUE;
            Button publier = Ui.boutonPrimaire("Publier le tournoi");
            publier.setDisable(!statutOk || p > 0);
            publier.setMaxWidth(Double.MAX_VALUE);
            publier.setOnAction(e -> {
                ResultatTransition r = cycle.publierDepuisDemande(tournoi);
                if (r.estEffectuee()) {
                    contexte.enregistrerTournoi(tournoi, fichier);
                    afficherApercu();
                } else {
                    Ui.erreur(message, "Publication impossible : " + String.join(", ", r.getManquants()) + ".");
                }
            });
            String raison = st == StatutTournoi.BROUILLON || annule
                    ? "Le tournoi apparaîtra dans l'onglet Tournois et les inscriptions en ligne s'ouvriront. "
                    + "Faites d'abord la demande d'homologation."
                    : p > 0 ? manquants(p) + " avant de publier (dont le numéro d'homologation s'il n'est pas "
                    + "encore saisi)."
                    : "Tout est prêt : la publication ouvre les inscriptions en ligne.";
            etape3 = carteEtape(3, st == StatutTournoi.HOMOLOGUE ? EtatOfficiel.EN_COURS : EtatOfficiel.A_VENIR,
                    "Publication",
                    p > 0 ? Ui.pastille(manquantsCourt(p), Ui.CADRE_MANQUANT)
                            : Ui.pastille(statutOk ? "Prêt" : "À venir", statutOk ? Ui.CADRE_OK : Ui.CADRE_FACULTATIF),
                    raison, publier);
        }

        Label titre = Ui.titreSection("Homologation et publication");
        Label aide = Ui.discret("Les trois étapes officielles, dans l'ordre. Chacune se débloque quand la précédente "
                + "est validée.");
        aide.setWrapText(true);
        javafx.scene.layout.GridPane grille = Ui.grilleAdaptative(3, 300, List.of(etape1, etape2, etape3));
        grille.getStyleClass().add("grille-etapes");
        VBox section = new VBox(14, new VBox(4, titre, aide), grille, message);
        section.getStyleClass().add("section-etapes");
        return section;
    }

    /** Une grande carte d'étape : numéro, surtitre d'état, pastille, titre, texte, contenu. */
    private static VBox carteEtape(int numero, EtatOfficiel etat, String titre, Label pastille, String texte,
                                   Node contenu) {
        Label rond = new Label(etat == EtatOfficiel.FAITE ? "✓" : String.valueOf(numero));
        rond.getStyleClass().add("etape-numero");
        rond.setMinSize(34, 34);
        Label surtitre = new Label(etat.surtitre);
        surtitre.getStyleClass().add("etape-surtitre");
        HBox entete = Ui.ligne(10, rond, surtitre, Ui.espace(), pastille);
        Label t = new Label(titre);
        t.getStyleClass().add("etape-titre");
        t.setWrapText(true);
        Label explication = Ui.discret(texte);
        explication.setWrapText(true);
        VBox v = new VBox(12, entete, t, explication);
        if (contenu != null) {
            Region pousse = new Region();
            VBox.setVgrow(pousse, Priority.ALWAYS);
            v.getChildren().addAll(pousse, contenu);
        }
        v.getStyleClass().addAll("etape-officielle", etat.classe);
        return v;
    }

    /**
     * Réponse à la demande : « Homologation acceptée » (numéro délivré) ou
     * « Homologation refusée » (motif obligatoire, retour au brouillon).
     */
    private Node reponseHomologation(Label message) {
        TextField numero = Ui.champ("n° délivré par la FFTT");
        numero.getStyleClass().add("champ-numero-homologation");
        numero.setMaxWidth(Double.MAX_VALUE);
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
        javafx.scene.layout.FlowPane boutons = new javafx.scene.layout.FlowPane(10, 10, accepter, refuser);
        return new VBox(10, Ui.champEtiquete("Numéro d'homologation", numero), boutons, saisieRefus);
    }

    private static String manquants(int n) {
        return n > 1 ? n + " éléments obligatoires à renseigner" : "1 élément obligatoire à renseigner";
    }

    private static String manquantsCourt(int n) {
        return n > 1 ? n + " manquants" : "1 manquant";
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
