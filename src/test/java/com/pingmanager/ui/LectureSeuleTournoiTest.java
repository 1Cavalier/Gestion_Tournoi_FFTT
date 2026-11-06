package com.pingmanager.ui;

import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@link ContexteApplication#lireTournoi(Path)} : lecture seule en cache, oubliée à chaque enregistrement. */
class LectureSeuleTournoiTest {

    @TempDir
    Path dossier;

    @Test
    void fichier_inchange_meme_lecture_puis_relu_apres_enregistrement_par_l_application() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        Path f = ctx.creerBrouillon(new Tournoi("Open lu", "Brunoy", LocalDate.of(2027, 6, 5))).toAbsolutePath();

        Tournoi premier = ctx.lireTournoi(f);
        assertSame(premier, ctx.lireTournoi(f), "fichier inchangé : même lecture");

        Tournoi modifie = ctx.chargerTournoi(f);
        modifie.setNom("Open lu et renommé");
        ctx.enregistrerTournoi(modifie, f);

        assertEquals("Open lu et renommé", ctx.lireTournoi(f).getNom(),
                "l'enregistrement retire le fichier du cache, même si sa date n'a pas bougé");
    }

    @Test
    void fichier_absent_leve_la_meme_erreur_qu_une_lecture_directe() throws Exception {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        Path f = ctx.creerBrouillon(new Tournoi("Open supprimé", "Brunoy", LocalDate.of(2027, 6, 5))).toAbsolutePath();
        ctx.lireTournoi(f);
        Files.delete(f);

        RuntimeException direct = assertThrows(RuntimeException.class, () -> ctx.chargerTournoi(f));
        RuntimeException enCache = assertThrows(RuntimeException.class, () -> ctx.lireTournoi(f));
        assertEquals(direct.getClass(), enCache.getClass());
        assertEquals(direct.getMessage(), enCache.getMessage());
    }
}
