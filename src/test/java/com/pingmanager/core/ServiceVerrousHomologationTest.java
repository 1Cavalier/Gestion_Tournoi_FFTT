package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verrous du dossier d'homologation (B035, cahier des charges v0 section 6) :
 * dès la demande, chaque champ de la grille 🔴 est refusé en modification ; les
 * autres champs restent modifiables.
 */
class ServiceVerrousHomologationTest {

    private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 3, 8, 9, 0);

    private final ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();
    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();

    // --- Montage ---------------------------------------------------------------

    private static Tableau tableau(String nom, Sexe sexe) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().sexeRequis(sexe).pointMax(1500)
                .placesMax(24).build());
        t.setType("≤ 1500 pts");
        t.setHoraires(new HorairesTableau(DEBUT.minusMinutes(60), DEBUT.minusMinutes(30), DEBUT));
        return t;
    }

    private static Tournoi brouillonComplet() {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setNumeroAffiliationClub("08910012");
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.ajouterTableau(tableau("A", null));
        t.ajouterTableau(tableau("Dames", Sexe.FEMME));
        return t;
    }

    private Tournoi enDemande() {
        Tournoi t = brouillonComplet();
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        return t;
    }

    private static void assertVerrouille(ResultatModificationTournoi r, ChampHomologation champ) {
        assertFalse(r.estAppliquee(), "la modification doit être refusée");
        assertEquals(ResultatModificationTournoi.Motif.CHAMP_VERROUILLE, r.getMotif());
        assertEquals(List.of(champ), r.getChamps());
        assertNotNull(r.getMessage());
        assertTrue(r.getMessage().contains(champ.getLibelle()), r.getMessage());
    }

    // --- Lecture des verrous -----------------------------------------------------

    @Test
    void brouillon_aucun_champ_verrouille() {
        Tournoi t = brouillonComplet();
        assertTrue(verrous.champsVerrouilles(t).isEmpty());
        assertFalse(verrous.estVerrouille(t, ChampHomologation.NOM));
        assertNull(verrous.motifVerrou(t, List.of(ChampHomologation.NOM)));
        assertTrue(verrous.verifier(t, ChampHomologation.DATES).estAppliquee());
    }

    @Test
    void demande_homologue_et_publie_verrouillent_tous_les_champs_du_dossier() {
        Tournoi t = enDemande();
        assertEquals(List.of(ChampHomologation.values()), verrous.champsVerrouilles(t));
        assertTrue(cycle.homologuer(t, "H-2026-001").estEffectuee());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        assertEquals(List.of(ChampHomologation.values()), verrous.champsVerrouilles(t));
        t.changerStatut(StatutTournoi.PUBLIE);
        assertEquals(List.of(ChampHomologation.values()), verrous.champsVerrouilles(t));
    }

    @Test
    void tournoi_annule_n_est_pas_verrouille_comme_le_reglement_de_b010() {
        Tournoi t = enDemande();
        cycle.annuler(t);
        assertTrue(verrous.champsVerrouilles(t).isEmpty());
    }

    @Test
    void motif_en_demande_invite_a_retirer_la_demande() {
        Tournoi t = enDemande();
        String motif = verrous.motifVerrou(t, List.of(ChampHomologation.NOM));
        assertTrue(motif.contains("L'homologation a été demandée"), motif);
        assertTrue(motif.contains("Retirez la demande"), motif);
    }

    @Test
    void motif_apres_homologation_le_dit() {
        Tournoi t = enDemande();
        cycle.homologuer(t, "H-1");
        String motif = verrous.verifier(t, ChampHomologation.ADRESSE).getMessage();
        assertTrue(motif.startsWith("Le tournoi est homologué"), motif);
    }

    // --- Section Général, champ par champ ------------------------------------------

    @Test
    void nom_modifiable_en_brouillon() {
        Tournoi t = brouillonComplet();
        assertTrue(verrous.modifierNom(t, "Open d'hiver").estAppliquee());
        assertEquals("Open d'hiver", t.getNom());
    }

    @Test
    void nom_refuse_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierNom(t, "Autre nom"), ChampHomologation.NOM);
        assertEquals("Open de Brunoy", t.getNom());
    }

    @Test
    void nom_inchange_accepte_des_la_demande() {
        Tournoi t = enDemande();
        assertTrue(verrous.modifierNom(t, "Open de Brunoy").estAppliquee());
    }

    @Test
    void categorie_refusee_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierCategorie(t, NiveauTournoi.REGIONAL), ChampHomologation.CATEGORIE);
        assertEquals(NiveauTournoi.PROMOTIONNEL, t.getNiveau());
        assertTrue(verrous.modifierCategorie(t, NiveauTournoi.PROMOTIONNEL).estAppliquee());
    }

    @Test
    void categorie_modifiable_en_brouillon() {
        Tournoi t = brouillonComplet();
        assertTrue(verrous.modifierCategorie(t, NiveauTournoi.REGIONAL).estAppliquee());
        assertEquals(NiveauTournoi.REGIONAL, t.getNiveau());
    }

    @Test
    void club_organisateur_refuse_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierClubOrganisateur(t, "Autre club"), ChampHomologation.CLUB_ORGANISATEUR);
        assertEquals("Brunoy CTT", t.getClubOrganisateur());
        assertTrue(verrous.modifierClubOrganisateur(t, " Brunoy CTT ").estAppliquee(),
                "espaces de bord : même valeur");
    }

    @Test
    void numero_affiliation_refuse_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierNumeroAffiliation(t, "08910099"), ChampHomologation.NUMERO_AFFILIATION);
        assertEquals("08910012", t.getNumeroAffiliationClub());
    }

    @Test
    void adresse_refusee_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierAdresse(t, AdresseTournoi.de("1 place du Marché", "91800", "Brunoy")),
                ChampHomologation.ADRESSE);
        assertVerrouille(verrous.modifierAdresse(t, null), ChampHomologation.ADRESSE);
        assertEquals("5 rue de la Concorde", t.getAdresse().getVoie());
    }

    @Test
    void adresse_geocodee_acceptee_des_la_demande_car_meme_lieu() {
        Tournoi t = enDemande();
        AdresseTournoi geocodee = t.getAdresse().avecCoordonnees(new Coordonnees(48.70, 2.50));
        assertTrue(verrous.modifierAdresse(t, geocodee).estAppliquee());
        assertNotNull(t.getAdresse().getCoordonnees());
    }

    @Test
    void dates_refusees_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierPeriode(t, LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 9)),
                ChampHomologation.DATES);
        assertEquals(LocalDate.of(2026, 3, 8), t.getDateFin());
    }

    @Test
    void dates_modifiables_en_brouillon() {
        Tournoi t = brouillonComplet();
        assertTrue(verrous.modifierPeriode(t, LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 9)).estAppliquee());
        assertEquals(LocalDate.of(2026, 3, 9), t.getDateFin());
    }

    @Test
    void correspondant_nom_prenom_email_refuses_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.modifierCorrespondant(t, "Martin", "Serge", "serge@club.fr", null, null),
                ChampHomologation.CORRESPONDANT);
        assertVerrouille(verrous.modifierCorrespondant(t, "Boulier", "Jean", "serge@club.fr", null, null),
                ChampHomologation.CORRESPONDANT);
        assertVerrouille(verrous.modifierCorrespondant(t, "Boulier", "Serge", "autre@club.fr", null, null),
                ChampHomologation.CORRESPONDANT);
        assertEquals("Boulier", t.getCorrespondantNom());
    }

    @Test
    void correspondant_telephone_reste_modifiable_des_la_demande() {
        Tournoi t = enDemande();
        assertTrue(verrous.modifierCorrespondant(t, "Boulier", "Serge", "serge@club.fr",
                "06 00 00 00 00", "le soir").estAppliquee());
        assertEquals("06 00 00 00 00", t.getCorrespondantTelephone());
    }

    // --- Juges-arbitres -------------------------------------------------------------

    @Test
    void ajout_et_retrait_de_juge_arbitre_refuses_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.ajouterJugeArbitre(t, new AffectationJugeArbitre(
                "Priou", "William", null, "7654321", GradeJugeArbitre.JA2, null, RoleJugeArbitre.ADJOINT)),
                ChampHomologation.JUGES_ARBITRES);
        assertVerrouille(verrous.retirerJugeArbitre(t, "1234567"), ChampHomologation.JUGES_ARBITRES);
        assertEquals(1, t.getJugesArbitres().size());
    }

    @Test
    void juge_arbitre_ajout_possible_en_brouillon() {
        Tournoi t = brouillonComplet();
        assertTrue(verrous.ajouterJugeArbitre(t, new AffectationJugeArbitre(
                "Priou", "William", null, "7654321", GradeJugeArbitre.JA2)).estAppliquee());
        assertEquals(2, t.getJugesArbitres().size());
    }

    @Test
    void juge_arbitre_grade_ou_identite_refuses_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.remplacerJugeArbitre(t, "1234567", new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA2)), ChampHomologation.JUGES_ARBITRES);
        assertVerrouille(verrous.remplacerJugeArbitre(t, "1234567", new AffectationJugeArbitre(
                "Dupont", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3)), ChampHomologation.JUGES_ARBITRES);
        assertVerrouille(verrous.remplacerJugeArbitre(t, "1234567", new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3, null, RoleJugeArbitre.ADJOINT)),
                ChampHomologation.JUGES_ARBITRES);
        assertEquals(GradeJugeArbitre.JA3, t.getJugesArbitres().get(0).getGrade());
    }

    @Test
    void juge_arbitre_licence_et_contact_restent_modifiables_des_la_demande() {
        Tournoi t = enDemande();
        assertTrue(verrous.remplacerJugeArbitre(t, "1234567", new AffectationJugeArbitre(
                "Durand", "Paul", "nouveau@x.fr", "9101112", GradeJugeArbitre.JA3, "06")).estAppliquee());
        assertEquals("9101112", t.getJugesArbitres().get(0).getNumeroLicence());
        assertEquals("nouveau@x.fr", t.getJugesArbitres().get(0).getEmail());
    }

    // --- Tableaux -----------------------------------------------------------------

    @Test
    void ajout_et_retrait_de_tableau_refuses_des_la_demande() {
        Tournoi t = enDemande();
        assertVerrouille(verrous.ajouterTableau(t, tableau("B", null)), ChampHomologation.LISTE_TABLEAUX);
        assertVerrouille(verrous.retirerTableau(t, t.getTableaux().get(0)), ChampHomologation.LISTE_TABLEAUX);
        assertEquals(2, t.getTableaux().size());
    }

    @Test
    void ajout_de_tableau_possible_en_brouillon() {
        Tournoi t = brouillonComplet();
        assertTrue(verrous.ajouterTableau(t, tableau("B", null)).estAppliquee());
        assertTrue(verrous.retirerTableau(t, t.getTableaux().get(2)).estAppliquee());
        assertEquals(2, t.getTableaux().size());
    }

    @Test
    void tableau_nom_refuse_des_la_demande() {
        Tournoi t = enDemande();
        Tableau ancien = t.getTableaux().get(0);
        assertVerrouille(verrous.remplacerTableau(t, ancien, tableau("Z", null)), ChampHomologation.TABLEAU_NOM);
        assertSame(ancien, t.getTableaux().get(0));
    }

    @Test
    void tableau_categorie_de_points_refusee_des_la_demande() {
        Tournoi t = enDemande();
        Tableau ancien = t.getTableaux().get(0);
        Tableau nouveau = new Tableau("A", ConfigurationTableau.builder().pointMax(1200).placesMax(24).build());
        nouveau.setHoraires(ancien.getHoraires());
        assertVerrouille(verrous.remplacerTableau(t, ancien, nouveau), ChampHomologation.TABLEAU_CATEGORIE_POINTS);
    }

    @Test
    void tableau_public_refuse_des_la_demande() {
        Tournoi t = enDemande();
        Tableau ancien = t.getTableaux().get(0);
        assertVerrouille(verrous.remplacerTableau(t, ancien, tableau("A", Sexe.HOMME)),
                ChampHomologation.TABLEAU_PUBLIC);
    }

    @Test
    void tableau_jour_et_heure_refuses_des_la_demande() {
        Tournoi t = enDemande();
        Tableau ancien = t.getTableaux().get(0);
        Tableau nouveau = tableau("A", null);
        nouveau.setHoraires(new HorairesTableau(null, null, DEBUT.plusHours(1)));
        assertVerrouille(verrous.remplacerTableau(t, ancien, nouveau), ChampHomologation.TABLEAU_DEBUT);
    }

    @Test
    void tableau_plusieurs_champs_changes_tous_listes() {
        Tournoi t = enDemande();
        Tableau ancien = t.getTableaux().get(0);
        Tableau nouveau = tableau("Z", Sexe.FEMME);
        ResultatModificationTournoi r = verrous.remplacerTableau(t, ancien, nouveau);
        assertEquals(List.of(ChampHomologation.TABLEAU_NOM, ChampHomologation.TABLEAU_PUBLIC), r.getChamps());
        assertTrue(r.getMessage().contains("ne peuvent plus être modifiés"), r.getMessage());
    }

    @Test
    void tableau_places_tarif_et_pointage_restent_modifiables_des_la_demande() {
        Tournoi t = enDemande();
        Tableau ancien = t.getTableaux().get(0);
        Tableau nouveau = new Tableau("A", ConfigurationTableau.builder().pointMax(1500).placesMax(48).build());
        nouveau.setHoraires(new HorairesTableau(DEBUT.minusMinutes(90), DEBUT.minusMinutes(15), DEBUT));
        assertTrue(verrous.remplacerTableau(t, ancien, nouveau).estAppliquee());
        assertEquals(48, t.getTableaux().get(0).getConfiguration().getPlacesMax());
    }

    @Test
    void tableau_modifiable_en_brouillon() {
        Tournoi t = brouillonComplet();
        Tableau ancien = t.getTableaux().get(0);
        assertTrue(verrous.remplacerTableau(t, ancien, tableau("Z", Sexe.HOMME)).estAppliquee());
        assertEquals("Z", t.getTableaux().get(0).getNom());
    }

    // --- Champs hors dossier : toujours modifiables ---------------------------------

    @Test
    void numero_homologation_hors_dossier_n_est_pas_verrouille() {
        Tournoi t = enDemande();
        for (ChampHomologation c : ChampHomologation.values()) {
            assertNotEquals("numéro d'homologation", c.getLibelle());
        }
        // Le numéro se saisit justement pendant la demande (section Général).
        t.setNumeroHomologation("H-2026-001");
        assertEquals("H-2026-001", t.getNumeroHomologation());
    }

    @Test
    void retrait_de_la_demande_deverrouille() {
        Tournoi t = enDemande();
        cycle.retirerDemande(t);
        assertTrue(verrous.modifierNom(t, "Autre nom").estAppliquee());
    }
}
