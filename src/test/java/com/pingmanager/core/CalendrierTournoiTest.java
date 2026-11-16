package com.pingmanager.core;

import com.pingmanager.model.StatutTournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendrierTournoiTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 4, 10);
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    @Test
    void moment_a_partir_des_dates_veille_jour_j_lendemain() {
        assertEquals(CalendrierTournoi.Moment.A_VENIR, CalendrierTournoi.moment(SAMEDI, DIMANCHE, SAMEDI.minusDays(1)));
        assertEquals(CalendrierTournoi.Moment.EN_COURS, CalendrierTournoi.moment(SAMEDI, DIMANCHE, SAMEDI));
        assertEquals(CalendrierTournoi.Moment.EN_COURS, CalendrierTournoi.moment(SAMEDI, DIMANCHE, DIMANCHE));
        assertEquals(CalendrierTournoi.Moment.PASSE, CalendrierTournoi.moment(SAMEDI, DIMANCHE, DIMANCHE.plusDays(1)));
        assertEquals(CalendrierTournoi.Moment.A_VENIR, CalendrierTournoi.moment(null, null, SAMEDI));
    }

    @Test
    void en_cours_seulement_si_publie_et_date_dans_la_periode() {
        assertTrue(CalendrierTournoi.estEnCours(StatutTournoi.PUBLIE, SAMEDI, SAMEDI, SAMEDI));
        assertFalse(CalendrierTournoi.estEnCours(StatutTournoi.HOMOLOGUE, SAMEDI, SAMEDI, SAMEDI));
        assertFalse(CalendrierTournoi.estEnCours(StatutTournoi.PUBLIE, SAMEDI, SAMEDI, SAMEDI.minusDays(1)));
        assertFalse(CalendrierTournoi.estEnCours(StatutTournoi.PUBLIE, SAMEDI, SAMEDI, SAMEDI.plusDays(1)));
    }
}
