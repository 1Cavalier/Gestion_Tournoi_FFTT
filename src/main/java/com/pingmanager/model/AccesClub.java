package com.pingmanager.model;

import java.util.Objects;

/**
 * Lien d'autorisation entre une personne (identifiée par son email) et un club
 * (identifié par son numéro FFTT).
 *
 * <p>Un accès matérialise le fait qu'une personne est <b>habilitée à administrer</b>
 * un club donné. Il est distinct du {@link CompteOrganisateur} : le compte porte
 * l'authentification (identité, mot de passe, vérification), tandis que l'accès
 * porte le <b>droit</b>. Cette séparation permet, à terme, qu'un club ait
 * plusieurs personnes habilitées, ou qu'un accès soit préparé avant l'existence
 * d'un compte actif.</p>
 *
 * <p>Classe de données pure. La création d'un accès à l'issue d'une vérification
 * d'email réussie relèvera d'un service ({@code core}), pas du modèle.</p>
 *
 * <p>L'identité d'un accès est le couple (numéro de club, email) : une même
 * personne n'a qu'un seul accès par club.</p>
 */
public final class AccesClub {

    private final String numeroClub;
    private final String email;

    /** Identité de la personne habilitée (optionnelle, pour l'affichage). */
    private String nom;
    private String prenom;

    /** Niveau d'habilitation ; premier niveau par défaut. */
    private NiveauAcces niveau = NiveauAcces.ADMIN_NIVEAU_1;

    /**
     * @param numeroClub numéro FFTT du club concerné (non vide)
     * @param email      email de la personne habilitée (non vide)
     */
    public AccesClub(String numeroClub, String email) {
        this.numeroClub = exigerNonVide(numeroClub, "numeroClub");
        this.email = exigerNonVide(email, "email").toLowerCase();
    }

    /**
     * @param numeroClub numéro FFTT du club concerné (non vide)
     * @param email      email de la personne habilitée (non vide)
     * @param nom        nom de la personne (optionnel)
     * @param prenom     prénom de la personne (optionnel)
     */
    public AccesClub(String numeroClub, String email, String nom, String prenom) {
        this(numeroClub, email);
        this.nom = normaliserOptionnel(nom);
        this.prenom = normaliserOptionnel(prenom);
    }

    /**
     * @param numeroClub numéro FFTT du club concerné (non vide)
     * @param email      email de la personne habilitée (non vide)
     * @param nom        nom de la personne (optionnel)
     * @param prenom     prénom de la personne (optionnel)
     * @param niveau     niveau d'habilitation (non nul)
     */
    public AccesClub(String numeroClub, String email, String nom, String prenom, NiveauAcces niveau) {
        this(numeroClub, email, nom, prenom);
        this.niveau = niveau == null ? NiveauAcces.ADMIN_NIVEAU_1 : niveau;
    }

    /** @return le niveau d'habilitation (jamais nul). */
    public NiveauAcces getNiveau() {
        return niveau;
    }

    public void setNiveau(NiveauAcces niveau) {
        this.niveau = niveau == null ? NiveauAcces.ADMIN_NIVEAU_1 : niveau;
    }

    /** @return {@code true} si cet accès est celui de l'administrateur principal (référent). */
    public boolean estPrincipal() {
        return niveau == NiveauAcces.ADMIN_PRINCIPAL;
    }

    public String getNumeroClub() {
        return numeroClub;
    }

    public String getEmail() {
        return email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = normaliserOptionnel(nom);
    }

    public String getPrenom() {
        return prenom;
    }

    // -------------------------------------------------------------------------
    // Identité : couple (numéro de club, email).
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccesClub autre)) {
            return false;
        }
        return numeroClub.equals(autre.numeroClub) && email.equals(autre.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroClub, email);
    }

    @Override
    public String toString() {
        return "Accès club " + numeroClub + " pour <" + email + ">";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String exigerNonVide(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » est obligatoire.");
        }
        return valeur.trim();
    }

    private static String normaliserOptionnel(String valeur) {
        if (valeur == null) {
            return null;
        }
        String nettoye = valeur.trim();
        return nettoye.isEmpty() ? null : nettoye;
    }
}
