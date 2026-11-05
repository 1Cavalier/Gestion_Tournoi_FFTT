package com.pingmanager.ui.ecran;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarsTest {

    @Test
    void initiales_prenom_puis_nom_en_majuscules() {
        assertEquals("QS", Avatars.initiales("Quentin", "SOUMET"));
        assertEquals("ÉL", Avatars.initiales("élodie", "lemoine"));
    }

    @Test
    void initiales_ignorent_les_espaces_et_signes_en_tete() {
        assertEquals("JD", Avatars.initiales("  Jean-Pierre", "'d'Arc"));
    }

    @Test
    void initiales_avec_une_seule_partie_connue_donnent_une_lettre() {
        assertEquals("M", Avatars.initiales(null, "Martin"));
        assertEquals("P", Avatars.initiales("Paul", "  "));
    }

    @Test
    void initiales_inconnues_donnent_un_point_d_interrogation() {
        assertEquals("?", Avatars.initiales(null, null));
        assertEquals("?", Avatars.initiales("", "123"));
    }

    @Test
    void teinte_stable_et_dans_la_gamme() {
        int t = Avatars.teinte("Quentin", "Soumet");
        assertEquals(t, Avatars.teinte(" quentin ", "SOUMET"));
        for (String nom : new String[]{"A", "Bernard", "Céline", "Durand", "Élise", "", null}) {
            int x = Avatars.teinte(nom, nom);
            assertTrue(x >= 1 && x <= Avatars.NOMBRE_TEINTES, "teinte hors gamme : " + x);
        }
    }
}
