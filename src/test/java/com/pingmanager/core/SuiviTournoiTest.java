package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Suivi public d'un tournoi en cours : bascule, ordre des tableaux, matchs en cours, prochaine partie. */
class SuiviTournoiTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 4, 10);

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    private static Tableau tableau(Tournoi t, String nom, int heure, int nbJoueurs) {
        Tableau tableau = new Tableau(nom,
                ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        tableau.setHoraires(new HorairesTableau(null, null, LocalDateTime.of(JOUR, java.time.LocalTime.of(heure, 0))));
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j(nom + i, 500 + 10 * i);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        return tableau;
    }

    private static void lancer(Tournoi t, Tableau tableau) {
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
    }

    private static Tournoi publie() {
        Tournoi t = new Tournoi("Open", "Club", JOUR);
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private static void jouer(Match m) {
        m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
    }

    private static List<Manche> victoireA(Match m) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(new Manche(11, 6));
        }
        return r;
    }

    @Test
    void pas_de_suivi_avant_la_publication_ni_avant_le_lancement_du_premier_tableau() {
        Tournoi brouillon = new Tournoi("Open", "Club", JOUR);
        Tableau a = tableau(brouillon, "A", 9, 6);
        lancer(brouillon, a);
        assertFalse(SuiviTournoi.estEnSuivi(brouillon), "tournoi non publié");

        Tournoi t = publie();
        Tableau b = tableau(t, "B", 9, 6);
        assertFalse(SuiviTournoi.estEnSuivi(t), "pointage seulement : la fiche reste la fiche");
        lancer(t, b);
        assertTrue(SuiviTournoi.estEnSuivi(t), "premier tableau lancé : mode suivi");
        assertFalse(SuiviTournoi.estEnSuivi(null));
    }

    @Test
    void tableaux_du_joueur_en_premier_puis_ordre_de_la_journee() {
        Tournoi t = publie();
        Tableau tard = tableau(t, "C", 14, 3);
        Tableau tot = tableau(t, "A", 8, 3);
        Tableau milieu = tableau(t, "B", 10, 3);
        assertEquals(List.of(tot, milieu, tard), SuiviTournoi.tableauxOrdonnes(t, null));
        assertEquals(List.of(tard, tot, milieu), SuiviTournoi.tableauxOrdonnes(t, "C2"));
        assertTrue(SuiviTournoi.estInscrit(tard, "C2"));
        assertFalse(SuiviTournoi.estInscrit(tot, "C2"));
    }

    @Test
    void prochaine_partie_premier_match_non_joue_de_sa_poule_dans_l_ordre_officiel() {
        Tournoi t = publie();
        Tableau a = tableau(t, "A", 9, 6);
        lancer(t, a);
        Poule poule = a.getPoules().get(0);
        Joueur moi = poule.getJoueurs().get(0);
        String licence = moi.getNumeroLicence();
        List<Match> miens = poule.getMatchs().stream()
                .filter(m -> moi.equals(m.getJoueurA()) || moi.equals(m.getJoueurB())).toList();
        jouer(miens.get(0));

        SuiviTournoi.Partie p = SuiviTournoi.prochainePartie(t, licence).orElseThrow();
        assertSame(miens.get(1), p.match());
        assertSame(poule, p.poule());
        assertTrue(p.estEnPoule());
        assertFalse(p.estEnCours());
        assertNotNull(p.adversaire(licence));
        assertNotEquals(moi, p.adversaire(licence));
        assertEquals(SuiviPoule.tourDuMatch(poule, poule.getMatchs().indexOf(miens.get(1))), p.tourDePoule());
    }

    @Test
    void prochaine_partie_d_abord_celle_qui_se_joue_en_ce_moment() {
        Tournoi t = publie();
        Tableau a = tableau(t, "A", 9, 6);
        lancer(t, a);
        Poule poule = a.getPoules().get(0);
        Joueur moi = poule.getJoueurs().get(0);
        List<Match> miens = poule.getMatchs().stream()
                .filter(m -> moi.equals(m.getJoueurA()) || moi.equals(m.getJoueurB())).toList();
        Match dernier = miens.get(miens.size() - 1);
        dernier.setStatut(StatutMatch.EN_COURS);
        dernier.setNumeroTable(4);

        SuiviTournoi.Partie p = SuiviTournoi.prochainePartie(t, moi.getNumeroLicence()).orElseThrow();
        assertSame(dernier, p.match());
        assertTrue(p.estEnCours());
        assertEquals(1, SuiviTournoi.matchsEnCours(a).size());
        assertSame(dernier, SuiviTournoi.matchsEnCours(a).get(0).match());
    }

    @Test
    void pas_de_prochaine_partie_pour_un_non_inscrit_ni_une_fois_ses_matchs_joues() {
        Tournoi t = publie();
        Tableau a = tableau(t, "A", 9, 6);
        lancer(t, a);
        assertEquals(Optional.empty(), SuiviTournoi.prochainePartie(t, "INCONNU"));
        assertEquals(Optional.empty(), SuiviTournoi.prochainePartie(t, null));

        a.getPoules().forEach(p -> p.getMatchs().forEach(SuiviTournoiTest::jouer));
        Joueur moi = a.getPoules().get(0).getJoueurs().get(0);
        assertEquals(Optional.empty(), SuiviTournoi.prochainePartie(t, moi.getNumeroLicence()),
                "poules jouées, tableau final pas encore tiré");
        assertEquals(2, SuiviTournoi.poulesTerminees(a));
    }

    @Test
    void tableau_final_tour_en_cours_matchs_en_cours_et_prochaine_partie() {
        Tournoi t = publie();
        Tableau a = tableau(t, "A", 9, 12);
        lancer(t, a);
        a.getPoules().forEach(p -> p.getMatchs().forEach(SuiviTournoiTest::jouer));
        ServicePhaseFinale service = new ServicePhaseFinale();
        assertTrue(service.construire(a, 42L).estConstruit());
        TableauElimination elim = a.getPhaseFinale();
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        assertSame(principaux.get(0), SuiviTournoi.tourEnCours(elim));

        Match premier = principaux.get(0).getMatchs().stream()
                .filter(m -> !m.estBye() && m.getStatut() != StatutMatch.TERMINE).findFirst().orElseThrow();
        premier.setStatut(StatutMatch.EN_COURS);
        premier.setNumeroTable(2);
        List<SuiviTournoi.Partie> enCours = SuiviTournoi.matchsEnCours(a);
        assertEquals(1, enCours.size());
        assertSame(principaux.get(0), enCours.get(0).tour());
        assertFalse(enCours.get(0).estEnPoule());

        String licence = premier.getJoueurA().getNumeroLicence();
        assertSame(premier, SuiviTournoi.prochainePartie(t, licence).orElseThrow().match());

        // Tout jouer : plus de tour en cours.
        for (Tour tour : elim.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (!m.estBye() && m.getStatut() != StatutMatch.TERMINE) {
                    assertTrue(service.saisirScore(a, m, victoireA(m)).estValide());
                }
            }
        }
        assertTrue(ServicePhaseFinale.estTerminee(elim));
        assertNull(SuiviTournoi.tourEnCours(elim));
        assertEquals(Optional.empty(), SuiviTournoi.prochainePartie(t, licence));
    }
}
