package com.pingmanager.persistence.json;

/**
 * Analyseur JSON récursif descendant, sans dépendance.
 *
 * <p>Lit un texte JSON complet et le transforme en arbre {@link Json}. Couvre
 * la grammaire JSON standard (objets, tableaux, chaînes avec échappements et
 * séquences {@code \\uXXXX}, nombres, {@code true}/{@code false}/{@code null}).
 * Rejette clairement un texte mal formé via {@link JsonException}.</p>
 */
public final class JsonParser {

    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
    }

    /** Analyse un document JSON complet et renvoie sa racine. */
    public static Json analyser(String texte) {
        if (texte == null) {
            throw new JsonException("Texte JSON nul.");
        }
        JsonParser p = new JsonParser(texte);
        p.sauterEspaces();
        Json v = p.lireValeur();
        p.sauterEspaces();
        if (!p.fini()) {
            throw p.erreur("Contenu inattendu après la valeur racine");
        }
        return v;
    }

    // ------------------------------------------------------------------

    private Json lireValeur() {
        if (fini()) {
            throw erreur("Valeur attendue mais fin de texte");
        }
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> lireObjet();
            case '[' -> lireTableau();
            case '"' -> Json.of(lireChaine());
            case 't', 'f' -> lireBooleen();
            case 'n' -> lireNul();
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield lireNombre();
                }
                throw erreur("Caractère inattendu '" + c + "'");
            }
        };
    }

    private Json lireObjet() {
        Json obj = Json.objet();
        attendre('{');
        sauterEspaces();
        if (regarder() == '}') {
            pos++;
            return obj;
        }
        while (true) {
            sauterEspaces();
            if (regarder() != '"') {
                throw erreur("Clé de chaîne attendue dans l'objet");
            }
            String cle = lireChaine();
            sauterEspaces();
            attendre(':');
            sauterEspaces();
            obj.set(cle, lireValeur());
            sauterEspaces();
            char c = regarder();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                break;
            } else {
                throw erreur("',' ou '}' attendu dans l'objet");
            }
        }
        return obj;
    }

    private Json lireTableau() {
        Json arr = Json.tableau();
        attendre('[');
        sauterEspaces();
        if (regarder() == ']') {
            pos++;
            return arr;
        }
        while (true) {
            sauterEspaces();
            arr.add(lireValeur());
            sauterEspaces();
            char c = regarder();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                break;
            } else {
                throw erreur("',' ou ']' attendu dans le tableau");
            }
        }
        return arr;
    }

    private String lireChaine() {
        attendre('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (fini()) {
                throw erreur("Chaîne non terminée");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (fini()) {
                    throw erreur("Échappement non terminé");
                }
                char e = src.charAt(pos++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw erreur("Séquence \\u incomplète");
                        }
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ex) {
                            throw erreur("Séquence \\u invalide : " + hex);
                        }
                    }
                    default -> throw erreur("Échappement invalide '\\" + e + "'");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Json lireNombre() {
        int debut = pos;
        if (regarder() == '-') {
            pos++;
        }
        while (!fini() && estChiffreNombre(src.charAt(pos))) {
            pos++;
        }
        String s = src.substring(debut, pos);
        try {
            return Json.of(Double.parseDouble(s));
        } catch (NumberFormatException ex) {
            throw erreur("Nombre invalide : " + s);
        }
    }

    private static boolean estChiffreNombre(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'
                || c == '+' || c == '-';
    }

    private Json lireBooleen() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Json.of(true);
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Json.of(false);
        }
        throw erreur("Booléen attendu");
    }

    private Json lireNul() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return Json.nul();
        }
        throw erreur("'null' attendu");
    }

    // ------------------------------------------------------------------

    private char regarder() {
        if (fini()) {
            throw erreur("Fin de texte inattendue");
        }
        return src.charAt(pos);
    }

    private void attendre(char c) {
        if (fini() || src.charAt(pos) != c) {
            throw erreur("'" + c + "' attendu");
        }
        pos++;
    }

    private void sauterEspaces() {
        while (!fini()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private boolean fini() {
        return pos >= src.length();
    }

    private JsonException erreur(String message) {
        int ligne = 1;
        int col = 1;
        for (int i = 0; i < pos && i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                ligne++;
                col = 1;
            } else {
                col++;
            }
        }
        return new JsonException(message + " (ligne " + ligne + ", colonne " + col + ")");
    }
}
