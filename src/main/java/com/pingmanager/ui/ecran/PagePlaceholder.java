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
        VBox boite = Ui.page(620,
                Ui.ligne(12, Ui.titre(titre), Ui.badge("À venir")),
                Ui.texte(message));
        return Ui.defilant(boite);
    }
}
