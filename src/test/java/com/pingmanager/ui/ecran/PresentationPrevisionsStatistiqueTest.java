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
        StatTableau t = new StatTableau("A", J.plusHours(9), 12, J.plusHours(19), fin, fin.plusMinutes(20),
                debordement, attente);
        return new StatistiquesSimulation(s, 100, 12, fin, fin.minusMinutes(20), fin, fin.plusMinutes(20),
                fin.plusMinutes(40), fin.minusMinutes(90), fin.minusMinutes(30), 0.6, 0.3, 0.1, attente, attente - 2, attente + 5, attente * 4, 0.7,
                debordement, 6.4, 0, List.of(t));
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
        assertEquals(List.of("Début à l'heure (5 min de retard au plus)", "Début en léger retard (5 à 30 min)",
                "Début en gros retard (plus de 30 min)", "Première finale (en moyenne)", "Fin moyenne d'un tableau",
                "Fin de la journée (moyenne)", "Cas rare : fin tôt (1 fois sur 20)",
                "Cas rare : fin tard (1 fois sur 20)", "Attente moyenne d'un joueur inscrit sur 1 tableau",
                "Attente moyenne d'un joueur inscrit sur 2 tableaux ou plus", "Plus longue attente",
                "Occupation des tables", "Risque qu'un tableau finisse après l'heure visée"),
                lignes.stream().map(PresentationPrevisions.LigneStrategies::indicateur).toList());
        assertEquals("60 %", lignes.get(0).valeurs().get(0));
        assertEquals(List.of("sam. 10/04, 18 h 40", "sam. 10/04, 18 h 10", "sam. 10/04, 18 h 25"),
                lignes.get(5).valeurs());
        assertEquals(1, lignes.get(5).meilleure());
        assertEquals("sam. 10/04, 17 h 10", lignes.get(3).valeurs().get(0), "première finale");
        assertEquals(2, lignes.get(8).meilleure(), "attente sur 1 tableau la plus faible");
        assertEquals("13 min", lignes.get(8).valeurs().get(2));
        assertEquals("20 min", lignes.get(9).valeurs().get(2));
        assertEquals(-1, lignes.get(11).meilleure(), "occupation égale : pas de meilleure");
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
        assertEquals("9 h 00 (+12 min)", l.debut());
        assertEquals("18 h 10", l.finMoyenne());
        assertEquals("18 h 30", l.finTard());
        assertEquals("19 h 00", l.finSouhaitee());
        assertEquals("35 % des fois", l.risque());
        assertEquals(StatutPastille.ATTENTION, l.statut());
        assertEquals("Jamais", PresentationPrevisions.lignesTableaux(
                stat(StrategieOrdonnancement.PAR_DEFAUT, 10, 12, 0)).get(0).risque());
        assertEquals("< 1 %", PresentationPrevisions.pourcent(0.004));
    }

    @Test
    void temps_autour_du_match_lus_et_annonces_dans_la_synthese() {
        PresentationPrevisions.ReglageAnnexes ok = PresentationPrevisions.lireTempsAnnexes("", "", "", "", "", "", "");
        assertNull(ok.erreur());
        assertEquals(120, ok.temps().getEchauffement());
        assertEquals(90, ok.temps().getFeuille(), "1,5 min");
        assertEquals(0.15, ok.temps().getProbabiliteRappel(), 1e-9);
        assertEquals(PresentationPrevisions.ERREUR_ANNEXES,
                PresentationPrevisions.lireTempsAnnexes("un", "", "", "", "", "", "").erreur());
        assertTrue(PresentationPrevisions.lireTempsAnnexes("", "", "", "", "90", "", "").erreur()
                .contains("3.4.4.1.1"));
        String synthese = PresentationPrevisions.syntheseStatistique(
                List.of(stat(StrategieOrdonnancement.PAR_DEFAUT, 10, 12, 0)), null);
        assertTrue(synthese.contains("chaque match occupe sa table 6 min 20 en moyenne"), synthese);
        assertEquals("6 min 30", PresentationPrevisions.minutesDecimales(6.5));
        assertEquals("7 min", PresentationPrevisions.minutesDecimales(6.999));
    }

    @Test
    void verdict_decrit_les_finales_etalees_ou_gardees_pour_la_fin() {
        List<StatistiquesSimulation> stats = List.of(
                stat(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, 40, 20, 0.3),
                stat(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 10, 30, 0.1),
                stat(StrategieOrdonnancement.MINIMISER_ATTENTE, 25, 15, 0.2));
        String v = PresentationPrevisions.verdictStrategies(stats, StrategieOrdonnancement.PAR_DEFAUT);
        assertTrue(v.contains("chaque tableau joue sa finale à son heure : la première vers 17 h 10"), v);
        assertTrue(v.contains("toutes les finales sont gardées pour la fin : de 16 h 40 à 18 h 10"), v);
    }

    @Test
    void comparaison_par_tableau_trois_strategies_cote_a_cote() {
        List<StatistiquesSimulation> stats = List.of(
                stat(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, 40, 20, 0.3),
                stat(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 10, 30, 0),
                stat(StrategieOrdonnancement.MINIMISER_ATTENTE, 25, 15, 0.6));
        List<PresentationPrevisions.TableauCompare> t = PresentationPrevisions.tableauxCompares(stats);
        assertEquals(1, t.size());
        assertEquals("A", t.get(0).nom());
        assertEquals(3, t.get(0).cellules().size());
        assertEquals(1, t.get(0).finitLePlusTot());
        assertEquals("18 h 40", t.get(0).cellules().get(0).fin());
        assertEquals("Jamais", t.get(0).cellules().get(1).risque());
        assertEquals(StatutPastille.ERREUR, t.get(0).cellules().get(2).statut());
        assertTrue(PresentationPrevisions.tableauxCompares(List.of()).isEmpty());
    }

    @Test
    void heure_de_fin_visee_et_journees() {
        assertEquals(java.time.LocalTime.of(20, 0), PresentationPrevisions.lireFinVisee(""));
        assertEquals(java.time.LocalTime.of(21, 30), PresentationPrevisions.lireFinVisee("21:30"));
        assertEquals(java.time.LocalTime.of(21, 30), PresentationPrevisions.lireFinVisee("21 h 30"));
        assertEquals(java.time.LocalTime.of(19, 0), PresentationPrevisions.lireFinVisee("19h"));
        assertNull(PresentationPrevisions.lireFinVisee("25:00"));
        assertNull(PresentationPrevisions.lireFinVisee("tard"));
        assertEquals("Samedi 10 avril", PresentationPrevisions.libelleJour(java.time.LocalDate.of(2027, 4, 10)));
        assertEquals("21:30", PresentationPrevisions.finViseeProposee(List.of(J.withHour(19), J.withHour(21).withMinute(30))));
        assertEquals("20:00", PresentationPrevisions.finViseeProposee(java.util.Arrays.asList((LocalDateTime) null)));
    }
}
