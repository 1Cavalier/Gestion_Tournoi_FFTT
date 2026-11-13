package com.pingmanager.core;

import java.time.Duration;

/**
 * Issue détaillée de la confirmation de l'email d'un compte
 * ({@link ServiceVerificationEmail#confirmer}), point 13 du README.
 */
public final class ResultatVerificationEmail {

    /** Motif de l'issue. */
    public enum Statut {
        /** Code correct : l'email est vérifié, les droits du compte s'activent. */
        VERIFIE,
        /** L'email était déjà vérifié : rien à faire. */
        DEJA_VERIFIE,
        /** Aucune demande en cours (application redémarrée, code jamais demandé). */
        AUCUNE_DEMANDE,
        /** Le code a expiré. */
        CODE_EXPIRE,
        /** Le code saisi est incorrect. */
        CODE_INCORRECT,
        /** Trop de codes faux récents : essai refusé sans vérification (point 15). */
        TROP_D_ESSAIS
    }

    private final Statut statut;
    private final Duration attente;

    private ResultatVerificationEmail(Statut statut, Duration attente) {
        this.statut = statut;
        this.attente = attente;
    }

    static ResultatVerificationEmail de(Statut statut) {
        return new ResultatVerificationEmail(statut, Duration.ZERO);
    }

    public static ResultatVerificationEmail aucuneDemande() {
        return de(Statut.AUCUNE_DEMANDE);
    }

    static ResultatVerificationEmail tropDEssais(Duration attente) {
        return new ResultatVerificationEmail(Statut.TROP_D_ESSAIS, attente);
    }

    public Statut getStatut() {
        return statut;
    }

    /** @return vrai si l'email est vérifié à l'issue de l'appel (vérifié maintenant ou déjà). */
    public boolean estVerifie() {
        return statut == Statut.VERIFIE || statut == Statut.DEJA_VERIFIE;
    }

    /** @return l'attente avant un nouvel essai (statut {@link Statut#TROP_D_ESSAIS}), sinon zéro. */
    public Duration getAttente() {
        return attente;
    }

    /** @return le message à afficher, en clair. */
    public String getMessage() {
        String s = statut.name();
        if ("VERIFIE".equals(s)) {
            return "Email vérifié : votre compte est activé.";
        }
        if ("DEJA_VERIFIE".equals(s)) {
            return "Votre email est déjà vérifié.";
        }
        if ("AUCUNE_DEMANDE".equals(s)) {
            return "Aucun code en cours : demandez un nouveau code.";
        }
        if ("CODE_EXPIRE".equals(s)) {
            return "Le code a expiré. Demandez un nouveau code.";
        }
        if ("CODE_INCORRECT".equals(s)) {
            return "Code incorrect.";
        }
        return LimiteurEssais.messageBlocage(attente);
    }

    @Override
    public String toString() {
        return "ResultatVerificationEmail[" + statut + "]";
    }
}
