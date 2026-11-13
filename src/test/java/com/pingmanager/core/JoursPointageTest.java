package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B043 : pointage séparé par jour (tournoi d'un ou deux jours). */
class JoursPointageTest {

    private static final LocalDate SAMEDI = LocalDate.of(2026, 11, 14);
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "Brunoy", 800);
    private final Joueur bruno = new Joueur("200", "Durand", "Bruno", Sexe.HOMME, "Yerres", 900);

    private static Tableau tableau(String nom, LocalDate jour) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().placesMax(8).build());
        if (jour != null) {
            t.setHoraires(new HorairesTableau(jour.atTime(8, 0), jour.atTime(8, 45), jour.atTime(9, 0)));
        }
        return t;
    }

    /** Samedi : A ; dimanche : B ; C sans horaire. Alice est inscrite à A et B. */
    private Tournoi deuxJours() {
        Tournoi t = new Tournoi("Open", "Brunoy", List.of(SAMEDI, DIMANCHE));
        Tableau a = tableau("A", SAMEDI);
        Tableau b = tableau("B", DIMANCHE);
        Tableau c = tableau("C", null);
        t.ajouterTableau(a);
        t.ajouterTableau(b);
        t.ajouterTableau(c);
        a.inscrire(alice);
        a.inscrire(bruno);
        b.inscrire(alice);
        c.inscrire(bruno);
        return t;
    }

    @Test
    void tournoi_d_un_jour_n_a_qu_un_jour_ouvert_par_defaut() {
        Tournoi t = new Tournoi("Open", "Brunoy", SAMEDI);
        t.ajouterTableau(tableau("A", SAMEDI));
        assertEquals(List.of(SAMEDI), JoursPointage.jours(t));
        assertEquals(SAMEDI, JoursPointage.jourParDefaut(t, SAMEDI.atTime(8, 0)));
        assertEquals(SAMEDI, JoursPointage.jourParDefaut(t, SAMEDI.minusDays(3).atTime(8, 0)));
    }

    @Test
    void tournoi_de_deux_jours_ouvre_sur_aujourd_hui() {
        Tournoi t = deuxJours();
        assertEquals(List.of(SAMEDI, DIMANCHE), JoursPointage.jours(t));
        assertEquals(SAMEDI, JoursPointage.jourParDefaut(t, SAMEDI.atTime(9, 0)));
        assertEquals(DIMANCHE, JoursPointage.jourParDefaut(t, DIMANCHE.atTime(9, 0)));
    }

    @Test
    void hors_des_jours_du_tournoi_le_premier_jour_non_termine() {
        assertEquals(SAMEDI, JoursPointage.jourParDefaut(deuxJours(), SAMEDI.minusDays(5).atTime(9, 0)));
    }

    @Test
    void passage_de_minuit_le_tableau_du_samedi_reste_au_samedi() {
        Tournoi t = deuxJours();
        Tableau a = t.getTableaux().get(0);
        a.setHoraires(new HorairesTableau(SAMEDI.atTime(20, 0), SAMEDI.atTime(20, 30), SAMEDI.atTime(21, 0)));
        a.ouvrirPointage();
        assertEquals(SAMEDI, JoursPointage.jourDe(t, a));
        // Dimanche 0 h 30, le samedi n'est pas terminé : on reste sur le samedi.
        assertEquals(SAMEDI, JoursPointage.jourParDefaut(t, DIMANCHE.atTime(0, 30)));
        // Dimanche 7 h : la journée du dimanche commence.
        assertEquals(DIMANCHE, JoursPointage.jourParDefaut(t, DIMANCHE.atTime(7, 0)));
    }

    @Test
    void tableau_sans_horaire_est_rattache_au_premier_jour_et_signale() {
        Tournoi t = deuxJours();
        Tableau c = t.getTableaux().get(2);
        assertTrue(JoursPointage.jourNonDefini(c));
        assertEquals(SAMEDI, JoursPointage.jourDe(t, c));
        assertEquals(List.of("A", "C"), new ListePointage(t).nomsTableaux(SAMEDI));
        ListePointage.Engagement e = new ListePointage(t).lignes("durand", "C", null, SAMEDI).get(0).engagements().get(0);
        assertTrue(e.jourNonDefini());
        assertEquals(SAMEDI, e.jour());
    }

    @Test
    void joueur_inscrit_aux_deux_jours_n_apparait_qu_avec_les_tableaux_du_jour_choisi() {
        ListePointage lp = new ListePointage(deuxJours());
        List<ListePointage.Ligne> samedi = lp.lignes("martin", null, null, SAMEDI);
        assertEquals(1, samedi.size());
        assertEquals(List.of("A"), samedi.get(0).engagements().stream().map(ListePointage.Engagement::tableau).toList());
        List<ListePointage.Ligne> dimanche = lp.lignes("martin", null, null, DIMANCHE);
        assertEquals(List.of("B"), dimanche.get(0).engagements().stream().map(ListePointage.Engagement::tableau).toList());
        // « Tous les jours » : une fois par tableau, chacun avec son jour.
        List<ListePointage.Engagement> tous = lp.lignes("martin", null, null, null).get(0).engagements();
        assertEquals(List.of(SAMEDI, DIMANCHE), tous.stream().map(ListePointage.Engagement::jour).toList());
    }

    @Test
    void pointer_le_samedi_ne_pointe_pas_le_dimanche_et_les_totaux_sont_par_jour() {
        Tournoi t = deuxJours();
        ServicePostePointage s = new ServicePostePointage(t, () -> Instant.parse("2026-11-14T08:00:00Z"), "accueil");
        assertTrue(s.pointer("A", "100").estAcceptee());
        assertTrue(t.getTableaux().get(0).aPointe(alice));
        assertFalse(t.getTableaux().get(1).aPointe(alice));
        assertEquals(SAMEDI, t.getJournalPointage().get(0).getJour());
        ListePointage lp = new ListePointage(t);
        assertEquals(3, lp.totalJour(SAMEDI).inscrits()); // A : 2, C (sans horaire) : 1
        assertEquals(1, lp.totalJour(SAMEDI).pointes());
        assertEquals(2, lp.nonPointes(SAMEDI));
        assertEquals(1, lp.nonPointes(DIMANCHE));
        assertEquals(4, lp.totalGeneral().inscrits());
    }
}
