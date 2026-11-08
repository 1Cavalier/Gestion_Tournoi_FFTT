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
import com.pingmanager.model.format.FormatConfigurable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Rendu PDF : noms démesurés, caractères hors police, tableaux de 2 et de 128, poules de 3 à 6. */
class RenduPdfFeuillesLimitesTest {

    private final GenerateurFeuilles generateur = new GenerateurFeuilles();

    private static Tournoi lance(String nomTournoi, String nom, String club, int n, int taille, int qualifies) {
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(FormatConfigurable.builder()
                .nom("L").identifiant("PL" + taille + qualifies).taillePouleSouhaitee(taille)
                .qualifiesParPoule(qualifies).nombreManchesGagnantes(3).build()).build();
        Tournoi t = new Tournoi(nomTournoi, "Club", LocalDate.of(2026, 11, 14));
        Tableau tableau = new Tableau("Tableau " + nom, config);
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= n; i++) {
            Joueur j = new Joueur("L" + i, nom + i, nom, Sexe.HOMME, club, 500 + i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return t;
    }

    private static void construireFinale(Tableau tableau) {
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (!m.estBye()) {
                    m.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
                }
            }
        }
        assertTrue(new ServicePhaseFinale().construire(tableau, 2L).estConstruit());
    }

    private static void valide(byte[] pdf) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    void noms_tres_longs_et_caracteres_speciaux_ne_font_pas_planter_le_rendu() throws IOException {
        String nom = "Œuvre-Ñandú-ÆØÅ-ß-Ł-Đ-ő-İ-" + "X".repeat(120) + "\n\t  \"<>&%()\\";
        Tournoi t = lance("Tournoi « ≤ » " + "très long ".repeat(30), nom, "Club ✓ " + "Z".repeat(200), 6, 3, 2);
        Tableau tableau = t.getTableaux().get(0);
        Poule poule = tableau.getPoules().get(0);

        valide(RenduPdfFeuilles.feuillesMatch(generateur.feuillesMatchsPoule(t, tableau, poule)));
        valide(RenduPdfFeuilles.feuillePoule(generateur.feuillePoule(t, tableau, poule).feuille()));
        construireFinale(tableau);
        valide(RenduPdfFeuilles.arbre(generateur.arbre(t, tableau).feuille()));
    }

    @Test
    void feuille_de_poule_de_3_4_5_et_6_se_rend() throws IOException {
        for (int taille = 3; taille <= 6; taille++) {
            Tournoi t = lance("T", "Martin", "Club", taille, taille, 1);
            Tableau tableau = t.getTableaux().get(0);
            FeuillePoule f = generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille();
            valide(RenduPdfFeuilles.feuillePoule(f));
        }
    }

    @Test
    void tableau_final_de_2_et_de_128_se_rend_sur_une_page() throws IOException {
        for (int[] cas : new int[][] {{3, 2}, {192, 128}}) {
            Tournoi t = lance("T", "Martin", "Club", cas[0], 3, 2);
            Tableau tableau = t.getTableaux().get(0);
            construireFinale(tableau);
            ArbreImprimable a = generateur.arbre(t, tableau).feuille();
            byte[] pdf = RenduPdfFeuilles.arbre(a);
            try (PDDocument doc = PDDocument.load(pdf)) {
                assertEquals(1, doc.getNumberOfPages(), "tableau de " + cas[1]);
            }
        }
    }

    @Test
    void joueur_sans_club_se_rend() throws IOException {
        Tournoi t = lance("T", "Sansclub", "", 3, 3, 1);
        Tableau tableau = t.getTableaux().get(0);
        List<FeuilleMatch> feuilles = generateur.feuillesMatchsPoule(t, tableau, tableau.getPoules().get(0));
        valide(RenduPdfFeuilles.feuillesMatch(feuilles));
    }

    /** Défaut : un caractère hors de la police (CJK, emoji) fait échouer toute l'impression au lieu d'un « ? ». */
    @Test
    void caractere_hors_police_ne_fait_pas_echouer_l_impression() {
        Tournoi t = lance("T", "Tanaka 日本 😀", "Club", 3, 3, 1);
        Tableau tableau = t.getTableaux().get(0);
        assertDoesNotThrow(() -> RenduPdfFeuilles.feuillesMatch(
                generateur.feuillesMatchsPoule(t, tableau, tableau.getPoules().get(0))));
        assertDoesNotThrow(() -> RenduPdfFeuilles.feuillePoule(
                generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille()));
    }
}
