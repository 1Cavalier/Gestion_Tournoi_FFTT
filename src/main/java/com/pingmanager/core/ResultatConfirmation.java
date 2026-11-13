package com.pingmanager.core;

import com.pingmanager.model.AccesClub;

/**
 * Résultat d'une tentative de confirmation du code de vérification, passée par
 * le {@link ServiceComptesOrganisateurs}.
 *
 * <p>En cas de succès, le compte a été marqué « email vérifié » et un
 * {@link AccesClub} a été créé (le droit d'administrer le club). En cas d'échec,
 * le motif précise la raison (code faux ou expiré) et aucun accès n'est produit.</p>
 *
 * <p>Objet valeur immuable, obtenu via les fabriques statiques.</p>
 */
public final class ResultatConfirmation {

    /** Motif de l'issue d'une confirmation. */
    public enum Motif {
        /** Succès : email vérifié et accès au club créé. */
        VERIFIE,
        /** Refus : le code saisi ne correspond pas. */
        CODE_INCORRECT,
        /** Refus : le délai de validité du code est dépassé. */
        CODE_EXPIRE,
        /** Refus : trop de codes faux récents, essai refusé sans vérification (point 15). */
        TROP_D_ESSAIS
    }

    private final Motif motif;
    private final AccesClub acces;
    private final java.time.Duration attente;

    private ResultatConfirmation(Motif motif, AccesClub acces) {
        this(motif, acces, java.time.Duration.ZERO);
    }

    private ResultatConfirmation(Motif motif, AccesClub acces, java.time.Duration attente) {
        this.motif = motif;
        this.acces = acces;
        this.attente = attente;
    }

    public static ResultatConfirmation tropDEssais(java.time.Duration attente) {
        return new ResultatConfirmation(Motif.TROP_D_ESSAIS, null, attente);
    }

    /** @return l'attente avant un nouvel essai (motif {@link Motif#TROP_D_ESSAIS}), sinon zéro. */
    public java.time.Duration getAttente() {
        return attente;
    }

    public static ResultatConfirmation verifie(AccesClub acces) {
        return new ResultatConfirmation(Motif.VERIFIE, acces);
    }

    public static ResultatConfirmation codeIncorrect() {
        return new ResultatConfirmation(Motif.CODE_INCORRECT, null);
    }

    public static ResultatConfirmation codeExpire() {
        return new ResultatConfirmation(Motif.CODE_EXPIRE, null);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return l'accès créé (motif {@link Motif#VERIFIE}), ou {@code null}. */
    public AccesClub getAcces() {
        return acces;
    }

    /** @return true si la vérification a réussi. */
    public boolean estVerifie() {
        return motif == Motif.VERIFIE;
    }

    @Override
    public String toString() {
        return "ResultatConfirmation[" + motif + "]";
    }
}
