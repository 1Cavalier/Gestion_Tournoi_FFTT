package com.pingmanager.ui.ecran;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** L'export des résultats n'écrase jamais un fichier déjà présent. */
class ExportResultatsCibleLibreTest {

    @TempDir
    Path dossier;

    @Test
    void fichier_absent_garde_son_nom_puis_numerote_les_suivants() throws Exception {
        assertEquals("resultats-SM-parties.csv", VuePhaseFinale.cibleLibre(dossier, "resultats-SM-parties.csv").getFileName().toString());
        Files.writeString(dossier.resolve("resultats-SM-parties.csv"), "ancien");
        assertEquals("resultats-SM-parties-2.csv", VuePhaseFinale.cibleLibre(dossier, "resultats-SM-parties.csv").getFileName().toString());
        Files.writeString(dossier.resolve("resultats-SM-parties-2.csv"), "ancien");
        assertEquals("resultats-SM-parties-3.csv", VuePhaseFinale.cibleLibre(dossier, "resultats-SM-parties.csv").getFileName().toString());
    }
}
