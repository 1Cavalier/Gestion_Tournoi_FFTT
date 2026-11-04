package com.pingmanager.core.reglement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatsTest {

    @Test
    void periodes_en_toutes_lettres() {
        assertEquals("le samedi 21 novembre 2026", Formats.periode(LocalDate.of(2026, 11, 21), null));
        assertEquals("les samedi 17 et dimanche 18 avril 2027",
                Formats.periode(LocalDate.of(2027, 4, 17), LocalDate.of(2027, 4, 18)));
        assertEquals("les samedi 31 octobre et dimanche 1er novembre 2026".replace("les samedi 31 octobre et", "du samedi 31 octobre au"),
                Formats.periode(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void premier_du_mois_et_saison() {
        assertEquals("lundi 1er février 2027", Formats.jourLong(LocalDate.of(2027, 2, 1)));
        assertEquals("mardi 11 mai 2027", Formats.jourLong(LocalDate.of(2027, 5, 11)));
        assertEquals("2026-2027", Formats.saison(LocalDate.of(2027, 4, 17)));
        assertEquals("2026-2027", Formats.saison(LocalDate.of(2026, 9, 1)));
        assertEquals("3 110 €", Formats.montant(3110));
        assertEquals("8,50 €", Formats.montant(8.5));
    }
}
