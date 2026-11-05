package com.pingmanager.core;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Saisie des résultats des matchs de poule, le jour J : score manche par manche,
 * issue non normale (forfait, abandon, refus de jouer, disqualification) et
 * correction d'une saisie.
 *
 * <p>Les contrôles s'appuient sur les règles du jeu :</p>
 * <ul>
 *   <li>2.11.1 : une manche est gagnée par le premier à 11 points ; à 10-10, par
 *       le premier qui prend 2 points d'avance ;</li>
 *   <li>2.12.1 : une partie se joue au meilleur d'un nombre impair de manches
 *       (5 ou 7) ; le nombre de manches gagnantes est porté par le match ;</li>
 *   <li>I.303 a) : une absence à l'appel, un abandon, un refus de jouer ou une
 *       disqualification font marquer 0 point au fautif (calculé par
 *       {@link ClassementPouleService}).</li>
 * </ul>
 *
 * <p>Les résultats de poule ne sont modifiables que pendant la phase de poules
 * ({@link StatutTableau#LANCE}) : une fois la phase finale construite à partir du
 * classement, ils sont figés. Les refus sont renvoyés avec un motif précis
 * ({@link ResultatSaisieMatch}), sans exception.</p>
 */
public final class SaisieResultatMatch {

    private final Horloge horloge;

    public SaisieResultatMatch() {
        this(Horloge.systeme());
    }

    /** @param horloge source de l'heure de fin enregistrée sur le match */
    public SaisieResultatMatch(Horloge horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    /**
     * 2.11.1 : une manche est gagnée par le joueur qui marque le premier 11 points ;
     * si les deux joueurs ont marqué 10 points, par celui qui prend ensuite 2 points
     * d'avance.
     *
     * @return true si le score de la manche est possible
     */
    public static boolean mancheValide(Manche manche) {
        Objects.requireNonNull(manche, "manche");
        int gagnant = Math.max(manche.pointsJoueurA(), manche.pointsJoueurB());
        int perdant = Math.min(manche.pointsJoueurA(), manche.pointsJoueurB());
        if (perdant <= 9) {
            return gagnant == 11;
        }
        return gagnant == perdant + 2;
    }

    /**
     * Vérifie qu'une suite de manches forme un match complet et cohérent pour ce
     * match (sans rien enregistrer).
     */
    public ResultatSaisieMatch verifierScore(Match match, List<Manche> manches) {
        Objects.requireNonNull(match, "match");
        if (match.estBye()) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_EXEMPT);
        }
        if (manches == null || manches.isEmpty()) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.AUCUNE_MANCHE);
        }
        for (int i = 0; i < manches.size(); i++) {
            if (!mancheValide(manches.get(i))) {
                return ResultatSaisieMatch.refusManche(ResultatSaisieMatch.Motif.MANCHE_INVALIDE, i + 1);
            }
        }
        int gagnantes = match.getNombreManchesGagnantes();
        // 2.12.1 : au meilleur de (2 × gagnantes − 1) manches.
        if (manches.size() > 2 * gagnantes - 1) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.TROP_DE_MANCHES);
        }
        int a = 0;
        int b = 0;
        for (int i = 0; i < manches.size(); i++) {
            if (a == gagnantes || b == gagnantes) {
                return ResultatSaisieMatch.refusManche(ResultatSaisieMatch.Motif.MANCHE_APRES_DECISION, i + 1);
            }
            if (manches.get(i).gagneeParA()) {
                a++;
            } else {
                b++;
            }
        }
        if (a < gagnantes && b < gagnantes) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_NON_DECIDE);
        }
        return ResultatSaisieMatch.enregistre();
    }

    /** Enregistre le score d'un match de poule joué normalement. */
    public ResultatSaisieMatch enregistrerScore(Tableau tableau, Match match, List<Manche> manches) {
        ResultatSaisieMatch acces = verifierAcces(tableau, match);
        if (acces != null) {
            return acces;
        }
        ResultatSaisieMatch verif = verifierScore(match, manches);
        if (!verif.estEnregistre()) {
            return verif;
        }
        match.definirScore(List.copyOf(manches));
        match.setInstantFin(maintenant());
        return verif;
    }

    /**
     * Enregistre une issue non normale (I.303 a : le fautif marque 0 point). Les
     * manches éventuellement saisies auparavant sont effacées.
     *
     * @param fautif le joueur absent, qui abandonne, refuse de jouer ou est disqualifié
     */
    public ResultatSaisieMatch enregistrerIssue(Tableau tableau, Match match, IssueMatch issue, Joueur fautif) {
        Objects.requireNonNull(issue, "issue");
        ResultatSaisieMatch acces = verifierAcces(tableau, match);
        if (acces != null) {
            return acces;
        }
        if (match.estBye()) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_EXEMPT);
        }
        if (issue == IssueMatch.NORMALE) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.ISSUE_NORMALE);
        }
        if (fautif == null || (!fautif.equals(match.getJoueurA()) && !fautif.equals(match.getJoueurB()))) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.FAUTIF_HORS_MATCH);
        }
        match.effacerResultat();
        match.declarerIssue(issue, fautif);
        match.setInstantFin(maintenant());
        return ResultatSaisieMatch.enregistre();
    }

    /** Efface le résultat d'un match (erreur de saisie) : il redevient à jouer. */
    public ResultatSaisieMatch effacer(Tableau tableau, Match match) {
        ResultatSaisieMatch acces = verifierAcces(tableau, match);
        if (acces != null) {
            return acces;
        }
        if (match.estBye()) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_EXEMPT);
        }
        match.effacerResultat();
        return ResultatSaisieMatch.enregistre();
    }

    /** @return un refus si le tableau n'accepte pas de saisie pour ce match, sinon null. */
    private static ResultatSaisieMatch verifierAcces(Tableau tableau, Match match) {
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(match, "match");
        if (tableau.getStatut() != StatutTableau.LANCE) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.POULES_NON_MODIFIABLES);
        }
        for (Poule p : tableau.getPoules()) {
            if (p.getMatchs().contains(match)) {
                return null;
            }
        }
        return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_HORS_TABLEAU);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.ofInstant(horloge.maintenant(), ZoneId.systemDefault());
    }
}
