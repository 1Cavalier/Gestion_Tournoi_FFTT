package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des champs de la demande d'homologation ajoutés au niveau tournoi
 * (étape 1) : type de tournoi, dates début/fin, adresse structurée,
 * correspondant, site web, dotation, documents. On teste ici le <b>portage</b>
 * et les invariants de ces champs — la logique de complétude H/P viendra ensuite.
 */
class ChampsHomologationTest {

    // --- TypeTournoi ----------------------------------------------------------

    @Test
    void type_tournoi_porte() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        assertNull(t.getTypeTournoi());
        t.setTypeTournoi(TypeTournoi.PROMOTIONNEL);
        assertEquals(TypeTournoi.PROMOTIONNEL, t.getTypeTournoi());
    }

    // --- Dates début / fin ----------------------------------------------------

    @Test
    void periode_debut_fin() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setPeriode(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 8));
        assertEquals(LocalDate.of(2026, 3, 7), t.getDateDebut());
        assertEquals(LocalDate.of(2026, 3, 8), t.getDateFin());
    }

    @Test
    void periode_fin_avant_debut_refusee() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        assertThrows(IllegalArgumentException.class,
                () -> t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 7)));
    }

    // --- Adresse --------------------------------------------------------------

    @Test
    void adresse_complete_ok() {
        AdresseTournoi a = AdresseTournoi.de("5 rue de la Concorde", "54300", "Chanteheux");
        assertTrue(a.estComplete());
        assertEquals("5 rue de la Concorde", a.getVoie());
        assertEquals("54300", a.getCodePostal());
        assertEquals("Chanteheux", a.getVille());
    }

    @Test
    void adresse_obligatoires_manquants_refuses() {
        assertThrows(IllegalArgumentException.class,
                () -> AdresseTournoi.de("  ", "54300", "Chanteheux"));
    }

    @Test
    void adresse_partielle_toleree_mais_incomplete() {
        AdresseTournoi a = AdresseTournoi.partielle("5 rue X", null, null, null, null);
        assertFalse(a.estComplete());
        assertEquals("5 rue X", a.getVoie());
        assertNull(a.getVille());
    }

    @Test
    void adresse_champs_facultatifs() {
        AdresseTournoi a = AdresseTournoi.complete("5 rue X", "Bât. B", "Le Clos", "54300", "Ville");
        assertEquals("Bât. B", a.getResidenceBatiment());
        assertEquals("Le Clos", a.getLieuDit());
    }

    // --- Correspondant --------------------------------------------------------

    @Test
    void correspondant_porte_nom_prenom_email() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Lachambre", "Cédric", "cedric@x.fr", null, null);
        assertEquals("Lachambre", t.getCorrespondantNom());
        assertEquals("Cédric", t.getCorrespondantPrenom());
        assertEquals("cedric@x.fr", t.getCorrespondantEmail());
        assertNull(t.getCorrespondantTelephone());
    }

    // --- Site web / dotation --------------------------------------------------

    @Test
    void site_web_optionnel_vide_devient_null() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setSiteWeb("   ");
        assertNull(t.getSiteWeb());
        t.setSiteWeb("https://open.club.fr");
        assertEquals("https://open.club.fr", t.getSiteWeb());
    }

    @Test
    void dotation_globale_negative_refusee() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        assertThrows(IllegalArgumentException.class, () -> t.setDotationGlobale(-1));
        t.setDotationGlobale(1500.0);
        assertEquals(1500.0, t.getDotationGlobale(), 0.001);
    }

    // --- Documents ------------------------------------------------------------

    @Test
    void documents_reglement_et_feuille() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 3, 8));
        t.setReglementDocument(new DocumentReference("Règlement", "/docs/reglement.pdf"));
        assertEquals("/docs/reglement.pdf", t.getReglementDocument().getEmplacement());
        assertNull(t.getFeuilleEngagement());
    }

    @Test
    void document_emplacement_obligatoire() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentReference("Règlement", "  "));
    }

    // --- Juge-arbitre (nouvelle API saisie libre) -----------------------------

    @Test
    void ja_saisie_libre() {
        AffectationJugeArbitre ja = new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3, "0600000000");
        assertEquals("Durand", ja.getNom());
        assertEquals("1234567", ja.getNumeroLicence());
        assertEquals("0600000000", ja.getTelephone());
        assertTrue(ja.couvre(GradeJugeArbitre.JA1));
    }

    @Test
    void ja_champs_obligatoires() {
        assertThrows(IllegalArgumentException.class,
                () -> new AffectationJugeArbitre("Durand", "Paul", "paul@x.fr", "  ",
                        GradeJugeArbitre.JA3));
    }
}
