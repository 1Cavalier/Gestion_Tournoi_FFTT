package com.pingmanager.ui.infra;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.ResultatVerificationLancement;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.ui.ContexteApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Tournoi de démonstration du jour J : une étape du juge-arbitre par tableau. */
class TournoiDemoJourJTest {

    @TempDir
    Path dossier;

    private static Tableau tab(Tournoi t, String lettre) {
        return t.getTableaux().stream().filter(x -> x.getNom().startsWith(lettre + " ")).findFirst().orElseThrow();
    }

    @Test
    void chaque_tableau_montre_une_etape_du_jour_j() {
        Tournoi t = TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS);
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
        assertEquals(DonneesDemo.CLUB_BRUNOY, t.getNumeroAffiliationClub());

        Tableau a = tab(t, "A");
        assertEquals(StatutTableau.INSCRIPTION, a.getStatut());
        assertEquals(17, a.getInscrits().size());

        Tableau b = tab(t, "B");
        assertEquals(StatutTableau.POINTAGE, b.getStatut());
        assertFalse(b.pointageEstCloture());
        assertEquals(12, b.getInscrits().size());
        assertEquals(3, b.getFileAttente().size(), "liste d'attente à repêcher à la clôture");
        assertEquals(4, ServicePointage.nonPointes(b).size());

        Tableau c = tab(t, "C");
        assertEquals(StatutTableau.LANCE, c.getStatut());
        assertEquals(Long.valueOf(TournoiDemoJourJ.GRAINE_TABLEAU_C), c.getGrainePoules());
        assertEquals(4, c.getPoules().size());
        assertTrue(c.getPoules().get(0).estTerminee());
        Match forfait = c.getPoules().get(1).getMatchs().get(1);
        assertEquals(IssueMatch.FORFAIT, forfait.getIssue());
        assertFalse(c.poulesTerminees());

