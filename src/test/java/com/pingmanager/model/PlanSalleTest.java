package com.pingmanager.model;

import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Plan d'une salle sur une grille (B024) : table 2 × 2, allée 1 × 1, repères ; placement validé. */
class PlanSalleTest {

    private static PlanSalle avec(PlanSalle p, ElementPlan e) {
        ResultatPlacementPlan r = p.ajouter(e);
        assertTrue(r.estReussi(), r.toString());
        return r.getPlan();
    }

    @Test
    void une_table_occupe_deux_cases_sur_deux_et_une_allee_une_case() {
        PlanSalle p = avec(avec(PlanSalle.vide(6, 4), new TablePlan(1, 0, 0)), new AlleePlan(2, 0));
        assertEquals(1, p.elementEn(1, 1).map(e -> ((TablePlan) e).numero()).orElse(0));
        assertTrue(p.elementEn(2, 0).orElseThrow() instanceof AlleePlan);
        assertTrue(p.elementEn(2, 1).isEmpty(), "l'allée ne fait qu'une case");
        assertTrue(p.elementEn(0, 2).isEmpty());
        assertEquals(List.of(new AlleePlan(2, 0)), p.getAllees());
    }

    @Test
    void un_element_qui_deborde_de_la_grille_est_refuse_et_le_plan_ne_change_pas() {
        PlanSalle p = PlanSalle.vide(4, 4);
        ResultatPlacementPlan r = p.ajouter(new TablePlan(1, 3, 0));
        assertEquals(ResultatPlacementPlan.Motif.HORS_GRILLE, r.getMotif());
        assertFalse(r.estReussi());
        assertSame(p, r.getPlan());
        assertEquals(ResultatPlacementPlan.Motif.HORS_GRILLE, p.ajouter(new AlleePlan(-1, 0)).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.PLACE, p.ajouter(new TablePlan(1, 2, 2)).getMotif(),
                "une table dans le dernier coin tient juste");
    }

    @Test
    void deux_elements_ne_se_chevauchent_jamais_et_le_refus_nomme_l_element_qui_gene() {
        PlanSalle p = avec(PlanSalle.vide(8, 8), new TablePlan(1, 2, 2));
        ResultatPlacementPlan r = p.ajouter(new TablePlan(2, 3, 3));
        assertEquals(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, r.getMotif());
        assertEquals(new TablePlan(1, 2, 2), r.getConflit());
        assertEquals("CHEVAUCHEMENT (la table 1)", r.toString());
        assertEquals(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, p.ajouter(new AlleePlan(3, 3)).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.CHEVAUCHEMENT,
                p.ajouter(new ReperePlan(TypeRepere.TABLE_ARBITRAGE, 1, 2)).getMotif(), "repère de 2 × 1");
        assertTrue(p.ajouter(new TablePlan(2, 4, 2)).estReussi(), "tables côte à côte sans case commune");
    }

    @Test
    void deux_tables_du_meme_plan_ne_portent_pas_le_meme_numero() {
        PlanSalle p = avec(PlanSalle.vide(8, 8), new TablePlan(5, 0, 0));
        assertEquals(ResultatPlacementPlan.Motif.NUMERO_DEJA_PRIS, p.ajouter(new TablePlan(5, 4, 4)).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.NUMERO_INVALIDE, p.ajouter(new TablePlan(0, 4, 4)).getMotif());
    }

    @Test
    void deplacer_une_table_par_n_importe_laquelle_de_ses_cases_sans_qu_elle_se_gene_elle_meme() {
        PlanSalle p = avec(avec(PlanSalle.vide(8, 4), new TablePlan(1, 0, 0)), new TablePlan(2, 4, 0));
        ResultatPlacementPlan r = p.deplacer(1, 1, 1, 0);
        assertEquals(ResultatPlacementPlan.Motif.DEPLACE, r.getMotif());
        assertEquals(new TablePlan(1, 1, 0), r.getPlan().table(1).orElseThrow());
        assertEquals(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, r.getPlan().deplacer(1, 0, 3, 0).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, p.deplacer(7, 3, 0, 0).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.HORS_GRILLE, p.deplacer(0, 0, 0, 3).getMotif());
    }

    @Test
    void numeroter_tourner_reserver_et_retirer_une_table() {
        PlanSalle p = avec(avec(PlanSalle.vide(8, 4), new TablePlan(1, 0, 0)), new TablePlan(2, 4, 0));
        assertEquals(ResultatPlacementPlan.Motif.NUMERO_DEJA_PRIS, p.numeroter(1, 2).getMotif());
        PlanSalle q = p.numeroter(1, 12).getPlan();
        assertTrue(q.table(12).isPresent());
        assertTrue(q.table(1).isEmpty());
        assertEquals(List.of(2, 12), q.getTables().stream().map(TablePlan::numero).toList(), "par numéro croissant");

        PlanSalle tournee = q.tourner(12).getPlan();
        assertEquals(OrientationTable.VERTICALE, tournee.table(12).orElseThrow().orientation());
        assertEquals(OrientationTable.HORIZONTALE, tournee.tourner(12).getPlan().table(12).orElseThrow().orientation());

        PlanSalle reservee = q.reserver(2, " Dames ").getPlan();
        assertEquals("Dames", reservee.table(2).orElseThrow().tableauReserve());
        assertNull(reservee.reserver(2, null).getPlan().table(2).orElseThrow().tableauReserve());

        ResultatPlacementPlan retrait = q.retirer(5, 1);
        assertEquals(ResultatPlacementPlan.Motif.RETIRE, retrait.getMotif());
        assertEquals(1, retrait.getPlan().getTables().size());
        assertEquals(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, q.tourner(99).getMotif());
    }

