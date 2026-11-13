package com.pingmanager.core;

import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * B043 : un tournoi est-il <b>en cours</b> ? Sert à afficher le groupe de
 * navigation « Tournoi en cours » (Gestion, Tableau, Pointage).
 *
 * <p>Un tournoi est en cours s'il n'est pas annulé et :</p>
 * <ul>
 *   <li>soit il est homologué ou publié et l'un de ses jours
 *       ({@link Tournoi#getJours()}) est aujourd'hui ;</li>
 *   <li>soit au moins un de ses tableaux est commencé (pointage ouvert, tirage
 *       fait, matchs en cours) et pas terminé, et l'on est entre son premier jour
 *       et le lendemain de son dernier jour (tournoi qui déborde après minuit).
 *       Au-delà, un tableau resté « commencé » (tournoi abandonné, fichier
 *       ancien) ne rend plus le tournoi en cours : il reste accessible par
 *       « Gestion des tournois » (choix prudent, README section 6).</li>
 * </ul>
 *
 * <p>La date du jour vient du port {@link Horloge}, dans le fuseau donné.</p>
 */
public final class ServiceTournoiEnCours {

    private final Horloge horloge;
    private final ZoneId fuseau;

    public ServiceTournoiEnCours(Horloge horloge, ZoneId fuseau) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.fuseau = Objects.requireNonNull(fuseau, "fuseau");
    }

    /** @return la date du jour selon l'horloge. */
    public LocalDate aujourdhui() {
        return horloge.maintenant().atZone(fuseau).toLocalDate();
    }

    /** @return vrai si le tournoi est en cours aujourd'hui (voir la classe). */
    public boolean estEnCours(Tournoi t) {
        Objects.requireNonNull(t, "tournoi");
        if (t.getStatut() == StatutTournoi.ANNULE) {
            return false;
        }
        LocalDate jour = aujourdhui();
        boolean homologueOuPublie = t.getStatut() == StatutTournoi.HOMOLOGUE
                || t.getStatut() == StatutTournoi.PUBLIE;
        if (homologueOuPublie && t.getJours().contains(jour)) {
            return true;
        }
        boolean dansLaPeriode = !jour.isBefore(t.getJours().first())
                && !jour.isAfter(t.getJours().last().plusDays(1));
        return dansLaPeriode && aUnTableauCommenceNonTermine(t);
    }

    /** @return vrai si un tableau a quitté la phase d'inscription sans être terminé. */
    public static boolean aUnTableauCommenceNonTermine(Tournoi t) {
        for (Tableau tab : t.getTableaux()) {
            if (tab.getStatut() != StatutTableau.INSCRIPTION && !estTermine(tab)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return vrai si le tableau est joué jusqu'au bout : phase finale entièrement
     *         jouée, ou, pour un tableau sans phase finale, toutes ses poules terminées
     */
    public static boolean estTermine(Tableau tab) {
        if (ServiceResultatsFederaux.estTermine(tab)) {
            return true;
        }
        return tab.getConfiguration().getFormatPhaseFinale() == FormatPhaseFinale.AUCUNE
                && tab.getStatut() != StatutTableau.INSCRIPTION && tab.poulesTerminees();
    }

    /** @return l'écart en jours entre aujourd'hui et le jour du tournoi le plus proche. */
    public long ecart(Tournoi t) {
        LocalDate jour = aujourdhui();
        long min = Long.MAX_VALUE;
        for (LocalDate d : t.getJours()) {
            min = Math.min(min, Math.abs(ChronoUnit.DAYS.between(jour, d)));
        }
        return min;
    }

    /**
     * Garde les éléments dont le tournoi est en cours (ou forcé, par exemple le
     * tournoi « jour J » de la démo) et les trie du plus proche d'aujourd'hui au
     * plus lointain (ordre d'origine conservé à égalité).
     *
     * @param elements éléments quelconques (fichier et tournoi…)
     * @param tournoi  accès au tournoi d'un élément
     * @param force    éléments considérés en cours quoi qu'il arrive (jamais s'ils sont annulés)
     */
    public <T> List<T> enCours(Collection<T> elements, Function<T, Tournoi> tournoi, Predicate<T> force) {
        List<T> res = new ArrayList<>();
        for (T e : elements) {
            Tournoi t = tournoi.apply(e);
            if (t == null) {
                continue;
            }
            boolean estForce = force != null && force.test(e) && t.getStatut() != StatutTournoi.ANNULE;
            if (estForce || estEnCours(t)) {
                res.add(e);
            }
        }
        res.sort(Comparator.comparingLong(e -> ecart(tournoi.apply(e))));
        return res;
    }
}
