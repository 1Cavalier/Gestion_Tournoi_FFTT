package com.pingmanager.importation;

import com.pingmanager.core.FichierExporte;
import com.pingmanager.core.ResultatsTournoi;
import com.pingmanager.core.ServiceResultatsFederaux;
import com.pingmanager.core.TournoiTermineDeTest;
import com.pingmanager.model.Manche;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B023 : export CSV provisoire des résultats d'un tableau terminé. */
class ExportResultatsCsvTest {

    private final ExportResultatsCsv export = new ExportResultatsCsv();

    private static ResultatsTournoi resultats(String nomJoueur1) {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H-2027-091", nomJoueur1);
        return new ServiceResultatsFederaux(() -> Instant.parse("2027-04-10T20:00:00Z"), ZoneId.of("Europe/Paris"))
                .construire(jeu.tournoi, jeu.termine).getResultats().orElseThrow();
    }

    private static String texte(FichierExporte f) {
        return new String(f.getContenu(), StandardCharsets.UTF_8);
    }

    @Test
    void produit_parties_et_classements_en_csv_point_virgule_utf8_avec_bom() {
        ResultatsTournoi r = resultats(null);
        List<FichierExporte> fichiers = export.produire(r);
        assertEquals("CSV provisoire PingManager v1", export.nomFormat());
        assertEquals(List.of("parties.csv", "classements.csv"), fichiers.stream().map(FichierExporte::getNom).toList());
        String parties = texte(fichiers.get(0));
        assertTrue(parties.startsWith("﻿N° homologation;Tableau;Phase;Étape;N° partie;"));
        String[] lignes = parties.split("\r\n");
        assertEquals(r.parties().size() + 1, lignes.length);
        assertTrue(lignes[1].startsWith("H-2027-091;A;POULE;"));
        assertTrue(parties.contains(";TABLEAU;"));
        assertTrue(parties.contains("11-4 9-11 11-4 11-4;3;1;"), "score du point de vue de A et manches");
        assertTrue(parties.contains(";NORMALE;"));
        String classements = texte(fichiers.get(1));
        assertTrue(classements.startsWith("﻿N° homologation;Tableau;Rang;Licence;Nom;Prénom;Club;Points\r\n"));
        assertTrue(classements.split("\r\n")[1].startsWith("H-2027-091;A;1;"));
    }

    @Test
    void relu_par_le_lecteur_de_tableur_meme_nombre_de_colonnes() throws Exception {
        List<FichierExporte> fichiers = export.produire(resultats("O'NEIL; \"Bob\""));
        List<List<String>> lignes = LecteurTableur.lire("parties.csv", fichiers.get(0).getContenu()).lignes();
        for (List<String> l : lignes) {
            assertEquals(ExportResultatsCsv.ENTETES_PARTIES.size(), l.size());
        }
        assertTrue(lignes.stream().anyMatch(l -> l.contains("O'NEIL; \"Bob\"")), "guillemets et « ; » échappés");
    }

    @Test
    void injection_de_formule_neutralisee() {
        String parties = texte(export.produire(resultats("=HYPERLINK(\"http://x\")")).get(0));
        assertFalse(parties.contains(";=HYPERLINK"));
        assertTrue(parties.contains(";\"'=HYPERLINK(\"\"http://x\"\")\";"));
        assertEquals("'+1", CsvFederal.cellule("+1"));
        assertEquals("'-1", CsvFederal.cellule("-1"));
        assertEquals("'@SUM(A1)", CsvFederal.cellule("@SUM(A1)"));
        assertEquals("\"a\r\nb\"", CsvFederal.cellule("a\r\nb"));
        assertEquals("11-7", CsvFederal.cellule("11-7"));
        assertEquals("", CsvFederal.cellule(null));
    }

    @Test
    void score_vide_pour_une_partie_sans_manche() {
        assertEquals("", ExportResultatsCsv.score(List.of()));
        assertEquals("11-7 9-11", ExportResultatsCsv.score(List.of(new Manche(11, 7), new Manche(9, 11))));
    }
}
