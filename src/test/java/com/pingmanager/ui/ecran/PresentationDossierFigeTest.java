package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ecran.PresentationDossierFige.Nature;
import com.pingmanager.ui.ecran.PresentationDossierFige.Traitement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B048 : fiches en consultation pendant la demande d'homologation. */
class PresentationDossierFigeTest {

    private static Tournoi brouillon() {
        return new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2027, 3, 8));
    }

    private static Tournoi enDemande() {
        Tournoi t = brouillon();
        t.changerStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        return t;
    }

    private static Tournoi homologue() {
        Tournoi t = enDemande();
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        return t;
    }

    @Test
    void fiches_en_consultation_seulement_pendant_la_demande() {
        assertFalse(PresentationDossierFige.estFige(brouillon()));
        assertTrue(PresentationDossierFige.estFige(enDemande()));
        assertFalse(PresentationDossierFige.estFige(homologue()));
    }

    @Test
    void bandeau_annonce_le_dossier_fige_et_ce_qui_reste_possible() {
        String b = PresentationDossierFige.bandeau();
        assertTrue(b.startsWith("Demande en cours : dossier figé"));
        assertTrue(b.contains("exports PDF"));
        assertTrue(b.contains("refus"));
    }

    @Test
    void champs_en_lecture_choix_desactives_actions_de_modification_retirees() {
        assertEquals(Traitement.LECTURE_SEULE, PresentationDossierFige.traitement(Nature.SAISIE_TEXTE, false));
        assertEquals(Traitement.DESACTIVER, PresentationDossierFige.traitement(Nature.CHOIX, false));
        assertEquals(Traitement.MASQUER, PresentationDossierFige.traitement(Nature.ACTION, false));
        assertEquals(Traitement.LAISSER, PresentationDossierFige.traitement(Nature.AUTRE, false));
    }

    @Test
    void retour_apercus_et_exports_restent_actifs() {
        assertEquals(Traitement.LAISSER, PresentationDossierFige.traitement(Nature.ACTION, true));
    }

    @Test
    void libelles_d_acces_aux_blocs_selon_le_statut() {
        int tableaux = PresentationPreparationTournoi.SECTION_TABLEAUX;
        int general = PresentationPreparationTournoi.SECTION_GENERAL;
        assertEquals("Gérer les tableaux", PresentationDossierFige.libelleOuvrirSection(brouillon(), tableaux));
        assertEquals("Ouvrir", PresentationDossierFige.libelleOuvrirSection(brouillon(), general));
        assertEquals("Voir les tableaux", PresentationDossierFige.libelleOuvrirSection(enDemande(), tableaux));
        assertEquals("Consulter", PresentationDossierFige.libelleOuvrirSection(enDemande(), general));
        assertEquals("Ouvrir", PresentationDossierFige.libelleOuvrirSection(homologue(), general));
    }

    @Test
    void formulaire_quitte_par_retour_en_consultation_par_annuler_sinon() {
        assertEquals("Annuler", PresentationDossierFige.libelleQuitter(brouillon()));
        assertEquals("Retour", PresentationDossierFige.libelleQuitter(enDemande()));
    }

    @Test
    void bascule_vers_l_import_non_proposee_pendant_la_demande() {
        assertTrue(PresentationDossierFige.basculeImportProposee(brouillon()));
        assertFalse(PresentationDossierFige.basculeImportProposee(enDemande()));
    }

    @Test
    void message_de_refus_repris_du_coeur() {
        ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();
        assertNull(PresentationDossierFige.messageRefus(verrous.verifierDossierModifiable(brouillon())));
        assertEquals(ServiceVerrousHomologation.MOTIF_DOSSIER_FIGE,
                PresentationDossierFige.messageRefus(verrous.verifierDossierModifiable(enDemande())));
    }
}
