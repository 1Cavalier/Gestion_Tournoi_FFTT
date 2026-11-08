package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Alphabet et longueur d'un SMS (GSM 7 bits / UCS-2). */
class TexteSmsTest {

    @Test
    void lettres_francaises_de_l_alphabet_gsm_gardees() {
        assertTrue(TexteSms.estGsm7("éèàùÉ Ç ì ò ä ö ü ñ"));
        assertEquals("Léa à la fete, ca va", TexteSms.versGsm7("Léa à la fête, ça va"));
        assertEquals("Élodie MÉRY à Évry", TexteSms.versGsm7("Élodie MÉRY à Évry"));
    }

    @Test
    void lettres_hors_alphabet_gsm_remplacees_par_leur_lettre_de_base() {
        assertFalse(TexteSms.estGsm7("êtes"));
        assertEquals("etes", TexteSms.versGsm7("êtes"));
        assertEquals("Ile", TexteSms.versGsm7("Île"));
        assertEquals("garcon", TexteSms.versGsm7("garçon"));
        assertEquals("coeur OEuvre", TexteSms.versGsm7("cœur Œuvre"));
        assertEquals("Noel aigue", TexteSms.versGsm7("Noël aiguë"));
        assertEquals("cote foret hotel", TexteSms.versGsm7("côte forêt hôtel"));
    }

    @Test
    void ponctuation_typographique_ramenee_a_l_ascii() {
        assertEquals("l'arbitre \"A\" - B ...", TexteSms.versGsm7("l’arbitre «A» – B …"));
        assertEquals("C - Toutes séries", TexteSms.versGsm7("C · Toutes séries"));
        assertEquals("a b", TexteSms.versGsm7("a b"));
    }

    @Test
    void caractere_sans_equivalent_devient_point_d_interrogation() {
        String s = TexteSms.versGsm7("ok ☃ 中");
        assertEquals("ok ? ?", s);
        assertTrue(TexteSms.estGsm7(s));
    }

    @Test
    void longueur_gsm_compte_double_les_caracteres_d_extension() {
        assertEquals(3, TexteSms.longueur("abc"));
        assertEquals(2, TexteSms.longueur("€"));
        assertEquals(5, TexteSms.longueur("[a]"));
    }

    @Test
    void un_sms_gsm_fait_160_caracteres_puis_segments_de_153() {
        assertEquals(0, TexteSms.nombreDeSms(""));
        assertEquals(1, TexteSms.nombreDeSms("a".repeat(160)));
        assertEquals(2, TexteSms.nombreDeSms("a".repeat(161)));
        assertEquals(2, TexteSms.nombreDeSms("a".repeat(306)));
        assertEquals(3, TexteSms.nombreDeSms("a".repeat(307)));
    }

    @Test
    void un_seul_caractere_unicode_fait_passer_a_70_caracteres() {
        assertEquals(1, TexteSms.nombreDeSms("ê" + "a".repeat(69)));
        assertEquals(2, TexteSms.nombreDeSms("ê" + "a".repeat(70)));
        assertEquals(3, TexteSms.nombreDeSms("ê" + "a".repeat(134)));
    }
}
