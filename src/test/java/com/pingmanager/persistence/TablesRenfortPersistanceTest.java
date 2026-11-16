package com.pingmanager.persistence;

import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B054 : tables de renfort sauvegardées, anciens fichiers relus avec 0. */
class TablesRenfortPersistanceTest {

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Tournoi de Brunoy", "TT Brunoy", LocalDate.of(2026, 11, 14));
        t.setTablesEchauffement(8);
        t.setTablesRenfort(4);
        return t;
    }

    @Test
    void tables_de_renfort_relues_a_l_identique() throws Exception {
        Path fichier = Files.createTempFile("renfort", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(tournoi(), fichier);
        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
        assertEquals(8, relu.getTablesEchauffement());
        assertEquals(4, relu.getTablesRenfort());
    }

    @Test
    void ancien_fichier_sans_tables_de_renfort_relu_avec_zero() throws Exception {
        Path fichier = Files.createTempFile("renfort_ancien", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(tournoi(), fichier);
        String json = Files.readString(fichier).replaceAll("\"tablesRenfort\"\\s*:\\s*4\\s*,?", "");
        assertFalse(json.contains("tablesRenfort"));
        Files.writeString(fichier, json);
        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
        assertEquals(8, relu.getTablesEchauffement());
        assertEquals(0, relu.getTablesRenfort());
    }

    @Test
    void renfort_superieur_a_la_salle_d_echauffement_ramene_a_la_salle() throws Exception {
        Path fichier = Files.createTempFile("renfort_trop", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(tournoi(), fichier);
        String json = Files.readString(fichier);
        assertTrue(json.contains("\"tablesRenfort\""));
        Files.writeString(fichier, json.replaceAll("\"tablesRenfort\"\\s*:\\s*4", "\"tablesRenfort\": 99"));
        assertEquals(8, DepotTournoi.charger(fichier).getTournoi().getTablesRenfort());
    }
}
