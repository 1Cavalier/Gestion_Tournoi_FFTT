package com.pingmanager.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Numéro de portable d'un licencié pour les convocations par SMS (B019).
 *
 * <p>Facultatif et soumis au <b>consentement explicite</b> de la personne
 * (RGPD) : un contact n'existe que si ce consentement a été recueilli, à
 * l'inscription ou par le club, et sa date est conservée. Le numéro ne sert
 * qu'aux convocations du tournoi et il est effacé à la fin du tournoi.</p>
 *
 * @param numeroLicence  licence du joueur ou de l'arbitre
 * @param numero         numéro au format international ({@code +33612345678})
 * @param consentementLe date et heure du consentement
 */
public record ContactSms(String numeroLicence, String numero, LocalDateTime consentementLe) {

    public ContactSms {
        Objects.requireNonNull(numeroLicence, "numeroLicence");
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(consentementLe, "consentementLe");
        if (numeroLicence.isBlank() || numero.isBlank()) {
            throw new IllegalArgumentException("Licence et numéro sont obligatoires.");
        }
    }
}
