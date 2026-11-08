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
    void libelle_d_un_match_sur_table() {
        Match m = new Match(new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "", 0),
                new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "", 0));
        m.setNumeroTable(4);
        assertEquals("Table 4 · Hugo MORIN – Léa DURAND", PresentationConvocationsSms.libelleMatch(m));
    }
}
