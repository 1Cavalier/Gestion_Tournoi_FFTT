package com.pingmanager.persistence.json;

import java.util.List;
import java.util.Map;

/**
 * Sérialise un arbre {@link Json} en texte JSON valide, sans dépendance.
 *
 * <p>Deux modes : compact (une ligne) ou indenté (lisible, pour qu'un fichier
 * de tournoi puisse être ouvert et inspecté à la main en cas de pépin hors
 * ligne). Les nombres entiers sont écrits sans partie décimale superflue.</p>
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    /** Écrit en mode indenté (lisible). */
    public static String ecrire(Json racine) {
        return ecrire(racine, true);
    }

    public static String ecrire(Json racine, boolean indente) {
        StringBuilder sb = new StringBuilder();
        ecrireValeur(sb, racine, indente, 0);
        return sb.toString();
    }

    private static void ecrireValeur(StringBuilder sb, Json v, boolean indente, int niveau) {
        switch (v.getType().name()) {
            case "OBJET" -> ecrireObjet(sb, v.entrees(), indente, niveau);
            case "TABLEAU" -> ecrireTableau(sb, v.elements(), indente, niveau);
            case "CHAINE" -> ecrireChaine(sb, v.asString());
            case "NOMBRE" -> sb.append(formaterNombre(v.asDouble()));
            case "BOOLEEN" -> sb.append(v.asBoolean() ? "true" : "false");
            case "NUL" -> sb.append("null");
        }
    }

    private static void ecrireObjet(StringBuilder sb, Map<String, Json> entrees,
                                    boolean indente, int niveau) {
        if (entrees.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        int i = 0;
        for (Map.Entry<String, Json> e : entrees.entrySet()) {
            if (i++ > 0) {
                sb.append(',');
            }
            sautLigne(sb, indente, niveau + 1);
            ecrireChaine(sb, e.getKey());
            sb.append(indente ? ": " : ":");
            ecrireValeur(sb, e.getValue(), indente, niveau + 1);
        }
        sautLigne(sb, indente, niveau);
        sb.append('}');
    }

    private static void ecrireTableau(StringBuilder sb, List<Json> elements,
                                      boolean indente, int niveau) {
        if (elements.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sautLigne(sb, indente, niveau + 1);
            ecrireValeur(sb, elements.get(i), indente, niveau + 1);
        }
        sautLigne(sb, indente, niveau);
        sb.append(']');
    }

    private static void sautLigne(StringBuilder sb, boolean indente, int niveau) {
        if (indente) {
            sb.append('\n');
            for (int i = 0; i < niveau; i++) {
                sb.append("  ");
            }
        }
    }

    private static void ecrireChaine(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    /** Écrit un entier sans « .0 », sinon le double tel quel. */
    private static String formaterNombre(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new JsonException("JSON n'accepte pas NaN/Infinity.");
        }
        if (d == Math.rint(d) && !Double.isInfinite(d)
                && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }
}
