package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.RefusHomologation;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transitions de la demande d'homologation (B035) : refus avec motif → retour
 * au brouillon, acceptation → homologué.
 */
class RefusHomologationTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-05T10:15:00Z");

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi(() -> MAINTENANT);

    private static Tableau tableau(String nom, Sexe sexe) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().sexeRequis(sexe).build());
        t.setType("Toutes séries");
        t.setHoraires(new HorairesTableau(null, null, LocalDateTime.of(2026, 3, 8, 9, 0)));
        return t;
    }

    private Tournoi enDemande() {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.ajouterTableau(tableau("A", null));
        t.ajouterTableau(tableau("Dames", Sexe.FEMME));
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        return t;
    }

    @Test
    void refus_avec_motif_ramene_au_brouillon_et_garde_le_motif() {
        Tournoi t = enDemande();
        ResultatTransition r = cycle.refuserHomologation(t, "  Dates en conflit avec le Critérium fédéral ");
        assertTrue(r.estEffectuee());
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
        RefusHomologation refus = t.getDernierRefusHomologation().orElseThrow();
        assertEquals("Dates en conflit avec le Critérium fédéral", refus.motif());
        assertEquals(MAINTENANT, refus.date());
    }

    @Test
    void refus_deverrouille_le_dossier() {
        Tournoi t = enDemande();
        assertTrue(t.champsHomologationVerrouilles());
        cycle.refuserHomologation(t, "Adresse erronée");
        assertFalse(t.champsHomologationVerrouilles());
        assertTrue(new ServiceVerrousHomologation()
                .modifierAdresse(t, AdresseTournoi.de("1 place du Marché", "91800", "Brunoy")).estAppliquee());
    }

    @Test
    void refus_sans_motif_est_refuse_et_ne_change_rien() {
        Tournoi t = enDemande();
        ResultatTransition r = cycle.refuserHomologation(t, "   ");
        assertFalse(r.estEffectuee());
        assertEquals(ResultatTransition.Motif.MOTIF_REFUS_ABSENT, r.getMotif());
        assertEquals(1, r.getManquants().size());
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());
        assertTrue(t.getRefusHomologation().isEmpty());
        assertFalse(cycle.refuserHomologation(t, null).estEffectuee());
    }

    @Test
    void refus_impossible_hors_demande() {
        Tournoi brouillon = enDemande();
        cycle.retirerDemande(brouillon);
        assertThrows(IllegalStateException.class, () -> cycle.refuserHomologation(brouillon, "motif"));

        Tournoi homologue = enDemande();
        cycle.homologuer(homologue, "H-1");
        assertThrows(IllegalStateException.class, () -> cycle.refuserHomologation(homologue, "motif"));
    }

    @Test
    void refus_successifs_conserves_dans_l_ordre_le_dernier_visible() {
        Tournoi t = enDemande();
        cycle.refuserHomologation(t, "Premier motif");
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        cycle.refuserHomologation(t, "Second motif");
        assertEquals(2, t.getRefusHomologation().size());
        assertEquals("Premier motif", t.getRefusHomologation().get(0).motif());
        assertEquals("Second motif", t.getDernierRefusHomologation().orElseThrow().motif());
    }

    @Test
    void acceptation_depuis_la_demande_homologue_le_tournoi() {
        Tournoi t = enDemande();
        assertTrue(cycle.homologuer(t, " H-2026-001 ").estEffectuee());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        assertEquals("H-2026-001", t.getNumeroHomologation());
        assertTrue(t.champsHomologationVerrouilles(), "le dossier reste verrouillé après acceptation");
    }

    @Test
    void acceptation_sans_numero_reste_en_demande() {
        Tournoi t = enDemande();
        ResultatTransition r = cycle.homologuer(t, "");
        assertFalse(r.estEffectuee());
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());
    }

    @Test
    void refus_motif_vide_interdit_dans_le_modele() {
        assertThrows(IllegalArgumentException.class, () -> new RefusHomologation(MAINTENANT, " "));
    }

    @Test
    void sans_refus_aucun_dernier_motif() {
        assertTrue(enDemande().getDernierRefusHomologation().isEmpty());
    }
}
