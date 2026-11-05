package com.pingmanager.ui.ecran;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Mise en forme de l'agenda du compte (sans JavaFX). */
class PresentationAgendaTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2027, 1, 10);

    private static AgendaCompte agenda() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("passe").titre("Réunion").date(LocalDate.of(2027, 1, 5)).construire());
        a.enregistrer(EvenementAgenda.builder().id("p1").titre("Entraînement").date(LocalDate.of(2027, 1, 12))
                .heure(LocalTime.of(18, 30)).note("Salle 2\nApporter des balles").construire());
        a.enregistrer(EvenementAgenda.builder().id("t").titre("National B").date(LocalDate.of(2027, 4, 10))
                .dateFin(LocalDate.of(2027, 4, 11)).lieu("Brunoy").origine(OrigineEvenement.TOURNOI)
                .cleTournoi("k").construire());
        return a;
    }

    @Test
    void lignes_a_venir_dans_l_ordre_sans_les_dates_passees() {
        List<PresentationAgenda.LigneAgenda> l = PresentationAgenda.lignes(agenda(), AUJOURDHUI, false);
        assertEquals(List.of("p1", "t"), l.stream().map(PresentationAgenda.LigneAgenda::id).toList());

        PresentationAgenda.LigneAgenda perso = l.get(0);
        assertEquals("mardi 12 janvier 2027", perso.jour());
        assertEquals("18h30", perso.heure());
        assertEquals("Salle 2", perso.detail(), "première ligne de la note");
        assertEquals("Date personnelle", perso.etiquette());
        assertFalse(perso.automatique());

        PresentationAgenda.LigneAgenda tournoi = l.get(1);
        assertEquals("du samedi 10 au dimanche 11 avril 2027", tournoi.jour());
        assertEquals("Toute la journée", tournoi.heure());
        assertEquals("Brunoy", tournoi.detail());
        assertEquals("Tournoi", tournoi.etiquette());
        assertTrue(tournoi.automatique());
    }

    @Test
    void dates_passees_gardees_sur_demande_et_marquees() {
        List<PresentationAgenda.LigneAgenda> l = PresentationAgenda.lignes(agenda(), AUJOURDHUI, true);
        assertEquals(3, l.size());
        assertTrue(l.get(0).passee());
        assertFalse(l.get(1).passee());
        assertTrue(PresentationAgenda.lignes(null, AUJOURDHUI, true).isEmpty());
        assertFalse(PresentationAgenda.texteVide().isBlank());
    }

    @Test
    void periode_sur_deux_mois_garde_le_mois_du_premier_jour() {
        EvenementAgenda e = EvenementAgenda.builder().id("x").titre("Stage").date(LocalDate.of(2027, 4, 30))
                .dateFin(LocalDate.of(2027, 5, 1)).construire();
        assertEquals("du vendredi 30 avril au samedi 1er mai 2027", PresentationAgenda.jour(e));
        EvenementAgenda annee = EvenementAgenda.builder().id("y").titre("Stage").date(LocalDate.of(2026, 12, 31))
                .dateFin(LocalDate.of(2027, 1, 1)).construire();
        assertEquals("du jeudi 31 décembre 2026 au vendredi 1er janvier 2027", PresentationAgenda.jour(annee));
    }
}
