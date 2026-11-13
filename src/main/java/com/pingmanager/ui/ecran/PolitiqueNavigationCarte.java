package com.pingmanager.ui.ecran;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Politique de navigation de la carte des tournois (README section 5, point 19),
 * sans JavaFX pour être testée seule : la WebView ne doit afficher que sa page
 * locale (chargée par {@code loadContent}, adresse vide), le pont Java n'est
 * posé que sur cette page, et toute autre adresse est bloquée. Un lien web
 * (http ou https) cliqué par l'utilisateur s'ouvre dans le navigateur du système.
 */
final class PolitiqueNavigationCarte {

    /** Ce que la carte fait d'une adresse. */
    enum Decision {
        /** La page locale de la carte : affichée, pont Java autorisé. */
        PAGE_LOCALE,
        /** Lien web : jamais dans la WebView, ouvert dans le navigateur du système sur clic. */
        NAVIGATEUR_SYSTEME,
        /** Tout le reste (file:, jar:, javascript:, data:, adresse mal formée…) : bloqué. */
        BLOQUEE
    }

    private PolitiqueNavigationCarte() {
    }

    /**
     * Page locale de la carte : {@code loadContent} donne une adresse vide
     * (vérifié sous JavaFX 21) ; {@code about:blank} est accepté pour la même page.
     */
    static boolean estPageLocale(String adresse) {
        return adresse == null || adresse.isEmpty() || "about:blank".equals(adresse);
    }

    /** Le pont Java ({@code window.java}) ne se pose que sur la page locale. */
    static boolean pontAutorise(String adresse) {
        return estPageLocale(adresse);
    }

    static Decision decider(String adresse) {
        if (estPageLocale(adresse)) {
            return Decision.PAGE_LOCALE;
        }
        return lienWeb(adresse) ? Decision.NAVIGATEUR_SYSTEME : Decision.BLOQUEE;
    }

    /** http ou https, avec un hôte, sans identifiants dans l'adresse ni caractère de contrôle. */
    private static boolean lienWeb(String adresse) {
        if (adresse.length() > 2048 || adresse.chars().anyMatch(c -> c < 0x20 || c == 0x7F)) {
            return false;
        }
        try {
            URI u = new URI(adresse);
            String schema = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
            return ("http".equals(schema) || "https".equals(schema))
                    && u.getHost() != null && !u.getHost().isEmpty()
                    && u.getRawUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
