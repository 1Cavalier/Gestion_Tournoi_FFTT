package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServicePhaseFinale;
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
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.EntreeTableau;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.LigneMatch;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.LignePoule;
import com.pingmanager.ui.ecran.PresentationSuiviTournoi.ProchainePartie;
import com.pingmanager.ui.infra.DonneesDemo;
import com.pingmanager.ui.infra.TournoiDemoJourJ;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suivi public d'un tournoi en cours, sur la démo du jour J (A inscription,
 * B pointage, C poules en cours avec deux parties en salle, D prêt, E poules
 * terminées, F tableau final en cours).
 */
class PresentationSuiviTournoiTest {

    private static Tournoi demo() {
        return TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS);
    }

    private static Tableau tab(Tournoi t, String lettre) {
        return t.getTableaux().stream().filter(x -> x.getNom().startsWith(lettre + " ")).findFirst().orElseThrow();
    }

    private static List<String> lettres(List<EntreeTableau> entrees) {
        List<String> r = new ArrayList<>();
        entrees.forEach(e -> r.add(e.nom().substring(0, 1)));
        return r;
    }

    @Test
    void la_fiche_bascule_en_suivi_des_le_premier_tableau_lance_d_un_tournoi_publie() {
        assertTrue(PresentationSuiviTournoi.afficherSuivi(demo()));
        assertFalse(PresentationSuiviTournoi.afficherSuivi(new Tournoi("Open", "Club", LocalDate.of(2027, 1, 9))));
    }

    @Test
    void un_compte_non_inscrit_voit_tous_les_tableaux_dans_l_ordre_de_la_journee() {
        Tournoi t = demo();
        List<EntreeTableau> entrees = PresentationSuiviTournoi.tableaux(t, null);
        // F 8 h 30, A 9 h, C 9 h 30, B 10 h 30, D 13 h, E 14 h.
        assertEquals(List.of("F", "A", "C", "B", "D", "E"), lettres(entrees));
        assertTrue(entrees.stream().noneMatch(EntreeTableau::estAMoi));
        assertEquals(List.of("F", "A", "C", "B", "D", "E"), lettres(PresentationSuiviTournoi.tableaux(t, "INCONNU")));
    }

    @Test
    void tableau_affiche_garde_le_choix_par_nom_sinon_le_premier_tableau_commence() {
        Tournoi t = demo();
        List<EntreeTableau> entrees = PresentationSuiviTournoi.tableaux(t, null);
        assertSame(tab(t, "F"), PresentationSuiviTournoi.tableauAffiche(entrees, null));
        assertSame(tab(t, "F"), PresentationSuiviTournoi.tableauAffiche(entrees, "Tableau disparu"));
        // Après relecture du fichier, le tableau choisi est retrouvé par son nom.
        Tournoi relu = demo();
        assertSame(tab(relu, "C"), PresentationSuiviTournoi.tableauAffiche(
                PresentationSuiviTournoi.tableaux(relu, null), tab(t, "C").getNom()));
        assertNull(PresentationSuiviTournoi.tableauAffiche(List.of(), null));
    }

    @Test
    void etape_de_chaque_tableau_vue_du_public() {
        Tournoi t = demo();
        assertEquals("Inscriptions", PresentationSuiviTournoi.etape(tab(t, "A")));
        assertEquals("Pointage", PresentationSuiviTournoi.etape(tab(t, "B")));
        assertEquals("Poules : 1 sur 4 jouée", PresentationSuiviTournoi.etape(tab(t, "C")));
        assertEquals("Poules terminées : tirage du tableau final à venir", PresentationSuiviTournoi.etape(tab(t, "E")));
        assertEquals("Tableau final : Quarts de finale", PresentationSuiviTournoi.etape(tab(t, "F")));

        assertEquals("suivi-etape-attente", PresentationSuiviTournoi.classeEtape(tab(t, "A")));
        assertEquals("suivi-etape-poules", PresentationSuiviTournoi.classeEtape(tab(t, "C")));
        assertEquals("suivi-etape-final", PresentationSuiviTournoi.classeEtape(tab(t, "F")));
        assertTrue(PresentationSuiviTournoi.texteNonCommence(tab(t, "B")).contains("pointage"));
    }

    @Test
    void matchs_en_cours_avec_leur_table_en_poule_et_au_tableau_final() {
        Tournoi t = demo();
        List<LigneMatch> c = PresentationSuiviTournoi.matchsEnCours(tab(t, "C"));
        assertEquals(2, c.size());
        assertTrue(c.get(0).situation().startsWith("Poule B, tour "), c.get(0).situation());
        assertEquals("Table 2", c.get(0).table());
        assertTrue(c.get(1).situation().startsWith("Poule C, tour 1"), c.get(1).situation());
        assertEquals("Table 5", c.get(1).table());
        assertTrue(c.get(0).enCours());
        assertEquals("En cours", c.get(0).resultat());
        assertEquals("", c.get(0).detail());

        List<LigneMatch> f = PresentationSuiviTournoi.matchsEnCours(tab(t, "F"));
        assertEquals(1, f.size());
        assertEquals("Quarts de finale", f.get(0).situation());
        assertEquals("Table " + TournoiDemoJourJ.TABLE_EN_COURS_F, f.get(0).table());

        assertTrue(PresentationSuiviTournoi.matchsEnCours(tab(t, "E")).isEmpty());
        assertFalse(PresentationSuiviTournoi.aucunMatchEnCours().isEmpty());
    }

    @Test
    void poules_avec_classement_en_direct_qualifies_et_matchs_dans_l_ordre_officiel() {
        Tournoi t = demo();
        Tableau c = tab(t, "C");
        Poule a = c.getPoules().get(0);
        Poule b = c.getPoules().get(1);
        assertEquals("Poule A · terminée", PresentationSuiviTournoi.titrePoule(a));
        assertEquals("Poule B · 2 / 3 matchs joués", PresentationSuiviTournoi.titrePoule(b));

        List<LignePoule> classement = PresentationSuiviTournoi.classement(c, a, null);
        assertEquals(3, classement.size());
        assertEquals("1er", classement.get(0).rang());
        assertEquals("2e", classement.get(1).rang());
        assertEquals(List.of("Qualifié", "Qualifié", "Non qualifié"),
                classement.stream().map(LignePoule::qualification).toList());
        assertEquals("suivi-qualifie", classement.get(0).classe());
        assertEquals("suivi-non-qualifie", classement.get(2).classe());
        assertTrue(classement.stream().noneMatch(LignePoule::estMoi));

        // Le joueur connecté est repéré dans le classement.
        String licence = a.getJoueurs().get(0).getNumeroLicence();
        assertEquals(1, PresentationSuiviTournoi.classement(c, a, licence).stream().filter(LignePoule::estMoi).count());

        List<LigneMatch> matchs = PresentationSuiviTournoi.matchsPoule(b);
        assertEquals(List.of("Tour 1", "Tour 2", "Tour 3"), matchs.stream().map(LigneMatch::situation).toList());
        Match forfait = b.getMatchs().get(1);
        assertEquals(IssueMatch.FORFAIT, forfait.getIssue());
        assertEquals("Forfait", matchs.get(1).resultat());
        assertTrue(matchs.get(1).detail().startsWith("Forfait de "), matchs.get(1).detail());
        assertTrue(matchs.get(2).enCours());
    }

    @Test
    void score_en_manches_puis_detail_des_manches() {
        Match m = new Match(new Joueur("1", "Un", "Ana", Sexe.FEMME, "Club", 900),
                new Joueur("2", "Deux", "Bea", Sexe.FEMME, "Club", 800));
        assertEquals("À jouer", PresentationSuiviTournoi.ligneMatch("Tour 1", m).resultat());
        assertEquals("", PresentationSuiviTournoi.ligneMatch("Tour 1", m).detail());
        m.definirScore(List.of(new Manche(3, 11), new Manche(11, 9), new Manche(11, 5), new Manche(11, 7)));
        LigneMatch l = PresentationSuiviTournoi.ligneMatch("Tour 1", m);
        assertEquals("3-1", l.resultat());
        assertEquals("3-11, 11-9, 11-5, 11-7", l.detail());
        assertTrue(l.vainqueurA());
        assertFalse(l.vainqueurB());
        assertEquals("", PresentationSuiviTournoi.ligneMatch("Tour 1", m).table(), "pas de table une fois joué");
    }

    @Test
    void prochaine_partie_d_un_joueur_en_train_de_jouer() {
        Tournoi t = demo();
        Match enCours = PresentationSuiviTournoiTest.enCoursTable(tab(t, "C"), 2);
        String licence = enCours.getJoueurA().getNumeroLicence();
        ProchainePartie p = PresentationSuiviTournoi.prochainePartie(t, licence).orElseThrow();
        assertEquals("Vous jouez en ce moment", p.titre());
        assertTrue(p.enCours());
        assertEquals("Table 2", p.table());
        assertTrue(p.adversaire().startsWith("Contre " + PresentationPhaseFinale.nomJoueur(enCours.getJoueurB())),
                p.adversaire());
        assertTrue(p.detail().startsWith(tab(t, "C").getNom() + " · Poule B, tour "), p.detail());

        // Ses tableaux d'abord, marqués comme les siens.
        List<EntreeTableau> entrees = PresentationSuiviTournoi.tableaux(t, licence);
        assertTrue(entrees.get(0).estAMoi());
        assertTrue(entrees.stream().anyMatch(e -> e.estAMoi() && e.nom().startsWith("C ")));
    }

    @Test
    void prochaine_partie_a_venir_sans_table_attribuee() {
        Tournoi t = demo();
        // Poule D de C : rien n'est encore envoyé en salle.
        Poule d = tab(t, "C").getPoules().get(3);
        int vues = 0;
        for (Joueur joueur : d.getJoueurs()) {
            ProchainePartie p = PresentationSuiviTournoi.prochainePartie(t, joueur.getNumeroLicence()).orElseThrow();
            if (p.enCours() || !p.detail().startsWith(tab(t, "C").getNom())) {
                continue; // joueur aussi engagé dans un autre tableau commencé
            }
            vues++;
            assertEquals("Votre prochaine partie", p.titre());
            assertTrue(p.adversaire().startsWith("Contre "), p.adversaire());
            assertEquals("Table pas encore attribuée", p.table());
            assertTrue(p.detail().contains("Poule D, tour "), p.detail());
        }
        assertTrue(vues > 0, "au moins un joueur de la poule D n'a que C en cours");
    }

    @Test
    void pas_de_prochaine_partie_pour_un_non_inscrit() {
        Tournoi t = demo();
        assertTrue(PresentationSuiviTournoi.prochainePartie(t, null).isEmpty());
        assertTrue(PresentationSuiviTournoi.prochainePartie(t, "INCONNU").isEmpty());
        assertEquals("", PresentationSuiviTournoi.texteSansProchainePartie(t, null));
        assertEquals("", PresentationSuiviTournoi.texteSansProchainePartie(t, "INCONNU"));
    }

    @Test
    void joueur_inscrit_sans_partie_a_jouer_pour_l_instant() {
        Tournoi t = demo();
        // E : poules toutes jouées, tableau final pas encore tiré.
        Tableau e = tab(t, "E");
        for (Joueur j : e.getInscrits()) {
            if (PresentationSuiviTournoi.prochainePartie(t, j.getNumeroLicence()).isEmpty()) {
                assertFalse(PresentationSuiviTournoi.texteSansProchainePartie(t, j.getNumeroLicence()).isEmpty());
                return;
            }
        }
        fail("Au moins un joueur de E n'a rien à jouer en ce moment.");
    }

    @Test
    void podium_et_etape_terminee_une_fois_le_tableau_final_joue() {
        Tournoi t = demo();
        Tableau f = tab(t, "F");
        assertFalse(PresentationSuiviTournoi.afficherPodium(f));
        assertFalse(PresentationSuiviTournoi.afficherPodium(tab(t, "C")));
        assertEquals("Petite finale", PresentationSuiviTournoi.libelleTour(f.getPhaseFinale(),
                ServicePhaseFinale.petiteFinale(f.getPhaseFinale())));

        ServicePhaseFinale service = new ServicePhaseFinale();
        for (Tour tour : f.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                if (!m.estBye() && m.getStatut() != StatutMatch.TERMINE) {
                    List<Manche> manches = new ArrayList<>();
                    for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
                        manches.add(new Manche(11, 7));
                    }
                    assertTrue(service.saisirScore(f, m, manches).estValide());
                }
            }
        }
        assertTrue(PresentationSuiviTournoi.afficherPodium(f));
        assertEquals("Terminé", PresentationSuiviTournoi.etape(f));
        assertEquals("suivi-etape-termine", PresentationSuiviTournoi.classeEtape(f));
        assertEquals(List.of("1er", "2e", "3e", "4e"),
                PresentationSuiviTournoi.podium(f).stream().map(PresentationPhaseFinale.LignePodium::place).toList());
    }

    @Test
    void textes_de_mise_a_jour_et_d_erreur_de_relecture() {
        assertEquals("Mis à jour à 14 h 05 · actualisation automatique toutes les 30 s",
                PresentationSuiviTournoi.texteMiseAJour(LocalTime.of(14, 5)));
        assertEquals("Le tournoi n'a pas pu être relu : fichier verrouillé. L'affichage précédent est conservé.",
                PresentationSuiviTournoi.erreurRelecture("fichier verrouillé"));
        assertEquals("Le tournoi n'a pas pu être relu. L'affichage précédent est conservé.",
                PresentationSuiviTournoi.erreurRelecture(null));
    }

    private static Match enCoursTable(Tableau t, int table) {
        for (Poule p : t.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (m.getStatut() == StatutMatch.EN_COURS && Integer.valueOf(table).equals(m.getNumeroTable())) {
                    return m;
                }
            }
        }
        throw new AssertionError("Aucun match en cours à la table " + table);
    }
}
