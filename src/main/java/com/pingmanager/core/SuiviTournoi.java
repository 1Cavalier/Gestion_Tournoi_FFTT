package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Suivi public d'un tournoi en cours, <b>en lecture seule</b> : ce que voit
 * n'importe quel compte connecté une fois le premier tableau lancé (tableaux,
 * matchs en cours, prochaine partie d'un joueur inscrit). Aucune méthode ne
 * modifie le tournoi : c'est le juge-arbitre qui pilote depuis la page Jour J.
 */
public final class SuiviTournoi {

    private SuiviTournoi() {
    }

    /** Une partie d'un joueur : son tableau, sa poule ou son tour du tableau final, et le match. */
    public record Partie(Tableau tableau, Poule poule, Tour tour, Match match) {
        public Partie {
            Objects.requireNonNull(tableau, "tableau");
            Objects.requireNonNull(match, "match");
        }

        /** @return true si la partie se joue en ce moment sur une table. */
        public boolean estEnCours() {
            return match.getStatut() == StatutMatch.EN_COURS;
        }

        /** @return true pour un match de poule, false pour un match du tableau final. */
        public boolean estEnPoule() {
            return poule != null;
        }

        /** I.301 : tour (à partir de 1) du match dans l'ordre de sa poule ; 0 hors poule. */
        public int tourDePoule() {
            return poule == null ? 0 : SuiviPoule.tourDuMatch(poule, poule.getMatchs().indexOf(match));
        }

        /** L'adversaire du joueur de licence donnée ({@code null} s'il n'est pas encore connu). */
        public Joueur adversaire(String numeroLicence) {
            Joueur a = match.getJoueurA();
            Joueur b = match.getJoueurB();
            Joueur adv = a != null && numeroLicence.equals(a.getNumeroLicence()) ? b : a;
            return adv == null || adv.estADeterminer() ? null : adv;
        }
    }

    /**
     * Le tournoi passe en mode suivi dès qu'il est publié et que son premier
     * tableau est lancé (tirage des poules fait).
     */
    public static boolean estEnSuivi(Tournoi tournoi) {
        if (tournoi == null || tournoi.getStatut() != StatutTournoi.PUBLIE) {
            return false;
        }
        for (Tableau t : tournoi.getTableaux()) {
            if (estCommence(t)) {
                return true;
            }
        }
        return false;
    }

    /** @return true si le tableau est lancé (poules tirées) ou en phase finale. */
    public static boolean estCommence(Tableau tableau) {
        return tableau.getStatut() == StatutTableau.LANCE || tableau.getStatut() == StatutTableau.PHASE_FINALE;
    }

    /** @return true si le joueur est inscrit (hors liste d'attente) au tableau. */
    public static boolean estInscrit(Tableau tableau, String numeroLicence) {
        if (numeroLicence == null) {
            return false;
        }
        for (Joueur j : tableau.getInscrits()) {
            if (numeroLicence.equals(j.getNumeroLicence())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tableaux du tournoi dans l'ordre de la journée (heure de début, puis nom ;
     * sans horaire à la fin), ceux du joueur en premier si une licence est donnée.
     */
    public static List<Tableau> tableauxOrdonnes(Tournoi tournoi, String numeroLicence) {
        List<Tableau> tries = new ArrayList<>(tournoi.getTableaux());
        tries.sort(Comparator
                .comparing((Tableau t) -> t.getHoraires().getDebutMatchs(),
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()))
                .thenComparing(Tableau::getNom));
        List<Tableau> res = new ArrayList<>();
        for (Tableau t : tries) {
            if (estInscrit(t, numeroLicence)) {
                res.add(t);
            }
        }
        for (Tableau t : tries) {
            if (!res.contains(t)) {
                res.add(t);
            }
        }
        return res;
    }

    /** @return le nombre de poules terminées du tableau. */
    public static int poulesTerminees(Tableau tableau) {
        int n = 0;
        for (Poule p : tableau.getPoules()) {
            if (p.estTerminee()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Tour en cours du tableau final : le premier tour du tableau principal qui
     * n'est pas terminé ; la petite finale si elle reste seule à jouer ;
     * {@code null} si tout est joué.
     */
    public static Tour tourEnCours(TableauElimination elimination) {
        for (Tour t : ServicePhaseFinale.toursPrincipaux(elimination)) {
            if (!t.estTermine()) {
                return t;
            }
        }
        Tour pf = ServicePhaseFinale.petiteFinale(elimination);
        return pf != null && !pf.estTermine() ? pf : null;
    }

    /** Matchs du tableau qui se jouent en ce moment : poules d'abord, puis tableau final. */
    public static List<Partie> matchsEnCours(Tableau tableau) {
        List<Partie> res = new ArrayList<>();
        for (Partie p : parties(tableau)) {
            if (p.estEnCours()) {
                res.add(p);
            }
        }
        return res;
    }

    /**
     * Prochaine partie du joueur dans le tournoi : celle qu'il joue en ce moment
     * s'il y en a une ; sinon, dans l'ordre de ses tableaux
     * ({@link #tableauxOrdonnes}), son premier match non joué (ordre officiel de
     * la poule, I.301, puis tableau final), en préférant un match dont
     * l'adversaire est connu. Vide s'il n'a plus rien à jouer pour l'instant.
     */
    public static Optional<Partie> prochainePartie(Tournoi tournoi, String numeroLicence) {
        if (tournoi == null || numeroLicence == null) {
            return Optional.empty();
        }
        Partie premiere = null;
        Partie premiereAvecAdversaire = null;
        for (Tableau t : tableauxOrdonnes(tournoi, numeroLicence)) {
            if (!estInscrit(t, numeroLicence) || !estCommence(t)) {
                continue;
            }
            for (Partie p : parties(t)) {
                if (!concerne(p.match(), numeroLicence) || p.match().getStatut() == StatutMatch.TERMINE
                        || p.match().estBye()) {
                    continue;
                }
                if (p.estEnCours()) {
                    return Optional.of(p);
                }
                if (premiere == null) {
                    premiere = p;
                }
                if (premiereAvecAdversaire == null && p.adversaire(numeroLicence) != null) {
                    premiereAvecAdversaire = p;
                }
            }
        }
        return Optional.ofNullable(premiereAvecAdversaire != null ? premiereAvecAdversaire : premiere);
    }

    /** Tous les matchs du tableau, poules (dans l'ordre I.301) puis tableau final (tour par tour). */
    static List<Partie> parties(Tableau tableau) {
        List<Partie> res = new ArrayList<>();
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                res.add(new Partie(tableau, p, null, m));
            }
        }
        if (tableau.aPhaseFinale()) {
            for (Tour t : tableau.getPhaseFinale().getTours()) {
                for (Match m : t.getMatchs()) {
                    res.add(new Partie(tableau, null, t, m));
                }
            }
        }
        return res;
    }

    private static boolean concerne(Match m, String numeroLicence) {
        return estJoueur(m.getJoueurA(), numeroLicence) || estJoueur(m.getJoueurB(), numeroLicence);
    }

    private static boolean estJoueur(Joueur j, String numeroLicence) {
        return j != null && !j.estADeterminer() && numeroLicence.equals(j.getNumeroLicence());
    }
}
