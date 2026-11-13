package com.pingmanager.persistence;

import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonWriter;

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
        // Point 21 : fichier illisible ou tronqué → copie de secours « .bak », signalée.
        return SessionMapperComplet.lire(EcritureSure.lireJson(source).document());
    }
}
