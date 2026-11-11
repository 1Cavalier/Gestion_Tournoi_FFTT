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
        assertTrue(s.finTot().isBefore(s.finTard()), "dispersion des heures de fin");
        assertFalse(s.finMediane().isBefore(s.finTot()));
        assertFalse(s.finTard().isAfter(s.finMax()));
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

    @Test
    void debut_des_tableaux_a_l_heure_avec_assez_de_tables_en_retard_sinon() {
        // Un tableau seul (aucun joueur occupé ailleurs) sur 40 tables : toujours à l'heure.
        ScenarioSimulation seul = ScenarioSimulation.builder().nombreDeTables(40).graine(4)
                .tableau(JeuDeSimulation.petitTournoi(40, 4).getTableaux().get(2)).build();
        StatistiquesSimulation large = simulateur.simulerPlusieurs(seul, StrategieOrdonnancement.PAR_DEFAUT, 20);
        assertEquals(1.0, large.debutALHeure(), 1e-9, "40 tables : toutes les poules commencent à l'heure");
        // Avec des joueurs encore occupés dans le tableau précédent, le suivant peut attendre.
        assertTrue(simulateur.simulerPlusieurs(JeuDeSimulation.petitTournoi(40, 4), StrategieOrdonnancement.PAR_DEFAUT,
                20).debutALHeure() <= 1.0);
        StatistiquesSimulation serre = simulateur.simulerPlusieurs(JeuDeSimulation.petitTournoi(2, 4),
                StrategieOrdonnancement.PAR_DEFAUT, 20);
        assertTrue(serre.debutGrosRetard() > 0, "2 tables : des tableaux commencent avec plus de 30 min de retard");
        assertEquals(1.0, serre.debutALHeure() + serre.debutLegerRetard() + serre.debutGrosRetard(), 1e-9);
        for (StatistiquesSimulation.StatTableau t : serre.tableaux()) {
            assertTrue(t.retardDebutMoyen() >= 0);
            assertNotNull(t.debutPrevu());
        }
    }

    @Test
    void attente_separee_des_joueurs_sur_un_tableau_et_sur_plusieurs() {
        // Six joueurs du tableau A jouent aussi le B.
        StatistiquesSimulation s = simulateur.simulerPlusieurs(JeuDeSimulation.petitTournoi(4, 4),
                StrategieOrdonnancement.PAR_DEFAUT, 20);
        assertTrue(s.attenteUnTableau() >= 0);
        assertTrue(s.attentePlusieursTableaux() >= 0);
        assertTrue(s.plusLongueAttente() >= s.attenteMoyenne());
        ScenarioSimulation seul = ScenarioSimulation.builder().nombreDeTables(4).graine(1)
                .tableau(JeuDeSimulation.petitTournoi(4, 4).getTableaux().get(2)).build();
        assertEquals(-1, simulateur.simulerPlusieurs(seul, StrategieOrdonnancement.PAR_DEFAUT, 5)
                .attentePlusieursTableaux(), "aucun joueur sur deux tableaux");
    }

    @Test
    void les_temps_autour_du_match_rallongent_la_journee() {
        ScenarioSimulation sans = ScenarioSimulation.builder().nombreDeTables(6).graine(3)
                .tempsAnnexes(com.pingmanager.ordonnancement.TempsAnnexesMatch.AUCUN)
                .tableau(JeuDeSimulation.petitTournoi(6, 3).getTableaux().get(0)).build();
        ScenarioSimulation avec = ScenarioSimulation.builder().nombreDeTables(6).graine(3)
                .tableau(JeuDeSimulation.petitTournoi(6, 3).getTableaux().get(0)).build();
        StatistiquesSimulation a = simulateur.simulerPlusieurs(sans, StrategieOrdonnancement.PAR_DEFAUT, 20);
        StatistiquesSimulation b = simulateur.simulerPlusieurs(avec, StrategieOrdonnancement.PAR_DEFAUT, 20);
        assertEquals(0, a.tempsAnnexesMoyen(), 1e-9);
        assertTrue(b.tempsAnnexesMoyen() > 5, "appel, échauffement, pauses, temps morts, feuille : plus de 5 min");
        assertTrue(b.finMoyenne().isAfter(a.finMoyenne().plusMinutes(20)),
                "mis bout à bout, ces minutes repoussent nettement la fin");
    }

    @Test
    void finales_etalees_avec_tableaux_commences_tot_groupees_en_fin_de_journee_avec_petit_a_petit() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(12, 6);
        StatistiquesSimulation tot = simulateur.simulerPlusieurs(sc, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, 20);
        StatistiquesSimulation vagues = simulateur.simulerPlusieurs(sc, StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 20);
        long etalementTot = java.time.Duration.between(tot.premiereFinale(), tot.finMoyenne()).toMinutes();
        long etalementVagues = java.time.Duration.between(vagues.premiereFinale(), vagues.finMoyenne()).toMinutes();
        assertTrue(tot.premiereFinale().isBefore(vagues.premiereFinale().minusMinutes(60)),
                "le tableau de 9 h joue sa finale bien plus tôt");
        assertTrue(etalementTot > 2 * etalementVagues, "finales étalées contre finales groupées : "
                + etalementTot + " / " + etalementVagues + " min");
        assertTrue(tot.finMoyenneTableau().isBefore(vagues.finMoyenneTableau()));
    }

    @Test
    void chaque_journee_simulee_separement_avec_son_heure_de_fin_visee() {
        ScenarioSimulation.TableauSimule samedi = JeuDeSimulation.petitTournoi(6, 1).getTableaux().get(0);
        ScenarioSimulation.TableauSimule b = JeuDeSimulation.petitTournoi(6, 1).getTableaux().get(1);
        ScenarioSimulation.TableauSimule dimanche = new ScenarioSimulation.TableauSimule(b.nom(),
                b.debut().plusDays(1), b.joueurs(), b.format(), false, null, 0);
        ScenarioSimulation deuxJours = ScenarioSimulation.builder().nombreDeTables(6).graine(1)
                .tableau(samedi).tableau(dimanche).build();
        var jours = deuxJours.parJour();
        assertEquals(List.of(samedi.debut().toLocalDate(), dimanche.debut().toLocalDate()),
                List.copyOf(jours.keySet()));
        assertEquals(List.of("A"), jours.get(samedi.debut().toLocalDate()).getTableaux().stream()
                .map(ScenarioSimulation.TableauSimule::nom).toList());
        ScenarioSimulation vise = jours.get(dimanche.debut().toLocalDate()).avecFinVisee(java.time.LocalTime.of(18, 30));
        assertEquals(dimanche.debut().toLocalDate().atTime(18, 30), vise.getTableaux().get(0).finSouhaitee());
        var stats = simulateur.comparerToutesParJour(deuxJours, 5);
        assertEquals(2, stats.size());
        for (List<StatistiquesSimulation> s : stats.values()) {
            assertEquals(1, s.get(0).tableaux().size(), "une journée ne contient que ses tableaux");
        }
        // Heure visée plus tôt : risque de débordement plus grand.
        ScenarioSimulation j = jours.get(samedi.debut().toLocalDate());
        double tot = simulateur.simulerPlusieurs(j.avecFinVisee(java.time.LocalTime.of(11, 0)),
                StrategieOrdonnancement.PAR_DEFAUT, 10).probabiliteDebordement();
        double tard = simulateur.simulerPlusieurs(j.avecFinVisee(java.time.LocalTime.of(23, 0)),
                StrategieOrdonnancement.PAR_DEFAUT, 10).probabiliteDebordement();
        assertTrue(tot > tard, tot + " > " + tard);
    }
}
