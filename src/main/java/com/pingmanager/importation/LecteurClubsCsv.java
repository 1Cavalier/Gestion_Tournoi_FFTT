package com.pingmanager.importation;

import com.pingmanager.model.Club;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lit {@code clubs.csv}, une liste de clubs fournie par l'organisateur (B023).
 * <b>Format provisoire</b> : voir le README, section 8. Mêmes
 * règles de lecture que {@link LecteurLicencesCsv}.
 *
 * <p>Colonnes : {@code N° club} (obligatoire, 8 chiffres ; 7 chiffres complétés
 * d'un zéro), {@code Nom} (obligatoire), {@code Département} (déduit du code
 * postal si vide), {@code Adresse 1}, {@code Adresse 2}, {@code Code postal},
 * {@code Ville}, {@code Courriel officiel}, {@code Nom du responsable},
 * {@code Prénom du responsable}.</p>
 *
 * <p>Le modèle {@link Club} exige un département, une ville et un courriel
 * officiel (destinataire du code de liaison d'un compte au club) : une ligne
 * où ils manquent est refusée avec un motif précis.</p>
 */
public final class LecteurClubsCsv {

    static final List<String> OBLIGATOIRES = List.of("N° club", "Nom");
    private static final Pattern CLUB = Pattern.compile("[0-9]{7,8}");
    private static final Pattern CODE_POSTAL = Pattern.compile("[0-9]{5}");
    private static final Pattern DEPARTEMENT = Pattern.compile("[0-9]{2,3}|2[AB]");
    private static final Pattern COURRIEL = Pattern.compile("[^@\\s;,<>\"]+@[^@\\s;,<>\"]+\\.[^@\\s;,<>\"]+");

    private LecteurClubsCsv() {
    }

    public static ResultatImportClubs lire(Path fichier) throws LecteurTableur.ImportException {
        return lire(fichier.getFileName().toString(), LecteurLicencesCsv.lireOctets(fichier));
    }

    public static ResultatImportClubs lire(String nom, byte[] octets) throws LecteurTableur.ImportException {
        CsvFederal.Table t = CsvFederal.lire(nom, octets, OBLIGATOIRES);
        Map<String, Club> clubs = new LinkedHashMap<>();
        List<AnomalieImport> anomalies = new ArrayList<>();
        for (int i = 0; i < t.lignes.size(); i++) {
            List<String> l = t.lignes.get(i);
            int n = t.numeros.get(i);
            if (CsvFederal.tropLongue(l)) {
                anomalies.add(new AnomalieImport(n, "cellule trop longue (plus de "
                        + CsvFederal.LONGUEUR_MAX_CELLULE + " caractères)", true));
                continue;
            }
            String numero = t.valeur(l, "N° club");
            String nomClub = t.valeur(l, "Nom");
            if (!CLUB.matcher(numero).matches()) {
                anomalies.add(new AnomalieImport(n, "numéro de club attendu sur 8 chiffres : « " + numero + " »", true));
                continue;
            }
            if (numero.length() == 7) {
                numero = "0" + numero;
            }
            if (nomClub.isEmpty()) {
                anomalies.add(new AnomalieImport(n, "nom du club manquant", true));
                continue;
            }
            if (clubs.containsKey(numero)) {
                anomalies.add(new AnomalieImport(n, "club " + numero + " en double", true));
                continue;
            }
            String cp = t.valeur(l, "Code postal");
            if (!cp.isEmpty() && !CODE_POSTAL.matcher(cp).matches()) {
                anomalies.add(new AnomalieImport(n, "code postal ignoré (5 chiffres attendus)", false));
                cp = "";
            }
            String dep = t.valeur(l, "Département").toUpperCase(java.util.Locale.ROOT);
            if (dep.isEmpty()) {
                dep = departementDepuis(cp);
            }
            if (!DEPARTEMENT.matcher(dep).matches()) {
                anomalies.add(new AnomalieImport(n, "département manquant ou invalide (ni département ni code postal)", true));
                continue;
            }
            String ville = t.valeur(l, "Ville");
            if (ville.isEmpty()) {
                anomalies.add(new AnomalieImport(n, "ville manquante", true));
                continue;
            }
            String courriel = t.valeur(l, "Courriel officiel");
            if (!COURRIEL.matcher(courriel).matches()) {
                anomalies.add(new AnomalieImport(n, "courriel officiel manquant ou invalide", true));
                continue;
            }
            Club c = new Club(numero, nomClub, dep, ville, courriel);
            c.setAdresse1(t.valeur(l, "Adresse 1"));
            c.setAdresse2(t.valeur(l, "Adresse 2"));
            c.setCodePostal(cp);
            c.setResponsableNom(t.valeur(l, "Nom du responsable"));
            c.setResponsablePrenom(t.valeur(l, "Prénom du responsable"));
            clubs.put(numero, c);
        }
        t.signalerDepassement(anomalies);
        return new ResultatImportClubs(new ArrayList<>(clubs.values()), anomalies);
    }

    /** Département déduit d'un code postal (Corse : 2A / 2B ; outre-mer : 3 chiffres). */
    static String departementDepuis(String cp) {
        if (cp == null || !CODE_POSTAL.matcher(cp).matches()) {
            return "";
        }
        if (cp.startsWith("97") || cp.startsWith("98")) {
            return cp.substring(0, 3);
        }
        if (cp.startsWith("20")) {
            return cp.compareTo("20200") < 0 ? "2A" : "2B";
        }
        return cp.substring(0, 2);
    }
}
