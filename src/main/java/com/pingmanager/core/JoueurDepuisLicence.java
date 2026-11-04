package com.pingmanager.core;

import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tournoi;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Construit le {@link Joueur} d'une inscription à partir de sa fiche de licence :
 * identité, sexe, club, <b>points</b> (ceux de la fiche, c'est-à-dire de la phase en
 * cours, cf. {@link Tournoi#getPhasePoints()}) et <b>catégorie d'âge</b> de la saison du
 * tournoi (II.302 / II.303, depuis la date de naissance ; à défaut, le libellé de
 * catégorie porté par la fiche).
 */
public final class JoueurDepuisLicence {

    private JoueurDepuisLicence() {
    }

    public static Joueur creer(FicheLicence f, Tournoi tournoi) {
        Joueur j = new Joueur(f.getNumeroLicence(), f.getNom(), f.getPrenom(),
                f.getSexe() == null ? Sexe.HOMME : f.getSexe(),
                f.getNomClub() == null ? "" : f.getNomClub(), Math.max(0, f.getPoints()));
        LocalDate ref = tournoi == null ? null : tournoi.getDateDebut();
        if (ref == null && tournoi != null && !tournoi.getJours().isEmpty()) {
            ref = tournoi.getJours().first();
        }
        CategorieAge cat = CategorieAge.pour(f.getDateNaissance(), ref == null ? LocalDate.now() : ref);
        j.setCategorie(cat != null ? cat : depuisLibelle(f.getCategorie()));
        return j;
    }

    /** « Senior », « Vétéran 45 », « Poussins » → la catégorie ; {@code null} si inconnue. */
    static CategorieAge depuisLibelle(String libelle) {
        if (libelle == null) {
            return null;
        }
        String l = sansAccents(libelle);
        for (CategorieAge c : CategorieAge.values()) {
            String cl = sansAccents(c.getLibelle());
            if (cl.equals(l) || cl.equals(l + "s") || cl.replace("veterans", "veteran").equals(l)) {
                return c;
            }
        }
        return null;
    }

    private static String sansAccents(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }
}
