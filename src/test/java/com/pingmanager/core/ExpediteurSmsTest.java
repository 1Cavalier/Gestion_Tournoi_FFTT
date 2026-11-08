package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Nom d'expéditeur des SMS (charte AF2M, docs/RECHERCHE_SMS.md). */
class ExpediteurSmsTest {

    @Test
    void expediteur_propose_d_apres_le_nom_du_club() {
        assertEquals("TTBRUNOY", ExpediteurSms.proposer("TT Brunoy"));
        assertEquals("USETETENNIS", ExpediteurSms.proposer("U.S. Été Tennis"));
        assertEquals("ASSOCIATION", ExpediteurSms.proposer("Association sportive de Brunoy"));
    }

    @Test
    void repli_sur_pingmanager_jamais_pingtournoi() {
        assertEquals("PINGMANAGER", ExpediteurSms.proposer(null));
        assertEquals("PINGMANAGER", ExpediteurSms.proposer("12"));
        assertEquals("PINGMANAGER", ExpediteurSms.proposer("1234567"), "uniquement des chiffres");
        assertNotEquals("PINGTOURNOI", ExpediteurSms.PAR_DEFAUT);
    }

    @Test
    void charte_trois_a_onze_lettres_ou_chiffres_avec_une_lettre() {
        assertTrue(ExpediteurSms.estValide("TTBRUNOY"));
        assertTrue(ExpediteurSms.estValide("PINGMANAGER"));
        assertFalse(ExpediteurSms.estValide("PINGMANAGER1"), "12 caractères");
        assertFalse(ExpediteurSms.estValide("TT BRUNOY"), "espace");
        assertFalse(ExpediteurSms.estValide("ÉTÉ"), "accent");
        assertFalse(ExpediteurSms.estValide("123456"), "chiffres seuls");
        assertFalse(ExpediteurSms.estValide("AB"));
        assertFalse(ExpediteurSms.estValide(null));
    }
}
