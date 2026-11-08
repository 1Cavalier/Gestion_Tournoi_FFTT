package com.pingmanager.ui.infra;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.feuilles.ArbreImprimable;
import com.pingmanager.core.feuilles.FeuilleMatch;
import com.pingmanager.core.feuilles.FeuillePoule;
import com.pingmanager.core.feuilles.GenerateurFeuilles;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Rendu PDF des feuilles : le document s'ouvre, a le bon nombre de pages et contient les textes attendus. */
class RenduPdfFeuillesTest {

    private final GenerateurFeuilles generateur = new GenerateurFeuilles();

    private static Tournoi tournoi(int n) {
        Tournoi t = new Tournoi("Tournoi d'Évry « test »", "Club", LocalDate.of(2026, 11, 14));
        Tableau tableau = new Tableau("E – ≤ 1599 pts",
                ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= n; i++) {
            Joueur j = new Joueur("L" + i, "Lefèvre" + i, "Zoé", Sexe.FEMME, "Ping Club Évry-Courcouronnes " + i,
                    600 + i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 3L);
        return t;
    }

    private static String texte(byte[] pdf, int pagesAttendues) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(pagesAttendues, doc.getNumberOfPages());
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void trois_feuilles_de_match_tiennent_sur_deux_pages() throws IOException {
        Tournoi t = tournoi(9);
        Tableau tableau = t.getTableaux().get(0);
        List<FeuilleMatch> feuilles = generateur.feuillesMatchsPoule(t, tableau, tableau.getPoules().get(0));
        assertTrue(feuilles.size() >= 3);

        String s = texte(RenduPdfFeuilles.feuillesMatch(feuilles.subList(0, 3)), 2);

        assertTrue(s.contains("Feuille de match"));
        assertTrue(s.contains("Manche 5"));
        assertTrue(s.contains("LEFÈVRE"));
        assertTrue(s.contains("Signature — Arbitre"));
    }

    @Test
    void aucune_feuille_de_match_est_refuse() {
        assertThrows(IllegalArgumentException.class, () -> RenduPdfFeuilles.feuillesMatch(List.of()));
    }

    @Test
    void feuille_de_poule_puis_feuille_de_rencontre() throws IOException {
        Tournoi t = tournoi(9);
        Tableau tableau = t.getTableaux().get(0);
        FeuillePoule f = generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille();

        byte[] pdf = RenduPdfFeuilles.feuillePoule(f);
        String s;
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 2);
            s = new PDFTextStripper().getText(doc);
        }
        assertTrue(s.contains("Feuille de poule"));
        assertTrue(s.contains("Ordre des parties (I.301)"));
        assertTrue(s.contains("Classement à remplir (I.303)"));
        assertTrue(s.contains("Feuille de rencontre"));
    }

    @Test
    void tableau_final_a4_paysage_puis_a3_au_dela_de_16_places() throws IOException {
        for (int n : new int[] {9, 30}) {
            Tournoi t = tournoi(n);
            Tableau tableau = t.getTableaux().get(0);
            for (Poule p : tableau.getPoules()) {
                for (Match m : p.getMatchs()) {
                    m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
                }
            }
            assertTrue(new ServicePhaseFinale().construire(tableau, 5L).estConstruit());
            ArbreImprimable a = generateur.arbre(t, tableau).feuille();

            byte[] pdf = RenduPdfFeuilles.arbre(a);
            try (PDDocument doc = PDDocument.load(pdf)) {
                assertEquals(1, doc.getNumberOfPages());
                float largeur = doc.getPage(0).getMediaBox().getWidth();
                float hauteur = doc.getPage(0).getMediaBox().getHeight();
                assertTrue(largeur > hauteur, "paysage");
                assertEquals(a.casesPremierTour() <= 8 ? 842 : 1191, Math.round(largeur), 1);
                assertTrue(new PDFTextStripper().getText(doc).contains("Finale"));
            }
        }
    }
}
