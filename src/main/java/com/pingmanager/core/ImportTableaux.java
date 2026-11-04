package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture de la feuille « Tableaux » d'un classeur d'import : une ligne par tableau, avec
 * ses <b>points imposés</b> (minimum et/ou maximum ; les deux vides = toutes séries), son
 * public (mixte, dames, messieurs), son jour, son heure, ses places, sa liste d'attente et
 * son tarif. Seul le nom est obligatoire.
 *
 * <p>Les tableaux ainsi définis sont créés à l'import (s'ils n'existent pas déjà) et les
 * joueurs de la feuille « Joueurs » y sont contrôlés : points et sexe du tableau, capacité.</p>
 */
public final class ImportTableaux {

    /** Un tableau défini dans la feuille. Valeurs nulles = non renseignées. */
    public record Definition(int ligne, String nom, Integer pointsMin, Integer pointsMax, Sexe sexe,
                             LocalDate jour, LocalTime heure, Integer places, Integer attente, Double tarif) {

        /** « 500 à 899 pts », « ≤ 1299 pts », « Toutes séries ». */
        public String libellePoints() {
            if (pointsMin == null && pointsMax == null) {
                return "Toutes séries";
            }
            if (pointsMin == null) {
                return "≤ " + pointsMax + " pts";
            }
            return pointsMax == null ? "≥ " + pointsMin + " pts" : pointsMin + " à " + pointsMax + " pts";
        }
    }

    /** Résultat de la lecture de la feuille. */
    public record Analyse(List<Definition> definitions, List<ImportJoueurs.Probleme> erreurs,
                          List<ImportJoueurs.Probleme> avertissements, boolean enteteTrouvee) {
    }

    private enum Col { NOM, MIN, MAX, PUBLIC, JOUR, HEURE, PLACES, ATTENTE, TARIF }

    private static final Map<String, Col> SYNONYMES = new LinkedHashMap<>();

    static {
        for (String s : List.of("nomdutableau", "nomtableau", "tableau", "nom", "intitule")) {
            SYNONYMES.put(s, Col.NOM);
        }
        for (String s : List.of("pointsmin", "pointsminimum", "min", "minimum", "pointsmini", "pointsde")) {
            SYNONYMES.put(s, Col.MIN);
        }
        for (String s : List.of("pointsmax", "pointsmaximum", "max", "maximum", "pointsmaxi", "plafond", "pointsa")) {
            SYNONYMES.put(s, Col.MAX);
        }
        for (String s : List.of("public", "sexe", "joueursadmis", "reserve")) {
            SYNONYMES.put(s, Col.PUBLIC);
        }
        for (String s : List.of("jour", "date")) {
            SYNONYMES.put(s, Col.JOUR);
        }
        for (String s : List.of("heure", "heuredebut", "debut", "horaire", "heurededebut")) {
            SYNONYMES.put(s, Col.HEURE);
        }
        for (String s : List.of("places", "capacite", "nbplaces", "nombredeplaces")) {
            SYNONYMES.put(s, Col.PLACES);
        }
        for (String s : List.of("listedattente", "attente", "placesenattente", "listeattente")) {
            SYNONYMES.put(s, Col.ATTENTE);
        }
        for (String s : List.of("tarif", "tarifeur", "prix", "tarifeuros", "engagement")) {
            SYNONYMES.put(s, Col.TARIF);
        }
    }

    private ImportTableaux() {
    }

