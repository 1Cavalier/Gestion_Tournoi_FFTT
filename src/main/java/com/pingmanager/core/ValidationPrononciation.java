package com.pingmanager.core;

import java.util.Locale;

/**
 * Vérifie une prononciation saisie en alphabet phonétique international
 * (API / IPA) avant de l'enregistrer (B018).
 *
 * <p>Liste blanche : lettres minuscules latines, symboles API (bloc « IPA
 * Extensions », lettres modificatives comme {@code ː ˈ ˌ}, diacritiques
 * combinants comme le tilde des voyelles nasales {@code ɑ̃ ɛ̃ ɔ̃ œ̃}), quelques
 * lettres utilisées par l'API ({@code æ ç ð ø ŋ œ β θ χ}), l'espace, le point
 * (séparateur de syllabes), la barre {@code |} et la liaison {@code ‿}. Tout le
 * reste est refusé, en particulier {@code < > & " '}, les chiffres et les
 * caractères de contrôle : rien ne peut casser le document SSML, même si le
 * générateur échappe encore le texte par sécurité.</p>
 */
public final class ValidationPrononciation {

    /** Longueur maximale d'une prononciation, en caractères (après normalisation). */
    public static final int LONGUEUR_MAX = 60;

    private ValidationPrononciation() {
    }

    /**
     * @param saisie texte saisi (null ou blanc = effacer la prononciation)
     * @return la prononciation normalisée, ou un refus avec motif
     */
    public static ResultatSaisiePrononciation verifier(String saisie) {
        if (saisie == null) {
            return ResultatSaisiePrononciation.acceptee("");
        }
        String ipa = saisie.strip().replaceAll("\\s+", " ");
        if (ipa.isEmpty()) {
            return ResultatSaisiePrononciation.acceptee("");
        }
        int longueur = ipa.codePointCount(0, ipa.length());
        if (longueur > LONGUEUR_MAX) {
            return ResultatSaisiePrononciation.refusee("Prononciation trop longue : " + longueur
                    + " caractères pour " + LONGUEUR_MAX + " au plus.");
        }
        int precedent = ' ';
        for (int i = 0; i < ipa.length(); ) {
            int c = ipa.codePointAt(i);
            if (Character.isUpperCase(c)) {
                return ResultatSaisiePrononciation.refusee("Pas de majuscule en alphabet phonétique : « "
                        + new String(Character.toChars(c)) + " » à écrire « "
                        + new String(Character.toChars(c)).toLowerCase(Locale.ROOT) + " ».");
            }
            if (estDiacritique(c)) {
                if (!estSymbole(precedent)) {
                    return ResultatSaisiePrononciation.refusee(
                            "Un signe diacritique (comme le tilde des nasales) doit suivre un symbole.");
                }
            } else if (!estSymbole(c) && !estSeparateur(c)) {
                return ResultatSaisiePrononciation.refusee("Caractère non autorisé : " + afficher(c)
                        + ". Utilisez les symboles de l'alphabet phonétique.");
            }
            precedent = c;
            i += Character.charCount(c);
        }
        return ResultatSaisiePrononciation.acceptee(ipa);
    }

    /** Ce caractère est-il accepté dans une prononciation (hors contexte) ? */
    public static boolean estAutorise(int c) {
        return estSymbole(c) || estSeparateur(c) || estDiacritique(c);
    }

    private static boolean estSymbole(int c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 0x0250 && c <= 0x02AF)   // IPA Extensions : ʁ ʃ ʒ ɲ ɛ ɔ ə ɑ ɥ ɡ…
                || (c >= 0x02B0 && c <= 0x02FF)   // lettres modificatives : ː ˈ ˌ ʰ…
                || c == 0x00E6 || c == 0x00E7 || c == 0x00F0 || c == 0x00F8   // æ ç ð ø
                || c == 0x0127 || c == 0x014B || c == 0x0153                  // ħ ŋ œ
                || c == 0x03B2 || c == 0x03B8 || c == 0x03C7;                 // β θ χ
    }

    private static boolean estSeparateur(int c) {
        return c == ' ' || c == '.' || c == '|' || c == 0x203F; // ‿ liaison
    }

    private static boolean estDiacritique(int c) {
        return c >= 0x0300 && c <= 0x036F;
    }

    private static String afficher(int c) {
        if (Character.isISOControl(c) || Character.isWhitespace(c) || !Character.isDefined(c)) {
            return String.format("U+%04X", c);
        }
        return "« " + new String(Character.toChars(c)) + " »";
    }
}
