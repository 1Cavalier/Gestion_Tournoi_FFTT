package com.pingmanager.core;

/**
 * Port : envoie un SMS (B019). Jamais d'appel : des messages écrits seulement.
 *
 * <p>Le cœur exprime le besoin d'envoyer un message court sans dépendre d'un
 * prestataire. Les implémentations vivent hors du {@code core} : simulation
 * affichée à l'écran aujourd'hui, prestataire réel plus tard (Brevo recommandé
 * par la veille B020, {@code docs/RECHERCHE_SMS.md} : une requête POST JSON
 * {@code sender} / {@code recipient} / {@code content}, type transactionnel,
 * réponse avec un {@code messageId}).</p>
 */
public interface EnvoiSms {

    /**
     * Envoie un SMS.
     *
     * @param expediteur nom d'expéditeur alphanumérique (nom du club, 11 caractères au plus)
     * @param numero     numéro du destinataire au format international ({@code +33612345678})
     * @param texte      texte du message (GSM 7 bits, 160 caractères)
     * @return l'issue de l'envoi ; ne lève pas d'exception pour un refus du service
     */
    ResultatEnvoiSms envoyer(String expediteur, String numero, String texte);
}
