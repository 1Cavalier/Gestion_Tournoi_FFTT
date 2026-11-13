package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListePointage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** B043 : textes du sélecteur de jour et de la ligne « Total du tournoi ». */
class PresentationPostePointageJoursTest {

    @Test
    void onglet_du_jour_affiche_le_nombre_de_non_pointes() {
        LocalDate samedi = LocalDate.of(2026, 11, 14);
        assertEquals("Samedi 14", PresentationPostePointage.jour(samedi));
        assertEquals("Samedi 14 · 3 non pointés", PresentationPostePointage.ongletJour(samedi, 3));
        assertEquals("Dimanche 15 · 1 non pointé", PresentationPostePointage.ongletJour(samedi.plusDays(1), 1));
        assertEquals("Samedi 14 · tout pointé", PresentationPostePointage.ongletJour(samedi, 0));
    }

    @Test
    void total_du_tournoi_reprend_les_cinq_tuiles() {
        String texte = PresentationPostePointage.totalTournoi(new ListePointage.Totaux("Tous", 12, 8, 6, 60, 40));
        assertTrue(texte.startsWith("Total du tournoi : 12 inscriptions · 8 / 12 pointées · 6 / 12 payées"), texte);
        assertTrue(texte.contains("en attente"));
    }
}
