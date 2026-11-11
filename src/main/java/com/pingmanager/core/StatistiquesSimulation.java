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
 * <p>Début d'un tableau : il a vraiment commencé quand sa dernière poule a
 * commencé. Il est « à l'heure » avec au plus {@value #RETARD_A_L_HEURE} min de
 * retard, en « gros retard » au-delà de {@value #GROS_RETARD} min, en « léger
 * retard » entre les deux. Les cas rares de fin sont les tirages extrêmes :
 * 1 fois sur 20 le tournoi finit avant {@link #finTot()}, 1 fois sur 20 après
 * {@link #finTard()}. Les attentes sont en minutes, par partie.</p>
 *
 * @param strategie               stratégie simulée
 * @param tirages                 nombre de tirages
 * @param nombreDeTables          tables disponibles
 * @param finMoyenne              heure de fin moyenne du tournoi
 * @param finTot                  cas rare de fin tôt (1 fois sur 20, le tournoi finit avant)
 * @param finMediane              1 fois sur 2
 * @param finTard                 cas rare de fin tard (1 fois sur 20, le tournoi finit après)
 * @param finMax                  fin la plus tardive observée
 * @param debutALHeure            part des débuts de tableau à l'heure (0 à 1)
 * @param debutLegerRetard        part des débuts en léger retard
 * @param debutGrosRetard         part des débuts en gros retard (ou jamais commencés)
 * @param attenteMoyenne          attente moyenne d'un joueur avant chaque partie
 * @param attenteUnTableau        attente moyenne d'un joueur inscrit dans un seul tableau (-1 : aucun)
 * @param attentePlusieursTableaux attente moyenne d'un joueur inscrit dans deux tableaux ou plus (-1 : aucun)
 * @param plusLongueAttente       plus longue attente d'un tirage, en moyenne sur les tirages
 * @param occupationMoyenne       part moyenne du temps où les tables sont occupées (0 à 1)
 * @param probabiliteDebordement  part des tirages où au moins un tableau finit après son heure souhaitée
 * @param tempsAnnexesMoyen       temps moyen autour du jeu par match, en minutes
 * @param tiragesIncomplets       tirages arrêtés avant la fin (blocage)
 * @param tableaux                statistiques par tableau, dans l'ordre du scénario
 */
public record StatistiquesSimulation(StrategieOrdonnancement strategie, int tirages, int nombreDeTables,
                                     LocalDateTime finMoyenne, LocalDateTime finTot, LocalDateTime finMediane,
                                     LocalDateTime finTard, LocalDateTime finMax, double debutALHeure,
                                     double debutLegerRetard, double debutGrosRetard, double attenteMoyenne,
                                     double attenteUnTableau, double attentePlusieursTableaux,
                                     double plusLongueAttente, double occupationMoyenne,
                                     double probabiliteDebordement, double tempsAnnexesMoyen,
                                     int tiragesIncomplets,
                                     List<StatTableau> tableaux) {

    /** Retard de début toléré pour être « à l'heure », en minutes. */
    public static final int RETARD_A_L_HEURE = 5;
    /** Au-delà, le début est en gros retard, en minutes. */
    public static final int GROS_RETARD = 30;

    public StatistiquesSimulation {
        Objects.requireNonNull(strategie, "strategie");
        tableaux = List.copyOf(tableaux);
    }

    /**
     * Statistiques d'un tableau sur tous les tirages.
     *
     * @param nom                    nom du tableau
     * @param debutPrevu             heure de début prévue
     * @param retardDebutMoyen       retard moyen du début réel, en minutes
     * @param finSouhaitee           heure à ne pas dépasser
     * @param finMoyenne             fin moyenne du tableau
     * @param finTard                cas rare de fin tard (1 fois sur 20, il finit après)
     * @param probabiliteDebordement part des tirages où il finit après l'heure souhaitée
     * @param attenteMoyenne         attente moyenne de ses joueurs, en minutes
     */
    public record StatTableau(String nom, LocalDateTime debutPrevu, double retardDebutMoyen,
                              LocalDateTime finSouhaitee, LocalDateTime finMoyenne, LocalDateTime finTard,
                              double probabiliteDebordement, double attenteMoyenne) {
    }

    /** Agrège les résultats de tirages d'une même stratégie. */
    static StatistiquesSimulation depuis(StrategieOrdonnancement strategie, int tables,
                                         List<ResultatSimulation> resultats) {
        List<Long> fins = new ArrayList<>();
        double attente = 0;
        double plusLongue = 0;
        double occupation = 0;
        double annexes = 0;
        Moyenne unTableau = new Moyenne();
        Moyenne plusieurs = new Moyenne();
        int debordements = 0;
        int incomplets = 0;
        int debuts = 0;
        int aLHeure = 0;
        int leger = 0;
        int gros = 0;
        Map<String, List<ResultatSimulation.BilanTableau>> parTableau = new LinkedHashMap<>();
        for (ResultatSimulation r : resultats) {
            if (r.fin() != null) {
                fins.add(secondes(r.fin()));
            }
            attente += r.attenteMoyenne();
            plusLongue += r.attenteMaximale();
            occupation += r.occupationTables();
            annexes += r.tempsAnnexesMoyen();
            unTableau.ajouter(r.attenteUnTableau());
            plusieurs.ajouter(r.attentePlusieursTableaux());
            if (!r.tableauxQuiDebordent().isEmpty()) {
                debordements++;
            }
            if (!r.complete()) {
                incomplets++;
            }
            for (ResultatSimulation.BilanTableau b : r.tableaux()) {
                parTableau.computeIfAbsent(b.nom(), k -> new ArrayList<>()).add(b);
                debuts++;
                Long retard = b.retardDebut();
                if (retard != null && retard <= RETARD_A_L_HEURE) {
                    aLHeure++;
                } else if (retard != null && retard <= GROS_RETARD) {
                    leger++;
                } else {
                    gros++;
                }
            }
        }
        int n = Math.max(1, resultats.size());
        int d = Math.max(1, debuts);
        long[] f = fins.stream().mapToLong(Long::longValue).sorted().toArray();
        List<StatTableau> tableaux = new ArrayList<>();
        for (Map.Entry<String, List<ResultatSimulation.BilanTableau>> e : parTableau.entrySet()) {
            List<ResultatSimulation.BilanTableau> bs = e.getValue();
            long[] ft = bs.stream().filter(b -> b.fin() != null).mapToLong(b -> secondes(b.fin())).sorted().toArray();
            long deb = bs.stream().filter(ResultatSimulation.BilanTableau::deborde).count();
            double att = bs.stream().mapToDouble(ResultatSimulation.BilanTableau::attenteMoyenne).average().orElse(0);
            double retard = bs.stream().filter(b -> b.retardDebut() != null)
                    .mapToLong(ResultatSimulation.BilanTableau::retardDebut).average().orElse(-1);
            tableaux.add(new StatTableau(e.getKey(), bs.get(0).debut(), retard, bs.get(0).finSouhaitee(),
                    heure(moyenne(ft)), heure(quantile(ft, 0.95)), (double) deb / bs.size(), att));
        }
        return new StatistiquesSimulation(strategie, resultats.size(), tables, heure(moyenne(f)),
                heure(quantile(f, 0.05)), heure(quantile(f, 0.5)), heure(quantile(f, 0.95)),
                f.length == 0 ? null : heure((double) f[f.length - 1]), (double) aLHeure / d, (double) leger / d,
                (double) gros / d, attente / n, unTableau.valeur(), plusieurs.valeur(), plusLongue / n,
                occupation / n, (double) debordements / n, annexes / n, incomplets, tableaux);
    }

    /** Moyenne des valeurs connues (les -1 « aucun joueur » sont ignorés) ; -1 si aucune. */
    private static final class Moyenne {
        private double somme;
        private int nombre;

        void ajouter(double v) {
            if (v >= 0) {
                somme += v;
                nombre++;
            }
        }

        double valeur() {
            return nombre == 0 ? -1 : somme / nombre;
        }
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
