package com.pingmanager.ui.ecran;

import com.pingmanager.core.PlansTypesSalle;
import com.pingmanager.core.ResultatEditionSalles;
import com.pingmanager.core.ServicePlanSalle;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.ResultatPlacementPlan;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRepere;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Logique d'affichage du plan de salle (B024), sans JavaFX. */
class PresentationPlanSalleTest {

    private final ServicePlanSalle service = new ServicePlanSalle();

    @Test
    void tuiles_d_un_plan_type_avec_textes_et_classes() {
        PlanSalle p = PlansTypesSalle.generer(1, 2, 1).tourner(2).getPlan().reserver(1, "Dames").getPlan();
        List<PresentationPlanSalle.Tuile> tuiles = PresentationPlanSalle.tuiles(p);
        assertEquals(p.getElements().size(), tuiles.size());
        PresentationPlanSalle.Tuile t1 = tuiles.stream().filter(t -> t.element() instanceof TablePlan tp
                && tp.numero() == 1).findFirst().orElseThrow();
        assertEquals("1", t1.texte());
        assertEquals(2, t1.largeur());
        assertTrue(t1.classes().containsAll(List.of("plan-table", "plan-table-horizontale", "plan-table-reservee")));
        assertEquals("Table 1 · horizontale · réservée au tableau Dames", t1.infobulle());
        PresentationPlanSalle.Tuile t2 = tuiles.stream().filter(t -> t.element() instanceof TablePlan tp
                && tp.numero() == 2).findFirst().orElseThrow();
        assertTrue(t2.classes().contains("plan-table-verticale"));
        assertEquals("JA", PresentationPlanSalle.texte(new ReperePlan(TypeRepere.TABLE_ARBITRAGE, 0, 0)));
        assertEquals("Buvette", PresentationPlanSalle.texte(new ReperePlan(TypeRepere.BUVETTE, null, 0, 0, 3, 1)));
        assertEquals("WC du haut (WC)", PresentationPlanSalle.infobulle(
                new ReperePlan(TypeRepere.WC, "WC du haut", 0, 0, 1, 1)));
    }

    @Test
    void colonnes_et_lignes_qui_portent_une_table() {
        PlanSalle p = PlanSalle.vide(5, 4).ajouter(new TablePlan(1, 1, 2)).getPlan();
        assertArrayEquals(new boolean[] {false, true, true, false, false}, PresentationPlanSalle.avecTable(p, true));
        assertArrayEquals(new boolean[] {false, false, true, true}, PresentationPlanSalle.avecTable(p, false));
    }

    @Test
    void mini_carte_par_etage_du_plus_haut_au_plus_bas() {
        List<SalleTournoi> salles = List.of(
                new SalleTournoi("Gymnase", 0, PlansTypesSalle.generer(2, 12, 1), Set.of(), Set.of()),
                new SalleTournoi("Dojo", 1, PlansTypesSalle.generer(1, 1, 13), Set.of("Dames"), Set.of("Dames")),
                new SalleTournoi("Annexe", 0, PlansTypesSalle.generer(1, 0, 1), Set.of(), Set.of()));
        List<PresentationPlanSalle.EtageMiniCarte> carte = PresentationPlanSalle.miniCarte(salles, 1);
        assertEquals(List.of("1er étage", "Rez-de-chaussée"),
                carte.stream().map(PresentationPlanSalle.EtageMiniCarte::libelle).toList());
        PresentationPlanSalle.SalleMiniCarte dojo = carte.get(0).salles().get(0);
        assertTrue(dojo.courante());
        assertEquals("1 table · réservée à Dames · préférée par Dames", dojo.detail());
        assertEquals(List.of("Gymnase", "Annexe"),
                carte.get(1).salles().stream().map(PresentationPlanSalle.SalleMiniCarte::nom).toList());
        assertEquals("aucune table", carte.get(1).salles().get(1).detail());
        assertTrue(PresentationPlanSalle.afficheMiniCarte(salles));
        assertFalse(PresentationPlanSalle.afficheMiniCarte(salles.subList(0, 1)));
    }

    @Test
    void phrase_de_la_carte_et_alertes_de_coherence() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        assertTrue(PresentationPlanSalle.phraseCarte(t, service).startsWith("Pas encore de plan. Indiquez"));
        t.setNombreDeTables(12);
        assertEquals("Pas encore de plan : les 12 tables sont rangées en grille par défaut.",
                PresentationPlanSalle.phraseCarte(t, service));
        t.setSalles(List.of(new SalleTournoi("Gymnase", PlansTypesSalle.generer(2, 10, 1)),
                new SalleTournoi("Dojo", PlansTypesSalle.generer(1, 1, 14))));
        assertEquals("2 salles, 11 tables placées.", PresentationPlanSalle.phraseCarte(t, service));
        List<String> alertes = PresentationPlanSalle.alertes(service.verifier(t));
        assertEquals(List.of(
                "Les tables 11 et 12 ne sont sur aucun plan : elles s'afficheront à part dans la salle du jour J.",
                "La table 14 dépasse le nombre de tables du tournoi : elle ne recevra pas de match."), alertes);
        assertEquals(List.of(11, 12, 13), PresentationPlanSalle.tablesHorsPlan(t.getSalles(), 13));
    }

    @Test
    void chaque_motif_a_son_message() {
        PlanSalle p = PlanSalle.vide(4, 4);
        for (ResultatPlacementPlan.Motif m : ResultatPlacementPlan.Motif.values()) {
            String texte = PresentationPlanSalle.message(ResultatPlacementPlan.refus(m, p, new TablePlan(3, 0, 0)));
            assertNotEquals(m.name(), texte, "texte manquant pour " + m);
        }
        assertEquals("Cet emplacement est déjà pris par la table 3.", PresentationPlanSalle.message(
                ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, p, new TablePlan(3, 0, 0))));
        for (ResultatEditionSalles.Motif m : ResultatEditionSalles.Motif.values()) {
            assertNotEquals(m.name(), PresentationPlanSalle.message(ResultatEditionSalles.de(m, "Dojo")));
        }
    }
}
