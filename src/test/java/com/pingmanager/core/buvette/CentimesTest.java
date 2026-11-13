package com.pingmanager.core.buvette;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CentimesTest {

    @Test
    void mise_en_forme_francaise_en_euros() {
        assertEquals("1,10 €", Centimes.euros(110));
        assertEquals("0,05 €", Centimes.euros(5));
        assertEquals("-2,50 €", Centimes.euros(-250));
        assertEquals("12 345,67 €", Centimes.euros(1_234_567));
    }

    @Test
    void lecture_des_saisies_courantes() {
        assertEquals(OptionalLong.of(110), Centimes.lire("1,1"));
        assertEquals(OptionalLong.of(110), Centimes.lire("1.10"));
        assertEquals(OptionalLong.of(200), Centimes.lire(" 2 € "));
        assertEquals(OptionalLong.of(0), Centimes.lire("0"));
    }

    @Test
    void lecture_refuse_negatif_trop_de_decimales_texte_et_depassement() {
        assertEquals(OptionalLong.empty(), Centimes.lire("-1"));
        assertEquals(OptionalLong.empty(), Centimes.lire("1,234"));
        assertEquals(OptionalLong.empty(), Centimes.lire("abc"));
        assertEquals(OptionalLong.empty(), Centimes.lire(""));
        assertEquals(OptionalLong.empty(), Centimes.lire("1000,01"));
        assertEquals(OptionalLong.of(100_000), Centimes.lire("1000"));
    }

    @Test
    void arrondi_au_centime_moitie_vers_le_haut() {
        assertEquals(37, Centimes.arrondir(new BigDecimal("36.6666")));
        assertEquals(37, Centimes.arrondir(new BigDecimal("36.5")));
        assertEquals(36, Centimes.arrondir(new BigDecimal("36.49")));
    }
}
