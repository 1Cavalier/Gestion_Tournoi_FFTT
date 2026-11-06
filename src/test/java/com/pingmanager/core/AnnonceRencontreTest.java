package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Annonce d'une rencontre envoyée sur une table : texte et façon de l'annoncer. */
class AnnonceRencontreTest {

    private static Joueur j(String licence, String nom, String prenom) {
        return new Joueur(licence, nom, prenom, Sexe.HOMME, "Club", 1000);
    }

    @Test
    void texte_de_l_annonce_avec_table_joueurs_et_contexte() {
        Match m = new Match(j("1", "MORIN", "Hugo"), j("2", "DURAND", "Léo"));
        AnnonceRencontre a = AnnonceRencontre.preparer(ModeAnnonce.SMS, m, 4, "C – Toutes séries · Poule B · tour 2");
        assertEquals(4, a.table());
        assertEquals(ModeAnnonce.SMS, a.mode());
        assertTrue(a.texte().startsWith("Table 4 : "), a.texte());
        assertTrue(a.texte().contains(" contre "), a.texte());
        assertTrue(a.texte().endsWith("(C – Toutes séries · Poule B · tour 2)."), a.texte());
        assertTrue(a.aEnvoyer());
    }

    @Test
    void sans_contexte_et_sans_envoi_pour_rien() {
        Match m = new Match(j("1", "MORIN", "Hugo"), j("2", "DURAND", "Léo"));
        AnnonceRencontre a = AnnonceRencontre.preparer(ModeAnnonce.AUCUNE, m, 1, null);
        assertTrue(a.texte().endsWith("."), a.texte());
        assertFalse(a.texte().contains("("));
        assertFalse(a.aEnvoyer());
    }

    @Test
    void modes_d_annonce_sms_appel_ou_les_deux() {
        assertFalse(ModeAnnonce.AUCUNE.avecSms() || ModeAnnonce.AUCUNE.avecAppel());
        assertTrue(ModeAnnonce.SMS.avecSms() && !ModeAnnonce.SMS.avecAppel());
        assertTrue(!ModeAnnonce.APPEL.avecSms() && ModeAnnonce.APPEL.avecAppel());
        assertTrue(ModeAnnonce.SMS_ET_APPEL.avecSms() && ModeAnnonce.SMS_ET_APPEL.avecAppel());
        assertEquals("Appel vocal", ModeAnnonce.APPEL.getLibelle());
    }
}
