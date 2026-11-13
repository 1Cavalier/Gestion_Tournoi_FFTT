package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
 * <p>Le service ne dépend que des ports {@link HachageMotDePasse} et {@link Horloge} :
 * aucune technologie concrète, le cœur reste pur et testable. Le mot de passe en clair
 * ne transite que le temps de l'appel. Son seul état est la {@link LimiteurEssais limite
 * d'essais} de connexion (point 15 du README) : 5 échecs pour un email, puis blocage
 * temporaire croissant. La {@link PolitiqueMotDePasse politique} (10 caractères au
 * moins) s'applique à la création et au changement, jamais à la connexion : les comptes
 * existants ne sont pas invalidés.</p>
 */
public final class ServiceAuthentification {

    private final HachageMotDePasse hachage;
    private final LimiteurEssais limiteConnexion;

    /** Limite d'essais sur l'horloge système (préférer le constructeur à horloge injectée). */
    public ServiceAuthentification(HachageMotDePasse hachage) {
        this(hachage, Horloge.systeme());
    }

    public ServiceAuthentification(HachageMotDePasse hachage, Horloge horloge) {
        this(hachage, new LimiteurEssais(horloge));
    }

    public ServiceAuthentification(HachageMotDePasse hachage, LimiteurEssais limiteConnexion) {
        this.hachage = Objects.requireNonNull(hachage, "hachage");
        this.limiteConnexion = Objects.requireNonNull(limiteConnexion, "limiteConnexion");
    }

    /**
     * Crée un compte utilisateur, avec un motif précis en cas de refus.
     *
     * @param email             email de connexion (normalisé en minuscules)
     * @param nom               nom de la personne
     * @param prenom            prénom de la personne
     * @param motDePasseEnClair mot de passe choisi (haché immédiatement, non conservé) ;
     *                          {@value PolitiqueMotDePasse#LONGUEUR_MINIMALE} caractères au moins
     * @param emailDejaUtilise  prédicat « cet email est déjà pris » fourni par
     *                          l'appelant (par ex. le dépôt applicatif) ;
     *                          {@code null} = aucun contrôle d'unicité
     * @return le résultat détaillé (compte créé, ou motif du refus)
     */
    public ResultatCreationCompte creer(String email, String nom, String prenom,
                                        String motDePasseEnClair,
                                        Predicate<String> emailDejaUtilise) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(motDePasseEnClair, "motDePasseEnClair");

        String emailNormalise = email.trim().toLowerCase();
        if (emailDejaUtilise != null && emailDejaUtilise.test(emailNormalise)) {
            return ResultatCreationCompte.emailDejaUtilise();
        }
        Optional<String> refus = PolitiqueMotDePasse.motifRefus(motDePasseEnClair);
        if (refus.isPresent()) {
            return ResultatCreationCompte.motDePasseTropCourt(refus.get());
        }
        String empreinte = hachage.hacher(motDePasseEnClair);
        return ResultatCreationCompte.cree(new CompteUtilisateur(emailNormalise, nom, prenom, empreinte));
    }

    /**
     * Crée un compte utilisateur (forme courte de {@link #creer}).
     *
     * @return le compte créé, ou {@link Optional#empty()} si l'email est déjà pris ou si
     *         le mot de passe est trop court
     */
    public Optional<CompteUtilisateur> creerCompte(String email, String nom, String prenom,
                                                   String motDePasseEnClair,
                                                   Predicate<String> emailDejaUtilise) {
        return Optional.ofNullable(creer(email, nom, prenom, motDePasseEnClair, emailDejaUtilise).getCompte());
    }

    /**
     * Vérifie un mot de passe contre l'empreinte d'un compte (sans limite d'essais).
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
     * Connexion avec limite d'essais : retrouve le compte via une fonction de recherche
     * fournie par l'appelant et vérifie le mot de passe. Après 5 échecs pour un même
     * email (connu ou non : rien ne trahit l'existence d'un compte), l'email est bloqué
     * 1 min, puis 2, 4… jusqu'à 1 h ; pendant le blocage, même le bon mot de passe est
     * refusé sans être vérifié. Une connexion réussie remet le compteur à zéro.
     *
     * @param email             email saisi
     * @param motDePasseEnClair mot de passe saisi
     * @param rechercheCompte   fonction email → compte (par ex. le dépôt applicatif)
     * @return le résultat détaillé
     */
    public ResultatConnexion connexion(String email, String motDePasseEnClair,
                                       Function<String, Optional<CompteUtilisateur>> rechercheCompte) {
        Objects.requireNonNull(rechercheCompte, "rechercheCompte");
        if (email == null || email.isBlank()) {
            return ResultatConnexion.identifiantsIncorrects();
        }
        String emailNormalise = email.trim().toLowerCase();
        Duration attente = limiteConnexion.attenteRestante(emailNormalise);
        if (!attente.isZero()) {
            return ResultatConnexion.tropDEssais(attente);
        }
        Optional<CompteUtilisateur> compte = rechercheCompte.apply(emailNormalise);
        if (compte.isPresent() && motDePasseCorrect(compte.get(), motDePasseEnClair)) {
            limiteConnexion.enregistrerReussite(emailNormalise);
            return ResultatConnexion.connecte(compte.get());
        }
        limiteConnexion.enregistrerEchec(emailNormalise);
        return ResultatConnexion.identifiantsIncorrects();
    }

    /**
     * Connexion (forme courte de {@link #connexion}, limite d'essais comprise).
     *
     * @return le compte si l'email existe, que le mot de passe est correct et que
     *         l'email n'est pas bloqué, sinon {@link Optional#empty()}
     */
    public Optional<CompteUtilisateur> connecter(String email, String motDePasseEnClair,
                                                 Function<String, Optional<CompteUtilisateur>> rechercheCompte) {
        return Optional.ofNullable(connexion(email, motDePasseEnClair, rechercheCompte).getCompte());
    }

    /**
     * Change le mot de passe d'un compte : le mot de passe actuel est exigé (et compte
     * dans la limite d'essais de connexion de cet email), le nouveau doit respecter la
     * {@link PolitiqueMotDePasse}. L'empreinte est remplacée dans le compte ; la
     * persistance est laissée à l'appelant.
     */
    public ResultatChangementMotDePasse changerMotDePasse(CompteUtilisateur compte, String ancienEnClair,
                                                          String nouveauEnClair) {
        Objects.requireNonNull(compte, "compte");
        Duration attente = limiteConnexion.attenteRestante(compte.getEmail());
        if (!attente.isZero()) {
            return ResultatChangementMotDePasse.tropDEssais(attente);
        }
        if (!motDePasseCorrect(compte, ancienEnClair)) {
            limiteConnexion.enregistrerEchec(compte.getEmail());
            return ResultatChangementMotDePasse.ancienIncorrect();
        }
        limiteConnexion.enregistrerReussite(compte.getEmail());
        Optional<String> refus = PolitiqueMotDePasse.motifRefus(nouveauEnClair);
        if (refus.isPresent()) {
            return ResultatChangementMotDePasse.motDePasseTropCourt(refus.get());
        }
        compte.setEmpreinteMotDePasse(hachage.hacher(nouveauEnClair));
        return ResultatChangementMotDePasse.change();
    }
}
