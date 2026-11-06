package com.pingmanager.model;

import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.ResultatPlacementPlan.Motif;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Cas limites du plan de salle (B024) : bords de grille, numéros, plan vide, tourner. */
class PlanSalleLimitesTest {

    @Test
    void table_collee_au_dernier_coin_tient_et_un_cran_plus_loin_deborde() {
        PlanSalle p = PlanSalle.vide(4, 4);
        assertEquals(Motif.PLACE, p.ajouter(new TablePlan(1, 2, 2)).getMotif());
        assertEquals(Motif.HORS_GRILLE, p.ajouter(new TablePlan(1, 3, 2)).getMotif());
        assertEquals(Motif.HORS_GRILLE, p.ajouter(new TablePlan(1, 2, 3)).getMotif());
        assertEquals(Motif.HORS_GRILLE, p.ajouter(new TablePlan(1, -1, 0)).getMotif());
    }

    @Test
    void deux_tables_cote_a_cote_ne_se_chevauchent_pas_mais_decalees_d_une_case_si() {
        PlanSalle p = PlanSalle.vide(6, 2).ajouter(new TablePlan(1, 0, 0)).getPlan();
        assertEquals(Motif.PLACE, p.ajouter(new TablePlan(2, 2, 0)).getMotif());
        assertEquals(Motif.CHEVAUCHEMENT, p.ajouter(new TablePlan(2, 1, 0)).getMotif());
    }

    @Test
    void tourner_une_table_dans_une_grille_minimale_reussit_car_l_emprise_reste_2_sur_2() {
        PlanSalle p = PlanSalle.vide(2, 2).ajouter(new TablePlan(1, 0, 0)).getPlan();
        ResultatPlacementPlan r = p.tourner(1);
        assertTrue(r.estReussi());
        assertEquals(OrientationTable.VERTICALE, r.getPlan().table(1).orElseThrow().orientation());
        assertEquals(OrientationTable.HORIZONTALE,
                r.getPlan().tourner(1).getPlan().table(1).orElseThrow().orientation());
    }

    @Test
    void numeros_zero_negatif_et_doublon_refuses_et_la_meme_table_peut_garder_son_numero() {
        PlanSalle p = PlanSalle.vide(8, 2).ajouter(new TablePlan(1, 0, 0)).getPlan();
        assertEquals(Motif.NUMERO_INVALIDE, p.ajouter(new TablePlan(0, 2, 0)).getMotif());
        assertEquals(Motif.NUMERO_INVALIDE, p.ajouter(new TablePlan(-3, 2, 0)).getMotif());
        assertEquals(Motif.NUMERO_DEJA_PRIS, p.ajouter(new TablePlan(1, 2, 0)).getMotif());
        assertTrue(p.numeroter(1, 1).estReussi(), "renuméroter en soi-même n'est pas un doublon");
        PlanSalle deux = p.ajouter(new TablePlan(2, 2, 0)).getPlan();
        assertEquals(Motif.NUMERO_DEJA_PRIS, deux.numeroter(2, 1).getMotif());
        assertEquals(Motif.ELEMENT_INTROUVABLE, deux.numeroter(9, 10).getMotif());
        assertEquals(Motif.ELEMENT_INTROUVABLE, deux.tourner(9).getMotif());
        assertEquals(Motif.ELEMENT_INTROUVABLE, deux.reserver(9, "A").getMotif());
    }

    @Test
    void plan_vide_aucune_table_aucune_place_prise_et_retrait_introuvable() {
        PlanSalle p = PlanSalle.vide(1, 1);
        assertTrue(p.getTables().isEmpty());
        assertTrue(p.table(1).isEmpty());
        assertTrue(p.elementEn(0, 0).isEmpty());
        assertTrue(p.premierePlaceLibre(2, 2).isEmpty(), "une table 2x2 ne tient pas dans 1x1");
        assertArrayEquals(new int[] {0, 0}, p.premierePlaceLibre(1, 1).orElseThrow());
        assertEquals(Motif.ELEMENT_INTROUVABLE, p.retirer(0, 0).getMotif());
        assertEquals(Motif.ELEMENT_INTROUVABLE, p.deplacer(0, 0, 0, 0).getMotif());
    }

