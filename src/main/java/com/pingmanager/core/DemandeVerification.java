package com.pingmanager.core;

import java.time.Instant;
import java.util.Objects;

/**
 * État transitoire d'une vérification d'email en cours.
 *
 * <p>Lorsqu'un organisateur s'inscrit, un code est envoyé à l'adresse officielle
 * de son club. Cet objet mémorise ce qu'il faut pour vérifier ensuite le code
 * saisi : l'email concerné, l'<b>empreinte</b> du code (jamais le code en clair)
 * et l'instant d'expiration.</p>
 *
 * <p>Il est délibérément tenu <b>hors du modèle</b> : le
 * {@link com.pingmanager.model.CompteOrganisateur} reste minimal et pur, et ce
 * porteur d'état vit dans le {@code core}. Le service le renvoie à l'appelant à
 * l'inscription, puis le reçoit à la confirmation. Son stockage durable (le
 * temps que l'organisateur saisisse le code) sera un détail de persistance
 * traité plus tard ; ici, l'objet est immuable et sans effet de bord.</p>
 */
public final class DemandeVerification {

    private final String email;
    private final String empreinteCode;
    private final Instant expiration;
    /**
     * Objet de la demande (par exemple « club:08910002 » ou « licence:9999001 »), ou
     * {@code null} pour une demande sans objet précis. Le code confirmé doit viser le
     * même objet que la demande : un code reçu pour un club n'ouvre pas un autre club.
     */
    private final String objet;

    /**
     * @param email         email du compte concerné (non vide)
     * @param empreinteCode empreinte du code de vérification (non vide) — produite
     *                      via le port de hachage, jamais le code en clair
     * @param expiration    instant au-delà duquel le code n'est plus valable (non nul)
     */
    public DemandeVerification(String email, String empreinteCode, Instant expiration) {
        this(email, empreinteCode, expiration, null);
    }

    /**
     * @param objet objet de la demande (club, licence, email…), ou {@code null}
     */
    public DemandeVerification(String email, String empreinteCode, Instant expiration, String objet) {
        this.email = exigerNonVide(email, "email").toLowerCase();
        this.empreinteCode = exigerNonVide(empreinteCode, "empreinteCode");
        this.expiration = Objects.requireNonNull(expiration, "expiration");
        this.objet = objet == null || objet.isBlank() ? null : objet.trim();
    }

    /** @return l'objet de la demande, ou {@code null}. */
    public String getObjet() {
        return objet;
    }

    /** @return {@code true} si la demande vise exactement cet objet. */
    public boolean viseObjet(String attendu) {
        return objet != null && attendu != null && objet.equals(attendu.trim());
    }

    public String getEmail() {
        return email;
    }

    public String getEmpreinteCode() {
        return empreinteCode;
    }

    /**
     * @param maintenant instant courant
     * @return {@code true} si la demande est expirée à cet instant
     */
    public boolean estExpiree(Instant maintenant) {
        Objects.requireNonNull(maintenant, "maintenant");
        return !maintenant.isBefore(expiration);
    }

    @Override
    public String toString() {
        return "DemandeVerification[" + email + ", expire " + expiration + "]";
    }

    private static String exigerNonVide(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » est obligatoire.");
        }
        return valeur.trim();
    }
}
