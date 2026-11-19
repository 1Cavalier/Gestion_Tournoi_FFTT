package com.pingmanager.core;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
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
 *
 * <p><b>Un match se joue sur une table.</b> Son score (ou un abandon, un refus de
 * jouer, une disqualification) ne s'enregistre que s'il a été lancé sur une
 * table ({@link StatutMatch#EN_COURS}) ; un match déjà terminé peut être corrigé.
 * Seul le <b>forfait</b> d'un joueur absent, déclaré par le juge-arbitre, se
 * saisit sans lancement : il n'y a pas de table à occuper. Voir
 * {@link #scoreSaisissable} et {@link #issueSaisissable}. La règle ne vaut que pour
 * la saisie : la relecture d'un ancien fichier (matchs terminés sans lancement)
 * passe par le modèle et n'est pas concernée.</p>
 *
 * <p><b>Poule lancée en bloc</b> (phase de poules) : tous ses matchs sont en cours
 * sur la table de la poule, ils comptent donc comme lancés et reçoivent leur
 * résultat au fil de l'eau. Le début d'un match est alors la fin du résultat
 * précédent de la poule (matchs à la suite, I.301), pour que les durées restent
 * apprises match par match. Au dernier résultat, la table et les joueurs de la
 * poule sont libérés ; un résultat effacé pendant la poule remet le match en cours
 * sur sa table. La saisie de la poule entière d'un coup est dans {@link SaisiePoule}.</p>
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
     * Le score d'un match peut-il être saisi ? Oui s'il est en cours sur une table,
     * ou déjà terminé (correction d'une saisie), sauf s'il s'est terminé par un
     * forfait sans avoir été lancé : il n'a alors jamais été joué, il faut effacer
     * le forfait puis lancer le match. Non pour un match à jouer ou en attente.
     */
    public static boolean scoreSaisissable(Match match) {
        Objects.requireNonNull(match, "match");
        if (match.getStatut() == StatutMatch.EN_COURS) {
            return true;
        }
        if (match.getStatut() != StatutMatch.TERMINE) {
            return false;
        }
        return !(match.getIssue() == IssueMatch.FORFAIT && match.getInstantLancement() == null);
    }

    /**
     * Une issue non normale peut-elle être déclarée ? Le forfait d'un joueur
     * absent (I.303 a, déclaré par le juge-arbitre) toujours : il n'occupe aucune
     * table. L'abandon, le refus de jouer et la disqualification se constatent sur
     * un match lancé : mêmes conditions que {@link #scoreSaisissable}.
     */
    public static boolean issueSaisissable(Match match, IssueMatch issue) {
        Objects.requireNonNull(match, "match");
        return issue == IssueMatch.FORFAIT || scoreSaisissable(match);
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
        if (!match.estBye() && !scoreSaisissable(match)) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_NON_LANCE);
        }
        ResultatSaisieMatch verif = verifierScore(match, manches);
        if (!verif.estEnregistre()) {
            return verif;
        }
        LocalDateTime fin = maintenant();
        Poule poule = pouleLancee(tableau, match);
        match.definirScore(List.copyOf(manches));
        terminerDansLaPoule(poule, match, fin, false);
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
        if (!issueSaisissable(match, issue)) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_NON_LANCE);
        }
        LocalDateTime fin = maintenant();
        Poule poule = pouleLancee(tableau, match);
        match.effacerResultat();
        match.declarerIssue(issue, fautif);
        terminerDansLaPoule(poule, match, fin, false);
        return ResultatSaisieMatch.enregistre();
    }

    /** Poule lancée en bloc qui contient ce match, ou null. */
    static Poule pouleLancee(Tableau tableau, Match match) {
        for (Poule p : tableau.getPoules()) {
            if (p.estLanceeEnBloc() && p.getMatchs().contains(match)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Pose l'heure de fin d'un match qui vient de recevoir son résultat. Dans une
     * poule lancée en bloc : son début est la fin du résultat précédent de la
     * poule (sinon le lancement de la poule) ; d'un coup ({@code dureeInconnue}),
     * début et fin sont confondus : la durée réelle n'est pas connue et n'est pas
     * apprise. Au dernier résultat, la poule libère sa table et ses joueurs.
     */
    static void terminerDansLaPoule(Poule poule, Match match, LocalDateTime fin, boolean dureeInconnue) {
        match.setInstantFin(fin);
        if (poule == null) {
            return;
        }
        if (dureeInconnue) {
            match.setInstantLancement(fin);
        } else {
            LocalDateTime debut = poule.getInstantLancement();
            for (Match m : poule.getMatchs()) {
                LocalDateTime f = m.getInstantFin();
                if (m != match && m.getStatut() == StatutMatch.TERMINE && f != null && f.isAfter(debut)
                        && !f.isAfter(fin)) {
                    debut = f;
                }
            }
            match.setInstantLancement(debut);
        }
        if (poule.estTerminee()) {
            poule.definirLancement(null, null);
        }
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
        Poule poule = pouleLancee(tableau, match);
        match.effacerResultat();
        if (poule != null && poule.getNumeroTable() != null) {
            // La poule est toujours sur sa table : le match y reste en cours.
            match.setStatut(StatutMatch.EN_COURS);
            match.setNumeroTable(poule.getNumeroTable());
            match.setInstantLancement(poule.getInstantLancement());
        }
        return ResultatSaisieMatch.enregistre();
    }

    /** @return un refus si le tableau n'accepte pas de saisie pour ce match, sinon null. */
    static ResultatSaisieMatch verifierAcces(Tableau tableau, Match match) {
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

    LocalDateTime maintenant() {
        return LocalDateTime.ofInstant(horloge.maintenant(), ZoneId.systemDefault());
    }
}
