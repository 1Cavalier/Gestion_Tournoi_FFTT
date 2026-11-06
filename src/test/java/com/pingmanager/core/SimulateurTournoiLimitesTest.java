package com.pingmanager.core;

import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatPoule3Qualifie2;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cas limites de la simulation de stratégies (B025). */
class SimulateurTournoiLimitesTest {

    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    @Test
    void zero_table_refuse_par_le_scenario() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioSimulation.builder().nombreDeTables(0));
        assertThrows(IllegalArgumentException.class, () -> JeuDeSimulation.petitTournoi(4, 1).avecTables(0));
    }

    @Test
    void variabilite_hors_bornes_refusee() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioSimulation.builder().variabilite(1.0));
        assertThrows(IllegalArgumentException.class, () -> ScenarioSimulation.builder().variabilite(-0.1));
    }

    @Test
    void valeurs_nulles_refusees() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(4, 1);
        assertThrows(NullPointerException.class, () -> simulateur.simuler(null, StrategieOrdonnancement.PAR_DEFAUT));
        assertThrows(NullPointerException.class, () -> simulateur.simuler(sc, null));
        assertThrows(NullPointerException.class, () -> PoidsOrdonnancement.pour(null));
        assertThrows(NullPointerException.class, () -> SimulateurTournoi.scenarioDepuis(null, 4, 1, false));
    }

    @Test
    void tournoi_sans_tableau_donne_un_scenario_vide_et_zero_table_devient_une_table() {
        Tournoi t = new Tournoi("Vide", "Club", JeuDeSimulation.JOUR);
        ScenarioSimulation sc = SimulateurTournoi.scenarioDepuis(t, 0, 5, true);
        assertTrue(sc.getTableaux().isEmpty());
        assertEquals(1, sc.getNombreDeTables());
        ResultatSimulation r = simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals(0, r.nombreParties());
        assertTrue(r.tableaux().isEmpty());
    }

    @Test
    void tableau_sans_joueur_est_ignore_et_un_seul_joueur_aussi() {
        ScenarioSimulation sc = ScenarioSimulation.builder().nombreDeTables(2)
                .tableau(JeuDeSimulation.tableau("Zero", LocalTime.of(9, 0), List.of()))
                .tableau(JeuDeSimulation.tableau("Un", LocalTime.of(9, 0), List.of(JeuDeSimulation.joueur("X1", 900))))
                .build();
        ResultatSimulation r = simulateur.simuler(sc, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        assertEquals(2, r.ignores().size());
        assertTrue(r.tableaux().isEmpty());
        assertEquals(0, r.nombreParties());
        assertTrue(r.complete());
    }

    @Test
    void effectifs_non_ronds_tous_les_tableaux_se_terminent_pour_chaque_strategie() {
        for (int n : new int[] {3, 4, 5, 7, 11, 13}) {
            ScenarioSimulation sc = ScenarioSimulation.builder().nombreDeTables(1).graine(3)
                    .tableau(JeuDeSimulation.tableau("T" + n, LocalTime.of(9, 0),
                            JeuDeSimulation.joueurs("N" + n + "_", n, 600, 1400, n)))
                    .build();
            for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
                ResultatSimulation r = simulateur.simuler(sc, s);
                assertTrue(r.complete(), n + " joueurs, " + s);
                assertEquals(1, r.tableaux().size());
                assertNotNull(r.tableaux().get(0).fin(), n + " joueurs, " + s);
                assertTrue(r.nombreParties() >= n - 1, "au moins n-1 parties : " + n);
                assertTrue(r.attenteMaximale() >= 0);
                assertTrue(r.occupationTables() >= 0 && r.occupationTables() <= 1.0 + 1e-9, "occupation " + r.occupationTables());
            }
        }
    }

    @Test
    void petite_finale_ajoute_une_partie() {
        var joueurs = JeuDeSimulation.joueurs("P", 16, 600, 1400, 9);
        var sans = new ScenarioSimulation.TableauSimule("S", JeuDeSimulation.JOUR.atTime(9, 0), joueurs,
                FormatPoule3Qualifie2.INSTANCE, false, null, 0);
        var avec = new ScenarioSimulation.TableauSimule("S", JeuDeSimulation.JOUR.atTime(9, 0), joueurs,
                FormatPoule3Qualifie2.INSTANCE, true, null, 0);
        int a = simulateur.simuler(ScenarioSimulation.builder().nombreDeTables(4).tableau(sans).build(),
                StrategieOrdonnancement.PAR_DEFAUT).nombreParties();
        int b = simulateur.simuler(ScenarioSimulation.builder().nombreDeTables(4).tableau(avec).build(),
                StrategieOrdonnancement.PAR_DEFAUT).nombreParties();
        assertEquals(a + 1, b);
    }

    @Test
    void le_nombre_de_parties_ne_depend_ni_de_la_strategie_ni_des_tables_et_la_graine_fixe_tout() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(3, 42);
        int ref = simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT).nombreParties();
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            assertEquals(ref, simulateur.simuler(sc, s).nombreParties(), s.name());
            assertEquals(simulateur.simuler(sc, s), simulateur.simuler(sc, s), "même graine, même résultat " + s);
        }
        assertEquals(ref, simulateur.simuler(sc.avecTables(1), StrategieOrdonnancement.PAR_DEFAUT).nombreParties());
    }

    @Test
    void un_joueur_inscrit_a_deux_tableaux_ne_joue_jamais_deux_parties_en_meme_temps_une_seule_table_suffit() {
        // Avec une seule table, la fin ne peut qu'être >= début + parties x durée minimale.
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(1, 7);
        ResultatSimulation r = simulateur.simuler(sc, StrategieOrdonnancement.MINIMISER_ATTENTE);
        assertTrue(r.complete());
        LocalDateTime debut = r.debut();
        assertNotNull(debut);
        assertTrue(r.fin().isAfter(debut));
        r.tableaux().forEach(b -> assertFalse(b.fin().isBefore(b.debut())));
    }

    @Test
    void comparaison_sur_scenario_vide_ne_plante_pas() {
        ComparaisonStrategies c = simulateur.comparer(ScenarioSimulation.builder().build(),
                StrategieOrdonnancement.MINIMISER_ATTENTE, StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT);
        assertEquals(0, c.minutesFinGagneesParLaSeconde());
        assertEquals(0.0, c.attenteMoyenneGagneeParLaSeconde(), 1e-9);
    }
}
