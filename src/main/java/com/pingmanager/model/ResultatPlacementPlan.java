package com.pingmanager.model;

import java.util.Objects;

/**
 * Résultat détaillé d'une modification du plan d'une salle (B024) : poser,
 * déplacer, retirer, numéroter, orienter un élément, ou changer la taille de la
 * grille. Un refus porte un {@link Motif} précis et, s'il y a lieu, l'élément
 * qui gêne ; le plan rendu est alors le plan d'origine, inchangé.
 */
public final class ResultatPlacementPlan {

    /** Issue de l'opération. Les cinq premiers motifs sont des réussites. */
    public enum Motif {
        /** L'élément est posé sur le plan. */
        PLACE,
        /** L'élément est déplacé. */
        DEPLACE,
        /** L'élément est retiré du plan. */
        RETIRE,
        /** L'élément est modifié (numéro, orientation, réservation, taille). */
        MODIFIE,
        /** La grille a changé de taille. */
        REDIMENSIONNE,
        /** L'élément dépasserait du plan. */
        HORS_GRILLE,
        /** L'élément recouvrirait un autre élément. */
        CHEVAUCHEMENT,
        /** Le numéro de table doit être un entier positif. */
        NUMERO_INVALIDE,
        /** Une autre table porte déjà ce numéro (dans cette salle ou une autre). */
        NUMERO_DEJA_PRIS,
        /** Aucun élément à cet endroit, ou aucune table de ce numéro. */
        ELEMENT_INTROUVABLE,
        /** Largeur ou hauteur nulle, négative ou trop grande. */
        DIMENSIONS_INVALIDES,
        /** La nouvelle grille couperait un élément déjà posé. */
        GRILLE_TROP_PETITE
    }

    private final Motif motif;
    private final PlanSalle plan;
    private final ElementPlan conflit;

    private ResultatPlacementPlan(Motif motif, PlanSalle plan, ElementPlan conflit) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.conflit = conflit;
    }

    public static ResultatPlacementPlan reussite(Motif motif, PlanSalle nouveauPlan) {
        return new ResultatPlacementPlan(motif, nouveauPlan, null);
    }

    /** Refus : le plan d'origine est rendu tel quel. */
    public static ResultatPlacementPlan refus(Motif motif, PlanSalle planOrigine, ElementPlan conflit) {
        return new ResultatPlacementPlan(motif, planOrigine, conflit);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return vrai si le plan a été modifié. */
    public boolean estReussi() {
        return motif == Motif.PLACE || motif == Motif.DEPLACE || motif == Motif.RETIRE
                || motif == Motif.MODIFIE || motif == Motif.REDIMENSIONNE;
    }

    /** @return le nouveau plan en cas de réussite, sinon le plan d'origine inchangé. */
    public PlanSalle getPlan() {
        return plan;
    }

    /** @return l'élément qui gêne (chevauchement, numéro déjà pris, grille trop petite), sinon null. */
    public ElementPlan getConflit() {
        return conflit;
    }

    @Override
    public String toString() {
        return motif + (conflit == null ? "" : " (" + conflit.designation() + ")");
    }
}
