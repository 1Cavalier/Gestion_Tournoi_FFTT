package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Saisie d'un numéro de portable pour les SMS. */
class NumeroPortableTest {

    @Test
    void portable_francais_sous_toutes_ses_ecritures() {
        assertEquals(Optional.of("+33612345678"), NumeroPortable.normaliser("06 12 34 56 78"));
        assertEquals(Optional.of("+33712345678"), NumeroPortable.normaliser("07.12.34.56.78"));
        assertEquals(Optional.of("+33612345678"), NumeroPortable.normaliser("+33 6 12 34 56 78"));
        assertEquals(Optional.of("+33612345678"), NumeroPortable.normaliser("0033612345678"));
        assertEquals(Optional.of("+33612345678"), NumeroPortable.normaliser(" 06-12-34-56-78 "));
    }

    @Test
    void zero_de_tete_entre_parentheses_accepte() {
        assertEquals(Optional.of("+33612345678"), NumeroPortable.normaliser("+33 (0)6 12 34 56 78"));
        assertEquals(Optional.of("+33612345678"), NumeroPortable.normaliser("+33 (0) 6 12 34 56 78"));
    }

    @Test
    void numero_international_accepte() {
        assertEquals(Optional.of("+32470123456"), NumeroPortable.normaliser("+32 470 12 34 56"));
    }

    @Test
    void fixe_numero_incomplet_ou_texte_refuses() {
        assertTrue(NumeroPortable.normaliser("01 23 45 67 89").isEmpty(), "fixe");
        assertTrue(NumeroPortable.normaliser("+33 1 23 45 67 89").isEmpty(), "fixe international");
        assertTrue(NumeroPortable.normaliser("06 12 34 56").isEmpty());
        assertTrue(NumeroPortable.normaliser("abc").isEmpty());
        assertTrue(NumeroPortable.normaliser("").isEmpty());
        assertTrue(NumeroPortable.normaliser(null).isEmpty());
    }

    @Test
    void numero_masque_a_l_affichage() {
        assertEquals("+33 6 •• •• •• 78", NumeroPortable.masquer("+33612345678"));
        assertEquals("+32 •••• 56", NumeroPortable.masquer("+32470123456"));
        assertFalse(NumeroPortable.masquer("+33612345678").contains("1234"));
    }
}
