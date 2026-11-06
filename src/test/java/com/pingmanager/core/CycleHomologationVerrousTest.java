package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ChampHomologation;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B035 : parcours complet du cycle, verrous exhaustifs et cas limites (vérificateur). */
class CycleHomologationVerrousTest {

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi(() -> Instant.parse("2026-11-06T08:00:00Z"));
    private final ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();

    private static Tableau tableau(String nom, Sexe sexe) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().sexeRequis(sexe).build());
        t.setType("Toutes séries");
        t.setHoraires(new HorairesTableau(null, null, LocalDateTime.of(2026, 3, 8, 9, 0)));
        return t;
    }

    private static Tournoi brouillon() {
        Tournoi t = new Tournoi("Open", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Paul", "p@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.ajouterTableau(tableau("A", null));
        t.ajouterTableau(tableau("Dames", Sexe.FEMME));
        return t;
    }

    @Test
    void parcours_complet_demande_refus_demande_homologation_publication() {
        Tournoi t = brouillon();
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        assertTrue(cycle.refuserHomologation(t, "Dates").estEffectuee());
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
        assertTrue(verrous.modifierNom(t, "Open 2").estAppliquee());
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        assertFalse(verrous.modifierNom(t, "Open 3").estAppliquee());
        assertTrue(cycle.homologuer(t, "2026/123").estEffectuee());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        assertEquals("Open 2", t.getNom());
        assertEquals(1, t.getRefusHomologation().size());
    }

    @Test
    void refus_impossible_depuis_homologue_publie_et_annule_et_ne_touche_pas_le_statut() {
        Tournoi t = brouillon();
        cycle.demanderHomologation(t);
        cycle.homologuer(t, "2026/123");
        assertThrows(IllegalStateException.class, () -> cycle.refuserHomologation(t, "x"));
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        cycle.publier(t);
        assertThrows(IllegalStateException.class, () -> cycle.refuserHomologation(t, "x"));
        Tournoi a = brouillon();
        cycle.annuler(a);
        assertThrows(IllegalStateException.class, () -> cycle.refuserHomologation(a, "x"));
        assertTrue(t.getRefusHomologation().isEmpty());
    }

    @Test
    void refus_depuis_brouillon_leve_et_annulation_apres_refus_possible() {
        Tournoi t = brouillon();
        assertThrows(IllegalStateException.class, () -> cycle.refuserHomologation(t, "x"));
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "x");
        cycle.annuler(t);
        assertEquals(StatutTournoi.ANNULE, t.getStatut());
    }

    @Test
    void refus_motif_null_ou_blanc_ne_change_rien() {
        Tournoi t = brouillon();
        cycle.demanderHomologation(t);
        assertFalse(cycle.refuserHomologation(t, null).estEffectuee());
        assertFalse(cycle.refuserHomologation(t, " \t\n ").estEffectuee());
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());
        assertTrue(t.getRefusHomologation().isEmpty());
    }

    @Test
    void refus_tournoi_null_leve_npe() {
        assertThrows(NullPointerException.class, () -> cycle.refuserHomologation(null, "x"));
    }

    @Test
    void refus_modele_refuse_null_et_blanc() {
        Instant d = Instant.now();
        assertThrows(NullPointerException.class, () -> new RefusHomologation(null, "x"));
        assertThrows(NullPointerException.class, () -> new RefusHomologation(d, null));
        assertThrows(IllegalArgumentException.class, () -> new RefusHomologation(d, "   "));
        assertThrows(NullPointerException.class, () -> brouillon().enregistrerRefusHomologation(null));
    }

    @Test
    void historique_des_refus_est_en_lecture_seule() {
        Tournoi t = brouillon();
        assertThrows(UnsupportedOperationException.class,
                () -> t.getRefusHomologation().add(new RefusHomologation(Instant.now(), "x")));
    }

    @Test
    void chaque_champ_est_verrouille_dans_les_trois_statuts_et_libre_en_brouillon() {
        Tournoi t = brouillon();
        assertTrue(verrous.champsVerrouilles(t).isEmpty());
        cycle.demanderHomologation(t);
        assertEquals(List.of(ChampHomologation.values()), verrous.champsVerrouilles(t));
        for (ChampHomologation c : ChampHomologation.values()) {
            assertTrue(verrous.estVerrouille(t, c), c.name());
            assertFalse(verrous.verifier(t, c).estAppliquee(), c.name());
        }
        cycle.homologuer(t, "2026/1");
        cycle.publier(t);
        for (ChampHomologation c : ChampHomologation.values()) {
            assertFalse(verrous.verifier(t, c).estAppliquee(), c.name());
        }
    }

    @Test
    void refus_ne_laisse_aucun_verrou() {
        Tournoi t = brouillon();
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "x");
        for (ChampHomologation c : ChampHomologation.values()) {
            assertTrue(verrous.verifier(t, c).estAppliquee(), c.name());
        }
    }

    @Test
    void valeurs_nulles_refusees_ou_acceptees_sans_erreur_inattendue() {
        Tournoi t = brouillon();
        assertThrows(NullPointerException.class, () -> verrous.champsVerrouilles(null));
        assertThrows(NullPointerException.class, () -> verrous.verifier(t, null));
        assertThrows(NullPointerException.class, () -> verrous.ajouterTableau(t, null));
        assertThrows(NullPointerException.class, () -> verrous.retirerTableau(t, null));
        assertNull(verrous.motifVerrou(t, List.of(ChampHomologation.NOM)), "brouillon : pas de motif");
        cycle.demanderHomologation(t);
        assertNull(verrous.motifVerrou(t, null));
        assertNull(verrous.motifVerrou(t, List.of()));
    }

    @Test
    void ajout_de_tableau_refuse_ne_modifie_pas_la_liste() {
        Tournoi t = brouillon();
        cycle.demanderHomologation(t);
        int avant = t.getTableaux().size();
        ResultatModificationTournoi r = verrous.ajouterTableau(t, tableau("B", null));
        assertFalse(r.estAppliquee());
        assertEquals(List.of(ChampHomologation.LISTE_TABLEAUX), r.getChamps());
        assertEquals(avant, t.getTableaux().size());
        assertFalse(verrous.retirerTableau(t, t.getTableaux().get(0)).estAppliquee());
        assertEquals(avant, t.getTableaux().size());
    }

    @Test
    void remplacer_tableau_identique_ou_champs_libres_accepte_meme_verrouille() {
        Tournoi t = brouillon();
        cycle.demanderHomologation(t);
        Tableau ancien = t.getTableaux().get(0);
        assertTrue(verrous.remplacerTableau(t, ancien, tableau("A", null)).estAppliquee());
    }
}
