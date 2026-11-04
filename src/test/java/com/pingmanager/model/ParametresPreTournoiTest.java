package com.pingmanager.model;

import com.pingmanager.core.ResultatTransition;
import com.pingmanager.core.ServiceCycleTournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des paramètres pré-tournoi ajoutés : tournoi multi-jours, niveau du
 * tournoi, juges-arbitres (grade), et métadonnées de règlement optionnelles
 * (politique des balles, aire de jeu, tirage public, horaire des finales).
 */
class ParametresPreTournoiTest {

    private AffectationJugeArbitre ja(String licence, GradeJugeArbitre grade) {
        return new AffectationJugeArbitre("Arbitre", "Jean", "jean@x.fr", licence, grade);
    }

    // --- Multi-jours ----------------------------------------------------------

    @Test
    void tournoi_mono_jour_compat() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        assertEquals(1, t.nombreDeJours());
        assertEquals(LocalDate.of(2026, 3, 8), t.getDate());
    }

    @Test
    void tournoi_multi_jours() {
        Tournoi t = new Tournoi("Open", "Club",
                List.of(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 7)));
        assertEquals(2, t.nombreDeJours());
        // Jours triés : le premier est le 7.
        assertEquals(LocalDate.of(2026, 3, 7), t.getDate());
        assertEquals(LocalDate.of(2026, 3, 8), t.getJours().last());
    }

    @Test
    void ajouter_et_retirer_des_jours() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 7));
        t.ajouterJour(LocalDate.of(2026, 3, 8));
        assertEquals(2, t.nombreDeJours());
        t.retirerJour(LocalDate.of(2026, 3, 8));
        assertEquals(1, t.nombreDeJours());
    }

    @Test
    void retirer_le_dernier_jour_est_refuse() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 7));
        assertThrows(IllegalStateException.class, () -> t.retirerJour(LocalDate.of(2026, 3, 7)));
    }

    @Test
    void construction_sans_jour_refusee() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tournoi("Open", "Club", List.of()));
    }

    // --- Niveau + grade JA ----------------------------------------------------

    @Test
    void niveau_definit_grade_ja_requis() {
        // IX.104 : JA national en international et national A, au moins JA3 ailleurs.
        assertEquals(GradeJugeArbitre.JA3,
                NiveauTournoi.DEPARTEMENTAL.getGradeJugeArbitreRequis());
        assertEquals(GradeJugeArbitre.JA3,
                NiveauTournoi.NATIONAL_B.getGradeJugeArbitreRequis());
        assertEquals(GradeJugeArbitre.JA_NATIONAL,
                NiveauTournoi.NATIONAL_A.getGradeJugeArbitreRequis());
        assertEquals(GradeJugeArbitre.JA_NATIONAL,
                NiveauTournoi.INTERNATIONAL.getGradeJugeArbitreRequis());
        assertTrue(NiveauTournoi.PROMOTIONNEL.derogationJugeArbitrePossible());
        assertFalse(NiveauTournoi.REGIONAL.derogationJugeArbitrePossible());
    }

    @Test
    void grade_couvre_les_grades_inferieurs() {
        assertTrue(GradeJugeArbitre.JA_NATIONAL.couvre(GradeJugeArbitre.JA1));
        assertTrue(GradeJugeArbitre.JA3.couvre(GradeJugeArbitre.JA3));
        assertFalse(GradeJugeArbitre.JA1.couvre(GradeJugeArbitre.JA_NATIONAL));
    }

    @Test
    void affectation_ja_et_recherche_par_grade() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterJugeArbitre(ja("LIC1", GradeJugeArbitre.JA3));
        assertTrue(t.possedeJugeArbitreDeGrade(GradeJugeArbitre.JA1));
        assertTrue(t.possedeJugeArbitreDeGrade(GradeJugeArbitre.JA3));
        assertFalse(t.possedeJugeArbitreDeGrade(GradeJugeArbitre.JA_NATIONAL));
    }

    @Test
    void ja_en_double_refuse() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterJugeArbitre(ja("LIC1", GradeJugeArbitre.JA3));
        assertThrows(IllegalArgumentException.class,
                () -> t.ajouterJugeArbitre(ja("LIC1", GradeJugeArbitre.JA_NATIONAL)));
    }

    // --- Niveau ⇒ JA requis pour homologuer -----------------------------------

    private Tournoi tournoiHomologable() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setInfosReglement(InfosReglement.builder()
                .responsableNom("R").responsableEmail("r@x.fr")
                .salleNom("Gymnase").salleVille("Brunoy")
                .nombreDeTables(10).marqueBalles("ABS 40+")
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 1, 12, 0)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 1, 12, 0))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 8, 30))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build());
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA1));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue X", "91800", "Brunoy"));
        Tableau dames = new Tableau("Dames", ConfigurationTableau.builder().sexeRequis(Sexe.FEMME).build());
        dames.setType("Toutes séries");
        HorairesTableau h = new HorairesTableau();
        h.setDebutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0));
        dames.setHoraires(h);
        t.ajouterTableau(dames);
        return t;
    }

    @Test
    void sans_niveau_le_ja_n_est_pas_exige() {
        ServiceCycleTournoi service = new ServiceCycleTournoi();
        Tournoi t = tournoiHomologable(); // pas de niveau, pas de JA
        assertTrue(service.elementsManquantsPourHomologation(t, "NUM").isEmpty());
    }

    @Test
    void ix104_le_grade_du_ja_bloque_l_homologation_selon_la_categorie() {
        ServiceCycleTournoi service = new ServiceCycleTournoi();
        Tournoi t = tournoiHomologable();
        new java.util.ArrayList<>(t.getJugesArbitres()).forEach(a -> t.retirerJugeArbitre(a.getNumeroLicence()));
        t.setNiveau(NiveauTournoi.NATIONAL_A); // exige un JA national

        t.ajouterJugeArbitre(ja("LIC1", GradeJugeArbitre.JA3));
        assertTrue(service.elementsManquantsPourHomologation(t).stream().anyMatch(m -> m.contains("IX.104")),
                "un JA3 ne suffit pas en national A");

        t.ajouterJugeArbitre(ja("LIC2", GradeJugeArbitre.JA_NATIONAL));
        assertTrue(service.elementsManquantsPourHomologation(t).isEmpty());
    }

    @Test
    void ix104_en_promotionnel_un_grade_inferieur_passe_avec_avertissement() {
        ServiceCycleTournoi service = new ServiceCycleTournoi();
        Tournoi t = tournoiHomologable();
        new java.util.ArrayList<>(t.getJugesArbitres()).forEach(a -> t.retirerJugeArbitre(a.getNumeroLicence()));
        t.setNiveau(NiveauTournoi.PROMOTIONNEL);
        t.ajouterJugeArbitre(ja("LIC1", GradeJugeArbitre.JA2));

        assertTrue(service.elementsManquantsPourHomologation(t).isEmpty());
        assertTrue(service.avertissementsPourHomologation(t).stream()
                .anyMatch(m -> m.contains("commission d'arbitrage")));

        t.ajouterJugeArbitre(ja("LIC2", GradeJugeArbitre.JA3));
        assertTrue(service.avertissementsPourHomologation(t).isEmpty());
    }

    // --- Règlement étendu (optionnel) -----------------------------------------

    @Test
    void reglement_champs_optionnels_ne_bloquent_pas_l_homologation() {
        // Un règlement complet SANS les champs optionnels reste complet.
        InfosReglement infos = InfosReglement.builder()
                .responsableNom("R").responsableEmail("r@x.fr")
                .salleNom("Gymnase").salleVille("Brunoy")
                .nombreDeTables(10).marqueBalles("ABS 40+")
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 1, 12, 0)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 1, 12, 0))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 8, 30))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build();
        assertTrue(infos.estCompletPourHomologation());
        assertNull(infos.getPolitiqueBalles());
        assertNull(infos.getAireDeJeu());
    }

    @Test
    void reglement_porte_les_champs_optionnels() {
        InfosReglement infos = InfosReglement.builder()
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .aireDeJeu(new AireDeJeu(14.0, 7.0, 600))
                .horairePrevuFinales(LocalTime.of(16, 30))
                .tiragePublicDateHeure(LocalDateTime.of(2026, 3, 5, 19, 0))
                .tiragePublicLieu("Salle du club")
                .build();
        assertEquals(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR, infos.getPolitiqueBalles());
        assertEquals(14.0, infos.getAireDeJeu().getLongueurCourtM(), 0.001);
        assertEquals(600, infos.getAireDeJeu().getEclairageLux().intValue());
        assertEquals(LocalTime.of(16, 30), infos.getHorairePrevuFinales());
        assertEquals("Salle du club", infos.getTiragePublicLieu());
    }

    @Test
    void aire_de_jeu_valeurs_negatives_refusees() {
        assertThrows(IllegalArgumentException.class, () -> new AireDeJeu(-1.0, 7.0, 600));
        assertThrows(IllegalArgumentException.class, () -> new AireDeJeu(14.0, 7.0, 0));
    }

    @Test
    void tobuilder_preserve_les_champs_optionnels() {
        InfosReglement infos = InfosReglement.builder()
                .politiqueBalles(PolitiqueBalles.APPORTEES_PAR_JOUEURS)
                .aireDeJeu(new AireDeJeu(14.0, 7.0, 600))
                .build();
        InfosReglement copie = infos.toBuilder().nombreDeTables(8).build();
        assertEquals(PolitiqueBalles.APPORTEES_PAR_JOUEURS, copie.getPolitiqueBalles());
        assertNotNull(copie.getAireDeJeu());
        assertEquals(8, copie.getNombreDeTables());
    }
}
