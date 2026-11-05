package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * « Mes tournois » : en tant que <b>joueur</b>, les tournois où la licence liée est
 * inscrite (ou en liste d'attente) ; en tant que <b>club</b>, ceux qu'il organise.
 * Chaque groupe est réparti en à venir, en cours et passés.
 */
final class MesTournois {

    private final ContexteApplication contexte;
    private final List<ContexteApplication.TournoiFichier> tous;
    private final Consumer<String> ouvrirFiche;

    MesTournois(ContexteApplication contexte, List<ContexteApplication.TournoiFichier> tous, Consumer<String> ouvrirFiche) {
        this.contexte = contexte;
        this.tous = tous;
        this.ouvrirFiche = ouvrirFiche;
    }

    Node build() {
        LocalDate auj = LocalDate.now();
        VBox page = Ui.page(900);

        Optional<FicheLicence> fiche = contexte.ficheLicenceLiee();
        page.getChildren().add(intertitre("Comme joueur"));
        if (fiche.isEmpty()) {
            page.getChildren().add(Ui.carteNue(Ui.discret("Liez votre licence dans « Mon profil » pour retrouver ici "
                    + "les tournois auxquels vous êtes inscrit.")));
        } else {
            List<ContexteApplication.TournoiFichier> joueur = contexte.mesTournoisJoueur(tous);
            joueur.removeIf(tf -> tf.tournoi().getStatut() == StatutTournoi.BROUILLON);
            ajouterSections(page, joueur, auj, tf -> carteJoueur(tf, fiche.get()),
                    "Vous n'êtes inscrit à aucun tournoi. Trouvez-en un dans « Trouver un tournoi ».");
        }

        if (contexte.estOrganisateur()) {
            page.getChildren().add(intertitre("Organisés par mon club"));
            ajouterSections(page, contexte.mesTournoisClub(tous), auj, this::carteClub,
                    "Votre club n'organise aucun tournoi pour l'instant.");
        }
        return Ui.defilant(page);
    }

    private void ajouterSections(VBox page, List<ContexteApplication.TournoiFichier> liste, LocalDate auj,
                                 java.util.function.Function<ContexteApplication.TournoiFichier, Node> carte, String vide) {
        if (liste.isEmpty()) {
            page.getChildren().add(Ui.carteNue(Ui.discret(vide)));
            return;
        }
        List<ContexteApplication.TournoiFichier> aVenir = new ArrayList<>();
        List<ContexteApplication.TournoiFichier> enCours = new ArrayList<>();
        List<ContexteApplication.TournoiFichier> passes = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : liste) {
            CalendrierTournoi.Moment m = CalendrierTournoi.moment(tf.tournoi(), auj);
            if (m == CalendrierTournoi.Moment.EN_COURS) {
                enCours.add(tf);
            } else if (m == CalendrierTournoi.Moment.PASSE) {
                passes.add(tf);
            } else {
                aVenir.add(tf);
            }
        }
        Comparator<ContexteApplication.TournoiFichier> parDate = Comparator.comparing(
                tf -> CalendrierTournoi.debut(tf.tournoi()) == null ? LocalDate.MAX : CalendrierTournoi.debut(tf.tournoi()));
        aVenir.sort(parDate);
        enCours.sort(parDate);
        passes.sort(parDate.reversed());
        section(page, "En cours", enCours, carte);
        section(page, "À venir", aVenir, carte);
        section(page, "Passés", passes, carte);
    }

    private static Label intertitre(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("intertitre-groupe");
        return l;
    }

    private static void section(VBox page, String titre, List<ContexteApplication.TournoiFichier> liste,
                                java.util.function.Function<ContexteApplication.TournoiFichier, Node> carte) {
        VBox v = new VBox(8, Ui.titreAvecCompteur(titre, liste.size()));
        if (liste.isEmpty()) {
            v.getChildren().add(Ui.discret("Aucun."));
        }
        liste.forEach(tf -> v.getChildren().add(carte.apply(tf)));
        page.getChildren().add(v);
    }

    private Node carteJoueur(ContexteApplication.TournoiFichier tf, FicheLicence fiche) {
        Tournoi t = tf.tournoi();
        RemplissageTableaux.Participant p = RemplissageTableaux.participant(t, fiche.getNumeroLicence()).orElseThrow();
        FlowPane puces = new FlowPane(6, 4);
        for (Tableau tab : p.inscrit()) {
            puces.getChildren().add(BlocsInscriptions.puceTableau(tab, null));
        }
        for (Tableau tab : p.enAttente()) {
            puces.getChildren().add(BlocsInscriptions.puceTableau(tab, tab.getFileAttente().indexOf(p.joueur()) + 1));
        }
        Label paiement = BlocsInscriptions.pastillePaiement(t, p);
        return ligne(tf, puces, paiement);
    }

    private Node carteClub(ContexteApplication.TournoiFichier tf) {
        Tournoi t = tf.tournoi();
        int inscrits = 0;
        int places = 0;
        for (Tableau tab : t.getTableaux()) {
            inscrits += tab.getInscrits().size();
            places += Math.max(0, tab.getConfiguration().getPlacesMax());
        }
        Label remplissage = Ui.discret(RemplissageTableaux.nombreDeParticipants(t) + " joueurs · " + inscrits + " / "
                + places + " places");
        return ligne(tf, remplissage, Ui.badge(PageSupervision.libelleStatut(t.getStatut())));
    }

    private Node ligne(ContexteApplication.TournoiFichier tf, Node detail, Node etat) {
        Tournoi t = tf.tournoi();
        String date = BlocsInscriptions.periode(t);
        String lieu = t.getAdresse() == null || t.getAdresse().getVille() == null ? "" : " · " + t.getAdresse().getVille();
        VBox texte = new VBox(4, Ui.gras(t.getNom()), Ui.discret(date + lieu), detail);
        HBox.setHgrow(texte, Priority.ALWAYS);
        Button voir = Ui.boutonSecondaire(
                PresentationSuiviTournoi.afficherSuivi(t) ? "Suivre le tournoi" : "Voir la fiche");
        voir.setDisable(t.getStatut() != StatutTournoi.PUBLIE);
        voir.setOnAction(e -> ouvrirFiche.accept(tf.fichier().toString()));
        HBox h = Ui.ligne(14, texte, etat, voir);
        VBox c = Ui.carteNue(h);
        return c;
    }
}
