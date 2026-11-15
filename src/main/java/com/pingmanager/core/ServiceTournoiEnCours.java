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
 *   <li>soit il est homologué ou publié et l'on est entre <b>J-7</b>
 *       ({@link #JOURS_AVANT} jours avant son premier jour, B047 : l'organisateur
 *       prépare son jour J la semaine qui précède) et son dernier jour
 *       ({@link Tournoi#getJours()}) ;</li>
 *   <li>soit au moins un de ses tableaux est commencé (pointage ouvert, tirage
 *       fait, matchs en cours) et pas terminé, et l'on est entre J-7 et le
 *       lendemain de son dernier jour (tournoi qui déborde après minuit).
 *       Au-delà, un tableau resté « commencé » (tournoi abandonné, fichier
 *       ancien) ne rend plus le tournoi en cours : il reste accessible par
 *       « Gestion des tournois » (choix prudent, README section 6).</li>
 * </ul>
 *
 * <p>La date du jour vient du port {@link Horloge}, dans le fuseau donné.</p>
 */
public final class ServiceTournoiEnCours {

    /** B047 : le tournoi devient « en cours » ce nombre de jours avant son premier jour (J-7). */
    public static final int JOURS_AVANT = 7;

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
        LocalDate debut = debutPreparation(t);
        LocalDate fin = t.getJours().last();
        if (homologueOuPublie && !jour.isBefore(debut) && !jour.isAfter(fin)) {
            return true;
        }
        boolean dansLaPeriode = !jour.isBefore(debut) && !jour.isAfter(fin.plusDays(1));
        return dansLaPeriode && aUnTableauCommenceNonTermine(t);
    }

    /** @return J-7 : le jour où la préparation du jour J s'ouvre ({@link #JOURS_AVANT} jours avant le premier jour). */
    public static LocalDate debutPreparation(Tournoi t) {
        return t.getJours().first().minusDays(JOURS_AVANT);
    }

    /**
     * @return le nombre de jours d'ici le premier jour du tournoi : 7 à J-7, 0 le
     *         premier jour, négatif ensuite
     */
    public long joursAvantPremierJour(Tournoi t) {
        return ChronoUnit.DAYS.between(aujourdhui(), t.getJours().first());
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