    @Test
    void dimensions_limites_de_la_grille() {
        assertThrows(IllegalArgumentException.class, () -> PlanSalle.vide(0, 5));
        assertThrows(IllegalArgumentException.class, () -> PlanSalle.vide(5, PlanSalle.COTE_MAXIMAL + 1));
        assertNotNull(PlanSalle.vide(PlanSalle.COTE_MAXIMAL, PlanSalle.COTE_MAXIMAL));
        assertEquals(Motif.DIMENSIONS_INVALIDES, PlanSalle.vide(3, 3).redimensionner(0, 3).getMotif());
        assertEquals(PlanSalle.COTE_MAXIMAL,
                PlanSalle.reconstruire(9999, -4, List.of()).getColonnes());
        assertEquals(1, PlanSalle.reconstruire(9999, -4, List.of()).getLignes());
    }

    @Test
    void un_element_deplace_hors_grille_ou_sur_un_autre_reste_ou_il_etait() {
        PlanSalle p = PlanSalle.vide(6, 2).ajouter(new TablePlan(1, 0, 0)).getPlan()
                .ajouter(new TablePlan(2, 2, 0)).getPlan();
        assertEquals(Motif.HORS_GRILLE, p.deplacer(0, 0, 5, 0).getMotif());
        assertEquals(Motif.CHEVAUCHEMENT, p.deplacer(0, 0, 1, 0).getMotif());
        assertEquals(0, p.table(1).orElseThrow().colonne());
        assertTrue(p.deplacer(0, 0, 4, 0).estReussi());
    }

    @Test
    void reserver_puis_lever_la_reservation_et_nom_blanc_equivaut_a_aucune() {
        PlanSalle p = PlanSalle.vide(4, 2).ajouter(new TablePlan(1, 0, 0)).getPlan();
        PlanSalle r = p.reserver(1, "  Dames ").getPlan();
        assertEquals("Dames", r.table(1).orElseThrow().tableauReserve());
        assertNull(r.reserver(1, null).getPlan().table(1).orElseThrow().tableauReserve());
        assertNull(r.reserver(1, "   ").getPlan().table(1).orElseThrow().tableauReserve());
    }

    @Test
    void allee_et_repere_comptent_pour_le_chevauchement_mais_pas_pour_les_numeros() {
        PlanSalle p = PlanSalle.vide(4, 4).ajouter(new AlleePlan(0, 0)).getPlan();
        assertEquals(Motif.CHEVAUCHEMENT, p.ajouter(new TablePlan(1, 0, 0)).getMotif());
        assertTrue(p.ajouter(new TablePlan(1, 1, 0)).estReussi());
        assertTrue(p.getTables().isEmpty());
        assertEquals(Motif.CHEVAUCHEMENT,
                p.ajouter(new ReperePlan(TypeRepere.WC, "x", 0, 0, 1, 1)).getMotif());
    }

    @Test
    void salle_sans_table_et_noms_de_reservation_blancs_ou_nuls_nettoyes() {
        SalleTournoi s = new SalleTournoi("Annexe", 0, PlanSalle.vide(3, 3),
                Set.of(" "), null);
        assertTrue(s.getPlan().getTables().isEmpty());
        assertTrue(s.getTableauxReserves().isEmpty());
        assertTrue(s.getTableauxPreferes().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new SalleTournoi("  ", PlanSalle.vide(1, 1)));
        assertThrows(NullPointerException.class, () -> new SalleTournoi("A", null));
    }

    @Test
    void le_tournoi_accepte_une_liste_nulle_ou_vide_de_salles_et_revient_a_sans_plan() {
        Tournoi t = new Tournoi("T", "C", java.time.LocalDate.of(2026, 11, 14));
        t.setSalles(List.of(new SalleTournoi("A", PlanSalle.vide(2, 2))));
        assertTrue(t.aUnPlanDeSalle());
        t.setSalles(null);
        assertFalse(t.aUnPlanDeSalle());
        t.setSalles(List.of());
        assertFalse(t.aUnPlanDeSalle());
    }
}
