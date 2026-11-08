package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import com.pingmanager.model.format.FormatTournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Audit B009 : déroulé du tableau final (I.304.2, avancement des vainqueurs,
 * forfait / abandon, petite finale). Le règlement fédéral des tournois
 * (chapitre IX) ne décrit pas le déroulé d'un tableau : seules les règles de
 * placement I.304 / I.305 sont fédérales, le reste est une décision de
 * l'organisateur (voir CONFORMITE_FFTT.md).
 */
class ConformiteTableauFinalTest {

    private final ServicePhaseFinale service = new ServicePhaseFinale();

    private static Joueur j(int i) {
        return new Joueur("L" + i, "N" + i, "P" + i, Sexe.HOMME, "Club" + i, 500 + 10 * i);
    }

    // ------------------------------------------------------------------
    // I.304.2 : têtes de série
    // ------------------------------------------------------------------

    /** Position (0-based) du seed dans le cadre. */
    private static int position(int[] ordre, int seed) {
        for (int i = 0; i < ordre.length; i++) {
            if (ordre[i] == seed) {
                return i;
            }
        }
        throw new AssertionError("seed absent " + seed);
    }

    /** Premier tour (1 = 1er tour) où les positions p et q se rencontrent. */
    private static int tourDeRencontre(int p, int q) {
        int tour = 1;
        while (p != q) {
            p /= 2;
            q /= 2;
            tour++;
        }
        return tour - 1;
    }

    @Test
    void i304_2_les_2_puissance_k_premiers_ne_se_rencontrent_qu_au_tour_prevu() {
        GenerateurTableauElimination g = new GenerateurTableauElimination();
        for (int cadre = 2; cadre <= 128; cadre *= 2) {
            int[] ordre = g.ordreDuCadre(cadre);
            int nbTours = Integer.numberOfTrailingZeros(cadre);
            // « Le 1er en haut du tableau, le 2e en bas du tableau »
            assertEquals(0, position(ordre, 1), "1er en haut, cadre " + cadre);
            assertTrue(position(ordre, 2) >= cadre / 2, "2e dans la moitié basse, cadre " + cadre);
            // 2^k premiers : ne peuvent se rencontrer qu'à partir du tour (nbTours - k + 1)
            for (int k = 1; (1 << k) <= cadre; k++) {
                int groupe = 1 << k;
                int tourMin = nbTours - k + 1; // k=1 : finale ; k=2 : demi-finales...
                for (int a = 1; a <= groupe; a++) {
                    for (int b = a + 1; b <= groupe; b++) {
                        int tour = tourDeRencontre(position(ordre, a), position(ordre, b));
                        assertTrue(tour >= tourMin, "cadre " + cadre + " : seeds " + a + " et " + b
                                + " se rencontrent au tour " + tour + " (min " + tourMin + ")");
                    }
                }
            }
        }
    }

    @Test
    void i304_2_les_groupes_3_4_puis_5_8_occupent_des_places_de_chaque_quart_ou_huitieme() {
        // Les têtes de série d'un même groupe (3e-4e, 5e-8e...) sont réparties
        // une par sous-tableau : le groupe 3-4 occupe une place dans chaque demi.
        int[] ordre = new GenerateurTableauElimination().ordreDuCadre(32);
        int demi3 = position(ordre, 3) / 16;
        int demi4 = position(ordre, 4) / 16;
        assertNotEquals(demi3, demi4, "3e et 4e dans des demis différents");
        boolean[] quartPris = new boolean[4];
        for (int s = 5; s <= 8; s++) {
            quartPris[position(ordre, s) / 8] = true;
        }
        for (boolean b : quartPris) {
            assertTrue(b, "les 5e à 8e occupent chacun un quart différent");
        }
    }

