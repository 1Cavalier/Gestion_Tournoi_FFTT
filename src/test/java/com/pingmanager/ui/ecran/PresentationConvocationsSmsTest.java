package com.pingmanager.ui.ecran;

import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Textes de l'écran des convocations par SMS. */
class PresentationConvocationsSmsTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 14, 10, 2);

    @Test
    void arbitres_proposes_avec_les_joueurs() {
        Tournoi t = new Tournoi("T", "TT Brunoy", LocalDate.of(2026, 11, 14));
        t.ajouterArbitre(new AffectationArbitre("9", "MARTIN", "Paul", "AR"));
        List<PresentationConvocationsSms.Personne> p = PresentationConvocationsSms.personnes(t);
        assertEquals(1, p.size());
        assertTrue(p.get(0).arbitre());
        assertEquals("Paul MARTIN (arbitre) · 9", p.get(0).toString());
        assertEquals("Paul MARTIN", PresentationConvocationsSms.nomDe(p, "9"));
        assertEquals("Licence 7", PresentationConvocationsSms.nomDe(p, "7"));
    }

    @Test
    void contact_compteur_et_journal_lisibles() {
        assertEquals("+33 6 •• •• •• 78 · accord du 14/11 10:02",
                PresentationConvocationsSms.detailContact(new ContactSms("1", "+33612345678", T)));
        ConvocationsSms c = new ConvocationsSms();
        assertEquals("0 SMS envoyé sur 300 autorisés", PresentationConvocationsSms.compteur(c));
        MessageSms m = new MessageSms(T, TypeConvocation.MATCH_POULE, "1", "Hugo MORIN", "x",
                StatutEnvoiSms.ENVOYE, "simulation-1");
        assertEquals("14/11 10:02 · Match de poule · Hugo MORIN · Envoyé (simulation-1)",
                PresentationConvocationsSms.enteteMessage(m));
    }

    @Test
    void tuiles_comptent_envois_refus_et_numeros() {
        ConvocationsSms c = new ConvocationsSms();
        c.enregistrerContact(new ContactSms("1", "+33612345678", T));
        c.journaliser(new MessageSms(T, TypeConvocation.MATCH_POULE, "1", "Hugo MORIN", "x", StatutEnvoiSms.ENVOYE, "s"));
        c.journaliser(new MessageSms(T, TypeConvocation.MATCH_POULE, "1", "Hugo MORIN", "x", StatutEnvoiSms.ECHEC, "panne"));
        List<PresentationConvocationsSms.Tuile> t = PresentationConvocationsSms.tuiles(c);
        assertEquals("1 / 300", t.get(0).valeur());
        assertEquals("1", t.get(1).valeur());
        assertEquals("non envoyé (échec ou limite)", t.get(1).libelle());
        assertEquals("numéro avec accord", t.get(2).libelle());
    }

    @Test
    void alerte_quand_la_limite_est_proche_puis_atteinte() {
        ConvocationsSms c = new ConvocationsSms();
        c.setLimiteEnvois(10);
        assertTrue(PresentationConvocationsSms.alerteLimite(c).isEmpty());
        for (int i = 0; i < 9; i++) {
            c.journaliser(new MessageSms(T, TypeConvocation.ARBITRAGE, "1", "", "x", StatutEnvoiSms.ENVOYE, ""));
        }
        assertEquals("Limite bientôt atteinte : encore 1 SMS possible.",
                PresentationConvocationsSms.alerteLimite(c).orElseThrow());
        c.journaliser(new MessageSms(T, TypeConvocation.ARBITRAGE, "1", "", "x", StatutEnvoiSms.ENVOYE, ""));
        assertTrue(PresentationConvocationsSms.alerteLimite(c).orElseThrow().startsWith("Limite atteinte"));
    }

    @Test
    void limite_saisie_bornee_et_confirmation_d_effacement() {
        assertEquals(250, PresentationConvocationsSms.limite(" 250 ").orElseThrow());
        assertTrue(PresentationConvocationsSms.limite("-1").isEmpty());
        assertTrue(PresentationConvocationsSms.limite("5001").isEmpty());
        assertTrue(PresentationConvocationsSms.limite("abc").isEmpty());
        assertEquals("Effacer les 3 numéros ? Plus aucun SMS ne partira vers ces personnes. Le journal des envois "
                + "reste (sans numéro).", PresentationConvocationsSms.confirmationEffacement(3));
        assertTrue(PresentationConvocationsSms.confirmationEffacement(1).startsWith("Effacer le numéro ?"));
    }

    @Test
    void journal_le_plus_recent_d_abord_limite_et_statuts_en_pastilles() {
        ConvocationsSms c = new ConvocationsSms();
        for (int i = 0; i < PresentationConvocationsSms.JOURNAL_AFFICHE + 2; i++) {
            c.journaliser(new MessageSms(T.plusMinutes(i), TypeConvocation.MATCH_POULE, "1", "", "x",
                    StatutEnvoiSms.ENVOYE, ""));
        }
        List<MessageSms> j = PresentationConvocationsSms.journalAffiche(c);
        assertEquals(PresentationConvocationsSms.JOURNAL_AFFICHE, j.size());
        assertEquals(T.plusMinutes(PresentationConvocationsSms.JOURNAL_AFFICHE + 1), j.get(0).instant());
        assertEquals("… et 2 envois plus anciens (gardés dans le fichier du tournoi).",
                PresentationConvocationsSms.resteJournal(c).orElseThrow());
        assertEquals("Licence 1", PresentationConvocationsSms.destinataire(j.get(0)));
        assertEquals("14/11 10:02 · Match de poule", PresentationConvocationsSms.detailMessage(
                new MessageSms(T, TypeConvocation.MATCH_POULE, "1", "", "x", StatutEnvoiSms.ENVOYE, "")));
        assertEquals(StatutPastille.SUCCES, PresentationConvocationsSms.pastille(StatutEnvoiSms.ENVOYE));
        assertEquals(StatutPastille.ERREUR, PresentationConvocationsSms.pastille(StatutEnvoiSms.ECHEC));
        assertEquals(StatutPastille.ATTENTION, PresentationConvocationsSms.pastille(StatutEnvoiSms.LIMITE_ATTEINTE));
    }

    @Test
    void motif_d_echec_affiche_mais_pas_l_identifiant_d_un_envoi_reussi() {
        assertTrue(PresentationConvocationsSms.motif(new MessageSms(T, TypeConvocation.ARBITRAGE, "1", "", "x",
                StatutEnvoiSms.ENVOYE, "simulation-3")).isEmpty());
        assertEquals("Motif : limite de 300 SMS.", PresentationConvocationsSms.motif(new MessageSms(T,
                TypeConvocation.ARBITRAGE, "1", "", "x", StatutEnvoiSms.LIMITE_ATTEINTE, "limite de 300 SMS"))
                .orElseThrow());
        assertEquals("5 caractères · 1 SMS", PresentationConvocationsSms.tailleTexte("Table"));
    }

    @Test
    void libelle_d_un_match_sur_table() {
        Match m = new Match(new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "", 0),
                new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "", 0));
        m.setNumeroTable(4);
        assertEquals("Table 4 · Hugo MORIN – Léa DURAND", PresentationConvocationsSms.libelleMatch(m));
    }
}
