package com.pingmanager.core;

import com.pingmanager.model.StrategieOrdonnancement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Prévision d'un tournoi (B050) : pour chaque journée, simulée seule, les
 * statistiques des trois stratégies sur les mêmes tirages, la fin visée
 * retenue et les effectifs simulés par tableau.
 *
 * @param tirages  nombre de fois où chaque journée a été rejouée, par stratégie
 * @param journees prévision de chaque journée, dans l'ordre chronologique
 */
public record PrevisionTournoi(int tirages, SortedMap<LocalDate, PrevisionJournee> journees) {

    public PrevisionTournoi {
        journees = Collections.unmodifiableSortedMap(new TreeMap<>(journees));
    }

    /**
     * Prévision d'une journée.
     *
     * @param jour           jour de début des matchs
     * @param finVisee       instant à ne pas dépasser (jour et heure, éventuellement le lendemain)
     * @param nombreDeTables tables disponibles
     * @param effectifs      joueurs simulés par tableau (inscrits, capacité, taux appliqué)
     * @param strategies     statistiques de chaque stratégie, dans l'ordre de l'énumération ; avec
     *                       renfort, elles en tiennent compte
     * @param tablesRenfort  tables d'échauffement utilisables en renfort (B054 ; 0 : aucune)
     * @param sansRenfort    avec renfort, les mêmes tirages rejoués sans lui (pour comparer la fin) ;
     *                       vide sans renfort
     */
    public record PrevisionJournee(LocalDate jour, LocalDateTime finVisee, int nombreDeTables,
                                   List<EffectifTableau> effectifs, List<StatistiquesSimulation> strategies,
                                   int tablesRenfort, List<StatistiquesSimulation> sansRenfort) {
        public PrevisionJournee {
            Objects.requireNonNull(jour, "jour");
            effectifs = List.copyOf(effectifs);
            strategies = List.copyOf(strategies);
            sansRenfort = sansRenfort == null ? List.of() : List.copyOf(sansRenfort);
            tablesRenfort = Math.max(0, tablesRenfort);
        }

        /** Sans tables de renfort. */
        public PrevisionJournee(LocalDate jour, LocalDateTime finVisee, int nombreDeTables,
                                List<EffectifTableau> effectifs, List<StatistiquesSimulation> strategies) {
            this(jour, finVisee, nombreDeTables, effectifs, strategies, 0, List.of());
        }

        /** @return les statistiques d'une stratégie (avec le renfort s'il y en a). */
        public Optional<StatistiquesSimulation> pour(StrategieOrdonnancement strategie) {
            return strategies.stream().filter(s -> s.strategie() == strategie).findFirst();
        }

        /** @return les statistiques d'une stratégie sans le renfort (vide si la journée n'a pas de renfort). */
        public Optional<StatistiquesSimulation> sansRenfortPour(StrategieOrdonnancement strategie) {
            return sansRenfort.stream().filter(s -> s.strategie() == strategie).findFirst();
        }

        /** @return vrai si des tables de renfort étaient permises pour la journée. */
        public boolean avecRenfort() {
            return tablesRenfort > 0;
        }
    }

    /**
     * Effectif simulé d'un tableau.
     *
     * @param nom      nom du tableau
     * @param debut    heure de début des matchs retenue
     * @param inscrits inscrits réels
     * @param capacite places maximales (0 : sans limite)
     * @param taux     taux de remplissage appliqué, de 0 à 1 (-1 : aucun, inscrits réels)
     * @param simules  joueurs joués dans la simulation
     */
    public record EffectifTableau(String nom, LocalDateTime debut, int inscrits, int capacite, double taux,
                                  int simules) {

        /** @return le nombre de joueurs fictifs ajoutés (0 si le tableau n'est pas complété). */
        public int fictifs() {
            return Math.max(0, simules - inscrits);
        }

        /** @return le nombre d'inscrits écartés de la simulation (0 si le tableau n'est pas réduit). */
        public int ecartes() {
            return Math.max(0, inscrits - simules);
        }
    }
}
