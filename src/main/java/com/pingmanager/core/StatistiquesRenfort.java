package com.pingmanager.core;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Usage des tables de renfort sur tous les tirages d'une stratégie (B054).
 *
 * <p>Les <b>plages habituelles</b> sont les quarts d'heure où le renfort est
 * ouvert dans au moins {@value #PART_PLAGE_HABITUELLE_POURCENT} % des tirages,
 * regroupés en plages continues : c'est là qu'il faudra, le plus probablement,
 * ouvrir les tables d'échauffement.</p>
 *
 * @param tablesMax                tables de renfort permises (0 : pas de renfort)
 * @param probabiliteDeploiement   part des tirages où au moins une table de renfort a été ouverte
 * @param utilisationMoyenne       part d'utilisation moyenne des tables de renfort (0 à 1), sur tous
 *                                 les tirages (voir {@link RenfortTirage})
 * @param minutesDeJeuMoyennes     minutes de jeu moyennes sur les tables de renfort
 * @param premiereMiseEnService    première ouverture, médiane sur les tirages qui ont ouvert ({@code null} : jamais)
 * @param premiereMiseEnServiceTot première ouverture la plus précoce observée ({@code null} : jamais)
 * @param tablesEnServiceMax       plus grand nombre de tables ouvertes en même temps (médiane des tirages
 *                                 qui ont ouvert)
 * @param plagesHabituelles        plages où le renfort est ouvert dans au moins la moitié des tirages
 */
public record StatistiquesRenfort(int tablesMax, double probabiliteDeploiement, double utilisationMoyenne,
                                  double minutesDeJeuMoyennes, LocalDateTime premiereMiseEnService,
                                  LocalDateTime premiereMiseEnServiceTot, int tablesEnServiceMax,
                                  List<RenfortTirage.Plage> plagesHabituelles) {

    /** Part des tirages (en %) à partir de laquelle un quart d'heure appartient à une plage habituelle. */
    public static final int PART_PLAGE_HABITUELLE_POURCENT = 50;
    /** Largeur d'un créneau d'agrégation, en minutes. */
    public static final int CRENEAU_MINUTES = 15;

    /** Pas de renfort. */
    public static final StatistiquesRenfort AUCUN = new StatistiquesRenfort(0, 0, 0, 0, null, null, 0, List.of());

    public StatistiquesRenfort {
        plagesHabituelles = List.copyOf(plagesHabituelles);
    }

    /** @return vrai si des tables de renfort étaient permises. */
    public boolean actif() {
        return tablesMax > 0;
    }

    /** Agrège les tirages d'une stratégie. */
    static StatistiquesRenfort depuis(List<ResultatSimulation> resultats) {
        int max = 0;
        for (ResultatSimulation r : resultats) {
            max = Math.max(max, r.renfort().tablesMax());
        }
        if (max == 0 || resultats.isEmpty()) {
            return AUCUN;
        }
        int n = resultats.size();
        int deployes = 0;
        double utilisation = 0;
        double minutes = 0;
        List<Long> premieres = new ArrayList<>();
        List<Long> picTables = new ArrayList<>();
        // Créneau (début, en secondes) → tables ouvertes au plus pendant le créneau, une valeur par tirage.
        Map<Long, List<Integer>> creneaux = new TreeMap<>();
        long largeur = CRENEAU_MINUTES * 60L;
        for (ResultatSimulation r : resultats) {
            RenfortTirage t = r.renfort();
            utilisation += t.utilisation();
            minutes += t.minutesDeJeu();
            if (!t.deploye()) {
                continue;
            }
            deployes++;
            premieres.add(secondes(t.premiereMiseEnService()));
            picTables.add((long) t.tablesEnServiceMax());
            Map<Long, Integer> duTirage = new TreeMap<>();
            for (RenfortTirage.Plage p : t.plages()) {
                long debut = Math.floorDiv(secondes(p.debut()), largeur) * largeur;
                for (long c = debut; c < secondes(p.fin()); c += largeur) {
                    duTirage.merge(c, p.tablesMax(), Math::max);
                }
            }
            duTirage.forEach((c, tables) -> creneaux.computeIfAbsent(c, k -> new ArrayList<>()).add(tables));
        }
        long[] p = premieres.stream().mapToLong(Long::longValue).sorted().toArray();
        long[] pic = picTables.stream().mapToLong(Long::longValue).sorted().toArray();
        Double mediane = StatistiquesSimulation.quantile(p, 0.5);
        Double picMedian = StatistiquesSimulation.quantile(pic, 0.5);
        return new StatistiquesRenfort(max, (double) deployes / n, utilisation / n, minutes / n,
                mediane == null ? null : heure(Math.round(mediane)), p.length == 0 ? null : heure(p[0]),
                picMedian == null ? 0 : (int) Math.round(picMedian), plages(creneaux, n, largeur));
    }

    private static List<RenfortTirage.Plage> plages(Map<Long, List<Integer>> creneaux, int tirages, long largeur) {
        List<RenfortTirage.Plage> plages = new ArrayList<>();
        Long debut = null;
        long fin = 0;
        int tables = 0;
        for (Map.Entry<Long, List<Integer>> e : creneaux.entrySet()) {
            boolean habituel = e.getValue().size() * 100 >= PART_PLAGE_HABITUELLE_POURCENT * tirages;
            if (!habituel) {
                continue;
            }
            int median = mediane(e.getValue());
            if (debut != null && e.getKey() == fin) {
                fin += largeur;
                tables = Math.max(tables, median);
                continue;
            }
            if (debut != null) {
                plages.add(new RenfortTirage.Plage(heure(debut), heure(fin), tables));
            }
            debut = e.getKey();
            fin = e.getKey() + largeur;
            tables = median;
        }
        if (debut != null) {
            plages.add(new RenfortTirage.Plage(heure(debut), heure(fin), tables));
        }
        return plages;
    }

    private static int mediane(List<Integer> valeurs) {
        List<Integer> triees = new ArrayList<>(valeurs);
        triees.sort(null);
        return triees.get(triees.size() / 2);
    }

    private static long secondes(LocalDateTime t) {
        return t.toEpochSecond(ZoneOffset.UTC);
    }

    private static LocalDateTime heure(long secondes) {
        return LocalDateTime.ofEpochSecond(secondes, 0, ZoneOffset.UTC);
    }
}
