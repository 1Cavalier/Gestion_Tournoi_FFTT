package com.pingmanager.ui;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import com.pingmanager.ui.infra.TournoiDemoBrunoyJourJ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Contexte de l'onglet Tournois : tournois de démo publiés, « Mes tournois » joueur et club. */
class OngletTournoisContexteTest {

    @TempDir
    Path dossier;

    @Test
    void demos_publiees_et_mes_tournois() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());
        List<ContexteApplication.TournoiFichier> tous = ctx.tousLesTournois();
        assertEquals(2, tous.size(), "le National B de Brunoy et sa copie au jour J");
        // Le vrai National B en brouillon, sa copie de démo publiée.
        assertEquals(StatutTournoi.BROUILLON, ctx.chargerTournoi(ctx.fichierTournoiDemo()).getStatut());
        assertEquals(StatutTournoi.PUBLIE, ctx.chargerTournoi(ctx.fichierTournoiJourJDemo()).getStatut());

        // Joueur : inscrit à la copie au jour J.
        List<ContexteApplication.TournoiFichier> joueur = ctx.mesTournoisJoueur(tous);
        assertEquals(1, joueur.size());
        assertEquals(TournoiDemoBrunoyJourJ.NOM, joueur.get(0).tournoi().getNom());
        // Club : le National B de Brunoy et sa copie au jour J.
        assertEquals(2, ctx.mesTournoisClub(tous).size());

        // La copie rechargée depuis le disque : remplie par les joueurs fictifs.
        var copie = ctx.chargerTournoi(ctx.fichierTournoiJourJDemo());
        assertTrue(RemplissageTableaux.nombreDeParticipants(copie) > 200);
        assertNotNull(copie.getAdresse().getCoordonnees());
        // Le vrai National B : aucun inscrit.
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(ctx.chargerTournoi(ctx.fichierTournoiDemo())));
        assertEquals(DonneesDemo.LICENCE_PROFIL_DEMO, ctx.ficheLicenceLiee().orElseThrow().getNumeroLicence());
        assertNotNull(ctx.positionDuJoueur());
    }
}
