package com.pingmanager.core;

import com.pingmanager.model.StrategieOrdonnancement;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Statistiques d'une stratégie sur de nombreux tirages du même tournoi (B025,
 * méthode de Monte-Carlo) : chaque tirage rejoue le tournoi avec d'autres
 * poules, d'autres résultats et d'autres durées de match.
 *
 * <p>Les heures sont résumées par leur moyenne et leurs quantiles : « 9 fois sur
 * 10, le tournoi est fini avant {@link #finP90()} ». Les attentes sont en minutes.</p>
 *
 * @param strategie            stratégie simulée
 * @param tirages              nombre de tirages
 * @param nombreDeTables       tables disponibles
 * @param finMoyenne           heure de fin moyenne du tournoi
 * @param finP10               1 fois sur 10, le tournoi finit avant cette heure
 * @param finMediane           1 fois sur 2
 * @param finP90               9 fois sur 10
 * @param finMax               fin la plus tardive observée
 * @param attenteMoyenne       attente moyenne d'un joueur avant chaque partie, en minutes
 * @param attenteMaxP90        attente la plus longue d'un tirage, 9 fois sur 10 inférieure à… (minutes)
 * @param occupationMoyenne    part moyenne du temps où les tables sont occupées (0 à 1)
 * @param probabiliteDebordement part des tirages où au moins un tableau finit après son heure souhaitée
 * @param tiragesIncomplets    tirages arrêtés avant la fin (blocage)
 * @param tableaux             statistiques par tableau, dans l'ordre du scénario
 */
public record StatistiquesSimulation(StrategieOrdonnancement strategie, int tirages, int nombreDeTables,
                                     LocalDateTime finMoyenne, LocalDateTime finP10, LocalDateTime finMediane,
                                     LocalDateTime finP90, LocalDateTime finMax, double attenteMoyenne,
                                     double attenteMaxP90, double occupationMoyenne,
                                     double probabiliteDebordement, int tiragesIncomplets,
                                     List<StatTableau> tableaux) {

    public StatistiquesSimulation {
        Objects.requireNonNull(strategie, "strategie");
        tableaux = List.copyOf(tableaux);
    }

    /**
     * Statistiques d'un tableau sur tous les tirages.
     *
     * @param nom                    nom du tableau
     * @param finSouhaitee           heure à ne pas dépasser
     * @param finMoyenne             fin moyenne du tableau
     * @param finP90                 9 fois sur 10, le tableau est fini avant cette heure
     * @param probabiliteDebordement part des tirages où il finit après l'heure souhaitée
     * @param attenteMoyenne         attente moyenne de ses joueurs, en minutes
     */
    public record StatTableau(String nom, LocalDateTime finSouhaitee, LocalDateTime finMoyenne,
                              LocalDateTime finP90, double probabiliteDebordement, double attenteMoyenne) {
    }

    /** Agrège les résultats de tirages d'une même stratégie. */
    static StatistiquesSimulation depuis(StrategieOrdonnancement strategie, int tables,
                                         List<ResultatSimulation> resultats) {
        List<Long> fins = new ArrayList<>();
        double attente = 0;
        double occupation = 0;
        int debordements = 0;
        int incomplets = 0;
        long[] attentesMax = new long[resultats.size()];
        Map<String, List<ResultatSimulation.BilanTableau>> parTableau = new LinkedHashMap<>();
        for (int i = 0; i < resultats.size(); i++) {
            ResultatSimulation r = resultats.get(i);
            if (r.fin() != null) {
                fins.add(secondes(r.fin()));
            }
            attente += r.attenteMoyenne();
            occupation += r.occupationTables();
            attentesMax[i] = r.attenteMaximale();
            if (!r.tableauxQuiDebordent().isEmpty()) {
                debordements++;
            }
            if (!r.complete()) {
                incomplets++;
            }
            for (ResultatSimulation.BilanTableau b : r.tableaux()) {
                parTableau.computeIfAbsent(b.nom(), k -> new ArrayList<>()).add(b);
            }
        }
        int n = Math.max(1, resultats.size());
        long[] f = fins.stream().mapToLong(Long::longValue).sorted().toArray();
        Arrays.sort(attentesMax);
        List<StatTableau> tableaux = new ArrayList<>();
        for (Map.Entry<String, List<ResultatSimulation.BilanTableau>> e : parTableau.entrySet()) {
            List<ResultatSimulation.BilanTableau> bs = e.getValue();
            long[] ft = bs.stream().filter(b -> b.fin() != null).mapToLong(b -> secondes(b.fin())).sorted().toArray();
            long deb = bs.stream().filter(ResultatSimulation.BilanTableau::deborde).count();
            double att = bs.stream().mapToDouble(ResultatSimulation.BilanTableau::attenteMoyenne).average().orElse(0);
            tableaux.add(new StatTableau(e.getKey(), bs.get(0).finSouhaitee(), heure(moyenne(ft)),
                    heure(quantile(ft, 0.9)), (double) deb / bs.size(), att));
        }
        return new StatistiquesSimulation(strategie, resultats.size(), tables, heure(moyenne(f)),
                heure(quantile(f, 0.1)), heure(quantile(f, 0.5)), heure(quantile(f, 0.9)),
                f.length == 0 ? null : heure((double) f[f.length - 1]), attente / n,
                quantile(attentesMax, 0.9) == null ? 0 : quantile(attentesMax, 0.9), occupation / n,
                (double) debordements / n, incomplets, tableaux);
    }

    /** Quantile empirique (interpolé) d'une série triée, ou {@code null} si vide. */
    static Double quantile(long[] tries, double q) {
        if (tries.length == 0) {
            return null;
        }
        double pos = q * (tries.length - 1);
        int bas = (int) Math.floor(pos);
        int haut = Math.min(tries.length - 1, bas + 1);
        return tries[bas] + (pos - bas) * (tries[haut] - tries[bas]);
    }

    private static Double moyenne(long[] v) {
        return v.length == 0 ? null : Arrays.stream(v).average().orElse(0);
    }

    private static long secondes(LocalDateTime t) {
        return t.toEpochSecond(ZoneOffset.UTC);
    }

    private static LocalDateTime heure(Double secondes) {
        return secondes == null ? null : LocalDateTime.ofEpochSecond(Math.round(secondes), 0, ZoneOffset.UTC);
    }
}
