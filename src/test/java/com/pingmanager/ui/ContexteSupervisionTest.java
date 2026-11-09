package com.pingmanager.ui;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import com.pingmanager.ui.infra.TournoiDemoBrunoy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Compte ultra admin et nettoyage des tournois de Brunoy (fait une seule fois). */
class ContexteSupervisionTest {

    @TempDir
    Path dossier;

    @Test
    void ultra_admin_voit_tous_les_tournois_et_le_club_seulement_les_siens() throws Exception {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        Tournoi paris = new Tournoi("Open de Paris 15", "TT Paris 15", LocalDate.of(2027, 5, 8));
        paris.setNumeroAffiliationClub(DonneesDemo.CLUB_PARIS_15);
        ctx.creerBrouillon(paris);

        assertTrue(ctx.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, DonneesDemo.MDP_ULTRA_ADMIN).isPresent());
        assertTrue(ctx.estUltraAdmin());
        assertFalse(ctx.estOrganisateur(), "l'ultra admin n'est rattaché à aucun club");
        assertEquals(3, ctx.fichiersTournois().size(), "Brunoy + sa copie au jour J + Paris 15");

        ctx.deconnecter();
        ctx.connecter(DonneesDemo.EMAIL_QUENTIN, "Soumet123/");
        assertFalse(ctx.estUltraAdmin());
        assertTrue(ctx.estTournoiDuCompte(TournoiDemoBrunoy.construire()));
        assertFalse(ctx.estTournoiDuCompte(paris));
    }

    @Test
    void nettoyage_brunoy_archive_les_autres_tournois_et_recree_le_complet() throws Exception {
        // Avant le nettoyage : deux anciens tournois de Brunoy et un ancien brouillon de démo.
        Path tournois = Files.createDirectories(dossier.resolve("tournois"));
        Tournoi ancien = new Tournoi("Open d'automne", "Brunoy", LocalDate.of(2026, 11, 21));
        DepotTournoi.sauvegarder(ancien, new RegistreDisciplinaire(), tournois.resolve("open" + DepotTournoi.EXTENSION));
        Tournoi vieuxDemo = new Tournoi(TournoiDemoBrunoy.NOM, "Brunoy", LocalDate.of(2027, 4, 17));
        DepotTournoi.sauvegarder(vieuxDemo, new RegistreDisciplinaire(),
                tournois.resolve("Tournoi_National_B_Brunoy_2027-demo" + DepotTournoi.EXTENSION));

        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        // Hors copie au jour J du National B.
        Path copieJourJ = ctx.fichierTournoiJourJDemo().toAbsolutePath();
        List<Path> restants = ctx.fichiersTournois().stream().filter(p -> !p.equals(copieJourJ)).toList();
        assertEquals(1, restants.size(), restants.toString());
        assertEquals(13, ctx.chargerTournoi(restants.get(0)).getTableaux().size(), "version de la réunion");
        assertTrue(Files.exists(tournois.resolve("archives").resolve("open" + DepotTournoi.EXTENSION)));

        // Une seule fois : un tournoi créé ensuite n'est plus archivé au redémarrage.
        Tournoi nouveau = new Tournoi("Nouveau", "Brunoy", LocalDate.of(2027, 6, 5));
        ctx.creerBrouillon(nouveau);
        ContexteApplication ctx2 = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertEquals(3, ctx2.fichiersTournois().size(), "Brunoy, sa copie au jour J et le nouveau");
    }
}
