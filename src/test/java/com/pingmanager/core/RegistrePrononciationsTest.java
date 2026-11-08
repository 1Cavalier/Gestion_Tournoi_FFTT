package com.pingmanager.core;

import com.pingmanager.model.PrononciationJoueur;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrePrononciationsTest {

    private final RegistrePrononciations registre = new RegistrePrononciations();

    @Test
    void joueur_enregistre_par_licence_puis_corrige_puis_efface() {
        ResultatSaisiePrononciation r = registre.definirJoueur(" 9412345 ", "ʒɑ̃", " dy.pɔ̃ ");
        assertTrue(r.valide());
        assertEquals("ʒɑ̃ dy.pɔ̃", r.ipa());
        assertEquals(new PrononciationJoueur("9412345", "ʒɑ̃", "dy.pɔ̃"), registre.joueur("9412345").orElseThrow());

        registre.definirJoueur("9412345", "", "dy.pɔ̃t");
        assertEquals("", registre.joueur("9412345").orElseThrow().prenom());
        assertEquals(1, registre.tousLesJoueurs().size());

        assertTrue(registre.definirJoueur("9412345", " ", null).valide());
        assertTrue(registre.joueur("9412345").isEmpty(), "deux champs vides : entrée effacée");
        assertTrue(registre.estVide());
    }

    @Test
    void saisie_refusee_ne_modifie_rien_et_motif_designe_le_champ() {
        registre.definirJoueur("1", "ʒɑ̃", "dy.pɔ̃");
        ResultatSaisiePrononciation r = registre.definirJoueur("1", "ʒɑ̃", "dy<pɔ̃");
        assertFalse(r.valide());
        assertTrue(r.motif().startsWith("Nom : "), r.motif());
        assertEquals("dy.pɔ̃", registre.joueur("1").orElseThrow().nom());
        assertTrue(registre.definirJoueur("1", "Jean", "").motif().startsWith("Prénom : "));
        assertFalse(registre.definirJoueur("  ", "ʒɑ̃", "").valide(), "licence obligatoire");
    }

    @Test
    void club_enregistre_par_numero_puis_efface() {
        assertTrue(registre.definirClub("08910001", "TT Brunoy", "te.te bʁy.nwa").valide());
        assertEquals("te.te bʁy.nwa", registre.club(" 08910001").orElseThrow().ipa());
        assertEquals("TT Brunoy", registre.club("08910001").orElseThrow().nom());
        assertFalse(registre.definirClub("08910001", "TT Brunoy", "&").valide());
        assertEquals("te.te bʁy.nwa", registre.club("08910001").orElseThrow().ipa());
        assertFalse(registre.definirClub("", "X", "a").valide());
        assertTrue(registre.definirClub("08910001", "TT Brunoy", "").estEffacement());
        assertTrue(registre.club("08910001").isEmpty());
    }
}