    @Test
    void i304_2_les_exemptes_vont_aux_meilleures_tetes_de_serie_quand_le_tableau_est_construit_par_classement() {
        // 12 joueurs, cadre 16 : les 4 meilleurs sont exemptés (usage standard ;
        // le texte ne dit pas où placer les exempts, voir QUESTIONS.md).
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            joueurs.add(j(i));
        }
        TableauElimination t = new GenerateurTableauElimination().genererTableau(joueurs);
        List<Joueur> jouent = new ArrayList<>();
        for (Match m : t.getPremierTour().getMatchs()) {
            jouent.add(m.getJoueurA());
            jouent.add(m.getJoueurB());
        }
        for (int i = 1; i <= 4; i++) {
            assertFalse(jouent.contains(j(i)), "tête de série " + i + " exemptée");
        }
        assertEquals(8, jouent.size());
    }

    // ------------------------------------------------------------------
    // Déroulé : avancement, forfaits, abandons
    // ------------------------------------------------------------------

    private static Tableau tableauFinal(int nbJoueurs, boolean petiteFinale) {
        FormatTournoi format = FormatConfigurable.builder()
                .nom("P3Q2").identifiant("P3Q2-B009")
                .taillePouleSouhaitee(3).qualifiesParPoule(2)
                .nombreManchesGagnantes(3).avecPetiteFinale(petiteFinale).build();
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(format)
                .petiteFinale(petiteFinale).build();
        Tableau tableau = new Tableau("A", config);
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j(i);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
            }
        }
        assertTrue(new ServicePhaseFinale().construire(tableau, 42L).estConstruit());
        return tableau;
    }

    private static List<Manche> victoire(Match m, boolean pourA) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(pourA ? new Manche(11, 6) : new Manche(6, 11));
        }
        return r;
    }

    /** Joue tous les tours avant la finale (A gagne), jusqu'aux demi-finales incluses. */
    private void jouerJusquAuxDemies(Tableau tableau) {
        TableauElimination e = tableau.getPhaseFinale();
        List<com.pingmanager.model.Tour> tours = ServicePhaseFinale.toursPrincipaux(e);
        for (int i = 0; i < tours.size() - 1; i++) {
            for (Match m : tours.get(i).getMatchs()) {
                assertTrue(service.saisirScore(tableau, m, victoire(m, true)).estValide());
            }
        }
    }

    @Test
    void abandon_en_cours_de_partie_donne_la_victoire_a_l_adversaire_malgre_un_score_partiel() {
        // 3.5.3.1.1 : abandon admis pour maladie ou blessure. Le tournoi ne
        // décrit pas la suite : l'adversaire avance (choix de l'organisateur).
        Tableau tableau = tableauFinal(12, false);
        Match m = tableau.getPhaseFinale().getPremierTour().getMatchs().get(0);
        Joueur a = m.getJoueurA();
        Joueur b = m.getJoueurB();
        m.definirScore(List.of(new Manche(11, 3))); // A mène 1-0 puis abandonne
        m.setStatut(StatutMatch.EN_COURS);
        assertTrue(service.declarerIssue(tableau, m, IssueMatch.ABANDON, a).estValide());
        assertEquals(b, m.getVainqueur());
        assertEquals(a, m.getPerdant());
        assertEquals(StatutMatch.TERMINE, m.getStatut());
    }

    @Test
    void forfait_en_finale_le_fautif_est_deuxieme() {
        Tableau tableau = tableauFinal(12, false);
        jouerJusquAuxDemies(tableau);
        Match finale = ServicePhaseFinale.matchFinale(tableau.getPhaseFinale());
        Joueur fautif = finale.getJoueurA();
        Joueur adversaire = finale.getJoueurB();
        assertTrue(service.declarerIssue(tableau, finale, IssueMatch.FORFAIT, fautif).estValide());
        PodiumPhaseFinale p = ServicePhaseFinale.podium(tableau.getPhaseFinale());
        assertEquals(adversaire, p.premier());
        assertEquals(fautif, p.deuxieme());
    }

    @Test
    void forfait_en_petite_finale_le_fautif_est_quatrieme() {
        Tableau tableau = tableauFinal(12, true);
        jouerJusquAuxDemies(tableau);
        Match pf = ServicePhaseFinale.petiteFinale(tableau.getPhaseFinale()).getMatchs().get(0);
        assertEquals(StatutMatch.A_JOUER, pf.getStatut());
        Joueur fautif = pf.getJoueurA();
        Joueur adversaire = pf.getJoueurB();
        assertTrue(service.declarerIssue(tableau, pf, IssueMatch.FORFAIT, fautif).estValide());
        PodiumPhaseFinale p = ServicePhaseFinale.podium(tableau.getPhaseFinale());
        assertEquals(List.of(adversaire), p.troisiemes());
        assertEquals(fautif, p.quatrieme());
    }

    @Test
    void petite_finale_oppose_les_perdants_et_jamais_un_finaliste() {
        Tableau tableau = tableauFinal(12, true);
        jouerJusquAuxDemies(tableau);
        TableauElimination e = tableau.getPhaseFinale();
        List<com.pingmanager.model.Tour> principaux = ServicePhaseFinale.toursPrincipaux(e);
        List<Match> demies = principaux.get(principaux.size() - 2).getMatchs();
        Match pf = ServicePhaseFinale.petiteFinale(e).getMatchs().get(0);
        Match finale = ServicePhaseFinale.matchFinale(e);
        assertEquals(List.of(demies.get(0).getPerdant(), demies.get(1).getPerdant()),
                List.of(pf.getJoueurA(), pf.getJoueurB()));
        assertEquals(List.of(demies.get(0).getVainqueur(), demies.get(1).getVainqueur()),
                List.of(finale.getJoueurA(), finale.getJoueurB()));
    }

    @Test
    void petite_finale_pas_jouable_avant_les_deux_demies() {
        Tableau tableau = tableauFinal(12, true);
        TableauElimination e = tableau.getPhaseFinale();
        List<com.pingmanager.model.Tour> principaux = ServicePhaseFinale.toursPrincipaux(e);
        for (int i = 0; i < principaux.size() - 2; i++) {
            for (Match m : principaux.get(i).getMatchs()) {
                service.saisirScore(tableau, m, victoire(m, true));
            }
        }
        Match demi1 = principaux.get(principaux.size() - 2).getMatchs().get(0);
        service.saisirScore(tableau, demi1, victoire(demi1, true));
        Match pf = ServicePhaseFinale.petiteFinale(e).getMatchs().get(0);
        assertNotEquals(StatutMatch.A_JOUER, pf.getStatut());
        assertTrue(pf.getJoueurA().estADeterminer() || pf.getJoueurB().estADeterminer());
    }

    @Test
    void correction_forfait_en_demie_vers_victoire_de_l_autre_met_a_jour_finale_et_petite_finale() {
        Tableau tableau = tableauFinal(12, true);
        jouerJusquAuxDemies(tableau);
        TableauElimination e = tableau.getPhaseFinale();
        List<com.pingmanager.model.Tour> principaux = ServicePhaseFinale.toursPrincipaux(e);
        Match demi = principaux.get(principaux.size() - 2).getMatchs().get(0);
        Joueur a = demi.getJoueurA();
        Joueur b = demi.getJoueurB();
        assertTrue(service.declarerIssue(tableau, demi, IssueMatch.FORFAIT, a).estValide()); // b gagne
        Match finale = ServicePhaseFinale.matchFinale(e);
        Match pf = ServicePhaseFinale.petiteFinale(e).getMatchs().get(0);
        assertEquals(b, finale.getJoueurA());
        assertEquals(a, pf.getJoueurA());
        // correction : b est finalement le fautif
        assertTrue(service.declarerIssue(tableau, demi, IssueMatch.ABANDON, b).estValide());
        assertEquals(a, finale.getJoueurA());
        assertEquals(b, pf.getJoueurA());
    }

    @Test
    void sans_petite_finale_un_forfait_en_demie_laisse_deux_troisiemes_ex_aequo() {
        Tableau tableau = tableauFinal(12, false);
        TableauElimination e = tableau.getPhaseFinale();
        List<com.pingmanager.model.Tour> principaux = ServicePhaseFinale.toursPrincipaux(e);
        for (int i = 0; i < principaux.size() - 2; i++) {
            for (Match m : principaux.get(i).getMatchs()) {
                service.saisirScore(tableau, m, victoire(m, true));
            }
        }
        List<Match> demies = principaux.get(principaux.size() - 2).getMatchs();
        Joueur fautif = demies.get(0).getJoueurA();
        service.declarerIssue(tableau, demies.get(0), IssueMatch.FORFAIT, fautif);
        assertEquals(List.of(fautif), ServicePhaseFinale.podium(e).troisiemes());
        service.saisirScore(tableau, demies.get(1), victoire(demies.get(1), true));
        PodiumPhaseFinale p = ServicePhaseFinale.podium(e);
        assertEquals(2, p.troisiemes().size());
        assertTrue(p.troisiemesExAequo());
        assertTrue(p.troisiemes().contains(fautif));
    }
}
