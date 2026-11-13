package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des entités du pré-tournoi (brique A, niveau modèle) : {@link Club},
 * {@link CompteOrganisateur} et {@link AccesClub}. On vérifie ici les invariants
 * de construction, les identités (equals/hashCode) et l'état de vérification —
 * pas d'orchestration (génération/envoi de code), qui viendra dans {@code core}.
 */
class CompteEtClubTest {

    // -------------------------------------------------------------------------
    // Club
    // -------------------------------------------------------------------------

    @Test
    void club_construit_avec_champs_valides() {
        Club club = new Club("08911132", "Brunoy CTT", "91", "Brunoy",
                "contact@brunoyctt.fr");
        assertEquals("08911132", club.getNumero());
        assertEquals("Brunoy CTT", club.getNom());
        assertEquals("91", club.getDepartement());
        assertEquals("Brunoy", club.getVille());
        assertEquals("contact@brunoyctt.fr", club.getEmailOfficiel());
    }

    @Test
    void club_refuse_numero_vide() {
        assertThrows(IllegalArgumentException.class,
                () -> new Club("  ", "Brunoy CTT", "91", "Brunoy", "contact@brunoyctt.fr"));
    }

    @Test
    void club_refuse_email_officiel_vide() {
        assertThrows(IllegalArgumentException.class,
                () -> new Club("08911132", "Brunoy CTT", "91", "Brunoy", null));
    }

    @Test
    void club_identite_fondee_sur_le_numero() {
        Club a = new Club("08911132", "Brunoy CTT", "91", "Brunoy", "a@x.fr");
        Club b = new Club("08911132", "Autre nom", "75", "Paris", "b@y.fr");
        Club c = new Club("07500001", "Brunoy CTT", "91", "Brunoy", "a@x.fr");
        assertEquals(a, b, "même numéro => même club");
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c, "numéro différent => club différent");
    }

    @Test
    void club_adresse_optionnelle_vide_devient_null() {
        Club club = new Club("08911132", "Brunoy CTT", "91", "Brunoy", "a@x.fr");
        club.setAdresse1("   ");
        assertNull(club.getAdresse1());
        club.setAdresse1("Salle municipale");
        assertEquals("Salle municipale", club.getAdresse1());
    }

    // -------------------------------------------------------------------------
    // CompteOrganisateur
    // -------------------------------------------------------------------------

    @Test
    void compte_construit_non_verifie_par_defaut() {
        CompteOrganisateur compte = new CompteOrganisateur(
                "Referent.Demo@Example.org", "Leroy", "Bernard", "HASH", "08911132");
        assertFalse(compte.isEmailVerifie());
        assertEquals("08911132", compte.getNumeroClub());
        assertEquals("HASH", compte.getEmpreinteMotDePasse());
    }

    @Test
    void compte_normalise_email_en_minuscules() {
        CompteOrganisateur compte = new CompteOrganisateur(
                "Referent.Demo@Example.org", "Leroy", "Bernard", "HASH", "08911132");
        assertEquals("referent.demo@example.org", compte.getEmail());
    }

    @Test
    void compte_refuse_empreinte_vide() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompteOrganisateur("s@x.fr", "Leroy", "Bernard", "  ", "08911132"));
    }

    @Test
    void compte_refuse_club_vide() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompteOrganisateur("s@x.fr", "Leroy", "Bernard", "HASH", null));
    }

    @Test
    void compte_marquer_verifie_est_idempotent() {
        CompteOrganisateur compte = new CompteOrganisateur(
                "s@x.fr", "Leroy", "Bernard", "HASH", "08911132");
        compte.marquerEmailVerifie();
        assertTrue(compte.isEmailVerifie());
        compte.marquerEmailVerifie();
        assertTrue(compte.isEmailVerifie());
    }

    @Test
    void compte_identite_fondee_sur_email_insensible_a_la_casse() {
        CompteOrganisateur a = new CompteOrganisateur(
                "Bernard@x.fr", "Leroy", "Bernard", "HASH", "08911132");
        CompteOrganisateur b = new CompteOrganisateur(
                "bernard@x.fr", "Autre", "Nom", "AUTRE", "07500001");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compte_changer_empreinte() {
        CompteOrganisateur compte = new CompteOrganisateur(
                "s@x.fr", "Leroy", "Bernard", "HASH", "08911132");
        compte.setEmpreinteMotDePasse("NOUVEAU_HASH");
        assertEquals("NOUVEAU_HASH", compte.getEmpreinteMotDePasse());
        assertThrows(IllegalArgumentException.class,
                () -> compte.setEmpreinteMotDePasse(""));
    }

    // -------------------------------------------------------------------------
    // AccesClub
    // -------------------------------------------------------------------------

    @Test
    void acces_construit_avec_couple_club_email() {
        AccesClub acces = new AccesClub("08911132", "Bernard@x.fr");
        assertEquals("08911132", acces.getNumeroClub());
        assertEquals("bernard@x.fr", acces.getEmail(), "email normalisé en minuscules");
    }

    @Test
    void acces_refuse_email_vide() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccesClub("08911132", "  "));
    }

    @Test
    void acces_identite_couple_club_email() {
        AccesClub a = new AccesClub("08911132", "bernard@x.fr");
        AccesClub b = new AccesClub("08911132", "Bernard@x.fr", "Leroy", "Bernard");
        AccesClub autreClub = new AccesClub("07500001", "bernard@x.fr");
        AccesClub autreEmail = new AccesClub("08911132", "autre@x.fr");
        assertEquals(a, b, "même club + même email => même accès");
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, autreClub);
        assertNotEquals(a, autreEmail);
    }

    @Test
    void acces_identite_infos_optionnelles() {
        AccesClub acces = new AccesClub("08911132", "bernard@x.fr", "  ", null);
        assertNull(acces.getNom());
        assertNull(acces.getPrenom());
    }
}
