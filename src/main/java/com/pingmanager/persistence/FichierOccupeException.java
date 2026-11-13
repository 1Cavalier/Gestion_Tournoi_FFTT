package com.pingmanager.persistence;

/**
 * Levée quand un fichier de tournoi reste verrouillé par un autre poste ou une autre
 * fenêtre après plusieurs essais (point 21). Rien n'a été écrit : l'action peut être
 * refaite. Le message est prêt à afficher.
 */
public final class FichierOccupeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FichierOccupeException(String message) {
        super(message);
    }
}
