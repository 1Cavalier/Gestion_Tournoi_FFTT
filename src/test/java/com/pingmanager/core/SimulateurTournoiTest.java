package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatPoule3Qualifie2;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simulateur de tournoi (B025) : reproductibilité par la graine, déroulé complet
 * (poules puis tableau final), différences entre stratégies, débordements.
 */
class SimulateurTournoiTest {

    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    @Test
    void meme_graine_meme_resultat_au_detail_pres() {
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            ResultatSimulation premier = simulateur.simuler(JeuDeSimulation.petitTournoi(6, 42), s);
            ResultatSimulation second = simulateur.simuler(JeuDeSimulation.petitTournoi(6, 42), s);
            assertEquals(premier, second, "même graine, même stratégie : " + s);
        }
    }

    @Test
    void autre_graine_autre_deroule() {
        ResultatSimulation a = simulateur.simuler(JeuDeSimulation.petitTournoi(6, 42), StrategieOrdonnancement.PAR_DEFAUT);
        ResultatSimulation b = simulateur.simuler(JeuDeSimulation.petitTournoi(6, 43), StrategieOrdonnancement.PAR_DEFAUT);
        assertNotEquals(a.fin(), b.fin());
        assertEquals(43, b.graine());
    }

    @Test
    void tournoi_joue_en_entier_poules_puis_tableau_final() {
        ResultatSimulation r = simulateur.simuler(JeuDeSimulation.petitTournoi(6, 7), StrategieOrdonnancement.PAR_DEFAUT);
        assertTrue(r.complete());
        // A : 8 poules de 3 (24 parties) + 16 qualifiés (15 parties) ; B : idem ;
        // C : 7 poules (21 parties) + 14 qualifiés dans un tableau de 16 (13 parties).
        assertEquals(39, r.tableau("A").orElseThrow().nombreParties());
        assertEquals(39, r.tableau("B").orElseThrow().nombreParties());
        assertEquals(34, r.tableau("C").orElseThrow().nombreParties());
        assertEquals(112, r.nombreParties());
        for (ResultatSimulation.BilanTableau b : r.tableaux()) {
            assertNotNull(b.fin());
            assertTrue(b.fin().isAfter(b.debut()), b.nom());
        }
        assertEquals(r.tableau("C").orElseThrow().fin(), r.fin(), "le tournoi finit avec son dernier tableau");
        assertTrue(r.occupationTables() > 0 && r.occupationTables() <= 1.0);
        assertTrue(r.attenteMoyenne() > 0);
        assertTrue(r.attenteMaximale() >= r.attenteMoyenne());
        assertNotNull(r.joueurAttenteMax());
    }

    @Test
    void comparaison_memes_parties_seul_l_ordre_de_lancement_change() {
        ComparaisonStrategies c = simulateur.comparer(JeuDeSimulation.petitTournoi(4, 7),
                StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        assertEquals(c.premiere().nombreParties(), c.seconde().nombreParties());
        assertEquals(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, c.premiere().strategie());
        assertEquals(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, c.seconde().strategie());
        assertNotEquals(c.premiere().tableaux(), c.seconde().tableaux(), "les stratégies donnent des déroulés différents");
        assertEquals(java.time.Duration.between(c.seconde().fin(), c.premiere().fin()).toMinutes(),
                c.minutesFinGagneesParLaSeconde());
        assertEquals(c.premiere().attenteMoyenne() - c.seconde().attenteMoyenne(),
                c.attenteMoyenneGagneeParLaSeconde(), 1e-9);
    }

    @Test
    void avancer_les_tableaux_commences_tot_finit_le_premier_tableau_bien_plus_tot() {
        for (int tables : new int[]{4, 6}) {
            ScenarioSimulation sc = JeuDeSimulation.petitTournoi(tables, 7);
            LocalDateTime finAvancer = simulateur.simuler(sc, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES)
                    .tableau("A").orElseThrow().fin();
            for (StrategieOrdonnancement autre : List.of(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT,
                    StrategieOrdonnancement.MINIMISER_ATTENTE)) {
                LocalDateTime finAutre = simulateur.simuler(sc, autre).tableau("A").orElseThrow().fin();
                assertTrue(finAvancer.plusMinutes(60).isBefore(finAutre),
                        tables + " tables : A fini à " + finAvancer + " contre " + finAutre + " (" + autre + ")");
            }
        }
    }

    @Test
    void petit_a_petit_les_tableaux_finissent_ensemble() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(4, 7);
        long ecartPetit = ecartDesFins(simulateur.simuler(sc, StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT));
        long ecartAvancer = ecartDesFins(simulateur.simuler(sc, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES));
        assertTrue(ecartPetit * 3 < ecartAvancer, "écart des fins : " + ecartPetit + " min contre " + ecartAvancer);
    }

    private static long ecartDesFins(ResultatSimulation r) {
        LocalDateTime min = r.tableaux().stream().map(ResultatSimulation.BilanTableau::fin).min(LocalDateTime::compareTo).orElseThrow();
        LocalDateTime max = r.tableaux().stream().map(ResultatSimulation.BilanTableau::fin).max(LocalDateTime::compareTo).orElseThrow();
        return java.time.Duration.between(min, max).toMinutes();
    }

    @Test
    void plus_de_tables_tournoi_fini_plus_tot() {
        LocalDateTime fin4 = simulateur.simuler(JeuDeSimulation.petitTournoi(4, 7), StrategieOrdonnancement.PAR_DEFAUT).fin();
        LocalDateTime fin10 = simulateur.simuler(JeuDeSimulation.petitTournoi(4, 7).avecTables(10),
                StrategieOrdonnancement.PAR_DEFAUT).fin();
        assertTrue(fin10.isBefore(fin4));
    }

    @Test
    void tableau_qui_deborde_de_l_heure_limite_ou_de_sa_fin_theorique() {
        ScenarioSimulation tard = ScenarioSimulation.builder().nombreDeTables(4).graine(7)
                .heureLimite(LocalTime.of(12, 0))
                .tableau(JeuDeSimulation.tableau("A", LocalTime.of(9, 0), JeuDeSimulation.joueurs("A", 24, 500, 1200, 1)))
                .tableau(new ScenarioSimulation.TableauSimule("Z", JeuDeSimulation.JOUR.atTime(9, 0),
                        JeuDeSimulation.joueurs("Z", 6, 500, 900, 5), FormatPoule3Qualifie2.INSTANCE, false,
                        JeuDeSimulation.JOUR.atTime(23, 0), 0))
                .build();
        ResultatSimulation r = simulateur.simuler(tard, StrategieOrdonnancement.PAR_DEFAUT);
        ResultatSimulation.BilanTableau a = r.tableau("A").orElseThrow();
        assertEquals(JeuDeSimulation.JOUR.atTime(12, 0), a.finSouhaitee(), "sans fin théorique : heure limite du jour");
        assertTrue(a.deborde());
        assertTrue(a.minutesDepassement() > 0);
        ResultatSimulation.BilanTableau z = r.tableau("Z").orElseThrow();
        assertFalse(z.deborde(), "fin théorique à 23 h respectée");
        assertEquals(0, z.minutesDepassement());
        assertEquals(List.of(a), r.tableauxQuiDebordent());
    }

    @Test
    void tableau_avec_trop_peu_de_joueurs_ignore_avec_motif() {
        ScenarioSimulation sc = ScenarioSimulation.builder().nombreDeTables(2)
                .tableau(JeuDeSimulation.tableau("A", LocalTime.of(9, 0), JeuDeSimulation.joueurs("A", 9, 500, 1200, 1)))
                .tableau(JeuDeSimulation.tableau("Vide", LocalTime.of(9, 0), JeuDeSimulation.joueurs("V", 2, 500, 900, 2)))
                .build();
        ResultatSimulation r = simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals(1, r.tableaux().size());
        assertEquals(1, r.ignores().size());
        assertEquals("Vide", r.ignores().get(0).nom());
        assertTrue(r.ignores().get(0).motif().contains("2 joueur(s)"));
        assertTrue(r.complete());
    }

    @Test
    void scenario_vide_rien_a_jouer() {
        ResultatSimulation r = simulateur.simuler(ScenarioSimulation.builder().build(), StrategieOrdonnancement.PAR_DEFAUT);
        assertNull(r.fin());
        assertEquals(0, r.nombreParties());
        assertEquals(0, r.occupationTables());
        assertTrue(r.complete());
    }

    @Test
    void scenario_depuis_un_tournoi_horaires_inscrits_et_tableaux_completes() {
        Tournoi t = new Tournoi("Test", "Club", JeuDeSimulation.JOUR);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(FormatPoule3Qualifie2.INSTANCE)
                .placesMax(12).pointsMax(1199).build());
        a.setHoraires(new HorairesTableau(null, null, JeuDeSimulation.JOUR.atTime(10, 0),
                JeuDeSimulation.JOUR.atTime(13, 0)));
        JeuDeSimulation.joueurs("A", 5, 500, 1100, 1).forEach(a::inscrire);
        t.ajouterTableau(a);
        Tableau b = new Tableau("B", ConfigurationTableau.depuisFormat(FormatPoule3Qualifie2.INSTANCE).build());
        t.ajouterTableau(b);

        ScenarioSimulation reel = SimulateurTournoi.scenarioDepuis(t, 8, 3, false);
        assertEquals(8, reel.getNombreDeTables());
        assertEquals(3, reel.getGraine());
        assertEquals(2, reel.getTableaux().size());
        ScenarioSimulation.TableauSimule sa = reel.getTableaux().get(0);
        assertEquals(JeuDeSimulation.JOUR.atTime(10, 0), sa.debut());
        assertEquals(JeuDeSimulation.JOUR.atTime(13, 0), sa.finSouhaitee());
        assertEquals(5, sa.joueurs().size());
        assertEquals(1199, sa.niveauPoints());
        assertEquals(JeuDeSimulation.JOUR.atTime(SimulateurTournoi.DEBUT_PAR_DEFAUT),
                reel.getTableaux().get(1).debut(), "sans horaire : 9 h le premier jour");

        ScenarioSimulation plein = SimulateurTournoi.scenarioDepuis(t, 8, 3, true);
        List<com.pingmanager.model.Joueur> joueurs = plein.getTableaux().get(0).joueurs();
        assertEquals(12, joueurs.size(), "complété jusqu'aux places maximales");
        assertTrue(joueurs.stream().allMatch(j -> j.getPoints() <= 1199));
        assertEquals(0, plein.getTableaux().get(1).joueurs().size(), "tableau sans limite : rien à compléter");
        assertEquals(plein.getTableaux().get(0).joueurs(), SimulateurTournoi.scenarioDepuis(t, 8, 3, true)
                .getTableaux().get(0).joueurs(), "joueurs fictifs reproductibles");

        ResultatSimulation r = new SimulateurTournoi().simuler(plein, StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals(1, r.tableaux().size());
        assertEquals("B", r.ignores().get(0).nom());
    }
}
