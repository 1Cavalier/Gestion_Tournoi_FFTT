package com.pingmanager.core;

import com.pingmanager.model.CompteOrganisateur;

/**
 * Résultat d'une tentative d'inscription d'un organisateur passée par le
 * {@link ServiceComptesOrganisateurs}.
 *
 * <p>En cas de succès, porte le {@link CompteOrganisateur} créé (non encore
 * vérifié) et la {@link DemandeVerification} associée, à re-présenter lors de la
 * confirmation. En cas d'échec, porte le motif précis et aucun compte.</p>
 *
 * <p>Objet valeur immuable, obtenu via les fabriques statiques.</p>
 */
public final class ResultatInscriptionOrganisateur {

    /** Motif de l'issue d'une inscription d'organisateur. */
    public enum Motif {
        /** Inscription enregistrée : un code a été envoyé à l'email officiel du club. */
        CODE_ENVOYE,
        /** Refus : aucun club ne correspond au numéro fourni. */
        CLUB_INTROUVABLE,
        /** Refus : un compte existe déjà avec cet email. */
        EMAIL_DEJA_UTILISE,
        /** Refus : mot de passe trop court (point 15, {@link PolitiqueMotDePasse}). */
        MOT_DE_PASSE_TROP_COURT
    }

    private final Motif motif;
    private final CompteOrganisateur compte;
    private final DemandeVerification demande;
    private final String message;

    private ResultatInscriptionOrganisateur(Motif motif, CompteOrganisateur compte,
                                            DemandeVerification demande) {
        this(motif, compte, demande, null);
    }

    private ResultatInscriptionOrganisateur(Motif motif, CompteOrganisateur compte,
                                            DemandeVerification demande, String message) {
        this.motif = motif;
        this.compte = compte;
        this.demande = demande;
        this.message = message;
    }

    public static ResultatInscriptionOrganisateur motDePasseTropCourt(String message) {
        return new ResultatInscriptionOrganisateur(Motif.MOT_DE_PASSE_TROP_COURT, null, null, message);
    }

    /** @return le motif du refus en clair (mot de passe trop court), sinon {@code null}. */
    public String getMessage() {
        return message;
    }

    public static ResultatInscriptionOrganisateur codeEnvoye(CompteOrganisateur compte,
                                                             DemandeVerification demande) {
        return new ResultatInscriptionOrganisateur(Motif.CODE_ENVOYE, compte, demande);
    }

    public static ResultatInscriptionOrganisateur clubIntrouvable() {
        return new ResultatInscriptionOrganisateur(Motif.CLUB_INTROUVABLE, null, null);
    }

    public static ResultatInscriptionOrganisateur emailDejaUtilise() {
        return new ResultatInscriptionOrganisateur(Motif.EMAIL_DEJA_UTILISE, null, null);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return le compte créé (motif {@link Motif#CODE_ENVOYE}), ou {@code null}. */
    public CompteOrganisateur getCompte() {
        return compte;
    }

    /** @return la demande de vérification (motif {@link Motif#CODE_ENVOYE}), ou {@code null}. */
    public DemandeVerification getDemande() {
        return demande;
    }

    /** @return true si l'inscription a réussi et qu'un code a été envoyé. */
    public boolean estSucces() {
        return motif == Motif.CODE_ENVOYE;
    }

    @Override
    public String toString() {
        return "ResultatInscriptionOrganisateur[" + motif + "]";
    }
}
