package com.pingmanager.ui.infra;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.feuilles.FeuillePoule;
import com.pingmanager.core.feuilles.GenerateurFeuilles;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feuille de poule imprimée au lancement d'une poule entière sur une table :
 * une seule page A4 pour les poules de 3 à 6, avec la table et l'heure de
 * lancement, les joueurs, les parties dans l'ordre I.301 et le classement final.
 */
class RenduPdfFeuillePouleDeTableTest {

    private final GenerateurFeuilles generateur = new GenerateurFeuilles();

    /** Tournoi d'une poule de {@code taille} joueurs, lancée en bloc sur la table 5 à 10 h 42 (heure du poste). */
    static Tournoi pouleLancee(int taille, int manchesGagnantes) {
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(FormatConfigurable.builder()
                .nom("P").identifiant("PT" + taille + manchesGagnantes).taillePouleSouhaitee(taille)
                .qualifiesParPoule(2).nombreManchesGagnantes(manchesGagnantes).build()).build();
        Tournoi t = new Tournoi("Tournoi national de Brunoy", "TT Brunoy", LocalDate.of(2027, 4, 10));
        t.setNombreDeTables(8);
        Tableau tableau = new Tableau("B – 500 à 1399 points", config);
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        String[] noms = {"DUPONT", "MARTIN", "DURAND", "LEFÈVRE", "MOREAU", "GARNIER"};
        for (int i = 0; i < taille; i++) {
            Joueur j = new Joueur("L" + i, noms[i], "Prénom" + i, Sexe.HOMME, "Club " + noms[i], 1390 - 37 * i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        Poule p = tableau.getPoules().get(0);
        java.time.LocalDateTime dixHeures = java.time.LocalDateTime.of(2027, 4, 10, 10, 42);
        Instant i = dixHeures.atZone(java.time.ZoneId.systemDefault()).toInstant();
        assertTrue(new ServiceSalle(() -> i).lancerPoule(t, p, 5).estReussi());
        // 2.12.1 : au meilleur des 7 manches pour 4 manches gagnantes.
        p.getMatchs().forEach(m -> m.setNombreManchesGagnantes(manchesGagnantes));
        return t;
    }

    private static String texte(byte[] pdf, int pagesAttendues) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(pagesAttendues, doc.getNumberOfPages());
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void feuille_de_poule_lancee_avec_table_et_heure_de_lancement() {
        Tournoi t = pouleLancee(3, 3);
        Tableau tableau = t.getTableaux().get(0);
        FeuillePoule f = generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille();
        assertEquals("Table 5", f.table());
        assertEquals("10 h 42", f.heureLancement());
    }

    @Test
    void poule_de_3_a_6_sur_une_seule_page_meme_en_4_manches_gagnantes() throws IOException {
        for (int manches : new int[] {3, 4}) {
            for (int taille = 3; taille <= 6; taille++) {
                Tournoi t = pouleLancee(taille, manches);
                Tableau tableau = t.getTableaux().get(0);
                FeuillePoule f = generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille();
                String s = texte(RenduPdfFeuilles.feuillePouleDeTable(f), 1);
                assertTrue(s.contains("Feuille de poule"), "poule de " + taille);
                assertTrue(s.contains("Table 5"));
                assertTrue(s.contains("Lancée à 10 h 42"));
                assertTrue(s.contains("(I.301)"));
                assertTrue(s.contains(FeuillePoule.TITRE_CLASSEMENT_FINAL));
                assertTrue(s.contains("DUPONT"));
                assertTrue(s.contains("1390 pts"), "points des joueurs");
                assertTrue(s.contains("Club DUPONT"), "club des joueurs");
                assertTrue(s.contains("M" + (2 * manches - 1)), "une colonne par manche possible (2.12.1)");
                // Les parties dans l'ordre où elles se jouent : la dernière est bien imprimée.
                assertTrue(s.contains(String.valueOf(f.parties().size())));
            }
        }
    }

    @Test
    void feuille_d_une_poule_pas_lancee_garde_des_cases_a_remplir() throws IOException {
        Tournoi t = pouleLancee(4, 3);
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);
        p.definirLancement(null, null);
        FeuillePoule f = generateur.feuillePoule(t, tableau, p).feuille();
        assertEquals("", f.table());
        String s = texte(RenduPdfFeuilles.feuillePouleDeTable(f), 1);
        assertTrue(s.contains("Table : ______"));
        // L'impression d'avant le jour J (feuille de poule puis feuille de rencontre) ne change pas.
        try (PDDocument doc = PDDocument.load(RenduPdfFeuilles.feuillePoule(f))) {
            assertTrue(doc.getNumberOfPages() >= 2);
        }
    }

    /** Exemples pour la relecture (Pull Request) : écrits dans le dossier temporaire, jamais dans le dépôt. */
    @Test
    void exemples_poule_de_3_et_de_6_ecrits_hors_du_depot() throws IOException {
        Path dossier = Path.of(System.getProperty("java.io.tmpdir"), "pingmanager-feuilles");
        Files.createDirectories(dossier);
        for (int taille : new int[] {3, 6}) {
            Tournoi t = pouleLancee(taille, 3);
            t.setAdresse(AdresseTournoi.de("Gymnase", "91800", "Brunoy"));
            Tableau tableau = t.getTableaux().get(0);
            byte[] pdf = RenduPdfFeuilles.feuillePouleDeTable(
                    generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille());
            Files.write(dossier.resolve("feuille-poule-de-" + taille + ".pdf"), pdf);
        }
        assertTrue(Files.exists(dossier.resolve("feuille-poule-de-6.pdf")));
    }
}
