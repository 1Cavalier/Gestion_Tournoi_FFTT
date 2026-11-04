package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que la boucle complète d'un tableau est reliée de bout en bout :
 * inscription → pointage → lancement (poules) → poules jouées → phase finale
 * (tableau à élimination avec finale).
 */
class BoucleCompleteTableauTest {

    private final FormatTournoi format = RegistreFormats.formatParDefaut(); // P3Q2

    private Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    /** Termine tous les matchs d'une poule : le joueur A de chaque match gagne 3-0. */
    private void jouerPoule(Poule poule) {
        for (Match m : poule.getMatchs()) {
            m.definirScore(List.of(new Manche(11, 0), new Manche(11, 0), new Manche(11, 0)));
        }
    }

    private Tableau prepareEtLance(int nbJoueurs, long grainePoules) {
        Tableau tableau = new Tableau("A", ConfigurationTableau.depuisFormat(format).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j("J" + i);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, grainePoules);
        return tableau;
    }

    @Test
    void boucle_complete_jusqu_a_la_phase_finale() {
        // 12 joueurs -> 4 poules de 3 -> 2 qualifiés par poule -> 8 qualifiés.
        Tableau tableau = prepareEtLance(12, 1L);
        assertEquals(StatutTableau.LANCE, tableau.getStatut());
        assertEquals(4, tableau.getPoules().size());

        // Les poules ne sont pas terminées : la phase finale est bloquée.
        assertFalse(tableau.poulesTerminees());
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.construirePhaseFinale(tableau, 2L));

        // On joue toutes les poules.
        tableau.getPoules().forEach(this::jouerPoule);
        assertTrue(tableau.poulesTerminees());

        // Construction de la phase finale : second tirage (I.304/I.305).
        TableauElimination elim = service.construirePhaseFinale(tableau, 2L);

        assertEquals(StatutTableau.PHASE_FINALE, tableau.getStatut());
        assertTrue(tableau.aPhaseFinale());
        assertSame(elim, tableau.getPhaseFinale());
        // 8 qualifiés -> le tableau a une finale.
        assertNotNull(elim.getFinale());
        assertEquals(8, elim.getNombreJoueurs());
    }

    @Test
    void phase_finale_bloquee_si_pas_en_phase_poules() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.depuisFormat(format).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        // encore en INSCRIPTION
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.construirePhaseFinale(tableau, 1L));
    }

    @Test
    void tirage_phase_finale_reproductible() {
        Tableau t1 = prepareEtLance(12, 1L);
        Tableau t2 = prepareEtLance(12, 1L);
        t1.getPoules().forEach(this::jouerPoule);
        t2.getPoules().forEach(this::jouerPoule);

        ServiceLancementTableau service = new ServiceLancementTableau();
        TableauElimination e1 = service.construirePhaseFinale(t1, 99L);
        TableauElimination e2 = service.construirePhaseFinale(t2, 99L);

        // Même graine => même structure de premier tour (mêmes licences aux mêmes places).
        assertEquals(e1.getNombreTours(), e2.getNombreTours());
        List<Match> m1 = e1.getPremierTour().getMatchs();
        List<Match> m2 = e2.getPremierTour().getMatchs();
        assertEquals(m1.size(), m2.size());
        for (int i = 0; i < m1.size(); i++) {
            assertEquals(licence(m1.get(i).getJoueurA()), licence(m2.get(i).getJoueurA()));
            assertEquals(licence(m1.get(i).getJoueurB()), licence(m2.get(i).getJoueurB()));
        }
    }

    private String licence(Joueur joueur) {
        return joueur == null ? "null" : joueur.getNumeroLicence();
    }

    @Test
    void inscription_joueur_a_determiner_refusee() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.builder().build());
        assertThrows(IllegalArgumentException.class, () -> tableau.inscrire(Joueur.aDeterminer()));
    }

    @Test
    void lancement_refuse_si_moins_d_une_poule() {
        // 2 présents pour des poules de 3 : refus métier clair.
        Tableau tableau = new Tableau("A", ConfigurationTableau.depuisFormat(format).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        tableau.inscrire(j("1"));
        tableau.pointer(j("1"));
        tableau.inscrire(j("2"));
        tableau.pointer(j("2"));
        new ServicePointage(t).cloturerPointage(tableau);
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
    }
}
