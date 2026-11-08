package com.pingmanager.importation;

import com.pingmanager.model.Club;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** B023 : lecture de clubs.csv et registre des clubs par fichier. */
class LecteurClubsCsvTest {

    private static final String ENTETES = "﻿N° club;Nom;Département;Adresse 1;Code postal;Ville;"
            + "Courriel officiel;Nom du responsable;Prénom du responsable\r\n";

    private static ResultatImportClubs lire(String lignes) throws Exception {
        return LecteurClubsCsv.lire("clubs.csv", (ENTETES + lignes).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void lit_un_club_complet_et_deduit_le_departement_du_code_postal() throws Exception {
        ResultatImportClubs r = lire("08910001;Brunoy TT;;Gymnase;91800;Brunoy;contact@brunoytt.fr;DUPONT;Jean\n"
                + "8200001;AJ Ajaccio;;;20000;Ajaccio;aj@exemple.fr;;\n");
        assertEquals(2, r.clubs().size(), r.anomalies()::toString);
        Club c = r.clubs().get(0);
        assertEquals("91", c.getDepartement());
        assertEquals("Gymnase", c.getAdresse1());
        assertEquals("DUPONT", c.getResponsableNom());
        assertEquals("08200001", r.clubs().get(1).getNumero());
        assertEquals("2A", r.clubs().get(1).getDepartement());
    }

    @Test
    void departement_depuis_code_postal() {
        assertEquals("75", LecteurClubsCsv.departementDepuis("75011"));
        assertEquals("974", LecteurClubsCsv.departementDepuis("97400"));
        assertEquals("2B", LecteurClubsCsv.departementDepuis("20200"));
        assertEquals("", LecteurClubsCsv.departementDepuis("abc"));
    }

    @Test
    void lignes_incompletes_refusees_avec_motif() throws Exception {
        ResultatImportClubs r = lire(
                "123;Court;;;91800;Brunoy;a@b.fr;;\n"            // 2 : numéro
                + "08910002;;;;91800;Brunoy;a@b.fr;;\n"          // 3 : nom
                + "08910003;Sans dep;;;;Brunoy;a@b.fr;;\n"       // 4 : ni département ni CP
                + "08910004;Sans ville;91;;;;a@b.fr;;\n"         // 5 : ville
                + "08910005;Sans mail;91;;;Brunoy;pas-un-mail;;\n" // 6 : courriel
                + "08910006;Bon;91;;;Brunoy;a@b.fr;;\n"
                + "08910006;Doublon;91;;;Brunoy;a@b.fr;;\n");    // 8 : doublon
        assertEquals(1, r.clubs().size());
        assertEquals(6, r.lignesRefusees());
        assertTrue(r.anomalies().stream().anyMatch(a -> a.ligne() == 6 && a.motif().contains("courriel")));
    }

    @Test
    void registre_trouve_et_recherche_par_nom_ville_ou_numero() throws Exception {
        RegistreClubsFichier reg = new RegistreClubsFichier(lire(
                "08910001;Brunoy Tennis de Table;;;91800;Brunoy;a@b.fr;;\n"
                + "07510002;Paris Ping;;;75011;Paris;p@b.fr;;\n"));
        assertTrue(reg.existe("08910001"));
        assertTrue(reg.trouverParNumero(" ").isEmpty());
        assertEquals("07510002", reg.rechercher("PARIS", 5).get(0).getNumero());
        assertEquals(1, reg.rechercher("0891", 5).size());
        assertEquals(2, reg.clubs().size());
    }
}
