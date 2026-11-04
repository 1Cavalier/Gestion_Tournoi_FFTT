package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GenerateurTableauEliminationTest {

    private final GenerateurTableauElimination generateur = new GenerateurTableauElimination();
    private final ProgressionTableau progression = new ProgressionTableau();

    /** Joueurs déjà triés par force décroissante (seed 1 = index 0). */
    private List<Joueur> joueursClasses(int n) {
        List<Joueur> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Joueur("S" + (i + 1), "Nom" + i, "Prenom" + i, Sexe.HOMME, "Club", 3000 - i * 10));
        }
        return l;
    }

    private void jouerTour(Tour tour) {
        for (Match m : tour.getMatchs()) {
            if (m.getStatut() == StatutMatch.A_JOUER && !m.estBye()) {
                m.definirScore(List.of(new Manche(11, 5), new Manche(11, 7), new Manche(11, 9)));
            }
        }
    }

    @Test
    void cadre_est_la_puissance_de_deux_superieure_ou_egale() {
        assertEquals(8, generateur.puissanceDeDeuxSuperieureOuEgale(8));
        assertEquals(16, generateur.puissanceDeDeuxSuperieureOuEgale(12));
        assertEquals(64, generateur.puissanceDeDeuxSuperieureOuEgale(33));
    }

    @Test
    void huit_joueurs_donnent_trois_tours_sans_exempt() {
        TableauElimination t = generateur.genererTableau(joueursClasses(8));
        assertEquals(8, t.getCadre());
        assertEquals(0, t.getNombreExemptes());
        assertEquals(3, t.getNombreTours());
        assertEquals(4, t.getPremierTour().getNombreMatchs());
        assertEquals("Finale", t.getFinale().getNom());
    }

    @Test
    void tete_de_serie_affronte_le_plus_faible_au_premier_tour() {
        TableauElimination t = generateur.genererTableau(joueursClasses(8));
        Match premier = t.getPremierTour().getMatchs().get(0);
        assertEquals("S1", premier.getJoueurA().getNumeroLicence());
        assertEquals("S8", premier.getJoueurB().getNumeroLicence());
    }

    @Test
    void douze_joueurs_les_quatre_meilleurs_sont_exemptes() {
        TableauElimination t = generateur.genererTableau(joueursClasses(12));
        assertEquals(16, t.getCadre());
        assertEquals(4, t.getNombreExemptes());
        assertEquals("8es de finale", t.getPremierTour().getNom());

        Set<String> auPremierTour = new HashSet<>();
        for (Match m : t.getPremierTour().getMatchs()) {
            auPremierTour.add(m.getJoueurA().getNumeroLicence());
            auPremierTour.add(m.getJoueurB().getNumeroLicence());
        }
        // Les 4 têtes de série sautent le tour préliminaire.
        assertFalse(auPremierTour.contains("S1"));
        assertFalse(auPremierTour.contains("S2"));
        assertFalse(auPremierTour.contains("S3"));
        assertFalse(auPremierTour.contains("S4"));
        // Les plus faibles le disputent.
        assertTrue(auPremierTour.contains("S12"));
    }

    @Test
    void trente_trois_joueurs_commencent_par_un_seul_trente_deuxieme() {
        TableauElimination t = generateur.genererTableau(joueursClasses(33));
        assertEquals(64, t.getCadre());
        assertEquals(31, t.getNombreExemptes());
        assertEquals("32es de finale", t.getPremierTour().getNom());
        assertEquals(1, t.getPremierTour().getNombreMatchs());
    }

    @Test
    void l_arbre_contient_exactement_n_moins_un_matchs() {
        for (int n : new int[]{2, 5, 6, 7, 9, 12, 20, 33, 64}) {
            TableauElimination t = generateur.genererTableau(joueursClasses(n));
            int total = t.getTours().stream().mapToInt(Tour::getNombreMatchs).sum();
            assertEquals(n - 1, total, "n=" + n);
        }
    }

    @Test
    void refuse_moins_de_deux_joueurs() {
        assertThrows(IllegalArgumentException.class,
                () -> generateur.genererTableau(joueursClasses(1)));
    }

    @Test
    void simulation_complete_produit_un_unique_vainqueur() {
        TableauElimination t = generateur.genererTableau(joueursClasses(12));
        int garde = 0;
        for (Tour tour : t.getTours()) {
            jouerTour(tour);
            progression.propager(t);
            if (garde++ > 50) break;
        }
        Match finale = t.getFinale().getMatchs().get(0);
        assertEquals(StatutMatch.TERMINE, finale.getStatut());
        assertNotNull(finale.getVainqueur());
        assertFalse(finale.getVainqueur().estADeterminer());
        // Avec le joueur A toujours vainqueur, la tête de série remporte le tableau.
        assertEquals("S1", finale.getVainqueur().getNumeroLicence());
    }

    @Test
    void les_exemptes_attendent_le_vainqueur_du_tour_preliminaire() {
        TableauElimination t = generateur.genererTableau(joueursClasses(12));
        jouerTour(t.getTours().get(0)); // 8es
        progression.propager(t);

        Tour quarts = t.getTours().get(1);
        // Après le 1er tour, tous les matchs du 2e tour doivent être complets et jouables.
        for (Match m : quarts.getMatchs()) {
            assertFalse(m.getJoueurA().estADeterminer());
            assertFalse(m.getJoueurB().estADeterminer());
            assertEquals(StatutMatch.A_JOUER, m.getStatut());
        }
    }
}
