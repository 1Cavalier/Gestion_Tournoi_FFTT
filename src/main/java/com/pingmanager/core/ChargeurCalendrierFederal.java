package com.pingmanager.core;

import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.DateFederale;
import com.pingmanager.model.StatutDateFederale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Lecture et validation d'un fichier de calendrier fédéral (B045), sans dépendance :
 * l'appelant fournit le texte ou le flux (ressource versionnée
 * {@code calendrier-federal-AAAA-AAAA.csv}) ; le cœur ne lit aucun fichier lui-même.
 *
 * <p>Format : texte UTF-8, séparateur point-virgule, une date par ligne ; lignes
 * vides et lignes commençant par {@code #} ignorées ; première ligne utile = en-tête
 * {@value #EN_TETE}. Dates au format {@code AAAA-MM-JJ}, fin facultative.</p>
 *
 * <p>Validation stricte : identifiant, catégorie, titre et source non vides, dates
 * valides (fin pas avant le début), statut connu ({@code confirmee} ou
 * {@code a-verifier}), date de relecture valide ; identifiants en double et doublons
 * (même titre et même premier jour) refusés. <b>Une seule erreur refuse tout le
 * fichier</b> : on n'affiche pas un calendrier à moitié lu.</p>
 */
public final class ChargeurCalendrierFederal {

    /** En-tête attendu (première ligne utile). */
    public static final String EN_TETE = "id;categorie;titre;debut;fin;statut;source;relu_le;note";

    /** Dossier des ressources de calendrier (chemin absolu dans le classpath). */
    public static final String DOSSIER_RESSOURCES = "/com/pingmanager/calendrier-federal/";

    private static final int COLONNES = 9;

    private ChargeurCalendrierFederal() {
    }

    /** Résultat d'une lecture : les dates (vides si refus) et les erreurs, ligne par ligne. */
    public record Resultat(List<DateFederale> dates, List<String> erreurs) {

        public Resultat {
            dates = List.copyOf(dates);
            erreurs = List.copyOf(erreurs);
        }

        /** @return vrai si le fichier est accepté (aucune erreur). */
        public boolean estValide() {
            return erreurs.isEmpty();
        }
    }

    /**
     * Nom de la ressource de la saison d'une date (septembre à août, comme
     * {@link Formats#saison(LocalDate)}) : « calendrier-federal-2026-2027.csv ».
     * Ajouter une saison = ajouter le fichier de ce nom, sans toucher au code.
     */
    public static String nomRessource(LocalDate jourDeLaSaison) {
        return "calendrier-federal-" + Formats.saison(jourDeLaSaison) + ".csv";
    }

    /** Ressources à chercher pour l'agenda : saison en cours, puis la suivante. */
    public static List<String> ressourcesAChercher(LocalDate aujourdhui) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        return List.of(nomRessource(aujourdhui), nomRessource(aujourdhui.plusYears(1)));
    }

    /** Lit un flux UTF-8 (fermé par l'appelant). */
    public static Resultat lire(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        return lire(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }

    /** Lit et valide le texte d'un fichier de calendrier. */
    public static Resultat lire(String texte) {
        List<DateFederale> dates = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();
        if (texte == null || texte.isBlank()) {
            erreurs.add("Fichier vide.");
            return new Resultat(List.of(), erreurs);
        }
        String[] lignes = texte.replace("﻿", "").split("\r?\n|\r", -1);
        boolean enTeteLu = false;
        Set<String> ids = new HashSet<>();
        Set<String> cles = new HashSet<>();
        for (int i = 0; i < lignes.length; i++) {
            int numero = i + 1;
            String ligne = lignes[i];
            if (ligne.isBlank() || ligne.trim().startsWith("#")) {
                continue;
            }
            if (!enTeteLu) {
                if (!normaliserEnTete(ligne).equals(EN_TETE)) {
                    erreurs.add("Ligne " + numero + " : en-tête attendu « " + EN_TETE + " ».");
                    return new Resultat(List.of(), erreurs);
                }
                enTeteLu = true;
                continue;
            }
            String[] c = ligne.split(";", -1);
            if (c.length != COLONNES) {
                erreurs.add("Ligne " + numero + " : " + COLONNES + " colonnes attendues, " + c.length + " trouvées.");
                continue;
            }
            List<String> motifs = new ArrayList<>();
            String id = c[0].trim();
            String categorie = c[1].trim();
            String titre = c[2].trim();
            LocalDate debut = lireDate(c[3], "début", motifs, true);
            LocalDate fin = lireDate(c[4], "fin", motifs, false);
            StatutDateFederale statut = StatutDateFederale.depuisCode(c[5]);
            String source = c[6].trim();
            LocalDate reluLe = lireDate(c[7], "relecture", motifs, true);
            if (id.isEmpty()) {
                motifs.add("identifiant vide");
            }
            if (categorie.isEmpty()) {
                motifs.add("catégorie vide");
            }
            if (titre.isEmpty()) {
                motifs.add("titre vide");
            }
            if (statut == null) {
                motifs.add("statut inconnu « " + c[5].trim() + " » (confirmee ou a-verifier)");
            }
            if (source.isEmpty()) {
                motifs.add("source vide");
            }
            if (debut != null && fin != null && fin.isBefore(debut)) {
                motifs.add("fin avant le début");
            }
            if (!id.isEmpty() && !ids.add(id)) {
                motifs.add("identifiant « " + id + " » en double");
            }
            if (!titre.isEmpty() && debut != null
                    && !cles.add(titre.toLowerCase(Locale.ROOT) + "|" + debut)) {
                motifs.add("doublon (même titre, même jour)");
            }
            if (!motifs.isEmpty()) {
                erreurs.add("Ligne " + numero + " : " + String.join(", ", motifs) + ".");
                continue;
            }
            dates.add(DateFederale.builder().id(id).categorie(categorie).titre(titre).date(debut).dateFin(fin)
                    .statut(statut).source(source).reluLe(reluLe).note(c[8]).construire());
        }
        if (!enTeteLu) {
            erreurs.add("En-tête « " + EN_TETE + " » absent.");
        }
        if (!erreurs.isEmpty()) {
            return new Resultat(List.of(), erreurs);
        }
        dates.sort((a, b) -> {
            int r = a.getDate().compareTo(b.getDate());
            return r != 0 ? r : a.getId().compareTo(b.getId());
        });
        return new Resultat(dates, erreurs);
    }

    private static String normaliserEnTete(String ligne) {
        String[] c = ligne.split(";", -1);
        List<String> n = new ArrayList<>();
        for (String s : c) {
            n.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return String.join(";", n);
    }

    private static LocalDate lireDate(String valeur, String nom, List<String> motifs, boolean obligatoire) {
        String v = valeur == null ? "" : valeur.trim();
        if (v.isEmpty()) {
            if (obligatoire) {
                motifs.add("date de " + nom + " manquante");
            }
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            motifs.add("date de " + nom + " invalide « " + v + " » (AAAA-MM-JJ)");
            return null;
        }
    }
}
