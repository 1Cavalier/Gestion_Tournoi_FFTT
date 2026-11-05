package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ValidationScoreMatch;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Tableau d'essai (provisoire) pour tester la vue « Phase finale » à l'écran. */
class TableauEssaiPhaseFinaleTest {

    private static Tableau modele() {
        return new Tableau("Série B", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
    }

    @Test
    void essai_depuis_un_tableau_sans_inscrits_prend_des_joueurs_fictifs_et_finit_les_poules() {
        Tableau modele = modele();
        Tableau essai = TableauEssaiPhaseFinale.creer(modele, 5L);

        assertEquals("Série B (essai)", essai.getNom());
        assertEquals(StatutTableau.LANCE, essai.getStatut());
        assertTrue(essai.poulesTerminees());
        assertEquals(DiagnosticPhaseFinale.Etat.PRETE_A_CONSTRUIRE,
                new ServicePhaseFinale().diagnostiquer(essai).getEtat());
        // Le tableau réel n'est pas touché.
        assertEquals(StatutTableau.INSCRIPTION, modele.getStatut());
        assertTrue(modele.getPoules().isEmpty());
    }

    @Test
    void essai_reprend_les_inscrits_du_tableau_reel() {
        Tableau modele = modele();
        List<Joueur> joueurs = TableauEssaiPhaseFinale.joueursFictifs(9);
        joueurs.forEach(modele::inscrire);
        Tableau essai = TableauEssaiPhaseFinale.creer(modele, 5L);
        assertEquals(9, essai.getInscrits().size());
        assertTrue(essai.getInscrits().containsAll(joueurs));
        assertEquals(StatutTableau.INSCRIPTION, modele.getStatut());
    }

    @Test
    void scores_aleatoires_toujours_conformes_aux_regles_du_jeu() {
        ValidationScoreMatch validation = new ValidationScoreMatch();
        Random rng = new Random(11L);
        for (int i = 0; i < 300; i++) {
            int gagnantes = 2 + i % 3;
            List<Manche> score = TableauEssaiPhaseFinale.scoreAleatoire(gagnantes, rng);
            assertTrue(validation.valider(score, gagnantes).estValide(), score.toString());
        }
    }

    @Test
    void meme_graine_meme_essai() {
        Tableau a = TableauEssaiPhaseFinale.creer(modele(), 9L);
        Tableau b = TableauEssaiPhaseFinale.creer(modele(), 9L);
        assertEquals(a.getPoules().size(), b.getPoules().size());
        for (int i = 0; i < a.getPoules().size(); i++) {
            assertEquals(a.getPoules().get(i).getJoueurs(), b.getPoules().get(i).getJoueurs());
            assertEquals(a.getPoules().get(i).getMatchs().get(0).getManches(),
                    b.getPoules().get(i).getMatchs().get(0).getManches());
        }
    }
}
