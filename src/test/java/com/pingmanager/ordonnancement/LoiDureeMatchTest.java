package com.pingmanager.ordonnancement;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LoiDureeMatchTest {

    private static Duration min(long m) {
        return Duration.ofMinutes(m);
    }

    @Test
    void les_durees_tirees_restent_entre_le_minimum_et_le_maximum_et_ont_la_bonne_moyenne() {
        LoiDureeMatch loi = new LoiDureeMatch(min(20), min(10), min(50));
        Random r = new Random(3);
        double somme = 0;
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            Duration d = loi.tirer(r);
            assertTrue(d.compareTo(min(10)) >= 0 && d.compareTo(min(50)) <= 0, "hors bornes : " + d);
            somme += d.toSeconds() / 60.0;
        }
        assertEquals(20.0, somme / n, 0.1, "moyenne exacte malgré la troncature au maximum");
    }

    @Test
    void les_matchs_longs_sont_rares() {
        LoiDureeMatch loi = new LoiDureeMatch(min(20), min(10), min(50));
        // Médiane sous la moyenne (queue vers les durées longues).
        assertTrue(loi.quantileMinutes(0.5) < 20);
        // Moins d'un match sur dix dépasse 35 min.
        assertTrue(loi.quantileMinutes(0.9) < 35);
        assertEquals(10.0, loi.quantileMinutes(0.0), 1e-9);
        double avant = 0;
        for (double u = 0; u < 1; u += 0.01) {
            double q = loi.quantileMinutes(u);
            assertTrue(q >= avant, "durée croissante avec le quantile");
            avant = q;
        }
    }

    @Test
    void moyenne_trop_proche_du_maximum_ou_bornes_incoherentes_refusees() {
        assertThrows(IllegalArgumentException.class, () -> new LoiDureeMatch(min(30), min(10), min(50)));
        assertThrows(IllegalArgumentException.class, () -> new LoiDureeMatch(min(10), min(10), min(50)));
        assertThrows(IllegalArgumentException.class, () -> new LoiDureeMatch(min(20), min(0), min(50)));
        assertThrows(IllegalArgumentException.class, () -> new LoiDureeMatch(min(20), min(10), min(15)));
    }

    @Test
    void mise_a_l_echelle_pour_un_match_en_sept_manches() {
        LoiDureeMatch loi = LoiDureeMatch.autour(min(20)).echelle(4 / 3.0);
        assertEquals(min(80).toSeconds() / 3, loi.getMoyenne().toSeconds(), 1);
        assertEquals(loi.getMoyenne().toSeconds() / 2, loi.getMinimum().toSeconds(), 1);
    }
}
