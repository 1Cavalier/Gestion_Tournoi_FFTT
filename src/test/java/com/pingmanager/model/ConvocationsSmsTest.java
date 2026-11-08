package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Données des convocations par SMS d'un tournoi. */
class ConvocationsSmsTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 14, 10, 0);

    @Test
    void par_defaut_rien_ne_part_et_rien_n_est_garde() {
        ConvocationsSms c = new Tournoi("T", "Club", T.toLocalDate()).getConvocationsSms();
        assertFalse(c.isEnvoiAutomatique());
        assertNull(c.getExpediteur());
        assertEquals(ConvocationsSms.LIMITE_PAR_DEFAUT, c.getLimiteEnvois());
        assertTrue(c.getContacts().isEmpty());
        assertTrue(c.getJournal().isEmpty());
        assertTrue(c.estVierge());
    }

    @Test
    void limite_bornee() {
        ConvocationsSms c = new ConvocationsSms();
        c.setLimiteEnvois(0);
        c.setLimiteEnvois(ConvocationsSms.LIMITE_MAXIMALE);
        assertThrows(IllegalArgumentException.class, () -> c.setLimiteEnvois(-1));
        assertThrows(IllegalArgumentException.class, () -> c.setLimiteEnvois(ConvocationsSms.LIMITE_MAXIMALE + 1));
        assertFalse(c.estVierge());
    }

    @Test
    void contact_remplace_puis_efface_avec_la_date() {
        ConvocationsSms c = new ConvocationsSms();
        c.enregistrerContact(new ContactSms("1", "+33612345678", T));
        c.enregistrerContact(new ContactSms("1", "+33712345678", T));
        assertEquals(1, c.getContacts().size());
        assertEquals("+33712345678", c.contact("1").numero());
        c.effacerNumeros(T.plusDays(1));
        assertNull(c.contact("1"));
        assertEquals(T.plusDays(1), c.getNumerosEffacesLe());
        assertThrows(UnsupportedOperationException.class, () -> c.getContacts().clear());
    }

    @Test
    void seuls_les_envois_reussis_comptent() {
        ConvocationsSms c = new ConvocationsSms();
        c.journaliser(new MessageSms(T, TypeConvocation.MATCH_POULE, "1", "A", "x", StatutEnvoiSms.ENVOYE, null));
        c.journaliser(new MessageSms(T, TypeConvocation.MATCH_POULE, "1", "A", "x", StatutEnvoiSms.ECHEC, "ko"));
        c.journaliser(new MessageSms(T, TypeConvocation.ARBITRAGE, "1", null, "x",
                StatutEnvoiSms.LIMITE_ATTEINTE, ""));
        assertEquals(1, c.nombreEnvoyes());
        assertEquals("", c.getJournal().get(2).destinataire());
    }

    @Test
    void expediteur_vide_revient_a_la_proposition() {
        ConvocationsSms c = new ConvocationsSms();
        c.setExpediteur("  ");
        assertNull(c.getExpediteur());
        c.setExpediteur(" TTBRUNOY ");
        assertEquals("TTBRUNOY", c.getExpediteur());
    }
}
