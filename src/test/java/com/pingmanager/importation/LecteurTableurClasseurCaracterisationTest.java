package com.pingmanager.importation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pingmanager.importation.LecteurTableur.Classeur;
import com.pingmanager.importation.LecteurTableur.Contenu;
import com.pingmanager.importation.LecteurTableur.Format;
import com.pingmanager.importation.LecteurTableur.ImportException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de caractérisation : ils figent le comportement de
 * {@link LecteurTableur#lireClasseur} et de la lecture par chemin avant la
 * refonte de nuit du 13 novembre 2026.
 */
class LecteurTableurClasseurCaracterisationTest {

    private static byte[] octets(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void csv_donne_un_classeur_d_une_seule_feuille_nommee_feuille_1() throws Exception {
        Classeur c = LecteurTableur.lireClasseur("joueurs.csv", octets("Nom;Prénom\nDURAND;Paul\n"));
        assertEquals(Format.CSV, c.format());
        assertEquals(List.of("Feuille 1"), List.copyOf(c.feuilles().keySet()));
        assertEquals(List.of(List.of("Nom", "Prénom"), List.of("DURAND", "Paul")), c.feuilles().get("Feuille 1"));
    }

    @Test
    void json_liste_simple_donne_la_seule_feuille_joueurs() throws Exception {
        Classeur c = LecteurTableur.lireClasseur("joueurs.JSON",
                octets("[{\"nom\":\"DURAND\",\"prenom\":\"Paul\"}]"));
        assertEquals(Format.JSON, c.format());
        assertEquals(List.of("Joueurs"), List.copyOf(c.feuilles().keySet()));
        assertEquals(LecteurTableur.lire("joueurs.json", octets("[{\"nom\":\"DURAND\",\"prenom\":\"Paul\"}]")).lignes(),
                c.feuilles().get("Joueurs"));
    }

    @Test
    void json_objet_tableaux_et_joueurs_donne_deux_feuilles_dans_cet_ordre() throws Exception {
        String texte = "{\"tableaux\":[{\"nom\":\"A\",\"max\":\"899\"}],"
                + "\"joueurs\":[{\"nom\":\"DURAND\",\"prenom\":\"Paul\"}]}";
        Classeur c = LecteurTableur.lireClasseur("classeur.json", octets(texte));
        assertEquals(List.of("Tableaux", "Joueurs"), List.copyOf(c.feuilles().keySet()));
        assertEquals(List.of(List.of("nom", "max"), List.of("A", "899")), c.feuilles().get("Tableaux"));
        assertEquals("Tableaux", c.nomFeuille("tableau"));
        assertEquals("Joueurs", c.nomFeuille("JOUEUR"));
        assertNull(c.nomFeuille("absente"));
    }

    @Test
    void json_objet_tableaux_seuls_ne_cree_pas_de_feuille_joueurs() throws Exception {
        Classeur c = LecteurTableur.lireClasseur("classeur.json", octets("{\"tableaux\":[{\"nom\":\"A\"}]}"));
        assertEquals(List.of("Tableaux"), List.copyOf(c.feuilles().keySet()));
    }

    @Test
    void json_illisible_est_refuse_avec_un_message_qui_commence_par_json_illisible() {
        ImportException e = assertThrows(ImportException.class,
                () -> LecteurTableur.lireClasseur("classeur.json", octets("{\"tableaux\": [")));
        assertTrue(e.getMessage().startsWith("JSON illisible : "), e.getMessage());
    }

    @Test
    void lecture_par_chemin_identique_a_la_lecture_par_octets(@TempDir Path dossier) throws Exception {
        Path f = dossier.resolve("joueurs.csv");
        Files.write(f, octets("Nom,Prénom\nDURAND,Paul\n"));
        Contenu parChemin = LecteurTableur.lire(f);
        assertEquals(LecteurTableur.lire("joueurs.csv", Files.readAllBytes(f)), parChemin);
        assertEquals(LecteurTableur.lireClasseur("joueurs.csv", Files.readAllBytes(f)), LecteurTableur.lireClasseur(f));
    }

    @Test
    void fichier_absent_est_refuse_avec_lecture_impossible(@TempDir Path dossier) {
        Path absent = dossier.resolve("absent.csv");
        assertTrue(assertThrows(ImportException.class, () -> LecteurTableur.lire(absent))
                .getMessage().startsWith("Lecture impossible : "));
        assertTrue(assertThrows(ImportException.class, () -> LecteurTableur.lireClasseur(absent))
                .getMessage().startsWith("Lecture impossible : "));
    }

    @Test
    void extension_inconnue_et_ancien_xls_sont_refuses() {
        assertEquals("Format non reconnu : utilisez un fichier .xlsx, .ods, .csv ou .json.",
                assertThrows(ImportException.class, () -> LecteurTableur.lire("joueurs.pdf", octets("x"))).getMessage());
        assertThrows(ImportException.class, () -> LecteurTableur.lire("joueurs.xls", octets("x")));
        assertThrows(ImportException.class, () -> LecteurTableur.lireClasseur("joueurs.xlsx", octets("pas un zip")));
        assertThrows(ImportException.class, () -> LecteurTableur.lireClasseur("joueurs.ods", octets("pas un zip")));
    }
}
