package com.pingmanager.core;

import java.time.Duration;

/** Issue détaillée d'un changement de mot de passe ({@link ServiceAuthentification}). */
public final class ResultatChangementMotDePasse {

    /** Motif de l'issue. */
    public enum Motif {
        /** Mot de passe changé (empreinte remplacée dans le compte). */
        CHANGE,
        /** Refus : le mot de passe actuel saisi est faux. */
        ANCIEN_INCORRECT,
        /** Refus : le nouveau mot de passe est trop court ({@link PolitiqueMotDePasse}). */
        MOT_DE_PASSE_TROP_COURT,
        /** Refus : trop d'échecs récents sur ce compte. */
        TROP_D_ESSAIS
    }

    private final Motif motif;
    private final String message;
    private final Duration attente;

    private ResultatChangementMotDePasse(Motif motif, String message, Duration attente) {
        this.motif = motif;
        this.message = message;
        this.attente = attente;
    }

    public static ResultatChangementMotDePasse change() {
        return new ResultatChangementMotDePasse(Motif.CHANGE, "Mot de passe changé.", Duration.ZERO);
    }

    public static ResultatChangementMotDePasse ancienIncorrect() {
        return new ResultatChangementMotDePasse(Motif.ANCIEN_INCORRECT,
                "Le mot de passe actuel est incorrect.", Duration.ZERO);
    }

    public static ResultatChangementMotDePasse motDePasseTropCourt(String message) {
        return new ResultatChangementMotDePasse(Motif.MOT_DE_PASSE_TROP_COURT, message, Duration.ZERO);
    }

    public static ResultatChangementMotDePasse tropDEssais(Duration attente) {
        return new ResultatChangementMotDePasse(Motif.TROP_D_ESSAIS, LimiteurEssais.messageBlocage(attente), attente);
    }

    public Motif getMotif() {
        return motif;
    }

    public boolean estChange() {
        return motif == Motif.CHANGE;
    }

    public String getMessage() {
        return message;
    }

    public Duration getAttente() {
        return attente;
    }

    @Override
    public String toString() {
        return "ResultatChangementMotDePasse[" + motif + "]";
    }
}
