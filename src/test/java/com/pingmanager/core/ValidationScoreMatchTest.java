package com.pingmanager.core;

import com.pingmanager.model.Manche;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règles du jeu appliquées à la saisie d'un score : 2.11.1 (une manche) et
 * 2.12.1 (une partie au meilleur d'un nombre impair de manches).
 */
class ValidationScoreMatchTest {

    private final ValidationScoreMatch validation = new ValidationScoreMatch();

    private static Manche m(int a, int b) {
        return new Manche(a, b);
    }

    @Test
    void trois_manches_a_zero_au_meilleur_des_cinq_est_valide() {
        assertTrue(validation.valider(List.of(m(11, 3), m(11, 9), m(11, 0)), 3).estValide());
    }

    @Test
    void cinq_manches_avec_prolongations_est_valide() {
        // 2.11.1 : après 10-10, deux points d'écart (12-10, 15-13).
        ResultatSaisieScore r = validation.valider(
                List.of(m(12, 10), m(9, 11), m(13, 15), m(11, 7), m(15, 13)), 3);
        assertTrue(r.estValide(), r.toString());
    }

    @Test
    void victoire_du_joueur_b_est_valide() {
        assertTrue(validation.valider(List.of(m(5, 11), m(11, 8), m(8, 11), m(10, 12)), 3).estValide());
    }

    @Test
    void aucune_manche_est_refusee() {
        assertEquals(ResultatSaisieScore.Motif.AUCUNE_MANCHE, validation.valider(List.of(), 3).getMotif());
    }

    @Test
    void manche_a_egalite_est_refusee_avec_son_numero() {
        ResultatSaisieScore r = validation.valider(List.of(m(11, 5), m(10, 10), m(11, 5)), 3);
        assertEquals(ResultatSaisieScore.Motif.MANCHE_A_EGALITE, r.getMotif());
        assertEquals(2, r.getNumeroManche());
    }

    @Test
    void manche_gagnee_avec_moins_de_onze_points_est_refusee() {
        ResultatSaisieScore r = validation.valider(List.of(m(9, 5), m(11, 5), m(11, 5)), 3);
        assertEquals(ResultatSaisieScore.Motif.MANCHE_SOUS_ONZE_POINTS, r.getMotif());
        assertEquals(1, r.getNumeroManche());
    }

    @Test
    void onze_dix_est_refuse_faute_de_deux_points_d_ecart() {
        ResultatSaisieScore r = validation.valider(List.of(m(11, 10), m(11, 5), m(11, 5)), 3);
        assertEquals(ResultatSaisieScore.Motif.MANCHE_ECART_INSUFFISANT, r.getMotif());
    }

    @Test
    void prolongation_avec_plus_de_deux_points_d_ecart_est_refusee() {
        // 13-9 est impossible : la manche se serait arrêtée à 11-9.
        assertEquals(ResultatSaisieScore.Motif.MANCHE_PROLONGATION_INCORRECTE,
                validation.controlerManche(m(13, 9)));
        assertEquals(ResultatSaisieScore.Motif.MANCHE_PROLONGATION_INCORRECTE,
                validation.controlerManche(m(12, 11)));
        assertNull(validation.controlerManche(m(14, 12)));
        assertNull(validation.controlerManche(m(11, 9)));
    }

    @Test
    void manche_jouee_apres_la_victoire_est_refusee() {
        ResultatSaisieScore r = validation.valider(List.of(m(11, 5), m(11, 5), m(11, 5), m(5, 11)), 3);
        assertEquals(ResultatSaisieScore.Motif.MANCHE_EN_TROP, r.getMotif());
        assertEquals(4, r.getNumeroManche());
    }

    @Test
    void match_non_decide_est_refuse() {
        ResultatSaisieScore r = validation.valider(List.of(m(11, 5), m(5, 11), m(11, 5)), 3);
        assertEquals(ResultatSaisieScore.Motif.MATCH_NON_DECIDE, r.getMotif());
        assertEquals(0, r.getNumeroManche());
    }

    @Test
    void meilleur_des_sept_exige_quatre_manches_gagnees() {
        assertEquals(ResultatSaisieScore.Motif.MATCH_NON_DECIDE,
                validation.valider(List.of(m(11, 5), m(11, 5), m(11, 5)), 4).getMotif());
        assertTrue(validation.valider(List.of(m(11, 5), m(11, 5), m(11, 5), m(11, 5)), 4).estValide());
    }
}
