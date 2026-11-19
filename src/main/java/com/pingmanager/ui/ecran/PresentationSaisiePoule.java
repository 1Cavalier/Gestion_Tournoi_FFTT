package com.pingmanager.ui.ecran;

import com.pingmanager.core.NotationManche;
import com.pingmanager.core.SaisiePoule;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Logique d'affichage de la saisie d'une poule entière d'un coup
 * ({@link PanneauSaisiePoule}), sans JavaFX : pré-remplissage des champs depuis
 * les résultats déjà saisis et lecture des champs en résultats à enregistrer.
 * Les règles (manches possibles, match lancé) et l'enregistrement tout ou rien
 * sont dans le cœur ({@link SaisiePoule}).
 */
final class PresentationSaisiePoule {

    private PresentationSaisiePoule() {
    }

    /** Ce qu'une ligne de la saisie montre au départ : les manches et l'issue déjà enregistrées. */
    record Valeurs(List<String> manches, IssueMatch issue, Joueur fautif) {
    }

    /** Nombre de champs de manches d'un match : 2 × manches gagnantes − 1 (2.12.1). */
    static int champs(Match m) {
        return Math.max(1, 2 * m.getNombreManchesGagnantes() - 1);
    }

    /** Valeurs pré-remplies d'un match (vides s'il n'a pas de résultat). */
    static Valeurs valeurs(Match m) {
        List<String> manches = new ArrayList<>();
        boolean score = m.getStatut() == StatutMatch.TERMINE && m.getIssue() == IssueMatch.NORMALE;
        for (int i = 0; i < champs(m); i++) {
            manches.add(score && i < m.getManches().size() ? NotationManche.ecrire(m.getManches().get(i)) : "");
        }
        boolean anormale = m.getStatut() == StatutMatch.TERMINE && m.getIssue() != IssueMatch.NORMALE;
        return new Valeurs(manches, anormale ? m.getIssue() : null, anormale ? m.getJoueurFautif() : null);
    }

    /**
     * Lecture d'une ligne : un résultat à enregistrer, ou le message d'une manche
     * illisible (« Partie 2, manche 3 illisible… »).
     */
    record Lecture(SaisiePoule.Partie partie, String erreur) {
    }

    /**
     * Lit une ligne de la saisie : une issue anormale choisie l'emporte sur les
     * manches ; sinon les manches (vides : rien à enregistrer pour ce match).
     *
     * @param numero numéro de la partie dans l'ordre de la feuille (1, 2…)
     */
    static Lecture lire(int numero, Match m, List<String> manches, IssueMatch issue, Joueur fautif) {
        if (issue != null && issue != IssueMatch.NORMALE) {
            return new Lecture(SaisiePoule.Partie.issue(m, issue, fautif), null);
        }
        PresentationJourJ.LectureManches l = PresentationJourJ.lireManches(manches);
        if (l.premiereIllisible() > 0) {
            return new Lecture(null, "Partie " + numero + ", manche " + l.premiereIllisible()
                    + " illisible : tapez par exemple 11-9, ou 9 (-9 si le second joueur gagne la manche).");
        }
        return new Lecture(SaisiePoule.Partie.score(m, l.manches()), null);
    }

    /** Message d'un refus du cœur : « Partie 2 : … ». */
    static String refus(SaisiePoule.Resultat r) {
        String motif = PresentationJourJ.motifSaisie(r.detail());
        return r.partie() > 0 ? "Partie " + r.partie() + " : " + motif : motif;
    }

    /** Compte rendu d'une validation réussie. */
    static String enregistre(SaisiePoule.Resultat r, Poule poule, Integer table) {
        if (r.liberee()) {
            return "Poule " + poule.getNom() + " terminée" + (table == null ? "" : " : la table " + table + " est libre")
                    + ".";
        }
        if (r.enregistres() == 0) {
            return "Aucun résultat nouveau à enregistrer.";
        }
        return r.enregistres() + (r.enregistres() > 1 ? " résultats enregistrés" : " résultat enregistré")
                + " pour la poule " + poule.getNom() + ".";
    }

    /** Libellé d'une partie de la feuille : « 2. 1–2  DUPONT – MARTIN ». */
    static String libelle(int numero, Poule poule, Match m) {
        int a = poule.getJoueurs().indexOf(m.getJoueurA()) + 1;
        int b = poule.getJoueurs().indexOf(m.getJoueurB()) + 1;
        return numero + ". " + a + "–" + b + "  " + PresentationLancementMatch.nomCourt(m.getJoueurA()) + " – "
                + PresentationLancementMatch.nomCourt(m.getJoueurB());
    }

    /** Manches lues d'une ligne (pour l'aperçu « 3-1 »), vide si rien de lisible. */
    static String apercu(List<String> manches) {
        List<Manche> l = PresentationJourJ.lireManches(manches).manches();
        return l.isEmpty() ? "" : PresentationJourJ.manchesGagnees(l);
    }
}