    public static Analyse analyser(List<List<String>> cellules) {
        int iEntete = -1;
        Map<Col, Integer> cols = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(15, cellules.size()) && iEntete < 0; i++) {
            Map<Col, Integer> c = new LinkedHashMap<>();
            List<String> l = cellules.get(i);
            for (int k = 0; k < l.size(); k++) {
                Col col = SYNONYMES.get(ImportJoueurs.normaliser(l.get(k)).replaceAll("[^a-z0-9]", ""));
                if (col != null && !c.containsKey(col)) {
                    c.put(col, k);
                }
            }
            if (c.containsKey(Col.NOM) && c.size() >= 2) {
                iEntete = i;
                cols = c;
            }
        }
        List<Definition> defs = new ArrayList<>();
        List<ImportJoueurs.Probleme> err = new ArrayList<>();
        List<ImportJoueurs.Probleme> av = new ArrayList<>();
        if (iEntete < 0) {
            return new Analyse(defs, err, av, false);
        }
        java.util.Set<String> noms = new java.util.HashSet<>();
        for (int i = iEntete + 1; i < cellules.size(); i++) {
            List<String> l = cellules.get(i);
            int n = i + 1;
            String nom = val(l, cols.get(Col.NOM));
            if (nom.isEmpty()) {
                if (l.stream().anyMatch(c -> c != null && !c.isBlank())) {
                    err.add(new ImportJoueurs.Probleme(n, "tableau sans nom"));
                }
                continue;
            }
            if (!noms.add(ImportJoueurs.normaliser(nom))) {
                av.add(new ImportJoueurs.Probleme(n, "tableau « " + nom + " » défini deux fois : ligne ignorée"));
                continue;
            }
            Integer min = entier(val(l, cols.get(Col.MIN)), n, "points minimum", err);
            Integer max = entier(val(l, cols.get(Col.MAX)), n, "points maximum", err);
            if (min != null && max != null && min > max) {
                err.add(new ImportJoueurs.Probleme(n, "« " + nom + " » : points minimum (" + min
                        + ") supérieurs au maximum (" + max + ")"));
                continue;
            }
            Sexe sexe = lirePublic(val(l, cols.get(Col.PUBLIC)));
            String jourTxt = val(l, cols.get(Col.JOUR));
            LocalDate jour = ImportJoueurs.lireDate(jourTxt);
            if (jour == null && !jourTxt.isEmpty()) {
                av.add(new ImportJoueurs.Probleme(n, "« " + nom + " » : jour « " + jourTxt + " » illisible, ignoré"));
            }
            String heureTxt = val(l, cols.get(Col.HEURE));
            LocalTime heure = lireHeure(heureTxt);
            if (heure == null && !heureTxt.isEmpty()) {
                av.add(new ImportJoueurs.Probleme(n, "« " + nom + " » : heure « " + heureTxt + " » illisible, ignorée"));
            }
            Integer places = entier(val(l, cols.get(Col.PLACES)), n, "places", err);
            Integer attente = entier(val(l, cols.get(Col.ATTENTE)), n, "liste d'attente", err);
            Double tarif = null;
            String t = val(l, cols.get(Col.TARIF)).replace("€", "").replace(',', '.').trim();
            if (!t.isEmpty()) {
                try {
                    tarif = Double.parseDouble(t);
                } catch (NumberFormatException e) {
                    av.add(new ImportJoueurs.Probleme(n, "« " + nom + " » : tarif « " + t + " » illisible, ignoré"));
                }
            }
            defs.add(new Definition(n, nom, min, max, sexe, jour, heure, places, attente, tarif));
        }
        return new Analyse(defs, err, av, true);
    }

    /**
     * Construit le tableau d'une définition. {@code places} : capacité retenue (celle de la
     * feuille, ou la taille de la liste importée quand la feuille ne la donne pas ; 0 = sans limite).
     */
    public static Tableau construire(Definition d, int places, Tournoi tournoi) {
        ConfigurationTableau.Builder b = ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut());
        if (d.pointsMin() == null && d.pointsMax() == null) {
            b.toutesSeries();
        } else if (d.pointsMin() == null) {
            b.pointMax(d.pointsMax());
        } else {
            b.tranche(d.pointsMin(), d.pointsMax() == null ? 0 : d.pointsMax());
        }
        if (d.sexe() != null) {
            b.sexeRequis(d.sexe());
        }
        b.placesMax(Math.max(0, places)).tailleMaxListeAttente(d.attente() == null ? 0 : Math.max(0, d.attente()));
        b.tarif(d.tarif() == null ? TarifInscription.gratuit() : new TarifInscription(d.tarif(), d.tarif()));
        Tableau t = new Tableau(d.nom(), b.build());
        t.setType(com.pingmanager.core.reglement.LibellesTableau.type(t.getConfiguration()));
        LocalDate jour = d.jour();
        if (jour == null && tournoi != null && tournoi.getJours().size() == 1) {
            jour = tournoi.getJours().first();
        }
        if (jour != null && d.heure() != null) {
            t.setHoraires(new HorairesTableau(null, null, jour.atTime(d.heure())));
        }
        return t;
    }

    private static Sexe lirePublic(String s) {
        String n = ImportJoueurs.normaliser(s);
        if (n.startsWith("dame") || n.startsWith("femme") || n.startsWith("feminin") || n.equals("f") || n.equals("d")) {
            return Sexe.FEMME;
        }
        if (n.startsWith("messieur") || n.startsWith("homme") || n.startsWith("masculin") || n.equals("h") || n.equals("m")) {
            return Sexe.HOMME;
        }
        return null;
    }

    /** « 9h », « 9h30 », « 09:30 », ou fraction de journée d'un tableur (0,375 = 9h). */
    static LocalTime lireHeure(String s) {
        String t = s.trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
        if (t.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{1,2})(?:[h:.](\\d{2})?)?(?::\\d{2})?$").matcher(t);
        if (m.matches()) {
            int h = Integer.parseInt(m.group(1));
            int mi = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            if (h < 24 && mi < 60) {
                return LocalTime.of(h, mi);
            }
        }
        if (t.matches("0?[.,]\\d+")) {
            double f = Double.parseDouble(t.replace(',', '.'));
            int minutes = (int) Math.round(f * 24 * 60);
            if (minutes < 24 * 60) {
                return LocalTime.of(minutes / 60, minutes % 60);
            }
        }
        return null;
    }

    private static Integer entier(String s, int ligne, String champ, List<ImportJoueurs.Probleme> err) {
        if (s.isEmpty()) {
            return null;
        }
        Integer v = ImportJoueurs.lirePoints(s);
        if (v == null) {
            err.add(new ImportJoueurs.Probleme(ligne, champ + " illisible « " + s + " »"));
        }
        return v;
    }

    private static String val(List<String> l, Integer i) {
        if (i == null || i >= l.size() || l.get(i) == null) {
            return "";
        }
        return l.get(i).trim();
    }
}
