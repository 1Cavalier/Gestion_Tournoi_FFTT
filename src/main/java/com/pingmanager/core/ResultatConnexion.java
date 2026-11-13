package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;

import java.time.Duration;

/** Issue détaillée d'une connexion ({@link ServiceAuthentification#connexion}). */
public final class ResultatConnexion {

    /** Motif de l'issue. */
    public enum Motif {
        /** Identifiants corrects. */
        CONNECTE,
        /** Email inconnu ou mot de passe faux (indistincts, pour ne rien révéler). */
        IDENTIFIANTS_INCORRECTS,
        /** Trop d'échecs récents pour cet email : essai refusé sans vérification. */
        TROP_D_ESSAIS
    }

    private final Motif motif;
    private final CompteUtilisateur compte;
    private final Duration attente;

    private ResultatConnexion(Motif motif, CompteUtilisateur compte, Duration attente) {
        this.motif = motif;
        this.compte = compte;
        this.attente = attente;
    }

    public static ResultatConnexion connecte(CompteUtilisateur compte) {
        return new ResultatConnexion(Motif.CONNECTE, compte, Duration.ZERO);
    }

    public static ResultatConnexion identifiantsIncorrects() {
        return new ResultatConnexion(Motif.IDENTIFIANTS_INCORRECTS, null, Duration.ZERO);
    }

    public static ResultatConnexion tropDEssais(Duration attente) {
        return new ResultatConnexion(Motif.TROP_D_ESSAIS, null, attente);
    }

    public Motif getMotif() {
        return motif;
    }

    public boolean estConnecte() {
        return motif == Motif.CONNECTE;
    }

    /** @return le compte connecté (motif {@link Motif#CONNECTE}), sinon {@code null}. */
    public CompteUtilisateur getCompte() {
        return compte;
    }

    /** @return l'attente avant un nouvel essai (motif {@link Motif#TROP_D_ESSAIS}), sinon zéro. */
    public Duration getAttente() {
        return attente;
    }

    /** @return le message à afficher, en clair. */
    public String getMessage() {
        if (motif == Motif.CONNECTE) {
            return "Connecté.";
        }
        if (motif == Motif.TROP_D_ESSAIS) {
            return LimiteurEssais.messageBlocage(attente);
        }
        return "Email ou mot de passe incorrect.";
    }

    @Override
    public String toString() {
        return "ResultatConnexion[" + motif + "]";
    }
}
