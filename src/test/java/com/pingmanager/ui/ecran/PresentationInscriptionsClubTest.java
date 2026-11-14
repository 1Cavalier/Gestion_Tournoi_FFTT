package com.pingmanager.ui.ecran;

import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B049 : inscriptions en consultation au statut homologué. */
class PresentationInscriptionsClubTest {

    private static Tournoi auStatut(StatutTournoi... etapes) {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2027, 3, 8));
        for (StatutTournoi s : etapes) {
            t.changerStatut(s);
        }
        return t;
    }

    private static Tournoi homologue() {
        return auStatut(StatutTournoi.DEMANDE_HOMOLOGATION, StatutTournoi.HOMOLOGUE);
    }

    private static Tournoi publie() {
        return auStatut(StatutTournoi.DEMANDE_HOMOLOGATION, StatutTournoi.HOMOLOGUE, StatutTournoi.PUBLIE);
    }

    @Test
    void consultation_seulement_au_statut_homologue() {
        assertFalse(PresentationInscriptionsClub.enConsultation(auStatut()));
        assertFalse(PresentationInscriptionsClub.enConsultation(auStatut(StatutTournoi.DEMANDE_HOMOLOGATION)));
        assertTrue(PresentationInscriptionsClub.enConsultation(homologue()));
        assertFalse(PresentationInscriptionsClub.enConsultation(publie()));
    }

    @Test
    void pas_d_import_de_joueurs_au_statut_homologue() {
        assertFalse(PresentationInscriptionsClub.importPropose(homologue()));
        assertEquals(PresentationInscriptionsClub.RAPPEL_CONSULTATION,
                PresentationInscriptionsClub.aideCarte(homologue()));
        assertEquals(PresentationInscriptionsClub.RAPPEL_CONSULTATION,
                PresentationInscriptionsClub.rappelEcran(homologue()).orElseThrow());
        assertFalse(PresentationInscriptionsClub.RAPPEL_CONSULTATION.contains("Importer"));
    }

    @Test
    void rien_ne_change_au_statut_publie() {
        assertTrue(PresentationInscriptionsClub.importPropose(publie()));
        assertEquals(PresentationInscriptionsClub.AIDE_IMPORT, PresentationInscriptionsClub.aideCarte(publie()));
        assertTrue(PresentationInscriptionsClub.rappelEcran(publie()).isEmpty());
        assertTrue(PresentationInscriptionsClub.AIDE_IMPORT.contains("Importer des joueurs"));
    }

    @Test
    void bascule_vers_l_import_sans_joueur_hors_demande_homologue_et_publie() {
        assertTrue(PresentationInscriptionsClub.basculeImportProposee(auStatut(), 0));
        assertFalse(PresentationInscriptionsClub.basculeImportProposee(auStatut(), 3));
        assertFalse(PresentationInscriptionsClub.basculeImportProposee(
                auStatut(StatutTournoi.DEMANDE_HOMOLOGATION), 0));
        assertFalse(PresentationInscriptionsClub.basculeImportProposee(homologue(), 0));
        assertFalse(PresentationInscriptionsClub.basculeImportProposee(publie(), 0));
    }
}
