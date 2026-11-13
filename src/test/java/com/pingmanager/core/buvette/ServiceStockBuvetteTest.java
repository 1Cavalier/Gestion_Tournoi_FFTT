package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.TypeMouvementStock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceStockBuvetteTest {

    private static final Instant T = Instant.parse("2026-11-14T08:00:00Z");
    private final ServiceStockBuvette service = new ServiceStockBuvette(() -> T);

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("baguette").nom("Baguette").uniteAchat("baguette")
                .unitePortion("sandwich").prixAchatCentimes(110).rendement(2, 3).build());
        c.enregistrerIngredient(Ingredient.builder().id("soda").nom("Soda").uniteAchat("carton de 24")
                .unitePortion("canette").prixAchatCentimes(1200).rendement(24).build());
        return c;
    }

    @Test
    void achat_converti_en_portions_avec_le_rendement_minimal_et_trace() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        ResultatMouvementStock r = service.acheter(b, c, "baguette", 10, 1100, LocalDate.of(2026, 11, 14), "Quentin");
        assertTrue(r.accepte());
        MouvementStock m = r.mouvements().get(0);
        assertEquals(1, m.numero());
        assertEquals(20, m.quantite(), "10 baguettes × 2 sandwichs (rendement minimal)");
        assertEquals(1100, m.prixPayeCentimes());
        assertEquals("Quentin", m.auteur());
        assertEquals(T, m.horodatage());
        assertEquals("10 × baguette", m.commentaire());
        assertEquals(20, ServiceStockBuvette.stock(b, "baguette"));
        assertTrue(c.estIngredientUtilise("baguette"), "un ingrédient en stock ne se supprime plus");
    }

    @Test
    void depart_achats_pertes_dons_et_inventaire_de_fin() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.stockDepart(b, c, "soda", 30, "A");
        service.acheter(b, c, "soda", 2, 2400, null, "A");
        service.sortie(b, c, TypeMouvementStock.PERTE, "soda", 2, "A", "canettes percées");
        service.sortie(b, c, TypeMouvementStock.DON, "soda", 3, "A", "arbitres");
        assertEquals(73, ServiceStockBuvette.stock(b, "soda"), "30 + 48 − 2 − 3");
        ResultatMouvementStock inv = service.inventaire(b, c, "soda", 70, "A");
        assertEquals(-3, inv.mouvements().get(0).quantite(), "écart : 70 comptées − 73 théoriques");
        assertEquals(70, inv.mouvements().get(0).compte());
        EtatStock e = ServiceStockBuvette.etat(b, c).stream().filter(x -> x.ingredient().getId().equals("soda"))
                .findFirst().orElseThrow();
        assertEquals(30, e.depart());
        assertEquals(48, e.achats());
        assertEquals(5, e.pertesDons());
        assertEquals(-3, e.ecartsInventaire());
        assertEquals(70, e.restant());
        assertEquals(2400, e.achatsPayesCentimes());
        assertEquals(2, e.unitesAchatEntieres());
    }

    @Test
    void saisies_refusees_avec_motif() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        assertEquals(ResultatMouvementStock.Motif.INGREDIENT_INCONNU,
                service.acheter(b, c, "jambon", 1, 100, null, "A").motif());
        assertEquals(ResultatMouvementStock.Motif.QUANTITE_INVALIDE,
                service.acheter(b, c, "soda", 0, 100, null, "A").motif());
        assertEquals(ResultatMouvementStock.Motif.PRIX_INVALIDE,
                service.acheter(b, c, "soda", 1, -1, null, "A").motif());
        assertEquals(ResultatMouvementStock.Motif.QUANTITE_INVALIDE,
                service.sortie(b, c, TypeMouvementStock.PERTE, "soda", 0, "A", "").motif());
        assertEquals(ResultatMouvementStock.Motif.QUANTITE_INVALIDE, service.inventaire(b, c, "soda", -1, "A").motif());
        assertTrue(b.getMouvements().isEmpty());
    }

    @Test
    void correction_par_mouvement_inverse_une_seule_fois_sans_rien_effacer() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.acheter(b, c, "soda", 2, 2400, LocalDate.of(2026, 11, 14), "A");
        ResultatMouvementStock r = service.corriger(b, 1, "B", null);
        assertTrue(r.accepte());
        MouvementStock inverse = r.mouvements().get(0);
        assertEquals(TypeMouvementStock.CORRECTION, inverse.type());
        assertEquals(-48, inverse.quantite());
        assertEquals(-2400, inverse.prixPayeCentimes());
        assertEquals(1, inverse.numeroLie());
        assertEquals("annule le mouvement n° 1", inverse.commentaire());
        assertEquals(2, b.getMouvements().size(), "le mouvement d'origine reste");
        assertEquals(0, ServiceStockBuvette.stock(b, "soda"));
        assertEquals(ResultatMouvementStock.Motif.DEJA_CORRIGE, service.corriger(b, 1, "B", null).motif());
        assertEquals(ResultatMouvementStock.Motif.CORRECTION_NON_CORRIGEABLE, service.corriger(b, 2, "B", null).motif());
        assertEquals(ResultatMouvementStock.Motif.MOUVEMENT_INCONNU, service.corriger(b, 9, "B", null).motif());
    }

    @Test
    void une_vente_ne_se_corrige_pas_depuis_le_stock() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.ajouter(b, c, TypeMouvementStock.VENTE, "soda", -1, 0, null, "A", "", 1, -1);
        assertEquals(ResultatMouvementStock.Motif.VENTE_A_ANNULER_EN_CAISSE, service.corriger(b, 1, "A", null).motif());
    }

    @Test
    void seuil_d_alerte_reglable_et_retirable() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.stockDepart(b, c, "soda", 5, "A");
        service.definirSeuil(b, c, "soda", 12);
        assertTrue(etat(b, c, "soda").sousLeSeuil());
        assertFalse(etat(b, c, "baguette").sousLeSeuil(), "sans seuil, pas d'alerte");
        service.definirSeuil(b, c, "soda", 5);
        assertFalse(etat(b, c, "soda").sousLeSeuil(), "au seuil exactement : pas encore en dessous");
        service.definirSeuil(b, c, "soda", -1);
        assertEquals(-1, etat(b, c, "soda").seuil());
        assertEquals(ResultatMouvementStock.Motif.INGREDIENT_INCONNU, service.definirSeuil(b, c, "x", 3).motif());
    }

    @Test
    void report_du_stock_restant_sur_demande_une_seule_fois() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi ancien = new BuvetteTournoi();
        service.stockDepart(ancien, c, "soda", 30, "A");
        service.sortie(ancien, c, TypeMouvementStock.PERTE, "soda", 30, "A", "");
        service.acheter(ancien, c, "baguette", 3, 330, null, "A");
        BuvetteTournoi nouveau = new BuvetteTournoi();
        ResultatMouvementStock r = service.reporter(ancien, "Tournoi de mai", nouveau, c, "B");
        assertTrue(r.accepte());
        assertEquals(1, r.mouvements().size(), "le soda est épuisé : seule la baguette est reportée");
        assertEquals(6, ServiceStockBuvette.stock(nouveau, "baguette"));
        assertEquals("report de Tournoi de mai", r.mouvements().get(0).commentaire());
        assertEquals(3, ancien.getMouvements().size(), "le tournoi d'origine n'est pas modifié");
        assertEquals(ResultatMouvementStock.Motif.DEJA_REPORTE,
                service.reporter(ancien, "Tournoi de mai", nouveau, c, "B").motif());
        assertEquals(ResultatMouvementStock.Motif.RIEN_A_REPORTER,
                service.reporter(new BuvetteTournoi(), "Vide", nouveau, c, "B").motif());
    }

    @Test
    void numero_de_mouvement_deja_utilise_refuse() {
        BuvetteTournoi b = new BuvetteTournoi();
        b.ajouterMouvement(new MouvementStock(1, TypeMouvementStock.ACHAT, "soda", 24, 0, null, T, "A", "", 0, -1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> b.ajouterMouvement(
                new MouvementStock(1, TypeMouvementStock.ACHAT, "soda", 24, 0, null, T, "A", "", 0, -1)));
        assertEquals(List.of(1), b.getMouvements().stream().map(MouvementStock::numero).toList());
    }

    private static EtatStock etat(BuvetteTournoi b, CatalogueBuvette c, String id) {
        return ServiceStockBuvette.etat(b, c).stream().filter(x -> x.ingredient().getId().equals(id)).findFirst()
                .orElseThrow();
    }
}