    @Test
    void la_grille_ne_retrecit_pas_au_point_de_couper_un_element() {
        PlanSalle p = avec(PlanSalle.vide(10, 10), new TablePlan(1, 6, 6));
        ResultatPlacementPlan r = p.redimensionner(7, 10);
        assertEquals(ResultatPlacementPlan.Motif.GRILLE_TROP_PETITE, r.getMotif());
        assertEquals(new TablePlan(1, 6, 6), r.getConflit());
        assertEquals(ResultatPlacementPlan.Motif.REDIMENSIONNE, p.redimensionner(8, 8).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES, p.redimensionner(0, 8).getMotif());
        assertEquals(ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES,
                p.redimensionner(PlanSalle.COTE_MAXIMAL + 1, 8).getMotif());
        assertThrows(IllegalArgumentException.class, () -> PlanSalle.vide(0, 3));
    }

    @Test
    void un_repere_prend_sa_taille_par_defaut_et_son_libelle() {
        ReperePlan ja = new ReperePlan(TypeRepere.TABLE_ARBITRAGE, 0, 0);
        assertEquals(2, ja.largeur());
        assertEquals(1, ja.hauteur());
        assertEquals("Table d'arbitrage", ja.libelle());
        assertEquals("Buvette du club", new ReperePlan(TypeRepere.BUVETTE, "Buvette du club", 0, 0, 3, 1).libelle());
        assertEquals(ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES,
                PlanSalle.vide(4, 4).ajouter(new ReperePlan(TypeRepere.AUTRE, null, 0, 0, 0, 1)).getMotif());
    }

    @Test
    void la_premiere_place_libre_se_cherche_ligne_par_ligne() {
        PlanSalle p = avec(PlanSalle.vide(4, 4), new TablePlan(1, 0, 0));
        assertArrayEquals(new int[] {2, 0}, p.premierePlaceLibre(2, 2).orElseThrow());
        PlanSalle plein = avec(avec(avec(p, new TablePlan(2, 2, 0)), new TablePlan(3, 0, 2)), new TablePlan(4, 2, 2));
        assertTrue(plein.premierePlaceLibre(1, 1).isEmpty());
    }

    @Test
    void un_plan_relu_ecarte_les_elements_qui_ne_tiennent_pas() {
        PlanSalle p = PlanSalle.reconstruire(4, 4, List.of(new TablePlan(1, 0, 0), new TablePlan(2, 1, 1),
                new TablePlan(1, 2, 2), new AlleePlan(9, 9), new AlleePlan(3, 3)));
        assertEquals(List.of(new TablePlan(1, 0, 0), new AlleePlan(3, 3)), p.getElements());
    }

    @Test
    void salle_du_tournoi_nom_etage_et_reservations() {
        SalleTournoi s = new SalleTournoi(" Dojo ", 1, PlanSalle.vide(4, 4), java.util.Set.of(" Dames", ""),
                null);
        assertEquals("Dojo", s.getNom());
        assertEquals(java.util.Set.of("Dames"), s.getTableauxReserves());
        assertTrue(s.getTableauxPreferes().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new SalleTournoi(" ", PlanSalle.vide(2, 2)));
        assertEquals("Rez-de-chaussée", SalleTournoi.libelleEtage(0));
        assertEquals("1er étage", SalleTournoi.libelleEtage(1));
        assertEquals("2e étage", SalleTournoi.libelleEtage(2));
        assertEquals("Sous-sol 1", SalleTournoi.libelleEtage(-1));
    }

    @Test
    void le_tournoi_refuse_deux_salles_du_meme_nom_ou_une_table_dans_deux_salles() {
        Tournoi t = new Tournoi("Open", "Club", java.time.LocalDate.of(2026, 11, 14));
        assertFalse(t.aUnPlanDeSalle());
        PlanSalle avecTable1 = avec(PlanSalle.vide(4, 4), new TablePlan(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> t.setSalles(List.of(
                new SalleTournoi("Gymnase", PlanSalle.vide(4, 4)), new SalleTournoi("gymnase", PlanSalle.vide(4, 4)))));
        assertThrows(IllegalArgumentException.class, () -> t.setSalles(List.of(
                new SalleTournoi("Gymnase", avecTable1), new SalleTournoi("Dojo", avecTable1))));
        assertFalse(t.aUnPlanDeSalle(), "rien n'est changé après un refus");
        t.setSalles(List.of(new SalleTournoi("Gymnase", avecTable1), new SalleTournoi("Dojo", PlanSalle.vide(4, 4))));
        assertEquals(2, t.getSalles().size());
        t.setSalles(null);
        assertFalse(t.aUnPlanDeSalle());
    }
}
