package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Convocations par SMS : consentement, déclenchement, limite, journal, effacement. */
class ServiceConvocationsSmsTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 14);
    private static final LocalDateTime MAINTENANT = JOUR.atTime(10, 0);

    /** Faux service d'envoi : garde les messages, peut refuser. */
    private static final class EnvoiEnregistre implements EnvoiSms {
        final List<String> envois = new ArrayList<>();
        boolean refuser;

        @Override
        public ResultatEnvoiSms envoyer(String expediteur, String numero, String texte) {
            if (refuser) {
                return ResultatEnvoiSms.echec("crédit épuisé");
            }
            envois.add(expediteur + "|" + numero + "|" + texte);
            return ResultatEnvoiSms.envoye("id-" + envois.size());
        }
    }

    private final Joueur hugo = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200);
    private final Joueur lea = new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100);
    private EnvoiEnregistre envoi;
    private ServiceConvocationsSms service;
    private Tournoi tournoi;
    private Match match;

    @BeforeEach
    void preparer() {
        envoi = new EnvoiEnregistre();
        service = new ServiceConvocationsSms(envoi);
        tournoi = new Tournoi("Tournoi de Brunoy", "TT Brunoy", JOUR);
        Tableau c = new Tableau("C", ConfigurationTableau.builder().toutesSeries().build());
        Poule b = new Poule("B", List.of(hugo, lea));
        match = new Match(hugo, lea);
        b.ajouterMatch(match);
        c.definirPoules(List.of(b));
        tournoi.ajouterTableau(c);
    }

    private void consentir(Joueur j, String numero) {
        assertTrue(service.enregistrerContact(tournoi, j.getNumeroLicence(), numero, true, MAINTENANT).estReussi());
    }

    @Test
    void numero_garde_seulement_avec_consentement_explicite() {
        ResultatContactSms r = service.enregistrerContact(tournoi, "1", "06 12 34 56 78", false, MAINTENANT);
        assertEquals(ResultatContactSms.Motif.CONSENTEMENT_MANQUANT, r.motif());
        assertNull(tournoi.getConvocationsSms().contact("1"));

        r = service.enregistrerContact(tournoi, "1", "06 12 34 56 78", true, MAINTENANT);
        assertTrue(r.estReussi());
        assertEquals("+33612345678", tournoi.getConvocationsSms().contact("1").numero());
        assertEquals(MAINTENANT, tournoi.getConvocationsSms().contact("1").consentementLe());
    }

    @Test
    void numero_invalide_ou_licence_absente_refuses() {
        assertEquals(ResultatContactSms.Motif.NUMERO_INVALIDE,
                service.enregistrerContact(tournoi, "1", "01 23 45 67 89", true, MAINTENANT).motif());
        assertEquals(ResultatContactSms.Motif.LICENCE_MANQUANTE,
                service.enregistrerContact(tournoi, " ", "06 12 34 56 78", true, MAINTENANT).motif());
        assertTrue(tournoi.getConvocationsSms().getContacts().isEmpty());
    }

    @Test
    void saisie_facultative_vide_ou_recevable_sans_refus_et_sans_rien_enregistrer() {
        assertTrue(ServiceConvocationsSms.refusSaisieFacultative("", false).isEmpty());
        assertTrue(ServiceConvocationsSms.refusSaisieFacultative(null, true).isEmpty());
        assertTrue(ServiceConvocationsSms.refusSaisieFacultative("06 12 34 56 78", true).isEmpty());
        assertEquals(ResultatContactSms.Motif.CONSENTEMENT_MANQUANT,
                ServiceConvocationsSms.refusSaisieFacultative("06 12 34 56 78", false).orElseThrow().motif());
        assertEquals(ResultatContactSms.Motif.NUMERO_INVALIDE,
                ServiceConvocationsSms.refusSaisieFacultative("01 23 45 67 89", true).orElseThrow().motif());
        assertTrue(tournoi.getConvocationsSms().getContacts().isEmpty());
    }

    @Test
    void effacement_automatique_le_lendemain_du_dernier_jour() {
        assertEquals(JOUR.plusDays(1), ServiceConvocationsSms.dateEffacementAutomatique(tournoi));
        tournoi.ajouterJour(JOUR.plusDays(1));
        assertEquals(JOUR.plusDays(2), ServiceConvocationsSms.dateEffacementAutomatique(tournoi));
    }

    @Test
    void retrait_du_consentement_efface_le_numero() {
        consentir(hugo, "0612345678");
        assertTrue(service.retirerContact(tournoi, "1"));
        assertNull(tournoi.getConvocationsSms().contact("1"));
        assertFalse(service.retirerContact(tournoi, "1"));
    }

    @Test
    void envoi_automatique_seulement_si_l_annonce_comprend_le_sms_et_si_l_option_est_active() {
        consentir(hugo, "0612345678");
        assertFalse(service.apresLancement(tournoi, match, 4, ModeAnnonce.SMS, MAINTENANT).declenche(),
                "option coupée par défaut");
        tournoi.getConvocationsSms().setEnvoiAutomatique(true);
        assertFalse(service.apresLancement(tournoi, match, 4, null, MAINTENANT).declenche());
        assertFalse(service.apresLancement(tournoi, match, 4, ModeAnnonce.AUCUNE, MAINTENANT).declenche());
        assertFalse(service.apresLancement(tournoi, match, 4, ModeAnnonce.APPEL, MAINTENANT).declenche(),
                "jamais d'appel, et pas de SMS pour « Appel vocal »");
        assertTrue(envoi.envois.isEmpty());

        ResultatConvocationsSms r = service.apresLancement(tournoi, match, 4, ModeAnnonce.SMS, MAINTENANT);
        assertTrue(r.declenche());
        assertEquals(1, r.nombreEnvoyes());
        assertEquals(List.of("Léa DURAND"), r.sansNumero());
        assertEquals("TTBRUNOY|+33612345678|Hugo, vous etes attendu(e) table 4 contre Léa DURAND (US Évry) - "
                + "Tableau C, poule B.", envoi.envois.get(0));
        assertEquals("1 SMS envoyé · sans numéro : Léa DURAND.", r.resume());
    }

    @Test
    void sms_et_appel_vocal_n_envoie_que_le_sms() {
        consentir(hugo, "0612345678");
        consentir(lea, "0712345678");
        tournoi.getConvocationsSms().setEnvoiAutomatique(true);
        ResultatConvocationsSms r = service.apresLancement(tournoi, match, 2, ModeAnnonce.SMS_ET_APPEL, MAINTENANT);
        assertEquals(2, r.nombreEnvoyes());
        assertEquals(2, envoi.envois.size());
        assertTrue(envoi.envois.get(1).startsWith("TTBRUNOY|+33712345678|Léa, vous etes attendu(e) table 2 contre "
                + "Hugo MORIN"), envoi.envois.get(1));
    }

    @Test
    void envoi_manuel_sans_option_et_journal_des_envois() {
        consentir(hugo, "0612345678");
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, match, 5, MAINTENANT);
        assertTrue(r.declenche());
        List<MessageSms> journal = tournoi.getConvocationsSms().getJournal();
        assertEquals(1, journal.size());
        MessageSms m = journal.get(0);
        assertEquals(TypeConvocation.MATCH_POULE, m.type());
        assertEquals("1", m.numeroLicence());
        assertEquals("Hugo MORIN", m.destinataire());
        assertEquals(StatutEnvoiSms.ENVOYE, m.statut());
        assertEquals("id-1", m.detail());
        assertEquals(MAINTENANT, m.instant());
        assertFalse(m.texte().contains("+33"), "le numéro n'est pas dans le journal");
    }

    @Test
    void convocation_d_arbitrage() {
        tournoi.getConvocationsSms().enregistrerContact(
                new com.pingmanager.model.ContactSms("9", "+33600000009", MAINTENANT));
        ResultatConvocationsSms r = service.convoquerArbitre(tournoi, "9", "Paul", "Paul MARTIN", match, 4,
                MAINTENANT);
        assertEquals(1, r.nombreEnvoyes());
        assertEquals(TypeConvocation.ARBITRAGE, r.messages().get(0).type());
        assertEquals("Paul, vous arbitrez la partie table 4 : Hugo MORIN contre Léa DURAND - Tableau C, poule B.",
                r.messages().get(0).texte());
    }

    @Test
    void limite_d_envois_bloque_et_journalise_le_refus() {
        consentir(hugo, "0612345678");
        consentir(lea, "0712345678");
        tournoi.getConvocationsSms().setLimiteEnvois(3);
        service.convoquerJoueurs(tournoi, match, 1, MAINTENANT);
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, match, 1, MAINTENANT);
        assertEquals(1, r.nombreEnvoyes());
        assertTrue(r.limiteAtteinte());
        assertEquals(3, envoi.envois.size());
        assertEquals(3, tournoi.getConvocationsSms().nombreEnvoyes());
        assertEquals(4, tournoi.getConvocationsSms().getJournal().size());
        assertEquals(StatutEnvoiSms.LIMITE_ATTEINTE, tournoi.getConvocationsSms().getJournal().get(3).statut());
        assertTrue(r.resume().contains("limite d'envois atteinte"), r.resume());
    }

    @Test
    void echec_du_service_journalise_sans_compter_dans_la_limite() {
        consentir(hugo, "0612345678");
        envoi.refuser = true;
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, match, 1, MAINTENANT);
        assertTrue(r.aEchoue());
        assertEquals(0, tournoi.getConvocationsSms().nombreEnvoyes());
        assertEquals("crédit épuisé", tournoi.getConvocationsSms().getJournal().get(0).detail());
    }

    @Test
    void exception_du_service_devient_un_echec() {
        consentir(hugo, "0612345678");
        ServiceConvocationsSms fragile = new ServiceConvocationsSms((e, n, t) -> {
            throw new IllegalStateException("réseau coupé");
        });
        ResultatConvocationsSms r = fragile.convoquerJoueurs(tournoi, match, 1, MAINTENANT);
        assertEquals(StatutEnvoiSms.ECHEC, r.messages().get(0).statut());
        assertEquals("réseau coupé", r.messages().get(0).detail());
    }

    @Test
    void match_avec_joueur_a_determiner_rien_n_est_envoye() {
        consentir(hugo, "0612345678");
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, new Match(hugo, Joueur.aDeterminer()), 1,
                MAINTENANT);
        assertTrue(r.messages().isEmpty());
        assertTrue(envoi.envois.isEmpty());
    }

    @Test
    void match_hors_tableau_convoque_sans_contexte() {
        consentir(hugo, "0612345678");
        service.convoquerJoueurs(tournoi, new Match(hugo, lea), 6, MAINTENANT);
        assertTrue(envoi.envois.get(0).endsWith("contre Léa DURAND (US Évry)."), envoi.envois.get(0));
    }

    @Test
    void expediteur_du_club_ou_choisi_selon_la_charte() {
        assertEquals("TTBRUNOY", ServiceConvocationsSms.expediteur(tournoi));
        assertFalse(service.definirExpediteur(tournoi, "PING TOURNOI"));
        assertEquals("TTBRUNOY", ServiceConvocationsSms.expediteur(tournoi));
        assertTrue(service.definirExpediteur(tournoi, "BRUNOYTT"));
        assertEquals("BRUNOYTT", ServiceConvocationsSms.expediteur(tournoi));
        assertTrue(service.definirExpediteur(tournoi, ""));
        assertEquals("TTBRUNOY", ServiceConvocationsSms.expediteur(tournoi));
    }

    @Test
    void numeros_effaces_le_lendemain_du_dernier_jour_journal_garde() {
        consentir(hugo, "0612345678");
        service.convoquerJoueurs(tournoi, match, 1, MAINTENANT);
        assertEquals(0, service.effacerSiTermine(tournoi, JOUR.atTime(23, 59)), "pas pendant le tournoi");
        assertEquals(1, service.effacerSiTermine(tournoi, JOUR.plusDays(1).atTime(8, 0)));
        assertTrue(tournoi.getConvocationsSms().getContacts().isEmpty());
        assertEquals(JOUR.plusDays(1).atTime(8, 0), tournoi.getConvocationsSms().getNumerosEffacesLe());
        assertEquals(1, tournoi.getConvocationsSms().getJournal().size(), "le journal reste");
        assertEquals(0, service.effacerSiTermine(tournoi, JOUR.plusDays(2).atTime(8, 0)));
    }

    @Test
    void pas_de_nouveau_numero_apres_la_fin_du_tournoi() {
        ResultatContactSms r = service.enregistrerContact(tournoi, "1", "0612345678", true,
                JOUR.plusDays(1).atStartOfDay());
        assertEquals(ResultatContactSms.Motif.TOURNOI_TERMINE, r.motif());
    }

    @Test
    void tournoi_sur_plusieurs_jours_efface_apres_le_dernier() {
        tournoi.ajouterJour(JOUR.plusDays(1));
        consentir(hugo, "0612345678");
        assertEquals(0, service.effacerSiTermine(tournoi, JOUR.plusDays(1).atTime(20, 0)));
        assertEquals(1, service.effacerSiTermine(tournoi, JOUR.plusDays(2).atTime(8, 0)));
    }

    @Test
    void effacement_manuel_par_le_club() {
        consentir(hugo, "0612345678");
        consentir(lea, "0712345678");
        assertEquals(2, service.effacerNumeros(tournoi, MAINTENANT));
        assertTrue(tournoi.getConvocationsSms().getContacts().isEmpty());
    }
}
