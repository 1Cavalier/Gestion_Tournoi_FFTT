package com.pingmanager.model.buvette;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleBuvetteTest {

    @Test
    void un_contenant_cite_deux_fois_cumule_ses_quantites() {
        ArticleBuvette a = ArticleBuvette.builder().id("cafe").nom("Café").contenants(List.of(
                new LienContenant("sucre", 1), new LienContenant("gobelet", 1), new LienContenant("sucre", 2)))
                .build();
        assertEquals(List.of(new LienContenant("sucre", 3), new LienContenant("gobelet", 1)), a.getContenants());
    }

    @Test
    void les_contenants_d_un_contenant_sont_ignores() {
        ArticleBuvette a = ArticleBuvette.builder().id("g").nom("Gobelet").famille(FamilleArticle.CONTENANT)
                .contenants(List.of(new LienContenant("x", 1))).build();
        assertTrue(a.getContenants().isEmpty());
    }

    @Test
    void valeurs_impossibles_refusees() {
        assertThrows(IllegalArgumentException.class, () -> ArticleBuvette.builder().id("a").nom("A").stock(-1).build());
        assertThrows(IllegalArgumentException.class, () -> ArticleBuvette.builder().id("a").nom("A").lot(100, 0).build());
        assertThrows(IllegalArgumentException.class, () -> ArticleBuvette.builder().id("a").nom(" ").build());
        assertThrows(IllegalArgumentException.class, () -> new LienContenant("g", 0));
    }

    @Test
    void identifiant_libre_derive_du_nom() {
        StockBuvette s = new StockBuvette("1");
        assertEquals("cafe-creme", s.idLibre("Café crème"));
        s.enregistrer(ArticleBuvette.builder().id("cafe-creme").nom("Café crème").build());
        assertEquals("cafe-creme-2", s.idLibre("café CRÈME"));
        assertEquals("article", s.idLibre("!!"));
    }
}
