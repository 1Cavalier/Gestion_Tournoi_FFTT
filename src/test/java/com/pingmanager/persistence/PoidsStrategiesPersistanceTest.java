package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Les réglages réels des stratégies (B025) survivent à la sauvegarde de la session. */
class PoidsStrategiesPersistanceTest {

    private PoidsOrdonnancement allerRetour(PoidsOrdonnancement poids) throws Exception {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        SessionSalle s = new SessionSalle(t, new RegistreDisciplinaire(), new Ordonnanceur(4, poids),
                new EstimateurDuree(Duration.ofMinutes(20)));
        Path f = Files.createTempFile("poids", ".json");
        DepotSession.sauvegarder(s, f);
        return DepotSession.charger(f).getOrdonnanceur().getPoids();
    }

    @Test
    void petit_a_petit_tour_par_tour_et_finales_en_fin_de_journee_conserves() throws Exception {
        PoidsOrdonnancement p = allerRetour(PoidsOrdonnancement.pour(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT));
        assertTrue(p.isTourParTour());
        assertTrue(p.isFinalesEnFinDeJournee());
    }

    @Test
    void tableaux_commences_tot_priorite_aux_tours_avances_conservee() throws Exception {
        PoidsOrdonnancement p = allerRetour(PoidsOrdonnancement.pour(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES));
        assertEquals(30.0, p.getPoidsProfondeur(), 1e-9);
        assertFalse(p.isFinalesEnFinDeJournee());
        assertFalse(p.isTourParTour());
    }

    @Test
    void ancienne_session_sans_ces_reglages_relue_avec_les_valeurs_par_defaut() throws Exception {
        PoidsOrdonnancement p = allerRetour(new PoidsOrdonnancement());
        assertFalse(p.isTourParTour());
        assertFalse(p.isFinalesEnFinDeJournee());
        assertEquals(0.0, p.getPoidsProfondeur(), 1e-9);
    }
}
