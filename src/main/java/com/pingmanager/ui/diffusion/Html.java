package com.pingmanager.ui.diffusion;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Outils HTML du suivi sur téléphone : échappement systématique et lecture bornée des paramètres. */
public final class Html {

    /** Longueur maximale gardée d'une valeur de paramètre (recherche par nom ou licence). */
    public static final int LONGUEUR_MAX_PARAMETRE = 60;
    /** Longueur maximale acceptée d'une chaîne de requête entière. */
    public static final int LONGUEUR_MAX_REQUETE = 512;
    /** Nombre maximal de paramètres lus dans une requête. */
    public static final int NOMBRE_MAX_PARAMETRES = 8;

    private Html() {
    }

    /** Échappe un texte pour le placer dans le corps HTML ou dans un attribut entre guillemets. */
    public static String echapper(String texte) {
        if (texte == null || texte.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texte.length() + 16);
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&#39;");
            } else if (c < 0x20 && c != '\n' && c != '\t') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Lit la chaîne de requête brute ({@code q=dupont&id=3}) : au plus
     * {@link #NOMBRE_MAX_PARAMETRES} paramètres, valeurs coupées à
     * {@link #LONGUEUR_MAX_PARAMETRE} caractères ; une chaîne trop longue ou
     * mal encodée donne une table vide.
     */
    public static Map<String, String> parametres(String requeteBrute) {
        Map<String, String> res = new LinkedHashMap<>();
        if (requeteBrute == null || requeteBrute.isEmpty() || requeteBrute.length() > LONGUEUR_MAX_REQUETE) {
            return res;
        }
        for (String paire : requeteBrute.split("&", NOMBRE_MAX_PARAMETRES + 1)) {
            if (res.size() >= NOMBRE_MAX_PARAMETRES) {
                break;
            }
            int egal = paire.indexOf('=');
            String cle = egal < 0 ? paire : paire.substring(0, egal);
            String valeur = egal < 0 ? "" : paire.substring(egal + 1);
            try {
                cle = URLDecoder.decode(cle, StandardCharsets.UTF_8);
                valeur = URLDecoder.decode(valeur, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException malEncode) {
                continue;
            }
            valeur = valeur.strip();
            if (valeur.length() > LONGUEUR_MAX_PARAMETRE) {
                valeur = valeur.substring(0, LONGUEUR_MAX_PARAMETRE);
            }
            if (!cle.isEmpty()) {
                res.putIfAbsent(cle, valeur);
            }
        }
        return res;
    }
}
