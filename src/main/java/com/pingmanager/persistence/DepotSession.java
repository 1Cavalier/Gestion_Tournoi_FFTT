package com.pingmanager.persistence;

import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;
import com.pingmanager.persistence.json.JsonWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sauvegarde et recharge une {@link SessionSalle} complète (tournoi +
 * discipline + ordonnancement) dans un fichier local, via une écriture
 * atomique.
 *
 * <p>Utilisé aussi bien pour un enregistrement volontaire de session que pour
 * l'autosave de reprise sur incident. Le format est le même JSON versionné,
 * sans dépendance externe.</p>
 */
public final class DepotSession {

    private DepotSession() {
    }

    public static void sauvegarder(SessionSalle session, Path cible) {
        Json doc = SessionMapperComplet.ecrire(session);
        String texte = JsonWriter.ecrire(doc, true);
        DepotTournoi.ecrireAtomique(cible, texte);
    }

    public static SessionSalle charger(Path source) {
        String texte;
        try {
            texte = Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec du chargement de session depuis " + source, e);
        }
        Json doc = JsonParser.analyser(texte);
        return SessionMapperComplet.lire(doc);
    }
}
