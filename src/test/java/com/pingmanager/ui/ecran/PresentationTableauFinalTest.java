package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Page Jour J reliée au tableau final : action proposée à chaque étape, résumé
 * du tableau final, textes de la petite finale. Sans JavaFX.
 */
class PresentationTableauFinalTest {

    private static Tableau poulesJouees(int n, boolean petiteFinale) {
        Tableau modele = new Tableau("E", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .petiteFinale(petiteFinale).build());
        for (Joueur joueur : TableauEssaiPhaseFinale.joueursFictifs(n)) {
            modele.inscrire(joueur);
        }
        return TableauEssaiPhaseFinale.creer(modele, 4L);
    }

    @Test
    void action_proposee_a_chaque_etape_dont_la_construction_du_tableau_final() {
        assertEquals("Ouvrir le pointage", PresentationJourJ.libelleAction(PresentationJourJ.Etape.INSCRIPTION));
        assertEquals("Pointer", PresentationJourJ.libelleAction(PresentationJourJ.Etape.POINTAGE_OUVERT));
        assertEquals("Lancer le tirage", PresentationJourJ.libelleAction(PresentationJourJ.Etape.POINTAGE_CLOTURE));
        assertEquals("Poules", PresentationJourJ.libelleAction(PresentationJourJ.Etape.POULES));
        assertEquals("Construire le tableau final",
                PresentationJourJ.libelleAction(PresentationJourJ.Etape.POULES_TERMINEES));
        assertEquals("Tableau final", PresentationJourJ.libelleAction(PresentationJourJ.Etape.PHASE_FINALE));
    }

    @Test
    void poules_terminees_puis_construction_change_l_etape_et_le_resume() {
        Tableau tab = poulesJouees(12, true);
        assertEquals(PresentationJourJ.Etape.POULES_TERMINEES, PresentationJourJ.etape(tab));
        assertTrue(new ServicePhaseFinale().construire(tab, 9L).estConstruit());
        assertEquals(PresentationJourJ.Etape.PHASE_FINALE, PresentationJourJ.etape(tab));
        // Tableau de 8 : 4 quarts, 2 demies, la finale et la petite finale.
        assertEquals("Tableau de 8 · 0 / 8 matchs joués · petite finale", PresentationJourJ.resumeTableauFinal(tab));

        TableauElimination elim = tab.getPhaseFinale();
        Match quart = elim.getPremierTour().getMatchs().get(0);
        new ServicePhaseFinale().saisirScore(tab, quart, List.of(new Manche(11, 1), new Manche(11, 1), new Manche(11, 1)));
        assertEquals("Tableau de 8 · 1 / 8 matchs joués · petite finale", PresentationJourJ.resumeTableauFinal(tab));
    }

    @Test
    void resume_du_tableau_final_termine() {
        Tableau tab = poulesJouees(12, false);
        new ServicePhaseFinale().construire(tab, 9L);
        for (Tour tour : tab.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                new ServicePhaseFinale().saisirScore(tab, m,
                        List.of(new Manche(11, 3), new Manche(11, 3), new Manche(11, 3)));
            }
        }
        assertEquals("Tableau final terminé · 7 matchs joués", PresentationJourJ.resumeTableauFinal(tab));
    }

    @Test
    void sans_tableau_final_le_resume_reste_celui_des_poules() {
        Tableau tab = poulesJouees(6, false);
        assertEquals(PresentationJourJ.resumePoules(tab), PresentationJourJ.resumeTableauFinal(tab));
    }

    @Test
    void textes_de_la_petite_finale_avant_et_apres_le_tirage() {
        assertTrue(PresentationPhaseFinale.textePetiteFinaleAvantTirage(true).startsWith("Petite finale prévue"));
        assertTrue(PresentationPhaseFinale.textePetiteFinaleAvantTirage(false).contains("3es ex aequo"));

        Tableau avec = poulesJouees(12, true);
        new ServicePhaseFinale().construire(avec, 9L);
        assertEquals("", PresentationPhaseFinale.notePetiteFinale(true, avec.getPhaseFinale()));
        Tableau sans = poulesJouees(12, false);
        new ServicePhaseFinale().construire(sans, 9L);
        assertEquals("", PresentationPhaseFinale.notePetiteFinale(false, sans.getPhaseFinale()));
        // Prévue mais impossible (pas de petite finale dans le tableau construit).
        assertTrue(PresentationPhaseFinale.notePetiteFinale(true, sans.getPhaseFinale())
                .startsWith("Petite finale prévue, mais"));
    }

    @Test
    void le_jeu_d_essai_reprend_l_option_petite_finale_du_modele() {
        assertTrue(poulesJouees(12, true).getConfiguration().avecPetiteFinale());
        assertFalse(poulesJouees(12, false).getConfiguration().avecPetiteFinale());
    }
}
