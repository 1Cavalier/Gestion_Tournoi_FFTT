package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Déroulé de la phase finale : diagnostic avant construction, second tirage,
 * saisie des résultats avec progression, corrections et podium.
 */
class ServicePhaseFinaleTest {

    private final ServicePhaseFinale service = new ServicePhaseFinale();

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    private static FormatTournoi formatAvecPetiteFinale() {
        return FormatConfigurable.builder()
                .nom("P3Q2 avec petite finale").identifiant("P3Q2-PF")
                .taillePouleSouhaitee(3).qualifiesParPoule(2)
                .nombreManchesGagnantes(3)
                .avecPetiteFinale(true)
                .build();
    }

    private static Tableau tableauLance(int nbJoueurs, ConfigurationTableau config) {
        Tableau tableau = new Tableau("A", config);
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j("J" + i, 500 + 10 * i);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return tableau;
    }

    private static Tableau tableauLance(int nbJoueurs) {
        return tableauLance(nbJoueurs, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
    }

    private static void jouerPoule(Poule poule) {
        for (Match m : poule.getMatchs()) {
            m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
        }
    }

    /** Score où le joueur A (ou B) gagne toutes les manches nécessaires. */
    private static List<Manche> victoire(Match m, boolean pourA) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(pourA ? new Manche(11, 6) : new Manche(6, 11));
        }
        return r;
    }

    private Tableau phaseFinaleConstruite(int nbJoueurs, ConfigurationTableau config) {
        Tableau tableau = tableauLance(nbJoueurs, config);
        tableau.getPoules().forEach(ServicePhaseFinaleTest::jouerPoule);
        assertTrue(service.construire(tableau, 42L).estConstruit());
        return tableau;
    }

    private Tableau phaseFinaleConstruite(int nbJoueurs) {
        return phaseFinaleConstruite(nbJoueurs, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
    }

    /** Joue tout le tableau, tour par tour, le joueur A gagnant chaque match. */
    private void jouerToutLeTableau(Tableau tableau) {
        for (Tour tour : tableau.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                assertTrue(service.saisirScore(tableau, m, victoire(m, true)).estValide());
            }
        }
    }

    // ------------------------------------------------------------------
    // Diagnostic et construction
    // ------------------------------------------------------------------

    @Test
    void diagnostic_poules_non_lancees_tant_que_le_tableau_est_en_inscription() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        DiagnosticPhaseFinale d = service.diagnostiquer(tableau);
        assertEquals(DiagnosticPhaseFinale.Etat.POULES_NON_LANCEES, d.getEtat());
        assertFalse(d.peutConstruire());
    }

    @Test
    void diagnostic_sans_format_quand_le_tableau_n_a_pas_de_format() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.builder().build());
        assertEquals(DiagnosticPhaseFinale.Etat.SANS_FORMAT, service.diagnostiquer(tableau).getEtat());
    }

    @Test
    void diagnostic_poules_en_cours_compte_les_poules_et_les_matchs_restants() {
        // 12 joueurs : 4 poules de 3, 3 matchs par poule.
        Tableau tableau = tableauLance(12);
        jouerPoule(tableau.getPoules().get(0));
        DiagnosticPhaseFinale d = service.diagnostiquer(tableau);
        assertEquals(DiagnosticPhaseFinale.Etat.POULES_EN_COURS, d.getEtat());
        assertEquals(4, d.getNombrePoules());
        assertEquals(3, d.getPoulesNonTerminees());
        assertEquals(9, d.getMatchsPouleRestants());
        assertEquals(8, d.getQualifiesAttendus());
    }

    @Test
    void construction_refusee_tant_que_les_poules_ne_sont_pas_terminees_sans_rien_modifier() {
        Tableau tableau = tableauLance(12);
        ResultatConstructionPhaseFinale r = service.construire(tableau, 7L);
        assertFalse(r.estConstruit());
        assertNull(r.getTableau());
        assertEquals(DiagnosticPhaseFinale.Etat.POULES_EN_COURS, r.getDiagnostic().getEtat());
        assertEquals(StatutTableau.LANCE, tableau.getStatut());
        assertFalse(tableau.aPhaseFinale());
    }

    @Test
    void construction_apres_les_poules_conserve_la_graine_et_passe_en_phase_finale() {
        Tableau tableau = tableauLance(12);
        tableau.getPoules().forEach(ServicePhaseFinaleTest::jouerPoule);
        assertEquals(DiagnosticPhaseFinale.Etat.PRETE_A_CONSTRUIRE, service.diagnostiquer(tableau).getEtat());

        ResultatConstructionPhaseFinale r = service.construire(tableau, 123456L);

        assertTrue(r.estConstruit());
        assertEquals(123456L, r.getGraine());
        assertSame(tableau.getPhaseFinale(), r.getTableau());
        assertEquals(StatutTableau.PHASE_FINALE, tableau.getStatut());
        assertEquals(DiagnosticPhaseFinale.Etat.EN_COURS, r.getDiagnostic().getEtat());
        assertEquals(8, r.getTableau().getNombreJoueurs());
    }

    @Test
    void sans_elimination_si_le_tableau_ne_prevoit_pas_de_phase_finale() {
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .formatPhaseFinale(FormatPhaseFinale.AUCUNE).build();
        Tableau tableau = tableauLance(6, config);
        tableau.getPoules().forEach(ServicePhaseFinaleTest::jouerPoule);
        ResultatConstructionPhaseFinale r = service.construire(tableau, 1L);
        assertFalse(r.estConstruit());
        assertEquals(DiagnosticPhaseFinale.Etat.SANS_ELIMINATION, r.getDiagnostic().getEtat());
    }

    // ------------------------------------------------------------------
    // Saisie des résultats
    // ------------------------------------------------------------------

    @Test
    void saisie_du_score_fait_progresser_le_vainqueur_au_tour_suivant() {
        Tableau tableau = phaseFinaleConstruite(12);
        TableauElimination elim = tableau.getPhaseFinale();
        Match quart = elim.getPremierTour().getMatchs().get(0);
        Match demi = elim.getTours().get(1).getMatchs().get(0);
        assertTrue(demi.getJoueurA().estADeterminer());

        ResultatSaisieScore r = service.saisirScore(tableau, quart,
                List.of(new Manche(11, 9), new Manche(9, 11), new Manche(11, 7), new Manche(12, 10)));

        assertTrue(r.estValide());
        assertEquals(StatutMatch.TERMINE, quart.getStatut());
        assertEquals(quart.getJoueurA(), demi.getJoueurA());
    }

    @Test
    void score_non_conforme_refuse_et_rien_n_est_enregistre() {
        Tableau tableau = phaseFinaleConstruite(12);
        Match quart = tableau.getPhaseFinale().getPremierTour().getMatchs().get(0);
        ResultatSaisieScore r = service.saisirScore(tableau, quart,
                List.of(new Manche(11, 9), new Manche(11, 10), new Manche(11, 7)));
        assertEquals(ResultatSaisieScore.Motif.MANCHE_ECART_INSUFFISANT, r.getMotif());
        assertEquals(2, r.getNumeroManche());
        assertNotEquals(StatutMatch.TERMINE, quart.getStatut());
        assertTrue(quart.getManches().isEmpty());
    }

    @Test
    void match_dont_les_joueurs_ne_sont_pas_connus_refuse() {
        Tableau tableau = phaseFinaleConstruite(12);
        Match finale = ServicePhaseFinale.matchFinale(tableau.getPhaseFinale());
        assertEquals(ResultatSaisieScore.Motif.JOUEURS_NON_CONNUS,
                service.saisirScore(tableau, finale, victoire(finale, true)).getMotif());
    }

    @Test
    void match_d_un_autre_tableau_refuse() {
        Tableau tableau = phaseFinaleConstruite(12);
        Match etranger = new Match(j("X1", 500), j("X2", 500));
        assertEquals(ResultatSaisieScore.Motif.MATCH_HORS_TABLEAU,
                service.saisirScore(tableau, etranger, victoire(etranger, true)).getMotif());
    }

    @Test
    void forfait_qualifie_l_adversaire_pour_le_tour_suivant() {
        Tableau tableau = phaseFinaleConstruite(12);
        TableauElimination elim = tableau.getPhaseFinale();
        Match quart = elim.getPremierTour().getMatchs().get(0);
        Joueur absent = quart.getJoueurA();
        Joueur present = quart.getJoueurB();

        ResultatSaisieScore r = service.declarerIssue(tableau, quart, IssueMatch.FORFAIT, absent);

        assertTrue(r.estValide());
        assertEquals(present, quart.getVainqueur());
        assertEquals(present, elim.getTours().get(1).getMatchs().get(0).getJoueurA());
    }

    @Test
    void issue_normale_ou_fautif_hors_match_refuse() {
        Tableau tableau = phaseFinaleConstruite(12);
        Match quart = tableau.getPhaseFinale().getPremierTour().getMatchs().get(0);
        assertEquals(ResultatSaisieScore.Motif.ISSUE_INVALIDE,
                service.declarerIssue(tableau, quart, IssueMatch.NORMALE, quart.getJoueurA()).getMotif());
        assertEquals(ResultatSaisieScore.Motif.ISSUE_INVALIDE,
                service.declarerIssue(tableau, quart, IssueMatch.ABANDON, j("X9", 500)).getMotif());
    }

    @Test
    void correction_qui_change_le_vainqueur_met_a_jour_le_tour_suivant_non_joue() {
        Tableau tableau = phaseFinaleConstruite(12);
        TableauElimination elim = tableau.getPhaseFinale();
        Match quart = elim.getPremierTour().getMatchs().get(0);
        Match demi = elim.getTours().get(1).getMatchs().get(0);
        service.saisirScore(tableau, quart, victoire(quart, true));
        assertEquals(quart.getJoueurA(), demi.getJoueurA());

        ResultatSaisieScore r = service.saisirScore(tableau, quart, victoire(quart, false));

        assertTrue(r.estValide());
        assertEquals(quart.getJoueurB(), quart.getVainqueur());
        assertEquals(quart.getJoueurB(), demi.getJoueurA());
    }

    @Test
    void correction_qui_change_le_vainqueur_refusee_si_le_match_suivant_est_joue() {
        Tableau tableau = phaseFinaleConstruite(12);
        TableauElimination elim = tableau.getPhaseFinale();
        List<Match> quarts = elim.getPremierTour().getMatchs();
        service.saisirScore(tableau, quarts.get(0), victoire(quarts.get(0), true));
        service.saisirScore(tableau, quarts.get(1), victoire(quarts.get(1), true));
        Match demi = elim.getTours().get(1).getMatchs().get(0);
        assertTrue(service.saisirScore(tableau, demi, victoire(demi, true)).estValide());

        ResultatSaisieScore r = service.saisirScore(tableau, quarts.get(0), victoire(quarts.get(0), false));

        assertEquals(ResultatSaisieScore.Motif.SUITE_DEJA_JOUEE, r.getMotif());
        assertEquals(quarts.get(0).getJoueurA(), quarts.get(0).getVainqueur());
    }

    @Test
    void correction_du_score_sans_changer_le_vainqueur_reste_possible_apres_la_suite() {
        Tableau tableau = phaseFinaleConstruite(12);
        TableauElimination elim = tableau.getPhaseFinale();
        List<Match> quarts = elim.getPremierTour().getMatchs();
        service.saisirScore(tableau, quarts.get(0), victoire(quarts.get(0), true));
        service.saisirScore(tableau, quarts.get(1), victoire(quarts.get(1), true));
        Match demi = elim.getTours().get(1).getMatchs().get(0);
        service.saisirScore(tableau, demi, victoire(demi, true));

        List<Manche> corrige = List.of(new Manche(11, 9), new Manche(8, 11), new Manche(11, 6), new Manche(11, 4));
        assertTrue(service.saisirScore(tableau, quarts.get(0), corrige).estValide());
        assertEquals(corrige, quarts.get(0).getManches());
    }

    // ------------------------------------------------------------------
    // Podium
    // ------------------------------------------------------------------

    @Test
    void podium_vide_tant_que_rien_n_est_joue() {
        Tableau tableau = phaseFinaleConstruite(12);
        PodiumPhaseFinale p = ServicePhaseFinale.podium(tableau.getPhaseFinale());
        assertNull(p.premier());
        assertTrue(p.troisiemes().isEmpty());
        assertFalse(p.estCommence());
    }

    @Test
    void podium_sans_petite_finale_deux_troisiemes_ex_aequo() {
        Tableau tableau = phaseFinaleConstruite(12);
        TableauElimination elim = tableau.getPhaseFinale();
        assertNull(ServicePhaseFinale.petiteFinale(elim));
        jouerToutLeTableau(tableau);

        PodiumPhaseFinale p = ServicePhaseFinale.podium(elim);
        Match finale = ServicePhaseFinale.matchFinale(elim);
        assertEquals(finale.getJoueurA(), p.premier());
        assertEquals(finale.getJoueurB(), p.deuxieme());
        assertEquals(2, p.troisiemes().size());
        assertTrue(p.troisiemesExAequo());
        assertNull(p.quatrieme());
        assertEquals(DiagnosticPhaseFinale.Etat.TERMINEE, service.diagnostiquer(tableau).getEtat());
    }

    @Test
    void podium_avec_petite_finale_un_seul_troisieme_et_un_quatrieme() {
        Tableau tableau = phaseFinaleConstruite(12,
                ConfigurationTableau.depuisFormat(formatAvecPetiteFinale()).build());
        TableauElimination elim = tableau.getPhaseFinale();
        Tour petiteFinale = ServicePhaseFinale.petiteFinale(elim);
        assertNotNull(petiteFinale);
        assertEquals(3, ServicePhaseFinale.toursPrincipaux(elim).size());

        jouerToutLeTableau(tableau);

        PodiumPhaseFinale p = ServicePhaseFinale.podium(elim);
        Match pf = petiteFinale.getMatchs().get(0);
        assertTrue(p.avecPetiteFinale());
        assertEquals(List.of(pf.getVainqueur()), p.troisiemes());
        assertEquals(pf.getPerdant(), p.quatrieme());
        assertFalse(p.troisiemesExAequo());
    }

    @Test
    void phase_finale_pas_terminee_tant_que_la_petite_finale_n_est_pas_jouee() {
        Tableau tableau = phaseFinaleConstruite(12,
                ConfigurationTableau.depuisFormat(formatAvecPetiteFinale()).build());
        TableauElimination elim = tableau.getPhaseFinale();
        for (Tour tour : ServicePhaseFinale.toursPrincipaux(elim)) {
            for (Match m : tour.getMatchs()) {
                service.saisirScore(tableau, m, victoire(m, true));
            }
        }
        assertEquals(DiagnosticPhaseFinale.Etat.EN_COURS, service.diagnostiquer(tableau).getEtat());
        Match pf = ServicePhaseFinale.petiteFinale(elim).getMatchs().get(0);
        assertEquals(StatutMatch.A_JOUER, pf.getStatut());
        service.saisirScore(tableau, pf, victoire(pf, false));
        assertEquals(DiagnosticPhaseFinale.Etat.TERMINEE, service.diagnostiquer(tableau).getEtat());
    }

    @Test
    void correction_d_une_demi_met_a_jour_finale_et_petite_finale_non_jouees() {
        Tableau tableau = phaseFinaleConstruite(12,
                ConfigurationTableau.depuisFormat(formatAvecPetiteFinale()).build());
        TableauElimination elim = tableau.getPhaseFinale();
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        for (Match m : principaux.get(0).getMatchs()) {
            service.saisirScore(tableau, m, victoire(m, true));
        }
        Match demi = principaux.get(1).getMatchs().get(0);
        service.saisirScore(tableau, demi, victoire(demi, true));
        Match finale = ServicePhaseFinale.matchFinale(elim);
        Match pf = ServicePhaseFinale.petiteFinale(elim).getMatchs().get(0);
        assertEquals(demi.getJoueurA(), finale.getJoueurA());
        assertEquals(demi.getJoueurB(), pf.getJoueurA());

        assertTrue(service.saisirScore(tableau, demi, victoire(demi, false)).estValide());

        assertEquals(demi.getJoueurB(), finale.getJoueurA());
        assertEquals(demi.getJoueurA(), pf.getJoueurA());
    }
}
