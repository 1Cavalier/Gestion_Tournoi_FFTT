package com.pingmanager.ui.ecran;

import com.pingmanager.core.PrevisionTournoi;
import com.pingmanager.core.RenfortTirage;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.core.StatistiquesRenfort;
import com.pingmanager.model.StrategieOrdonnancement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** B054 : saisie des tables de renfort et bilan du renfort dans les prévisions. */
class PresentationRenfortTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 14);
    private static final StrategieOrdonnancement S = StrategieOrdonnancement.PAR_DEFAUT;

    @Test
    void saisie_du_renfort_de_zero_au_nombre_de_tables_d_echauffement() {
        assertEquals(0, PresentationRenfort.lireTablesRenfort("", 8));
        assertEquals(4, PresentationRenfort.lireTablesRenfort(" 4 ", 8));
        assertEquals(8, PresentationRenfort.lireTablesRenfort("8", 8));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PresentationRenfort.lireTablesRenfort("9", 8));
        assertTrue(e.getMessage().contains("au plus 8 tables"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> PresentationRenfort.lireTablesRenfort("deux", 8));
    }

    @Test
    void sans_salle_d_echauffement_le_renfort_vaut_zero() {
        assertEquals(0, PresentationRenfort.lireTablesRenfort("5", 0));
    }

    @Test
    void reglage_de_la_prevision_seuils_par_defaut_et_erreurs() {
        PresentationRenfort.ReglageLu lu = PresentationRenfort.lireReglage("3", "", "", 8);
        assertNull(lu.erreur());
        assertEquals(3, lu.reglage().getTablesMax());
        assertEquals(15, lu.reglage().getSeuilDeploiementMinutes());
        assertEquals(0, lu.reglage().getSeuilRetraitMinutes());
        assertNotNull(PresentationRenfort.lireReglage("9", "15", "0", 8).erreur());
        assertNotNull(PresentationRenfort.lireReglage("3", "10", "20", 8).erreur(), "retour au-delà de l'ouverture");
        assertNotNull(PresentationRenfort.lireReglage("3", "x", "0", 8).erreur());
        assertEquals(30, PresentationRenfort.lireReglage("3", "30", "5", 8).reglage().getSeuilDeploiementMinutes());
    }

    private static StatistiquesSimulation stats(LocalDateTime fin, double debordement, StatistiquesRenfort r) {
        return new StatistiquesSimulation(S, 100, 10, fin, fin, fin, fin.plusMinutes(20), fin.plusMinutes(30),
                null, null, 1, 0, 0, 0, -1, -1, 0, 0.8, debordement, 7, 0, List.of(), null, r);
    }

    private static PrevisionTournoi.PrevisionJournee journee(StatistiquesRenfort r, LocalDateTime finAvec,
                                                             double risqueAvec, LocalDateTime finSans,
                                                             double risqueSans) {
        return new PrevisionTournoi.PrevisionJournee(JOUR, JOUR.atTime(20, 0), 10, List.of(),
                List.of(stats(finAvec, risqueAvec, r)), 4, List.of(stats(finSans, risqueSans, StatistiquesRenfort.AUCUN)));
    }

    private static final StatistiquesRenfort OUVERT = new StatistiquesRenfort(4, 0.9, 0.18, 200,
            JOUR.atTime(14, 30), JOUR.atTime(13, 45), 3,
            List.of(new RenfortTirage.Plage(JOUR.atTime(14, 30), JOUR.atTime(17, 15), 3)));

    @Test
    void bilan_le_renfort_permet_de_finir_a_l_heure() {
        PresentationRenfort.Bilan b = PresentationRenfort.bilan(
                journee(OUVERT, JOUR.atTime(19, 40), 0.1, JOUR.atTime(20, 35), 0.85), S).orElseThrow();
        assertEquals("Le renfort permet de finir à l'heure : sans lui, la journée dépasse la fin visée dans 85 % des "
                + "cas, avec lui dans 10 %. Prévoir d'ouvrir les tables d'échauffement vers 14 h 30.", b.verdict());
        String lignes = b.lignes().stream().map(l -> l.indicateur() + " = " + l.valeur())
                .collect(Collectors.joining("\n"));
        assertTrue(lignes.contains("Fin visée = 20 h 00"), lignes);
        assertTrue(lignes.contains("Fin prévue sans renfort = vers 20 h 35 (1 fois sur 20 après 20 h 55)"), lignes);
        assertTrue(lignes.contains("Fin prévue avec renfort = vers 19 h 40 (1 fois sur 20 après 20 h 00)"), lignes);
        assertTrue(lignes.contains("Temps gagné en moyenne = 55 min"), lignes);
        assertTrue(lignes.contains("Renfort ouvert = 90 % des simulations"), lignes);
        assertTrue(lignes.contains("Première mise en service = vers 14 h 30 (au plus tôt 13 h 45)"), lignes);
        assertTrue(lignes.contains("Plages habituelles = 14 h 30 – 17 h 15 (3 tables)"), lignes);
        assertTrue(lignes.contains("Tables ouvertes en même temps = 3 sur 4"), lignes);
        assertTrue(lignes.contains("Utilisation des tables de renfort = 18 % de la journée"), lignes);
    }

    @Test
    void bilan_meme_avec_le_renfort_la_fin_est_depassee() {
        String v = PresentationRenfort.bilan(
                journee(OUVERT, JOUR.atTime(21, 0), 0.9, JOUR.atTime(22, 0), 1), S).orElseThrow().verdict();
        assertTrue(v.startsWith("Même avec le renfort, la journée dépasse la fin visée dans 90 % des cas"), v);
    }

    @Test
    void bilan_renfort_presque_jamais_necessaire() {
        StatistiquesRenfort rare = new StatistiquesRenfort(4, 0.02, 0.001, 1, JOUR.atTime(15, 0),
                JOUR.atTime(15, 0), 1, List.of());
        PresentationRenfort.Bilan b = PresentationRenfort.bilan(
                journee(rare, JOUR.atTime(18, 0), 0, JOUR.atTime(18, 1), 0), S).orElseThrow();
        assertEquals("Le renfort n'a presque jamais été nécessaire : les 10 tables de compétition suffisent.",
                b.verdict());
        assertTrue(b.lignes().stream().anyMatch(l -> l.indicateur().equals("Plages habituelles")
                && l.valeur().startsWith("aucune")));
    }

    @Test
    void sans_renfort_pas_de_bilan() {
        PrevisionTournoi.PrevisionJournee j = new PrevisionTournoi.PrevisionJournee(JOUR, JOUR.atTime(20, 0), 10,
                List.of(), List.of(stats(JOUR.atTime(19, 0), 0, StatistiquesRenfort.AUCUN)));
        assertTrue(PresentationRenfort.bilan(j, S).isEmpty());
    }

    @Test
    void bilan_journee_sans_partie_simulee() {
        PrevisionTournoi.PrevisionJournee j = new PrevisionTournoi.PrevisionJournee(JOUR, JOUR.atTime(20, 0), 10,
                List.of(), List.of(new StatistiquesSimulation(S, 100, 10, null, null, null, null, null, null, null,
                0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, List.of(), null, StatistiquesRenfort.AUCUN)), 4,
                List.of(new StatistiquesSimulation(S, 100, 10, null, null, null, null, null, null, null,
                        0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, List.of(), null, StatistiquesRenfort.AUCUN)));
        PresentationRenfort.Bilan b = PresentationRenfort.bilan(j, S).orElseThrow();
        assertEquals("Aucune partie simulée pour cette journée : rien à dire sur le renfort.", b.verdict());
        assertTrue(b.lignes().stream().anyMatch(l -> l.valeur().equals("—")));
    }
}
