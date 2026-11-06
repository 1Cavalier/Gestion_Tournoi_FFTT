package com.pingmanager.core;

import com.pingmanager.model.MessageSms;
import com.pingmanager.model.StatutEnvoiSms;

import java.util.List;

/**
 * Bilan d'une convocation par SMS (B019) : messages inscrits au journal et
 * personnes sans numéro consenti.
 *
 * @param declenche  false si rien ne devait partir (annonce sans SMS, envoi automatique coupé)
 * @param messages   messages inscrits au journal (envoyés, en échec ou bloqués par la limite)
 * @param sansNumero prénom et nom des personnes sans numéro consenti
 */
public record ResultatConvocationsSms(boolean declenche, List<MessageSms> messages, List<String> sansNumero) {

    public ResultatConvocationsSms {
        messages = List.copyOf(messages);
        sansNumero = List.copyOf(sansNumero);
    }

    /** Rien à envoyer. */
    public static ResultatConvocationsSms aucun() {
        return new ResultatConvocationsSms(false, List.of(), List.of());
    }

    public long nombreEnvoyes() {
        return messages.stream().filter(m -> m.statut() == StatutEnvoiSms.ENVOYE).count();
    }

    public boolean limiteAtteinte() {
        return messages.stream().anyMatch(m -> m.statut() == StatutEnvoiSms.LIMITE_ATTEINTE);
    }

    public boolean aEchoue() {
        return messages.stream().anyMatch(m -> m.statut() == StatutEnvoiSms.ECHEC);
    }

    /** Résumé court pour l'écran, vide si rien ne devait partir. */
    public String resume() {
        if (!declenche) {
            return "";
        }
        long n = nombreEnvoyes();
        StringBuilder sb = new StringBuilder(n == 0 ? "Aucun SMS envoyé" : n + (n > 1 ? " SMS envoyés" : " SMS envoyé"));
        if (!sansNumero.isEmpty()) {
            sb.append(" · sans numéro : ").append(String.join(", ", sansNumero));
        }
        if (limiteAtteinte()) {
            sb.append(" · limite d'envois atteinte");
        }
        if (aEchoue()) {
            sb.append(" · échec d'envoi (voir le journal)");
        }
        return sb.append('.').toString();
    }
}
