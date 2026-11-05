package com.pingmanager.core;

import java.util.Objects;

/**
 * Où en est la phase finale d'un tableau, et pourquoi on ne peut pas (encore)
 * la construire. Produit par {@link ServicePhaseFinale#diagnostiquer}.
 *
 * <p>Le diagnostic reprend les pré-conditions de
 * {@link ServiceLancementTableau#construirePhaseFinale} sous forme de motif
 * précis, avec les chiffres utiles à l'affichage (poules restantes, matchs de
 * poule restants, qualifiés attendus), au lieu d'une exception.</p>
 */
public final class DiagnosticPhaseFinale {

    /** État de la phase finale d'un tableau. */
    public enum Etat {
        /** Le tableau n'a pas de format de compétition : rien à construire. */
        SANS_FORMAT,
        /** Le format du tableau ne prévoit pas de phase à élimination directe. */
        SANS_ELIMINATION,
        /** Les poules ne sont pas encore tirées (inscription ou pointage en cours). */
        POULES_NON_LANCEES,
        /** Des matchs de poule restent à jouer. */
        POULES_EN_COURS,
        /** Les poules sont finies mais donnent moins de deux qualifiés. */
        QUALIFIES_INSUFFISANTS,
        /** Les poules sont terminées : le tableau final peut être tiré. */
        PRETE_A_CONSTRUIRE,
        /** Le tableau final est construit, des matchs restent à jouer. */
        EN_COURS,
        /** Finale (et petite finale s'il y en a une) jouée(s). */
        TERMINEE
    }

    private final Etat etat;
    private final int nombrePoules;
    private final int poulesNonTerminees;
    private final int matchsPouleRestants;
    private final int qualifiesAttendus;

    public DiagnosticPhaseFinale(Etat etat, int nombrePoules, int poulesNonTerminees,
                                 int matchsPouleRestants, int qualifiesAttendus) {
        this.etat = Objects.requireNonNull(etat, "etat");
        this.nombrePoules = nombrePoules;
        this.poulesNonTerminees = poulesNonTerminees;
        this.matchsPouleRestants = matchsPouleRestants;
        this.qualifiesAttendus = qualifiesAttendus;
    }

    public Etat getEtat() {
        return etat;
    }

    public int getNombrePoules() {
        return nombrePoules;
    }

    public int getPoulesNonTerminees() {
        return poulesNonTerminees;
    }

    public int getMatchsPouleRestants() {
        return matchsPouleRestants;
    }

    /** Nombre de qualifiés que les poules envoient dans le tableau final. */
    public int getQualifiesAttendus() {
        return qualifiesAttendus;
    }

    /** @return true si le tableau final peut être construit maintenant. */
    public boolean peutConstruire() {
        return etat == Etat.PRETE_A_CONSTRUIRE;
    }

    /** @return true si le tableau final existe déjà (en cours ou terminé). */
    public boolean estConstruite() {
        return etat == Etat.EN_COURS || etat == Etat.TERMINEE;
    }

    @Override
    public String toString() {
        return "DiagnosticPhaseFinale[" + etat + ", poules=" + nombrePoules
                + ", non terminées=" + poulesNonTerminees
                + ", matchs restants=" + matchsPouleRestants
                + ", qualifiés=" + qualifiesAttendus + "]";
    }
}
