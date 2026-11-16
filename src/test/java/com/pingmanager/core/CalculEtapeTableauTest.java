package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Étape d'un tableau le jour J (frise en six étapes de l'écran du juge-arbitre). */
class CalculEtapeTableauTest {

    // ------------------------------------------------------------------
    // Fonction pure : une situation, une étape
    // ------------------------------------------------------------------

    private static CalculEtapeTableau.EtapeCourante calculer(StatutTableau statut, boolean cloture,
                                                            boolean poulesTerminees, boolean sansFinal,
                                                            boolean construit, String tour, boolean termine) {
        return CalculEtapeTableau.calculer(new CalculEtapeTableau.Situation(statut, cloture, poulesTerminees,
                sansFinal, construit, tour, termine));
    }

    @Test
    void etape_1_avant_pointage_tant_que_les_inscriptions_sont_ouvertes() {
        CalculEtapeTableau.EtapeCourante e = calculer(StatutTableau.INSCRIPTION, false, false, false, false, "",
                false);
        assertEquals(EtapeTableau.AVANT_POINTAGE, e.etape());
        assertEquals(1, e.etape().numero());
        assertEquals("Avant pointage", e.libelle());
    }

    @Test
    void etape_2_pointage_ouvert_puis_etape_3_pointage_ferme_a_la_cloture() {
        assertEquals(EtapeTableau.POINTAGE_OUVERT,
                calculer(StatutTableau.POINTAGE, false, false, false, false, "", false).etape());
        CalculEtapeTableau.EtapeCourante ferme = calculer(StatutTableau.POINTAGE, true, false, false, false, "",
                false);
        assertEquals(EtapeTableau.POINTAGE_FERME, ferme.etape());
        assertEquals("Pointage fermé", ferme.libelle());
    }

    @Test
    void etape_4_phase_de_poules_et_tableau_final_a_tirer_quand_les_poules_sont_jouees() {
        CalculEtapeTableau.EtapeCourante enCours = calculer(StatutTableau.LANCE, true, false, false, false, "",
                false);
        assertEquals(EtapeTableau.POULES, enCours.etape());
        assertEquals("Phase de poules", enCours.libelle());
        CalculEtapeTableau.EtapeCourante jouees = calculer(StatutTableau.LANCE, true, true, false, false, "", false);
        assertEquals(EtapeTableau.POULES, jouees.etape(), "le tableau final n'est pas encore tiré");
        assertEquals(CalculEtapeTableau.TABLEAU_FINAL_A_TIRER, jouees.precision());
    }

    @Test
    void etape_5_tableau_final_avec_le_tour_en_cours() {
        CalculEtapeTableau.EtapeCourante e = calculer(StatutTableau.PHASE_FINALE, true, true, false, true,
                "1/8 de finale", false);
        assertEquals(EtapeTableau.TABLEAU_FINAL, e.etape());
        assertEquals(5, e.etape().numero());
        assertEquals("1/8 de finale", e.libelle());
    }

    @Test
    void etape_6_terminee_quand_la_finale_est_jouee_ou_sans_tableau_final_apres_les_poules() {
        assertEquals(EtapeTableau.TERMINE,
                calculer(StatutTableau.PHASE_FINALE, true, true, false, true, "", true).etape());
        assertEquals(EtapeTableau.TERMINE,
                calculer(StatutTableau.LANCE, true, true, true, false, "", false).etape(),
                "format qui s'arrête aux poules");
        assertEquals("Tableau terminé", calculer(StatutTableau.LANCE, true, true, true, false, "", false).libelle());
    }

    @Test
    void phase_finale_sans_arbre_fichier_incoherent_reste_en_poules() {
        assertEquals(EtapeTableau.POULES,
                calculer(StatutTableau.PHASE_FINALE, true, true, false, false, "", false).etape());
    }

    @Test
    void les_six_etapes_numerotees_dans_l_ordre_avec_libelles_courts() {
        EtapeTableau[] etapes = EtapeTableau.values();
        assertEquals(6, etapes.length);
        for (int i = 0; i < etapes.length; i++) {
            assertEquals(i + 1, etapes[i].numero());
            assertFalse(etapes[i].libelleCourt().isBlank());
            assertTrue(etapes[i].libelleCourt().length() <= etapes[i].libelle().length());
        }
        assertTrue(EtapeTableau.AVANT_POINTAGE.estAvant(EtapeTableau.POULES));
        assertFalse(EtapeTableau.POULES.estAvant(EtapeTableau.POULES));
        assertFalse(EtapeTableau.TERMINE.estAvant(EtapeTableau.POULES));
    }

    @Test
    void situation_obligatoire_et_tour_nul_lu_comme_vide() {
        assertThrows(NullPointerException.class, () -> CalculEtapeTableau.calculer((CalculEtapeTableau.Situation) null));
        assertThrows(NullPointerException.class,
                () -> new CalculEtapeTableau.Situation(null, false, false, false, false, "", false));
        assertEquals("", new CalculEtapeTableau.Situation(StatutTableau.LANCE, true, false, false, false, null,
                false).tourEnCours());
    }

