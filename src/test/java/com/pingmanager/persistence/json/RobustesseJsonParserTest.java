package com.pingmanager.persistence.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fichiers JSON piégés (ancienne faille S-09, README section 5 point 17) :
 * l'analyseur répond par une {@link JsonLimiteException} claire et rapide,
 * jamais par un {@code StackOverflowError} ni un blocage.
 */
class RobustesseJsonParserTest {

    /** Ancien test de démonstration de la faille S-09, inversé après correction. */
    @Test
    void imbrication_de_200_000_crochets_donne_une_erreur_claire_meme_avec_une_pile_reduite() throws Exception {
        String malveillant = "[".repeat(200_000);
        AtomicReference<Throwable> leve = new AtomicReference<>();
        // Pile réduite et fixe : résultat identique quelle que soit la JVM de test.
        Thread t = new Thread(null, () -> {
            try {
                JsonParser.analyser(malveillant);
            } catch (Throwable e) {
                leve.set(e);
            }
        }, "audit-json", 256 * 1024);
        t.start();
        t.join();

        assertInstanceOf(JsonLimiteException.class, leve.get());
        assertTrue(leve.get().getMessage().contains("trop imbriqué"), leve.get().getMessage());
    }

    @Test
    void objets_imbriques_sur_100_000_niveaux_refuses_rapidement() {
        String malveillant = "{\"a\":".repeat(100_000) + "1" + "}".repeat(100_000);
        JsonLimiteException e = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> assertThrows(JsonLimiteException.class, () -> JsonParser.analyser(malveillant)));
        assertTrue(e.getMessage().contains(String.valueOf(JsonParser.PROFONDEUR_MAX)), e.getMessage());
    }

    @Test
    void imbrication_a_la_limite_acceptee_et_un_niveau_de_plus_refuse() {
        int n = JsonParser.PROFONDEUR_MAX;
        Json v = JsonParser.analyser("[".repeat(n) + "]".repeat(n));
        assertTrue(v.estTableau());
        assertThrows(JsonLimiteException.class,
                () -> JsonParser.analyser("[".repeat(n + 1) + "]".repeat(n + 1)));
    }

    @Test
    void la_profondeur_redescend_apres_chaque_objet_ferme() {
        // Beaucoup d'objets frères peu profonds : jamais pris pour une imbrication.
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10_000; i++) {
            sb.append(i == 0 ? "" : ",").append("{\"x\":[{\"y\":[1]}]}");
        }
        Json v = assertDoesNotThrow(() -> JsonParser.analyser(sb.append(']').toString()));
        assertEquals(10_000, v.elements().size());
    }

    @Test
    void texte_au_dela_de_la_taille_maximale_refuse_sans_analyse() {
        String enorme = " ".repeat(JsonParser.TAILLE_MAX_CARACTERES + 1);
        JsonLimiteException e = assertThrows(JsonLimiteException.class, () -> JsonParser.analyser(enorme));
        assertTrue(e.getMessage().contains("trop volumineux"), e.getMessage());
    }

    @Test
    void fichier_trop_gros_refuse_avant_lecture(@TempDir Path dossier) throws IOException {
        Path f = dossier.resolve("piege.json");
        try (RandomAccessFile r = new RandomAccessFile(f.toFile(), "rw")) {
            r.setLength(FichierJson.TAILLE_MAX_OCTETS + 1); // fichier creux : rien n'est écrit
        }
        assertThrows(JsonLimiteException.class, () -> FichierJson.verifierTaille(f));
        Files.writeString(f, "{}");
        assertDoesNotThrow(() -> FichierJson.verifierTaille(f));
    }

    @Test
    void une_erreur_de_limite_reste_une_json_exception_pour_les_appelants_existants() {
        assertThrows(JsonException.class, () -> JsonParser.analyser("[".repeat(1000)));
    }
}
