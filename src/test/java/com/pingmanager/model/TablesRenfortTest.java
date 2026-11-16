package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** B054 : tables d'échauffement utilisables en renfort, jamais plus que la salle d'échauffement. */
class TablesRenfortTest {

    private static Tournoi tournoi() {
        return new Tournoi("Tournoi de Brunoy", "TT Brunoy", LocalDate.of(2026, 11, 14));
    }

    @Test
    void par_defaut_aucune_table_de_renfort() {
        assertEquals(0, tournoi().getTablesRenfort());
    }

    @Test
    void renfort_de_zero_au_nombre_de_tables_d_echauffement() {
        Tournoi t = tournoi();
        t.setTablesEchauffement(8);
        t.setTablesRenfort(0);
        assertEquals(0, t.getTablesRenfort());
        t.setTablesRenfort(8);
        assertEquals(8, t.getTablesRenfort());
    }

    @Test
    void renfort_superieur_a_la_salle_d_echauffement_refuse() {
        Tournoi t = tournoi();
        t.setTablesEchauffement(4);
        assertThrows(IllegalArgumentException.class, () -> t.setTablesRenfort(5));
        assertEquals(0, t.getTablesRenfort(), "rien n'est modifié en cas de refus");
    }

    @Test
    void renfort_sans_salle_d_echauffement_refuse() {
        Tournoi t = tournoi();
        assertThrows(IllegalArgumentException.class, () -> t.setTablesRenfort(1));
    }

    @Test
    void renfort_negatif_refuse() {
        Tournoi t = tournoi();
        t.setTablesEchauffement(4);
        assertThrows(IllegalArgumentException.class, () -> t.setTablesRenfort(-1));
    }

    @Test
    void reduire_la_salle_d_echauffement_reduit_le_renfort() {
        Tournoi t = tournoi();
        t.setTablesEchauffement(8);
        t.setTablesRenfort(6);
        t.setTablesEchauffement(3);
        assertEquals(3, t.getTablesRenfort());
        t.setTablesEchauffement(0);
        assertEquals(0, t.getTablesRenfort());
    }
}
