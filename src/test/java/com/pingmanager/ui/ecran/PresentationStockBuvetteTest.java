package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.NiveauStock;
import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationStockBuvetteTest {

    private static StockBuvette stock() {
        StockBuvette s = new StockBuvette("08910012");
        s.enregistrer(ArticleBuvette.builder().id("gobelet").nom("Gobelet").famille(FamilleArticle.CONTENANT)
                .unite("gobelet").lot(450, 100).stock(30).seuilAlerte(50).build());
        s.enregistrer(ArticleBuvette.builder().id("touillette").nom("Touillette").famille(FamilleArticle.CONTENANT)
                .unite("touillette").lot(300, 1000).stock(150).seuilAlerte(100).build());
        s.enregistrer(ArticleBuvette.builder().id("cafe").nom("Café").unite("dose").lot(800, 125).stock(200)
                .seuilAlerte(40).contenants(List.of(new LienContenant("gobelet", 1), new LienContenant("touillette", 1)))
                .build());
        return s;
    }

    @Test
    void jauges_vert_orange_rouge() {
        PresentationStockBuvette p = new PresentationStockBuvette(stock());
        List<PresentationStockBuvette.Section> sections = p.sections();
        assertEquals("Produits", sections.get(0).titre());
        assertEquals("Contenants et consommables", sections.get(1).titre());
        PresentationStockBuvette.LigneArticle cafe = sections.get(0).lignes().get(0);
        assertEquals("jauge-stock-vert", cafe.classeJauge());
        assertEquals("200 doses", cafe.stock());
        PresentationStockBuvette.LigneArticle gobelet = sections.get(1).lignes().get(0);
        assertEquals(NiveauStock.URGENT, gobelet.niveau());
        assertEquals("jauge-stock-rouge", gobelet.classeJauge());
        assertEquals("jauge-stock-orange", sections.get(1).lignes().get(1).classeJauge());
        assertEquals(StatutPastille.ATTENTION, PresentationStockBuvette.statut(NiveauStock.BAS));
    }

    @Test
    void detail_du_produit_contenants_et_couts_separes() {
        PresentationStockBuvette p = new PresentationStockBuvette(stock());
        PresentationStockBuvette.LigneArticle cafe = p.sections().get(0).lignes().get(0);
        assertEquals("Seuil d'alerte 40 · Par portion : 1 gobelet, 1 touillette · "
                + "Produit 0,064 € + emballage 0,048 € = 0,112 € la portion", cafe.detail());
        PresentationStockBuvette.LigneArticle gobelet = p.sections().get(1).lignes().get(0);
        assertEquals("Seuil d'alerte 50 · 0,045 € l'unité · Utilisé par Café", gobelet.detail());
        assertFalse(gobelet.retirable());
        assertTrue(cafe.retirable());
    }

    @Test
    void liste_a_racheter_en_tete() {
        PresentationStockBuvette p = new PresentationStockBuvette(stock());
        List<PresentationStockBuvette.LigneARacheter> l = p.aRacheter();
        assertEquals(1, l.size());
        assertEquals("Gobelet", l.get(0).nom());
        assertEquals("Il reste 30 gobelets · seuil d'alerte 50", l.get(0).detail());
    }

    @Test
    void derniers_mouvements_du_plus_recent_au_plus_ancien() {
        StockBuvette s = stock();
        ServiceStockBuvette srv = new ServiceStockBuvette(() -> Instant.parse("2026-11-14T09:30:00Z"));
        srv.sortir(s, "cafe", 1, "Claire");
        srv.entrer(s, "gobelet", 100, "Claire");
        List<String> m = new PresentationStockBuvette(s).derniersMouvements(2, ZoneOffset.UTC);
        assertEquals(List.of("14/11 09:30 · Entrée · Gobelet +100 (reste 129) · Claire",
                "14/11 09:30 · Sortie · Touillette -1 (reste 149) · Claire"), m);
    }

    @Test
    void stock_vide_et_pluriel_des_unites() {
        PresentationStockBuvette p = new PresentationStockBuvette(new StockBuvette("1"));
        assertTrue(p.vide());
        assertTrue(p.aRacheter().isEmpty());
        assertEquals(2, p.sections().size());
        assertEquals("1 dose", PresentationStockBuvette.quantite(1, "dose"));
        assertEquals("0 dose", PresentationStockBuvette.quantite(0, "dose"));
        assertEquals("3 gâteaux", PresentationStockBuvette.quantite(3, "gâteaux"));
    }
}
