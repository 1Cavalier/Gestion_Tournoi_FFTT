package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B043 : définition d'un tournoi « en cours » (groupe de navigation Tournoi en cours). */
class ServiceTournoiEnCoursTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate SAMEDI = LocalDate.of(2026, 11, 14);
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    private static ServiceTournoiEnCours a(LocalDate jour, int heure) {
        return new ServiceTournoiEnCours(() -> ZonedDateTime.of(jour.atTime(heure, 30), PARIS).toInstant(), PARIS);
    }

    private static Tournoi tournoi(StatutTournoi statut, LocalDate... jours) {
        Tournoi t = new Tournoi("Open", "Brunoy", List.of(jours));
        t.ajouterTableau(new Tableau("A", ConfigurationTableau.builder().placesMax(8).build()));
        if (statut == StatutTournoi.HOMOLOGUE || statut == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.HOMOLOGUE);
        }
        if (statut == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.PUBLIE);
        }
        if (statut == StatutTournoi.ANNULE) {
            t.changerStatut(StatutTournoi.ANNULE);
        }
        return t;
    }

    @Test
    void tournoi_publie_dont_un_jour_est_aujourd_hui_est_en_cours() {
        assertTrue(a(SAMEDI, 9).estEnCours(tournoi(StatutTournoi.PUBLIE, SAMEDI)));
        assertTrue(a(DIMANCHE, 9).estEnCours(tournoi(StatutTournoi.HOMOLOGUE, SAMEDI, DIMANCHE)));
    }

    @Test
    void brouillon_du_jour_sans_tableau_commence_n_est_pas_en_cours() {
        assertFalse(a(SAMEDI, 9).estEnCours(tournoi(StatutTournoi.BROUILLON, SAMEDI)));
    }

    @Test
    void tournoi_annule_n_est_jamais_en_cours_meme_force() {
        Tournoi t = tournoi(StatutTournoi.ANNULE, SAMEDI);
        assertFalse(a(SAMEDI, 9).estEnCours(t));
        assertTrue(a(SAMEDI, 9).enCours(List.of(t), x -> x, x -> true).isEmpty());
    }

    @Test
    void tournoi_d_hier_dont_un_tableau_est_commence_reste_en_cours_apres_minuit() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE, SAMEDI);
        t.getTableaux().get(0).ouvrirPointage();
        assertTrue(a(DIMANCHE, 0).estEnCours(t), "le tournoi déborde après minuit");
        assertFalse(a(DIMANCHE.plusDays(1), 9).estEnCours(t), "au-delà du lendemain, plus en cours");
    }

    @Test
    void tournoi_d_hier_sans_tableau_commence_n_est_plus_en_cours() {
        assertFalse(a(DIMANCHE, 0).estEnCours(tournoi(StatutTournoi.PUBLIE, SAMEDI)));
    }

    @Test
    void tableau_sans_phase_finale_dont_les_poules_sont_vides_n_est_pas_termine() {
        Tableau tab = new Tableau("B", ConfigurationTableau.builder().placesMax(8).build());
        assertFalse(ServiceTournoiEnCours.estTermine(tab));
        tab.ouvrirPointage();
        assertFalse(ServiceTournoiEnCours.estTermine(tab));
    }

    @Test
    void plusieurs_tournois_en_cours_le_plus_proche_d_aujourd_hui_d_abord() {
        Tournoi hier = tournoi(StatutTournoi.PUBLIE, SAMEDI);
        hier.getTableaux().get(0).ouvrirPointage();
        Tournoi aujourdhui = tournoi(StatutTournoi.PUBLIE, DIMANCHE);
        Tournoi futur = tournoi(StatutTournoi.PUBLIE, DIMANCHE.plusDays(20));
        List<Tournoi> res = a(DIMANCHE, 10).enCours(List.of(hier, futur, aujourdhui), x -> x, x -> false);
        assertEquals(List.of(aujourdhui, hier), res);
    }

    @Test
    void un_tournoi_force_comme_la_demo_est_en_cours_meme_hors_de_ses_jours() {
        Tournoi demo = tournoi(StatutTournoi.PUBLIE, SAMEDI.plusDays(200));
        assertEquals(List.of(demo), a(SAMEDI, 9).enCours(List.of(demo), x -> x, x -> x == demo));
    }

    @Test
    void la_date_du_jour_suit_le_fuseau() {
        // 23 h 30 à Paris le samedi = 22 h 30 UTC : on est encore samedi.
        assertEquals(SAMEDI, a(SAMEDI, 23).aujourdhui());
    }
}
