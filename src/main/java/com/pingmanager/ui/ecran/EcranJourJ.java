package com.pingmanager.ui.ecran;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
 * <p>Écran principal unique du juge-arbitre (B011, {@link EcranSalle} avec le
 * bandeau « Où en sont les tableaux ») : matchs proposés tous tableaux
 * confondus, tables, saisie du score sur place. Le bandeau donne l'étape de
 * chaque tableau, jour par jour et dans l'ordre des horaires, et ouvre les vues
 * secondaires :</p>
 * <ul>
 *   <li>ouvrir le pointage (tableau par tableau) ;</li>
 *   <li>pointer, clôturer, puis lancer le tirage au sort
 *       ({@link EcranPointageTableau}) ;</li>
 *   <li>suivre les poules et saisir les scores ({@link EcranPoulesTableau}) ;</li>
 *   <li>construire le tableau final et suivre l'arbre ({@link VuePhaseFinale}).</li>
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
        EcranSalle principal = new EcranSalle(tournoi, this::enregistrer, onRetour, libelleRetour, "Jour J",
                this::bandeauTableaux, Horloge.systeme()).surOuvrirTableau(this::ouvrirTableau);
        racine.setCenter(principal.build());
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
     * Bandeau « Où en sont les tableaux » : une ligne par tableau (étape, avancement,
     * jauge, parties en jeu) ; un clic ouvre le détail. Reconstruit par l'écran
     * principal après chaque action, donc toujours à jour.
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
                lignes.getChildren().add(ligneAvancement(l, premiere));
                premiere = false;
            }
        }
        Node entete = Ui.titreCarte("Où en sont les tableaux");
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
            entete = Ui.ligne(10, Ui.titreCarte("Où en sont les tableaux"), jour, Ui.espace(), precedent, suivant);
        }
        VBox carte = Ui.carte(entete, lignes);
        carte.getStyleClass().add("avancement");
        if (tournoi.getStatut() == StatutTournoi.ANNULE) {
            carte.getChildren().add(Ui.encartInfo("Ce tournoi est annulé : le pointage et les tirages sont fermés."));
        }
        return carte;
    }

    private void changerJour(int index) {
        jourAffiche = index;
        afficherListe();
    }

    /** Une ligne du bandeau : nom, étape, avancement, jauge, bouton de détail. */
    private Node ligneAvancement(PresentationAvancement.Ligne l, boolean premiere) {
        Tableau t = l.tableau();
        Button nom = Ui.boutonLien(t.getNom());
        nom.getStyleClass().add("avancement-nom");
        nom.setTooltip(new Tooltip(PresentationJourJ.horaires(t)));
        Label etape = Ui.pastille(l.libelleEtape(), l.classeEtape());
        etape.getStyleClass().add("avancement-etape");
        HBox haut = Ui.ligne(10, nom, etape);
        String enJeu = PresentationAvancement.libelleEnJeu(l.enJeu());
        if (!enJeu.isEmpty()) {
            haut.getChildren().add(Ui.pastille(enJeu, StatutPastille.INFO));
        }
        Label detail = new Label(l.detail());
        detail.getStyleClass().add("avancement-detail");
        VBox gauche = new VBox(2, haut, detail);
        HBox.setHgrow(gauche, Priority.ALWAYS);
        gauche.setMaxWidth(Double.MAX_VALUE);

        ProgressBar jauge = new ProgressBar(Math.max(0, l.jauge()));
        jauge.getStyleClass().addAll("jauge", "avancement-jauge");
        jauge.setPrefWidth(120);
        jauge.setMinWidth(120);
        jauge.setPrefHeight(8);
        jauge.setVisible(l.aJauge());
        if (l.aJauge()) {
            Tooltip.install(jauge, new Tooltip(l.pourcentage()));
        }
        boolean ferme = tournoi.getStatut() == StatutTournoi.ANNULE;
        Button action = l.vue() == PresentationAvancement.Detail.OUVRIR_POINTAGE
                ? Ui.boutonPrimaire(l.libelleBouton()) : Ui.boutonSecondaire(l.libelleBouton());
        action.getStyleClass().add("avancement-action");
        action.setDisable(ferme && (l.vue() == PresentationAvancement.Detail.OUVRIR_POINTAGE
                || l.etape() == PresentationJourJ.Etape.POULES_TERMINEES));
        HBox ligne = Ui.ligne(14, gauche, jauge, action);

        VBox bloc = new VBox(8, ligne);
        bloc.getStyleClass().add("avancement-ligne");
        if (premiere) {
            bloc.getStyleClass().add("avancement-ligne-premiere");
        }
        Runnable ouvrir = () -> ouvrirDetail(l, bloc);
        nom.setOnAction(e -> ouvrir.run());
        action.setOnAction(e -> ouvrir.run());
        // Toute la ligne est cliquable (hors boutons, qui ont leur propre action).
        bloc.setCursor(Cursor.HAND);
        bloc.setOnMouseClicked(e -> {
            if (!dansUnBouton(e.getTarget(), bloc) && !(ferme && action.isDisabled())) {
                ouvrir.run();
            }
        });
        return bloc;
    }

    private static boolean dansUnBouton(Object cible, Node limite) {
        Node n = cible instanceof Node noeud ? noeud : null;
        while (n != null && n != limite) {
            if (n instanceof ButtonBase) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    private void ouvrirDetail(PresentationAvancement.Ligne l, VBox bloc) {
        Tableau t = l.tableau();
        PresentationAvancement.Detail d = l.vue();
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
                apresTirage, () -> afficherTableauFinal(t)).build());
    }

    /**
     * Tableau final : second tirage si les poules viennent de se terminer, puis
     * arbre des rencontres et saisie des scores ({@link VuePhaseFinale}).
     */
    private void afficherTableauFinal(Tableau t) {
        racine.setCenter(VuePhaseFinale.creer(contexte, tournoi, t, fichier, this::afficherListe));
    }
}
