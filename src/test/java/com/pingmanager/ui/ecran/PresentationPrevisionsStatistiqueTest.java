package com.pingmanager.ui.ecran;

import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.core.StatistiquesSimulation.StatTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresentationPrevisionsStatistiqueTest {

    private static final LocalDateTime J = LocalDateTime.of(2027, 4, 10, 0, 0);

    private static StatistiquesSimulation stat(StrategieOrdonnancement s, int finMinutes, double attente,
                                               double debordement) {
        LocalDateTime fin = J.plusHours(18).plusMinutes(finMinutes);
        StatTableau t = new StatTableau("A", J.plusHours(19), fin, fin.plusMinutes(20), debordement, attente);
        return new StatistiquesSimulation(s, 100, 12, fin, fin.minusMinutes(20), fin, fin.plusMinutes(20),
                fin.plusMinutes(40), attente, attente * 4, 0.7, debordement, 0, List.of(t));
    }

    @Test
    void reglages_tirages_et_durees_lus_avec_erreurs_claires() {
        assertEquals(100, PresentationPrevisions.lireTirages(""));
        assertEquals(250, PresentationPrevisions.lireTirages(" 250 "));
        assertNull(PresentationPrevisions.lireTirages("0"));
        assertNull(PresentationPrevisions.lireTirages("beaucoup"));
        PresentationPrevisions.ReglageDurees ok = PresentationPrevisions.lireDurees("", "", "");
        assertNull(ok.erreur());
        assertEquals(20, ok.loi().getMoyenne().toMinutes());
        assertEquals(PresentationPrevisions.ERREUR_DUREES, PresentationPrevisions.lireDurees("vingt", "10", "50").erreur());
        assertTrue(PresentationPrevisions.lireDurees("35", "10", "50").erreur().contains("plus proche du minimum"));
    }

    @Test
    void tableau_des_strategies_meilleure_valeur_et_verdict() {
        List<StatistiquesSimulation> stats = List.of(
                stat(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, 40, 20, 0.3),
                stat(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 10, 30, 0.1),
                stat(StrategieOrdonnancement.MINIMISER_ATTENTE, 25, 15, 0.2));
        List<PresentationPrevisions.LigneStrategies> lignes = PresentationPrevisions.lignesStrategies(stats);
        assertEquals("Fin moyenne", lignes.get(0).indicateur());
        assertEquals(List.of("18 h 40", "18 h 10", "18 h 25"), lignes.get(0).valeurs());
        assertEquals(1, lignes.get(0).meilleure());
        assertEquals(2, lignes.get(3).meilleure(), "attente moyenne la plus faible");
        assertEquals(-1, lignes.get(5).meilleure(), "occupation égale : pas de meilleure");
        String v = PresentationPrevisions.verdictStrategies(stats, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        assertTrue(v.startsWith("« Tout faire avancer petit à petit » finit en moyenne le plus tôt (30 min avant"), v);
        assertTrue(v.contains("attendent le moins avec « Faire attendre le moins possible les joueurs »"), v);
        assertTrue(v.contains("n'est la meilleure sur aucun"), v);
    }

    @Test
    void salle_jamais_saturee_le_verdict_le_dit() {
        List<StatistiquesSimulation> stats = List.of(
                stat(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, 10, 12, 0),
                stat(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 12, 12.5, 0),
                stat(StrategieOrdonnancement.MINIMISER_ATTENTE, 10, 12, 0));
        assertTrue(PresentationPrevisions.verdictStrategies(stats, StrategieOrdonnancement.PAR_DEFAUT)
                .startsWith("Avec 12 tables, la salle n'est presque jamais saturée"));
    }

    @Test
    void lignes_par_tableau_avec_risque_de_debordement() {
        PresentationPrevisions.LigneTableauStat l = PresentationPrevisions.lignesTableaux(
                stat(StrategieOrdonnancement.PAR_DEFAUT, 10, 12, 0.35)).get(0);
        assertEquals("A", l.nom());
        assertEquals("18 h 10", l.finMoyenne());
        assertEquals("18 h 30", l.finP90());
        assertEquals("19 h 00", l.finSouhaitee());
        assertEquals("Déborde 35 % des fois", l.risque());
        assertEquals(StatutPastille.ATTENTION, l.statut());
        assertEquals("À l'heure", PresentationPrevisions.lignesTableaux(
                stat(StrategieOrdonnancement.PAR_DEFAUT, 10, 12, 0)).get(0).risque());
        assertEquals("< 1 %", PresentationPrevisions.pourcent(0.004));
    }
}
