package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatPoule3Qualifie2;
import com.pingmanager.ordonnancement.ReglageRenfort;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simulation avec tables de renfort (B054) : la réserve de la salle
 * d'échauffement est ouverte quand le retard prévu dépasse le seuil, rendue
 * quand il est résorbé, et jamais au-delà du maximum saisi.
 */
class SimulationRenfortTest {

    private static final LocalDate JOUR = JeuDeSimulation.JOUR;
    private static final StrategieOrdonnancement S = StrategieOrdonnancement.PAR_DEFAUT;
    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    /** Un gros tableau à 9 h sur 4 tables : les poules ne peuvent pas toutes commencer à l'heure. */
    private static ScenarioSimulation matinCharge(ReglageRenfort renfort) {
        return ScenarioSimulation.builder().nombreDeTables(4).graine(7)
                .tableau(new ScenarioSimulation.TableauSimule("A", JOUR.atTime(9, 0),
                        JeuDeSimulation.joueurs("A", 48, 500, 1400, 1), FormatPoule3Qualifie2.INSTANCE, false,
                        JOUR.atTime(22, 0), 1399))
                .renfort(renfort).build();
    }

    @Test
    void sans_renfort_aucune_mesure_de_renfort() {
        ResultatSimulation r = simulateur.simuler(matinCharge(ReglageRenfort.AUCUN), S);
        assertSame(RenfortTirage.AUCUN, r.renfort());
        assertFalse(r.renfort().deploye());
    }

    @Test
    void sans_retard_renfort_jamais_ouvert_et_resultat_identique() {
        ScenarioSimulation large = matinCharge(ReglageRenfort.AUCUN).avecTables(40);
        ResultatSimulation sans = simulateur.simuler(large, S);
        ResultatSimulation avec = simulateur.simuler(large.avecRenfort(ReglageRenfort.tables(4)), S);
        assertFalse(avec.renfort().deploye());
        assertEquals(0, avec.renfort().utilisation());
        assertEquals(sans.fin(), avec.fin(), "renfort fermé : même déroulé");
    }

    @Test
    void retard_au_demarrage_renfort_ouvert_puis_rendu_une_fois_le_retard_resorbe() {
        ResultatSimulation r = simulateur.simuler(matinCharge(ReglageRenfort.tables(3)), S);
        RenfortTirage renfort = r.renfort();
        assertTrue(renfort.deploye());
        assertTrue(renfort.premiereMiseEnService().isAfter(JOUR.atTime(9, 0)), "ouvert quand le retard apparaît");
        assertTrue(renfort.premiereMiseEnService().isBefore(JOUR.atTime(10, 0)));
        assertFalse(renfort.plages().isEmpty());
        RenfortTirage.Plage premiere = renfort.plages().get(0);
        assertTrue(premiere.fin().isBefore(r.fin()), "les tables sont rendues avant la fin de la journée");
        assertTrue(renfort.utilisation() > 0 && renfort.utilisation() <= 1);
        assertTrue(renfort.minutesDeJeu() > 0);
    }

    @Test
    void jamais_plus_de_tables_de_renfort_que_le_maximum() {
        for (int max = 1; max <= 3; max++) {
            RenfortTirage renfort = simulateur.simuler(matinCharge(ReglageRenfort.tables(max)), S).renfort();
            assertTrue(renfort.tablesEnServiceMax() <= max);
            for (RenfortTirage.Plage p : renfort.plages()) {
                assertTrue(p.tablesMax() <= max);
            }
        }
    }

    @Test
    void le_renfort_avance_la_fin_d_une_journee_chargee() {
        // Fin visée intenable avec 4 tables : le renfort reste ouvert et fait gagner du temps.
        ScenarioSimulation serre = matinCharge(ReglageRenfort.AUCUN).avecFinVisee(JOUR.atTime(13, 0));
        ResultatSimulation sans = simulateur.simuler(serre, S);
        ResultatSimulation avec = simulateur.simuler(serre.avecRenfort(ReglageRenfort.tables(4)), S);
        assertTrue(avec.fin().isBefore(sans.fin()));
        assertEquals(4, avec.renfort().tablesEnServiceMax());
    }

    @Test
    void un_seuil_plus_haut_ouvre_plus_tard_ou_jamais() {
        ReglageRenfort reactif = ReglageRenfort.tables(3);
        ReglageRenfort patient = ReglageRenfort.builder().tablesMax(3).seuilDeploiementMinutes(240).build();
        RenfortTirage tot = simulateur.simuler(matinCharge(reactif), S).renfort();
        RenfortTirage tard = simulateur.simuler(matinCharge(patient), S).renfort();
        assertTrue(!tard.deploye() || !tard.premiereMiseEnService().isBefore(tot.premiereMiseEnService()));
        assertTrue(tard.minutesDeJeu() <= tot.minutesDeJeu());
    }

