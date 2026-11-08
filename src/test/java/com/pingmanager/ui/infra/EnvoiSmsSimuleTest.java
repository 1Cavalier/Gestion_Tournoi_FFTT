package com.pingmanager.ui.infra;

import com.pingmanager.core.ResultatEnvoiSms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Simulation d'envoi de SMS : rien ne part, le message est retenu numéro masqué. */
class EnvoiSmsSimuleTest {

    @Test
    void message_retenu_avec_numero_masque_et_identifiant() {
        EnvoiSmsSimule s = new EnvoiSmsSimule();
        ResultatEnvoiSms r = s.envoyer("TTBRUNOY", "+33612345678", "Hugo, vous etes attendu(e) table 4");
        assertTrue(r.reussi());
        assertEquals("simulation-1", r.identifiant());
        EnvoiSmsSimule.Message m = s.messages().get(0);
        assertEquals("TTBRUNOY", m.expediteur());
        assertEquals("+33 6 •• •• •• 78", m.numeroMasque());
        assertEquals("Hugo, vous etes attendu(e) table 4", m.texte());
    }

    @Test
    void memoire_bornee() {
        EnvoiSmsSimule s = new EnvoiSmsSimule();
        for (int i = 0; i < EnvoiSmsSimule.CAPACITE + 5; i++) {
            s.envoyer("X", "+33612345678", "m" + i);
        }
        assertEquals(EnvoiSmsSimule.CAPACITE, s.messages().size());
        assertEquals("m5", s.messages().get(0).texte());
    }
}
