package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JaugeStockTest {

    private static ArticleBuvette article(long stock, long seuil) {
        return ArticleBuvette.builder().id("a").nom("A").stock(stock).seuilAlerte(seuil).build();
    }

    @Test
    void niveau_change_aux_seuils_vert_orange_rouge() {
        // seuil 20 : rouge jusqu'à 20, orange jusqu'à 40, vert au-delà
        assertEquals(NiveauStock.SUFFISANT, JaugeStock.de(article(41, 20)).niveau());
        assertEquals(NiveauStock.BAS, JaugeStock.de(article(40, 20)).niveau());
        assertEquals(NiveauStock.BAS, JaugeStock.de(article(21, 20)).niveau());
        assertEquals(NiveauStock.URGENT, JaugeStock.de(article(20, 20)).niveau());
        assertEquals(NiveauStock.URGENT, JaugeStock.de(article(0, 20)).niveau());
    }

    @Test
    void sans_seuil_rouge_seulement_quand_le_stock_est_vide() {
        assertEquals(NiveauStock.SUFFISANT, JaugeStock.de(article(1, 0)).niveau());
        assertEquals(NiveauStock.URGENT, JaugeStock.de(article(0, 0)).niveau());
        assertEquals(1.0, JaugeStock.de(article(1, 0)).remplissage());
        assertEquals(0.0, JaugeStock.de(article(0, 0)).remplissage());
    }

    @Test
    void remplissage_plein_a_trois_fois_le_seuil() {
        assertEquals(1.0, JaugeStock.de(article(90, 20)).remplissage());
        assertEquals(0.5, JaugeStock.de(article(30, 20)).remplissage(), 1e-9);
        assertEquals(0.0, JaugeStock.de(article(0, 20)).remplissage());
    }

    @Test
    void une_sortie_fait_passer_la_jauge_au_rouge() {
        StockBuvette s = StockExemple.cafe(); // gobelet : 150, seuil 50
        ServiceStockBuvette srv = new ServiceStockBuvette(() -> java.time.Instant.EPOCH);
        assertEquals(NiveauStock.SUFFISANT, JaugeStock.de(s.article("gobelet").orElseThrow()).niveau());
        srv.sortir(s, "cafe", 50, "B"); // reste 100 gobelets
        assertEquals(NiveauStock.BAS, JaugeStock.de(s.article("gobelet").orElseThrow()).niveau());
        srv.sortir(s, "cafe", 50, "B"); // reste 50 gobelets = seuil
        assertEquals(NiveauStock.URGENT, JaugeStock.de(s.article("gobelet").orElseThrow()).niveau());
        assertEquals(List.of("Gobelet"), JaugeStock.aRacheter(s).stream().map(j -> j.article().getNom()).toList());
    }

    @Test
    void liste_a_racheter_les_plus_vides_d_abord() {
        StockBuvette s = new StockBuvette("1");
        s.enregistrer(ArticleBuvette.builder().id("a").nom("Assiettes").stock(10).seuilAlerte(10).build());
        s.enregistrer(ArticleBuvette.builder().id("b").nom("Biscuits").stock(1).seuilAlerte(10).build());
        s.enregistrer(ArticleBuvette.builder().id("c").nom("Café").stock(100).seuilAlerte(10).build());
        s.enregistrer(ArticleBuvette.builder().id("d").nom("Dosettes").stock(0).seuilAlerte(0).build());
        assertEquals(List.of("Dosettes", "Biscuits", "Assiettes"),
                JaugeStock.aRacheter(s).stream().map(j -> j.article().getNom()).toList());
    }
}