    // ------------------------------------------------------------------
    // Prévision : par journée, avec et sans renfort
    // ------------------------------------------------------------------

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Test", "Club", JOUR);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(FormatPoule3Qualifie2.INSTANCE)
                .placesMax(48).pointsMax(1399).build());
        a.setHoraires(new HorairesTableau(null, null, JOUR.atTime(9, 0), JOUR.atTime(14, 0)));
        JeuDeSimulation.joueurs("A", 42, 500, 1399, 3).forEach(a::inscrire);
        t.ajouterTableau(a);
        return t;
    }

    @Test
    void prevision_sans_renfort_inchangee() {
        PrevisionTournoi p = simulateur.prevoir(tournoi(), ReglagesPrevision.builder().nombreDeTables(4).build());
        PrevisionTournoi.PrevisionJournee j = p.journees().get(JOUR);
        assertFalse(j.avecRenfort());
        assertTrue(j.sansRenfort().isEmpty());
        assertFalse(j.pour(S).orElseThrow().renfort().actif());
    }

    @Test
    void prevision_avec_renfort_statistiques_par_journee_et_fin_avec_et_sans() {
        ReglagesPrevision reglages = ReglagesPrevision.builder().nombreDeTables(4)
                .renfort(ReglageRenfort.tables(4)).build();
        PrevisionTournoi p = simulateur.prevoir(tournoi(), reglages);
        PrevisionTournoi.PrevisionJournee j = p.journees().get(JOUR);
        assertTrue(j.avecRenfort());
        assertEquals(4, j.tablesRenfort());
        assertEquals(StrategieOrdonnancement.values().length, j.sansRenfort().size());
        StatistiquesSimulation avec = j.pour(S).orElseThrow();
        StatistiquesSimulation sans = j.sansRenfortPour(S).orElseThrow();
        assertFalse(sans.renfort().actif(), "les tirages « sans » n'ont pas de renfort");
        StatistiquesRenfort r = avec.renfort();
        assertEquals(4, r.tablesMax());
        assertTrue(r.probabiliteDeploiement() > 0.5);
        assertTrue(r.utilisationMoyenne() > 0 && r.utilisationMoyenne() <= 1);
        assertNotNull(r.premiereMiseEnService());
        assertFalse(r.premiereMiseEnServiceTot().isAfter(r.premiereMiseEnService()));
        assertTrue(r.tablesEnServiceMax() >= 1 && r.tablesEnServiceMax() <= 4);
        assertFalse(r.plagesHabituelles().isEmpty());
        assertTrue(avec.finMoyenne().isBefore(sans.finMoyenne()), "le renfort fait finir plus tôt");
        assertEquals(p, simulateur.prevoir(tournoi(), reglages), "reproductible");
    }

    // ------------------------------------------------------------------
    // Agrégation
    // ------------------------------------------------------------------

    private static ResultatSimulation tirage(RenfortTirage renfort) {
        LocalDateTime d = JOUR.atTime(9, 0);
        return new ResultatSimulation(S, 1, 4, d, d.plusHours(8), List.of(), List.of(), 0, 0, 0, null, 0,
                true, -1, -1, 0, MesuresTirage.VIDE, renfort);
    }

    private static RenfortTirage ouvert(LocalTime debut, LocalTime fin, int tables, double utilisation) {
        return new RenfortTirage(4, 60, utilisation, JOUR.atTime(debut), tables,
                List.of(new RenfortTirage.Plage(JOUR.atTime(debut), JOUR.atTime(fin), tables)));
    }

    @Test
    void agregation_probabilite_utilisation_premiere_mise_en_service_et_plages_habituelles() {
        List<ResultatSimulation> tirages = List.of(
                tirage(ouvert(LocalTime.of(10, 0), LocalTime.of(10, 40), 2, 0.2)),
                tirage(ouvert(LocalTime.of(10, 5), LocalTime.of(10, 40), 3, 0.3)),
                tirage(ouvert(LocalTime.of(10, 20), LocalTime.of(11, 50), 4, 0.5)),
                tirage(new RenfortTirage(4, 0, 0, null, 0, List.of())));
        StatistiquesRenfort s = StatistiquesRenfort.depuis(tirages);
        assertEquals(4, s.tablesMax());
        assertEquals(0.75, s.probabiliteDeploiement(), 1e-9);
        assertEquals(0.25, s.utilisationMoyenne(), 1e-9);
        assertEquals(JOUR.atTime(10, 5), s.premiereMiseEnService(), "médiane des tirages qui ont ouvert");
        assertEquals(JOUR.atTime(10, 0), s.premiereMiseEnServiceTot());
        assertEquals(3, s.tablesEnServiceMax());
        // Quarts d'heure ouverts dans au moins 2 tirages sur 4 : 10 h à 10 h 45.
        assertEquals(1, s.plagesHabituelles().size());
        assertEquals(JOUR.atTime(10, 0), s.plagesHabituelles().get(0).debut());
        assertEquals(JOUR.atTime(10, 45), s.plagesHabituelles().get(0).fin());
    }

    @Test
    void agregation_sans_renfort_vide() {
        assertSame(StatistiquesRenfort.AUCUN, StatistiquesRenfort.depuis(List.of(tirage(RenfortTirage.AUCUN))));
    }
}
