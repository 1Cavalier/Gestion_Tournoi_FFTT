package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Couleur des matchs proposés (cahier des charges v0, section 7) : rouge si
 * l'attente est longue, orange si le match est important, attend depuis un
 * moment ou serait envoyé maintenant par la stratégie, vert sinon.
 */
class ReglePrioriteSalleTest {

    private static Duration min(int n) {
        return Duration.ofMinutes(n);
    }

    @Test
    void match_qui_vient_d_etre_pret_sans_rien_de_particulier_est_vert() {
        ReglePrioriteSalle.Evaluation e = ReglePrioriteSalle.evaluer(min(3), null, false);
        assertEquals(NiveauPriorite.NORMAL, e.niveau());
        assertEquals("", e.motif());
        assertEquals(NiveauPriorite.NORMAL, ReglePrioriteSalle.evaluer(null, "", false).niveau(),
                "attente inconnue comptée nulle");
        assertEquals(NiveauPriorite.NORMAL, ReglePrioriteSalle.evaluer(min(-5), null, false).niveau(),
                "attente négative (horloge décalée) comptée nulle");
    }

    @Test
    void attente_longue_passe_au_rouge_des_le_seuil() {
        assertEquals(NiveauPriorite.IMPORTANT, ReglePrioriteSalle.evaluer(min(19), null, false).niveau());
        ReglePrioriteSalle.Evaluation e = ReglePrioriteSalle.evaluer(ReglePrioriteSalle.ATTENTE_LONGUE, null, false);
        assertEquals(NiveauPriorite.URGENT, e.niveau());
        assertEquals("attend depuis 20 min", e.motif());
    }

    @Test
    void attente_moyenne_passe_a_l_orange() {
        assertEquals(NiveauPriorite.NORMAL, ReglePrioriteSalle.evaluer(min(9), null, false).niveau());
        ReglePrioriteSalle.Evaluation e = ReglePrioriteSalle.evaluer(min(10), null, false);
        assertEquals(NiveauPriorite.IMPORTANT, e.niveau());
        assertEquals("attend depuis 10 min", e.motif());
    }

    @Test
    void match_important_orange_puis_rouge_s_il_attend() {
        ReglePrioriteSalle.Evaluation finale = ReglePrioriteSalle.evaluer(min(2), "Finale", false);
        assertEquals(NiveauPriorite.IMPORTANT, finale.niveau());
        assertEquals("Finale", finale.motif());
        ReglePrioriteSalle.Evaluation attend = ReglePrioriteSalle.evaluer(min(12), "Finale", false);
        assertEquals(NiveauPriorite.URGENT, attend.niveau());
        assertEquals("attend depuis 12 min, Finale", attend.motif());
    }

    @Test
    void choisi_par_la_strategie_passe_a_l_orange_avec_son_motif() {
        ReglePrioriteSalle.Evaluation e = ReglePrioriteSalle.evaluer(min(0), null, true);
        assertEquals(NiveauPriorite.IMPORTANT, e.niveau());
        assertEquals("prochain à envoyer", e.motif());
        // La stratégie ne fait pas monter au rouge à elle seule.
        assertEquals(NiveauPriorite.IMPORTANT, ReglePrioriteSalle.evaluer(min(15), null, true).niveau());
        assertEquals(NiveauPriorite.URGENT, ReglePrioriteSalle.evaluer(min(25), null, true).niveau());
    }
}
