package com.pingmanager.persistence.json;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DÉMONSTRATION DE FAILLE (audit de sécurité, voir RAPPORT_SECURITE.md).
 *
 * <p>Ce test passe <b>parce que</b> l'analyseur actuel n'a pas de limite de
 * profondeur. Lors de la correction, il devra être <b>inversé</b> (une
 * {@link JsonException} claire est attendue) et renommé sans « faille_S09 ».</p>
 */
class FaillesSecuriteJsonParserTest {

    /**
     * Faille S-09 : l'analyse est récursive sans borne. Un fichier de 200 000
     * crochets ouvrants (200 Ko) provoque un {@link StackOverflowError} — une
     * {@code Error}, pas une {@code RuntimeException} : les {@code catch
     * (RuntimeException)} des écrans (liste des tournois, import JSON, géocodage)
     * ne l'interceptent pas. Un tel fichier déposé dans le dossier « tournois »
     * empêche le démarrage du logiciel (nettoyage et liste lus au lancement).
     * Correctif : profondeur maximale (64) et taille maximale de texte.
     */
    @Test
    void imbrication_profonde_provoque_un_debordement_de_pile_faille_S09() throws Exception {
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

        assertNotNull(leve.get());
        assertInstanceOf(StackOverflowError.class, leve.get(),
                "comportement vulnérable : Error au lieu d'une JsonException");
    }
}
