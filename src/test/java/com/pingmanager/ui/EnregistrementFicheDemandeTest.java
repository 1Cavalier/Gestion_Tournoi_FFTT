package com.pingmanager.ui;

import com.pingmanager.core.ResultatModificationTournoi;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B048 : garde-fou hors de l'écran. Pendant la demande d'homologation,
 * l'enregistrement d'une fiche est refusé par le contexte (rien n'est écrit) ;
 * seuls les changements d'étape (homologation acceptée, refusée) s'enregistrent.
 */
class EnregistrementFicheDemandeTest {

    @TempDir
    Path dossier;

    private ContexteApplication ctx;
    private Path fichier;
    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();

    @BeforeEach
    void preparer() {
        ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        fichier = ctx.creerBrouillon(TournoisDeTest.deBrunoy(
                new Tournoi("Open de Brunoy", "Brunoy", LocalDate.of(2027, 6, 5))));
    }

    private Tournoi passerEnDemande() {
        Tournoi t = ctx.chargerTournoi(fichier);
        t.changerStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        ctx.enregistrerTournoi(t, fichier);
        return ctx.chargerTournoi(fichier);
    }

    @Test
    void fiche_d_un_brouillon_enregistree() {
        Tournoi t = ctx.chargerTournoi(fichier);
        t.setSiteWeb("https://exemple.test");
        assertTrue(ctx.enregistrerSection(t, fichier).estAppliquee());
        assertEquals("https://exemple.test", ctx.chargerTournoi(fichier).getSiteWeb());
    }

    @Test
    void fiche_d_un_tournoi_en_demande_refusee_avec_motif_et_rien_n_est_ecrit() throws Exception {
        Tournoi t = passerEnDemande();
        byte[] avant = Files.readAllBytes(fichier);
        t.setSiteWeb("https://exemple.test");
        ResultatModificationTournoi r = ctx.enregistrerSection(t, fichier);
        assertFalse(r.estAppliquee());
        assertEquals(ResultatModificationTournoi.Motif.DOSSIER_FIGE, r.getMotif());
        assertEquals(ServiceVerrousHomologation.MOTIF_DOSSIER_FIGE, r.getMessage());
        assertArrayEquals(avant, Files.readAllBytes(fichier), "le fichier n'a pas été réécrit");
    }

    @Test
    void ecran_reste_ouvert_sur_le_brouillon_alors_que_la_demande_a_ete_faite_ailleurs() throws Exception {
        Tournoi resteOuvert = ctx.chargerTournoi(fichier); // encore au brouillon en mémoire
        passerEnDemande();
        byte[] avant = Files.readAllBytes(fichier);
        resteOuvert.setSiteWeb("https://exemple.test");
        assertFalse(ctx.enregistrerSection(resteOuvert, fichier).estAppliquee());
        assertArrayEquals(avant, Files.readAllBytes(fichier));
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, ctx.chargerTournoi(fichier).getStatut());
    }

    @Test
    void refus_de_l_homologation_enregistre_puis_fiches_de_nouveau_modifiables() {
        Tournoi t = passerEnDemande();
        assertTrue(cycle.refuserHomologation(t, "Dates en conflit").estEffectuee());
        ctx.enregistrerTournoi(t, fichier);
        Tournoi relu = ctx.chargerTournoi(fichier);
        assertEquals(StatutTournoi.BROUILLON, relu.getStatut());
        relu.setSiteWeb("https://exemple.test");
        assertTrue(ctx.enregistrerSection(relu, fichier).estAppliquee());
        assertEquals("https://exemple.test", ctx.chargerTournoi(fichier).getSiteWeb());
    }

    @Test
    void copie_de_demo_en_demande_fiches_refusees_mais_homologation_enregistree() {
        ContexteApplication demo = new ContexteApplication(dossier.resolve("demo"),
                DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(demo.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());
        Path f = demo.fichierTournoiDemandeDemo();
        Tournoi t = demo.chargerTournoi(f);
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());
        assertFalse(demo.enregistrerSection(t, f).estAppliquee());

        // La validation (numéro délivré) reste possible et s'enregistre.
        assertTrue(cycle.homologuer(t, "H-2027-001").estEffectuee());
        demo.enregistrerTournoi(t, f);
        Tournoi relu = demo.chargerTournoi(f);
        assertEquals(StatutTournoi.HOMOLOGUE, relu.getStatut());
        assertEquals("H-2027-001", relu.getNumeroHomologation());
        assertTrue(demo.enregistrerSection(relu, f).estAppliquee(), "après l'homologation, les fiches se rouvrent");
    }
}
