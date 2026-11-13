package com.pingmanager.ui;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les listes de tournois en lecture seule ne relisent un fichier que s'il a changé. */
class CacheLecturesTournoisTest {

    @TempDir
    Path dossier;

    private static Tournoi trouver(List<ContexteApplication.TournoiFichier> tous, Path f) {
        return tous.stream().filter(tf -> tf.fichier().equals(f)).findFirst().orElseThrow().tournoi();
    }

    @Test
    void fichier_inchange_pas_relu_puis_relu_apres_modification() throws Exception {
        ContexteApplication ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Path f = ctx.creerBrouillon(TournoisDeTest.deBrunoy(new Tournoi("Open du cache", "Brunoy", LocalDate.of(2027, 6, 5)))).toAbsolutePath();

        Tournoi premier = trouver(ctx.tousLesTournois(), f);
        assertSame(premier, trouver(ctx.tousLesTournois(), f), "fichier inchangé : même lecture");

        Tournoi modifie = ctx.chargerTournoi(f);
        modifie.setNom("Open du cache renommé");
        DepotTournoi.sauvegarder(modifie, new RegistreDisciplinaire(), f);
        Files.setLastModifiedTime(f, FileTime.fromMillis(Files.getLastModifiedTime(f).toMillis() + 2000));

        Tournoi relu = trouver(ctx.tousLesTournois(), f);
        assertEquals("Open du cache renommé", relu.getNom());
    }

    @Test
    void fichier_supprime_disparait_de_la_liste() throws Exception {
        ContexteApplication ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Path f = ctx.creerBrouillon(TournoisDeTest.deBrunoy(new Tournoi("Open éphémère", "Brunoy", LocalDate.of(2027, 6, 5)))).toAbsolutePath();
        assertTrue(ctx.tousLesTournois().stream().anyMatch(tf -> tf.fichier().equals(f)));

        Files.delete(f);
        assertFalse(ctx.tousLesTournois().stream().anyMatch(tf -> tf.fichier().equals(f)));
    }
}
