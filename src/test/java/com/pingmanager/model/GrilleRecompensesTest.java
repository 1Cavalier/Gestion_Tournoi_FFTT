package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrilleRecompensesTest {

    @Test
    void grille_vide_ne_dote_personne() {
        GrilleRecompenses g = GrilleRecompenses.vide();
        assertTrue(g.estVide());
        assertEquals(0, g.nombreDePlacesDotees());
        assertNull(g.recompensePour(1));
    }

    @Test
    void dotation_aux_quatre_premiers_avec_plage() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.argent(150))
                .place(2, Recompense.argent(80))
                .plage(3, 4, Recompense.argent(40))
                .build();

        assertEquals(4, g.nombreDePlacesDotees());
        assertEquals(TypeRecompense.ARGENT, g.recompensePour(1).getType());
        assertEquals(150.0, g.recompensePour(1).getMontant());
        assertEquals(80.0, g.recompensePour(2).getMontant());
        assertEquals(40.0, g.recompensePour(3).getMontant());
        assertEquals(40.0, g.recompensePour(4).getMontant());
        assertNull(g.recompensePour(5));
    }

    @Test
    void nombre_de_places_libre_deux_premiers() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.argent(100))
                .place(2, Recompense.argent(50))
                .build();
        assertEquals(2, g.nombreDePlacesDotees());
    }

    @Test
    void nombre_de_places_libre_jusqu_a_huit() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.argent(200))
                .place(2, Recompense.argent(120))
                .plage(3, 4, Recompense.argent(60))
                .plage(5, 8, Recompense.argent(30))
                .build();
        assertEquals(8, g.nombreDePlacesDotees());
        assertEquals(30.0, g.recompensePour(8).getMontant());
    }

    @Test
    void trou_dans_la_grille_est_refuse() {
        // place 1 puis 3 : la place 2 manque
        assertThrows(IllegalArgumentException.class, () ->
                GrilleRecompenses.builder()
                        .place(1, Recompense.argent(100))
                        .place(3, Recompense.argent(40))
                        .build());
    }

    @Test
    void chevauchement_est_refuse() {
        assertThrows(IllegalArgumentException.class, () ->
                GrilleRecompenses.builder()
                        .plage(1, 3, Recompense.argent(50))
                        .plage(3, 4, Recompense.argent(20))
                        .build());
    }

    @Test
    void ne_commence_pas_a_la_place_un_est_refuse() {
        assertThrows(IllegalArgumentException.class, () ->
                GrilleRecompenses.builder()
                        .place(2, Recompense.argent(50))
                        .build());
    }

    @Test
    void types_de_recompense_varies() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.argent(100))
                .place(2, Recompense.reductionPourcentage(50))
                .place(3, Recompense.autre("Coupe + lot"))
                .build();
        assertEquals(TypeRecompense.ARGENT, g.recompensePour(1).getType());
        assertEquals(TypeRecompense.REDUCTION_POURCENTAGE, g.recompensePour(2).getType());
        assertEquals(50.0, g.recompensePour(2).getPourcentage());
        assertEquals(TypeRecompense.AUTRE, g.recompensePour(3).getType());
        assertEquals("Coupe + lot", g.recompensePour(3).getLibelle());
    }

    @Test
    void recompense_argent_negative_refusee() {
        assertThrows(IllegalArgumentException.class, () -> Recompense.argent(-10));
    }

    @Test
    void pourcentage_hors_bornes_refuse() {
        assertThrows(IllegalArgumentException.class, () -> Recompense.reductionPourcentage(150));
    }
}
