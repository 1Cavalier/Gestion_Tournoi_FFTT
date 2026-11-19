package com.pingmanager.core;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Saisie de la <b>poule entière d'un coup</b>, au retour de la feuille de poule :
 * tous ses matchs dans l'ordre de la feuille (I.301), les résultats déjà saisis
 * repris et modifiables, <b>une seule validation</b> qui enregistre tout ou rien.
 *
 * <p>Mêmes règles que la saisie match par match ({@link SaisieResultatMatch}) :
 * 2.11.1 et 2.12.1 pour les manches, I.303 a) pour les issues anormales, un
 * score seulement sur un match lancé (un match d'une poule lancée compte comme
 * lancé), poules modifiables seulement pendant la phase de poules. Tout est
 * vérifié avant d'écrire : au premier refus, rien n'est enregistré et le refus
 * dit quelle partie corriger.</p>
 *
 * <p>Un match laissé vide n'est pas touché (il reste à saisir). Un résultat
 * inchangé garde ses heures. Les durées des matchs saisis d'un coup ne sont pas
 * connues : elles ne sont pas apprises. Si la poule est lancée et que tous ses
 * matchs ont désormais un résultat, la table et les joueurs sont libérés.</p>
 */
public final class SaisiePoule {

    /**
     * Le résultat d'une partie tel qu'il est saisi sur l'écran de la poule.
     *
     * @param match   le match de la poule
     * @param manches manches saisies (issue normale), vide si rien n'est saisi
     * @param issue   issue : {@link IssueMatch#NORMALE} pour un score
     * @param fautif  joueur fautif d'une issue anormale, sinon null
     */
    public record Partie(Match match, List<Manche> manches, IssueMatch issue, Joueur fautif) {
        public Partie {
            Objects.requireNonNull(match, "match");
            manches = manches == null ? List.of() : List.copyOf(manches);
            issue = issue == null ? IssueMatch.NORMALE : issue;
        }

        /** Score joué normalement. */
        public static Partie score(Match match, List<Manche> manches) {
            return new Partie(match, manches, IssueMatch.NORMALE, null);
        }

        /** Issue anormale (forfait, abandon, refus de jouer, disqualification). */
        public static Partie issue(Match match, IssueMatch issue, Joueur fautif) {
            return new Partie(match, List.of(), issue, fautif);
        }

        /** Rien n'est saisi pour ce match. */
        public boolean estVide() {
            return issue == IssueMatch.NORMALE && manches.isEmpty();
        }
    }

    /**
     * Issue de la validation.
     *
     * @param detail      refus de la partie fautive, ou {@link ResultatSaisieMatch.Motif#ENREGISTRE}
     * @param partie      numéro de la partie refusée dans l'ordre de la feuille (1, 2…), 0 si tout est enregistré
     * @param enregistres nombre de résultats nouveaux ou modifiés
     * @param liberee     la poule a reçu son dernier résultat : table et joueurs libérés
     */
    public record Resultat(ResultatSaisieMatch detail, int partie, int enregistres, boolean liberee) {
        public boolean estEnregistre() {
            return detail.estEnregistre();
        }
    }

    private final SaisieResultatMatch saisie;

    public SaisiePoule(SaisieResultatMatch saisie) {
        this.saisie = Objects.requireNonNull(saisie, "saisie");
    }

    /**
     * Vérifie puis enregistre d'un coup les résultats de la poule.
     *
     * @param parties une saisie par match (ordre indifférent ; les matchs absents ne sont pas touchés)
     */
    public Resultat enregistrer(Tableau tableau, Poule poule, List<Partie> parties) {
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(poule, "poule");
        Objects.requireNonNull(parties, "parties");
        if (!tableau.getPoules().contains(poule)) {
            return new Resultat(ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_HORS_TABLEAU), 0, 0, false);
        }
        // 1) Tout vérifier, sans rien écrire.
        List<Partie> aEcrire = new ArrayList<>();
        for (Partie p : parties) {
            int numero = poule.getMatchs().indexOf(p.match()) + 1;
            if (numero == 0) {
                return new Resultat(ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_HORS_TABLEAU),
                        0, 0, false);
            }
            if (p.estVide() || inchange(p)) {
                continue;
            }
            ResultatSaisieMatch r = verifier(tableau, p);
            if (!r.estEnregistre()) {
                return new Resultat(r, numero, 0, false);
            }
            aEcrire.add(p);
        }
        // 2) Tout écrire.
        LocalDateTime fin = saisie.maintenant();
        boolean lancee = poule.estLanceeEnBloc();
        for (Partie p : aEcrire) {
            Match m = p.match();
            m.effacerResultat();
            if (p.issue() == IssueMatch.NORMALE) {
                m.definirScore(p.manches());
            } else {
                m.declarerIssue(p.issue(), p.fautif());
            }
            SaisieResultatMatch.terminerDansLaPoule(lancee ? poule : null, m, fin, true);
        }
        boolean liberee = lancee && !poule.estLanceeEnBloc();
        return new Resultat(ResultatSaisieMatch.enregistre(), 0, aEcrire.size(), liberee);
    }

    private ResultatSaisieMatch verifier(Tableau tableau, Partie p) {
        Match m = p.match();
        ResultatSaisieMatch acces = SaisieResultatMatch.verifierAcces(tableau, m);
        if (acces != null) {
            return acces;
        }
        if (m.estBye()) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_EXEMPT);
        }
        if (p.issue() == IssueMatch.NORMALE) {
            if (!SaisieResultatMatch.scoreSaisissable(m)) {
                return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_NON_LANCE);
            }
            return saisie.verifierScore(m, p.manches());
        }
        Joueur fautif = p.fautif();
        if (fautif == null || (!fautif.equals(m.getJoueurA()) && !fautif.equals(m.getJoueurB()))) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.FAUTIF_HORS_MATCH);
        }
        if (!SaisieResultatMatch.issueSaisissable(m, p.issue())) {
            return ResultatSaisieMatch.refus(ResultatSaisieMatch.Motif.MATCH_NON_LANCE);
        }
        return ResultatSaisieMatch.enregistre();
    }

    /** Le résultat saisi est-il celui déjà enregistré (rien à réécrire) ? */
    private static boolean inchange(Partie p) {
        Match m = p.match();
        if (m.getStatut() != StatutMatch.TERMINE || m.getIssue() != p.issue()) {
            return false;
        }
        if (p.issue() == IssueMatch.NORMALE) {
            return m.getManches().equals(p.manches());
        }
        return Objects.equals(m.getJoueurFautif(), p.fautif());
    }
}
