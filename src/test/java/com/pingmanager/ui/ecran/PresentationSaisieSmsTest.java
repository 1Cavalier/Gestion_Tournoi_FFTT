package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatContactSms;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Saisie facultative du portable avec consentement explicite (inscription en ligne et club). */
class PresentationSaisieSmsTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 1, 18, 30);

    private static Tournoi tournoi() {
        return new Tournoi("T", "TT Brunoy", LocalDate.of(2026, 11, 14));
    }

    @Test
    void numero_vide_aucun_refus_meme_sans_consentement() {
        assertTrue(PresentationSaisieSms.refus("", false).isEmpty());
        assertTrue(PresentationSaisieSms.refus("  ", false).isEmpty());
    }

    @Test
    void numero_saisi_sans_case_cochee_refuse_avec_le_motif() {
        assertTrue(PresentationSaisieSms.refus("06 12 34 56 78", false).orElseThrow().contains("accord explicite"));
        assertTrue(PresentationSaisieSms.refus("12", true).orElseThrow().startsWith("Numéro de portable non reconnu"));
        assertTrue(PresentationSaisieSms.refus("06 12 34 56 78", true).isEmpty());
    }

    @Test
    void etat_du_numero_masque_ou_absent_et_rappel_d_effacement() {
        Tournoi t = tournoi();
        assertEquals("Aucun numéro enregistré.", PresentationSaisieSms.etat(t, "1"));
        assertFalse(PresentationSaisieSms.aUnNumero(t, "1"));
        t.getConvocationsSms().enregistrerContact(new ContactSms("1", "+33612345678", T));
        assertEquals("Numéro enregistré : +33 6 •• •• •• 78 · accord du 01/11 18:30.", PresentationSaisieSms.etat(t, "1"));
        assertTrue(PresentationSaisieSms.aUnNumero(t, "1"));
        assertEquals("Effacé d'office le 15 novembre 2026, lendemain du dernier jour du tournoi.",
                PresentationSaisieSms.rappelEffacement(t));
        t.getConvocationsSms().effacerNumeros(LocalDateTime.of(2026, 11, 15, 9, 0));
        assertEquals("Aucun numéro : les numéros ont été effacés le 15 novembre 2026.",
                PresentationSaisieSms.etat(t, "1"));
    }

    @Test
    void ligne_de_confirmation_apres_inscription() {
        assertTrue(PresentationSaisieSms.apresInscription(null).isEmpty());
        assertEquals("Convocations par SMS : numéro enregistré (+33 6 •• •• •• 78).", PresentationSaisieSms
                .apresInscription(new ResultatContactSms(ResultatContactSms.Motif.ENREGISTRE, "+33612345678"))
                .orElseThrow());
        assertTrue(PresentationSaisieSms.apresInscription(new ResultatContactSms(
                ResultatContactSms.Motif.TOURNOI_TERMINE, "")).orElseThrow().contains("non enregistré"));
    }

    @Test
    void case_de_consentement_dit_l_usage_et_l_effacement() {
        assertTrue(PresentationSaisieSms.CONSENTEMENT_JOUEUR.startsWith("J'accepte"));
        assertTrue(PresentationSaisieSms.CONSENTEMENT_JOUEUR.contains("effacé à la fin du tournoi"));
        assertTrue(PresentationSaisieSms.CONSENTEMENT_CLUB.contains("effacé à la fin du tournoi"));
    }
}
