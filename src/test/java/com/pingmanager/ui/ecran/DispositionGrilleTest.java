package com.pingmanager.ui.ecran;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispositionGrilleTest {

    @Test
    void quatre_colonnes_quand_la_place_le_permet() {
        // 4 × 260 + 3 × 20 = 1100
        assertEquals(4, DispositionGrille.colonnes(1100, 20, 4, 260));
        assertEquals(4, DispositionGrille.colonnes(1800, 20, 4, 260));
    }

    @Test
    void quatre_puis_deux_puis_une_jamais_trois() {
        assertEquals(2, DispositionGrille.colonnes(1099, 20, 4, 260), "trois colonnes tiendraient, mais 4 → 2");
        assertEquals(2, DispositionGrille.colonnes(540, 20, 4, 260));
        assertEquals(1, DispositionGrille.colonnes(539, 20, 4, 260));
    }

    @Test
    void trois_colonnes_passent_directement_a_une() {
        // 3 × 300 + 2 × 20 = 940
        assertEquals(3, DispositionGrille.colonnes(940, 20, 3, 300));
        assertEquals(1, DispositionGrille.colonnes(939, 20, 3, 300), "pas de rangée bancale 2 + 1");
    }

    @Test
    void largeur_inconnue_ou_maximum_degenere_donne_une_colonne() {
        assertEquals(1, DispositionGrille.colonnes(0, 20, 4, 260));
        assertEquals(1, DispositionGrille.colonnes(2000, 20, 0, 260));
    }
}
