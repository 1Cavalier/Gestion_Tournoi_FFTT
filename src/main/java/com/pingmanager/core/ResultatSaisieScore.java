package com.pingmanager.core;

import java.util.Objects;

/**
 * Résultat de la saisie du résultat d'un match (score ou issue non normale),
 * produit par {@link ValidationScoreMatch} et {@link ServicePhaseFinale}.
 *
 * <p>Plutôt qu'un booléen, le résultat porte un <b>motif précis</b> et, pour
 * les refus qui concernent une manche, le <b>numéro de la manche</b> fautive
 * (à partir de 1) : l'interface peut ainsi dire exactement quoi corriger.</p>
 *
 * <p>Objet valeur immuable, obtenu via les fabriques statiques.</p>
 */
public final class ResultatSaisieScore {

    /** Motif de l'issue d'une saisie de résultat. */
    public enum Motif {
        /** Résultat conforme (et enregistré lorsqu'il vient du service). */
        VALIDE,
        /** Aucune manche saisie. */
        AUCUNE_MANCHE,
        /** Une manche se termine à égalité de points (2.11.1). */
        MANCHE_A_EGALITE,
        /** Le vainqueur d'une manche n'a pas atteint 11 points (2.11.1). */
        MANCHE_SOUS_ONZE_POINTS,
        /** Manche gagnée 11-10 : il faut deux points d'écart (2.11.1). */
        MANCHE_ECART_INSUFFISANT,
        /**
         * Au-delà de 11 points (après 10-10), la manche s'arrête dès que l'écart
         * atteint deux points : 12-10, 13-11… (2.11.1).
         */
        MANCHE_PROLONGATION_INCORRECTE,
        /** Une manche est saisie alors que le match était déjà gagné. */
        MANCHE_EN_TROP,
        /** Aucun joueur n'a atteint le nombre de manches gagnantes (2.12.1). */
        MATCH_NON_DECIDE,
        /** Le match n'appartient pas à la phase finale de ce tableau. */
        MATCH_HORS_TABLEAU,
        /** Les deux joueurs du match ne sont pas encore connus. */
        JOUEURS_NON_CONNUS,
        /**
         * Correction refusée : elle changerait le vainqueur (ou le perdant) alors
         * que le match suivant qui l'attend est déjà en cours ou joué.
         */
        SUITE_DEJA_JOUEE,
        /** Issue non normale incohérente (issue « normale », fautif hors du match). */
        ISSUE_INVALIDE
    }

    private final Motif motif;
    private final int numeroManche;

    private ResultatSaisieScore(Motif motif, int numeroManche) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.numeroManche = numeroManche;
    }

    public static ResultatSaisieScore valide() {
        return new ResultatSaisieScore(Motif.VALIDE, 0);
    }

    /** Refus qui ne concerne pas une manche en particulier. */
    public static ResultatSaisieScore refus(Motif motif) {
        if (motif == Motif.VALIDE) {
            throw new IllegalArgumentException("Un refus ne peut pas avoir le motif VALIDE.");
        }
        return new ResultatSaisieScore(motif, 0);
    }

    /** Refus portant sur une manche (numérotée à partir de 1). */
    public static ResultatSaisieScore refusManche(Motif motif, int numeroManche) {
        if (numeroManche < 1) {
            throw new IllegalArgumentException("Le numéro de manche commence à 1.");
        }
        if (motif == Motif.VALIDE) {
            throw new IllegalArgumentException("Un refus ne peut pas avoir le motif VALIDE.");
        }
        return new ResultatSaisieScore(motif, numeroManche);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return le numéro (à partir de 1) de la manche en cause, ou 0 si sans objet. */
    public int getNumeroManche() {
        return numeroManche;
    }

    public boolean estValide() {
        return motif == Motif.VALIDE;
    }

    @Override
    public String toString() {
        return numeroManche > 0
                ? "ResultatSaisieScore[" + motif + ", manche " + numeroManche + "]"
                : "ResultatSaisieScore[" + motif + "]";
    }
}
