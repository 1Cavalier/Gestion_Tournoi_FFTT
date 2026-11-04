package com.pingmanager.model;

import java.util.Objects;

/**
 * Compte de base d'une personne sur l'application, commun à tous les usages :
 * un simple membre qui consulte les tournois comme un organisateur qui les crée.
 *
 * <p>C'est le socle d'authentification : identité (nom, prénom, email de
 * connexion) et empreinte du mot de passe. Deux <b>rattachements facultatifs</b>
 * viennent se greffer dessus, chacun par une démarche distincte :</p>
 * <ul>
 *   <li>la <b>liaison à une licence FFTT</b> — le numéro de licence validé est
 *       porté ici ({@link #getLicenceLiee()}) une fois la vérification faite ;</li>
 *   <li>la <b>liaison à un ou plusieurs clubs</b> en tant qu'organisateur — elle,
 *       ne vit pas sur le compte mais dans des {@link AccesClub} séparés (le
 *       compte porte l'authentification, l'accès porte le droit).</li>
 * </ul>
 *
 * <p>Classe de données pure, dans l'esprit de {@link CompteOrganisateur} : elle
 * ne hache pas, n'envoie rien, ne vérifie aucun code — ces orchestrations
 * relèvent du {@code core}. Le mot de passe n'est jamais manipulé en clair : le
 * compte ne conserve qu'une <b>empreinte</b> produite ailleurs.</p>
 *
 * <p>L'identité d'un compte est son {@link #getEmail() email}, unique et
 * normalisé en minuscules.</p>
 */
public final class CompteUtilisateur {

    private final String email;
    private String nom;
    private String prenom;

    /** Empreinte (hash) du mot de passe, produite par un service externe. */
    private String empreinteMotDePasse;

    /** Numéro de licence FFTT lié, ou {@code null} tant qu'aucune liaison n'a été faite. */
    private String licenceLiee;

    /**
     * @param email               email de connexion (identité du compte, non vide)
     * @param nom                 nom de la personne (non vide)
     * @param prenom              prénom de la personne (non vide)
     * @param empreinteMotDePasse empreinte du mot de passe (non vide) ; le modèle
     *                            ne juge pas de sa forme, il exige seulement
     *                            qu'elle soit présente
     */
    public CompteUtilisateur(String email, String nom, String prenom, String empreinteMotDePasse) {
        this.email = exigerNonVide(email, "email").toLowerCase();
        this.nom = exigerNonVide(nom, "nom");
        this.prenom = exigerNonVide(prenom, "prenom");
        this.empreinteMotDePasse = exigerNonVide(empreinteMotDePasse, "empreinteMotDePasse");
        this.licenceLiee = null;
    }

    public String getEmail() {
        return email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = exigerNonVide(nom, "nom");
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = exigerNonVide(prenom, "prenom");
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public String getEmpreinteMotDePasse() {
        return empreinteMotDePasse;
    }

    /**
     * Remplace l'empreinte du mot de passe (changement de mot de passe). La
     * nouvelle empreinte est produite ailleurs ; le modèle exige seulement
     * qu'elle soit non vide.
     */
    public void setEmpreinteMotDePasse(String empreinteMotDePasse) {
        this.empreinteMotDePasse = exigerNonVide(empreinteMotDePasse, "empreinteMotDePasse");
    }

    // -------------------------------------------------------------------------
    // Liaison à une licence FFTT
    // -------------------------------------------------------------------------

    /** @return le numéro de licence lié, ou {@code null} si aucune liaison. */
    public String getLicenceLiee() {
        return licenceLiee;
    }

    /** @return {@code true} si une licence FFTT a été liée à ce compte. */
    public boolean aLicenceLiee() {
        return licenceLiee != null;
    }

    /**
     * Lie une licence FFTT à ce compte. La validité de la licence (existence
     * dans le référentiel fédéral) est vérifiée en amont par un service ; le
     * modèle se contente d'enregistrer le numéro validé.
     *
     * @param numeroLicence numéro de licence validé (non vide)
     */
    public void lierLicence(String numeroLicence) {
        this.licenceLiee = exigerNonVide(numeroLicence, "numeroLicence");
    }

    /** Rompt la liaison à la licence (retour à l'état non lié). */
    public void delierLicence() {
        this.licenceLiee = null;
    }

    // -------------------------------------------------------------------------
    // Identité : deux comptes sont égaux s'ils ont le même email.
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompteUtilisateur autre)) {
            return false;
        }
        return email.equals(autre.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return prenom + " " + nom + " <" + email + ">"
                + (licenceLiee != null ? " [licence " + licenceLiee + "]" : "");
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
}