    // ------------------------------------------------------------------
    // Transitions sur un tableau réel, de l'inscription à la finale
    // ------------------------------------------------------------------

    private static Joueur j(int i) {
        return new Joueur("L" + i, "NOM" + i, "Prénom" + i, Sexe.HOMME, "Club " + (i % 4), 1000 + i);
    }

    private static List<Manche> victoireA(Match m) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(new Manche(11, 6));
        }
        return r;
    }

    private static Tournoi tournoi(Tableau t) {
        Tournoi tournoi = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        tournoi.ajouterTableau(t);
        return tournoi;
    }

    @Test
    void transitions_reelles_de_l_inscription_au_tableau_termine() {
        Tableau t = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .petiteFinale(true).build());
        Tournoi tournoi = tournoi(t);
        for (int i = 1; i <= 12; i++) {
            t.inscrire(j(i));
        }
        assertEquals(EtapeTableau.AVANT_POINTAGE, CalculEtapeTableau.calculer(t).etape());

        t.ouvrirPointage();
        assertEquals(EtapeTableau.POINTAGE_OUVERT, CalculEtapeTableau.calculer(t).etape());
        for (int i = 1; i <= 12; i++) {
            t.pointer(j(i));
        }
        assertEquals(EtapeTableau.POINTAGE_OUVERT, CalculEtapeTableau.calculer(t).etape(),
                "tout le monde pointé ne ferme pas le pointage");

        new ServicePointage(tournoi).cloturerPointage(t);
        assertEquals(EtapeTableau.POINTAGE_FERME, CalculEtapeTableau.calculer(t).etape());

        new ServiceLancementTableau(tournoi).lancer(t, 7L);
        assertEquals(EtapeTableau.POULES, CalculEtapeTableau.calculer(t).etape());
        assertEquals("", CalculEtapeTableau.calculer(t).precision());

        for (Poule p : t.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(victoireA(m));
            }
        }
        CalculEtapeTableau.EtapeCourante jouees = CalculEtapeTableau.calculer(t);
        assertEquals(EtapeTableau.POULES, jouees.etape());
        assertEquals(CalculEtapeTableau.TABLEAU_FINAL_A_TIRER, jouees.precision());

        ServicePhaseFinale service = new ServicePhaseFinale();
        assertTrue(service.construire(t, 5L).estConstruit());
        CalculEtapeTableau.EtapeCourante premierTour = CalculEtapeTableau.calculer(t);
        assertEquals(EtapeTableau.TABLEAU_FINAL, premierTour.etape());
        assertEquals(CalculEtapeTableau.tourEnCours(t.getPhaseFinale()), premierTour.precision());
        assertFalse(premierTour.precision().isEmpty());

        // Tour par tour : la précision suit le tour en cours, jusqu'à la finale jouée.
        List<String> tours = new ArrayList<>();
        for (int garde = 0; garde < 20 && !ServicePhaseFinale.estTerminee(t.getPhaseFinale()); garde++) {
            CalculEtapeTableau.EtapeCourante e = CalculEtapeTableau.calculer(t);
            assertEquals(EtapeTableau.TABLEAU_FINAL, e.etape());
            tours.add(e.precision());
            List<Match> prets = new ArrayList<>();
            for (Tour tour : t.getPhaseFinale().getTours()) {
                for (Match m : tour.getMatchs()) {
                    if (!m.estBye() && m.getStatut() != StatutMatch.TERMINE
                            && !m.getJoueurA().estADeterminer() && !m.getJoueurB().estADeterminer()) {
                        prets.add(m);
                    }
                }
            }
            for (Match m : prets) {
                service.saisirScore(t, m, victoireA(m));
            }
        }
        assertTrue(tours.contains("Finale et petite finale"), tours.toString());
        assertEquals(EtapeTableau.TERMINE, CalculEtapeTableau.calculer(t).etape());
        assertEquals("", CalculEtapeTableau.calculer(t).precision());
    }

    @Test
    void format_sans_tableau_final_termine_a_la_fin_des_poules() {
        Tableau t = new Tableau("Poules seules", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .formatPhaseFinale(FormatPhaseFinale.AUCUNE).build());
        Tournoi tournoi = tournoi(t);
        t.ouvrirPointage();
        for (int i = 1; i <= 6; i++) {
            t.inscrire(j(i));
            t.pointer(j(i));
        }
        new ServicePointage(tournoi).cloturerPointage(t);
        new ServiceLancementTableau(tournoi).lancer(t, 3L);
        assertEquals(EtapeTableau.POULES, CalculEtapeTableau.calculer(t).etape());
        for (Poule p : t.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(victoireA(m));
            }
        }
        assertEquals(EtapeTableau.TERMINE, CalculEtapeTableau.calculer(t).etape());
    }
}
