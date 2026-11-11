package com.pingmanager.core;

import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Simulation statistique (B025) : de nombreux tirages, des stratégies qui diffèrent vraiment. */
class SimulationMonteCarloTest {

    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    @Test
    void meme_graine_memes_statistiques_malgre_le_parallelisme() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(6, 11);
        StatistiquesSimulation a = simulateur.simulerPlusieurs(sc, StrategieOrdonnancement.PAR_DEFAUT, 30);
        StatistiquesSimulation b = simulateur.simulerPlusieurs(sc, StrategieOrdonnancement.PAR_DEFAUT, 30);
        assertEquals(a, b);
        assertEquals(30, a.tirages());
        assertEquals(0, a.tiragesIncomplets());
    }

    @Test
    void les_tirages_different_les_uns_des_autres() {
        StatistiquesSimulation s = simulateur.simulerPlusieurs(JeuDeSimulation.petitTournoi(6, 11),
                StrategieOrdonnancement.PAR_DEFAUT, 40);
        assertTrue(s.finP10().isBefore(s.finP90()), "dispersion des heures de fin");
        assertFalse(s.finMediane().isBefore(s.finP10()));
        assertFalse(s.finP90().isAfter(s.finMax()));
    }

    @Test
    void salle_saturee_les_trois_strategies_donnent_trois_resultats_differents() {
        List<StatistiquesSimulation> toutes = simulateur.comparerToutes(JeuDeSimulation.petitTournoi(4, 5), 30);
        assertEquals(StrategieOrdonnancement.values().length, toutes.size());
        Set<String> differents = new HashSet<>();
        for (StatistiquesSimulation s : toutes) {
            differents.add(s.finMoyenne() + "/" + Math.round(s.attenteMoyenne() * 10));
        }
        assertEquals(toutes.size(), differents.size(), "chaque stratégie a ses propres statistiques");
    }

    @Test
    void petit_a_petit_tour_par_tour_coute_du_temps_meme_avec_des_tables_libres() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(40, 5);
        StatistiquesSimulation vagues = simulateur.simulerPlusieurs(sc,
                StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 30);
        StatistiquesSimulation auPlusTot = simulateur.simulerPlusieurs(sc,
                StrategieOrdonnancement.MINIMISER_ATTENTE, 30);
        assertTrue(vagues.finMoyenne().isAfter(auPlusTot.finMoyenne()),
                "attendre la fin de chaque tour retarde la fin");
        assertTrue(vagues.attenteMoyenne() > auPlusTot.attenteMoyenne());
    }

    @Test
    void des_matchs_plus_longs_finissent_le_tournoi_plus_tard() {
        ScenarioSimulation court = ScenarioSimulation.builder().nombreDeTables(6).graine(2)
                .loiDuree(new LoiDureeMatch(Duration.ofMinutes(15), Duration.ofMinutes(8), Duration.ofMinutes(40)))
                .tableau(JeuDeSimulation.petitTournoi(6, 2).getTableaux().get(0)).build();
        ScenarioSimulation lent = ScenarioSimulation.builder().nombreDeTables(6).graine(2)
                .loiDuree(new LoiDureeMatch(Duration.ofMinutes(25), Duration.ofMinutes(12), Duration.ofMinutes(60)))
                .tableau(JeuDeSimulation.petitTournoi(6, 2).getTableaux().get(0)).build();
        assertTrue(simulateur.simulerPlusieurs(lent, StrategieOrdonnancement.PAR_DEFAUT, 20).finMoyenne()
                .isAfter(simulateur.simulerPlusieurs(court, StrategieOrdonnancement.PAR_DEFAUT, 20).finMoyenne()));
    }

    @Test
    void statistiques_par_tableau_et_probabilite_de_debordement() {
        StatistiquesSimulation s = simulateur.simulerPlusieurs(JeuDeSimulation.petitTournoi(2, 3),
                StrategieOrdonnancement.PAR_DEFAUT, 20);
        assertEquals(3, s.tableaux().size());
        for (StatistiquesSimulation.StatTableau t : s.tableaux()) {
            assertNotNull(t.finMoyenne());
            assertTrue(t.probabiliteDebordement() >= 0 && t.probabiliteDebordement() <= 1);
        }
        assertTrue(s.probabiliteDebordement() >= 0 && s.probabiliteDebordement() <= 1);
        assertThrows(IllegalArgumentException.class,
                () -> simulateur.simulerPlusieurs(JeuDeSimulation.petitTournoi(2, 3), StrategieOrdonnancement.PAR_DEFAUT, 0));
    }
}
