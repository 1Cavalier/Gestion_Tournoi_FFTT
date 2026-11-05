package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatTransition;
import com.pingmanager.core.ServiceCompletudeTournoi;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.ServiceCompletudeTournoi.Item;
import com.pingmanager.core.ServiceCompletudeTournoi.Section;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

        VBox contenu = Ui.page(780, new VBox(6, retour, ligneTitre), recap(), carteInscriptions(),
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
            sup.getStyleClass().add("rappel-attention");
            contenu.getChildren().add(1, sup);
        }

        for (int i = 0; i < sections.size(); i++) {
            contenu.getChildren().add(carteSection(i, sections.get(i)));
        }
        contenu.getChildren().add(carteEtapes());
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

    /** Deux lignes : où en est le dossier pour l'homologation, puis la publication. */
    private Node recap() {
        int h = completude.bloquantsHomologation(tournoi);
        int p = completude.bloquantsPublication(tournoi);
        return Ui.carte("Avancement du dossier",
                ligneRecap("Demande d'homologation", h,
                        "Tout est prêt pour demander l'homologation.",
                        "à renseigner avant de demander l'homologation."),
                ligneRecap("Publication", p,
                        "Tout est prêt pour publier et ouvrir les inscriptions.",
                        "à renseigner avant de publier."));
    }

    private static Node ligneRecap(String etape, int manquants, String siComplet, String siManquant) {
        Label pastille = manquants == 0
                ? Ui.pastille("Complet", Ui.ETAT_OK)
                : Ui.pastille(manquants + (manquants > 1 ? " manquants" : " manquant"),
                        Ui.ETAT_ATTENTION);
        // Colonne de largeur fixe : la pastille garde sa taille, les textes restent alignés.
        HBox colonne = new HBox(pastille);
        colonne.setMinWidth(92);
        colonne.setPrefWidth(92);
        String phrase = manquants == 0 ? siComplet
                : (manquants > 1 ? manquants + " éléments obligatoires " : "1 élément obligatoire ")
                        + siManquant;
        VBox textes = new VBox(2, Ui.gras(etape), Ui.discret(phrase));
        HBox.setHgrow(textes, Priority.ALWAYS);
        HBox l = Ui.ligne(14, colonne, textes);
        l.getStyleClass().add("recap-ligne");
        return l;
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
                    "Demande faite. Dès réception, saisissez le numéro d'homologation dans la section Général.",
                    retirer, Ui.pastille("Demandée", Ui.ETAT_OK));
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
        return Ui.carte("Homologation et publication", etape1, etape2, message);
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
            v.getChildren().add(Ui.actions(action));
        }
        v.getStyleClass().add("bloc-regle");
        return v;
    }

    private Node carteSection(int index, Section s) {
        VBox lignes = new VBox(7);
        int renseignes = 0;
        // Tournoi publié : le règlement est figé, sa ligne laisse place à un lien de lecture.
        boolean reglementFige = index == 3 && !tournoi.reglementModifiable();
        for (Item it : s.items()) {
            if (reglementFige && it.libelle().startsWith("Règlement du tournoi")) {
                lignes.getChildren().add(lienReglementFige());
            } else {
                lignes.getChildren().add(ligneItem(it));
            }
            if (it.renseigne()) {
                renseignes++;
            }
        }

        Button modifier = Ui.boutonSecondaire(index == 1 ? "Gérer les tableaux" : index >= 2 ? "Gérer" : "Modifier");
        modifier.setOnAction(e -> afficherFormulaire(index, s.titre()));

        Label compte = Ui.discret(renseignes + " / " + s.items().size() + " renseignés");
        Node entete = Ui.enteteAvecActions(Ui.ligne(10, Ui.titreCarte(s.titre()), compte), modifier);
        return Ui.carte(entete, lignes);
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
