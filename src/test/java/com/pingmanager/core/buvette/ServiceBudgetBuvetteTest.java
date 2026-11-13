package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.Produit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceBudgetBuvetteTest {

    private static final LocalDate J = LocalDate.of(2027, 1, 9);
    private static final HypothesesConsommation S = new HypothesesConsommation(30, 15, 75, 125, Map.of("cafe", 35));
    private final ServiceBudgetBuvette service = new ServiceBudgetBuvette();

    @Test
    void joueurs_saisis_puis_vide_revient_a_l_estimation() {
        BuvetteTournoi b = new BuvetteTournoi();
        assertTrue(service.saisirJoueurs(b, J, "120").accepte());
        assertEquals(120, b.getBudget().joueursSaisis().get(J));
        assertEquals(ResultatBudgetBuvette.Motif.NOMBRE_INVALIDE, service.saisirJoueurs(b, J, "beaucoup").motif());
        assertEquals(ResultatBudgetBuvette.Motif.NOMBRE_INVALIDE, service.saisirJoueurs(b, J, "6000").motif());
        service.saisirJoueurs(b, J, " ");
        assertTrue(b.getBudget().joueursSaisis().isEmpty());
        assertTrue(b.estVide());
    }

    @Test
    void hypotheses_controlees_et_consommations_gardees() {
        BuvetteTournoi b = new BuvetteTournoi();
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerProduit(Produit.builder().id("cafe").nom("Café").prixVenteCentimes(100).build());
        assertTrue(service.saisirConsommation(b, S, c, "cafe", "0,5").accepte());
        assertEquals(Map.of("cafe", 50), b.getBudget().hypotheses().consommations());
        assertEquals(30, b.getBudget().hypotheses().accompagnantsPourcent(), "base : les suggestions");
        assertTrue(service.saisirHypotheses(b, "20", "2", "80", "120").accepte());
        HypothesesConsommation h = b.getBudget().hypotheses();
        assertEquals(20, h.accompagnantsPourcent());
        assertEquals(20, h.presenceDixiemes());
        assertEquals(Map.of("cafe", 50), h.consommations());
        assertEquals(ResultatBudgetBuvette.Motif.POURCENT_INVALIDE,
                service.saisirHypotheses(b, "-1", "1", "80", "120").motif());
        assertEquals(ResultatBudgetBuvette.Motif.PRESENCE_INVALIDE,
                service.saisirHypotheses(b, "20", "0", "80", "120").motif());
        assertEquals(ResultatBudgetBuvette.Motif.PRESENCE_INVALIDE,
                service.saisirHypotheses(b, "20", "1,55", "80", "120").motif());
        assertEquals(ResultatBudgetBuvette.Motif.SCENARIOS_INVALIDES,
                service.saisirHypotheses(b, "20", "1", "120", "80").motif());
        assertEquals(ResultatBudgetBuvette.Motif.CONSOMMATION_INVALIDE,
                service.saisirConsommation(b, S, c, "cafe", "0,355").motif());
        assertEquals(ResultatBudgetBuvette.Motif.PRODUIT_INCONNU,
                service.saisirConsommation(b, S, c, "biere", "1").motif());
        service.revenirAuxSuggestions(b);
        assertNull(b.getBudget().hypotheses());
    }

    @Test
    void deduction_du_stock_et_oubli_des_quantites_reprises() {
        BuvetteTournoi b = new BuvetteTournoi();
        service.deduireStock(b, false);
        assertFalse(b.getBudget().deduireStock());
        b.setBudget(b.getBudget().avecQuantitesImposees(Map.of("cafe", 3L), "Open"));
        service.oublierQuantitesReprises(b);
        assertTrue(b.getBudget().quantitesImposees().isEmpty());
        assertEquals("", b.getBudget().origineQuantites());
    }

    @Test
    void nombres_decimaux_sans_virgule_flottante() {
        assertEquals(15, NombresBuvette.lire("1,5", 1).getAsLong());
        assertEquals(35, NombresBuvette.lire("0.35", 2).getAsLong());
        assertTrue(NombresBuvette.lire("1,55", 1).isEmpty());
        assertEquals("1,5", NombresBuvette.ecrire(15, 1));
        assertEquals("0,35", NombresBuvette.ecrire(35, 2));
        assertEquals("1", NombresBuvette.ecrire(100, 2));
    }
}
