package com.pingmanager.persistence.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Borne de taille d'un fichier JSON, vérifiée <b>avant</b> de le charger en
 * mémoire : un fichier de plusieurs gigaoctets déposé dans le dossier de
 * données ne doit pas épuiser la mémoire au démarrage.
 */
public final class FichierJson {

    /** Taille maximale d'un fichier JSON lu, en octets (alignée sur l'analyseur). */
    public static final long TAILLE_MAX_OCTETS = JsonParser.TAILLE_MAX_CARACTERES;

    private FichierJson() {
    }

    /**
     * Refuse un fichier plus gros que {@link #TAILLE_MAX_OCTETS}, sans le lire.
     *
     * @throws JsonLimiteException fichier trop volumineux
     * @throws IOException taille illisible
     */
    public static void verifierTaille(Path source) throws IOException {
        long taille = Files.size(source);
        if (taille > TAILLE_MAX_OCTETS) {
            throw new JsonLimiteException("Fichier JSON trop volumineux ("
                    + (taille / (1024 * 1024)) + " Mo ; maximum "
                    + (TAILLE_MAX_OCTETS / (1024 * 1024)) + " Mo) : " + source.getFileName());
        }
    }
}
