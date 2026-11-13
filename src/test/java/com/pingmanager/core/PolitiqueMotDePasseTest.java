package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Politique de mot de passe (README section 5, point 15) : 10 caractères au moins. */
class PolitiqueMotDePasseTest {

    @Test
    void dix_caracteres_acceptes_neuf_refuses_avec_message_clair() {
        assertTrue(PolitiqueMotDePasse.estAcceptable("0123456789"));
        assertEquals(Optional.of("Le mot de passe doit contenir au moins 10 caractères (9 saisis)."),
                PolitiqueMotDePasse.motifRefus("012345678"));
    }

    @Test
    void mot_de_passe_vide_ou_absent_refuse() {
        assertFalse(PolitiqueMotDePasse.estAcceptable(""));
        assertFalse(PolitiqueMotDePasse.estAcceptable(null));
        assertEquals("Le mot de passe doit contenir au moins 10 caractères (0 saisi).",
                PolitiqueMotDePasse.motifRefus(null).orElseThrow());
    }

    @Test
    void caracteres_accentues_comptes_un_par_un() {
        assertTrue(PolitiqueMotDePasse.estAcceptable("éééééééééé"));
        assertFalse(PolitiqueMotDePasse.estAcceptable("😀😀😀😀😀"), "5 caractères, même s'ils occupent 10 unités");
    }
}
