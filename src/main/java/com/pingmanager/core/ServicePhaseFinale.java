package com.pingmanager.core;

import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.FormatTournoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Déroulé de la <b>phase finale</b> d'un tableau (tableau à élimination
 * directe après les poules), pour l'écran du juge-arbitre :
 * <ol>
 *   <li>{@link #diagnostiquer} : peut-on construire le tableau final, et sinon
 *       pourquoi (poules non tirées, matchs de poule restants…) ;</li>
 *   <li>{@link #construire} : second tirage au sort (placement des qualifiés,
 *       I.304 / I.305), en réutilisant
 *       {@link ServiceLancementTableau#construirePhaseFinale} ;</li>
 *   <li>{@link #saisirScore} / {@link #declarerIssue} : résultat d'un match,
 *       validé par {@link ValidationScoreMatch} (2.11.1 / 2.12.1), puis
 *       progression des vainqueurs ({@link ProgressionTableau}) ;</li>
 *   <li>{@link #podium} : 1er, 2e, 3e(s).</li>
 * </ol>
 *
 * <p>Ne connaît ni l'interface ni la persistance : l'appelant enregistre le
 * tournoi après chaque modification réussie.</p>
 */
public final class ServicePhaseFinale {

    /**
     * Nom du tour de la petite finale, tel que posé par {@link MoteurTournoi} et
     * reconnu par {@link ProgressionTableau}.
     */
    public static final String NOM_TOUR_PETITE_FINALE = "Petite finale";

    private final ServiceLancementTableau lancement;
    private final ProgressionTableau progression;
    private final ValidationScoreMatch validation;

    public ServicePhaseFinale() {
        this(new ServiceLancementTableau(), new ProgressionTableau(), new ValidationScoreMatch());
    }

    public ServicePhaseFinale(ServiceLancementTableau lancement, ProgressionTableau progression,
                              ValidationScoreMatch validation) {
        this.lancement = Objects.requireNonNull(lancement, "lancement");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    // ------------------------------------------------------------------
    // Diagnostic et construction
    // ------------------------------------------------------------------

    /** Indique où en est la phase finale du tableau et si elle peut être construite. */
    public DiagnosticPhaseFinale diagnostiquer(Tableau tableau) {
        Objects.requireNonNull(tableau, "tableau");
        List<Poule> poules = tableau.getPoules();
        int nbPoules = poules.size();
        int poulesNonTerminees = 0;
        int matchsRestants = 0;
        for (Poule p : poules) {
            if (!p.estTerminee()) {
                poulesNonTerminees++;
            }
            for (Match m : p.getMatchs()) {
                if (m.getStatut() != StatutMatch.TERMINE) {
                    matchsRestants++;
                }
            }
        }

        if (tableau.aPhaseFinale()) {
            DiagnosticPhaseFinale.Etat etat = estTerminee(tableau.getPhaseFinale())
                    ? DiagnosticPhaseFinale.Etat.TERMINEE : DiagnosticPhaseFinale.Etat.EN_COURS;
            return new DiagnosticPhaseFinale(etat, nbPoules, poulesNonTerminees, matchsRestants,
                    tableau.getPhaseFinale().getNombreJoueurs());
        }

        FormatTournoi format = tableau.getConfiguration().getFormat();
        int qualifies = format == null ? 0 : qualifiesAttendus(poules, format.qualifiesParPoule());
        if (format == null) {
            return new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.SANS_FORMAT,
                    nbPoules, poulesNonTerminees, matchsRestants, 0);
        }
        if (tableau.getConfiguration().getFormatPhaseFinale() == FormatPhaseFinale.AUCUNE
                || format.formatPhaseFinale() == FormatPhaseFinale.AUCUNE) {
            return new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.SANS_ELIMINATION,
                    nbPoules, poulesNonTerminees, matchsRestants, qualifies);
        }
        if (tableau.getStatut() != StatutTableau.LANCE || nbPoules == 0) {
            return new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.POULES_NON_LANCEES,
                    nbPoules, poulesNonTerminees, matchsRestants, qualifies);
        }
        if (!tableau.poulesTerminees()) {
            return new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.POULES_EN_COURS,
                    nbPoules, poulesNonTerminees, matchsRestants, qualifies);
        }
        if (qualifies < 2) {
            return new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.QUALIFIES_INSUFFISANTS,
                    nbPoules, 0, 0, qualifies);
        }
        return new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.PRETE_A_CONSTRUIRE,
                nbPoules, 0, 0, qualifies);
    }

    /**
     * Construit le tableau final (second tirage au sort du tableau, I.304 /
     * I.305) si le diagnostic le permet ; sinon renvoie le diagnostic sans rien
     * modifier.
     *
     * @param graine graine du tirage, à conserver pour rejouer/vérifier le tirage
     */
    public ResultatConstructionPhaseFinale construire(Tableau tableau, long graine) {
        DiagnosticPhaseFinale avant = diagnostiquer(tableau);
        if (!avant.peutConstruire()) {
            return ResultatConstructionPhaseFinale.refuse(avant);
        }
        TableauElimination elimination = lancement.construirePhaseFinale(tableau, graine);
        return ResultatConstructionPhaseFinale.construit(diagnostiquer(tableau), elimination, graine);
    }

    private static int qualifiesAttendus(List<Poule> poules, int qualifiesParPoule) {
        int total = 0;
        for (Poule p : poules) {
            total += Math.min(qualifiesParPoule, p.getTaille());
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Saisie des résultats
    // ------------------------------------------------------------------

    /**
     * Enregistre le score d'un match du tableau final, puis fait progresser le
     * tableau. Un match déjà joué peut être corrigé tant que la correction ne
     * change pas un joueur déjà engagé dans un match suivant en cours ou joué.
     */
    public ResultatSaisieScore saisirScore(Tableau tableau, Match match, List<Manche> manches) {
        Objects.requireNonNull(manches, "manches");
        ResultatSaisieScore prealable = controlerMatch(tableau, match);
        if (prealable != null) {
            return prealable;
        }
        ResultatSaisieScore valide = validation.valider(manches, match.getNombreManchesGagnantes());
        if (!valide.estValide()) {
            return valide;
        }
        int gagneesA = 0;
        for (Manche m : manches) {
            if (m.gagneeParA()) {
                gagneesA++;
            }
        }
        Joueur vainqueur = gagneesA >= match.getNombreManchesGagnantes() ? match.getJoueurA() : match.getJoueurB();
        List<Manche> copie = List.copyOf(manches);
        return appliquer(tableau.getPhaseFinale(), match, vainqueur, () -> match.definirScore(copie));
    }

    /**
     * Déclare une issue non normale (forfait, abandon, refus de jouer,
     * disqualification) : l'adversaire du fautif est qualifié pour le tour
     * suivant. Mêmes règles de correction que {@link #saisirScore}.
     */
    public ResultatSaisieScore declarerIssue(Tableau tableau, Match match, IssueMatch issue, Joueur fautif) {
        ResultatSaisieScore prealable = controlerMatch(tableau, match);
        if (prealable != null) {
            return prealable;
        }
        if (issue == null || issue == IssueMatch.NORMALE || fautif == null
                || !(fautif.equals(match.getJoueurA()) || fautif.equals(match.getJoueurB()))) {
            return ResultatSaisieScore.refus(ResultatSaisieScore.Motif.ISSUE_INVALIDE);
        }
        Joueur vainqueur = fautif.equals(match.getJoueurA()) ? match.getJoueurB() : match.getJoueurA();
        return appliquer(tableau.getPhaseFinale(), match, vainqueur, () -> match.declarerIssue(issue, fautif));
    }

    /** Contrôles communs : match du tableau, joueurs connus. {@code null} si tout va bien. */
    private ResultatSaisieScore controlerMatch(Tableau tableau, Match match) {
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(match, "match");
        if (!tableau.aPhaseFinale() || !contient(tableau.getPhaseFinale(), match)) {
            return ResultatSaisieScore.refus(ResultatSaisieScore.Motif.MATCH_HORS_TABLEAU);
        }
        if (!estConnu(match.getJoueurA()) || !estConnu(match.getJoueurB())) {
            return ResultatSaisieScore.refus(ResultatSaisieScore.Motif.JOUEURS_NON_CONNUS);
        }
        return null;
    }

    /**
     * Applique un résultat. Pour une correction d'un match déjà joué qui change
     * le vainqueur, les places des matchs suivants (vainqueur au tour suivant,
     * perdant en petite finale) sont mises à jour, à condition que ces matchs ne
     * soient ni en cours ni joués.
     */
    private ResultatSaisieScore appliquer(TableauElimination elimination, Match match,
                                          Joueur nouveauVainqueur, Runnable enregistrer) {
        boolean correction = match.getStatut() == StatutMatch.TERMINE;
        Joueur ancienVainqueur = correction ? match.getVainqueur() : null;
        boolean changeDeVainqueur = correction && ancienVainqueur != null
                && !ancienVainqueur.equals(nouveauVainqueur);

        List<Match> suivants = suivants(elimination, match);
        if (changeDeVainqueur) {
            for (Match suivant : suivants) {
                if (suivant.getStatut() == StatutMatch.TERMINE || suivant.getStatut() == StatutMatch.EN_COURS) {
                    return ResultatSaisieScore.refus(ResultatSaisieScore.Motif.SUITE_DEJA_JOUEE);
                }
            }
        }

        enregistrer.run();

        if (changeDeVainqueur) {
            for (Match suivant : suivants) {
                boolean petiteFinale = estDansPetiteFinale(elimination, suivant);
                Joueur entrant = petiteFinale ? match.getPerdant() : match.getVainqueur();
                if (suivant.getSourceA() == match) {
                    suivant.remplacerJoueurs(entrant, suivant.getJoueurB());
                } else {
                    suivant.remplacerJoueurs(suivant.getJoueurA(), entrant);
                }
            }
        }
        progression.propager(elimination);
        return ResultatSaisieScore.valide();
    }

    private static List<Match> suivants(TableauElimination elimination, Match match) {
        List<Match> r = new ArrayList<>();
        for (Tour tour : elimination.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.getSourceA() == match || m.getSourceB() == match) {
                    r.add(m);
                }
            }
        }
        return r;
    }

    private static boolean estDansPetiteFinale(TableauElimination elimination, Match match) {
        Tour pf = petiteFinale(elimination);
        return pf != null && pf.getMatchs().contains(match);
    }

    private static boolean contient(TableauElimination elimination, Match match) {
        for (Tour tour : elimination.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m == match) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean estConnu(Joueur j) {
        return j != null && !j.estADeterminer();
    }

    // ------------------------------------------------------------------
    // Structure du tableau et podium
    // ------------------------------------------------------------------

    /** @return true si ce tour est la petite finale (match pour la 3e place). */
    public static boolean estPetiteFinale(Tour tour) {
        return tour != null && NOM_TOUR_PETITE_FINALE.equals(tour.getNom());
    }

    /** Tours du tableau principal, du premier tour à la finale (sans la petite finale). */
    public static List<Tour> toursPrincipaux(TableauElimination elimination) {
        List<Tour> r = new ArrayList<>();
        for (Tour t : elimination.getTours()) {
            if (!estPetiteFinale(t)) {
                r.add(t);
            }
        }
        return r;
    }

    /** @return le tour de petite finale, ou {@code null} s'il n'y en a pas. */
    public static Tour petiteFinale(TableauElimination elimination) {
        for (Tour t : elimination.getTours()) {
            if (estPetiteFinale(t)) {
                return t;
            }
        }
        return null;
    }

    /** @return le match de la finale, ou {@code null} si le tableau est vide. */
    public static Match matchFinale(TableauElimination elimination) {
        List<Tour> principaux = toursPrincipaux(elimination);
        if (principaux.isEmpty()) {
            return null;
        }
        List<Match> matchs = principaux.get(principaux.size() - 1).getMatchs();
        return matchs.isEmpty() ? null : matchs.get(0);
    }

    /** @return true si la finale et, le cas échéant, la petite finale sont jouées. */
    public static boolean estTerminee(TableauElimination elimination) {
        Match finale = matchFinale(elimination);
        if (finale == null || finale.getStatut() != StatutMatch.TERMINE) {
            return false;
        }
        Tour pf = petiteFinale(elimination);
        if (pf != null) {
            for (Match m : pf.getMatchs()) {
                if (m.getStatut() != StatutMatch.TERMINE) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Podium du tableau : 1er et 2e dès que la finale est jouée ; 3e (et 4e)
     * par la petite finale si elle existe, sinon les perdants des demi-finales,
     * 3es ex aequo (choix provisoire, voir {@link PodiumPhaseFinale}).
     */
    public static PodiumPhaseFinale podium(TableauElimination elimination) {
        Objects.requireNonNull(elimination, "elimination");
        Joueur premier = null;
        Joueur deuxieme = null;
        Match finale = matchFinale(elimination);
        if (finale != null && finale.getStatut() == StatutMatch.TERMINE) {
            premier = finale.getVainqueur();
            deuxieme = finale.getPerdant();
        }

        List<Joueur> troisiemes = new ArrayList<>();
        Joueur quatrieme = null;
        Tour pf = petiteFinale(elimination);
        if (pf != null) {
            for (Match m : pf.getMatchs()) {
                if (m.getStatut() == StatutMatch.TERMINE && m.getVainqueur() != null) {
                    troisiemes.add(m.getVainqueur());
                    quatrieme = m.getPerdant();
                }
            }
        } else {
            List<Tour> principaux = toursPrincipaux(elimination);
            if (principaux.size() >= 2) {
                Tour demies = principaux.get(principaux.size() - 2);
                for (Match m : demies.getMatchs()) {
                    if (m.getStatut() == StatutMatch.TERMINE && m.getPerdant() != null) {
                        troisiemes.add(m.getPerdant());
                    }
                }
            }
        }
        return new PodiumPhaseFinale(premier, deuxieme, troisiemes, quatrieme, pf != null);
    }
}
