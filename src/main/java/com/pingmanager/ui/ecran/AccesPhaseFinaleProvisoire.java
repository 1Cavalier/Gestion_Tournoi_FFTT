package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

/**
 * PROVISOIRE : point d'entrée de test de la vue {@link VuePhaseFinale}, à
 * remplacer par la page Jour J. Liste les tableaux d'un tournoi ; pour chacun,
 * on ouvre sa vraie phase finale (enregistrée dans le fichier du tournoi) ou un
 * <b>essai</b> en mémoire ({@link TableauEssaiPhaseFinale}), utile tant que le
 * lancement des poules n'existe pas à l'écran.
 */
final class AccesPhaseFinaleProvisoire {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable retour;
    private final BorderPane racine = new BorderPane();
    private Tournoi tournoi;

    private AccesPhaseFinaleProvisoire(ContexteApplication contexte, Path fichier, Runnable retour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.retour = retour;
    }

    static Parent creer(ContexteApplication contexte, Path fichier, Runnable retour) {
        AccesPhaseFinaleProvisoire page = new AccesPhaseFinaleProvisoire(contexte, fichier, retour);
        page.afficherListe();
        return page.racine;
    }

    private void afficherListe() {
        tournoi = contexte.chargerTournoi(fichier);
        ServicePhaseFinale service = new ServicePhaseFinale();

        Button revenir = Ui.boutonSecondaire("Retour aux tournois");
        revenir.setOnAction(e -> retour.run());
        VBox titres = new VBox(4, Ui.titre("Phase finale (provisoire)"), Ui.discret(tournoi.getNom()));

        VBox liste = new VBox(0);
        if (tournoi.getTableaux().isEmpty()) {
            liste.getChildren().add(Ui.discret("Ce tournoi n'a pas encore de tableau."));
        }
        for (Tableau t : tournoi.getTableaux()) {
            DiagnosticPhaseFinale d = service.diagnostiquer(t);
            VBox infos = new VBox(2, Ui.gras(t.getNom()),
                    Ui.discret(PresentationPhaseFinale.titreEtat(d) + " · "
                            + PresentationPhaseFinale.pluriel(t.getInscrits().size(), "inscrit", "inscrits")));
            Button ouvrir = Ui.boutonSecondaire("Phase finale");
            ouvrir.setOnAction(e -> ouvrir(t, false));
            Button essai = Ui.boutonLien("Essai (non enregistré)");
            essai.setOnAction(e -> ouvrir(t, true));
            liste.getChildren().add(Ui.ligneListe(infos, Ui.espace(), essai, ouvrir));
        }

        VBox contenu = Ui.page(820,
                Ui.enteteAvecActions(titres, revenir),
                rappel(),
                Ui.carte("Tableaux", liste));
        racine.setTop(null);
        racine.setCenter(Ui.defilant(contenu));
    }

    private static Node rappel() {
        VBox v = new VBox(Ui.texte("Accès de test, en attendant la page Jour J. « Phase finale » ouvre le vrai "
                + "tableau : le tirage et les scores sont enregistrés dans le tournoi. « Essai » rejoue le "
                + "tableau en mémoire avec des poules jouées au hasard, sans rien enregistrer."));
        v.getStyleClass().add("rappel-regles");
        return v;
    }

    private void ouvrir(Tableau tableau, boolean essai) {
        Tableau cible = essai ? TableauEssaiPhaseFinale.creer(tableau, System.nanoTime()) : tableau;
        Button revenir = Ui.boutonLien("← Tous les tableaux");
        revenir.setOnAction(e -> afficherListe());
        HBox barre = Ui.ligne(8, revenir);
        barre.setPadding(new Insets(12, 28, 0, 28));
        racine.setTop(barre);
        racine.setCenter(VuePhaseFinale.creer(contexte, tournoi, cible, essai ? null : fichier));
    }
}
