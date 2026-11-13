package com.pingmanager.ui;

import com.pingmanager.core.ResultatLiaisonLicence;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 14 : liaison de licence dans le contexte (preuve, unicité, délier). */
class LiaisonLicenceContexteTest {

    @TempDir
    Path dossier;

    private ContexteApplication compteVerifie(String email) {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        ctx.creerCompte(email, "TEST", "Tess", "0123456789");
        assertTrue(ctx.confirmerEmail(FaillesSecuriteContexteTest.codeRecu(ctx)).estVerifie());
        return ctx;
    }

    private static FicheLicence licencieDeBrunoy() {
        // Juge-arbitre fictif de Brunoy (fiche sans date de naissance : code du référent seul).
        return DonneesDemo.licences().trouverParLicence("9411021").orElseThrow();
    }

    @Test
    void compte_non_verifie_ne_peut_pas_lier_de_licence() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        ctx.creerCompte("nv@example.org", "NV", "Nina", "0123456789");
        FicheLicence f = licencieDeBrunoy();
        assertEquals(ResultatLiaisonLicence.Statut.EMAIL_NON_VERIFIE,
                ctx.demanderLiaisonLicence(f.getNumeroLicence(), f.getDateNaissance()).getStatut());
    }

    @Test
    void licence_du_profil_de_demo_refusee_a_un_second_compte() {
        ContexteApplication ctx = compteVerifie("second@example.org");
        ResultatLiaisonLicence r = ctx.demanderLiaisonLicence(DonneesDemo.LICENCE_PROFIL_DEMO,
                java.time.LocalDate.of(2002, 3, 15));
        assertEquals(ResultatLiaisonLicence.Statut.DEJA_LIEE_A_UN_AUTRE_COMPTE, r.getStatut());
        assertFalse(ctx.compteConnecte().aLicenceLiee());
    }

    @Test
    void liaison_par_code_du_referent_puis_deliaison_par_un_administrateur_du_club() {
        ContexteApplication ctx = compteVerifie("licencie@example.org");
        FicheLicence f = licencieDeBrunoy();
        ResultatLiaisonLicence d = ctx.demanderLiaisonLicence(f.getNumeroLicence(), f.getDateNaissance());
        assertEquals(ResultatLiaisonLicence.Statut.CODE_ENVOYE, d.getStatut());
        String club = DonneesDemo.clubs().trouverParNumero(DonneesDemo.CLUB_BRUNOY).orElseThrow().getEmailOfficiel();
        assertEquals(club, ctx.email().dernierMessage().orElseThrow().getDestinataire(), "code au référent du club");
        assertEquals(ResultatLiaisonLicence.Statut.LIEE,
                ctx.confirmerLiaisonLicence(FaillesSecuriteContexteTest.codeRecu(ctx)).getStatut());
        assertEquals(f.getNumeroLicence(), ctx.compteConnecte().getLicenceLiee());

        // Un compte sans droit sur le club ne peut pas la délier.
        ContexteApplication intrus = compteVerifie("intrus@example.org");
        assertEquals(ResultatLiaisonLicence.Statut.DROIT_INSUFFISANT,
                intrus.delierLicenceParAdministrateur(f.getNumeroLicence()).getStatut());

        // Le profil de démo, administrateur de Brunoy, la délie ; c'est enregistré.
        ContexteApplication admin = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        admin.connexion(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO);
        assertEquals(ResultatLiaisonLicence.Statut.DELIEE,
                admin.delierLicenceParAdministrateur(f.getNumeroLicence()).getStatut());
        ContexteApplication relu = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        relu.connexion("licencie@example.org", "0123456789");
        assertNull(relu.compteConnecte().getLicenceLiee());
    }

    @Test
    void le_titulaire_delie_sa_propre_licence() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        ctx.connexion(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO);
        assertEquals(ResultatLiaisonLicence.Statut.DELIEE, ctx.delierLicence().getStatut());
        assertFalse(ctx.compteConnecte().aLicenceLiee());
    }
}
