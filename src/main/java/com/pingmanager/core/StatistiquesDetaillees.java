package com.pingmanager.core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Statistiques détaillées d'une stratégie sur tous les tirages (B050), en plus
 * des indicateurs de {@link StatistiquesSimulation} : de quoi comparer les
 * stratégies de façon crédible. Elles se lisent <b>par journée</b> (la prévision
 * simule chaque journée séparément). Les durées sont en minutes.
 *
 * <p>Comment chaque chiffre est calculé :</p>
 * <ul>
 *   <li><b>fin au plus tôt / au plus tard</b> : la fin la plus précoce et la plus
 *       tardive observées sur l'ensemble des tirages ; <b>écart-type</b> : la
 *       dispersion des heures de fin autour de leur moyenne (l'intervalle
 *       5 %–95 % est donné par {@code finTot} et {@code finTard} des statistiques) ;</li>
 *   <li><b>fin visée</b> : l'instant (jour et heure, éventuellement le lendemain)
 *       à ne pas dépasser — la plus tardive des fins souhaitées des tableaux ;
 *       <b>probabilité de dépassement</b> : part des tirages où la dernière
 *       partie finit après cet instant (ou qui n'ont pas pu finir) ;
 *       <b>dépassement moyen</b> : minutes au-delà de la fin visée, en moyenne sur
 *       tous les tirages (0 pour un tirage à l'heure) ;</li>
 *   <li><b>retard de début</b> : minutes entre l'heure prévue d'un tableau et le
 *       lancement de sa dernière poule, en moyenne et au pire sur tous les
 *       tableaux de tous les tirages (la part des débuts à l'heure est dans
 *       {@code debutALHeure} des statistiques) ;</li>
 *   <li><b>attente</b> : médiane des attentes par partie (moyenne des médianes
 *       de chaque tirage) et plus longue attente observée, tous tirages
 *       confondus ;</li>
 *   <li><b>plus longue pause</b> : plus long temps réel entre deux parties d'un
 *       même joueur dans la journée, en moyenne sur les tirages, et la pire
 *       observée ;</li>
 *   <li><b>pic de tables</b> : plus grand nombre de tables occupées en même
 *       temps, en moyenne sur les tirages, et le plus haut observé ;</li>
 *   <li><b>matchs par heure</b> : nombre moyen de matchs lancés dans chaque
 *       heure pleine (un tirage sans lancement dans l'heure compte pour 0) ;</li>
 *   <li><b>finales</b> : heure moyenne du lancement de la première et de la
 *       dernière finale de la journée ;</li>
 *   <li><b>durée de la journée</b> : du début du premier tableau à la fin de la
 *       dernière partie, en moyenne ;</li>
 *   <li><b>tableaux à risque</b> : classés du plus risqué au moins risqué.</li>
 * </ul>
 *
 * @param finAuPlusTot           fin la plus précoce observée ({@code null} si rien n'a été joué)
 * @param finAuPlusTard          fin la plus tardive observée
 * @param ecartTypeFin           écart-type de l'heure de fin, en minutes
 * @param finVisee               instant à ne pas dépasser ({@code null} si aucun tableau)
 * @param probabiliteDepassement part des tirages qui finissent après la fin visée (0 à 1)
 * @param depassementMoyen       minutes au-delà de la fin visée, en moyenne sur tous les tirages
 * @param depassementMax         plus grand dépassement observé, en minutes
 * @param retardDebutMoyen       retard moyen du début d'un tableau, en minutes
 * @param retardDebutMax         plus grand retard de début observé, en minutes
 * @param attenteMediane         médiane des attentes par partie, en minutes
 * @param attenteMaximale        plus longue attente observée sur tous les tirages, en minutes
 * @param plusLonguePause        plus longue pause d'un joueur, en moyenne sur les tirages
 * @param plusLonguePauseMax     plus longue pause observée sur tous les tirages
 * @param picTablesMoyen         pic de tables occupées en même temps, en moyenne sur les tirages
 * @param picTablesMax           pic le plus haut observé
 * @param matchsParHeure         courbe horaire : matchs lancés par heure, dans l'ordre chronologique
 * @param debutPremiereFinale    heure moyenne du lancement de la première finale
 * @param debutDerniereFinale    heure moyenne du lancement de la dernière finale
 * @param dureeJourneeMoyenne    durée moyenne de la journée, en minutes (-1 si rien n'a été joué)
 * @param tableauxARisque        tableaux classés du plus risqué au moins risqué
 */
public record StatistiquesDetaillees(LocalDateTime finAuPlusTot, LocalDateTime finAuPlusTard, double ecartTypeFin,
                                     LocalDateTime finVisee, double probabiliteDepassement,
                                     double depassementMoyen, long depassementMax, double retardDebutMoyen,
                                     long retardDebutMax, double attenteMediane, long attenteMaximale,
                                     double plusLonguePause, long plusLonguePauseMax, double picTablesMoyen,
                                     int picTablesMax, List<PointHoraire> matchsParHeure,
                                     LocalDateTime debutPremiereFinale, LocalDateTime debutDerniereFinale,
                                     double dureeJourneeMoyenne, List<RisqueTableau> tableauxARisque) {

    /** Statistiques détaillées inconnues (statistiques construites sans elles). */
    public static final StatistiquesDetaillees VIDE = new StatistiquesDetaillees(null, null, 0, null, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, List.of(), null, null, -1, List.of());

    public StatistiquesDetaillees {
        matchsParHeure = List.copyOf(matchsParHeure);
        tableauxARisque = List.copyOf(tableauxARisque);
    }

    /**
     * Un point de la courbe horaire.
     *
     * @param heure  début de l'heure pleine (10:00 pour les lancements de 10:00 à 10:59)
     * @param matchs matchs lancés dans l'heure, en moyenne sur les tirages
     */
    public record PointHoraire(LocalDateTime heure, double matchs) {
    }

    /**
     * Risque d'un tableau. Classement : probabilité de finir après son heure
     * souhaitée (décroissante), puis dépassement moyen, puis retard moyen de
     * début, puis ordre du scénario.
     *
     * @param nom                    nom du tableau
     * @param finSouhaitee           heure à ne pas dépasser
     * @param probabiliteDebordement part des tirages où il finit après (ou ne finit pas), 0 à 1
     * @param depassementMoyen       minutes au-delà de l'heure souhaitée, en moyenne sur tous les tirages
     * @param retardDebutMoyen       retard moyen de son début, en minutes (-1 s'il n'a jamais commencé)
     */
    public record RisqueTableau(String nom, LocalDateTime finSouhaitee, double probabiliteDebordement,
                                double depassementMoyen, double retardDebutMoyen) {
    }

    /** @return le pic de la courbe horaire (l'heure la plus chargée), ou {@code null} si elle est vide. */
    public PointHoraire heureLaPlusChargee() {
        PointHoraire max = null;
        for (PointHoraire p : matchsParHeure) {
            if (max == null || p.matchs() > max.matchs()) {
                max = p;
            }
        }
        return max;
    }

    /** Agrège les mesures des tirages d'une même stratégie. */
    static StatistiquesDetaillees depuis(List<ResultatSimulation> resultats) {
        if (resultats.isEmpty()) {
            return VIDE;
        }
        int n = resultats.size();
        LocalDateTime visee = null;
        for (ResultatSimulation r : resultats) {
            for (ResultatSimulation.BilanTableau b : r.tableaux()) {
                if (b.finSouhaitee() != null && (visee == null || b.finSouhaitee().isAfter(visee))) {
                    visee = b.finSouhaitee();
                }
            }
        }
        List<Long> fins = new ArrayList<>();
        List<Long> premieres = new ArrayList<>();
        List<Long> dernieres = new ArrayList<>();
        int depassements = 0;
        long depassementTotal = 0;
        long depassementMax = 0;
        long retardTotal = 0;
        long retardMax = 0;
        int retards = 0;
        double medianes = 0;
        long attenteMax = 0;
        long pauses = 0;
        long pauseMax = 0;
        long pics = 0;
        int picMax = 0;
        long durees = 0;
        int journees = 0;
        SortedMap<LocalDateTime, Integer> parHeure = new TreeMap<>();
        Map<String, List<ResultatSimulation.BilanTableau>> parTableau = new LinkedHashMap<>();
        for (ResultatSimulation r : resultats) {
            MesuresTirage m = r.mesures();
            if (r.fin() != null) {
                fins.add(secondes(r.fin()));
            }
            if (visee != null && !r.tableaux().isEmpty()) {
                if (r.fin() == null || !r.complete()) {
                    depassements++;
                } else if (r.fin().isAfter(visee)) {
                    depassements++;
                    long d = Duration.between(visee, r.fin()).toMinutes();
                    depassementTotal += d;
                    depassementMax = Math.max(depassementMax, d);
                }
            }
            for (ResultatSimulation.BilanTableau b : r.tableaux()) {
                parTableau.computeIfAbsent(b.nom(), k -> new ArrayList<>()).add(b);
                if (b.retardDebut() != null) {
                    retardTotal += b.retardDebut();
                    retardMax = Math.max(retardMax, b.retardDebut());
                    retards++;
                }
            }
            medianes += m.attenteMediane();
            attenteMax = Math.max(attenteMax, r.attenteMaximale());
            pauses += m.plusLonguePause();
            pauseMax = Math.max(pauseMax, m.plusLonguePause());
            pics += m.picTables();
            picMax = Math.max(picMax, m.picTables());
            m.matchsParHeure().forEach((h, v) -> parHeure.merge(h, v, Integer::sum));
            if (m.debutPremiereFinale() != null) {
                premieres.add(secondes(m.debutPremiereFinale()));
            }
            if (m.debutDerniereFinale() != null) {
                dernieres.add(secondes(m.debutDerniereFinale()));
            }
            if (m.dureeJournee() >= 0) {
                durees += m.dureeJournee();
                journees++;
            }
        }
        List<PointHoraire> courbe = new ArrayList<>();
        parHeure.forEach((h, v) -> courbe.add(new PointHoraire(h, (double) v / n)));
        long[] f = fins.stream().mapToLong(Long::longValue).sorted().toArray();
        return new StatistiquesDetaillees(f.length == 0 ? null : heure(f[0]),
                f.length == 0 ? null : heure(f[f.length - 1]), ecartTypeMinutes(f), visee,
                (double) depassements / n, (double) depassementTotal / n, depassementMax,
                retards == 0 ? 0 : (double) retardTotal / retards, retardMax, medianes / n, attenteMax,
                (double) pauses / n, pauseMax, (double) pics / n, picMax, courbe, moyenne(premieres),
                moyenne(dernieres), journees == 0 ? -1 : (double) durees / journees, risques(parTableau));
    }

    private static List<RisqueTableau> risques(Map<String, List<ResultatSimulation.BilanTableau>> parTableau) {
        List<RisqueTableau> risques = new ArrayList<>();
        for (Map.Entry<String, List<ResultatSimulation.BilanTableau>> e : parTableau.entrySet()) {
            List<ResultatSimulation.BilanTableau> bs = e.getValue();
            double proba = (double) bs.stream().filter(ResultatSimulation.BilanTableau::deborde).count() / bs.size();
            double depassement = bs.stream().mapToLong(ResultatSimulation.BilanTableau::minutesDepassement)
                    .average().orElse(0);
            double retard = bs.stream().filter(b -> b.retardDebut() != null)
                    .mapToLong(ResultatSimulation.BilanTableau::retardDebut).average().orElse(-1);
            risques.add(new RisqueTableau(e.getKey(), bs.get(0).finSouhaitee(), proba, depassement, retard));
        }
        // Tri stable : à égalité, l'ordre du scénario est conservé.
        risques.sort(Comparator.comparingDouble(RisqueTableau::probabiliteDebordement).reversed()
                .thenComparing(Comparator.comparingDouble(RisqueTableau::depassementMoyen).reversed())
                .thenComparing(Comparator.comparingDouble(RisqueTableau::retardDebutMoyen).reversed()));
        return risques;
    }

    private static double ecartTypeMinutes(long[] secondes) {
        if (secondes.length < 2) {
            return 0;
        }
        double moyenne = 0;
        for (long s : secondes) {
            moyenne += s;
        }
        moyenne /= secondes.length;
        double somme = 0;
        for (long s : secondes) {
            somme += (s - moyenne) * (s - moyenne);
        }
        return Math.sqrt(somme / secondes.length) / 60.0;
    }

    private static LocalDateTime moyenne(List<Long> secondes) {
        return secondes.isEmpty() ? null
                : heure(Math.round(secondes.stream().mapToLong(Long::longValue).average().orElse(0)));
    }

    private static long secondes(LocalDateTime t) {
        return t.toEpochSecond(ZoneOffset.UTC);
    }

    private static LocalDateTime heure(long secondes) {
        return LocalDateTime.ofEpochSecond(secondes, 0, ZoneOffset.UTC);
    }
}
