package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Jour J v2 (B037) : cas limites des seuils de couleur et de l'annonce. */
class ReglePrioriteSalleLimitesTest {

    private static NiveauPriorite niveau(Duration a, String imp, boolean strat) {
        return ReglePrioriteSalle.evaluer(a, imp, strat).niveau();
    }

    @Test
    void seuils_juste_en_dessous_et_au_seuil() {
        assertEquals(NiveauPriorite.NORMAL, niveau(Duration.ofSeconds(599), null, false));
        assertEquals(NiveauPriorite.IMPORTANT, niveau(Duration.ofSeconds(600), null, false));
        assertEquals(NiveauPriorite.IMPORTANT, niveau(Duration.ofSeconds(1199), null, false));
        assertEquals(NiveauPriorite.URGENT, niveau(Duration.ofSeconds(1200), null, false));
        // Important + 10 min = rouge, important + 9 min 59 = orange.
        assertEquals(NiveauPriorite.IMPORTANT, niveau(Duration.ofSeconds(599), "finale", false));
        assertEquals(NiveauPriorite.URGENT, niveau(Duration.ofSeconds(600), "finale", false));
    }

    @Test
    void attente_nulle_ou_negative_comptee_zero() {
        assertEquals(NiveauPriorite.NORMAL, niveau(null, null, false));
        assertEquals(NiveauPriorite.NORMAL, niveau(Duration.ofMinutes(-30), "", false));
        assertEquals(NiveauPriorite.NORMAL, niveau(Duration.ofMinutes(-30), "   ", false));
        assertEquals(NiveauPriorite.IMPORTANT, niveau(Duration.ZERO, "finale", false));
    }

    @Test
    void strategie_et_attente_longue_le_rouge_l_emporte_et_les_raisons_sont_listees() {
        ReglePrioriteSalle.Evaluation e = ReglePrioriteSalle.evaluer(Duration.ofMinutes(25), "finale", true);
        assertEquals(NiveauPriorite.URGENT, e.niveau());
        assertEquals("attend depuis 25 min, finale", e.motif());
        assertThrows(UnsupportedOperationException.class, () -> e.raisons().add("x"));
    }

    @Test
    void annonce_avec_joueur_a_determiner_ou_nul_et_mode_obligatoire() {
        Joueur a = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "C", 1000);
        Match m = new Match(a, Joueur.aDeterminer());
        AnnonceRencontre an = AnnonceRencontre.preparer(ModeAnnonce.SMS, m, 3, null);
        assertTrue(an.texte().contains("à déterminer"));
        assertTrue(an.aEnvoyer());
        assertThrows(NullPointerException.class, () -> AnnonceRencontre.preparer(null, m, 3, null));
        assertThrows(NullPointerException.class, () -> AnnonceRencontre.preparer(ModeAnnonce.AUCUNE, null, 3, null));
        assertFalse(AnnonceRencontre.preparer(ModeAnnonce.AUCUNE, m, 3, "  ").aEnvoyer());
    }
}
