package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatSaisieScore;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Logique, <b>sans JavaFX</b>, du formulaire de saisie du score d'un match de
 * phase finale : nombre de lignes de manches, valeurs déjà saisies, lecture des
 * champs texte et messages d'erreur en clair. La validation sportive elle-même
 * (2.11.1, 2.12.1) est faite par le cœur ({@code ValidationScoreMatch}).
 */
final class SaisieScorePhaseFinale {

    private SaisieScorePhaseFinale() {
    }

    /** Lignes de manches à proposer : 5 au meilleur des 5, 7 au meilleur des 7. */
    static int nombreLignes(Match m) {
        return 2 * m.getNombreManchesGagnantes() - 1;
    }

    /**
     * Valeurs initiales des champs ({@code [pointsA, pointsB]} par ligne) : le
     * score déjà enregistré pour une correction, des champs vides sinon.
     */
    static List<String[]> valeursInitiales(Match m) {
        List<String[]> lignes = new ArrayList<>();
        List<Manche> manches = m.getStatut() == StatutMatch.TERMINE ? m.getManches() : List.of();
        for (int i = 0; i < nombreLignes(m); i++) {
            if (i < manches.size()) {
                lignes.add(new String[] {String.valueOf(manches.get(i).pointsJoueurA()),
                        String.valueOf(manches.get(i).pointsJoueurB())});
            } else {
                lignes.add(new String[] {"", ""});
            }
        }
        return lignes;
    }

    /** Résultat de la lecture des champs : les manches, ou un message d'erreur. */
    record Lecture(List<Manche> manches, String erreur) {
        boolean estValide() {
            return erreur == null;
        }
    }

    /**
     * Lit les champs, ligne par ligne ({@code [pointsA, pointsB]}). Les lignes
     * vides en fin de liste sont ignorées (match gagné en moins de manches) ;
     * une ligne vide suivie d'une ligne remplie est une erreur.
     */
    static Lecture lire(List<String[]> lignes) {
        int derniere = -1;
        for (int i = 0; i < lignes.size(); i++) {
            if (!estVide(lignes.get(i))) {
                derniere = i;
            }
        }
        List<Manche> manches = new ArrayList<>();
        for (int i = 0; i <= derniere; i++) {
            String[] l = lignes.get(i);
            int numero = i + 1;
            if (estVide(l)) {
                return new Lecture(null, "Manche " + numero + " : elle est vide alors que la manche "
                        + (derniere + 1) + " est remplie.");
            }
            String a = texte(l, 0);
            String b = texte(l, 1);
            if (a.isEmpty() || b.isEmpty()) {
                return new Lecture(null, "Manche " + numero + " : indiquez les points des deux joueurs.");
            }
            Integer pa = entier(a);
            Integer pb = entier(b);
            if (pa == null || pb == null) {
                return new Lecture(null, "Manche " + numero + " : saisissez des nombres entiers de points.");
            }
            manches.add(new Manche(pa, pb));
        }
        return new Lecture(manches, null);
    }

    private static boolean estVide(String[] l) {
        return texte(l, 0).isEmpty() && texte(l, 1).isEmpty();
    }

    private static String texte(String[] l, int i) {
        return l == null || l.length <= i || l[i] == null ? "" : l[i].trim();
    }

    private static Integer entier(String s) {
        try {
            int v = Integer.parseInt(s);
            return v < 0 || v > 99 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Message en clair pour un refus du cœur (qui dit quoi corriger). */
    static String messageRefus(ResultatSaisieScore r, Match m) {
        ResultatSaisieScore.Motif motif = r.getMotif();
        String manche = "Manche " + r.getNumeroManche() + " : ";
        if (motif == ResultatSaisieScore.Motif.AUCUNE_MANCHE) {
            return "Saisissez au moins une manche.";
        }
        if (motif == ResultatSaisieScore.Motif.MANCHE_A_EGALITE) {
            return manche + "une manche ne peut pas finir à égalité.";
        }
        if (motif == ResultatSaisieScore.Motif.MANCHE_SOUS_ONZE_POINTS) {
            return manche + "le vainqueur d'une manche marque au moins 11 points.";
        }
        if (motif == ResultatSaisieScore.Motif.MANCHE_ECART_INSUFFISANT) {
            return manche + "à 10-10, la manche continue jusqu'à deux points d'écart (12-10, 13-11…).";
        }
        if (motif == ResultatSaisieScore.Motif.MANCHE_PROLONGATION_INCORRECTE) {
            return manche + "après 10-10, la manche s'arrête dès deux points d'écart (12-10, 13-11…) ; "
                    + "avant, elle s'arrête à 11 points.";
        }
        if (motif == ResultatSaisieScore.Motif.MANCHE_EN_TROP) {
            return manche + "le match était déjà gagné, cette manche est en trop.";
        }
        if (motif == ResultatSaisieScore.Motif.MATCH_NON_DECIDE) {
            return "Le match n'est pas fini : il faut " + m.getNombreManchesGagnantes()
                    + " manches gagnées pour l'emporter.";
        }
        if (motif == ResultatSaisieScore.Motif.MATCH_HORS_TABLEAU) {
            return "Ce match n'appartient pas à ce tableau.";
        }
        if (motif == ResultatSaisieScore.Motif.JOUEURS_NON_CONNUS) {
            return "Les deux joueurs de ce match ne sont pas encore connus.";
        }
        if (motif == ResultatSaisieScore.Motif.SUITE_DEJA_JOUEE) {
            return "Le vainqueur ne peut plus changer : le match suivant est déjà en cours ou joué. "
                    + "Voyez avec le juge-arbitre.";
        }
        if (motif == ResultatSaisieScore.Motif.ISSUE_INVALIDE) {
            return "Choisissez le joueur absent, qui abandonne ou qui est disqualifié.";
        }
        return "Résultat enregistré.";
    }
}
