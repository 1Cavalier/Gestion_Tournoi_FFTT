package com.pingmanager.core;

import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B048 (choix de l'application) : pendant la demande d'homologation, tout le
 * dossier est figé ; il redevient modifiable au refus, et après l'homologation
 * seuls les champs du dossier d'homologation restent verrouillés.
 */
class DossierFigeDemandeTest {

    private final ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();
    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();

    private static Tournoi auStatut(StatutTournoi statut) {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2027, 3, 8));
        if (statut != StatutTournoi.BROUILLON) {
            t.changerStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        }
        if (statut == StatutTournoi.HOMOLOGUE || statut == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.HOMOLOGUE);
        }
        if (statut == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.PUBLIE);
        }
        return t;
    }

    @Test
    void dossier_fige_seulement_pendant_la_demande() {
        assertFalse(verrous.dossierFige(auStatut(StatutTournoi.BROUILLON)));
        assertTrue(verrous.dossierFige(auStatut(StatutTournoi.DEMANDE_HOMOLOGATION)));
        assertFalse(verrous.dossierFige(auStatut(StatutTournoi.HOMOLOGUE)));
        assertFalse(verrous.dossierFige(auStatut(StatutTournoi.PUBLIE)));
    }

    @Test
    void enregistrement_d_une_fiche_refuse_pendant_la_demande_avec_un_motif_clair() {
        ResultatModificationTournoi r = verrous.verifierDossierModifiable(auStatut(StatutTournoi.DEMANDE_HOMOLOGATION));
        assertFalse(r.estAppliquee());
        assertEquals(ResultatModificationTournoi.Motif.DOSSIER_FIGE, r.getMotif());
        assertEquals(ServiceVerrousHomologation.MOTIF_DOSSIER_FIGE, r.getMessage());
        assertTrue(r.getMessage().startsWith("Demande d'homologation en cours : le dossier est figé"));
        assertTrue(r.getChamps().isEmpty(), "le refus porte sur tout le dossier, pas sur un champ");
    }

    @Test
    void enregistrement_d_une_fiche_accepte_hors_demande() {
        for (StatutTournoi s : new StatutTournoi[] {StatutTournoi.BROUILLON, StatutTournoi.HOMOLOGUE,
                StatutTournoi.PUBLIE}) {
            ResultatModificationTournoi r = verrous.verifierDossierModifiable(auStatut(s));
            assertTrue(r.estAppliquee(), s.name());
            assertNull(r.getMessage());
        }
    }

    @Test
    void le_refus_de_l_homologation_rend_le_dossier_modifiable() {
        Tournoi t = auStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        assertTrue(cycle.refuserHomologation(t, "Dates en conflit").estEffectuee());
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
        assertTrue(verrous.verifierDossierModifiable(t).estAppliquee());
    }

    @Test
    void le_retrait_de_la_demande_rend_le_dossier_modifiable() {
        Tournoi t = auStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        cycle.retirerDemande(t);
        assertFalse(verrous.dossierFige(t));
    }
}
