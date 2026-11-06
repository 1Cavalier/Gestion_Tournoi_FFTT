package com.pingmanager.model;

import java.util.Objects;

/**
 * Compte d'une personne administrant les tournois d'un club.
 *
 * <p>Classe de données pure. Un compte porte l'identité de la personne
 * (nom, prénom, email de connexion), l'empreinte de son mot de passe, le
 * <b>numéro du club</b> auquel elle demande à se rattacher, et l'état de
 * <b>vérification de son email</b>.</p>
 *
 * <p>L'identité d'un compte repose sur son {@link #getEmail() email}, unique.
 * Le rattachement au club se fait par le numéro FFTT du club (et non par une
 * référence directe à l'objet {@link Club}) : c'est l'identifiant métier stable,
 * cohérent avec le reste du modèle et robuste à la persistance. La résolution
 * numéro → {@link Club} se fera hors du modèle.</p>
 *
 * <p>Le mot de passe n'est jamais manipulé en clair par cette classe : elle ne
 * stocke qu'une <b>empreinte</b> ({@link #getEmpreinteMotDePasse()}) produite
 * ailleurs (le hachage relèvera d'un service, pas du modèle). De même, le compte
 * sait s'il est vérifié et peut passer à l'état « vérifié »
 * ({@link #marquerEmailVerifie()}), mais ce n'est pas lui qui génère, envoie ou
 * contrôle le code de vérification : cette orchestration viendra du {@code core}.</p>
 */
public final class CompteOrganisateur {

    private final String email;
    private String nom;
    private String prenom;

    /** Empreinte (hash) du mot de passe, produite par du code externe. */
    private String empreinteMotDePasse;

    /** Numéro FFTT du club auquel ce compte se rattache. */
    private final String numeroClub;

    /** Vrai une fois l'adresse email confirmée via le code de vérification. */
    private boolean emailVerifie;

    /**
     * @param email               email de connexion (identité du compte, non vide)
     * @param nom                 nom de la personne (non vide)
     * @param prenom              prénom de la personne (non vide)
     * @param empreinteMotDePasse empreinte du mot de passe (non vide) ; le modèle
     *                            ne juge pas de sa forme, il exige seulement qu'elle
     *                            soit présente
     * @param numeroClub          numéro FFTT du club de rattachement (non vide)
     */
    public CompteOrganisateur(String email, String nom, String prenom,
                              String empreinteMotDePasse, String numeroClub) {
        this.email = exigerNonVide(email, "email").toLowerCase();
        this.nom = exigerNonVide(nom, "nom");
        this.prenom = exigerNonVide(prenom, "prenom");
        this.empreinteMotDePasse = exigerNonVide(empreinteMotDePasse, "empreinteMotDePasse");
        this.numeroClub = exigerNonVide(numeroClub, "numeroClub");
        this.emailVerifie = false;
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

    public String getEmpreinteMotDePasse() {
        return empreinteMotDePasse;
    }

    /**
     * Remplace l'empreinte du mot de passe (changement de mot de passe).
     * La nouvelle empreinte est produite ailleurs ; le modèle exige seulement
     * qu'elle soit non vide.
     */
    public void setEmpreinteMotDePasse(String empreinteMotDePasse) {
        this.empreinteMotDePasse = exigerNonVide(empreinteMotDePasse, "empreinteMotDePasse");
    }

    public String getNumeroClub() {
        return numeroClub;
    }

    public boolean isEmailVerifie() {
        return emailVerifie;
    }

    /**
     * Marque l'email comme vérifié. Opération idempotente : appeler cette méthode
     * sur un compte déjà vérifié n'a aucun effet.
     */
    public void marquerEmailVerifie() {
        this.emailVerifie = true;
    }

    // -------------------------------------------------------------------------
    // Identité : deux comptes sont égaux s'ils ont le même email.
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompteOrganisateur autre)) {
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
        return prenom + " " + nom + " <" + email + "> (club " + numeroClub + ")";
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
