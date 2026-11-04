package com.pingmanager.core;

/**
 * Port : envoie un email.
 *
 * <p>Le cœur exprime le besoin d'envoyer un message (par exemple le code de
 * vérification à l'adresse officielle d'un club) sans dépendre d'aucune
 * technologie d'envoi. Les implémentations concrètes (console, SMTP, service
 * tiers…) vivent hors du {@code core} ; le cœur ne dépend ainsi d'aucune
 * bibliothèque de production, conformément à la règle d'architecture.</p>
 */
public interface EnvoiEmail {

    /**
     * Envoie un email.
     *
     * @param destinataire adresse email du destinataire (non vide)
     * @param sujet        objet du message
     * @param corps        contenu du message
     */
    void envoyer(String destinataire, String sujet, String corps);
}
