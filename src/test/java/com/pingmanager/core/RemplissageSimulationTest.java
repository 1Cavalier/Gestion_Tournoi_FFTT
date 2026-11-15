package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatPoule3Qualifie2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Taux de remplissage des tableaux pour la simulation (B050). */
class RemplissageSimulationTest {

    private static Tableau tableau(String nom, int placesMax, int inscrits) {
        ConfigurationTableau.Builder c = ConfigurationTableau.depuisFormat(FormatPoule3Qualifie2.INSTANCE)
                .pointsMax(1199);
        if (placesMax > 0) {
            c.placesMax(placesMax);
        }
        Tableau t = new Tableau(nom, c.build());
        JeuDeSimulation.joueurs(nom, inscrits, 500, 1100, nom.hashCode()).forEach(t::inscrire);
        return t;
    }

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Test", "Club", JeuDeSimulation.JOUR);
        t.ajouterTableau(tableau("A", 12, 5));
        t.ajouterTableau(tableau("B", 8, 8));
        t.ajouterTableau(tableau("C", 0, 7)); // sans limite de places
        return t;
    }

    @Test
    void le_taux_du_tableau_prime_sur_le_taux_global() {
        RemplissageSimulation r = RemplissageSimulation.global(0.95).avecTableau("B", 0.5);
        assertEquals(0.95, r.tauxGlobal().getAsDouble());
        assertEquals(0.95, r.tauxPour("A").getAsDouble());
        assertEquals(0.5, r.tauxPour("B").getAsDouble());
        assertEquals(0.95, r.sansTableau("B").tauxPour("B").getAsDouble(), "sans taux propre : le global");
        assertTrue(RemplissageSimulation.INSCRITS_REELS.tauxPour("A").isEmpty());
        assertTrue(RemplissageSimulation.INSCRITS_REELS.tauxGlobal().isEmpty());
        assertEquals(0.5, RemplissageSimulation.INSCRITS_REELS.avecTableau("B", 0.5).tauxPour("B").getAsDouble(),
                "un taux par tableau sans taux global");
        assertEquals(0.5, r.tauxParTableau().get("B"));
    }

    @Test
    void effectif_arrondi_de_la_capacite_fois_le_taux() {
        RemplissageSimulation r = RemplissageSimulation.global(RemplissageSimulation.TAUX_95);
        assertEquals(11, r.effectif("A", 12, 5), "12 × 0,95 = 11,4");
        assertEquals(29, r.effectif("A", 30, 5), "30 × 0,95 = 28,5 : arrondi à 29");
        assertEquals(46, r.effectif("A", 48, 60), "réduit même s'il y a plus d'inscrits");
        assertEquals(48, RemplissageSimulation.global(RemplissageSimulation.TAUX_PLEIN).effectif("A", 48, 3));
        assertEquals(0, RemplissageSimulation.global(0).effectif("A", 48, 3));
        assertEquals(7, r.effectif("C", 0, 7), "sans limite de places : les inscrits réels");
        assertEquals(5, RemplissageSimulation.INSCRITS_REELS.effectif("A", 12, 5), "sans taux : les inscrits réels");
    }

    @Test
    void taux_hors_de_zero_a_cent_pour_cent_refuse() {
        assertThrows(IllegalArgumentException.class, () -> RemplissageSimulation.global(1.01));
        assertThrows(IllegalArgumentException.class, () -> RemplissageSimulation.global(-0.1));
        assertThrows(IllegalArgumentException.class, () -> RemplissageSimulation.global(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> RemplissageSimulation.INSCRITS_REELS.avecTableau("A", 2));
        assertThrows(NullPointerException.class, () -> RemplissageSimulation.INSCRITS_REELS.avecTableau(null, 1));
    }

    @Test
    void la_simulation_complete_ou_reduit_chaque_tableau_selon_son_taux() {
        Tournoi t = tournoi();
        List<Joueur> inscritsB = t.getTableaux().get(1).getInscrits();
        RemplissageSimulation r = RemplissageSimulation.global(1.0).avecTableau("B", 0.5);
        ScenarioSimulation sc = SimulateurTournoi.scenarioDepuis(t, 8, r);
        assertEquals(SimulateurTournoi.GRAINE_INTERNE, sc.getGraine());
        assertEquals(12, sc.getTableaux().get(0).joueurs().size(), "A complété : 12 × 100 %");
        assertEquals(t.getTableaux().get(0).getInscrits(), sc.getTableaux().get(0).joueurs().subList(0, 5),
                "les inscrits réels d'abord");
        assertTrue(sc.getTableaux().get(0).joueurs().subList(5, 12).stream()
                .allMatch(j -> j.getNumeroLicence().startsWith("SIM-") && j.getPoints() <= 1199));
        assertEquals(inscritsB.subList(0, 4), sc.getTableaux().get(1).joueurs(), "B réduit : 8 × 50 %, premiers inscrits");
        assertEquals(7, sc.getTableaux().get(2).joueurs().size(), "C sans limite : inchangé");
        assertEquals(8, t.getTableaux().get(1).getInscrits().size(), "le tournoi n'est pas modifié");
        assertEquals(sc.getTableaux().get(0).joueurs(),
                SimulateurTournoi.scenarioDepuis(t, 8, r).getTableaux().get(0).joueurs(), "reproductible");
    }

    @Test
    void sans_taux_les_inscrits_reels_et_cent_pour_cent_vaut_l_ancien_completer() {
        Tournoi t = tournoi();
        ScenarioSimulation reel = SimulateurTournoi.scenarioDepuis(t, 8, RemplissageSimulation.INSCRITS_REELS);
        assertEquals(List.of(5, 8, 7), reel.getTableaux().stream().map(x -> x.joueurs().size()).toList());
        ScenarioSimulation plein = SimulateurTournoi.scenarioDepuis(t, 8, RemplissageSimulation.global(1.0));
        ScenarioSimulation ancien = SimulateurTournoi.scenarioDepuis(t, 8, SimulateurTournoi.GRAINE_INTERNE, true);
        for (int i = 0; i < 3; i++) {
            assertEquals(ancien.getTableaux().get(i).joueurs(), plein.getTableaux().get(i).joueurs());
        }
    }

    @Test
    void effectifs_par_tableau_inscrits_capacite_taux_et_joueurs_simules() {
        List<PrevisionTournoi.EffectifTableau> e = SimulateurTournoi.effectifs(tournoi(),
                RemplissageSimulation.global(0.95).avecTableau("B", 0.5));
        assertEquals(List.of("A", "B", "C"), e.stream().map(PrevisionTournoi.EffectifTableau::nom).toList());
        assertEquals(new PrevisionTournoi.EffectifTableau("A",
                JeuDeSimulation.JOUR.atTime(SimulateurTournoi.DEBUT_PAR_DEFAUT), 5, 12, 0.95, 11), e.get(0));
        assertEquals(6, e.get(0).fictifs());
        assertEquals(0, e.get(0).ecartes());
        assertEquals(4, e.get(1).simules());
        assertEquals(4, e.get(1).ecartes());
        assertEquals(0, e.get(1).fictifs());
        assertEquals(-1, e.get(2).taux(), "sans limite de places : aucun taux appliqué");
        assertEquals(0, e.get(2).capacite());
        assertEquals(7, e.get(2).simules());
        assertEquals(-1, SimulateurTournoi.effectifs(tournoi(), RemplissageSimulation.INSCRITS_REELS).get(0).taux());
    }
}
