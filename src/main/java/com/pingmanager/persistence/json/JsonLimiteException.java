package com.pingmanager.persistence.json;

/**
 * Document JSON refusé parce qu'il dépasse une borne de sécurité (taille ou
 * profondeur d'imbrication) : fichier corrompu ou piégé. Le message est
 * destiné à l'utilisateur.
 */
public class JsonLimiteException extends JsonException {

    private static final long serialVersionUID = 1L;

    public JsonLimiteException(String message) {
        super(message);
    }
}
