package com.pingmanager.model;

/**
 * Issue d'un envoi de SMS inscrite au journal des envois (B019). Pas de
 * {@code switch} sur cette énumération.
 */
public enum StatutEnvoiSms {

    /** Message remis au service d'envoi (ou à la simulation). */
    ENVOYE("Envoyé"),
    /** Le service d'envoi a refusé ou n'a pas répondu. */
    ECHEC("Échec"),
    /** Non envoyé : la limite d'envois du tournoi est atteinte. */
    LIMITE_ATTEINTE("Non envoyé (limite atteinte)");

    private final String libelle;

    StatutEnvoiSms(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
