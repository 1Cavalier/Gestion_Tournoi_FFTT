package com.pingmanager.persistence;

import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;
import com.pingmanager.persistence.json.Json;

import java.time.LocalDateTime;

/**
 * Convocations par SMS en JSON (B019), champ facultatif {@code convocationsSms}
 * du tournoi : écrit seulement si le club a touché aux réglages, saisi un numéro
 * ou envoyé un SMS ; un tournoi sans SMS garde exactement son ancien format.
 *
 * <pre>
 * "convocationsSms": { "envoiAutomatique": true, "expediteur": "TTBRUNOY", "limiteEnvois": 300,
 *   "numerosEffacesLe": "2026-11-15T08:00",
 *   "contacts": [ {"licence": "123", "numero": "+33612345678", "consentementLe": "2026-11-01T18:30"} ],
 *   "journal": [ {"instant": "…", "type": "MATCH_POULE", "licence": "123", "destinataire": "Hugo MORIN",
 *                 "texte": "…", "statut": "ENVOYE", "detail": "id-1"} ] }
 * </pre>
 *
 * <p>Relecture tolérante : un contact ou une ligne de journal illisible
 * (fichier abîmé ou plus récent) est écarté, une limite hors bornes revient à
 * la valeur par défaut, pour que le fichier s'ouvre toujours.</p>
 */
final class ConvocationsSmsMapper {

    private ConvocationsSmsMapper() {
    }

    static void ecrire(Tournoi t, Json o) {
        ConvocationsSms c = t.getConvocationsSms();
        if (c.estVierge()) {
            return;
        }
        Json j = Json.objet();
        j.set("envoiAutomatique", c.isEnvoiAutomatique());
        if (c.getExpediteur() != null) {
            j.set("expediteur", c.getExpediteur());
        }
        j.set("limiteEnvois", c.getLimiteEnvois());
        if (c.getNumerosEffacesLe() != null) {
            j.set("numerosEffacesLe", c.getNumerosEffacesLe().toString());
        }
        Json contacts = Json.tableau();
        for (ContactSms k : c.getContacts()) {
            contacts.add(Json.objet().set("licence", k.numeroLicence()).set("numero", k.numero())
                    .set("consentementLe", k.consentementLe().toString()));
        }
        j.set("contacts", contacts);
        Json journal = Json.tableau();
        for (MessageSms m : c.getJournal()) {
            journal.add(Json.objet().set("instant", m.instant().toString()).set("type", m.type().name())
                    .set("licence", m.numeroLicence()).set("destinataire", m.destinataire())
                    .set("texte", m.texte()).set("statut", m.statut().name()).set("detail", m.detail()));
        }
        j.set("journal", journal);
        o.set("convocationsSms", j);
    }

    static void lire(Json o, Tournoi t) {
        Json j = o.get("convocationsSms");
        if (j == null || !j.estObjet()) {
            return;
        }
        ConvocationsSms c = t.getConvocationsSms();
        Json auto = j.get("envoiAutomatique");
        c.setEnvoiAutomatique(auto != null && auto.getType() == Json.Type.BOOLEEN && auto.asBoolean());
        c.setExpediteur(chaine(j.get("expediteur")));
        Json limite = j.get("limiteEnvois");
        if (limite != null && limite.getType() == Json.Type.NOMBRE) {
            int l = limite.asInt();
            c.setLimiteEnvois(l < 0 || l > ConvocationsSms.LIMITE_MAXIMALE ? ConvocationsSms.LIMITE_PAR_DEFAUT : l);
        }
        c.setNumerosEffacesLe(dateHeure(j.get("numerosEffacesLe")));
        Json contacts = j.get("contacts");
        if (contacts != null && contacts.estTableau()) {
            for (Json k : contacts.elements()) {
                try {
                    c.enregistrerContact(new ContactSms(chaine(k.get("licence")), chaine(k.get("numero")),
                            dateHeure(k.get("consentementLe"))));
                } catch (RuntimeException ignore) {
                    // Contact illisible (ou sans date de consentement) : non gardé.
                }
            }
        }
        Json journal = j.get("journal");
        if (journal != null && journal.estTableau()) {
            for (Json m : journal.elements()) {
                try {
                    c.journaliser(new MessageSms(dateHeure(m.get("instant")),
                            TypeConvocation.valueOf(chaine(m.get("type"))), chaine(m.get("licence")),
                            chaine(m.get("destinataire")), chaine(m.get("texte")),
                            StatutEnvoiSms.valueOf(chaine(m.get("statut"))), chaine(m.get("detail"))));
                } catch (RuntimeException ignore) {
                    // Ligne illisible ou valeur inconnue (fichier plus récent) : écartée.
                }
            }
        }
    }

    private static String chaine(Json j) {
        return j == null || j.getType() != Json.Type.CHAINE ? null : j.asString();
    }

    private static LocalDateTime dateHeure(Json j) {
        if (j == null || j.getType() != Json.Type.CHAINE) {
            return null;
        }
        try {
            return LocalDateTime.parse(j.asString());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
