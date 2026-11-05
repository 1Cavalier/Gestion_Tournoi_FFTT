package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Corrections du juge-arbitre le jour J : paiement annulé, résultat effacé. */
class CorrectionsJourJTest {

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club", 1000);
    }

    @Test
    void annuler_paiement_reserve_aux_inscrits() {
        Tableau tab = new Tableau("A", ConfigurationTableau.builder().placesMax(1).tailleMaxListeAttente(2).build());
        tab.inscrire(j("L1"));
        tab.inscrire(j("L2")); // en liste d'attente
        tab.marquerPaye(j("L1"));
        tab.annulerPaiement(j("L1"));
        assertFalse(tab.aPaye(j("L1")));
        assertThrows(IllegalArgumentException.class, () -> tab.annulerPaiement(j("L2")));
    }

    @Test
    void effacer_le_resultat_remet_le_match_a_jouer() {
        Match m = new Match(j("A"), j("B"));
        m.definirScore(List.of(new Manche(11, 3), new Manche(11, 3), new Manche(11, 3)));
        m.setInstantFin(java.time.LocalDateTime.of(2026, 11, 14, 10, 0));
        m.effacerResultat();
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertTrue(m.getManches().isEmpty());
        assertNull(m.getVainqueur());
        assertNull(m.getInstantFin());

        m.declarerIssue(IssueMatch.DISQUALIFICATION, j("A"));
        m.effacerResultat();
        assertEquals(IssueMatch.NORMALE, m.getIssue());
        assertNull(m.getJoueurFautif());
    }

    @Test
    void effacer_un_exempt_est_interdit() {
        assertThrows(IllegalStateException.class, () -> new Match(j("A"), null).effacerResultat());
    }
}
