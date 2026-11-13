package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Limite d'essais (README section 5, point 15) : 5 échecs, puis blocage croissant. */
class LimiteurEssaisTest {

    private final ServiceComptesOrganisateursTest.HorlogeFixe horloge =
            new ServiceComptesOrganisateursTest.HorlogeFixe(Instant.parse("2026-11-13T10:00:00Z"));
    private final LimiteurEssais limite = new LimiteurEssais(horloge);

    private void echouer(String cle, int fois) {
        for (int i = 0; i < fois; i++) {
            limite.enregistrerEchec(cle);
        }
    }

    @Test
    void quatre_echecs_ne_bloquent_pas_le_cinquieme_bloque_une_minute() {
        echouer("a@example.org", 4);
        assertFalse(limite.estBloque("a@example.org"));
        limite.enregistrerEchec("a@example.org");
        assertEquals(Duration.ofMinutes(1), limite.attenteRestante("a@example.org"));
    }

    @Test
    void blocage_croissant_puis_plafonne_a_une_heure() {
        echouer("a", 5);
        horloge.avancer(Duration.ofMinutes(1));
        assertFalse(limite.estBloque("a"), "blocage levé après 1 min");
        limite.enregistrerEchec("a");
        assertEquals(Duration.ofMinutes(2), limite.attenteRestante("a"));
        horloge.avancer(Duration.ofMinutes(2));
        limite.enregistrerEchec("a");
        assertEquals(Duration.ofMinutes(4), limite.attenteRestante("a"));
        echouer("a", 40);
        assertEquals(Duration.ofHours(1), limite.attenteRestante("a"), "plafond d'une heure");
    }

    @Test
    void reussite_remet_le_compteur_a_zero() {
        echouer("a", 4);
        limite.enregistrerReussite("a");
        assertEquals(0, limite.echecs("a"));
        echouer("a", 4);
        assertFalse(limite.estBloque("a"));
    }

    @Test
    void cles_independantes_et_insensibles_a_la_casse() {
        echouer("Paul@Example.org ", 5);
        assertTrue(limite.estBloque("paul@example.org"));
        assertFalse(limite.estBloque("autre@example.org"));
    }

    @Test
    void message_de_blocage_arrondi_a_la_minute_superieure() {
        assertEquals("1 min", LimiteurEssais.formaterAttente(Duration.ofSeconds(5)));
        assertEquals("2 min", LimiteurEssais.formaterAttente(Duration.ofSeconds(61)));
        assertEquals("Trop d'essais infructueux : réessayez dans 4 min.",
                LimiteurEssais.messageBlocage(Duration.ofMinutes(4)));
    }

    @Test
    void parametres_incoherents_refuses() {
        assertThrows(IllegalArgumentException.class,
                () -> new LimiteurEssais(horloge, 0, Duration.ofMinutes(1), Duration.ofHours(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new LimiteurEssais(horloge, 5, Duration.ofHours(2), Duration.ofHours(1)));
    }
}
