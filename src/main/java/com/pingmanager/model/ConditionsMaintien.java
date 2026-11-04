package com.pingmanager.model;

import java.time.LocalDate;

/**
 * Maintien des tableaux et des dotations : un tableau trop peu rempli à une
 * date donnée peut être annulé ou regroupé ; la dotation peut être réduite
 * selon le remplissage du tableau ou le nombre d'absents.
 */
public final class ConditionsMaintien {

    /** Base de la réduction de dotation. */
    public enum ModeReduction {
        /** Moins de {@code seuilRemplissagePct} % de places occupées : dotation réduite de {@code reductionPct} %. */
        REMPLISSAGE,
        /** Par tranche de {@code trancheAbsents} absents : dotation réduite de {@code reductionPct} %. */
        ABSENTS
    }

    private final int minimumInscrits;
    private final LocalDate dateDecision;
    private final ModeReduction reduction;
    private final int seuilRemplissagePct;
    private final int trancheAbsents;
    private final int reductionPct;

    public ConditionsMaintien(int minimumInscrits, LocalDate dateDecision, ModeReduction reduction,
                              int seuilRemplissagePct, int trancheAbsents, int reductionPct) {
        if (minimumInscrits < 0 || seuilRemplissagePct < 0 || seuilRemplissagePct > 100
                || trancheAbsents < 0 || reductionPct < 0 || reductionPct > 100) {
            throw new IllegalArgumentException("Valeurs de maintien invalides (pourcentages entre 0 et 100).");
        }
        // Selon le remplissage, la réduction peut rester non chiffrée (0) : « peut être réduite ».
        if (reduction == ModeReduction.REMPLISSAGE && seuilRemplissagePct == 0) {
            throw new IllegalArgumentException("Réduction selon le remplissage : indiquez le seuil de remplissage.");
        }
        if (reduction == ModeReduction.ABSENTS && (trancheAbsents == 0 || reductionPct == 0)) {
            throw new IllegalArgumentException("Réduction selon les absents : indiquez la tranche d'absents et la réduction.");
        }
        this.minimumInscrits = minimumInscrits;
        this.dateDecision = minimumInscrits == 0 ? null : dateDecision;
        this.reduction = reduction;
        this.seuilRemplissagePct = reduction == ModeReduction.REMPLISSAGE ? seuilRemplissagePct : 0;
        this.trancheAbsents = reduction == ModeReduction.ABSENTS ? trancheAbsents : 0;
        this.reductionPct = reduction == null ? 0 : reductionPct;
    }

    public static ConditionsMaintien aucune() {
        return new ConditionsMaintien(0, null, null, 0, 0, 0);
    }

    /** 0 = pas de clause d'annulation. */
    public int getMinimumInscrits() {
        return minimumInscrits;
    }

    /** Date à laquelle le nombre d'inscrits est apprécié (facultative). */
    public LocalDate getDateDecision() {
        return dateDecision;
    }

    /** {@code null} = dotation toujours versée en entier. */
    public ModeReduction getReduction() {
        return reduction;
    }

    public int getSeuilRemplissagePct() {
        return seuilRemplissagePct;
    }

    public int getTrancheAbsents() {
        return trancheAbsents;
    }

    public int getReductionPct() {
        return reductionPct;
    }
}
