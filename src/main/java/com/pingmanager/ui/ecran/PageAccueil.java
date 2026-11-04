package com.pingmanager.ui.ecran;

import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Parent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Page d'accueil (vue joueur, visible de tous). Deux colonnes : à gauche la vie
 * du joueur (agenda, prochains tournois, notes personnelles), à droite la
 * recherche de tournois sur carte. Contenu encore en attente — la structure est
 * posée ; la présentation vient de {@code theme.css}.
 */
public final class PageAccueil {

    private final ContexteApplication contexte;

    public PageAccueil(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    public Parent build() {
        VBox colonneGauche = new VBox(16,
                Ui.carte("Agenda",
                        Ui.discret("Vos prochaines échéances (tournois, clôtures d'inscription) "
                                + "apparaîtront ici."),
                        Ui.badge("À venir")),
                Ui.carte("Prochains tournois",
                        Ui.discret("Les tournois à venir auxquels vous êtes inscrit seront listés "
                                + "ici, avec la date et le lieu."),
                        Ui.badge("À venir")),
                Ui.carte("Mes notes",
                        Ui.discret("Un espace de notes personnelles (objectifs, rappels) sera "
                                + "disponible ici."),
                        Ui.badge("À venir")));

        VBox colonneDroite = new VBox(16,
                Ui.carte("Rechercher un tournoi",
                        Ui.discret("La carte interactive permettra de trouver les tournois ouverts "
                                + "aux inscriptions près de chez vous, avec des filtres (date, "
                                + "niveau, catégorie)."),
                        Ui.badge("À venir")));

        GridPane grille = new GridPane();
        grille.setHgap(16);
        grille.add(colonneGauche, 0, 0);
        grille.add(colonneDroite, 1, 0);

        ColumnConstraints gauche = new ColumnConstraints();
        gauche.setPercentWidth(58);
        ColumnConstraints droite = new ColumnConstraints();
        droite.setPercentWidth(42);
        grille.getColumnConstraints().addAll(gauche, droite);

        VBox contenu = Ui.page(Double.MAX_VALUE,
                Ui.titre("Bonjour " + contexte.compteConnecte().getPrenom()),
                grille);
        VBox.setVgrow(grille, Priority.ALWAYS);
        return Ui.defilant(contenu);
    }
}
