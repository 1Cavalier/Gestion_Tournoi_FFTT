package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** B043 : statut affiché pour chaque tableau dans la page « Tableau » du tournoi en cours. */
class PresentationTournoiEnCoursTest {

    @Test
    void statut_suit_les_etapes_du_tableau_inscriptions_pointage_tirage() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2026, 11, 14));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(8).build());
        t.ajouterTableau(a);
        assertEquals("Inscriptions", PresentationTournoiEnCours.statut(a));
        assertEquals(StatutPastille.ATTENTION, PresentationTournoiEnCours.pastille(a));
        a.ouvrirPointage();
        assertEquals("Pointage", PresentationTournoiEnCours.statut(a));
        new ServicePointage(t).cloturerPointage(a);
        assertEquals("Tirage à faire", PresentationTournoiEnCours.statut(a));
        assertEquals(StatutPastille.INFO, PresentationTournoiEnCours.pastille(a));
    }
}