        Tableau d = tab(t, "D");
        assertTrue(d.getInscrits().stream().allMatch(j -> j.getSexe() == Sexe.FEMME));
        ResultatVerificationLancement r = new ServiceLancementTableau(t).verifierLancement(d);
        assertTrue(r.estPret(), r.toString());
        assertTrue(r.getNonPointes().isEmpty());
    }

    @Test
    void tableau_e_poules_terminees_pret_pour_le_tableau_final_avec_petite_finale() {
        Tournoi t = TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS);
        Tableau e = tab(t, "E");
        assertEquals(StatutTableau.LANCE, e.getStatut());
        assertTrue(e.poulesTerminees());
        assertTrue(e.getConfiguration().avecPetiteFinale());
        assertNull(e.getGraineTableauFinal());
        assertEquals(DiagnosticPhaseFinale.Etat.PRETE_A_CONSTRUIRE,
                new ServicePhaseFinale().diagnostiquer(e).getEtat());
        // Le tirage du tableau final ajoute bien la petite finale.
        assertTrue(new ServicePhaseFinale().construire(e, 1L).estConstruit());
        assertNotNull(ServicePhaseFinale.petiteFinale(e.getPhaseFinale()));
    }

    @Test
    void tableau_f_tableau_final_en_cours_avec_petite_finale_et_un_match_sur_une_table() {
        Tournoi t = TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS);
        Tableau f = tab(t, "F");
        assertEquals(StatutTableau.PHASE_FINALE, f.getStatut());
        assertEquals(Long.valueOf(TournoiDemoJourJ.GRAINE_TABLEAU_FINAL_F), f.getGraineTableauFinal());
        assertEquals(Long.valueOf(TournoiDemoJourJ.GRAINE_POULES_F), f.getGrainePoules());
        var elim = f.getPhaseFinale();
        assertNotNull(ServicePhaseFinale.petiteFinale(elim));
        assertTrue(elim.getCadre() > elim.getNombreJoueurs(), "des exempts dans l'arbre");
        var principaux = ServicePhaseFinale.toursPrincipaux(elim);
        assertTrue(principaux.get(0).getMatchs().stream().allMatch(m -> m.getStatut() == StatutMatch.TERMINE));
        Match enCours = principaux.get(1).getMatchs().stream()
                .filter(m -> m.getStatut() == StatutMatch.EN_COURS).findFirst().orElseThrow();
        assertEquals(Integer.valueOf(TournoiDemoJourJ.TABLE_EN_COURS_F), enCours.getNumeroTable());
        assertEquals(DiagnosticPhaseFinale.Etat.EN_COURS, new ServicePhaseFinale().diagnostiquer(f).getEtat());
        assertNull(ServicePhaseFinale.podium(elim).premier());
    }

    @Test
    void la_demo_se_relit_a_l_identique() throws Exception {
        Tournoi t = TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS);
        Path f = dossier.resolve("jourj" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertEquals(Long.valueOf(TournoiDemoJourJ.GRAINE_TABLEAU_C), tab(relu, "C").getGrainePoules());
        assertEquals(ServicePointage.nonPointes(tab(t, "B")).size(), ServicePointage.nonPointes(tab(relu, "B")).size());
        assertTrue(tab(relu, "D").pointageEstCloture());
        assertTrue(tab(relu, "E").poulesTerminees());
        assertTrue(tab(relu, "E").getConfiguration().avecPetiteFinale());
        Tableau tf = tab(relu, "F");
        assertEquals(Long.valueOf(TournoiDemoJourJ.GRAINE_TABLEAU_FINAL_F), tf.getGraineTableauFinal());
        assertNotNull(ServicePhaseFinale.petiteFinale(tf.getPhaseFinale()));
        assertTrue(ServicePhaseFinale.toursPrincipaux(tf.getPhaseFinale()).get(1).getMatchs().stream()
                .anyMatch(m -> m.getStatut() == StatutMatch.EN_COURS
                        && Integer.valueOf(TournoiDemoJourJ.TABLE_EN_COURS_F).equals(m.getNumeroTable())));
    }

    @Test
    void salle_avec_deux_parties_en_cours_et_la_poule_d_prete() throws Exception {
        java.time.Instant maintenant = java.time.Instant.parse("2026-11-14T10:30:00Z");
        Tournoi t = TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS, maintenant);
        ServiceSalle salle = new ServiceSalle(() -> maintenant);
        TableauDeBordSalle bord = salle.tableauDeBord(t);

        // Depuis la réunion des deux démos v14 (salle et tableau final), la salle porte
        // aussi le quart de finale du tableau F en cours sur la table 3 : les attentes
        // sur C et D sont vérifiées tableau par tableau.
        String c = "C – Toutes séries";
        String d = "D – Dames";
        assertEquals(16, bord.getNombreDeTables());
        assertFalse(bord.table(2).estLibre());
        assertFalse(bord.table(5).estLibre());
        assertEquals(c, bord.table(2).match().tableau());
        assertEquals(c, bord.table(5).match().tableau());
        assertEquals(java.time.Duration.ofMinutes(15), bord.table(2).ecoule());
        assertEquals(java.time.Duration.ofMinutes(6), bord.table(5).ecoule());
        assertFalse(bord.table(TournoiDemoJourJ.TABLE_EN_COURS_F).estLibre(), "quart de finale de F");
        assertEquals(3, bord.nombreTablesOccupees(), "deux parties de C et un quart de F");
        assertEquals(3, bord.getPrets().stream().filter(m -> d.equals(m.tableau())).count(),
                "les trois parties de la poule D");
        assertEquals(2, bord.getBloques().stream().filter(b -> c.equals(b.match().tableau())).count(),
                "le reste de la poule C");
        assertTrue(bord.getAnomalies().isEmpty());

        // Les parties en cours survivent à l'enregistrement.
        Path f = dossier.resolve("salle" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        TableauDeBordSalle relu = salle.tableauDeBord(DepotTournoi.charger(f).getTournoi());
        assertEquals(3, relu.nombreTablesOccupees());
        assertEquals(java.time.Duration.ofMinutes(15), relu.table(2).ecoule());
    }

    @Test
    void visible_dans_la_gestion_des_tournois_de_quentin() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_QUENTIN, "Soumet123/").isPresent());
        Path f = ctx.fichierTournoiJourJDemo().toAbsolutePath();
        assertTrue(ctx.tournoisRecents().stream().anyMatch(p -> p.getFichier().equals(f)), "dans les récents");
        Tournoi t = ctx.chargerTournoi(f);
        assertEquals(TournoiDemoJourJ.NOM, t.getNom());
        assertTrue(ctx.estTournoiDuCompte(t));
    }
}
