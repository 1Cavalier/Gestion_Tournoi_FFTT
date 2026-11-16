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

    // --- B052 : inscriptions ouvertes à la publication seulement -------------------

    @Test
    void entrees_desactivees_avec_raison_dans_les_trois_statuts_avant_publication() {
        for (Tournoi t : java.util.List.of(auStatut(), auStatut(StatutTournoi.DEMANDE_HOMOLOGATION), homologue())) {
            assertTrue(PresentationInscriptionsClub.avantPublication(t), t.getStatut().name());
            assertFalse(PresentationInscriptionsClub.importPropose(t), t.getStatut().name());
            assertFalse(PresentationInscriptionsClub.voirInscriptionsActif(t, 0), t.getStatut().name());
            assertFalse(PresentationInscriptionsClub.jaugeAffichee(t), "aucun compteur : " + t.getStatut());
            assertEquals(PresentationInscriptionsClub.PHRASE_AVANT_PUBLICATION,
                    PresentationInscriptionsClub.phraseCarte(t, 0, 0, 96, 0));
        }
        assertEquals("Disponible après la publication", PresentationInscriptionsClub.RAISON_AVANT_PUBLICATION);
    }

    @Test
    void apres_publication_entrees_actives_et_compteur() {
        Tournoi t = publie();
        assertFalse(PresentationInscriptionsClub.avantPublication(t));
        assertTrue(PresentationInscriptionsClub.voirInscriptionsActif(t, 0));
        assertTrue(PresentationInscriptionsClub.jaugeAffichee(t));
        assertEquals("3 joueurs · 4 / 96 places occupées · 1 en liste d'attente",
                PresentationInscriptionsClub.phraseCarte(t, 3, 4, 96, 1));
    }

    @Test
    void ancien_fichier_homologue_avec_joueurs_consultable_et_signale() {
        Tournoi t = homologue();
        t.ajouterTableau(new com.pingmanager.model.Tableau("A",
                com.pingmanager.model.ConfigurationTableau.builder().placesMax(8).build()));
        t.getTableaux().get(0).inscrire(new com.pingmanager.model.Joueur("1", "Un", "Un",
                com.pingmanager.model.Sexe.HOMME, "C", 800));
        assertTrue(PresentationInscriptionsClub.voirInscriptionsActif(t, 1), "consultation de l'existant");
        assertTrue(PresentationInscriptionsClub.rappelEcran(t).orElseThrow().contains("1 joueur enregistré"));
        assertTrue(PresentationInscriptionsClub.phraseCarte(t, 1, 1, 8, 0).contains("conservé"));
    }
}
