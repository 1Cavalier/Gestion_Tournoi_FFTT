package com.pingmanager.ui.ecran;

import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Page « Jour J » d'un tournoi (Gestion des tournois → tournoi → « Jour J »).
 *
 * <p>Liste des tableaux, jour par jour et dans l'ordre des horaires, avec leur
 * étape (inscriptions, pointage ouvert, prêt à lancer, poules en cours, poules
 * terminées, phase finale) et l'action suivante du juge-arbitre :</p>
 * <ul>
 *   <li>ouvrir le pointage (tableau par tableau) ;</li>
 *   <li>pointer, clôturer, puis lancer le tirage au sort
 *       ({@link EcranPointageTableau}) ;</li>
 *   <li>suivre les poules et saisir les scores ({@link EcranPoulesTableau}).</li>
 * </ul>
 *
 * <p>Le tournoi est relu depuis son fichier à chaque retour sur la liste ; chaque
 * action est enregistrée aussitôt.</p>
 */
final class EcranJourJ {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final String libelleRetour;
    private Tournoi tournoi;

    private final BorderPane racine = new BorderPane();

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
        return racine;
    }

    private void enregistrer() {
        contexte.enregistrerTournoi(tournoi, fichier);
    }

    // ------------------------------------------------------------------
    // Liste des tableaux
    // ------------------------------------------------------------------

    private void afficherListe() {
        tournoi = contexte.chargerTournoi(fichier);

        Button retour = Ui.boutonLien(libelleRetour);
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre("Jour J");
        Label nom = Ui.discret(tournoi.getNom() == null ? "" : tournoi.getNom());
        VBox entete = new VBox(6, retour, titre, nom);

        VBox contenu = Ui.page(860, entete, tuiles(),
                Ui.aide("Le juge-arbitre ouvre le pointage tableau par tableau, coche les présents et les "
                        + "paiements, clôture le pointage (les absents libèrent leur place, la liste d'attente est "
                        + "repêchée), puis lance le tirage au sort des poules. Tout est enregistré au fur et à mesure."));

        if (tournoi.getStatut() == StatutTournoi.ANNULE) {
            contenu.getChildren().add(Ui.encartInfo("Ce tournoi est annulé : le pointage et les tirages sont fermés."));
        }
        if (tournoi.getTableaux().isEmpty()) {
            contenu.getChildren().add(Ui.carte("Tableaux",
                    Ui.discret("Aucun tableau dans ce tournoi. Créez-les dans « Tableaux et inscriptions ».")));
        }

        for (Map.Entry<LocalDate, List<Tableau>> jour : PresentationJourJ.tableauxParJour(tournoi).entrySet()) {
            Label intertitre = Ui.sousTitre(PresentationJourJ.jour(jour.getKey()));
            intertitre.getStyleClass().add("intertitre-jour");
            VBox lignes = new VBox(0);
            for (Tableau t : jour.getValue()) {
                lignes.getChildren().add(ligneTableau(t));
            }
            contenu.getChildren().add(new VBox(4, intertitre, Ui.carteNue(lignes)));
        }
        racine.setCenter(Ui.defilant(contenu));
    }

    /** Chiffres clés : tableaux en pointage, lancés, terminés. */
    private Node tuiles() {
        int pointage = 0;
        int poules = 0;
        int termines = 0;
        for (Tableau t : tournoi.getTableaux()) {
            PresentationJourJ.Etape e = PresentationJourJ.etape(t);
            if (e == PresentationJourJ.Etape.POINTAGE_OUVERT || e == PresentationJourJ.Etape.POINTAGE_CLOTURE) {
                pointage++;
            } else if (e == PresentationJourJ.Etape.POULES) {
                poules++;
            } else if (e == PresentationJourJ.Etape.POULES_TERMINEES || e == PresentationJourJ.Etape.PHASE_FINALE) {
                termines++;
            }
        }
        int n = tournoi.getTableaux().size();
        FlowPane f = new FlowPane(12, 12,
                Ui.tuile(String.valueOf(n), n > 1 ? "tableaux" : "tableau"),
                Ui.tuile(String.valueOf(pointage), "en pointage"),
                Ui.tuile(String.valueOf(poules), "poules en cours"),
                Ui.tuile(String.valueOf(termines), "poules terminées"));
        return f;
    }

    /** Une ligne de tableau : nom, étape, horaires, résumé et action suivante. */
    private Node ligneTableau(Tableau t) {
        PresentationJourJ.Etape etape = PresentationJourJ.etape(t);
        Label pastille = Ui.pastille(PresentationJourJ.libelle(etape), PresentationJourJ.classe(etape));
        HBox titre = Ui.ligne(10, Ui.gras(t.getNom()), pastille);

        String resume = t.getStatut() == StatutTableau.LANCE || t.getStatut() == StatutTableau.PHASE_FINALE
                ? PresentationJourJ.resumePoules(t) : PresentationJourJ.resumePointage(t);
        VBox infos = new VBox(3, titre, Ui.discret(PresentationJourJ.horaires(t)), Ui.discret(resume));
        HBox.setHgrow(infos, Priority.ALWAYS);

        VBox actions = new VBox(6);
        VBox bloc = new VBox(8);
        boolean ferme = tournoi.getStatut() == StatutTournoi.ANNULE;

        if (etape == PresentationJourJ.Etape.INSCRIPTION) {
            Button ouvrir = Ui.boutonPrimaire("Ouvrir le pointage");
            ouvrir.setDisable(ferme);
            ouvrir.setOnAction(e -> confirmerOuverture(t, bloc));
            actions.getChildren().add(ouvrir);
        } else if (etape == PresentationJourJ.Etape.POINTAGE_OUVERT || etape == PresentationJourJ.Etape.POINTAGE_CLOTURE) {
            Button pointage = Ui.boutonPrimaire(etape == PresentationJourJ.Etape.POINTAGE_OUVERT
                    ? "Pointer" : "Lancer le tirage");
            pointage.setOnAction(e -> afficherPointage(t));
            actions.getChildren().add(pointage);
        } else {
            Button poules = Ui.boutonPrimaire("Poules");
            poules.setOnAction(e -> afficherPoules(t, false));
            Button pointage = Ui.boutonLien("Liste de pointage");
            pointage.setOnAction(e -> afficherPointage(t));
            actions.getChildren().addAll(poules, pointage);
        }
        bloc.getChildren().add(Ui.ligne(16, infos, actions));
        HBox ligne = Ui.ligneListe(bloc);
        HBox.setHgrow(bloc, Priority.ALWAYS);
        return ligne;
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
                apresTirage).build());
    }
}
