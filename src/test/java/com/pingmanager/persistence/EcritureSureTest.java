package com.pingmanager.persistence;

import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 21 : écriture sûre (temporaire forcé, copie .bak, déplacement atomique) et relecture de secours. */
class EcritureSureTest {

    @TempDir
    Path dossier;

    @Test
    void la_version_precedente_est_gardee_en_bak_et_aucun_temporaire_ne_reste() throws Exception {
        Path f = dossier.resolve("a.json");
        EcritureSure.ecrire(f, "{\"v\":1}");
        assertFalse(Files.exists(EcritureSure.secours(f)), "pas de version précédente");
        EcritureSure.ecrire(f, "{\"v\":2}");
        assertEquals("{\"v\":2}", Files.readString(f));
        assertEquals("{\"v\":1}", Files.readString(EcritureSure.secours(f)));
        try (Stream<Path> s = Files.list(dossier)) {
            assertEquals(2, s.count(), "seulement le fichier et sa copie");
        }
    }

    @Test
    void fichier_tronque_relu_depuis_la_copie_de_secours_et_signale() throws Exception {
        Path f = dossier.resolve("b.json");
        EcritureSure.ecrire(f, "{\"v\":1}");
        EcritureSure.ecrire(f, "{\"v\":2}");
        Files.writeString(f, "{\"v\":"); // coupure en pleine écriture hors application
        EcritureSure.LectureJson l = EcritureSure.lireJson(f);
        assertTrue(l.depuisSecours());
        assertEquals(1, l.document().get("v").asInt());
        assertTrue(l.avertissement().contains("copie de secours"));
        assertTrue(EcritureSure.avertissements().contains(l.avertissement()));
    }

    @Test
    void une_version_abimee_n_ecrase_jamais_la_bonne_copie_de_secours() throws Exception {
        Path f = dossier.resolve("c.json");
        EcritureSure.ecrire(f, "{\"v\":1}");
        EcritureSure.ecrire(f, "{\"v\":2}");
        Files.writeString(f, "{abîmé");
        EcritureSure.ecrire(f, "{\"v\":3}");
        assertEquals("{\"v\":1}", Files.readString(EcritureSure.secours(f)), "la copie lisible est conservée");
    }

    @Test
    void fichier_et_copie_illisibles_erreur_comme_avant() throws Exception {
        Path f = dossier.resolve("d.json");
        Files.writeString(f, "{");
        Files.writeString(EcritureSure.secours(f), "[");
        assertThrows(RuntimeException.class, () -> EcritureSure.lireJson(f));
        assertThrows(UncheckedIOException.class, () -> EcritureSure.lireJson(dossier.resolve("absent.json")));
    }

    @Test
    void tournoi_au_fichier_tronque_recharge_depuis_sa_copie() throws Exception {
        Path f = dossier.resolve("open" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(new Tournoi("Open v1", "Club", LocalDate.of(2027, 4, 10)), f);
        DepotTournoi.sauvegarder(new Tournoi("Open v2", "Club", LocalDate.of(2027, 4, 10)), f);
        String texte = Files.readString(f);
        Files.writeString(f, texte.substring(0, texte.length() / 2));
        assertEquals("Open v1", DepotTournoi.charger(f).getTournoi().getNom());
    }

    @Test
    void depot_applicatif_et_session_passent_par_l_ecriture_sure() throws Exception {
        Path app = dossier.resolve(DepotApplication.NOM_FICHIER);
        new DepotApplication().sauvegarder(app);
        new DepotApplication().sauvegarder(app);
        assertTrue(Files.exists(EcritureSure.secours(app)));
        Files.writeString(app, "");
        DepotApplication.charger(app); // pas d'exception : copie de secours relue
    }
}
