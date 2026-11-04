package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * L'échelon d'homologation découle de la catégorie et de la dotation
 * (règlements sportifs IX.103 / IX.105) : il n'est jamais choisi à la main.
 */
class EchelonHomologationTest {

    @Test
    void national_et_international_relevent_de_la_federation() {
        assertEquals(EchelonHomologation.FFTT, EchelonHomologation.pour(NiveauTournoi.NATIONAL_B, 0));
        assertEquals(EchelonHomologation.FFTT, EchelonHomologation.pour(NiveauTournoi.NATIONAL_A, 0));
        assertEquals(EchelonHomologation.FFTT, EchelonHomologation.pour(NiveauTournoi.INTERNATIONAL, 0));
    }

    @Test
    void regional_departemental_promotionnel_relevent_de_la_ligue_jusqu_a_3000_euros() {
        assertEquals(EchelonHomologation.LIGUE, EchelonHomologation.pour(NiveauTournoi.REGIONAL, 3000));
        assertEquals(EchelonHomologation.LIGUE, EchelonHomologation.pour(NiveauTournoi.DEPARTEMENTAL, 0));
        assertEquals(EchelonHomologation.LIGUE, EchelonHomologation.pour(NiveauTournoi.PROMOTIONNEL, 500));
    }

    @Test
    void au_dela_de_3000_euros_la_federation_homologue_quel_que_soit_le_niveau() {
        assertEquals(EchelonHomologation.FFTT, EchelonHomologation.pour(NiveauTournoi.DEPARTEMENTAL, 3000.01));
        assertEquals(EchelonHomologation.FFTT, EchelonHomologation.pour(NiveauTournoi.REGIONAL, 4000));
    }

    @Test
    void sans_categorie_pas_d_echelon() {
        assertNull(EchelonHomologation.pour(null, 10000));
    }

    @Test
    void le_tournoi_calcule_son_echelon_et_ignore_une_ancienne_valeur_stockee() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 3, 13));
        t.setEchelonHomologation(EchelonHomologation.COMITE_DEPARTEMENTAL); // ancien fichier
        assertEquals(EchelonHomologation.COMITE_DEPARTEMENTAL, t.getEchelonHomologation());

        t.setNiveau(NiveauTournoi.NATIONAL_B);
        assertEquals(EchelonHomologation.FFTT, t.getEchelonHomologation());

        t.setNiveau(NiveauTournoi.REGIONAL);
        assertEquals(EchelonHomologation.LIGUE, t.getEchelonHomologation());
        t.setDotationGlobale(3500);
        assertEquals(EchelonHomologation.FFTT, t.getEchelonHomologation());
    }
}
