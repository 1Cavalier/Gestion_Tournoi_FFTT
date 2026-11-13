package com.pingmanager.core;

import com.pingmanager.model.FicheLicence;

import java.time.Duration;

/**
 * Issue détaillée d'une étape de la liaison d'une licence à un compte (point 14 du
 * README) : demande, confirmation ou suppression de la liaison
 * ({@link ServiceLiaisonLicence}).
 */
public final class ResultatLiaisonLicence {

    /** Motif de l'issue. */
    public enum Statut {
        /** Demande acceptée : un code a été envoyé au référent du club de la licence. */
        CODE_ENVOYE,
        /** Confirmation réussie : la licence est liée au compte. */
        LIEE,
        /** La liaison a été retirée. */
        DELIEE,
        /** Le compte n'a pas encore vérifié son email (point 13). */
        EMAIL_NON_VERIFIE,
        /** Aucune licence ne porte ce numéro. */
        LICENCE_INTROUVABLE,
        /** La licence est déjà liée à un autre compte. */
        DEJA_LIEE_A_UN_AUTRE_COMPTE,
        /** Le compte porte déjà une licence (la délier d'abord). */
        COMPTE_DEJA_LIE,
        /** La date de naissance saisie ne correspond pas à la fiche. */
        DATE_DE_NAISSANCE_INCORRECTE,
        /** Le club de la licence est inconnu ou sans adresse officielle : personne pour valider. */
        CLUB_SANS_REFERENT,
        /** Aucune demande en cours, ou demande faite pour une autre licence. */
        AUCUNE_DEMANDE,
        /** Le code a expiré. */
        CODE_EXPIRE,
        /** Le code saisi est incorrect. */
        CODE_INCORRECT,
        /** Trop d'essais faux récents (point 15). */
        TROP_D_ESSAIS,
        /** Le compte ne porte aucune licence (rien à délier). */
        AUCUNE_LICENCE_LIEE,
        /** Délier la licence d'un autre : réservé à un administrateur du club de la licence. */
        DROIT_INSUFFISANT
    }

    private final Statut statut;
    private final FicheLicence fiche;
    private final DemandeVerification demande;
    private final Duration attente;

    private ResultatLiaisonLicence(Statut statut, FicheLicence fiche, DemandeVerification demande, Duration attente) {
        this.statut = statut;
        this.fiche = fiche;
        this.demande = demande;
        this.attente = attente;
    }

    static ResultatLiaisonLicence refus(Statut statut) {
        return new ResultatLiaisonLicence(statut, null, null, Duration.ZERO);
    }

    /** Refus « aucune licence liée » (aucun compte ne porte la licence visée). */
    public static ResultatLiaisonLicence aucuneLicenceLiee() {
        return refus(Statut.AUCUNE_LICENCE_LIEE);
    }

    static ResultatLiaisonLicence tropDEssais(Duration attente) {
        return new ResultatLiaisonLicence(Statut.TROP_D_ESSAIS, null, null, attente);
    }

    static ResultatLiaisonLicence codeEnvoye(FicheLicence fiche, DemandeVerification demande) {
        return new ResultatLiaisonLicence(Statut.CODE_ENVOYE, fiche, demande, Duration.ZERO);
    }

    static ResultatLiaisonLicence liee(FicheLicence fiche) {
        return new ResultatLiaisonLicence(Statut.LIEE, fiche, null, Duration.ZERO);
    }

    static ResultatLiaisonLicence deliee() {
        return new ResultatLiaisonLicence(Statut.DELIEE, null, null, Duration.ZERO);
    }

    public Statut getStatut() {
        return statut;
    }

    /** @return vrai pour {@link Statut#CODE_ENVOYE}, {@link Statut#LIEE} et {@link Statut#DELIEE}. */
    public boolean estReussi() {
        return statut == Statut.CODE_ENVOYE || statut == Statut.LIEE || statut == Statut.DELIEE;
    }

    /** @return la fiche de la licence (code envoyé ou liée), sinon {@code null}. */
    public FicheLicence getFiche() {
        return fiche;
    }

    /** @return la demande à conserver jusqu'à la confirmation (code envoyé), sinon {@code null}. */
    public DemandeVerification getDemande() {
        return demande;
    }

    public Duration getAttente() {
        return attente;
    }

    /** @return le message à afficher, en clair. */
    public String getMessage() {
        String s = statut.name();
        if ("CODE_ENVOYE".equals(s)) {
            return "Un code a été envoyé au référent de votre club (" + fiche.getNomClub()
                    + ") : demandez-le-lui pour terminer la liaison.";
        }
        if ("LIEE".equals(s)) {
            return "Licence liée à votre compte.";
        }
        if ("DELIEE".equals(s)) {
            return "Licence déliée.";
        }
        if ("EMAIL_NON_VERIFIE".equals(s)) {
            return "Vérifiez d'abord votre email.";
        }
        if ("LICENCE_INTROUVABLE".equals(s)) {
            return "Aucune licence ne correspond à ce numéro.";
        }
        if ("DEJA_LIEE_A_UN_AUTRE_COMPTE".equals(s)) {
            return "Cette licence est déjà liée à un autre compte. Si c'est la vôtre, "
                    + "demandez à un administrateur de votre club de la délier.";
        }
        if ("COMPTE_DEJA_LIE".equals(s)) {
            return "Votre compte porte déjà une licence : déliez-la d'abord.";
        }
        if ("DATE_DE_NAISSANCE_INCORRECTE".equals(s)) {
            return "La date de naissance ne correspond pas à cette licence.";
        }
        if ("CLUB_SANS_REFERENT".equals(s)) {
            return "Le club de cette licence n'a pas d'adresse officielle connue : liaison impossible "
                    + "pour l'instant.";
        }
        if ("AUCUNE_DEMANDE".equals(s)) {
            return "Aucune demande en cours pour cette licence : recommencez.";
        }
        if ("CODE_EXPIRE".equals(s)) {
            return "Le code a expiré. Recommencez la demande.";
        }
        if ("CODE_INCORRECT".equals(s)) {
            return "Code incorrect.";
        }
        if ("TROP_D_ESSAIS".equals(s)) {
            return LimiteurEssais.messageBlocage(attente);
        }
        if ("AUCUNE_LICENCE_LIEE".equals(s)) {
            return "Aucune licence n'est liée à ce compte.";
        }
        return "Seul le titulaire ou un administrateur du club de la licence peut la délier.";
    }

    @Override
    public String toString() {
        return "ResultatLiaisonLicence[" + statut + "]";
    }
}
