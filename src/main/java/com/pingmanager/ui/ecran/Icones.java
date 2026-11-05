package com.pingmanager.ui.ecran;

/**
 * Icônes simples de l'interface : tracés SVG sur une grille de 16 × 16,
 * dessinés pour PingManager (formes géométriques de base). On les pose avec
 * {@link Ui#icone(String, String)} ; la couleur vient de {@code theme.css}.
 */
final class Icones {

    private Icones() {
    }

    static final String ACCUEIL = "M8 1.5 L15 7.5 L13.5 7.5 L13.5 14.5 L9.5 14.5 L9.5 10 L6.5 10 "
            + "L6.5 14.5 L2.5 14.5 L2.5 7.5 L1 7.5 Z";
    static final String PROFIL = "M8 1.5 A3.25 3.25 0 1 1 7.99 1.5 Z M1.5 14.5 C1.5 10.5 4.5 9 8 9 "
            + "C11.5 9 14.5 10.5 14.5 14.5 Z";
    static final String TOURNOIS = "M8 1 C5.2 1 3 3.2 3 6 C3 9.8 8 15 8 15 C8 15 13 9.8 13 6 C13 3.2 10.8 1 8 1 Z "
            + "M8 3.8 A2.2 2.2 0 1 0 8.01 3.8 Z";
    static final String STATS = "M1.5 14.5 L1.5 8.5 L4.5 8.5 L4.5 14.5 Z M6.5 14.5 L6.5 3 L9.5 3 L9.5 14.5 Z "
            + "M11.5 14.5 L11.5 6 L14.5 6 L14.5 14.5 Z";
    static final String GESTION = "M1.5 3 L14.5 3 L14.5 14.5 L1.5 14.5 Z M3 6.5 L3 13 L13 13 L13 6.5 Z "
            + "M4.5 1 L6 1 L6 4.5 L4.5 4.5 Z M10 1 L11.5 1 L11.5 4.5 L10 4.5 Z";
    static final String HISTORIQUE = "M8 1 A7 7 0 1 1 7.99 1 Z M8 2.6 A5.4 5.4 0 1 0 8.01 2.6 Z "
            + "M7.2 4 L8.8 4 L8.8 7.6 L11.2 9.2 L10.4 10.5 L7.2 8.4 Z";
    static final String BUVETTE = "M2.5 4 L11 4 L11 10 C11 12.4 9.3 14 6.75 14 C4.2 14 2.5 12.4 2.5 10 Z "
            + "M11 5.5 L12.5 5.5 C14.3 5.5 14.3 9.5 12.5 9.5 L11 9.5 L11 8.2 L12.3 8.2 C12.8 8.2 12.8 6.8 12.3 6.8 L11 6.8 Z";
    /** Calendrier (même tracé que GESTION) avec une date marquée. */
    static final String AGENDA = "M1.5 3 L14.5 3 L14.5 14.5 L1.5 14.5 Z M3 6.5 L3 13 L13 13 L13 6.5 Z "
            + "M4.5 1 L6 1 L6 4.5 L4.5 4.5 Z M10 1 L11.5 1 L11.5 4.5 L10 4.5 Z M4.5 8 L7.5 8 L7.5 11 L4.5 11 Z";
    static final String SUPERVISION = "M8 1 L14 3.5 L14 8 C14 11.4 11.4 14 8 15 C4.6 14 2 11.4 2 8 L2 3.5 Z";
}
