package com.pingmanager.ui.infra;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Enregistrement des feuilles avant ouverture : nom de fichier propre, jamais d'écrasement. */
class ImpressionFeuillesTest {

    @Test
    void nom_de_fichier_sans_accents_ni_caracteres_genants() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 14, 9, 30, 12);
        assertEquals("feuille-poule-c-toutes-series-poule-b-20261114-093012.pdf",
                ImpressionFeuilles.nomFichier("Feuille poule — C – Toutes séries / Poule B", t));
        assertEquals("feuille-20261114-093012.pdf", ImpressionFeuilles.nomFichier("« »", t));
        assertTrue(ImpressionFeuilles.nomFichier("x".repeat(200), t).length() < 90);
    }

    @Test
    void deux_feuilles_dans_la_meme_seconde_ne_s_ecrasent_pas() throws Exception {
        Path dossier = Files.createTempDirectory("feuilles");
        ImpressionFeuilles.Resultat a = ImpressionFeuilles.ouvrir(new byte[] {1}, "feuille", false, dossier, false);
        ImpressionFeuilles.Resultat b = ImpressionFeuilles.ouvrir(new byte[] {2}, "feuille", false, dossier, false);

        assertTrue(a.estReussi());
        assertTrue(b.estReussi());
        assertNotEquals(a.fichier(), b.fichier());
        assertEquals(1, Files.readAllBytes(a.fichier())[0]);
        assertTrue(a.message().contains(a.fichier().toAbsolutePath().toString()));
    }

    @Test
    void echec_d_ecriture_explique_sans_planter() throws Exception {
        Path fichierBloquant = Files.createTempFile("pas-un-dossier", ".txt");
        ImpressionFeuilles.Resultat r = ImpressionFeuilles.ouvrir(new byte[] {1}, "feuille", false,
                fichierBloquant.resolve("sous"), false);
        assertFalse(r.estReussi());
        assertTrue(r.message().startsWith("La feuille n'a pas pu être enregistrée"));
    }
}
