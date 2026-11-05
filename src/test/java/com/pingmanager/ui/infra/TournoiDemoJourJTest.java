package com.pingmanager.ui.infra;

import com.pingmanager.core.ResultatVerificationLancement;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
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
    void la_demo_se_relit_a_l_identique() throws Exception {
        Tournoi t = TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS);
        Path f = dossier.resolve("jourj" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertEquals(Long.valueOf(TournoiDemoJourJ.GRAINE_TABLEAU_C), tab(relu, "C").getGrainePoules());
        assertEquals(ServicePointage.nonPointes(tab(t, "B")).size(), ServicePointage.nonPointes(tab(relu, "B")).size());
        assertTrue(tab(relu, "D").pointageEstCloture());
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
