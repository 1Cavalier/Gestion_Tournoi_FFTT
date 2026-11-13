package com.pingmanager.core;

import com.pingmanager.model.AccesClub;

/**
 * Compte rendu de la confirmation d'une liaison à un club (saisie du code reçu
 * à l'adresse officielle du club).
 *
 * <p>Soit le code est bon et un {@link AccesClub} est créé (avec son niveau),
 * soit il est refusé pour un motif précis. Objet immuable renvoyé par
 * {@link ServiceLiaisonClub#confirmer}.</p>
 */
public final class ResultatLiaisonClub {

    public enum Statut {
        /** Code correct : l'accès (habilitation) a été créé. */
        VERIFIE,
        /** Le club n'existe pas (ou plus) dans le référentiel. */
        CLUB_INTROUVABLE,
        /** Le code a expiré. */
        CODE_EXPIRE,
        /** Le code saisi est incorrect. */
        CODE_INCORRECT,
        /** Trop de codes faux récents : essai refusé sans vérification (point 15). */
        TROP_D_ESSAIS
    }

    private final Statut statut;
    private final AccesClub acces;
    private final java.time.Duration attente;

    private ResultatLiaisonClub(Statut statut, AccesClub acces) {
        this(statut, acces, java.time.Duration.ZERO);
    }

    private ResultatLiaisonClub(Statut statut, AccesClub acces, java.time.Duration attente) {
        this.statut = statut;
        this.acces = acces;
        this.attente = attente;
    }

    public static ResultatLiaisonClub tropDEssais(java.time.Duration attente) {
        return new ResultatLiaisonClub(Statut.TROP_D_ESSAIS, null, attente);
    }

    /** @return l'attente avant un nouvel essai (statut {@link Statut#TROP_D_ESSAIS}), sinon zéro. */
    public java.time.Duration getAttente() {
        return attente;
    }

    /** @return le message de refus pour trop d'essais, en clair. */
    public String messageBlocage() {
        return LimiteurEssais.messageBlocage(attente);
    }

    public static ResultatLiaisonClub verifie(AccesClub acces) {
        return new ResultatLiaisonClub(Statut.VERIFIE, acces);
    }

    public static ResultatLiaisonClub clubIntrouvable() {
        return new ResultatLiaisonClub(Statut.CLUB_INTROUVABLE, null);
    }

    public static ResultatLiaisonClub codeExpire() {
        return new ResultatLiaisonClub(Statut.CODE_EXPIRE, null);
    }

    public static ResultatLiaisonClub codeIncorrect() {
        return new ResultatLiaisonClub(Statut.CODE_INCORRECT, null);
    }

    public Statut getStatut() {
        return statut;
    }

    public boolean estReussi() {
        return statut == Statut.VERIFIE;
    }

    /** @return l'accès créé (uniquement si {@link #estReussi()}), sinon {@code null}. */
    public AccesClub getAcces() {
        return acces;
    }
}
