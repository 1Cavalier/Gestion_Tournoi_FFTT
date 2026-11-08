package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationPrononciationTest {

    @Test
    void prononciations_francaises_courantes_acceptees() {
        for (String ipa : new String[]{"dy.pɔ̃", "ʒɑ̃", "lə.fɛvʁ", "ŋɡu.jɛn", "sɛ̃.sɑ̃s", "bʁœ̃", "ʃa.ɲo", "ɥit",
                "kɑ̃.tɛ̃ su.mɛ", "pe.tʁys‿a", "ˈmɔ.ʁɛ̃", "deː", "θœ.ʁi", "øʁ.ʁø"}) {
            ResultatSaisiePrononciation r = ValidationPrononciation.verifier(ipa);
            assertTrue(r.valide(), ipa + " : " + r.motif());
            assertEquals(ipa, r.ipa());
        }
    }

    @Test
    void espaces_reduites_et_vide_vaut_effacement() {
        assertEquals("ʒɑ̃ dy.pɔ̃", ValidationPrononciation.verifier("  ʒɑ̃ \t  dy.pɔ̃ ").ipa());
        assertTrue(ValidationPrononciation.verifier("   ").estEffacement());
        assertTrue(ValidationPrononciation.verifier(null).estEffacement());
        assertFalse(ValidationPrononciation.verifier("dy").estEffacement());
    }

    @Test
    void caracteres_qui_casseraient_le_ssml_refuses() {
        for (String ipa : new String[]{"dy\"/><x", "a&b", "a<b", "a>b", "l'a", "a\u0000b", "a\u0007", "a/b"}) {
            ResultatSaisiePrononciation r = ValidationPrononciation.verifier(ipa);
            assertFalse(r.valide(), ipa);
            assertTrue(r.motif().startsWith("Caractère non autorisé"), r.motif());
            assertEquals("", r.ipa());
        }
        assertTrue(ValidationPrononciation.verifier("a\u0000b").motif().contains("U+0000"),
                "caractère de contrôle désigné par son code");
    }

    @Test
    void chiffres_et_majuscules_refuses_avec_motif_precis() {
        assertTrue(ValidationPrononciation.verifier("dy2").motif().contains("« 2 »"));
        ResultatSaisiePrononciation maj = ValidationPrononciation.verifier("Dypɔ̃");
        assertFalse(maj.valide());
        assertTrue(maj.motif().contains("majuscule") && maj.motif().contains("« d »"), maj.motif());
    }

    @Test
    void diacritique_isole_refuse() {
        assertFalse(ValidationPrononciation.verifier("̃a").valide());
        assertFalse(ValidationPrononciation.verifier("a ̃").valide());
        assertTrue(ValidationPrononciation.verifier("ã").valide());
    }

    @Test
    void longueur_bornee() {
        String max = "a".repeat(ValidationPrononciation.LONGUEUR_MAX);
        assertTrue(ValidationPrononciation.verifier(max).valide());
        ResultatSaisiePrononciation trop = ValidationPrononciation.verifier(max + "a");
        assertFalse(trop.valide());
        assertTrue(trop.motif().contains("trop longue"), trop.motif());
    }

    @Test
    void forme_enregistree_stable_quelle_que_soit_la_saisie_composee_ou_decomposee() {
        String decompose = ValidationPrononciation.verifier("e\u0301").ipa();
        String compose = ValidationPrononciation.verifier("\u00E9").ipa();
        assertEquals("\u00E9", compose);
        assertEquals(compose, decompose);
        // Le tilde des nasales n'a pas de forme précomposée : il reste combinant.
        assertEquals("\u0254\u0303", ValidationPrononciation.verifier("\u0254\u0303").ipa());
    }

    @Test
    void diacritique_apres_un_separateur_reste_refuse() {
        assertFalse(ValidationPrononciation.verifier("a \u0303").valide());
        assertFalse(ValidationPrononciation.verifier("\u0303\u0308a").valide());
    }
}
