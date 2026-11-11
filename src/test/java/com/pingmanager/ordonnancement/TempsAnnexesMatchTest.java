package com.pingmanager.ordonnancement;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TempsAnnexesMatchTest {

    @Test
    void echauffement_de_deux_minutes_et_temps_morts_d_une_minute_au_plus() {
        TempsAnnexesMatch t = TempsAnnexesMatch.PAR_DEFAUT;
        assertEquals(120, t.getEchauffement(), "3.4.3.1 : deux minutes d'adaptation à la table");
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            TempsAnnexesMatch.Tirage x = t.tirer(r, 5);
            assertTrue(x.tempsMorts() <= 2 * 60, "3.4.4.2 : un temps mort d'une minute au plus par joueur");
            assertTrue(x.pauses() <= 4 * 60, "3.4.4.1.1 : une minute au plus entre deux manches");
            assertTrue(x.appel() >= t.getAppel());
            assertEquals(x.appel() + x.echauffement() + x.pauses() + x.tempsMorts() + x.feuille(), x.total());
        }
    }

    @Test
    void la_moyenne_tiree_correspond_au_reglage_et_s_ajoute_a_chaque_match() {
        TempsAnnexesMatch t = TempsAnnexesMatch.PAR_DEFAUT;
        Random r = new Random(2);
        double somme = 0;
        int n = 100_000;
        int rappels = 0;
        for (int i = 0; i < n; i++) {
            TempsAnnexesMatch.Tirage x = t.tirer(r, 4);
            somme += x.total();
            if (x.appel() > t.getAppel()) {
                rappels++;
            }
        }
        assertEquals(t.moyenne(4), somme / n, 3.0);
        // 15 % par joueur : un match sur quatre environ demande au moins un rappel.
        assertEquals(1 - 0.85 * 0.85, (double) rappels / n, 0.01);
        assertTrue(t.moyenne(4) > 5 * 60, "plus de cinq minutes autour d'un match en 4 manches");
    }

    @Test
    void reglages_hors_regles_refuses() {
        assertThrows(IllegalArgumentException.class, () -> TempsAnnexesMatch.builder().pauseEntreManches(90));
        assertThrows(IllegalArgumentException.class, () -> TempsAnnexesMatch.builder().probabiliteRappel(1.5));
        assertThrows(IllegalArgumentException.class, () -> TempsAnnexesMatch.builder().echauffement(-1));
        assertEquals(0, TempsAnnexesMatch.AUCUN.tirer(new Random(3), 5).total());
    }
}
