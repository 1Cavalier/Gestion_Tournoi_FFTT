package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Agenda du compte : ordre chronologique, remplacement par identifiant, date de tournoi. */
class AgendaCompteTest {

    private static EvenementAgenda date(String id, String titre, LocalDate jour, LocalTime heure) {
        return EvenementAgenda.builder().id(id).titre(titre).date(jour).heure(heure).construire();
    }

    @Test
    void dates_triees_par_jour_puis_journee_entiere_avant_les_heures() {
        AgendaCompte a = new AgendaCompte(" Quentin@Exemple.fr ");
        LocalDate j = LocalDate.of(2027, 1, 10);
        a.enregistrer(date("3", "Entraînement", j, LocalTime.of(18, 0)));
        a.enregistrer(date("2", "Stage", j, null));
        a.enregistrer(date("1", "Réunion", j.minusDays(1), LocalTime.of(20, 0)));
        assertEquals("quentin@exemple.fr", a.getEmail());
        assertEquals(java.util.List.of("1", "2", "3"),
                a.evenements().stream().map(EvenementAgenda::getId).toList());
    }

    @Test
    void enregistrer_le_meme_identifiant_remplace_la_date() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        a.enregistrer(date("x", "Avant", LocalDate.of(2027, 1, 1), null));
        a.enregistrer(date("x", "Après", LocalDate.of(2027, 1, 2), null));
        assertEquals(1, a.taille());
        assertEquals("Après", a.trouver("x").orElseThrow().getTitre());
        assertTrue(a.retirer("x"));
        assertFalse(a.retirer("x"));
        assertTrue(a.estVide());
    }

    @Test
    void date_de_tournoi_retrouvee_par_sa_cle_et_exige_une_cle() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("t").titre("Tournoi").date(LocalDate.of(2027, 4, 10))
                .dateFin(LocalDate.of(2027, 4, 11)).origine(OrigineEvenement.TOURNOI).cleTournoi("cle").construire());
        assertTrue(a.dateDuTournoi("cle").isPresent());
        assertTrue(a.dateDuTournoi("autre").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> EvenementAgenda.builder().id("u").titre("T")
                .date(LocalDate.of(2027, 1, 1)).origine(OrigineEvenement.TOURNOI).construire());
    }

    @Test
    void periode_d_un_jour_ramenee_a_une_date_simple_et_fin_avant_debut_refusee() {
        LocalDate j = LocalDate.of(2027, 1, 1);
        EvenementAgenda e = EvenementAgenda.builder().id("a").titre("T").date(j).dateFin(j).construire();
        assertFalse(e.estSurPlusieursJours());
        assertEquals(j, e.getDateFin());
        assertThrows(IllegalArgumentException.class,
                () -> EvenementAgenda.builder().id("a").titre("T").date(j).dateFin(j.minusDays(1)).construire());
    }
}
