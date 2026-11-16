package com.pingmanager.core.buvette;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.NatureMouvement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceStockBuvetteTest {

    private static final Instant T = Instant.parse("2026-11-14T09:30:00Z");
    private final ServiceStockBuvette service = new ServiceStockBuvette(() -> T);

    private static long stock(StockBuvette s, String id) {
        return s.article(id).orElseThrow().getStock();
    }

    @Test
    void sortie_d_un_cafe_decremente_le_produit_et_ses_contenants() {
        StockBuvette s = StockExemple.cafe();
        ResultatMouvementStock r = service.sortir(s, "cafe", 1, "Bénévole");
        assertTrue(r.accepte());
        assertEquals(199, stock(s, "cafe"));
        assertEquals(149, stock(s, "gobelet"));
        assertEquals(499, stock(s, "touillette"));
        assertEquals(3, r.mouvements().size());
        assertEquals("Café : 1 portion sortie, avec 2 contenants.", r.explication());
    }

    @Test
    void sortie_de_plusieurs_portions_multiplie_les_contenants() {
        StockBuvette s = StockExemple.cafe();
        s.enregistrer(s.article("cafe").orElseThrow().modifier()
                .contenants(List.of(new com.pingmanager.model.buvette.LienContenant("touillette", 2))).build());
        service.sortir(s, "cafe", 5, "B");
        assertEquals(195, stock(s, "cafe"));
        assertEquals(150, stock(s, "gobelet"), "le gobelet n'est plus lié");
        assertEquals(490, stock(s, "touillette"));
    }

    @Test
    void sortie_d_un_contenant_seul_ne_touche_pas_les_produits() {
        StockBuvette s = StockExemple.cafe();
        service.sortir(s, "gobelet", 10, "B");
        assertEquals(140, stock(s, "gobelet"));
        assertEquals(200, stock(s, "cafe"));
    }

    @Test
    void entree_d_un_produit_ne_touche_pas_ses_contenants() {
        StockBuvette s = StockExemple.cafe();
        ResultatMouvementStock r = service.entrer(s, "cafe", 125, "B");
        assertTrue(r.accepte());
        assertEquals(325, stock(s, "cafe"));
        assertEquals(150, stock(s, "gobelet"));
        assertEquals("Café : +125 (325 en stock).", r.explication());
    }

    @Test
    void recomptage_fixe_le_stock_et_journalise_l_ecart() {
        StockBuvette s = StockExemple.cafe();
        ResultatMouvementStock r = service.recompter(s, "gobelet", "120", "B");
        assertTrue(r.accepte());
        assertEquals(120, stock(s, "gobelet"));
        MouvementStock m = r.mouvements().get(0);
        assertEquals(NatureMouvement.RECOMPTAGE, m.nature());
        assertEquals(-30, m.variation());
        assertEquals("Gobelet recompté : 120 en stock (écart de -30).", r.explication());
    }

    @Test
    void stock_insuffisant_ramene_a_zero_et_demande_de_recompter() {
        StockBuvette s = StockExemple.cafe();
        service.recompter(s, "gobelet", 1, "B");
        ResultatMouvementStock r = service.sortir(s, "cafe", 3, "B");
        assertTrue(r.accepte());
        assertEquals(0, stock(s, "gobelet"));
        assertEquals(197, stock(s, "cafe"));
        assertEquals(List.of("Gobelet"), r.epuises());
        assertEquals(-1, r.mouvements().get(1).variation(), "seule la quantité réellement présente sort");
        assertTrue(r.explication().contains("pensez à recompter"));
    }

    @Test
    void saisies_invalides_refusees_sans_rien_changer() {
        StockBuvette s = StockExemple.cafe();
        assertEquals(ResultatMouvementStock.Motif.QUANTITE_INVALIDE, service.sortir(s, "cafe", 0, "B").motif());
        assertEquals(ResultatMouvementStock.Motif.QUANTITE_INVALIDE, service.entrer(s, "cafe", -2, "B").motif());
        assertEquals(ResultatMouvementStock.Motif.INCONNU, service.sortir(s, "biere", 1, "B").motif());
        assertEquals(ResultatMouvementStock.Motif.COMPTE_INVALIDE, service.recompter(s, "cafe", "dix", "B").motif());
        assertEquals(ResultatMouvementStock.Motif.COMPTE_INVALIDE, service.recompter(s, "cafe", "", "B").motif());
        assertFalse(service.sortir(s, "cafe", 0, "B").accepte());
        assertEquals(200, stock(s, "cafe"));
        assertTrue(s.getJournal().isEmpty());
    }

    @Test
    void journal_une_operation_par_action_et_origine_saisie_manuelle() {
        StockBuvette s = StockExemple.cafe();
        service.sortir(s, "cafe", 1, "Claire");
        service.entrer(s, "gobelet", 50, "Claire");
        List<MouvementStock> j = s.getJournal();
        assertEquals(4, j.size());
        assertEquals(1, j.get(0).operation());
        assertEquals(1, j.get(2).operation(), "le café et ses contenants forment une seule opération");
        assertEquals(2, j.get(3).operation());
        assertEquals(OrigineMouvement.SAISIE_MANUELLE, j.get(0).origine());
        assertEquals("", j.get(0).reference());
        assertEquals(T, j.get(0).horodatage());
        assertEquals("Claire", j.get(0).auteur());
        assertEquals(199, j.get(0).stockApres());
    }

    @Test
    void sortie_garde_l_origine_et_la_reference_pour_une_future_commande() {
        StockBuvette s = StockExemple.cafe();
        ResultatMouvementStock r = service.sortir(s, "cafe", 2, OrigineMouvement.SAISIE_MANUELLE, "ticket-12", "B");
        assertTrue(r.mouvements().stream().allMatch(m -> m.reference().equals("ticket-12")));
    }

    @Test
    void journal_plafonne_aux_dernieres_lignes() {
        StockBuvette s = StockExemple.cafe();
        ServiceStockBuvette srv = new ServiceStockBuvette(Horloge.systeme());
        for (int i = 0; i < StockBuvette.JOURNAL_MAX + 10; i++) {
            srv.entrer(s, "gobelet", 1, "B");
        }
        assertEquals(StockBuvette.JOURNAL_MAX, s.getJournal().size());
        assertEquals(StockBuvette.JOURNAL_MAX + 11, s.prochaineOperation());
    }
}
