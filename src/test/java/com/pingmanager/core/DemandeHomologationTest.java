package com.pingmanager.core;

import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.TournoiMapper;
import com.pingmanager.ui.infra.TournoiDemoBrunoy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Brouillon → demande d'homologation → publié (via homologué, avec le numéro saisi). */
class DemandeHomologationTest {

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();

    /** Le tournoi de Brunoy a ses juges-arbitres (S. Loobuyck JA3, W. Priou JA2). */
    private static Tournoi avecJugeArbitre() {
        return TournoiDemoBrunoy.construire();
    }

    @Test
    void parcours_complet_du_brouillon_a_la_publication() {
        Tournoi t = avecJugeArbitre();
        assertTrue(cycle.demanderHomologation(t).estEffectuee(), cycle.elementsManquantsPourHomologation(t).toString());
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());

        // Sans numéro : publication refusée, rien ne bouge.
        ResultatTransition r = cycle.publierDepuisDemande(t);
        assertFalse(r.estEffectuee());
        assertTrue(r.getManquants().stream().anyMatch(m -> m.contains("numéro d'homologation")), r.getManquants().toString());
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());

        t.setNumeroHomologation("2027-N-042");
        assertTrue(cycle.publierDepuisDemande(t).estEffectuee());
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
    }

    @Test
    void demande_refusee_si_dossier_incomplet_et_retrait_possible() {
        Tournoi vide = new Tournoi("Open", "Club", LocalDate.of(2027, 1, 9));
        assertFalse(cycle.demanderHomologation(vide).estEffectuee());
        assertEquals(StatutTournoi.BROUILLON, vide.getStatut());

        Tournoi t = avecJugeArbitre();
        cycle.demanderHomologation(t);
        cycle.retirerDemande(t);
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
        assertThrows(IllegalStateException.class, () -> cycle.retirerDemande(t));
    }

    @Test
    void statut_demande_relu_apres_enregistrement() {
        Tournoi t = avecJugeArbitre();
        cycle.demanderHomologation(t);
        Tournoi r = TournoiMapper.lire(TournoiMapper.ecrireDocument(t, new RegistreDisciplinaire())).getTournoi();
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, r.getStatut());
    }
}
