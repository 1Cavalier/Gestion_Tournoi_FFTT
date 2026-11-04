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
        CODE_INCORRECT
    }

    private final Statut statut;
    private final AccesClub acces;

    private ResultatLiaisonClub(Statut statut, AccesClub acces) {
        this.statut = statut;
        this.acces = acces;
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
