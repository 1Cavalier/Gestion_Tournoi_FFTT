package com.pingmanager.ui;

import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.persistence.DocumentsDeposes;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reliquat du point 18 : à la première lecture, l'image d'un ancien fichier est copiée et le fichier réenregistré. */
class MigrationImagePromotionnelleTest {

    @TempDir
    Path dossier;

    @Test
    void ancien_fichier_avec_image_au_chemin_absolu_migre_a_la_lecture() throws Exception {
        Path image = dossier.resolve("ailleurs").resolve("affiche.png");
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        t.setImagePromotionnelle(new DocumentReference("Image promotionnelle", image.toAbsolutePath().toString()));
        Path f = dossier.resolve("ancien" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);

        ContexteApplication ctx = new ContexteApplication(dossier.resolve("donnees"), DonneesDemo.clubs(),
                DonneesDemo.licences());
        Tournoi relu = ctx.chargerTournoi(f);
        assertTrue(relu.getImagePromotionnelle().getEmplacement().startsWith(DocumentsDeposes.DOSSIER + "/"));
        assertEquals(relu.getImagePromotionnelle().getEmplacement(),
                DepotTournoi.charger(f).getTournoi().getImagePromotionnelle().getEmplacement(), "fichier réenregistré");
        assertTrue(ctx.documents().fichier(relu.getImagePromotionnelle()).isPresent());
        assertTrue(!Files.readString(f).contains(image.toAbsolutePath().toString()), "plus de chemin absolu");
    }
}
