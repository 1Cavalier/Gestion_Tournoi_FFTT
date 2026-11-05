package com.pingmanager.ui.ecran;

import com.pingmanager.model.StatutTournoi;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatutPastilleTest {

    @Test
    void chaque_statut_porte_une_classe_css_distincte_prefixee() {
        Set<String> classes = new HashSet<>();
        for (StatutPastille s : StatutPastille.values()) {
            assertTrue(s.classeCss().startsWith("pastille-"));
            assertTrue(classes.add(s.classeCss()), "classe en double : " + s.classeCss());
        }
    }

    @Test
    void publie_en_vert_annule_en_rouge_brouillon_en_gris() {
        assertEquals(StatutPastille.SUCCES, StatutPastille.pourTournoi(StatutTournoi.PUBLIE));
        assertEquals(StatutPastille.ERREUR, StatutPastille.pourTournoi(StatutTournoi.ANNULE));
        assertEquals(StatutPastille.NEUTRE, StatutPastille.pourTournoi(StatutTournoi.BROUILLON));
        assertEquals(StatutPastille.INFO, StatutPastille.pourTournoi(StatutTournoi.HOMOLOGUE));
        assertEquals(StatutPastille.INFO, StatutPastille.pourTournoi(StatutTournoi.DEMANDE_HOMOLOGATION));
    }

    @Test
    void chaque_statut_de_tournoi_a_un_libelle() {
        for (StatutTournoi s : StatutTournoi.values()) {
            assertFalse(StatutPastille.libelleTournoi(s).isBlank());
        }
        assertEquals("Publié", StatutPastille.libelleTournoi(StatutTournoi.PUBLIE));
    }
}
