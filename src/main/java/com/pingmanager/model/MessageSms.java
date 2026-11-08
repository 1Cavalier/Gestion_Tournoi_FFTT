package com.pingmanager.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ligne du journal des envois de SMS d'un tournoi (B019).
 *
 * <p>Le numéro de portable n'y figure pas : il est effacé à la fin du tournoi
 * alors que le journal reste, pour le compte des envois.</p>
 *
 * @param instant       date et heure de l'envoi
 * @param type          nature de la convocation
 * @param numeroLicence licence du destinataire
 * @param destinataire  prénom et nom du destinataire
 * @param texte         texte du message
 * @param statut        issue de l'envoi
 * @param detail        identifiant rendu par le service ou motif d'échec (peut être vide)
 */
public record MessageSms(LocalDateTime instant, TypeConvocation type, String numeroLicence, String destinataire,
                         String texte, StatutEnvoiSms statut, String detail) {

    public MessageSms {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(numeroLicence, "numeroLicence");
        Objects.requireNonNull(texte, "texte");
        Objects.requireNonNull(statut, "statut");
        destinataire = destinataire == null ? "" : destinataire;
        detail = detail == null ? "" : detail;
    }
}
