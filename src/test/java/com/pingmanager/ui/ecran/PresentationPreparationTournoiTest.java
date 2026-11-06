package com.pingmanager.ui.ecran;

import com.pingmanager.core.Horloge;
import com.pingmanager.core.ResultatTransition;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ecran.PresentationPreparationTournoi.EtatEtape;
import com.pingmanager.ui.ecran.PresentationPreparationTournoi.Etape;
import com.pingmanager.ui.ecran.PresentationPreparationTournoi.Progression;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationPreparationTournoiTest {

    private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 3, 8, 9, 0);
    /** 5 novembre 2026, 13 h 30 UTC = 14 h 30 à Paris. */
    private static final Instant REFUS = Instant.parse("2026-11-05T13:30:00Z");

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi(horlogeFixe());

    private static Horloge horlogeFixe() {
        return () -> REFUS;
    }

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

    private static PresentationPreparationTournoi p(Tournoi t) {
        return new PresentationPreparationTournoi(t);
    }

    private static List<EtatEtape> etats(List<Etape> etapes) {
        return etapes.stream().map(Etape::etat).toList();
    }

    // --- Parcours ---------------------------------------------------------------

    @Test
    void parcours_quatre_etapes_brouillon_en_cours_sur_la_preparation() {
        List<Etape> etapes = p(brouillonComplet()).parcours();
        assertEquals(List.of("Préparer le dossier", "Demande d'homologation", "Homologation obtenue",
                "Publication"), etapes.stream().map(Etape::titre).toList());
        assertEquals(List.of(EtatEtape.EN_COURS, EtatEtape.A_VENIR, EtatEtape.A_VENIR, EtatEtape.A_VENIR),
                etats(etapes));
        assertEquals("parcours-etape-en-cours", etapes.get(0).etat().classeCss());
    }

    @Test
    void parcours_avance_avec_la_demande_l_homologation_puis_la_publication() {
        Tournoi t = enDemande();
        assertEquals(List.of(EtatEtape.FAITE, EtatEtape.EN_COURS, EtatEtape.A_VENIR, EtatEtape.A_VENIR),
                etats(p(t).parcours()));
        assertTrue(cycle.homologuer(t, "H-2026-001").estEffectuee());
        assertEquals(List.of(EtatEtape.FAITE, EtatEtape.FAITE, EtatEtape.EN_COURS, EtatEtape.A_VENIR),
                etats(p(t).parcours()));
    }

    // --- Progression ------------------------------------------------------------

    @Test
    void progression_homologation_complete_pour_un_dossier_complet() {
        Progression pr = p(brouillonComplet()).progressionHomologation();
        assertEquals(pr.total(), pr.renseignes());
        assertEquals(100, pr.pourcentage());
        assertEquals("Tout est renseigné pour la demande d'homologation.", pr.libelle());
    }

    @Test
    void progression_partielle_pour_un_tournoi_vide() {
        Tournoi t = new Tournoi("Essai", "Club", LocalDate.of(2026, 3, 8));
        Progression pr = p(t).progressionPublication();
        assertTrue(pr.renseignes() < pr.total());
        assertTrue(pr.pourcentage() < 100);
        assertTrue(pr.libelle().startsWith(pr.renseignes() + " / " + pr.total() + " éléments obligatoires"),
                pr.libelle());
    }

    @Test
    void progression_d_une_section_compte_les_elements_renseignes() {
        Progression pr = p(brouillonComplet()).progressionSection(PresentationPreparationTournoi.SECTION_GENERAL);
        assertTrue(pr.total() > 0);
        assertEquals(pr.renseignes() + " / " + pr.total() + " renseignés", pr.libelle());
    }

    // --- Refus d'homologation ------------------------------------------------------

    @Test
    void refus_rappele_sur_le_brouillon_avec_date_et_motif() {
        Tournoi t = enDemande();
        assertTrue(p(t).titreRefus().isEmpty());
        assertTrue(cycle.refuserHomologation(t, "  Juge-arbitre non validé par la CRA ").estEffectuee());
        PresentationPreparationTournoi pr = p(t);
        assertEquals("Homologation refusée le 05/11/2026 à 14h30", pr.titreRefus().orElseThrow());
        assertEquals("Juge-arbitre non validé par la CRA", pr.motifRefus().orElseThrow());
        assertFalse(pr.deuxiemeVersion());
    }

    @Test
    void refus_plus_affiche_apres_une_nouvelle_demande() {
        Tournoi t = enDemande();
        cycle.refuserHomologation(t, "Dates en conflit");
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        assertTrue(p(t).titreRefus().isEmpty());
        assertTrue(p(t).motifRefus().isEmpty());
    }

    @Test
    void refus_sans_motif_message_explicite() {
        Tournoi t = enDemande();
        ResultatTransition r = cycle.refuserHomologation(t, "  ");
        assertEquals("Saisissez le motif du refus : il est obligatoire et restera visible sur le brouillon.",
                PresentationPreparationTournoi.messageTransition(r));
        assertNull(PresentationPreparationTournoi.messageTransition(ResultatTransition.effectuee()));
    }

    @Test
    void acceptation_sans_numero_message_avec_l_element_manquant() {
        Tournoi t = enDemande();
        String m = PresentationPreparationTournoi.messageTransition(cycle.homologuer(t, ""));
        assertTrue(m.contains("numéro d'homologation"), m);
    }

    @Test
    void boutons_de_reponse_proposes_seulement_en_demande() {
        Tournoi t = brouillonComplet();
        assertFalse(p(t).reponseHomologationAttendue());
        cycle.demanderHomologation(t);
        assertTrue(p(t).reponseHomologationAttendue());
        cycle.homologuer(t, "H-1");
        assertFalse(p(t).reponseHomologationAttendue());
    }

    // --- Verrous ----------------------------------------------------------------

    @Test
    void brouillon_aucune_section_verrouillee() {
        PresentationPreparationTournoi pr = p(brouillonComplet());
        for (int i = 0; i < 4; i++) {
            assertFalse(pr.sectionVerrouillee(i));
            assertNull(pr.motifVerrouSection(i));
            assertNull(pr.mentionVerrouSection(i));
        }
    }

    @Test
    void en_demande_general_tableaux_et_sportif_verrouilles_complementaire_libre() {
        PresentationPreparationTournoi pr = p(enDemande());
        assertTrue(pr.sectionVerrouillee(PresentationPreparationTournoi.SECTION_GENERAL));
        assertTrue(pr.sectionVerrouillee(PresentationPreparationTournoi.SECTION_TABLEAUX));
        assertTrue(pr.sectionVerrouillee(PresentationPreparationTournoi.SECTION_SPORTIF));
        assertFalse(pr.sectionVerrouillee(PresentationPreparationTournoi.SECTION_COMPLEMENTAIRE));
        String motif = pr.motifVerrouSection(PresentationPreparationTournoi.SECTION_SPORTIF);
        assertTrue(motif.startsWith("L'homologation a été demandée"), motif);
        assertTrue(motif.contains(ChampHomologation.JUGES_ARBITRES.getLibelle()), motif);
        assertEquals("Dossier d'homologation verrouillé",
                pr.mentionVerrouSection(PresentationPreparationTournoi.SECTION_GENERAL));
    }

    @Test
    void chaque_champ_du_dossier_est_porte_par_une_section() {
        List<ChampHomologation> couverts = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            couverts.addAll(PresentationPreparationTournoi.champsSection(i));
        }
        assertEquals(List.of(ChampHomologation.values()).size(), couverts.size());
        assertTrue(couverts.containsAll(List.of(ChampHomologation.values())));
    }

    @Test
    void modification_refusee_message_du_verrou_appliquee_aucun_message() {
        Tournoi t = enDemande();
        ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();
        String m = PresentationPreparationTournoi.messageModification(verrous.modifierNom(t, "Autre nom"));
        assertNotNull(m);
        assertTrue(m.contains(ChampHomologation.NOM.getLibelle()), m);
        assertEquals("Open de Brunoy", t.getNom());
        assertNull(PresentationPreparationTournoi.messageModification(verrous.modifierNom(t, "Open de Brunoy")));
    }

    // --- Deuxième version ---------------------------------------------------------

    @Test
    void deuxieme_version_apres_acceptation_avec_trois_cartes_bientot() {
        Tournoi t = enDemande();
        assertFalse(p(t).deuxiemeVersion());
        assertTrue(p(t).cartesAVenir().isEmpty());
        assertEquals("Préparation du dossier", p(t).titreDossier());
        assertTrue(cycle.homologuer(t, "H-2026-001").estEffectuee());
        PresentationPreparationTournoi pr = p(t);
        assertTrue(pr.deuxiemeVersion());
        assertEquals(List.of("Salle", "Stratégie d'ordonnancement", "Remplissage et finances"),
                pr.cartesAVenir().stream().map(PresentationPreparationTournoi.CarteAVenir::titre).toList());
        assertEquals(List.of("B024", "B025", "B036"),
                pr.cartesAVenir().stream().map(PresentationPreparationTournoi.CarteAVenir::tache).toList());
        assertEquals("Dossier du tournoi et règlement", pr.titreDossier());
    }

    @Test
    void remplissage_et_finances_remplace_sa_carte_bientot_apres_acceptation() {
        Tournoi t = enDemande();
        assertFalse(p(t).afficheRemplissageEtFinances());
        assertTrue(p(t).cartesBientot().isEmpty());
        assertTrue(cycle.homologuer(t, "H-2026-001").estEffectuee());
        PresentationPreparationTournoi pr = p(t);
        assertTrue(pr.afficheRemplissageEtFinances());
        assertEquals(List.of("B024", "B025"),
                pr.cartesBientot().stream().map(PresentationPreparationTournoi.CarteAVenir::tache).toList());
    }
}
