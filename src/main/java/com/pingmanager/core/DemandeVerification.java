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
     * @param email         email du compte concerné (non vide)
     * @param empreinteCode empreinte du code de vérification (non vide) — produite
     *                      via le port de hachage, jamais le code en clair
     * @param expiration    instant au-delà duquel le code n'est plus valable (non nul)
     */
    public DemandeVerification(String email, String empreinteCode, Instant expiration) {
        this.email = exigerNonVide(email, "email").toLowerCase();
        this.empreinteCode = exigerNonVide(empreinteCode, "empreinteCode");
        this.expiration = Objects.requireNonNull(expiration, "expiration");
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
