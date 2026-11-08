package com.pingmanager.core;

/**
 * Issue de la saisie d'un numéro de portable pour les convocations par SMS (B019).
 *
 * @param motif  motif précis
 * @param numero numéro retenu au format international (vide si refusé)
 */
public record ResultatContactSms(Motif motif, String numero) {

    /** Motif de l'issue. Pas de {@code switch} sur cette énumération. */
    public enum Motif {
        /** Numéro enregistré avec le consentement. */
        ENREGISTRE,
        /** Consentement non coché : rien n'est gardé. */
        CONSENTEMENT_MANQUANT,
        /** Numéro non reconnu comme un portable. */
        NUMERO_INVALIDE,
        /** Licence absente. */
        LICENCE_MANQUANTE,
        /** Le tournoi est terminé : les numéros n'ont plus lieu d'être gardés. */
        TOURNOI_TERMINE
    }

    public ResultatContactSms {
        numero = numero == null ? "" : numero;
    }

    public boolean estReussi() {
        return motif == Motif.ENREGISTRE;
    }

    /** Message pour l'écran. */
    public String message() {
        if (motif == Motif.ENREGISTRE) {
            return "Numéro enregistré (" + NumeroPortable.masquer(numero) + ").";
        }
        if (motif == Motif.CONSENTEMENT_MANQUANT) {
            return "Le numéro n'est enregistré qu'avec l'accord explicite de la personne : cochez le consentement.";
        }
        if (motif == Motif.NUMERO_INVALIDE) {
            return "Numéro de portable non reconnu : 06 ou 07 suivi de 8 chiffres, ou format international (+…).";
        }
        if (motif == Motif.LICENCE_MANQUANTE) {
            return "Choisissez d'abord le joueur ou l'arbitre.";
        }
        return "Le tournoi est terminé : les numéros ont été ou vont être effacés.";
    }
}
