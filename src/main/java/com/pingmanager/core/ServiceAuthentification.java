package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Service d'authentification : création d'un compte et connexion.
 *
 * <p>C'est la première porte de l'application, avant toute démarche de liaison
 * (licence, club). Il ne fait que deux choses, toutes deux adossées au port de
 * hachage :</p>
 * <ul>
 *   <li><b>Créer un compte</b> : normalise l'email, refuse un email déjà pris,
 *       hache le mot de passe et fabrique le {@link CompteUtilisateur}. La
 *       persistance du compte est laissée à l'appelant (le dépôt applicatif).</li>
 *   <li><b>Connecter</b> : vérifie un mot de passe en clair contre l'empreinte
 *       d'un compte existant fourni par l'appelant.</li>
 * </ul>
 *
 * <p>Le service est <b>sans état</b> et ne dépend que du port
 * {@link HachageMotDePasse} : aucune technologie concrète, le cœur reste pur et
 * testable. Le mot de passe en clair ne transite que le temps de l'appel.</p>
 */
public final class ServiceAuthentification {

    private final HachageMotDePasse hachage;

    public ServiceAuthentification(HachageMotDePasse hachage) {
        this.hachage = Objects.requireNonNull(hachage, "hachage");
    }

    /**
     * Crée un compte utilisateur.
     *
     * @param email             email de connexion (normalisé en minuscules)
     * @param nom               nom de la personne
     * @param prenom            prénom de la personne
     * @param motDePasseEnClair mot de passe choisi (haché immédiatement, non conservé)
     * @param emailDejaUtilise  prédicat « cet email est déjà pris » fourni par
     *                          l'appelant (par ex. le dépôt applicatif) ;
     *                          {@code null} = aucun contrôle d'unicité
     * @return le compte créé, ou {@link Optional#empty()} si l'email est déjà pris
     */
    public Optional<CompteUtilisateur> creerCompte(String email, String nom, String prenom,
                                                   String motDePasseEnClair,
                                                   Predicate<String> emailDejaUtilise) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(motDePasseEnClair, "motDePasseEnClair");

        String emailNormalise = email.trim().toLowerCase();
        if (emailDejaUtilise != null && emailDejaUtilise.test(emailNormalise)) {
            return Optional.empty();
        }
        String empreinte = hachage.hacher(motDePasseEnClair);
        return Optional.of(new CompteUtilisateur(emailNormalise, nom, prenom, empreinte));
    }

    /**
     * Vérifie un mot de passe contre l'empreinte d'un compte.
     *
     * @param compte            le compte concerné
     * @param motDePasseEnClair le mot de passe saisi
     * @return {@code true} si le mot de passe correspond
     */
    public boolean motDePasseCorrect(CompteUtilisateur compte, String motDePasseEnClair) {
        Objects.requireNonNull(compte, "compte");
        if (motDePasseEnClair == null || motDePasseEnClair.isEmpty()) {
            return false;
        }
        return hachage.verifier(motDePasseEnClair, compte.getEmpreinteMotDePasse());
    }

    /**
     * Connexion : retrouve le compte via une fonction de recherche fournie par
     * l'appelant et vérifie le mot de passe.
     *
     * @param email             email saisi
     * @param motDePasseEnClair mot de passe saisi
     * @param rechercheCompte   fonction email → compte (par ex. le dépôt applicatif)
     * @return le compte si l'email existe et que le mot de passe est correct,
     *         sinon {@link Optional#empty()}
     */
    public Optional<CompteUtilisateur> connecter(String email, String motDePasseEnClair,
                                                 java.util.function.Function<String, Optional<CompteUtilisateur>> rechercheCompte) {
        Objects.requireNonNull(rechercheCompte, "rechercheCompte");
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<CompteUtilisateur> compte = rechercheCompte.apply(email.trim().toLowerCase());
        if (compte.isPresent() && motDePasseCorrect(compte.get(), motDePasseEnClair)) {
            return compte;
        }
        return Optional.empty();
    }
}
