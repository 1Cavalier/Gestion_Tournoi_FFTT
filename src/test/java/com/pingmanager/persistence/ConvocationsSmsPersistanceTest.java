package com.pingmanager.persistence;

import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Convocations par SMS (B019) : aller-retour JSON et anciens fichiers. */
class ConvocationsSmsPersistanceTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Tournoi tournoi() {
        return new Tournoi("Tournoi de Brunoy", "TT Brunoy", LocalDate.of(2026, 11, 14));
    }

    private static Tournoi allerRetour(Tournoi t, Path fichier) throws Exception {
        DepotTournoi.sauvegarder(t, fichier);
        return DepotTournoi.charger(fichier).getTournoi();
    }

    @Test
    void tournoi_sans_sms_garde_son_ancien_format() throws Exception {
        Path fichier = Files.createTempFile("sms_vierge", DepotTournoi.EXTENSION);
        Tournoi relu = allerRetour(tournoi(), fichier);
        assertFalse(Files.readString(fichier).contains("convocationsSms"));
        assertTrue(relu.getConvocationsSms().estVierge());
    }

    @Test
    void reglages_numeros_et_journal_relus() throws Exception {
        Tournoi t = tournoi();
        ConvocationsSms c = t.getConvocationsSms();
        c.setEnvoiAutomatique(true);
        c.setExpediteur("BRUNOYTT");
        c.setLimiteEnvois(120);
        c.enregistrerContact(new ContactSms("123", "+33612345678", T));
        c.journaliser(new MessageSms(T, TypeConvocation.ARBITRAGE, "123", "Hugo MORIN", "Hugo, vous arbitrez…",
                StatutEnvoiSms.ENVOYE, "id-1"));
        c.journaliser(new MessageSms(T.plusMinutes(1), TypeConvocation.MATCH_CLASSEMENT, "9", "", "x",
                StatutEnvoiSms.LIMITE_ATTEINTE, ""));

        Path fichier = Files.createTempFile("sms_complet", DepotTournoi.EXTENSION);
        ConvocationsSms r = allerRetour(t, fichier).getConvocationsSms();
        assertTrue(r.isEnvoiAutomatique());
        assertEquals("BRUNOYTT", r.getExpediteur());
        assertEquals(120, r.getLimiteEnvois());
        assertEquals(new ContactSms("123", "+33612345678", T), r.contact("123"));
        assertEquals(c.getJournal(), r.getJournal());
        assertNull(r.getNumerosEffacesLe());
    }

    @Test
    void numeros_effaces_restent_effaces_apres_relecture() throws Exception {
        Tournoi t = tournoi();
        t.getConvocationsSms().enregistrerContact(new ContactSms("123", "+33612345678", T));
        t.getConvocationsSms().effacerNumeros(T.plusDays(1));
        Path fichier = Files.createTempFile("sms_efface", DepotTournoi.EXTENSION);
        ConvocationsSms r = allerRetour(t, fichier).getConvocationsSms();
        assertFalse(Files.readString(fichier).contains("+33612345678"), "le numéro n'est plus dans le fichier");
        assertTrue(r.getContacts().isEmpty());
        assertEquals(T.plusDays(1), r.getNumerosEffacesLe());
    }

    @Test
    void valeurs_abimees_ou_inconnues_n_empechent_pas_l_ouverture() throws Exception {
        Tournoi t = tournoi();
        ConvocationsSms c = t.getConvocationsSms();
        c.setEnvoiAutomatique(true);
        c.setExpediteur("BRUNOYTT");
        c.setLimiteEnvois(120);
        c.enregistrerContact(new ContactSms("123", "+33612345678", T));
        c.journaliser(new MessageSms(T, TypeConvocation.MATCH_POULE, "123", "Hugo", "x", StatutEnvoiSms.ENVOYE, ""));
        Path fichier = Files.createTempFile("sms_abime", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, fichier);
        String json = Files.readString(fichier);
        String abime = json.replace("\"MATCH_POULE\"", "\"PIGEON\"")
                .replaceAll("\"limiteEnvois\"(\\s*):(\\s*)120", "\"limiteEnvois\"$1:$2\"beaucoup\"")
                .replace("\"BRUNOYTT\"", "12")
                .replace("\"2026-11-14T10:00\"", "\"hier\"");
        assertNotEquals(json, abime);
        Files.writeString(fichier, abime);
        ConvocationsSms r = DepotTournoi.charger(fichier).getTournoi().getConvocationsSms();
        assertTrue(r.isEnvoiAutomatique());
        assertNull(r.getExpediteur());
        assertEquals(ConvocationsSms.LIMITE_PAR_DEFAUT, r.getLimiteEnvois());
        assertTrue(r.getContacts().isEmpty(), "contact sans date de consentement lisible : écarté");
        assertTrue(r.getJournal().isEmpty());

        Files.writeString(fichier, json.replace("\"convocationsSms\"", "\"convocationsSmsAutre\""));
        assertTrue(DepotTournoi.charger(fichier).getTournoi().getConvocationsSms().estVierge());
    }
}
