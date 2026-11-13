package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@link Tournoi#relocaliserReglementDocument} : migration de l'emplacement du PDF déposé. */
class RelocalisationReglementTest {

    @Test
    void relocaliser_remplace_l_emplacement_du_reglement_depose() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        t.setReglementDocument(new DocumentReference("Règlement", "/ancien/r.pdf"));

        t.relocaliserReglementDocument(new DocumentReference("Règlement", "documents/r-0123456789ab.pdf"));

        assertEquals("documents/r-0123456789ab.pdf", t.getReglementDocument().getEmplacement());
    }

    @Test
    void relocaliser_sans_reglement_depose_est_refuse() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        assertThrows(IllegalStateException.class,
                () -> t.relocaliserReglementDocument(new DocumentReference("R", "documents/r.pdf")));
        assertNull(t.getReglementDocument());
        t.setReglementDocument(new DocumentReference("R", "/r.pdf"));
        assertThrows(NullPointerException.class, () -> t.relocaliserReglementDocument(null));
    }
}
