package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReglement;

import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.TournoiDemoBrunoy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La feuille d'engagement est remplie à partir du tournoi, sans auteur ni date de rédaction. */
class GenerateurFeuilleEngagementTest {

    private static DocumentReglement.Grille grille(DocumentReglement d, String article) {
        return (DocumentReglement.Grille) d.articles().stream().filter(a -> a.titre().equals(article))
                .findFirst().orElseThrow().blocs().stream()
                .filter(b -> b instanceof DocumentReglement.Grille).findFirst().orElseThrow();
    }

    @Test
    void six_sections_dans_l_ordre_du_modele() {
        DocumentReglement d = new GenerateurFeuilleEngagement().generer(TournoiDemoBrunoy.construire());
        assertEquals("Feuille d'engagement — Tournoi National B de Brunoy 2027", d.titre());
        assertEquals(List.of("Le tournoi", "Comment s'inscrire", "Le joueur", "Choix des tableaux", "Paiement",
                "Rappels et engagement"), d.articles().stream().map(DocumentReglement.Article::titre).toList());
    }

    @Test
    void tableaux_tries_par_horaire_avec_les_deux_tarifs() {
        DocumentReglement d = new GenerateurFeuilleEngagement().generer(TournoiDemoBrunoy.construire());
        DocumentReglement.Grille g = grille(d, "Choix des tableaux");
        assertEquals(13, g.lignes().size());
        assertEquals("A", g.lignes().get(0).get(1));
        assertEquals("M – Non numérotés", g.lignes().get(12).get(1));
        assertTrue(g.lignes().stream().allMatch(l -> l.get(0).equals("□")));
        // F (≤ 2000) : 10 € en ligne comme sur place ; A : 9 € / 10 €.
        assertEquals(List.of("9 €", "10 €"), g.lignes().get(0).subList(g.entetes().size() - 2, g.entetes().size()));
        assertEquals(List.of("10 €", "10 €"), g.lignes().get(5).subList(g.entetes().size() - 2, g.entetes().size()));
    }

    @Test
    void pdf_rempli_sans_auteur_ni_date() throws Exception {
        Tournoi t = TournoiDemoBrunoy.construire();
        byte[] pdf = RenduPdfReglement.rendre(new GenerateurFeuilleEngagement().generer(t),
                GenerateurFeuilleEngagement.FORMAT_TITRE);
        try (PDDocument doc = PDDocument.load(pdf)) {
            String texte = new PDFTextStripper().getText(doc);
            assertTrue(texte.contains("1. Le tournoi"), texte);
            assertTrue(texte.contains("6. Rappels et engagement"), texte);
            assertFalse(texte.contains("Article 1"), texte);
            assertTrue(texte.contains("Bernard LEROY"), texte);
            assertTrue(texte.replace("\n", " ").contains("Samuel LOOBUYCK, JA3 ; adjoint : William PRIOU, JA2"), texte);
            assertTrue(texte.contains("157 Route de Brie, 91800 Brunoy"), texte);
            assertTrue(texte.contains("helloasso.com"), texte);
            assertTrue(texte.contains("Non admis : poussins."), texte);
            assertTrue(texte.contains("□"), texte);
            assertFalse(texte.contains("Quentin"), "pas de nom d'auteur");
            assertFalse(texte.contains("Rédigé"), "pas de mention de rédaction");
            assertFalse(texte.contains("Version"), "pas de date de version");
        }
    }
}
