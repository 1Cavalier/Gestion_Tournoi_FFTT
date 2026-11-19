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
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Un match se joue sur une table : son score ne s'enregistre que s'il a été
 * lancé (en cours). Seul le forfait d'un joueur absent, déclaré par le
 * juge-arbitre, se saisit sans lancement (I.303 a : l'absent marque 0 point).
 * Règle vérifiée pour les poules ({@link SaisieResultatMatch}) et pour le
 * tableau final ({@link ServicePhaseFinale}).
 */
class SaisieMatchNonLanceTest {

    private static final Instant MAINTENANT = Instant.parse("2027-04-10T10:00:00Z");

    private final SaisieResultatMatch saisie = new SaisieResultatMatch(() -> MAINTENANT);
    private final ServiceSalle salle = new ServiceSalle(() -> MAINTENANT);

    private Tournoi tournoi;

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    /** Tableau P3Q2 lancé (poules tirées), dans un tournoi de 4 tables. */
    private Tableau tableauLance(int nbJoueurs) {
        Tableau tab = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(0).build());
        tournoi = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        tournoi.setNombreDeTables(4);
        tournoi.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j("L" + i, 1500 - i * 10);
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(tournoi).cloturerPointage(tab);
        new ServiceLancementTableau(tournoi).lancer(tab, 7L);
        return tab;
    }

    private static List<Manche> victoireA() {
        return List.of(new Manche(11, 5), new Manche(11, 7), new Manche(11, 9));
    }

    private Match premierMatch(Tableau tab) {
        return tab.getPoules().get(0).getMatchs().get(0);
    }

    // ------------------------------------------------------------------
    // Poules
    // ------------------------------------------------------------------

    @Test
    void score_de_poule_refuse_sur_un_match_a_jouer_avec_motif_match_non_lance() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertFalse(SaisieResultatMatch.scoreSaisissable(m));

        ResultatSaisieMatch r = saisie.enregistrerScore(tab, m, victoireA());

        assertEquals(ResultatSaisieMatch.Motif.MATCH_NON_LANCE, r.getMotif());
        assertEquals(StatutMatch.A_JOUER, m.getStatut(), "rien n'est enregistré");
        assertTrue(m.getManches().isEmpty());
        assertNull(m.getInstantFin());
    }

    @Test
    void score_de_poule_accepte_sur_un_match_lance_puis_la_table_est_liberee() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        // Phase de poules : la poule entière part sur la table 2 ; ses matchs comptent comme lancés.
        assertTrue(salle.lancerPoule(tournoi, tab.getPoules().get(0), 2).estReussi());
        assertEquals(3, salle.tableauDeBord(tournoi).nombreTablesLibres());
        assertTrue(SaisieResultatMatch.scoreSaisissable(m));

        ResultatSaisieMatch r = saisie.enregistrerScore(tab, m, victoireA());

        assertTrue(r.estEnregistre(), r.toString());
        assertEquals(StatutMatch.TERMINE, m.getStatut());
        assertEquals(ResultatAffectationTable.Motif.POULE_CONTINUE, salle.libererApresResultat(tournoi, m).getMotif());
        for (Match x : tab.getPoules().get(0).getMatchs()) {
            if (x != m) {
                assertTrue(saisie.enregistrerScore(tab, x, victoireA()).estEnregistre());
            }
        }
        Match dernier = tab.getPoules().get(0).getMatchs().get(2);
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE,
                salle.libererApresResultat(tournoi, dernier).getMotif());
        assertEquals(4, salle.tableauDeBord(tournoi).nombreTablesLibres(), "la table 2 est de nouveau libre");
    }


    @Test
    void forfait_d_un_absent_declare_sans_lancer_le_match() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);

        // I.303 a : le joueur absent marque 0 point ; aucune table n'est occupée.
        ResultatSaisieMatch r = saisie.enregistrerIssue(tab, m, IssueMatch.FORFAIT, m.getJoueurB());

        assertTrue(r.estEnregistre(), r.toString());
        assertEquals(StatutMatch.TERMINE, m.getStatut());
        assertEquals(m.getJoueurA(), m.getVainqueur());
        assertNull(m.getNumeroTable());
        assertEquals(4, salle.tableauDeBord(tournoi).nombreTablesLibres());
    }

    @Test
    void abandon_refus_et_disqualification_refuses_sur_un_match_non_lance() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        for (IssueMatch issue : List.of(IssueMatch.ABANDON, IssueMatch.REFUS_DE_JOUER, IssueMatch.DISQUALIFICATION)) {
            assertEquals(ResultatSaisieMatch.Motif.MATCH_NON_LANCE,
                    saisie.enregistrerIssue(tab, m, issue, m.getJoueurA()).getMotif(), issue.name());
        }
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
    }

    @Test
    void abandon_accepte_sur_un_match_lance_comme_avant() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        assertTrue(salle.lancerPoule(tournoi, tab.getPoules().get(0), 1).estReussi());

        assertTrue(saisie.enregistrerIssue(tab, m, IssueMatch.ABANDON, m.getJoueurA()).estEnregistre());

        assertEquals(m.getJoueurB(), m.getVainqueur());
        assertEquals(3, salle.tableauDeBord(tournoi).nombreTablesLibres(), "la poule continue sur sa table");
    }


    @Test
    void match_termine_se_corrige_sans_etre_relance() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        assertTrue(salle.lancerPoule(tournoi, tab.getPoules().get(0), 1).estReussi());
        assertTrue(saisie.enregistrerScore(tab, m, victoireA()).estEnregistre());

        List<Manche> corrige = List.of(new Manche(5, 11), new Manche(7, 11), new Manche(9, 11));
        assertTrue(saisie.enregistrerScore(tab, m, corrige).estEnregistre());
        assertEquals(m.getJoueurB(), m.getVainqueur());
    }

    @Test
    void forfait_sans_lancement_ne_devient_pas_un_score_sans_lancer_le_match() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        assertTrue(saisie.enregistrerIssue(tab, m, IssueMatch.FORFAIT, m.getJoueurB()).estEnregistre());

        assertFalse(SaisieResultatMatch.scoreSaisissable(m));
        assertEquals(ResultatSaisieMatch.Motif.MATCH_NON_LANCE, saisie.enregistrerScore(tab, m, victoireA()).getMotif());
        assertEquals(IssueMatch.FORFAIT, m.getIssue(), "le forfait reste enregistré");

        // Effacer le forfait rend le match à jouer : il se lance alors normalement.
        assertTrue(saisie.effacer(tab, m).estEnregistre());
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertTrue(salle.lancerPoule(tournoi, tab.getPoules().get(0), 1).estReussi());
        assertTrue(saisie.enregistrerScore(tab, m, victoireA()).estEnregistre());
    }

    @Test
    void match_termine_sans_lancement_d_un_ancien_fichier_reste_corrigeable() {
        Tableau tab = tableauLance(6);
        Match m = premierMatch(tab);
        // Ancien fichier : le modèle relit un match terminé qui n'a jamais été lancé.
        m.definirScore(victoireA());
        assertNull(m.getInstantLancement());

        assertTrue(SaisieResultatMatch.scoreSaisissable(m));
        List<Manche> corrige = List.of(new Manche(5, 11), new Manche(7, 11), new Manche(9, 11));
        assertTrue(saisie.enregistrerScore(tab, m, corrige).estEnregistre());
    }

    // ------------------------------------------------------------------
    // Tableau final
    // ------------------------------------------------------------------

    /** Poules jouées (matchs lancés puis terminés), tableau final construit. */
    private Tableau tableauFinalConstruit(ServicePhaseFinale service) {
        Tableau tab = tableauLance(8);
        for (Poule p : tab.getPoules()) {
            for (Match m : p.getMatchs()) {
                assertTrue(saisie.enregistrerScore(tab, MatchsLances.lance(m), victoireA()).estEnregistre());
            }
        }
        assertTrue(service.construire(tab, 42L).estConstruit());
        return tab;
    }

    private static Match premierMatchJouable(Tableau tab) {
        for (Tour tour : tab.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.getStatut() == StatutMatch.A_JOUER && !m.getJoueurA().estADeterminer()
                        && m.getJoueurB() != null && !m.getJoueurB().estADeterminer()) {
                    return m;
                }
            }
        }
        throw new IllegalStateException("aucun match jouable");
    }

    @Test
    void score_du_tableau_final_refuse_sur_un_match_a_jouer() {
        ServicePhaseFinale service = new ServicePhaseFinale();
        Tableau tab = tableauFinalConstruit(service);
        Match m = premierMatchJouable(tab);

        ResultatSaisieScore r = service.saisirScore(tab, m, victoireA());

        assertEquals(ResultatSaisieScore.Motif.MATCH_NON_LANCE, r.getMotif());
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertEquals(ResultatSaisieScore.Motif.MATCH_NON_LANCE,
                service.declarerIssue(tab, m, IssueMatch.DISQUALIFICATION, m.getJoueurA()).getMotif());
    }

    @Test
    void score_du_tableau_final_accepte_sur_un_match_lance_puis_la_table_est_liberee() {
        ServicePhaseFinale service = new ServicePhaseFinale();
        Tableau tab = tableauFinalConstruit(service);
        Match m = premierMatchJouable(tab);
        assertTrue(salle.affecter(tournoi, m, 3).estReussi());
        assertEquals(3, salle.tableauDeBord(tournoi).nombreTablesLibres());

        assertTrue(service.saisirScore(tab, m, victoireA()).estValide());

        assertEquals(StatutMatch.TERMINE, m.getStatut());
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, salle.libererApresResultat(tournoi, m).getMotif());
        assertEquals(4, salle.tableauDeBord(tournoi).nombreTablesLibres());
    }

    @Test
    void forfait_du_tableau_final_declare_sans_lancement_et_le_vainqueur_avance() {
        ServicePhaseFinale service = new ServicePhaseFinale();
        Tableau tab = tableauFinalConstruit(service);
        Match m = premierMatchJouable(tab);

        assertTrue(service.declarerIssue(tab, m, IssueMatch.FORFAIT, m.getJoueurB()).estValide());

        assertEquals(StatutMatch.TERMINE, m.getStatut());
        assertEquals(m.getJoueurA(), m.getVainqueur());
        assertNull(m.getNumeroTable());
    }
}
