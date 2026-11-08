package com.pingmanager.core;

/**
 * Issue d'un envoi par le port {@link EnvoiSms}.
 *
 * @param reussi      le service a accepté le message
 * @param identifiant identifiant rendu par le service (vide si aucun)
 * @param motif       motif du refus (vide si réussi)
 */
public record ResultatEnvoiSms(boolean reussi, String identifiant, String motif) {

    public ResultatEnvoiSms {
        identifiant = identifiant == null ? "" : identifiant;
        motif = motif == null ? "" : motif;
    }

    public static ResultatEnvoiSms envoye(String identifiant) {
        return new ResultatEnvoiSms(true, identifiant, "");
    }

    public static ResultatEnvoiSms echec(String motif) {
        return new ResultatEnvoiSms(false, "", motif);
    }
}
