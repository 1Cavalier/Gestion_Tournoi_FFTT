package com.pingmanager.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Réglages, numéros de portable consentis et journal des convocations par SMS
 * d'un tournoi (B019). Jamais d'appel : des messages écrits seulement.
 *
 * <p>Par défaut rien ne part : l'envoi automatique est coupé, aucun numéro
 * n'est connu. Les règles (consentement, limite, déclenchement) sont dans
 * {@code core.ServiceConvocationsSms} ; cette classe ne fait que garder les
 * données.</p>
 */
public final class ConvocationsSms {

    /** Limite d'envois par défaut pour un tournoi (environ 150 joueurs × 2 convocations). */
    public static final int LIMITE_PAR_DEFAUT = 300;
    /** Plafond de la limite réglable, garde-fou contre une faute de frappe. */
    public static final int LIMITE_MAXIMALE = 5000;

    private boolean envoiAutomatique;
    private String expediteur;
    private int limiteEnvois = LIMITE_PAR_DEFAUT;
    private final Map<String, ContactSms> contacts = new LinkedHashMap<>();
    private final List<MessageSms> journal = new ArrayList<>();
    private LocalDateTime numerosEffacesLe;

    /** Les SMS partent-ils d'eux-mêmes quand un match est envoyé sur une table (si l'annonce choisie comprend le SMS) ? */
    public boolean isEnvoiAutomatique() {
        return envoiAutomatique;
    }

    public void setEnvoiAutomatique(boolean envoiAutomatique) {
        this.envoiAutomatique = envoiAutomatique;
    }

    /** Nom d'expéditeur choisi par le club, ou null pour celui proposé d'après le club organisateur. */
    public String getExpediteur() {
        return expediteur;
    }

    public void setExpediteur(String expediteur) {
        this.expediteur = expediteur == null || expediteur.isBlank() ? null : expediteur.trim();
    }

    /** Nombre maximal de SMS envoyés pour le tournoi. */
    public int getLimiteEnvois() {
        return limiteEnvois;
    }

    public void setLimiteEnvois(int limiteEnvois) {
        if (limiteEnvois < 0 || limiteEnvois > LIMITE_MAXIMALE) {
            throw new IllegalArgumentException("Limite d'envois hors bornes (0 à " + LIMITE_MAXIMALE + ") : "
                    + limiteEnvois);
        }
        this.limiteEnvois = limiteEnvois;
    }

    /** Numéros consentis, dans l'ordre de saisie. */
    public Collection<ContactSms> getContacts() {
        return Collections.unmodifiableCollection(contacts.values());
    }

    /** Numéro consenti d'un licencié, ou null. */
    public ContactSms contact(String numeroLicence) {
        return numeroLicence == null ? null : contacts.get(numeroLicence);
    }

    /** Ajoute ou remplace le numéro d'un licencié (le service vérifie le consentement et le format). */
    public void enregistrerContact(ContactSms contact) {
        Objects.requireNonNull(contact, "contact");
        contacts.put(contact.numeroLicence(), contact);
    }

    /** Retire le numéro d'un licencié (retrait du consentement). */
    public boolean retirerContact(String numeroLicence) {
        return contacts.remove(numeroLicence) != null;
    }

    /** Efface tous les numéros et note la date de l'effacement. */
    public void effacerNumeros(LocalDateTime instant) {
        contacts.clear();
        numerosEffacesLe = Objects.requireNonNull(instant, "instant");
    }

    /** Date du dernier effacement des numéros, ou null. */
    public LocalDateTime getNumerosEffacesLe() {
        return numerosEffacesLe;
    }

    /** Relecture d'un fichier : date d'effacement mémorisée. */
    public void setNumerosEffacesLe(LocalDateTime numerosEffacesLe) {
        this.numerosEffacesLe = numerosEffacesLe;
    }

    /** Journal des envois, du plus ancien au plus récent. */
    public List<MessageSms> getJournal() {
        return Collections.unmodifiableList(journal);
    }

    public void journaliser(MessageSms message) {
        journal.add(Objects.requireNonNull(message, "message"));
    }

    /** Nombre de SMS réellement envoyés (ceux qui comptent dans la limite). */
    public int nombreEnvoyes() {
        int n = 0;
        for (MessageSms m : journal) {
            if (m.statut() == StatutEnvoiSms.ENVOYE) {
                n++;
            }
        }
        return n;
    }

    /** Rien à écrire dans la sauvegarde : réglages par défaut, aucun numéro, journal vide. */
    public boolean estVierge() {
        return !envoiAutomatique && expediteur == null && limiteEnvois == LIMITE_PAR_DEFAUT && contacts.isEmpty()
                && journal.isEmpty() && numerosEffacesLe == null;
    }
}
