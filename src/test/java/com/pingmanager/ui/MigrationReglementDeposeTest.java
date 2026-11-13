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

/**
 * {@link ContexteApplication#chargerTournoi(Path)} : un ancien fichier dont le
 * règlement déposé est un chemin absolu est migré à la première lecture (copie
 * dans le dossier de données, fichier réenregistré) — README section 5, point 18.
 */
class MigrationReglementDeposeTest {

    @TempDir
    Path dossier;

    @Test
    void ancien_chemin_absolu_copie_et_fichier_du_tournoi_reenregistre_a_la_premiere_lecture() throws Exception {
        ContexteApplication ctx = new ContexteApplication(dossier.resolve("donnees"),
                DonneesDemo.clubs(), DonneesDemo.licences());
        Path pdf = dossier.resolve("externe").resolve("reglement.pdf");
        Files.createDirectories(pdf.getParent());
        Files.writeString(pdf, "%PDF-1.4 règlement");
        Tournoi t = new Tournoi("Open migré", "Brunoy", LocalDate.of(2027, 6, 5));
        t.setReglementDocument(new DocumentReference("Règlement", pdf.toAbsolutePath().toString()));
        Path f = dossier.resolve("ancien" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f); // comme une ancienne version : chemin absolu

        Tournoi lu = ctx.chargerTournoi(f);

        String emplacement = lu.getReglementDocument().getEmplacement();
        assertTrue(emplacement.startsWith(DocumentsDeposes.DOSSIER + "/"), emplacement);
        assertTrue(ctx.documents().fichier(lu.getReglementDocument()).isPresent());
        assertEquals(emplacement, DepotTournoi.charger(f).getTournoi().getReglementDocument().getEmplacement(),
                "la migration est enregistrée dans le fichier du tournoi");
        assertTrue(Files.readString(f).contains(emplacement));
    }

    @Test
    void ancien_chemin_absolu_disparu_lu_sans_erreur() {
        ContexteApplication ctx = new ContexteApplication(dossier.resolve("donnees"),
                DonneesDemo.clubs(), DonneesDemo.licences());
        Tournoi t = new Tournoi("Open orphelin", "Brunoy", LocalDate.of(2027, 6, 5));
        String disparu = dossier.resolve("nulle-part.pdf").toAbsolutePath().toString();
        t.setReglementDocument(new DocumentReference("Règlement", disparu));
        Path f = dossier.resolve("orphelin" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);

        Tournoi lu = ctx.chargerTournoi(f);

        assertEquals(disparu, lu.getReglementDocument().getEmplacement());
        assertTrue(ctx.documents().fichier(lu.getReglementDocument()).isEmpty());
    }
}
