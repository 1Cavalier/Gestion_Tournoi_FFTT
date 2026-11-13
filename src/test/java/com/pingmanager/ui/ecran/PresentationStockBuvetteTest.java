package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.ResultatMouvementStock;
import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.TypeMouvementStock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationStockBuvetteTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private final ServiceStockBuvette service = new ServiceStockBuvette(() -> Instant.parse("2026-11-14T09:05:00Z"));

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("baguette").nom("Baguette").uniteAchat("baguette")
                .unitePortion("sandwich").prixAchatCentimes(110).rendement(2, 3).build());
        return c;
    }

    @Test
    void saisie_d_un_achat_par_le_formulaire_puis_ligne_et_journal() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        ResultatMouvementStock r = PresentationStockBuvette.saisir(service, b, c, TypeMouvementStock.ACHAT, "baguette",
                "10", "11,00", LocalDate.of(2026, 11, 13), "Quentin S.");
        assertTrue(r.accepte());
        b.definirSeuil("baguette", 25);
        PresentationStockBuvette p = new PresentationStockBuvette(b, c, PARIS);
        PresentationStockBuvette.LigneStock l = p.lignes().get(0);
        assertEquals("Reste 20 sandwich (≈ 10 × baguette) · départ 0 · achats 20 · ventes 0 · seuil 25", l.detail());
        assertTrue(l.sousLeSeuil());
        PresentationStockBuvette.LigneMouvement m = p.journal().get(0);
        assertEquals("n° 1 · Achat · Baguette : +20 sandwich", m.titre());
        assertEquals("14/11/2026 10:05 · Quentin S. · acheté le 13/11/2026 · 11,00 € · 10 × baguette", m.detail());
        assertTrue(m.corrigeable());
        assertEquals("1", p.tuiles().get(1).valeur());
        assertEquals("11,00 €", p.tuiles().get(2).valeur(), "20 portions × 0,55 €");
        assertEquals("11,00 €", p.tuiles().get(3).valeur());
    }

    @Test
    void saisies_illisibles_refusees_avant_le_service() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        assertEquals(ResultatMouvementStock.Motif.QUANTITE_INVALIDE, PresentationStockBuvette.saisir(service, b, c,
                TypeMouvementStock.PERTE, "baguette", "deux", "", null, "A").motif());
        assertEquals(ResultatMouvementStock.Motif.INGREDIENT_INCONNU, PresentationStockBuvette.saisir(service, b, c,
                TypeMouvementStock.PERTE, null, "2", "", null, "A").motif());
        assertEquals(ResultatMouvementStock.Motif.PRIX_INVALIDE, PresentationStockBuvette.saisir(service, b, c,
                TypeMouvementStock.ACHAT, "baguette", "2", "cher", null, "A").motif());
        assertTrue(b.getMouvements().isEmpty());
    }

    @Test
    void inventaire_et_correction_dans_le_journal_le_plus_recent_en_tete() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        PresentationStockBuvette.saisir(service, b, c, TypeMouvementStock.STOCK_DEPART, "baguette", "6", "", null, "A");
        PresentationStockBuvette.saisir(service, b, c, TypeMouvementStock.INVENTAIRE, "baguette", "4", "", null, "A");
        service.corriger(b, 1, "A", null);
        List<PresentationStockBuvette.LigneMouvement> j = new PresentationStockBuvette(b, c, PARIS).journal();
        assertEquals(List.of(3, 2, 1), j.stream().map(PresentationStockBuvette.LigneMouvement::numero).toList());
        assertFalse(j.get(0).corrigeable(), "une correction ne se corrige pas");
        assertTrue(j.get(2).corrige());
        assertEquals("n° 2 · Écart d'inventaire · Baguette : -2 sandwich", j.get(1).titre());
    }

    @Test
    void libelle_de_quantite_selon_le_mouvement() {
        assertEquals("Unités d'achat", PresentationStockBuvette.libelleQuantite(TypeMouvementStock.ACHAT));
        assertEquals("Portions comptées", PresentationStockBuvette.libelleQuantite(TypeMouvementStock.INVENTAIRE));
        assertEquals("Portions", PresentationStockBuvette.libelleQuantite(TypeMouvementStock.DON));
        assertFalse(Arrays.asList(PresentationStockBuvette.TYPES_SAISIS).contains(TypeMouvementStock.VENTE));
    }

    @Test
    void tournoi_par_defaut_le_prochain_sinon_le_plus_recent() {
        LocalDate auj = LocalDate.of(2026, 11, 13);
        assertEquals(1, PresentationStockBuvette.tournoiParDefaut(List.of(LocalDate.of(2027, 3, 1),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 5, 1)), auj));
        assertEquals(0, PresentationStockBuvette.tournoiParDefaut(List.of(auj), auj), "aujourd'hui : en cours");
        assertEquals(1, PresentationStockBuvette.tournoiParDefaut(List.of(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 1)), auj));
        assertEquals(-1, PresentationStockBuvette.tournoiParDefaut(List.of(), auj));
    }
}
