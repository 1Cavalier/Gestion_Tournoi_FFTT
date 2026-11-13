package com.pingmanager.ui;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.OperationProtegee;
import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 16 : les droits sont contrôlés par le contexte à chaque opération sensible. */
class DroitsContexteTest {

    @TempDir
    Path dossier;

    private ContexteApplication nouveau() {
        return new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
    }

    private ContexteApplication compteVerifieSansClub() {
        ContexteApplication ctx = nouveau();
        ctx.creerCompte("sansclub@example.org", "SANS", "Club", "0123456789");
        ctx.confirmerEmail(FaillesSecuriteContexteTest.codeRecu(ctx));
        return ctx;
    }

    private static DroitRefuseException.Motif refus(Runnable action) {
        return assertThrows(DroitRefuseException.class, action::run).getMotif();
    }

    @Test
    void creer_un_tournoi_sans_compte_sans_email_verifie_ou_pour_un_autre_club_refuse() {
        Tournoi paris = new Tournoi("Open de Paris", "TT Paris 15", LocalDate.of(2027, 5, 8));
        paris.setNumeroAffiliationClub(DonneesDemo.CLUB_PARIS_15);
        ContexteApplication anonyme = nouveau();
        assertEquals(DroitRefuseException.Motif.NON_CONNECTE, refus(() -> anonyme.creerBrouillon(paris)));

        ContexteApplication nonVerifie = nouveau();
        nonVerifie.creerCompte(DonneesDemo.EMAIL_REFERENT_BRUNOY, "USURPATEUR", "Ivan", "0123456789");
        Tournoi brunoy = TournoisDeTest.deBrunoy(new Tournoi("Open", "Brunoy", LocalDate.of(2027, 5, 8)));
        assertEquals(DroitRefuseException.Motif.EMAIL_NON_VERIFIE, refus(() -> nonVerifie.creerBrouillon(brunoy)));

        ContexteApplication orga = TournoisDeTest.connecteBrunoy(nouveau());
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB, refus(() -> orga.creerBrouillon(paris)));
        assertTrue(Files.exists(orga.creerBrouillon(brunoy)), "son propre club : permis");
    }

    @Test
    void modifier_ou_homologuer_le_tournoi_d_un_club_sans_droit_refuse_et_rien_n_est_ecrit() throws Exception {
        ContexteApplication orga = TournoisDeTest.connecteBrunoy(nouveau());
        Path f = orga.creerBrouillon(TournoisDeTest.deBrunoy(new Tournoi("Open", "Brunoy", LocalDate.of(2027, 5, 8))));
        String avant = Files.readString(f);

        ContexteApplication intrus = compteVerifieSansClub();
        Tournoi t = intrus.chargerTournoi(f);
        t.setNom("Piraté");
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB, refus(() -> intrus.enregistrerTournoi(t, f)));
        // Changer le numéro de club en mémoire ne contourne pas le contrôle du fichier sur disque.
        t.setNumeroAffiliationClub(DonneesDemo.CLUB_PARIS_15);
        refus(() -> intrus.enregistrerTournoi(t, f));
        assertEquals(avant, Files.readString(f));

        Tournoi h = intrus.chargerTournoi(f);
        h.changerStatut(StatutTournoi.ANNULE);
        DroitRefuseException e = assertThrows(DroitRefuseException.class, () -> intrus.enregistrerTournoi(h, f));
        assertEquals(OperationProtegee.HOMOLOGUER_TOURNOI, e.getOperation());

        Tournoi ok = orga.chargerTournoi(f);
        ok.setNom("Open modifié");
        orga.enregistrerTournoi(ok, f);
        assertEquals("Open modifié", orga.chargerTournoi(f).getNom());
    }

    @Test
    void ancien_tournoi_sans_numero_n_est_plus_rattache_par_le_nom_du_club() throws Exception {
        ContexteApplication orga = TournoisDeTest.connecteBrunoy(nouveau());
        Tournoi ancien = new Tournoi("Ancien open", "Brunoy", LocalDate.of(2027, 5, 8));
        Path f = Files.createDirectories(dossier.resolve("tournois")).resolve("ancien" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(ancien, new RegistreDisciplinaire(), f);
        assertFalse(orga.estTournoiDuCompte(ancien), "plus de repli par le nom « Brunoy »");
        assertEquals(DroitRefuseException.Motif.CLUB_NON_DESIGNE,
                refus(() -> orga.enregistrerTournoi(orga.chargerTournoi(f), f)));
    }

    @Test
    void donnees_et_equipe_d_un_autre_club_refusees() {
        ContexteApplication orga = TournoisDeTest.connecteBrunoy(nouveau());
        assertFalse(orga.membresDuClub(DonneesDemo.CLUB_BRUNOY).isEmpty());
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB,
                refus(() -> orga.membresDuClub(DonneesDemo.CLUB_PARIS_15)));
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB,
                refus(() -> orga.correspondantParDefaut(DonneesDemo.CLUB_PARIS_15)));
        ContexteApplication anonyme = nouveau();
        assertEquals(DroitRefuseException.Motif.NON_CONNECTE,
                refus(() -> anonyme.membresDuClub(DonneesDemo.CLUB_BRUNOY)));
    }

    @Test
    void supervision_refusee_hors_mode_demo_et_permise_en_mode_demo() {
        ContexteApplication orga = TournoisDeTest.connecteBrunoy(nouveau());
        assertEquals(DroitRefuseException.Motif.SUPERVISION_REQUISE,
                refus(() -> orga.exigerDroit(OperationProtegee.SUPERVISION, null)));

        ContexteApplication demo = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), true);
        demo.connexion(DonneesDemo.EMAIL_ULTRA_ADMIN, demo.motDePasseSupervisionDemo().orElseThrow());
        demo.exigerDroit(OperationProtegee.SUPERVISION, null);
        assertTrue(demo.aLeDroit(OperationProtegee.MODIFIER_TOURNOI, DonneesDemo.CLUB_PARIS_15));
        assertFalse(demo.aLeDroit(OperationProtegee.CREER_TOURNOI, DonneesDemo.CLUB_PARIS_15));
    }
}
