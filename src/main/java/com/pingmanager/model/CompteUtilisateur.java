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

    /** Rôle enregistré dans le compte ({@link RoleCompte#MEMBRE} par défaut). */
    private RoleCompte role = RoleCompte.MEMBRE;

    /**
     * Point 13 : vrai une fois l'email confirmé par le code à usage unique envoyé à
     * cette adresse. Tant qu'il est faux, le compte n'a aucun droit d'organisateur,
     * de club ni de supervision (contrôlé par le contexte de l'application).
     */
    private boolean emailVerifie;

    /**
     * @param email               email de connexion (identité du compte, non vide)
     * @param nom                 nom de la personne (non vide)
     * @param prenom              prénom de la personne (non vide)
     * @param empreinteMotDePasse empreinte du mot de passe (non vide) ; le modèle
     *                            ne juge pas de sa forme, il exige seulement
     *                            qu'elle soit présente
     */
    public CompteUtilisateur(String email, String nom, String prenom, String empreinteMotDePasse) {
        this.email = Textes.obligatoire(email, "email").toLowerCase();
        this.nom = Textes.obligatoire(nom, "nom");
        this.prenom = Textes.obligatoire(prenom, "prenom");
        this.empreinteMotDePasse = Textes.obligatoire(empreinteMotDePasse, "empreinteMotDePasse");
        this.licenceLiee = null;
    }

    public String getEmail() {
        return email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = Textes.obligatoire(nom, "nom");
    }

    public String getPrenom() {
        return prenom;
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
        this.empreinteMotDePasse = Textes.obligatoire(empreinteMotDePasse, "empreinteMotDePasse");
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
        this.licenceLiee = Textes.obligatoire(numeroLicence, "numeroLicence");
    }

    /**
     * Retire la licence liée (point 14 : par le titulaire ou par un administrateur
     * du club de la licence ; le contrôle du droit relève du {@code core}).
     */
    public void delierLicence() {
        this.licenceLiee = null;
    }

    // -------------------------------------------------------------------------
    // Vérification de l'email (point 13)
    // -------------------------------------------------------------------------

    /** @return {@code true} si l'email a été confirmé par le code reçu à cette adresse. */
    public boolean isEmailVerifie() {
        return emailVerifie;
    }

    /** Marque l'email comme vérifié (idempotent). */
    public void marquerEmailVerifie() {
        this.emailVerifie = true;
    }

    // -------------------------------------------------------------------------
    // Rôle (persisté ; jamais déduit de l'email)
    // -------------------------------------------------------------------------

    /** @return le rôle du compte, jamais {@code null}. */
    public RoleCompte getRole() {
        return role;
    }

    /** Fixe le rôle du compte ; {@code null} refusé. */
    public void setRole(RoleCompte role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    /** @return {@code true} si le compte porte le rôle de supervision. */
    public boolean estSupervision() {
        return role == RoleCompte.SUPERVISION;
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
}
