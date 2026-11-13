package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Date repère du calendrier fédéral (B045) : invariants et statut. */
class DateFederaleTest {

    private static DateFederale.Builder base() {
        return DateFederale.builder().id("x").categorie("Cat").titre("Titre").date(LocalDate.of(2026, 10, 10))
                .statut(StatutDateFederale.CONFIRMEE).source("https://www.fftt.com").reluLe(LocalDate.of(2026, 11, 13));
    }

    @Test
    void champs_obligatoires_et_fin_avant_debut_refuses() {
        assertThrows(IllegalArgumentException.class, () -> base().titre(" ").construire());
        assertThrows(IllegalArgumentException.class, () -> base().source("").construire());
        assertThrows(IllegalArgumentException.class, () -> base().categorie(null).construire());
        assertThrows(NullPointerException.class, () -> base().statut(null).construire());
        assertThrows(NullPointerException.class, () -> base().reluLe(null).construire());
        assertThrows(IllegalArgumentException.class, () -> base().dateFin(LocalDate.of(2026, 10, 9)).construire());
    }

    @Test
    void fin_egale_au_debut_ramenee_a_une_date_d_un_jour_et_note_vide_ignoree() {
        DateFederale d = base().dateFin(LocalDate.of(2026, 10, 10)).note("  ").construire();
        assertFalse(d.estSurPlusieursJours());
        assertEquals(d.getDate(), d.getDateFin());
        assertNull(d.getNote());
        assertEquals(d, base().construire());
    }

    @Test
    void statut_lu_depuis_son_code_sans_tenir_compte_de_la_casse() {
        assertEquals(StatutDateFederale.A_VERIFIER, StatutDateFederale.depuisCode(" A-Verifier "));
        assertEquals(StatutDateFederale.CONFIRMEE, StatutDateFederale.depuisCode("confirmee"));
        assertNull(StatutDateFederale.depuisCode("confirmée ?"));
        assertNull(StatutDateFederale.depuisCode(null));
        assertEquals("À vérifier", StatutDateFederale.A_VERIFIER.libelle());
    }
}
