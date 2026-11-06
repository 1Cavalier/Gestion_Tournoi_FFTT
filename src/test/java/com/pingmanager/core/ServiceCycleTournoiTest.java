package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la brique B (cycle de vie du tournoi) : l'enum {@link StatutTournoi},
 * les métadonnées {@link InfosReglement} et le {@link ServiceCycleTournoi}.
 */
class ServiceCycleTournoiTest {

    private final ServiceCycleTournoi service = new ServiceCycleTournoi();

    // --- Helpers de montage ---------------------------------------------------

    private Tableau tableauMixte(String nom) {
        return tableauAvecTypeEtDate(new Tableau(nom, ConfigurationTableau.builder().build()));
    }

    private Tableau tableauDames(String nom) {
        return tableauAvecTypeEtDate(
                new Tableau(nom, ConfigurationTableau.builder().sexeRequis(Sexe.FEMME).build()));
    }

    /** Complète un tableau avec le type (SPID) et la date (début des matchs) requis en H. */
    private Tableau tableauAvecTypeEtDate(Tableau t) {
        t.setType("Toutes séries");
        HorairesTableau h = new HorairesTableau();
        h.setDebutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0));
        t.setHoraires(h);
        return t;
    }

    private Tableau tableauMixteP(String nom) {
        return tableauAvecTypeEtDate(new Tableau(nom, ConfigurationTableau.builder()
                .format(com.pingmanager.model.format.RegistreFormats.formatParDefaut())
                .placesMax(24).build()));
    }

    private Tableau tableauDamesP(String nom) {
        return tableauAvecTypeEtDate(new Tableau(nom, ConfigurationTableau.builder()
                .sexeRequis(Sexe.FEMME)
                .format(com.pingmanager.model.format.RegistreFormats.formatParDefaut())
                .placesMax(24).build()));
    }

    /** Tournoi satisfaisant en plus la grille P (prêt à publier une fois homologué). */
    private Tournoi tournoiPubliable() {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setInfosReglement(reglementComplet().toBuilder()
                .politiqueBalles(com.pingmanager.model.PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .build());
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(12);
        t.setReglementDocument(new com.pingmanager.model.DocumentReference("Règlement", "/r.pdf"));
        t.ajouterTableau(tableauMixteP("Tableau A"));
        t.ajouterTableau(tableauDamesP("Dames"));
        return t;
    }

    /** Règlement complet et cohérent pour l'homologation. */
    private InfosReglement reglementComplet() {
        return InfosReglement.builder()
                .responsableNom("Boulier Serge")
                .responsableEmail("serge@club.fr")
                .salleNom("Gymnase municipal")
                .salleVille("Brunoy")
                .nombreDeTables(12)
                .marqueBalles("Cornilleau ABS 40+")
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 1, 23, 59)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 1, 23, 59))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 8, 30))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build();
    }

    private Tournoi tournoiPretAHomologuer() {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setInfosReglement(reglementComplet());
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.ajouterTableau(tableauMixte("Tableau A"));
        t.ajouterTableau(tableauDames("Dames"));
        return t;
    }

    // --- StatutTournoi : transitions ------------------------------------------

    @Test
    void statut_transitions_autorisees() {
        assertTrue(StatutTournoi.BROUILLON.peutAllerVers(StatutTournoi.HOMOLOGUE));
        assertTrue(StatutTournoi.BROUILLON.peutAllerVers(StatutTournoi.ANNULE));
        assertTrue(StatutTournoi.HOMOLOGUE.peutAllerVers(StatutTournoi.PUBLIE));
        assertTrue(StatutTournoi.HOMOLOGUE.peutAllerVers(StatutTournoi.ANNULE));
        assertTrue(StatutTournoi.PUBLIE.peutAllerVers(StatutTournoi.ANNULE));
    }

    @Test
    void statut_transitions_interdites() {
        assertFalse(StatutTournoi.BROUILLON.peutAllerVers(StatutTournoi.PUBLIE), "pas de saut d'étape");
        assertFalse(StatutTournoi.HOMOLOGUE.peutAllerVers(StatutTournoi.BROUILLON), "pas de retour arrière");
        assertFalse(StatutTournoi.PUBLIE.peutAllerVers(StatutTournoi.HOMOLOGUE));
        assertFalse(StatutTournoi.ANNULE.peutAllerVers(StatutTournoi.BROUILLON), "état terminal");
        assertFalse(StatutTournoi.BROUILLON.peutAllerVers(StatutTournoi.BROUILLON), "pas vers soi-même");
        assertTrue(StatutTournoi.ANNULE.estTerminal());
    }

    @Test
    void tournoi_naissance_en_brouillon() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
    }

    @Test
    void tournoi_changer_statut_illegal_leve_exception() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        assertThrows(IllegalStateException.class, () -> t.changerStatut(StatutTournoi.PUBLIE));
    }

    // --- InfosReglement : chronologie et complétude ---------------------------

    @Test
    void reglement_chronologie_incoherente_refusee() {
        // pointage avant clôture des inscriptions => invalide
        assertThrows(IllegalArgumentException.class, () -> InfosReglement.builder()
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 8, 10, 0)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 8, 10, 0))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 9, 0))
                .build());
    }

    @Test
    void reglement_matchs_avant_pointage_refuses() {
        assertThrows(IllegalArgumentException.class, () -> InfosReglement.builder()
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 10, 0))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .build());
    }

    @Test
    void reglement_partiel_est_constructible_mais_incomplet() {
        InfosReglement infos = InfosReglement.builder()
                .responsableNom("X")
                .build();
        assertFalse(infos.estCompletPourHomologation());
        assertTrue(infos.champsManquantsPourHomologation().contains("email du responsable"));
        assertTrue(infos.champsManquantsPourHomologation().contains("nombre de tables"));
    }

    @Test
    void reglement_complet_ne_manque_de_rien() {
        assertTrue(reglementComplet().estCompletPourHomologation());
        assertTrue(reglementComplet().champsManquantsPourHomologation().isEmpty());
    }

    @Test
    void reglement_tobuilder_preserve_les_champs() {
        InfosReglement copie = reglementComplet().toBuilder().nombreDeTables(20).build();
        assertEquals(20, copie.getNombreDeTables());
        assertEquals("Gymnase municipal", copie.getSalleNom());
        assertTrue(copie.estCompletPourHomologation());
    }

    // --- Homologation ---------------------------------------------------------

    @Test
    void homologation_reussie_fige_le_numero_et_change_le_statut() {
        Tournoi t = tournoiPretAHomologuer();
        ResultatTransition res = service.homologuer(t, "2026-91-042");

        assertTrue(res.estEffectuee());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        assertEquals("2026-91-042", t.getNumeroHomologation());
    }

    @Test
    void homologation_refusee_si_correspondant_absent() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.ajouterTableau(tableauDames("Dames"));
        ResultatTransition res = service.homologuer(t, "NUM");

        assertEquals(ResultatTransition.Motif.DOSSIER_INCOMPLET, res.getMotif());
        assertTrue(res.getManquants().stream().anyMatch(m -> m.contains("correspondant")));
        assertEquals(StatutTournoi.BROUILLON, t.getStatut(), "le statut ne change pas");
    }

    @Test
    void homologation_refusee_si_aucun_tableau() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setInfosReglement(reglementComplet());
        ResultatTransition res = service.homologuer(t, "NUM");
        assertTrue(res.getManquants().contains("au moins un tableau"));
    }

    @Test
    void homologation_refusee_si_pas_de_tableau_feminin() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setInfosReglement(reglementComplet());
        t.ajouterTableau(tableauMixte("A"));
        ResultatTransition res = service.homologuer(t, "NUM");
        assertTrue(res.getManquants().stream().anyMatch(m -> m.contains("dames")));
    }

    @Test
    void homologation_refusee_si_numero_absent() {
        Tournoi t = tournoiPretAHomologuer();
        ResultatTransition res = service.homologuer(t, "  ");
        assertTrue(res.getManquants().stream().anyMatch(m -> m.contains("numéro d'homologation")));
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
    }

    @Test
    void homologation_cumule_tous_les_manques() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8)); // rien de rempli
        ResultatTransition res = service.homologuer(t, null);
        // règlement absent + au moins un tableau + numéro
        assertTrue(res.getManquants().size() >= 3);
    }

    @Test
    void homologation_illegale_hors_brouillon() {
        Tournoi t = tournoiPretAHomologuer();
        service.homologuer(t, "NUM");
        assertThrows(IllegalStateException.class, () -> service.homologuer(t, "NUM2"),
                "on ne peut pas homologuer deux fois");
    }

    @Test
    void elements_manquants_exposes_sans_transition() {
        Tournoi t = tournoiPretAHomologuer();
        // Le dossier de demande est complet (le numéro n'en fait pas partie).
        assertTrue(service.elementsManquantsPourHomologation(t).isEmpty());
        // Consultation seule : le statut n'a pas bougé.
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
    }

    // --- Publication ----------------------------------------------------------

    @Test
    void publication_reussie_depuis_homologue() {
        Tournoi t = tournoiPubliable();
        service.homologuer(t, "NUM");
        ResultatTransition res = service.publier(t);
        assertTrue(res.estEffectuee());
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
    }

    @Test
    void publication_illegale_depuis_brouillon() {
        Tournoi t = tournoiPretAHomologuer();
        assertThrows(IllegalStateException.class, () -> service.publier(t));
    }

    // --- Annulation -----------------------------------------------------------

    @Test
    void annulation_possible_depuis_brouillon() {
        Tournoi t = tournoiPretAHomologuer();
        service.annuler(t);
        assertEquals(StatutTournoi.ANNULE, t.getStatut());
    }

    @Test
    void annulation_possible_depuis_publie() {
        Tournoi t = tournoiPubliable();
        service.homologuer(t, "NUM");
        service.publier(t);
        service.annuler(t);
        assertEquals(StatutTournoi.ANNULE, t.getStatut());
    }

    @Test
    void annulation_impossible_deux_fois() {
        Tournoi t = tournoiPretAHomologuer();
        service.annuler(t);
        assertThrows(IllegalStateException.class, () -> service.annuler(t));
    }

    // --- Parcours complet -----------------------------------------------------

    @Test
    void parcours_complet_brouillon_homologue_publie() {
        Tournoi t = tournoiPubliable();
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
        assertTrue(service.homologuer(t, "2026-91-042").estEffectuee());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        assertTrue(service.publier(t).estEffectuee());
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
    }
}
