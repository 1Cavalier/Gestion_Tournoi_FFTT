package com.pingmanager.core;

import java.text.Normalizer;

/**
 * Longueur et alphabet d'un SMS (B019).
 *
 * <p>Un SMS fait 160 caractères dans l'alphabet GSM 7 bits (GSM 03.38), ou 70
 * en Unicode (UCS-2) dès qu'un seul caractère sort de cet alphabet. Au-delà,
 * le message est découpé en segments de 153 (GSM) ou 67 (UCS-2) caractères,
 * chacun facturé comme un SMS.
 * Plusieurs lettres françaises (ê, â, î, ô, û, ë, ï, ç minuscule, œ) ne sont pas
 * dans l'alphabet GSM : {@link #versGsm7(String)} les remplace pour que les
 * convocations tiennent dans un seul SMS à 160 caractères.</p>
 */
public final class TexteSms {

    /** Longueur d'un SMS unique en GSM 7 bits. */
    public static final int LONGUEUR_GSM7 = 160;
    /** Longueur d'un SMS unique en UCS-2. */
    public static final int LONGUEUR_UCS2 = 70;
    private static final int SEGMENT_GSM7 = 153;
    private static final int SEGMENT_UCS2 = 67;

    /** Alphabet GSM 03.38 de base (un septet par caractère). */
    private static final String GSM7_BASE = "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
            + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";
    /** Table d'extension GSM (deux septets par caractère : échappement + code). */
    private static final String GSM7_EXTENSION = "^{}\\[~]|€\f";

    private TexteSms() {
    }

    /** Le texte ne contient-il que des caractères de l'alphabet GSM 7 bits (base ou extension) ? */
    public static boolean estGsm7(String texte) {
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (GSM7_BASE.indexOf(c) < 0 && GSM7_EXTENSION.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Longueur facturée : en septets si le texte est GSM 7 bits (deux pour un
     * caractère d'extension comme €), sinon en unités UTF-16.
     */
    public static int longueur(String texte) {
        if (!estGsm7(texte)) {
            return texte.length();
        }
        int n = 0;
        for (int i = 0; i < texte.length(); i++) {
            n += GSM7_EXTENSION.indexOf(texte.charAt(i)) >= 0 ? 2 : 1;
        }
        return n;
    }

    /** Nombre de SMS facturés pour ce texte (0 pour un texte vide). */
    public static int nombreDeSms(String texte) {
        int l = longueur(texte);
        if (l == 0) {
            return 0;
        }
        boolean gsm = estGsm7(texte);
        int unique = gsm ? LONGUEUR_GSM7 : LONGUEUR_UCS2;
        int segment = gsm ? SEGMENT_GSM7 : SEGMENT_UCS2;
        return l <= unique ? 1 : (l + segment - 1) / segment;
    }

    /**
     * Ramène un texte à l'alphabet GSM 7 bits : lettres accentuées absentes de
     * l'alphabet remplacées par leur lettre de base (ê → e, Ê → E, ç → c,
     * œ → oe), apostrophes et guillemets typographiques par leurs équivalents
     * simples, tirets longs par « - », espaces insécables par des espaces.
     * Les lettres présentes (é, è, à, ù, É…) sont gardées. Un caractère sans
     * équivalent devient « ? ».
     */
    public static String versGsm7(String texte) {
        StringBuilder sb = new StringBuilder(texte.length());
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (GSM7_BASE.indexOf(c) >= 0 || GSM7_EXTENSION.indexOf(c) >= 0) {
                sb.append(c);
            } else {
                sb.append(remplacement(c));
            }
        }
        return sb.toString();
    }

    private static String remplacement(char c) {
        if (c == 'œ') {
            return "oe";
        }
        if (c == 'Œ') {
            return "OE";
        }
        if (c == 'ç') {
            return "c";
        }
        if (c == '’' || c == '‘' || c == '`' || c == '´') {
            return "'";
        }
        if (c == '«' || c == '»' || c == '“' || c == '”') {
            return "\"";
        }
        if (c == '–' || c == '—' || c == '−' || c == '·' || c == '•') {
            return "-";
        }
        if (c == ' ' || c == ' ' || c == '\t') {
            return " ";
        }
        if (c == '…') {
            return "...";
        }
        String decompose = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < decompose.length(); i++) {
            char d = decompose.charAt(i);
            if (Character.getType(d) != Character.NON_SPACING_MARK) {
                base.append(d);
            }
        }
        String b = base.toString();
        return !b.isEmpty() && estGsm7(b) ? b : "?";
    }
}
