package com.pingmanager.core;

import java.util.Objects;

/**
 * Compte rendu d'une saisie de résultat de match par {@link SaisieResultatMatch} :
 * enregistrée, ou refusée avec un motif précis (et, pour une manche invalide, son
 * numéro). Objet valeur immuable.
 */
public final class ResultatSaisieMatch {

    /** Issue de la saisie. */
    public enum Motif {
        /** Résultat enregistré (ou effacé, pour une correction). */
        ENREGISTRE,
        /** Le tableau n'est pas en phase de poules : les résultats de poule sont figés. */
        POULES_NON_MODIFIABLES,
        /** Le match n'appartient à aucune poule de ce tableau. */
        MATCH_HORS_TABLEAU,
        /** Le match est une exemption : il n'a pas de score. */
        MATCH_EXEMPT,
        /** Aucune manche saisie. */
        AUCUNE_MANCHE,
        /** Une manche ne respecte pas l'article 2.11.1 (11 points, 2 d'écart après 10-10). */
        MANCHE_INVALIDE,
        /** Plus de manches que le format n'en permet (2.12.1 : au meilleur de 5 ou 7…). */
        TROP_DE_MANCHES,
        /** Une manche a été saisie alors que le match était déjà gagné. */
        MANCHE_APRES_DECISION,
        /** Aucun joueur n'a atteint le nombre de manches gagnantes. */
        MATCH_NON_DECIDE,
        /** Une issue « normale » ne se déclare pas : saisir les manches. */
        ISSUE_NORMALE,
        /** Le joueur désigné comme fautif ne joue pas ce match. */
        FAUTIF_HORS_MATCH
    }

    private final Motif motif;
    private final int numeroManche;

    private ResultatSaisieMatch(Motif motif, int numeroManche) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.numeroManche = numeroManche;
    }

    static ResultatSaisieMatch enregistre() {
        return new ResultatSaisieMatch(Motif.ENREGISTRE, 0);
    }

    static ResultatSaisieMatch refus(Motif motif) {
        return new ResultatSaisieMatch(motif, 0);
    }

    static ResultatSaisieMatch refusManche(Motif motif, int numeroManche) {
        return new ResultatSaisieMatch(motif, numeroManche);
    }

    public Motif getMotif() {
        return motif;
    }

    public boolean estEnregistre() {
        return motif == Motif.ENREGISTRE;
    }

    /** @return le numéro (à partir de 1) de la manche en cause, ou 0 si sans objet. */
    public int getNumeroManche() {
        return numeroManche;
    }

    @Override
    public String toString() {
        return "ResultatSaisieMatch[" + motif + (numeroManche > 0 ? ", manche " + numeroManche : "") + "]";
    }
}
