package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;

/** Issue détaillée de la création d'un compte utilisateur ({@link ServiceAuthentification}). */
public final class ResultatCreationCompte {

    /** Motif de l'issue. */
    public enum Motif {
        /** Compte créé. */
        CREE,
        /** Refus : un compte existe déjà avec cet email. */
        EMAIL_DEJA_UTILISE,
        /** Refus : mot de passe trop court ({@link PolitiqueMotDePasse}). */
        MOT_DE_PASSE_TROP_COURT
    }

    private final Motif motif;
    private final CompteUtilisateur compte;
    private final String message;

    private ResultatCreationCompte(Motif motif, CompteUtilisateur compte, String message) {
        this.motif = motif;
        this.compte = compte;
        this.message = message;
    }

    public static ResultatCreationCompte cree(CompteUtilisateur compte) {
        return new ResultatCreationCompte(Motif.CREE, compte, "Compte créé.");
    }

    public static ResultatCreationCompte emailDejaUtilise() {
        return new ResultatCreationCompte(Motif.EMAIL_DEJA_UTILISE, null, "Un compte existe déjà avec cet email.");
    }

    public static ResultatCreationCompte motDePasseTropCourt(String message) {
        return new ResultatCreationCompte(Motif.MOT_DE_PASSE_TROP_COURT, null, message);
    }

    public Motif getMotif() {
        return motif;
    }

    public boolean estCree() {
        return motif == Motif.CREE;
    }

    /** @return le compte créé (motif {@link Motif#CREE}), sinon {@code null}. */
    public CompteUtilisateur getCompte() {
        return compte;
    }

    /** @return le message à afficher, en clair. */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ResultatCreationCompte[" + motif + "]";
    }
}
