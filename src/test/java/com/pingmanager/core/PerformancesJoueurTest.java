package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Performances d'un joueur (« Mes tournois », tournois passés) : victoires, défaites, forfaits. */
class PerformancesJoueurTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 4, 10);

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    private static Tournoi publie() {
        Tournoi t = new Tournoi("Open", "Club", JOUR);
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private static Tableau tableauLance(Tournoi t, String nom, int heure, List<Joueur> joueurs) {
        Tableau tableau = new Tableau(nom,
                ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        tableau.setHoraires(new HorairesTableau(null, null, LocalDateTime.of(JOUR, LocalTime.of(heure, 0))));
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return tableau;
    }

    private static List<Joueur> joueurs(String prefixe, int n) {
        List<Joueur> res = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            res.add(j(prefixe + i, 500 + 10 * i));
        }
        return res;
    }

    /** Fait gagner {@code gagnant} (A ou B du match) par 3 manches à 0. */
    private static void gagner(Match m, Joueur gagnant) {
        boolean a = m.getJoueurA().equals(gagnant);
        List<Manche> manches = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            manches.add(a ? new Manche(11, 5) : new Manche(5, 11));
        }
        m.definirScore(manches);
    }

    private static List<Match> matchsDe(Poule p, Joueur joueur) {
        List<Match> res = new ArrayList<>();
        for (Match m : p.getMatchs()) {
            if (joueur.equals(m.getJoueurA()) || joueur.equals(m.getJoueurB())) {
                res.add(m);
            }
        }
        return res;
    }

    @Test
    void bilan_vide_sans_licence_pour_un_inconnu_ou_avant_le_premier_match() {
        Tournoi t = publie();
        Tableau a = tableauLance(t, "A", 9, joueurs("A", 6));
        String moi = a.getPoules().get(0).getJoueurs().get(0).getNumeroLicence();

        assertTrue(PerformancesJoueur.calculer(t, null).estVide());
        assertTrue(PerformancesJoueur.calculer(t, "INCONNU").estVide());
        assertTrue(PerformancesJoueur.calculer(null, moi).estVide());
        PerformancesJoueur.Bilan avant = PerformancesJoueur.calculer(t, moi);
        assertTrue(avant.estVide(), "aucun match décidé");
        assertTrue(avant.tableaux().isEmpty(), "un tableau sans match décidé n'apparaît pas");
    }

    @Test
    void victoire_et_defaite_sur_forfait_comptent_le_match_non_joue_non() {
        Tournoi t = publie();
        Tableau a = tableauLance(t, "A", 9, joueurs("A", 6));
        Poule poule = a.getPoules().get(0);
        Joueur moi = poule.getJoueurs().get(0);
        List<Match> miens = matchsDe(poule, moi);
        assertTrue(miens.size() >= 2);

        gagner(miens.get(0), moi);
        // Forfait de « moi » : son adversaire est déclaré vainqueur.
        miens.get(1).declarerIssue(IssueMatch.FORFAIT, moi);
        // Un match en cours (s'il y en a un troisième) ne compte pas.
        if (miens.size() > 2) {
            miens.get(2).setStatut(StatutMatch.EN_COURS);
        }

        PerformancesJoueur.Bilan b = PerformancesJoueur.calculer(t, moi.getNumeroLicence());
        assertEquals(1, b.victoires());
        assertEquals(1, b.defaites());
        assertEquals(1, b.forfaitsEtAbandons());
        assertEquals(2, b.matchsDecides());
        assertEquals(1, b.tableaux().size());
        assertSame(a, b.tableaux().get(0).tableau());

        Joueur adversaireForfait = miens.get(1).getJoueurA().equals(moi)
                ? miens.get(1).getJoueurB() : miens.get(1).getJoueurA();
        PerformancesJoueur.Bilan adv = PerformancesJoueur.calculer(t, adversaireForfait.getNumeroLicence());
        assertEquals(1, adv.victoires(), "gagné sur forfait de l'adversaire");
        assertEquals(1, adv.forfaitsEtAbandons());
    }

    @Test
    void detail_par_tableau_dans_l_ordre_de_la_journee_et_totaux() {
        Tournoi t = publie();
        Joueur moi = j("MOI", 900);
        List<Joueur> jb = joueurs("B", 5);
        jb.add(moi);
        List<Joueur> ja = joueurs("A", 5);
        ja.add(moi);
        Tableau b = tableauLance(t, "B", 14, jb);
        Tableau a = tableauLance(t, "A", 9, ja);

        for (Tableau tab : List.of(a, b)) {
            for (Poule p : tab.getPoules()) {
                for (Match m : matchsDe(p, moi)) {
                    gagner(m, moi);
                }
            }
        }
        // Dans B, une défaite ajoutée : le premier match est rejoué en faveur de l'adversaire.
        Match perdu = b.getPoules().stream().flatMap(p -> matchsDe(p, moi).stream()).findFirst().orElseThrow();
        gagner(perdu, perdu.getJoueurA().equals(moi) ? perdu.getJoueurB() : perdu.getJoueurA());

        PerformancesJoueur.Bilan bilan = PerformancesJoueur.calculer(t, "MOI");
        assertEquals(2, bilan.tableaux().size());
        assertSame(a, bilan.tableaux().get(0).tableau(), "A (9 h) avant B (14 h)");
        assertSame(b, bilan.tableaux().get(1).tableau());
        PerformancesJoueur.BilanTableau dansA = bilan.tableaux().get(0);
        PerformancesJoueur.BilanTableau dansB = bilan.tableaux().get(1);
        assertEquals(0, dansA.defaites());
        assertEquals(1, dansB.defaites());
        assertEquals(dansA.victoires() + dansB.victoires(), bilan.victoires());
        assertEquals(1, bilan.defaites());
        assertEquals(0, bilan.forfaitsEtAbandons());
    }

    @Test
    void tableau_final_compte_et_les_byes_ne_comptent_pas() {
        Tournoi t = publie();
        Tableau a = tableauLance(t, "A", 9, joueurs("A", 12));
        a.getPoules().forEach(p -> p.getMatchs().forEach(m -> gagner(m, m.getJoueurA())));
        ServicePhaseFinale service = new ServicePhaseFinale();
        assertTrue(service.construire(a, 42L).estConstruit());
        for (Tour tour : a.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                if (!m.estBye() && m.getStatut() != StatutMatch.TERMINE) {
                    List<Manche> manches = new ArrayList<>();
                    for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
                        manches.add(new Manche(11, 6));
                    }
                    assertTrue(service.saisirScore(a, m, manches).estValide());
                }
            }
        }
        // Somme sur tous les joueurs : autant de victoires que de défaites, une par match décidé
        // (poules et tableau final), les byes exclus.
        int decides = 0;
        for (SuiviTournoi.Partie p : SuiviTournoi.parties(a)) {
            if (!p.match().estBye() && p.match().getVainqueur() != null) {
                decides++;
            }
        }
        int victoires = 0;
        int defaites = 0;
        for (Joueur joueur : a.getInscrits()) {
            PerformancesJoueur.BilanTableau b = PerformancesJoueur.calculerTableau(a, joueur.getNumeroLicence());
            victoires += b.victoires();
            defaites += b.defaites();
        }
        assertEquals(decides, victoires);
        assertEquals(decides, defaites);
        assertTrue(decides > 0);
    }
}
