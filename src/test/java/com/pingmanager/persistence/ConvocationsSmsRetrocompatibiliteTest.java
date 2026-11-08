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

/** Convocations SMS (B019) : anciens fichiers, consentement exigé à la relecture, réécriture stable. */
class ConvocationsSmsRetrocompatibiliteTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Tournoi tournoi() {
        return new Tournoi("Tournoi de Brunoy", "TT Brunoy", LocalDate.of(2026, 11, 14));
    }

    private static Path ecrire(Tournoi t) throws Exception {
        Path f = Files.createTempFile("sms_retro", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        return f;
    }

    @Test
    void ancien_fichier_sans_bloc_sms_se_relit_avec_reglages_par_defaut() throws Exception {
        Path f = ecrire(tournoi());
        String json = Files.readString(f);
        assertFalse(json.contains("convocationsSms"));
        ConvocationsSms c = DepotTournoi.charger(f).getTournoi().getConvocationsSms();
        assertTrue(c.estVierge());
        assertFalse(c.isEnvoiAutomatique(), "rien ne part par défaut");
        assertEquals(ConvocationsSms.LIMITE_PAR_DEFAUT, c.getLimiteEnvois());
    }

    @Test
    void bloc_sms_vide_ou_de_mauvais_type_se_relit() throws Exception {
        Tournoi t = tournoi();
        t.getConvocationsSms().setEnvoiAutomatique(true);
        Path f = ecrire(t);
        String json = Files.readString(f);
        Files.writeString(f, json.replaceAll("\"convocationsSms\"\\s*:\\s*\\{[^{}]*\\}", "\"convocationsSms\": {}"));
        ConvocationsSms c = DepotTournoi.charger(f).getTournoi().getConvocationsSms();
        assertFalse(c.isEnvoiAutomatique());
        Files.writeString(f, json.replaceAll("\"convocationsSms\"\\s*:\\s*\\{[^{}]*\\}", "\"convocationsSms\": 7"));
        assertTrue(DepotTournoi.charger(f).getTournoi().getConvocationsSms().estVierge());
    }

    @Test
    void contact_sans_consentement_dans_le_fichier_n_est_pas_garde() throws Exception {
        Tournoi t = tournoi();
        t.getConvocationsSms().enregistrerContact(new ContactSms("123", "+33612345678", T));
        Path f = ecrire(t);
        String json = Files.readString(f);
        Files.writeString(f, json.replaceAll("\"consentementLe\"\\s*:\\s*\"[^\"]*\"", "\"x\": 1"));
        assertTrue(DepotTournoi.charger(f).getTournoi().getConvocationsSms().getContacts().isEmpty());
    }

    @Test
    void contact_sans_numero_ou_sans_licence_est_ecarte() throws Exception {
        Tournoi t = tournoi();
        t.getConvocationsSms().enregistrerContact(new ContactSms("123", "+33612345678", T));
        Path f = ecrire(t);
        String json = Files.readString(f);
        Files.writeString(f, json.replace("\"+33612345678\"", "null"));
        assertTrue(DepotTournoi.charger(f).getTournoi().getConvocationsSms().getContacts().isEmpty());
        Files.writeString(f, json.replace("\"123\"", "\"\""));
        assertTrue(DepotTournoi.charger(f).getTournoi().getConvocationsSms().getContacts().isEmpty());
    }

    @Test
    void limite_hors_bornes_dans_le_fichier_revient_a_la_valeur_par_defaut() throws Exception {
        Tournoi t = tournoi();
        t.getConvocationsSms().setLimiteEnvois(120);
        Path f = ecrire(t);
        String json = Files.readString(f);
        for (String valeur : new String[]{"-5", "5001", "999999999999"}) {
            Files.writeString(f, json.replaceAll("(\"limiteEnvois\"\\s*:\\s*)120", "$1" + valeur));
            assertEquals(ConvocationsSms.LIMITE_PAR_DEFAUT,
                    DepotTournoi.charger(f).getTournoi().getConvocationsSms().getLimiteEnvois(), valeur);
        }
    }

    @Test
    void aller_retour_double_est_stable_avec_accents_retours_a_la_ligne_et_limite_zero() throws Exception {
        Tournoi t = tournoi();
        ConvocationsSms c = t.getConvocationsSms();
        c.setLimiteEnvois(0);
        c.setExpediteur("TTÉVRY");
        c.enregistrerContact(new ContactSms("123", "+447911123456", T));
        c.enregistrerContact(new ContactSms("456", "+33712345678", T.plusMinutes(5)));
        c.journaliser(new MessageSms(T, TypeConvocation.TOUR_TABLEAU, "123", "Léa \"D\" \\ DURAND",
                "ligne1\nligne2 « é » \t", StatutEnvoiSms.ECHEC, "crédit épuisé"));
        c.effacerNumeros(T.plusDays(1));
        c.enregistrerContact(new ContactSms("789", "+33611111111", T.plusDays(1)));
        Path f1 = ecrire(t);
        Tournoi relu = DepotTournoi.charger(f1).getTournoi();
        ConvocationsSms r = relu.getConvocationsSms();
        assertEquals(0, r.getLimiteEnvois());
        assertEquals(c.getJournal(), r.getJournal());
        assertEquals(c.getNumerosEffacesLe(), r.getNumerosEffacesLe());
        assertEquals(c.contact("789"), r.contact("789"));
        assertEquals(1, r.getContacts().size());
        Path f2 = ecrire(relu);
        assertEquals(Files.readString(f1), Files.readString(f2), "réécriture identique");
    }

    @Test
    void retrait_de_tous_les_numeros_sans_autre_reglage_retire_le_bloc_du_fichier() throws Exception {
        Tournoi t = tournoi();
        t.getConvocationsSms().enregistrerContact(new ContactSms("123", "+33612345678", T));
        t.getConvocationsSms().retirerContact("123");
        Path f = ecrire(t);
        assertFalse(Files.readString(f).contains("+33612345678"));
    }
}
