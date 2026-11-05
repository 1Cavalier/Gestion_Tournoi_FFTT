package com.pingmanager.ui.ecran;

import javafx.scene.Parent;
import javafx.scene.layout.VBox;

/**
 * Page d'attente réutilisable : un titre et un message « à venir ». Sert pour
 * les onglets encore non développés (carte, tournois, outils d'organisation).
 */
public final class PagePlaceholder {

    private PagePlaceholder() {
    }

    public static Parent page(String titre, String message) {
        VBox boite = Ui.page(720,
                Ui.ligne(12, Ui.titre(titre), Ui.pastille("Bientôt", StatutPastille.INFO)),
                Ui.carteNue(Ui.etatVide(message, null)));
        return Ui.defilant(boite);
    }
}
