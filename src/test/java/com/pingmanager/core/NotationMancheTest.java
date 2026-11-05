package com.pingmanager.core;

import com.pingmanager.model.Manche;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotationMancheTest {

    @Test
    void notation_complete_avec_plusieurs_separateurs() {
        assertEquals(new Manche(11, 9), NotationManche.lire("11-9").orElseThrow());
        assertEquals(new Manche(9, 11), NotationManche.lire(" 9 / 11 ").orElseThrow());
        assertEquals(new Manche(13, 11), NotationManche.lire("13:11").orElseThrow());
        assertEquals(new Manche(11, 4), NotationManche.lire("11 4").orElseThrow());
    }

    @Test
    void notation_courte_points_du_perdant_signe_moins_si_b_gagne() {
        // 2.11.1 : 11 points, ou 2 points d'écart après 10-10.
        assertEquals(new Manche(11, 9), NotationManche.lire("9").orElseThrow());
        assertEquals(new Manche(11, 9), NotationManche.lire("+9").orElseThrow());
        assertEquals(new Manche(9, 11), NotationManche.lire("-9").orElseThrow());
        assertEquals(new Manche(14, 12), NotationManche.lire("12").orElseThrow());
        assertEquals(new Manche(10, 12), NotationManche.lire("-10").orElseThrow());
        assertEquals(new Manche(11, 0), NotationManche.lire("0").orElseThrow());
        assertEquals(new Manche(0, 11), NotationManche.lire("-0").orElseThrow());
    }

    @Test
    void saisie_vide_ou_illisible_refusee() {
        assertTrue(NotationManche.lire(null).isEmpty());
        assertTrue(NotationManche.lire("  ").isEmpty());
        assertTrue(NotationManche.lire("onze").isEmpty());
        assertTrue(NotationManche.lire("11-").isEmpty());
        assertTrue(NotationManche.lire("--9").isEmpty());
    }

    @Test
    void ecriture_complete_et_courte() {
        assertEquals("11-9", NotationManche.ecrire(new Manche(11, 9)));
        assertEquals("9", NotationManche.ecrireCourt(new Manche(11, 9)));
        assertEquals("-12", NotationManche.ecrireCourt(new Manche(12, 14)));
        assertEquals("5-5", NotationManche.ecrireCourt(new Manche(5, 5)));
    }
}
