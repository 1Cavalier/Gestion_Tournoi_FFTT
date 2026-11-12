package com.pingmanager.persistence.json;

/**
 * Erreur de lecture, d'écriture ou de structure d'un document JSON.
 *
 * <p>Non vérifiée : une entrée corrompue ou un fichier illisible relèvent d'un
 * traitement au niveau applicatif (proposer un autre fichier, une sauvegarde de
 * secours…), pas d'un {@code try/catch} imposé à chaque appel du mapper.</p>
 */
public class JsonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonException(String message) {
        super(message);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
