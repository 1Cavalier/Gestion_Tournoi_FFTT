package com.pingmanager.core.buvette;

import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BudgetBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.TypeMouvementStock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculBilanBuvetteTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 1, 9);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final HypothesesConsommation H = new HypothesesConsommation(0, 10, 75, 125, Map.of("soda", 50));

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("soda").nom("Soda").rendement(24).prixAchatCentimes(1200)
                .build());
        c.enregistrerProduit(Produit.builder().id("soda").nom("Soda").prixVenteCentimes(150)
                .recette(List.of(new LigneRecette("soda", 1))).build());
        c.enregistrerProduit(Produit.builder().id("cafe").nom("Café").prixVenteCentimes(100).build());
        return c;
    }

    @Test
    void prevu_contre_reel_ventes_achats_pertes_et_stock_restant() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        b.setBudget(BudgetBuvette.VIDE.avecJoueurs(SAMEDI, 100));
        ServiceStockBuvette stock = new ServiceStockBuvette(() -> Instant.parse("2027-01-09T08:00:00Z"));
        ServiceVentesBuvette caisse = new ServiceVentesBuvette(() -> Instant.parse("2027-01-09T10:00:00Z"));
        stock.acheter(b, c, "soda", 2, 2400, SAMEDI, "A");                    // 48 canettes, 24 €
        stock.sortie(b, c, TypeMouvementStock.PERTE, "soda", 2, "A", "");    // 2 canettes perdues
        stock.sortie(b, c, TypeMouvementStock.DON, "soda", 1, "A", "");
        stock.corriger(b, 3, "A", null);                                       // le don était une erreur
        for (int i = 0; i < 30; i++) {
            caisse.vendre(b, c, "soda", "A", null);
        }
        caisse.vendre(b, c, "cafe", "A", null);
        BilanBuvette bilan = new CalculBilanBuvette().bilan(new Tournoi("Open", "Club", List.of(SAMEDI)), b, c, H,
                PARIS);
        assertEquals("2026-2027", bilan.saison());
        assertEquals(7500, bilan.recettePrevue(), "100 × 0,5 = 50 sodas à 1,50 €");
        assertEquals(3600, bilan.achatsPrevus(), "50 canettes → 3 cartons");
        assertEquals(30 * 150 + 100, bilan.recetteReelle());
        assertEquals(2400, bilan.achatsReels());
        assertEquals(4600 - 2400, bilan.margeReelle());
        assertEquals(31, bilan.articles());
        assertEquals(30 * 50, bilan.coutDesVentes(), "le café sans recette n'a pas de coût");
        assertEquals(List.of(Map.entry("Soda", 30L), Map.entry("Café", 1L)), bilan.plusVendus());
        assertEquals(2, bilan.pertesPortions(), "le don corrigé ne compte plus");
        assertEquals(100, bilan.pertesCentimes());
        assertEquals(16 * 50, bilan.stockRestantCentimes(), "48 − 2 − 30 = 16 canettes à 0,50 €");
        assertEquals(Map.of("soda", 30L, "cafe", 1L), bilan.ventesParProduit());
        assertTrue(bilan.aDesDonnees());
    }

    @Test
    void tournoi_sans_buvette_bilan_vide() {
        BilanBuvette bilan = new CalculBilanBuvette().bilan(new Tournoi("Open", "Club", List.of(SAMEDI)),
                new BuvetteTournoi(), catalogue(), H, PARIS);
        assertFalse(bilan.aDesDonnees());
        assertEquals(0, bilan.margeReelle());
        assertTrue(bilan.plusVendus().isEmpty());
    }

    @Test
    void reprendre_les_quantites_d_un_ancien_tournoi_preremplit_le_budget() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi ancien = new BuvetteTournoi();
        ServiceVentesBuvette caisse = new ServiceVentesBuvette(() -> Instant.parse("2027-01-09T10:00:00Z"));
        for (int i = 0; i < 12; i++) {
            caisse.vendre(ancien, c, "cafe", "A", null);
        }
        BilanBuvette bilan = new CalculBilanBuvette().bilan(new Tournoi("Open 2026", "Club", List.of(SAMEDI)),
                ancien, c, H, PARIS);
        BuvetteTournoi suivant = new BuvetteTournoi();
        ServiceBudgetBuvette budget = new ServiceBudgetBuvette();
        assertEquals(ResultatBudgetBuvette.Motif.QUANTITES_REPRISES, budget.reprendreQuantites(suivant, bilan).motif());
        assertEquals(Map.of("cafe", 12L), suivant.getBudget().quantitesImposees());
        assertEquals("Open 2026", suivant.getBudget().origineQuantites());
        BudgetCalcule r = new CalculBudgetBuvette().calculer(new Tournoi("Open 2027", "Club",
                List.of(SAMEDI.plusYears(1))), suivant, c, H);
        assertEquals(12, r.moyen().produits().stream().filter(p -> p.idProduit().equals("cafe")).findFirst()
                .orElseThrow().quantite());
        BilanBuvette vide = new CalculBilanBuvette().bilan(new Tournoi("Vide", "Club", List.of(SAMEDI)),
                new BuvetteTournoi(), c, H, PARIS);
        assertEquals(ResultatBudgetBuvette.Motif.RIEN_A_REPRENDRE, budget.reprendreQuantites(suivant, vide).motif());
    }

    @Test
    void export_csv_du_bilan_avec_totaux() {
        BilanBuvette a = new BilanBuvette("Open", SAMEDI, "2026-2027", 7500, 3600, 4600, 2400, 1500, 31,
                List.of(Map.entry("Soda", 30L)), 2, 100, 800, Map.of("soda", 30L));
        BilanBuvette b = new BilanBuvette("=Piège", SAMEDI, "2026-2027", 0, 0, 0, 500, 0, 0, List.of(), 0, 0, 0,
                Map.of());
        String csv = ExportCsvBuvette.bilan("2026-2027", List.of(a, b));
        assertTrue(csv.contains("Open;2027-01-09;2026-2027;75,00;46,00;36,00;24,00;39,00;22,00;31;1,00;8,00;Soda (30)"));
        assertTrue(csv.contains("'=Piège;"), "pas d'injection de formule");
        assertTrue(csv.contains("Total;;;75,00;46,00;36,00;29,00;39,00;17,00;31;1,00;8,00;"));
    }
}
