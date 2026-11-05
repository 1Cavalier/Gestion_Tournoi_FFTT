package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.GenerateurTableauElimination;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.CaseTableau;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.Colonne;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.LignePodium;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.TypeCase;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Logique d'affichage de la vue « Phase finale », sans JavaFX. */
class PresentationPhaseFinaleTest {

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    /** Tableau final construit à partir de {@code n} inscrits (poules jouées au hasard). */
    private static Tableau phaseFinale(int n, long graine) {
        Tableau modele = new Tableau("Modèle", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        for (Joueur joueur : TableauEssaiPhaseFinale.joueursFictifs(n)) {
            modele.inscrire(joueur);
        }
        Tableau essai = TableauEssaiPhaseFinale.creer(modele, graine);
        assertTrue(new ServicePhaseFinale().construire(essai, graine).estConstruit());
        return essai;
    }

    /** Placement explicite par seed (index 0 inutilisé, null = bye), cadre 8. */
    private static TableauElimination depuisPlacement(Integer... seedsOccupes) {
        Joueur[] parSeed = new Joueur[9];
        for (int s : seedsOccupes) {
            parSeed[s] = j("S" + s, 2000 - s);
        }
        return new GenerateurTableauElimination().genererDepuisPlacement(parSeed, seedsOccupes.length);
    }

    // ------------------------------------------------------------------
    // Libellés des tours
    // ------------------------------------------------------------------

    @Test
    void libelles_des_tours_depuis_la_finale() {
        assertEquals("Finale", PresentationPhaseFinale.libelleTour(0));
        assertEquals("Demi-finales", PresentationPhaseFinale.libelleTour(1));
        assertEquals("Quarts de finale", PresentationPhaseFinale.libelleTour(2));
        assertEquals("1/8 de finale", PresentationPhaseFinale.libelleTour(3));
        assertEquals("1/16 de finale", PresentationPhaseFinale.libelleTour(4));
        assertEquals("1/32 de finale", PresentationPhaseFinale.libelleTour(5));
        assertThrows(IllegalArgumentException.class, () -> PresentationPhaseFinale.libelleTour(-1));
    }

    // ------------------------------------------------------------------
    // Colonnes et byes
    // ------------------------------------------------------------------

    @Test
    void colonnes_d_un_tableau_de_huit_sans_exempt() {
        TableauElimination elim = phaseFinale(12, 3L).getPhaseFinale();
        List<Colonne> cols = PresentationPhaseFinale.colonnes(elim);

        assertEquals(List.of("Quarts de finale", "Demi-finales", "Finale"),
                cols.stream().map(Colonne::libelle).toList());
        assertEquals(4, cols.get(0).cases().size());
        assertEquals(2, cols.get(1).cases().size());
        assertEquals(1, cols.get(2).cases().size());
        assertTrue(cols.get(0).cases().stream().allMatch(c -> c.type() == TypeCase.MATCH));
        assertEquals(0, PresentationPhaseFinale.nombreExemptes(elim));
        assertEquals("8 qualifiés · tableau de 8 · aucun exempt", PresentationPhaseFinale.resume(elim));
    }

    @Test
    void colonnes_suivent_l_ordre_des_matchs_du_premier_tour() {
        TableauElimination elim = phaseFinale(12, 3L).getPhaseFinale();
        List<CaseTableau> premier = PresentationPhaseFinale.colonnes(elim).get(0).cases();
        List<Match> matchs = elim.getPremierTour().getMatchs();
        for (int i = 0; i < matchs.size(); i++) {
            assertSame(matchs.get(i), premier.get(i).match());
        }
    }

    @Test
    void tableau_incomplet_les_exempts_sont_visibles_et_chaque_match_affiche_une_fois() {
        // 15 inscrits -> 5 poules -> 10 qualifiés dans un tableau de 16.
        int exemptsVus = 0;
        for (long graine = 1; graine <= 12; graine++) {
            TableauElimination elim = phaseFinale(15, graine).getPhaseFinale();
            List<Colonne> cols = PresentationPhaseFinale.colonnes(elim);

            assertEquals(4, cols.size());
            assertEquals("1/8 de finale", cols.get(0).libelle());
            for (int c = 0; c < cols.size(); c++) {
                assertEquals(8 >> c, cols.get(c).cases().size(), "taille de la colonne " + c);
            }
            // Chaque joueur du premier tour est soit dans un match, soit exempt.
            int exempts = 0;
            int matchs = 0;
            for (CaseTableau cas : cols.get(0).cases()) {
                if (cas.type() == TypeCase.EXEMPT) {
                    exempts++;
                    assertNotNull(cas.joueurExempte());
                } else if (cas.type() == TypeCase.MATCH) {
                    matchs++;
                }
            }
            assertEquals(elim.getNombreJoueurs(), exempts + 2 * matchs, "graine " + graine);
            exemptsVus += exempts;

            // Chaque match du tableau principal apparaît exactement une fois.
            Map<Match, Integer> vus = new IdentityHashMap<>();
            for (Colonne col : cols) {
                for (CaseTableau cas : col.cases()) {
                    if (cas.type() == TypeCase.MATCH) {
                        vus.merge(cas.match(), 1, Integer::sum);
                    }
                }
            }
            int total = 0;
            for (Tour t : ServicePhaseFinale.toursPrincipaux(elim)) {
                total += t.getNombreMatchs();
                for (Match m : t.getMatchs()) {
                    assertEquals(1, vus.getOrDefault(m, 0));
                }
            }
            assertEquals(total, vus.size());
        }
        assertTrue(exemptsVus > 0);
    }

    @Test
    void deux_byes_face_a_face_le_vainqueur_saute_les_demies_sans_decaler_les_colonnes() {
        // Cadre 8, ordre des places [1,8,4,5,2,7,3,6] : les places 4 et 5 sont vides.
        TableauElimination elim = depuisPlacement(1, 2, 3, 6, 7, 8);
        List<Colonne> cols = PresentationPhaseFinale.colonnes(elim);
        Match premierMatch = elim.getPremierTour().getMatchs().get(0); // 1 contre 8

        List<CaseTableau> quarts = cols.get(0).cases();
        assertEquals(TypeCase.MATCH, quarts.get(0).type());
        assertSame(premierMatch, quarts.get(0).match());
        assertEquals(TypeCase.VIDE, quarts.get(1).type());

        CaseTableau demiHaut = cols.get(1).cases().get(0);
        assertEquals(TypeCase.EXEMPT, demiHaut.type());
        assertNull(demiHaut.joueurExempte()); // vainqueur pas encore connu
        premierMatch.definirScore(List.of(new Manche(11, 2), new Manche(11, 2), new Manche(11, 2)));
        assertEquals(premierMatch.getJoueurA(), demiHaut.joueurExempte());
    }

    @Test
    void joueur_exempte_deux_tours_apparait_dans_les_deux_colonnes() {
        // Places 8, 4 et 5 vides : le joueur 1 n'a d'adversaire ni en quart ni en demie.
        TableauElimination elim = depuisPlacement(1, 2, 3, 6, 7);
        List<Colonne> cols = PresentationPhaseFinale.colonnes(elim);
        CaseTableau demi = cols.get(1).cases().get(0);
        CaseTableau quart = cols.get(0).cases().get(0);
        assertEquals(TypeCase.EXEMPT, demi.type());
        assertEquals("S1", demi.joueurExempte().getNumeroLicence());
        assertEquals(TypeCase.EXEMPT, quart.type());
        assertEquals("S1", quart.joueurExempte().getNumeroLicence());
        assertEquals(TypeCase.VIDE, cols.get(0).cases().get(1).type());
        assertEquals("5 qualifiés · tableau de 8 · 1 exempt au premier tour", PresentationPhaseFinale.resume(elim));
    }

    @Test
    void petite_finale_presente_seulement_si_le_tableau_en_a_une() {
        assertNull(PresentationPhaseFinale.matchPetiteFinale(phaseFinale(12, 3L).getPhaseFinale()));
    }

    @Test
    void alignement_des_colonnes_centre_chaque_case_entre_ses_deux_sources() {
        double h = 60;
        double g = 10;
        double[] premier = PresentationPhaseFinale.alignement(0, h, g);
        assertArrayEquals(new double[] {0, 10}, premier, 1e-9);
        double[] second = PresentationPhaseFinale.alignement(1, h, g);
        // Centre de la 1re case du 2e tour = milieu des centres des deux premières cases.
        double centre = second[0] + h / 2;
        assertEquals(((h / 2) + (h + g + h / 2)) / 2, centre, 1e-9);
        assertEquals(h + 2 * g, second[1], 1e-9);
    }

    // ------------------------------------------------------------------
    // Joueurs et scores
    // ------------------------------------------------------------------

    @Test
    void nom_et_detail_d_un_joueur_et_d_une_place_a_determiner() {
        Joueur joueur = new Joueur("1", "Martin", "Léa", Sexe.FEMME, "US Brunoy", 1520);
        assertEquals("Léa MARTIN", PresentationPhaseFinale.nomJoueur(joueur));
        assertEquals("US Brunoy · 1520 pts", PresentationPhaseFinale.detailJoueur(joueur));
        assertEquals("À déterminer", PresentationPhaseFinale.nomJoueur(Joueur.aDeterminer()));
        assertEquals("", PresentationPhaseFinale.detailJoueur(Joueur.aDeterminer()));
    }

    @Test
    void score_d_un_match_joue_et_d_un_forfait() {
        Match m = new Match(j("A", 1000), j("B", 900));
        assertEquals("À jouer", PresentationPhaseFinale.etatMatch(m));
        assertEquals("", PresentationPhaseFinale.scoreManches(m));
        assertEquals("Au meilleur des 5 manches", PresentationPhaseFinale.formatMatch(m));

        m.definirScore(List.of(new Manche(11, 8), new Manche(9, 11), new Manche(11, 6), new Manche(12, 10)));
        assertEquals("Terminé", PresentationPhaseFinale.etatMatch(m));
        assertEquals("3-1", PresentationPhaseFinale.scoreManches(m));
        assertEquals("3", PresentationPhaseFinale.manchesDe(m, true));
        assertEquals("1", PresentationPhaseFinale.manchesDe(m, false));
        assertEquals("11-8, 9-11, 11-6, 12-10", PresentationPhaseFinale.detailScore(m));
        assertTrue(PresentationPhaseFinale.estVainqueur(m, true));
        assertFalse(PresentationPhaseFinale.estVainqueur(m, false));

        Match f = new Match(j("C", 1000), j("D", 900));
        f.declarerIssue(IssueMatch.FORFAIT, f.getJoueurA());
        assertEquals("Forfait", PresentationPhaseFinale.scoreManches(f));
        assertEquals("Forfait de PrenomC NOMC", PresentationPhaseFinale.detailScore(f));
        assertTrue(PresentationPhaseFinale.estVainqueur(f, false));
    }

    @Test
    void match_dont_un_joueur_est_inconnu_n_est_pas_saisissable() {
        Match m = new Match(j("A", 1000), Joueur.aDeterminer());
        assertFalse(PresentationPhaseFinale.estSaisissable(m));
        assertEquals("En attente des joueurs", PresentationPhaseFinale.etatMatch(m));
    }

    @Test
    void libelles_des_issues_sans_switch() {
        assertEquals("Forfait", PresentationPhaseFinale.libelleIssue(IssueMatch.FORFAIT));
        assertEquals("Abandon", PresentationPhaseFinale.libelleIssue(IssueMatch.ABANDON));
        assertEquals("Refus de jouer", PresentationPhaseFinale.libelleIssue(IssueMatch.REFUS_DE_JOUER));
        assertEquals("Disqualification", PresentationPhaseFinale.libelleIssue(IssueMatch.DISQUALIFICATION));
        assertEquals("Score", PresentationPhaseFinale.libelleIssue(IssueMatch.NORMALE));
    }

    // ------------------------------------------------------------------
    // Podium
    // ------------------------------------------------------------------

    @Test
    void podium_sans_petite_finale_affiche_deux_troisiemes_ex_aequo() {
        PodiumPhaseFinale p = new PodiumPhaseFinale(j("1", 0), j("2", 0), List.of(j("3", 0), j("4", 0)), null, false);
        List<LignePodium> lignes = PresentationPhaseFinale.lignesPodium(p);
        assertEquals(List.of("1er", "2e", "3e ex aequo", "3e ex aequo"),
                lignes.stream().map(LignePodium::place).toList());
        assertTrue(PresentationPhaseFinale.notePodium(p).contains("3es ex aequo"));
    }

    @Test
    void podium_avec_petite_finale_affiche_un_troisieme_et_un_quatrieme() {
        PodiumPhaseFinale p = new PodiumPhaseFinale(j("1", 0), j("2", 0), List.of(j("3", 0)), j("4", 0), true);
        assertEquals(List.of("1er", "2e", "3e", "4e"),
                PresentationPhaseFinale.lignesPodium(p).stream().map(LignePodium::place).toList());
        assertEquals("La 3e place se joue en petite finale.", PresentationPhaseFinale.notePodium(p));
    }

    @Test
    void podium_vide_explique_qu_il_se_remplira() {
        PodiumPhaseFinale p = new PodiumPhaseFinale(null, null, List.of(), null, false);
        assertTrue(PresentationPhaseFinale.lignesPodium(p).isEmpty());
        assertTrue(PresentationPhaseFinale.notePodium(p).startsWith("Le podium se remplit"));
    }

    // ------------------------------------------------------------------
    // Graine et état
    // ------------------------------------------------------------------

    @Test
    void graine_vide_generee_saisie_lue_et_saisie_invalide_refusee() {
        assertEquals(123L, PresentationPhaseFinale.lireGraine("", 1_000_000_123L).graine());
        assertEquals(123L, PresentationPhaseFinale.lireGraine(null, -1_000_000_123L).graine());
        assertEquals(12345L, PresentationPhaseFinale.lireGraine(" 12 345 ", 0L).graine());
        assertFalse(PresentationPhaseFinale.lireGraine("abc", 0L).estValide());
        assertFalse(PresentationPhaseFinale.lireGraine("-5", 0L).estValide());
    }

    @Test
    void texte_de_la_graine_connue_ou_non_conservee() {
        assertTrue(PresentationPhaseFinale.texteGraine(987L).startsWith("Graine du tirage : 987."));
        assertTrue(PresentationPhaseFinale.texteGraine(null).contains("non conservée"));
    }

    @Test
    void message_poules_en_cours_donne_les_chiffres() {
        DiagnosticPhaseFinale d = new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.POULES_EN_COURS, 4, 3, 9, 8);
        assertEquals("Poules en cours", PresentationPhaseFinale.titreEtat(d));
        assertEquals("Toutes les poules doivent être terminées avant le tirage du tableau final : "
                        + "3 poules ne sont pas terminées (9 matchs restants sur 4 poules).",
                PresentationPhaseFinale.messageEtat(d));
        DiagnosticPhaseFinale une = new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.POULES_EN_COURS, 4, 1, 1, 8);
        assertTrue(PresentationPhaseFinale.messageEtat(une).contains("1 poule n'est pas terminée (1 match restant"));
    }

    @Test
    void chaque_etat_a_un_titre_et_un_message() {
        for (DiagnosticPhaseFinale.Etat e : DiagnosticPhaseFinale.Etat.values()) {
            DiagnosticPhaseFinale d = new DiagnosticPhaseFinale(e, 2, 0, 0, 4);
            assertFalse(PresentationPhaseFinale.titreEtat(d).isBlank(), e.name());
            assertFalse(PresentationPhaseFinale.messageEtat(d).isBlank(), e.name());
        }
        assertTrue(PresentationPhaseFinale.messageEtat(
                new DiagnosticPhaseFinale(DiagnosticPhaseFinale.Etat.POULES_NON_LANCEES, 0, 0, 0, 0))
                .contains("lancez le tableau"));
    }
}
