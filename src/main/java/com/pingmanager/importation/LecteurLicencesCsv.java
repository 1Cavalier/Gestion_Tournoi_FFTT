package com.pingmanager.importation;

import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lit {@code licencies.csv}, la liste des licenciés fournie par un club
 * (B023). <b>Format provisoire</b>, en attendant un accès fédéral (API ou
 * fichier SPID) : voir {@code docs/FORMAT_ECHANGES_FFTT.md}.
 *
 * <p>CSV « ; » en UTF-8 (BOM accepté ; Windows-1252 en repli), ou .xlsx / .ods ;
 * une ligne d'en-têtes ; colonnes lues par leur nom (ordre libre, casse et
 * accents ignorés) ; colonnes inconnues ignorées.</p>
 *
 * <table>
 *   <caption>Colonnes</caption>
 *   <tr><th>Colonne</th><th>Obligatoire</th><th>Contenu</th></tr>
 *   <tr><td>Licence</td><td>oui</td><td>numéro de licence (lettres et chiffres, 12 au plus)</td></tr>
 *   <tr><td>Nom, Prénom</td><td>oui</td><td></td></tr>
 *   <tr><td>Sexe</td><td>oui</td><td>M (ou H) / F</td></tr>
 *   <tr><td>N° club</td><td>oui</td><td>8 chiffres ; 7 chiffres complétés d'un zéro en tête (zéro perdu par un tableur)</td></tr>
 *   <tr><td>Club</td><td>non</td><td>nom du club</td></tr>
 *   <tr><td>Points</td><td>oui</td><td>points officiels de début de phase, entier positif</td></tr>
 *   <tr><td>Points mensuels</td><td>non</td><td>vide = Points</td></tr>
 *   <tr><td>Catégorie, Nationalité, Type de licence</td><td>non</td><td>texte</td></tr>
 *   <tr><td>Date de naissance, Relevé le</td><td>non</td><td>jj/mm/aaaa</td></tr>
 *   <tr><td>Certificat</td><td>non</td><td>EN_REGLE, SANS_PRATIQUE, INCONNU (vide = INCONNU)</td></tr>
 *   <tr><td>Validation</td><td>non</td><td>lue mais pas encore conservée</td></tr>
 * </table>
 *
 * <p>Une ligne invalide (champ obligatoire vide ou mal formé, cellule de plus
 * de {@value CsvFederal#LONGUEUR_MAX_CELLULE} caractères, doublon de licence)
 * est refusée avec son numéro ; une valeur facultative mal formée est ignorée
 * et signalée.</p>
 */
public final class LecteurLicencesCsv {

    static final List<String> OBLIGATOIRES = List.of("Licence", "Nom", "Prénom", "Sexe", "N° club", "Points");
    private static final Pattern LICENCE = Pattern.compile("[0-9A-Za-z]{1,12}");
    private static final Pattern CLUB = Pattern.compile("[0-9]{7,8}");
    private static final int POINTS_MAX = 100_000;

    private LecteurLicencesCsv() {
    }

    public static ResultatImportLicences lire(Path fichier) throws LecteurTableur.ImportException {
        return lire(fichier.getFileName().toString(), lireOctets(fichier));
    }

    public static ResultatImportLicences lire(String nom, byte[] octets) throws LecteurTableur.ImportException {
        CsvFederal.Table t = CsvFederal.lire(nom, octets, OBLIGATOIRES);
        Map<String, FicheLicence> fiches = new LinkedHashMap<>();
        Map<String, SituationCertificat> certificats = new HashMap<>();
        List<AnomalieImport> anomalies = new ArrayList<>();
        java.util.Set<String> vues = new java.util.HashSet<>();
        LocalDate releve = null;
        for (int i = 0; i < t.lignes.size(); i++) {
            List<String> l = t.lignes.get(i);
            int n = t.numeros.get(i);
            if (CsvFederal.tropLongue(l)) {
                anomalies.add(new AnomalieImport(n, "cellule trop longue (plus de "
                        + CsvFederal.LONGUEUR_MAX_CELLULE + " caractères)", true));
                continue;
            }
            String licence = t.valeur(l, "Licence");
            String nomJ = t.valeur(l, "Nom");
            String prenom = t.valeur(l, "Prénom");
            if (licence.isEmpty() || nomJ.isEmpty() || prenom.isEmpty()) {
                anomalies.add(new AnomalieImport(n, "licence, nom ou prénom manquant", true));
                continue;
            }
            if (!LICENCE.matcher(licence).matches()) {
                anomalies.add(new AnomalieImport(n, "numéro de licence invalide : « " + licence + " »", true));
                continue;
            }
            // « ab1 » et « AB1 » : même licence (la saisie d'origine est conservée).
            if (vues.contains(licence.toUpperCase(java.util.Locale.ROOT))) {
                anomalies.add(new AnomalieImport(n, "licence " + licence + " en double", true));
                continue;
            }
            Sexe sexe = sexe(t.valeur(l, "Sexe"));
            if (sexe == null) {
                anomalies.add(new AnomalieImport(n, "sexe attendu : M ou F", true));
                continue;
            }
            String club = t.valeur(l, "N° club");
            if (!CLUB.matcher(club).matches()) {
                anomalies.add(new AnomalieImport(n, "numéro de club attendu sur 8 chiffres : « " + club + " »", true));
                continue;
            }
            if (club.length() == 7) {
                club = "0" + club;
            }
            Integer points = entier(t.valeur(l, "Points"));
            if (points == null) {
                anomalies.add(new AnomalieImport(n, "points non numériques", true));
                continue;
            }
            FicheLicence.Builder b = new FicheLicence.Builder(licence, nomJ, prenom)
                    .sexe(sexe).club(club, t.valeur(l, "Club")).points(points).pointsMensuels(points);
            String pm = t.valeur(l, "Points mensuels");
            if (!pm.isEmpty()) {
                Integer v = entier(pm);
                if (v == null) {
                    anomalies.add(new AnomalieImport(n, "points mensuels ignorés (non numériques)", false));
                } else {
                    b.pointsMensuels(v);
                }
            }
            b.categorie(t.valeur(l, "Catégorie"))
                    .nationalite(t.valeur(l, "Nationalité"))
                    .typeLicence(t.valeur(l, "Type de licence"));
            String naissance = t.valeur(l, "Date de naissance");
            if (!naissance.isEmpty()) {
                LocalDate d = CsvFederal.date(naissance);
                if (d == null) {
                    anomalies.add(new AnomalieImport(n, "date de naissance ignorée (attendu jj/mm/aaaa)", false));
                } else {
                    b.dateNaissance(d);
                }
            }
            String rel = t.valeur(l, "Relevé le");
            if (!rel.isEmpty()) {
                LocalDate d = CsvFederal.date(rel);
                if (d == null) {
                    anomalies.add(new AnomalieImport(n, "date « Relevé le » ignorée (attendu jj/mm/aaaa)", false));
                } else if (releve == null || d.isBefore(releve)) {
                    releve = d;
                }
            }
            certificats.put(licence, certificat(t.valeur(l, "Certificat"), n, anomalies));
            vues.add(licence.toUpperCase(java.util.Locale.ROOT));
            fiches.put(licence, b.build());
        }
        t.signalerDepassement(anomalies);
        return new ResultatImportLicences(new ArrayList<>(fiches.values()), anomalies, certificats, releve);
    }

    static byte[] lireOctets(Path fichier) throws LecteurTableur.ImportException {
        try {
            if (Files.size(fichier) > CsvFederal.TAILLE_MAX_OCTETS) {
                throw new LecteurTableur.ImportException("Fichier trop volumineux (plus de 10 Mo).");
            }
            return Files.readAllBytes(fichier);
        } catch (IOException e) {
            throw new LecteurTableur.ImportException("Lecture impossible : " + e.getMessage());
        }
    }

    private static Sexe sexe(String v) {
        String s = CsvFederal.normaliser(v);
        if (s.equals("m") || s.equals("h") || s.equals("homme")) {
            return Sexe.HOMME;
        }
        if (s.equals("f") || s.equals("femme")) {
            return Sexe.FEMME;
        }
        return null;
    }

    private static Integer entier(String brut) {
        String v = brut.replaceFirst("\\.0+$", ""); // 1234.0 venu d'un tableur
        if (!v.matches("[0-9]{1,6}")) {
            return null;
        }
        int x = Integer.parseInt(v);
        return x > POINTS_MAX ? null : x;
    }

    private static SituationCertificat certificat(String v, int n, List<AnomalieImport> anomalies) {
        if (v.isEmpty()) {
            return SituationCertificat.INCONNU;
        }
        String s = CsvFederal.normaliser(v).replace(' ', '_').toUpperCase(java.util.Locale.ROOT);
        for (SituationCertificat c : SituationCertificat.values()) {
            if (c.name().equals(s)) {
                return c;
            }
        }
        anomalies.add(new AnomalieImport(n, "certificat « " + v + " » non reconnu : INCONNU retenu", false));
        return SituationCertificat.INCONNU;
    }
}
