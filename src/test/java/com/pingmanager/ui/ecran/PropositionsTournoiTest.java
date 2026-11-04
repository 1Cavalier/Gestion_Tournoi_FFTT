package com.pingmanager.ui.ecran;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropositionsTournoiTest {

    @Test
    void nom_de_lieu() {
        assertEquals("Tournoi de Brunoy", PropositionsTournoi.nomTournoi("Brunoy"));
        assertEquals("Tournoi d'Évry", PropositionsTournoi.nomTournoi("Évry"));
        assertEquals("Tournoi d'Orsay", PropositionsTournoi.nomTournoi("Orsay"));
    }

    @Test
    void sigle_du_club() {
        assertEquals("Tournoi du TT Paris 15", PropositionsTournoi.nomTournoi("TT Paris 15"));
        assertEquals("Tournoi du CTT Melun", PropositionsTournoi.nomTournoi("CTT Melun"));
        assertEquals("Tournoi de l'AS Fontenay", PropositionsTournoi.nomTournoi("AS Fontenay"));
        assertEquals("Tournoi de l'US Créteil", PropositionsTournoi.nomTournoi("US Créteil"));
    }

    @Test
    void mot_de_tete_connu() {
        assertEquals("Tournoi du Club pongiste de Lyon", PropositionsTournoi.nomTournoi("Club pongiste de Lyon"));
        assertEquals("Tournoi de l'Entente Sud 91", PropositionsTournoi.nomTournoi("Entente Sud 91"));
    }

    @Test
    void club_vide() {
        assertEquals("Tournoi", PropositionsTournoi.nomTournoi("  "));
        assertEquals("Tournoi", PropositionsTournoi.nomTournoi(null));
    }
}
