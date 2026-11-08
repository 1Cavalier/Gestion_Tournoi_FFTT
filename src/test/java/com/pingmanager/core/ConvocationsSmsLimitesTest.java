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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Convocations SMS (B019) : cas limites vérifiés après coup (limite, consentement, effacement, textes, numéros). */
class ConvocationsSmsLimitesTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 14);
    private static final LocalDateTime T = JOUR.atTime(10, 0);

    private final List<String> envois = new ArrayList<>();
    private final Joueur hugo = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200);
    private final Joueur lea = new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100);
    private ServiceConvocationsSms service;
    private Tournoi tournoi;
    private Match match;

    @BeforeEach
    void preparer() {
        service = new ServiceConvocationsSms((e, n, t) -> {
            envois.add(e + "|" + n + "|" + t);
            return ResultatEnvoiSms.envoye("id" + envois.size());
        });
        tournoi = new Tournoi("Tournoi", "TT Brunoy", JOUR);
        Tableau c = new Tableau("C", ConfigurationTableau.builder().toutesSeries().build());
        Poule b = new Poule("B", List.of(hugo, lea));
        match = new Match(hugo, lea);
        b.ajouterMatch(match);
        c.definirPoules(List.of(b));
        tournoi.ajouterTableau(c);
    }

    private void consentir(Joueur j, String numero) {
        assertTrue(service.enregistrerContact(tournoi, j.getNumeroLicence(), numero, true, T).estReussi());
    }

    // ---------------- limite ----------------

    @Test
    void limite_exactement_atteinte_le_dernier_part_le_suivant_est_refuse() {
        consentir(hugo, "0612345678");
        consentir(lea, "0712345678");
        tournoi.getConvocationsSms().setLimiteEnvois(2);
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, match, 1, T);
        assertEquals(2, r.nombreEnvoyes());
        assertFalse(r.limiteAtteinte(), "deux envois pour une limite de deux : rien de refusé");
        ResultatConvocationsSms suite = service.convoquerJoueurs(tournoi, match, 1, T);
        assertEquals(0, suite.nombreEnvoyes());
        assertTrue(suite.limiteAtteinte());
        assertEquals(2, envois.size());
    }

    @Test
    void limite_zero_rien_ne_part() {
        consentir(hugo, "0612345678");
        tournoi.getConvocationsSms().setLimiteEnvois(0);
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, match, 1, T);
        assertTrue(envois.isEmpty());
        assertEquals(StatutEnvoiSms.LIMITE_ATTEINTE, r.messages().get(0).statut());
    }

    @Test
    void limite_hors_bornes_refusee() {
        assertThrows(IllegalArgumentException.class, () -> tournoi.getConvocationsSms().setLimiteEnvois(-1));
        assertThrows(IllegalArgumentException.class, () -> tournoi.getConvocationsSms().setLimiteEnvois(5001));
        tournoi.getConvocationsSms().setLimiteEnvois(5000);
    }

    @Test
    void arbitre_compte_dans_la_meme_limite() {
        consentir(hugo, "0612345678");
        service.enregistrerContact(tournoi, "A9", "0698765432", true, T);
        tournoi.getConvocationsSms().setLimiteEnvois(1);
        service.convoquerJoueurs(tournoi, match, 1, T);
        ResultatConvocationsSms r = service.convoquerArbitre(tournoi, "A9", "Paul", "Paul MARTIN", match, 1, T);
        assertTrue(r.limiteAtteinte());
        assertEquals(1, envois.size());
    }

    @Test
    void sans_numero_rien_n_est_journalise_ni_envoye_et_le_joueur_est_signale() {
        ResultatConvocationsSms r = service.convoquerJoueurs(tournoi, match, 1, T);
        assertTrue(envois.isEmpty());
        assertTrue(tournoi.getConvocationsSms().getJournal().isEmpty());
        assertEquals(2, r.sansNumero().size());
    }

    // ---------------- consentement et effacement ----------------

    @Test
    void retrait_puis_convocation_ne_part_pas() {
        consentir(hugo, "0612345678");
        assertTrue(service.retirerContact(tournoi, "1"));
        assertFalse(service.retirerContact(tournoi, "1"));
        assertFalse(service.retirerContact(tournoi, null));
        service.convoquerJoueurs(tournoi, match, 1, T);
        assertTrue(envois.isEmpty());
    }

    @Test
    void numero_invalide_avec_consentement_ne_laisse_rien() {
        assertEquals(ResultatContactSms.Motif.NUMERO_INVALIDE,
                service.enregistrerContact(tournoi, "1", "0512345678", true, T).motif());
        assertNull(tournoi.getConvocationsSms().contact("1"));
    }

    @Test
    void sans_consentement_un_numero_deja_consenti_n_est_pas_remplace() {
        consentir(hugo, "0612345678");
        service.enregistrerContact(tournoi, "1", "0799999999", false, T);
        assertEquals("+33612345678", tournoi.getConvocationsSms().contact("1").numero());
    }

    @Test
    void effacement_par_la_date_de_fin_et_numeros_absents_du_journal() {
        consentir(hugo, "0612345678");
        service.convoquerJoueurs(tournoi, match, 1, T);
        tournoi.setPeriode(JOUR, JOUR.plusDays(2));
        assertEquals(JOUR.plusDays(3), ServiceConvocationsSms.dateEffacementAutomatique(tournoi));
        assertEquals(0, service.effacerSiTermine(tournoi, JOUR.plusDays(2).atTime(23, 59)));
        assertEquals(1, service.effacerSiTermine(tournoi, JOUR.plusDays(3).atStartOfDay()));
        for (MessageSms m : tournoi.getConvocationsSms().getJournal()) {
            assertFalse((m.texte() + m.detail() + m.destinataire() + m.numeroLicence()).contains("612345678"));
        }
    }

    @Test
    void jours_non_ordonnes_le_dernier_compte() {
        tournoi.ajouterJour(JOUR.plusDays(5));
        tournoi.ajouterJour(JOUR.plusDays(1));
        assertEquals(JOUR.plusDays(6), ServiceConvocationsSms.dateEffacementAutomatique(tournoi));
        assertFalse(ServiceConvocationsSms.estTermine(tournoi, JOUR.plusDays(5)));
        assertTrue(ServiceConvocationsSms.estTermine(tournoi, JOUR.plusDays(6)));
    }

    @Test
    void effacement_sur_tournoi_vide_ne_fait_rien() {
        assertEquals(0, service.effacerSiTermine(tournoi, JOUR.plusYears(1).atStartOfDay()));
        assertNull(tournoi.getConvocationsSms().getNumerosEffacesLe());
        assertEquals(0, service.effacerNumeros(tournoi, T));
    }

    // ---------------- jamais d'appel, déclenchement ----------------

    @Test
    void jamais_d_appel_ni_pour_rien_ni_pour_appel_vocal() {
        consentir(hugo, "0612345678");
        tournoi.getConvocationsSms().setEnvoiAutomatique(true);
        for (ModeAnnonce m : ModeAnnonce.values()) {
            envois.clear();
            service.apresLancement(tournoi, match, 1, m, T);
            assertEquals(m.avecSms() ? 1 : 0, envois.size(), m.name());
        }
        assertTrue(service.apresLancement(tournoi, match, 1, null, T).messages().isEmpty());
    }

    // ---------------- expéditeur ----------------

    @Test
    void expediteur_limites() {
        assertTrue(ExpediteurSms.estValide("ABC"));
        assertTrue(ExpediteurSms.estValide("ABCDEFGHIJK"));
        assertFalse(ExpediteurSms.estValide("ABCDEFGHIJKL"));
        assertFalse(ExpediteurSms.estValide("AB"));
        assertFalse(ExpediteurSms.estValide("12345678"));
        assertFalse(ExpediteurSms.estValide("TT Brunoy"));
        assertFalse(ExpediteurSms.estValide("TTÉVRY"));
        assertFalse(ExpediteurSms.estValide(null));
        assertEquals("PINGMANAGER", ExpediteurSms.proposer(null));
        assertEquals("PINGMANAGER", ExpediteurSms.proposer("  "));
        assertEquals("PINGMANAGER", ExpediteurSms.proposer("12345"));
        assertEquals("PINGMANAGER", ExpediteurSms.proposer("TT"));
        assertEquals("ESPERANCEDE", ExpediteurSms.proposer("Espérance de Brunoy"));
        assertEquals("USEVRY", ExpediteurSms.proposer("US Évry"));
        assertTrue(ExpediteurSms.estValide(ExpediteurSms.proposer("Œuvres 1234567890123")));
    }

    @Test
    void expediteur_choisi_trop_long_refuse_et_envoi_utilise_celui_du_club() {
        assertFalse(service.definirExpediteur(tournoi, "ABCDEFGHIJKL"));
        assertTrue(service.definirExpediteur(tournoi, "  MONCLUB  "));
        assertEquals("MONCLUB", ServiceConvocationsSms.expediteur(tournoi));
        assertTrue(service.definirExpediteur(tournoi, ""));
        assertEquals("TTBRUNOY", ServiceConvocationsSms.expediteur(tournoi));
        consentir(hugo, "0612345678");
        service.convoquerJoueurs(tournoi, match, 1, T);
        assertTrue(envois.get(0).startsWith("TTBRUNOY|+33612345678|"));
    }

    // ---------------- textes ----------------

    @Test
    void longueur_gsm7_frontieres() {
        assertEquals(1, TexteSms.nombreDeSms("a".repeat(160)));
        assertEquals(2, TexteSms.nombreDeSms("a".repeat(161)));
        assertEquals(2, TexteSms.nombreDeSms("a".repeat(306)));
        assertEquals(3, TexteSms.nombreDeSms("a".repeat(307)));
        assertEquals(0, TexteSms.nombreDeSms(""));
        assertEquals(1, TexteSms.nombreDeSms("€".repeat(80)));
        assertEquals(2, TexteSms.nombreDeSms("€".repeat(81)));
    }

    @Test
    void longueur_ucs2_frontieres() {
        assertFalse(TexteSms.estGsm7("ê"));
        assertEquals(1, TexteSms.nombreDeSms("ê".repeat(70)));
        assertEquals(2, TexteSms.nombreDeSms("ê".repeat(71)));
        assertEquals(2, TexteSms.nombreDeSms("ê".repeat(134)));
        assertEquals(3, TexteSms.nombreDeSms("ê".repeat(135)));
        assertEquals(2, TexteSms.nombreDeSms("a".repeat(100) + "ê"), "un seul caractère hors GSM bascule en UCS-2");
    }

    @Test
    void translitteration() {
        assertEquals("Noel à Évry, café: oeuvre l'été", TexteSms.versGsm7("Noël à Évry, café: œuvre l’été"));
        assertEquals("a", TexteSms.versGsm7("ã"));
        assertEquals("Ça", TexteSms.versGsm7("Ça"));
        assertEquals("c", TexteSms.versGsm7("ç"));
        assertEquals("\"x\" - y...", TexteSms.versGsm7("«x» — y…"));
        assertTrue(TexteSms.estGsm7(TexteSms.versGsm7("Łukasz Дима 日本 😀")));
        assertEquals("", TexteSms.versGsm7(""));
    }

    @Test
    void convocation_avec_noms_demesures_reste_un_seul_sms_gsm7() {
        Joueur long1 = new Joueur("3", "DE LA FONTAINE-DUPONT-MARTIN-ÉLOI".repeat(3), "Jean-Baptiste-Émile".repeat(3),
                Sexe.HOMME, "Association Sportive de Tennis de Table de Saint-Rémy-lès-Chevreuse", 800);
        String t = TexteConvocation.joueur(hugo, long1, 12, null);
        assertTrue(TexteSms.estGsm7(t));
        assertTrue(TexteSms.nombreDeSms(t) == 1, t);
        String a = TexteConvocation.arbitre("Zoé", long1, long1, 12, null);
        assertTrue(TexteSms.estGsm7(a));
        assertEquals(1, TexteSms.nombreDeSms(a), a);
    }

    @Test
    void convocation_prenom_vide_ou_nul() {
        Joueur sansPrenom = new Joueur("4", "ROY", "", Sexe.HOMME, "", 500);
        assertTrue(TexteConvocation.joueur(sansPrenom, hugo, 1, null).startsWith("Bonjour, "));
        assertTrue(TexteConvocation.arbitre(null, hugo, lea, 1, null).startsWith("Bonjour, "));
    }

    // ---------------- numéros ----------------

    @Test
    void numeros_francais_etrangers_invalides() {
        assertEquals("+33612345678", NumeroPortable.normaliser("06.12.34.56.78").orElseThrow());
        assertEquals("+33612345678", NumeroPortable.normaliser("+33 6 12 34 56 78").orElseThrow());
        assertEquals("+33712345678", NumeroPortable.normaliser("0033 7 12 34 56 78").orElseThrow());
        assertEquals("+447911123456", NumeroPortable.normaliser("+44 7911 123456").orElseThrow());
        assertEquals("+32470123456", NumeroPortable.normaliser("0032 470 12 34 56").orElseThrow());
        for (String mauvais : new String[]{null, "", "  ", "0512345678", "+33512345678", "061234567",
                "06123456789", "+3361234567", "+330612345678", "0612345678x", "+1234567", "+1234567890123456",
                "+0123456789", "abc", "06 12 34 56 78 90", "++33612345678", "٠٦١٢٣٤٥٦٧٨"}) {
            assertTrue(NumeroPortable.normaliser(mauvais).isEmpty(), "refusé : " + mauvais);
        }
    }

    @Test
    void masque_ne_montre_pas_le_numero_entier() {
        assertEquals("+33 6 •• •• •• 78", NumeroPortable.masquer("+33612345678"));
        assertFalse(NumeroPortable.masquer("+447911123456").contains("7911"));
        assertEquals("••", NumeroPortable.masquer(null));
        assertEquals("••", NumeroPortable.masquer("+3"));
    }
}
