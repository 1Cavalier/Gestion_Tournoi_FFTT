package com.pingmanager.importation;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Outils communs aux fichiers d'échange FFTT provisoires (B023, format
 * documenté dans {@code docs/FORMAT_ECHANGES_FFTT.md}).
 *
 * <p><b>Entrée non fiable.</b> Taille de fichier plafonnée, cellules trop
 * longues signalées, BOM et espaces de bord retirés, colonnes lues par leur
 * nom (casse et accents ignorés), colonnes inconnues ignorées. Le découpage
 * CSV (guillemets, séparateur, encodage) est celui de {@link LecteurTableur},
 * qui accepte aussi .xlsx et .ods.</p>
 *
 * <p><b>Sortie.</b> Point-virgule, UTF-8 avec BOM, fin de ligne CRLF ; cellule
 * entre guillemets si elle contient « ; », « " » ou un saut de ligne ; une
 * cellule commençant par {@code = + - @}, une tabulation ou un retour chariot
 * est préfixée d'une apostrophe pour qu'un tableur ne l'exécute pas comme une
 * formule (injection de formule).</p>
 */
final class CsvFederal {

    /** Taille maximale d'un fichier importé (10 Mo). */
    static final int TAILLE_MAX_OCTETS = 10 * 1024 * 1024;
    /** Longueur maximale d'une cellule lue. */
    static final int LONGUEUR_MAX_CELLULE = 200;
    /** Nombre maximal de lignes de données lues. */
    static final int LIGNES_MAX = 20_000;

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.FRANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    private CsvFederal() {
    }

    /** Tableau lu : en-têtes normalisés → index, et lignes de données (numérotées à partir de 2). */
    static final class Table {
        private final Map<String, Integer> colonnes;
        final List<List<String>> lignes;
        /** Numéro de ligne dans le fichier (1 = en-têtes) de chaque ligne de données. */
        final List<Integer> numeros;

        Table(Map<String, Integer> colonnes, List<List<String>> lignes, List<Integer> numeros) {
            this.colonnes = colonnes;
            this.lignes = lignes;
            this.numeros = numeros;
        }

        boolean a(String entete) {
            return colonnes.containsKey(normaliser(entete));
        }

        /** @return la cellule nettoyée (jamais null, vide si absente). */
        String valeur(List<String> ligne, String entete) {
            Integer i = colonnes.get(normaliser(entete));
            if (i == null || i >= ligne.size()) {
                return "";
            }
            return nettoyer(ligne.get(i));
        }
    }

    /**
     * Lit un fichier d'échange et vérifie les colonnes obligatoires.
     *
     * @throws LecteurTableur.ImportException fichier illisible, trop gros, vide,
     *                                        ou colonne obligatoire absente
     */
    static Table lire(String nom, byte[] octets, List<String> obligatoires) throws LecteurTableur.ImportException {
        if (octets == null || octets.length == 0) {
            throw new LecteurTableur.ImportException("Le fichier est vide.");
        }
        if (octets.length > TAILLE_MAX_OCTETS) {
            throw new LecteurTableur.ImportException("Fichier trop volumineux (plus de 10 Mo).");
        }
        List<List<String>> brut = LecteurTableur.lire(nom, octets).lignes();
        int i = 0;
        while (i < brut.size() && estVide(brut.get(i))) {
            i++;
        }
        if (i >= brut.size()) {
            throw new LecteurTableur.ImportException("Aucune ligne d'en-têtes trouvée.");
        }
        Map<String, Integer> colonnes = new HashMap<>();
        List<String> entetes = brut.get(i);
        for (int c = 0; c < entetes.size() && c < 200; c++) {
            String e = normaliser(nettoyer(entetes.get(c)));
            if (!e.isEmpty()) {
                colonnes.putIfAbsent(e, c);
            }
        }
        List<String> manquantes = new ArrayList<>();
        for (String o : obligatoires) {
            if (!colonnes.containsKey(normaliser(o))) {
                manquantes.add(o);
            }
        }
        if (!manquantes.isEmpty()) {
            throw new LecteurTableur.ImportException("Colonnes obligatoires absentes : "
                    + String.join(", ", manquantes) + ".");
        }
        List<List<String>> lignes = new ArrayList<>();
        List<Integer> numeros = new ArrayList<>();
        for (int l = i + 1; l < brut.size() && lignes.size() < LIGNES_MAX; l++) {
            if (!estVide(brut.get(l))) {
                lignes.add(brut.get(l));
                numeros.add(l + 1);
            }
        }
        return new Table(colonnes, lignes, numeros);
    }

    /** @return true si une cellule de la ligne dépasse {@link #LONGUEUR_MAX_CELLULE}. */
    static boolean tropLongue(List<String> ligne) {
        for (String c : ligne) {
            if (c != null && c.length() > LONGUEUR_MAX_CELLULE) {
                return true;
            }
        }
        return false;
    }

    static boolean estVide(List<String> ligne) {
        for (String c : ligne) {
            if (c != null && !nettoyer(c).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Retire BOM, caractères de contrôle et espaces de bord. */
    static String nettoyer(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '﻿' || (Character.isISOControl(c))) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString().strip();
    }

    /** Minuscules, sans accents ni espaces multiples (comparaison des en-têtes et recherches). */
    static String normaliser(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** @return la date jj/mm/aaaa, ou null si la valeur ne respecte pas ce format. */
    static LocalDate date(String v) {
        try {
            return LocalDate.parse(v, DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    /** @return le CSV (BOM UTF-8, « ; », CRLF), cellules échappées. */
    static byte[] ecrire(List<String> entetes, List<List<String>> lignes) {
        StringBuilder sb = new StringBuilder("﻿");
        ligne(sb, entetes);
        for (List<String> l : lignes) {
            ligne(sb, l);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void ligne(StringBuilder sb, List<String> cellules) {
        for (int i = 0; i < cellules.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(cellule(cellules.get(i)));
        }
        sb.append("\r\n");
    }

    /** Échappe une cellule : neutralise les formules, puis met entre guillemets si besoin. */
    static String cellule(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty()) {
            char c = s.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
                s = "'" + s;
            }
        }
        if (s.indexOf(';') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
