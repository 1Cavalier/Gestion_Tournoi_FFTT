package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeRetraits;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.MotifRetrait;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** B043 : textes de la vue « Retraits » et de la fenêtre des motifs. */
class PresentationRetraitsTest {

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "Brunoy", 800);

    private ListeRetraits.Retrait retrait(ListeRetraits.Nature nature, MotifRetrait motif, String precision,
                                          boolean paye) {
        return new ListeRetraits.Retrait("100", alice, "A", LocalDate.of(2026, 11, 14), nature, motif, precision,
                Instant.parse("2026-11-14T08:12:00Z"), "accueil-1", paye, false);
    }

    @Test
    void la_decision_du_joueur_est_proposee_en_premier() {
        assertEquals(MotifRetrait.DECISION_JOUEUR, PresentationRetraits.motifsProposes().get(0));
        assertEquals(MotifRetrait.values().length, PresentationRetraits.motifsProposes().size());
    }

    @Test
    void autre_motif_exige_une_precision_pour_confirmer() {
        assertTrue(PresentationRetraits.confirmable(MotifRetrait.DECISION_JOUEUR, ""));
        assertFalse(PresentationRetraits.confirmable(MotifRetrait.AUTRE, " "));
        assertTrue(PresentationRetraits.confirmable(MotifRetrait.AUTRE, "Convoqué"));
        assertFalse(PresentationRetraits.confirmable(null, "x"));
    }

    @Test
    void un_non_pointe_n_est_jamais_presente_comme_un_choix_du_joueur() {
        String m = PresentationRetraits.motif(retrait(ListeRetraits.Nature.NON_POINTE, null, null, false));
        assertTrue(m.startsWith("Non pointé"), m);
        assertFalse(m.toLowerCase().contains("décision"), m);
    }

    @Test
    void motif_precision_heure_et_poste_sont_visibles() {
        ListeRetraits.Retrait r = retrait(ListeRetraits.Nature.AUTRE_MOTIF, MotifRetrait.BLESSURE_MALADIE,
                "entorse", true);
        assertEquals("Blessure ou maladie — entorse", PresentationRetraits.motif(r));
        assertEquals("Tableau A · Samedi 14 · 9h12 · poste accueil-1",
                PresentationRetraits.detail(r, ZoneId.of("Europe/Paris")));
        assertEquals("MARTIN Alice", PresentationRetraits.nom(r));
        assertEquals("Retraits (3)", PresentationRetraits.ongletRetraits(3));
        assertEquals("Retraits", PresentationRetraits.ongletRetraits(0));
    }
}
