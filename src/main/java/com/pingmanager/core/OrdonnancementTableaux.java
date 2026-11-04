package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.ModeLimiteClassement;
import com.pingmanager.model.Tableau;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Ordonnancement automatique des tableaux d'un tournoi.
 *
 * <ol>
 *   <li><b>Classement par niveau</b> : du plus faible au plus fort, d'après le
 *       plafond de points (tranche ou points maximum) ; les tableaux toutes séries
 *       viennent ensuite (ceux qui excluent des numérotés avant les autres).</li>
 *   <li><b>Répartition sur les jours</b> : un tableau sur x pour chacun des x
 *       jours (le 1er, le (x+1)e… le premier jour ; le 2e, le (x+2)e… le
 *       deuxième jour), ce qui équilibre les niveaux entre les journées.</li>
 *   <li><b>Alternance dans la journée</b> : les tableaux du jour sont coupés en
 *       deux moitiés, faibles et moyens/forts, puis alternés : faible, moyen,
 *       faible + 1, moyen + 1… Deux tableaux de niveaux voisins ne se suivent
 *       donc jamais, et les joueurs peuvent enchaîner deux tableaux éloignés.</li>
 *   <li><b>Tableaux dames en dernier</b> (option) : les tableaux réservés aux
 *       joueuses sortent de l'alternance et terminent leur journée.</li>
 *   <li><b>Horaires</b> : le premier tableau de chaque jour commence à l'heure
 *       choisie ; les écarts suivants parcourent une liste en boucle (ex. 1 h 30
 *       puis 2 h : A → 1 h 30 → B → 2 h → C → 1 h 30 → D…) ; le pointage se
 *       termine M minutes avant le début.</li>
 * </ol>
 */
public final class OrdonnancementTableaux {

    /** Place proposée pour un tableau. */
    public record Creneau(Tableau tableau, LocalDate jour, LocalTime debut) {
    }

    private OrdonnancementTableaux() {
    }

    /**
     * @param tableaux   tableaux à ordonner
     * @param jours      jours du tournoi, dans l'ordre (au moins un)
     * @param premier    heure de début du premier tableau de chaque jour
     * @param intervalle écart entre deux débuts de tableau (au moins 1 minute)
     * @return les créneaux, jour par jour puis dans l'ordre de la journée
     */
    public static List<Creneau> proposer(List<Tableau> tableaux, List<LocalDate> jours,
                                         LocalTime premier, Duration intervalle) {
        return proposer(tableaux, jours, premier, List.of(intervalle), false);
    }

    /**
     * @param ecarts          écarts successifs entre deux débuts, parcourus en boucle
     * @param damesEnDernier  les tableaux réservés aux joueuses terminent leur journée
     */
    public static List<Creneau> proposer(List<Tableau> tableaux, List<LocalDate> jours, LocalTime premier,
                                         List<Duration> ecarts, boolean damesEnDernier) {
        Objects.requireNonNull(premier, "premier");
        if (jours == null || jours.isEmpty()) {
            throw new IllegalArgumentException("Renseignez d'abord les dates du tournoi (section Général).");
        }
        if (ecarts == null || ecarts.isEmpty() || ecarts.stream().anyMatch(e -> e == null || e.toMinutes() < 1)) {
            throw new IllegalArgumentException("L'intervalle entre deux tableaux doit être d'au moins une minute.");
        }
        List<Tableau> tries = new ArrayList<>(tableaux);
        tries.sort(Comparator.comparingLong(OrdonnancementTableaux::niveau)
                .thenComparing(Tableau::getNom, String.CASE_INSENSITIVE_ORDER));

        // 1 tableau sur x pour chacun des x jours.
        List<List<Tableau>> parJour = new ArrayList<>();
        jours.forEach(j -> parJour.add(new ArrayList<>()));
        for (int i = 0; i < tries.size(); i++) {
            parJour.get(i % jours.size()).add(tries.get(i));
        }

        List<Creneau> res = new ArrayList<>();
        for (int d = 0; d < jours.size(); d++) {
            List<Tableau> duJour = parJour.get(d);
            List<Tableau> dames = new ArrayList<>();
            if (damesEnDernier) {
                for (Tableau t : duJour) {
                    if (t.getConfiguration().getSexeRequis() == com.pingmanager.model.Sexe.FEMME) {
                        dames.add(t);
                    }
                }
            }
            List<Tableau> autres = new ArrayList<>(duJour);
            autres.removeAll(dames);
            List<Tableau> ordre = alterner(autres);
            ordre.addAll(dames);
            LocalDateTime h = jours.get(d).atTime(premier);
            int k = 0;
            for (Tableau t : ordre) {
                if (!h.toLocalDate().equals(jours.get(d))) {
                    throw new IllegalArgumentException("Trop de tableaux le " + jours.get(d)
                            + " pour cet intervalle : la journée déborde après minuit.");
                }
                res.add(new Creneau(t, jours.get(d), h.toLocalTime()));
                h = h.plus(ecarts.get(k++ % ecarts.size()));
            }
        }
        return res;
    }

    /** Faible, moyen, faible + 1, moyen + 1… (liste déjà triée du plus faible au plus fort). */
    static List<Tableau> alterner(List<Tableau> tries) {
        int moitie = (tries.size() + 1) / 2;
        List<Tableau> faibles = tries.subList(0, moitie);
        List<Tableau> moyens = tries.subList(moitie, tries.size());
        List<Tableau> res = new ArrayList<>();
        for (int i = 0; i < moitie; i++) {
            res.add(faibles.get(i));
            if (i < moyens.size()) {
                res.add(moyens.get(i));
            }
        }
        return res;
    }

    /**
     * Niveau d'un tableau pour le tri : plafond de points ; toutes séries après
     * tous les tableaux limités, ceux qui excluent des numérotés en premier.
     */
    public static long niveau(Tableau t) {
        ConfigurationTableau c = t.getConfiguration();
        if (c.getModeLimite() != ModeLimiteClassement.TOUTES_SERIES && c.getPointsMax() > 0) {
            return c.getPointsMax();
        }
        long base = 1_000_000L;
        return c.aExclusionRangNational() ? base - c.getExclusionRangNational() : base + 1;
    }

    /**
     * Applique les créneaux : début, fin du pointage {@code finPointageAvant} avant
     * le début, ouverture du pointage {@code ouverturePointageAvant} avant le début.
     * Une fin théorique déjà saisie est décalée d'autant (la durée est conservée).
     */
    public static void appliquer(List<Creneau> creneaux, Duration finPointageAvant, Duration ouverturePointageAvant) {
        for (Creneau c : creneaux) {
            LocalDateTime debut = c.jour().atTime(c.debut());
            HorairesTableau ancien = c.tableau().getHoraires();
            LocalDateTime finTheorique = null;
            if (ancien != null && ancien.getFinTheorique() != null && ancien.getDebutMatchs() != null) {
                finTheorique = debut.plus(Duration.between(ancien.getDebutMatchs(), ancien.getFinTheorique()));
            }
            c.tableau().setHoraires(new HorairesTableau(debut.minus(ouverturePointageAvant),
                    debut.minus(finPointageAvant), debut, finTheorique));
        }
    }
}
