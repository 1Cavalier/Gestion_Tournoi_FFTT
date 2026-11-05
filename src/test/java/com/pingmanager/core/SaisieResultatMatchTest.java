package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaisieResultatMatchTest {

    private final SaisieResultatMatch saisie = new SaisieResultatMatch(() -> Instant.parse("2026-11-14T10:00:00Z"));

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    /** Tableau P3Q2 lancé avec 6 présents (2 poules de 3). */
    private static Tableau tableauLance() {
        Tableau tab = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(0).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (int i = 1; i <= 6; i++) {
            Joueur joueur = j("L" + i, 1500 - i * 10);
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 7L);
        return tab;
    }

    private static List<Manche> m(int... points) {
        Manche[] res = new Manche[points.length / 2];
        for (int i = 0; i < res.length; i++) {
            res[i] = new Manche(points[2 * i], points[2 * i + 1]);
        }
        return List.of(res);
    }

    @Test
    void manche_valide_selon_l_article_2_11_1() {
        assertTrue(SaisieResultatMatch.mancheValide(new Manche(11, 0)));
        assertTrue(SaisieResultatMatch.mancheValide(new Manche(11, 9)));
        assertTrue(SaisieResultatMatch.mancheValide(new Manche(12, 10)));
        assertTrue(SaisieResultatMatch.mancheValide(new Manche(15, 17)));
        assertFalse(SaisieResultatMatch.mancheValide(new Manche(11, 10)), "deux points d'écart après 10-10");
        assertFalse(SaisieResultatMatch.mancheValide(new Manche(10, 8)), "11 points pour gagner");
        assertFalse(SaisieResultatMatch.mancheValide(new Manche(13, 9)), "la manche s'arrête à 11");
        assertFalse(SaisieResultatMatch.mancheValide(new Manche(14, 11)));
        assertFalse(SaisieResultatMatch.mancheValide(new Manche(11, 11)));
    }

    @Test
    void score_complet_au_meilleur_des_5_enregistre_avec_heure_de_fin() {
        Tableau tab = tableauLance();
        Match match = tab.getPoules().get(0).getMatchs().get(0);
        ResultatSaisieMatch r = saisie.enregistrerScore(tab, match, m(11, 9, 8, 11, 11, 5, 12, 10));
        assertTrue(r.estEnregistre(), r.toString());
        assertEquals(StatutMatch.TERMINE, match.getStatut());
        assertEquals(match.getJoueurA(), match.getVainqueur());
        assertEquals(4, match.getManches().size());
        assertNotNull(match.getInstantFin());
    }

    @Test
    void scores_incoherents_refuses_avec_motif_precis() {
        Tableau tab = tableauLance();
        Match match = tab.getPoules().get(0).getMatchs().get(0);
        assertEquals(ResultatSaisieMatch.Motif.AUCUNE_MANCHE, saisie.enregistrerScore(tab, match, List.of()).getMotif());

        ResultatSaisieMatch invalide = saisie.enregistrerScore(tab, match, m(11, 9, 11, 10, 11, 3));
        assertEquals(ResultatSaisieMatch.Motif.MANCHE_INVALIDE, invalide.getMotif());
        assertEquals(2, invalide.getNumeroManche());

        assertEquals(ResultatSaisieMatch.Motif.MATCH_NON_DECIDE,
                saisie.enregistrerScore(tab, match, m(11, 9, 9, 11)).getMotif());
        ResultatSaisieMatch apres = saisie.enregistrerScore(tab, match, m(11, 9, 11, 9, 11, 9, 9, 11));
        assertEquals(ResultatSaisieMatch.Motif.MANCHE_APRES_DECISION, apres.getMotif());
        assertEquals(4, apres.getNumeroManche());
        assertEquals(ResultatSaisieMatch.Motif.TROP_DE_MANCHES,
                saisie.enregistrerScore(tab, match, m(11, 9, 9, 11, 11, 9, 9, 11, 11, 9, 11, 9)).getMotif());
        assertEquals(StatutMatch.A_JOUER, match.getStatut(), "rien n'est enregistré sur un refus");
    }

    @Test
    void forfait_donne_la_victoire_a_l_adversaire_et_zero_point_au_fautif() {
        Tableau tab = tableauLance();
        Poule poule = tab.getPoules().get(0);
        Match match = poule.getMatchs().get(0);
        // Une saisie antérieure est remplacée par l'issue non normale.
        saisie.enregistrerScore(tab, match, m(11, 9, 11, 9, 11, 9));
        ResultatSaisieMatch r = saisie.enregistrerIssue(tab, match, IssueMatch.FORFAIT, match.getJoueurA());
        assertTrue(r.estEnregistre());
        assertEquals(match.getJoueurB(), match.getVainqueur());
        assertTrue(match.getManches().isEmpty());

        // I.303 a) : absence à l'appel → 0 point au classement.
        LigneClassement fautif = new ClassementPouleService().calculer(poule).stream()
                .filter(l -> l.getJoueur().equals(match.getJoueurA())).findFirst().orElseThrow();
        assertEquals(0, fautif.getPointsClassement());
        assertEquals(1, fautif.getMatchsJoues());
    }

    @Test
    void issue_refusee_si_normale_ou_fautif_hors_match() {
        Tableau tab = tableauLance();
        Match match = tab.getPoules().get(0).getMatchs().get(0);
        assertEquals(ResultatSaisieMatch.Motif.ISSUE_NORMALE,
                saisie.enregistrerIssue(tab, match, IssueMatch.NORMALE, match.getJoueurA()).getMotif());
        assertEquals(ResultatSaisieMatch.Motif.FAUTIF_HORS_MATCH,
                saisie.enregistrerIssue(tab, match, IssueMatch.ABANDON, j("X", 500)).getMotif());
    }

    @Test
    void effacer_remet_le_match_a_jouer() {
        Tableau tab = tableauLance();
        Match match = tab.getPoules().get(0).getMatchs().get(0);
        saisie.enregistrerIssue(tab, match, IssueMatch.ABANDON, match.getJoueurB());
        assertTrue(saisie.effacer(tab, match).estEnregistre());
        assertEquals(StatutMatch.A_JOUER, match.getStatut());
        assertEquals(IssueMatch.NORMALE, match.getIssue());
        assertNull(match.getJoueurFautif());
        assertNull(match.getVainqueur());
        assertNull(match.getInstantFin());
    }

    @Test
    void saisie_refusee_hors_phase_de_poules_ou_hors_tableau() {
        Tableau tab = tableauLance();
        Tableau autre = tableauLance();
        Match etranger = autre.getPoules().get(0).getMatchs().get(0);
        assertEquals(ResultatSaisieMatch.Motif.MATCH_HORS_TABLEAU,
                saisie.enregistrerScore(tab, etranger, m(11, 9, 11, 9, 11, 9)).getMotif());

        // Toutes les poules jouées puis phase finale construite : poules figées.
        for (Poule p : tab.getPoules()) {
            for (Match x : p.getMatchs()) {
                assertTrue(saisie.enregistrerScore(tab, x, m(11, 9, 11, 9, 11, 9)).estEnregistre());
            }
        }
        new ServiceLancementTableau().construirePhaseFinale(tab, 3L);
        Match joue = tab.getPoules().get(0).getMatchs().get(0);
        assertEquals(ResultatSaisieMatch.Motif.POULES_NON_MODIFIABLES, saisie.effacer(tab, joue).getMotif());
        assertEquals(StatutMatch.TERMINE, joue.getStatut());
    }
}
