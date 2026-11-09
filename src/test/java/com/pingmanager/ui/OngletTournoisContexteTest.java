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
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_QUENTIN, "Soumet123/").isPresent());
        List<ContexteApplication.TournoiFichier> tous = ctx.tousLesTournois();
        assertEquals(2, tous.size(), "le National B de Brunoy et sa copie au jour J");
        assertTrue(tous.stream().allMatch(tf -> tf.tournoi().getStatut() == StatutTournoi.PUBLIE));

        // Joueur : inscrit à la copie au jour J.
        List<ContexteApplication.TournoiFichier> joueur = ctx.mesTournoisJoueur(tous);
        assertEquals(1, joueur.size());
        assertEquals(TournoiDemoBrunoyJourJ.NOM, joueur.get(0).tournoi().getNom());
        // Club : le National B de Brunoy et sa copie au jour J.
        assertEquals(2, ctx.mesTournoisClub(tous).size());

        // Brunoy rechargé depuis le disque : rempli par les joueurs fictifs.
        var brunoy = ctx.chargerTournoi(ctx.fichierTournoiDemo());
        assertTrue(RemplissageTableaux.nombreDeParticipants(brunoy) > 200);
        assertNotNull(brunoy.getAdresse().getCoordonnees());
        assertEquals(DonneesDemo.LICENCE_QUENTIN, ctx.ficheLicenceLiee().orElseThrow().getNumeroLicence());
        assertNotNull(ctx.positionDuJoueur());
    }
}
