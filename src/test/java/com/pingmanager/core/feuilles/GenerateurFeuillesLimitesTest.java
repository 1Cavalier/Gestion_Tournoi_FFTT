package com.pingmanager.core.feuilles;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.feuilles.ArbreImprimable.CaseArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.ColonneArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.TypeCase;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cas limites des feuilles (B016) : tailles de poule, tableaux extrêmes, données incomplètes. */
class GenerateurFeuillesLimitesTest {

    private final GenerateurFeuilles generateur = new GenerateurFeuilles();
    private final ServicePhaseFinale phaseFinale = new ServicePhaseFinale();

    private static Tournoi lance(int n, int taillePoule, int qualifies) {
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(FormatConfigurable.builder()
                .nom("L").identifiant("L" + taillePoule + qualifies).taillePouleSouhaitee(taillePoule)
                .qualifiesParPoule(qualifies).nombreManchesGagnantes(3).build()).build();
        Tournoi t = new Tournoi("T", "Club", LocalDate.of(2026, 11, 14));
        Tableau tableau = new Tableau("A", config);
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= n; i++) {
            Joueur j = new Joueur("L" + i, "Nom" + i, "P" + i, Sexe.HOMME, i % 2 == 0 ? "" : "Club", i % 3 == 0 ? 0 : 500 + i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return t;
    }

    private static void jouerPoules(Tableau tableau) {
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (!m.estBye()) {
                    m.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
                }
            }
        }
    }

    @Test
    void feuille_de_poule_de_3_4_5_et_6_grille_et_parties_coherentes() {
        for (int taille = 3; taille <= 6; taille++) {
            Tournoi t = lance(taille, taille, 1);
            Tableau tableau = t.getTableaux().get(0);
            assertEquals(1, tableau.getPoules().size(), "une seule poule de " + taille);
            Poule poule = tableau.getPoules().get(0);
            FeuillePoule f = generateur.feuillePoule(t, tableau, poule).feuille();

            assertEquals(taille, f.joueurs().size());
            assertEquals(taille * (taille - 1) / 2, f.parties().size(), "round robin de " + taille);
            assertEquals(taille + 1, f.grilleResultats().size());
            for (List<String> ligne : f.grilleResultats()) {
                assertEquals(taille + 1, ligne.size());
            }
            for (int i = 1; i <= taille; i++) {
                assertEquals(FeuillePoule.CASE_BARREE, f.grilleResultats().get(i).get(i));
            }
            // Chaque couple de positions apparaît exactement une fois ; aucun joueur n'est en face de lui-même.
            java.util.Set<String> couples = new java.util.HashSet<>();
            for (FeuillePoule.PartiePoule p : f.parties()) {
                assertNotEquals(p.positionA(), p.positionB());
                assertTrue(p.positionA() >= 1 && p.positionB() >= 1);
                assertTrue(couples.add(Math.min(p.positionA(), p.positionB()) + "-" + Math.max(p.positionA(), p.positionB())));
            }
            assertEquals(f.parties().size(), generateur.feuillesMatchsPoule(t, tableau, poule).size());
        }
    }

    @Test
    void joueur_sans_club_ni_points_n_a_ni_null_ni_separateur_orphelin() {
        Joueur sans = new Joueur("L1", "Dupont", "Léa", Sexe.FEMME, null, 0);
        JoueurFeuille jf = JoueurFeuille.de(sans);
        assertEquals("", jf.club());
        assertFalse(jf.detail().startsWith(" ·"));
        assertFalse(jf.detail().endsWith("· "));
        assertFalse(jf.detail().contains("null"));
        // Licence provisoire (« __… ») jamais imprimée.
        assertEquals("", JoueurFeuille.de(new Joueur("__tmp1", "A", "B", Sexe.HOMME, "C", 600)).licence());
        assertTrue(JoueurFeuille.de(null).estADeterminer());
        assertTrue(JoueurFeuille.de(Joueur.aDeterminer()).estADeterminer());
        assertEquals(JoueurFeuille.A_DETERMINER, JoueurFeuille.de(null).nomComplet());
    }

    @Test
    void feuilles_d_une_poule_avec_joueurs_sans_club_ni_points() {
        Tournoi t = lance(6, 3, 2); // un joueur sur trois n'a aucun point, un sur deux aucun club
        Tableau tableau = t.getTableaux().get(0);
        for (Poule p : tableau.getPoules()) {
            FeuillePoule f = generateur.feuillePoule(t, tableau, p).feuille();
            for (JoueurFeuille j : f.joueurs()) {
                assertFalse(j.detail().contains("null"));
                assertFalse(j.estADeterminer());
            }
        }
    }

    @Test
    void poule_vide_sans_aucune_partie_est_refusee_et_sans_feuille_de_match() {
        Tournoi t = lance(3, 3, 1);
        Tableau tableau = t.getTableaux().get(0);
        Poule vide = new Poule("Z", List.of());
        assertEquals(MotifRefusFeuille.POULE_SANS_PARTIE, generateur.feuillePoule(t, tableau, vide).motif());
        assertTrue(generateur.feuillesMatchsPoule(t, tableau, vide).isEmpty());
    }

    @Test
    void arguments_nuls_refuses_avec_une_exception_claire() {
        Tournoi t = lance(3, 3, 1);
        Tableau tableau = t.getTableaux().get(0);
        assertThrows(NullPointerException.class, () -> generateur.arbre(null, tableau));
        assertThrows(NullPointerException.class, () -> generateur.arbre(t, null));
        assertThrows(NullPointerException.class, () -> generateur.feuillePoule(t, tableau, null));
        assertThrows(NullPointerException.class, () -> generateur.feuilleMatch(t, (Match) null));
    }

    @Test
    void format_valeurs_limites() {
        assertEquals("", GenerateurFeuilles.format(0));
        assertEquals("", GenerateurFeuilles.format(-2));
        assertEquals("Au meilleur des 1 manches (1 manche gagnante)", GenerateurFeuilles.format(1));
        assertEquals("Au meilleur des 7 manches (4 manches gagnantes)", GenerateurFeuilles.format(4));
    }

    @Test
    void tableau_final_de_deux_qualifies_une_finale_une_colonne() {
        Tournoi t = lance(3, 3, 2);
        Tableau tableau = t.getTableaux().get(0);
        jouerPoules(tableau);
        assertTrue(phaseFinale.construire(tableau, 1L).estConstruit());

        ArbreImprimable a = generateur.arbre(t, tableau).feuille();

        assertEquals(1, a.colonnes().size());
        assertEquals("Finale", a.colonnes().get(0).libelle());
        assertEquals(1, a.colonnes().get(0).cases().size());
        assertEquals(TypeCase.MATCH, a.colonnes().get(0).cases().get(0).type());
        assertEquals(1, a.casesPremierTour());
        assertTrue(a.resume().contains("tableau de 2"));
        assertTrue(a.resume().contains("aucun exempt"));
    }

    @Test
    void tableaux_de_64_et_128_places_colonnes_doublees_et_sans_exception() {
        for (int[] cas : new int[][] {{96, 64}, {192, 128}}) {
            Tournoi t = lance(cas[0], 3, 2);
            Tableau tableau = t.getTableaux().get(0);
            jouerPoules(tableau);
            assertTrue(phaseFinale.construire(tableau, 1L).estConstruit());

            ArbreImprimable a = generateur.arbre(t, tableau).feuille();

            assertEquals(cas[1] / 2, a.casesPremierTour());
            List<ColonneArbre> cols = a.colonnes();
            assertEquals(Integer.numberOfTrailingZeros(cas[1]), cols.size());
            for (int i = 1; i < cols.size(); i++) {
                assertEquals(2 * cols.get(i).cases().size(), cols.get(i - 1).cases().size());
            }
            assertEquals(1, cols.get(cols.size() - 1).cases().size());
            assertTrue(a.resume().contains("tableau de " + cas[1]));
        }
    }

    @Test
    void tableau_final_exempts_et_forfait_au_premier_tour_le_vainqueur_avance() {
        Tournoi t = lance(15, 3, 2); // 10 qualifiés dans un tableau de 16 : 6 exempts
        Tableau tableau = t.getTableaux().get(0);
        jouerPoules(tableau);
        assertTrue(phaseFinale.construire(tableau, 3L).estConstruit());
        List<Match> premiers = tableau.getPhaseFinale().getPremierTour().getMatchs();
        Match reel = premiers.stream().filter(m -> !m.estBye()).findFirst().orElseThrow();
        assertTrue(phaseFinale.declarerIssue(tableau, reel, IssueMatch.FORFAIT, reel.getJoueurB()).estValide());

        ArbreImprimable a = generateur.arbre(t, tableau).feuille();

        List<CaseArbre> cases = a.colonnes().get(0).cases();
        long exempts = cases.stream().filter(c -> c.type() == TypeCase.EXEMPT).count();
        assertTrue(exempts > 0, "des exempts sont imprimés");
        assertTrue(cases.stream().anyMatch(c -> "Forfait".equals(c.mention())));
        // Aucune case de match ne porte un joueur « à déterminer » au premier tour.
        for (CaseArbre c : cases) {
            if (c.type() == TypeCase.MATCH) {
                assertFalse(c.joueurA().estADeterminer());
                assertFalse(c.joueurB().estADeterminer());
            }
        }
        // Les feuilles de match refusent les exempts.
        for (Match m : premiers) {
            if (m.estBye()) {
                assertEquals(MotifRefusFeuille.MATCH_EXEMPT, generateur.feuilleMatch(t, tableau, m).motif());
            }
        }
    }

    @Test
    void en_tete_avec_nom_de_tournoi_tres_long_et_caracteres_speciaux_reste_intact() {
        String nom = "Tournoi « é ñ ç œ ≤ » " + "très long ".repeat(40);
        Tournoi t = new Tournoi(nom, "Club", LocalDate.of(2026, 1, 5));
        Tableau tableau = new Tableau("Tab ≤ 1599", ConfigurationTableau.depuisFormat(
                com.pingmanager.model.format.RegistreFormats.formatParDefaut()).build());
        assertEquals(nom, GenerateurFeuilles.entete(t, tableau).tournoi());
        assertEquals("05/01/2026", GenerateurFeuilles.entete(t, tableau).date());
    }
}
