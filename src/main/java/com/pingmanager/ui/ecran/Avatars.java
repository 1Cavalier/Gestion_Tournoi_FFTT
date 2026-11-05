package com.pingmanager.ui.ecran;

import java.util.Locale;

/**
 * Logique des avatars à initiales (charte v2), sans JavaFX : les initiales à
 * afficher et la teinte, tirée du nom parmi une petite gamme de bleus
 * ({@code avatar-teinte-1} à {@code avatar-teinte-N} dans {@code theme.css}).
 */
final class Avatars {

    /** Nombre de teintes déclarées dans theme.css (-avatar-1 … -avatar-5). */
    static final int NOMBRE_TEINTES = 5;

    private Avatars() {
    }

    /**
     * Initiales : première lettre du prénom puis première lettre du nom, en
     * majuscules (« Quentin », « SOUMET » → « QS »). Un seul des deux connu :
     * une lettre. Aucun : « ? ».
     */
    static String initiales(String prenom, String nom) {
        String p = premiereLettre(prenom);
        String n = premiereLettre(nom);
        String s = p + n;
        return s.isEmpty() ? "?" : s;
    }

    /** Numéro de teinte (1 à {@link #NOMBRE_TEINTES}), stable pour un même nom. */
    static int teinte(String prenom, String nom) {
        String cle = normaliser(prenom) + " " + normaliser(nom);
        return Math.floorMod(cle.hashCode(), NOMBRE_TEINTES) + 1;
    }

    private static String premiereLettre(String s) {
        if (s == null) {
            return "";
        }
        for (int i = 0; i < s.length(); i++) {
            int c = s.codePointAt(i);
            if (Character.isLetter(c)) {
                return new String(Character.toChars(c)).toUpperCase(Locale.FRENCH);
            }
        }
        return "";
    }

    private static String normaliser(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.FRENCH);
    }
}
