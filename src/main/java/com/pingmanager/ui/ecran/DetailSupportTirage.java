package com.pingmanager.ui.ecran;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Détail discret « Détail technique (support) », replié par défaut, sous une
 * carte de tirage au sort : il contient la référence technique du tirage
 * (graine), que l'utilisateur courant n'a pas à connaître. Textes dans
 * {@link PresentationTirage}.
 */
final class DetailSupportTirage {

    static final String OUVRIR = "Détail technique (support)";
    static final String FERMER = "Masquer le détail technique";

    private DetailSupportTirage() {
    }

    /** @return un lien qui déplie ou replie le contenu donné (replié au départ). */
    static Node creer(Node... contenu) {
        VBox corps = new VBox(6, contenu);
        Ui.cacher(corps);
        Button lien = Ui.boutonLien(OUVRIR);
        lien.setOnAction(e -> {
            boolean ouvrir = !corps.isVisible();
            Ui.afficherSi(corps, ouvrir);
            lien.setText(ouvrir ? FERMER : OUVRIR);
        });
        return new VBox(4, lien, corps);
    }
}
