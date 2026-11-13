package com.pingmanager.core;

import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * B043 : les <b>jours</b> du pointage d'un tournoi qui dure plusieurs jours.
 *
 * <p>Le jour d'un tableau est celui de ses horaires
 * ({@link com.pingmanager.model.HorairesTableau#getJour()}, déduit du début des
 * matchs) : il ne dépend pas de l'heure qu'il est, donc un tableau du samedi qui
 * finit après minuit reste un tableau du samedi. Un tableau sans horaire est
 * rattaché au premier jour du tournoi et signalé « jour non défini ».</p>
 *
 * <p>Le pointage, le paiement et la clôture restent des faits <b>par tableau</b> :
 * le jour ne sert qu'à séparer l'affichage et à dater les opérations du journal.</p>
 */
public final class JoursPointage {

    /** Avant cette heure, la nuit prolonge encore la journée de la veille. */
    static final int FIN_DE_NUIT = 6;

    private JoursPointage() {
    }

    /** @return le jour du tableau, ou le premier jour du tournoi s'il n'a pas d'horaire. */
    public static LocalDate jourDe(Tournoi tournoi, Tableau tableau) {
        LocalDate j = tableau.getHoraires() == null ? null : tableau.getHoraires().getJour();
        return j != null ? j : tournoi.getJours().first();
    }

    /** @return vrai si le tableau n'a pas d'horaire (rattaché par défaut au premier jour). */
    public static boolean jourNonDefini(Tableau tableau) {
        return tableau.getHoraires() == null || tableau.getHoraires().getJour() == null;
    }

    /** @return les jours du tournoi, plus ceux de ses tableaux s'ils en diffèrent, triés. */
    public static List<LocalDate> jours(Tournoi tournoi) {
        TreeSet<LocalDate> res = new TreeSet<>(tournoi.getJours());
        for (Tableau t : tournoi.getTableaux()) {
            res.add(jourDe(tournoi, t));
        }
        return new ArrayList<>(res);
    }

    /** @return les tableaux de ce jour, dans l'ordre du tournoi. */
    public static List<Tableau> tableauxDu(Tournoi tournoi, LocalDate jour) {
        List<Tableau> res = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (jour == null || jourDe(tournoi, t).equals(jour)) {
                res.add(t);
            }
        }
        return res;
    }

    /** @return vrai si tous les tableaux de ce jour sont terminés (vrai aussi pour un jour sans tableau). */
    public static boolean jourTermine(Tournoi tournoi, LocalDate jour) {
        for (Tableau t : tableauxDu(tournoi, jour)) {
            if (!ServiceTournoiEnCours.estTermine(t)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Jour ouvert à l'arrivée sur l'écran de pointage :
     * <ul>
     *   <li>aujourd'hui, si c'est un jour du tournoi ; mais pendant la nuit (avant
     *       6 h), la veille si c'est un jour du tournoi pas encore terminé
     *       (tournoi qui déborde après minuit) ;</li>
     *   <li>hors des jours du tournoi : le premier jour non terminé, sinon le dernier.</li>
     * </ul>
     */
    public static LocalDate jourParDefaut(Tournoi tournoi, LocalDateTime maintenant) {
        Objects.requireNonNull(maintenant, "maintenant");
        List<LocalDate> jours = jours(tournoi);
        LocalDate aujourdhui = maintenant.toLocalDate();
        if (jours.contains(aujourdhui)) {
            LocalDate veille = aujourdhui.minusDays(1);
            if (maintenant.getHour() < FIN_DE_NUIT && jours.contains(veille) && !jourTermine(tournoi, veille)) {
                return veille;
            }
            return aujourdhui;
        }
        for (LocalDate j : jours) {
            if (!jourTermine(tournoi, j)) {
                return j;
            }
        }
        return jours.get(jours.size() - 1);
    }
}
