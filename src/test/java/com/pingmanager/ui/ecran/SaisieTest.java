package com.pingmanager.ui.ecran;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaisieTest {

    @Test
    void heures_tolerantes() {
        assertEquals(LocalTime.of(9, 0), Saisie.lireHeure("9", "x"));
        assertEquals(LocalTime.of(9, 0), Saisie.lireHeure("9h", "x"));
        assertEquals(LocalTime.of(9, 30), Saisie.lireHeure("9h30", "x"));
        assertEquals(LocalTime.of(9, 30), Saisie.lireHeure("09:30", "x"));
        assertEquals(LocalTime.of(13, 5), Saisie.lireHeure(" 13 H 05 ", "x"));
        assertNull(Saisie.lireHeure("  ", "x"));
        assertThrows(IllegalArgumentException.class, () -> Saisie.lireHeure("25h", "x"));
        assertThrows(IllegalArgumentException.class, () -> Saisie.lireHeure("midi", "x"));
    }

    @Test
    void montants_tolerants() {
        assertEquals(8.0, Saisie.lireMontant("8", "x"));
        assertEquals(8.5, Saisie.lireMontant("8,50 €", "x"));
        assertEquals(8.5, Saisie.lireMontant("8.5", "x"));
        assertNull(Saisie.lireMontant("", "x"));
        assertThrows(IllegalArgumentException.class, () -> Saisie.lireMontant("-2", "x"));
    }

    @Test
    void affichage() {
        assertEquals("9h", Saisie.heure(LocalTime.of(9, 0)));
        assertEquals("9h05", Saisie.heure(LocalTime.of(9, 5)));
        assertEquals("8 €", Saisie.montant(8));
    }
}
