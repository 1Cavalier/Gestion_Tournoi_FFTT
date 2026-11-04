package com.pingmanager.ordonnancement;

import java.util.Objects;

/**
 * Clé identifiant une catégorie de matchs supposés de durée homogène, pour
 * estimer leur temps : combinaison d'une {@link PhaseMatch} (poule / 1er tour /
 * tour suivant) et d'une <b>tranche de niveau</b> du tableau.
 *
 * <p>La durée d'un match dépend du contexte : une poule ne dure pas comme un
 * match à élimination, et un tableau de haut niveau (échanges serrés, manches à
 * rallonge) prend plus de temps qu'un tableau de niveau modeste. On regroupe
 * donc les matchs par (phase, tranche de niveau) pour estimer et apprendre
 * séparément le temps de chaque catégorie.</p>
 *
 * <p>La tranche de niveau est un entier libre : l'appelant décide de la
 * granularité (par exemple des paliers de 200 points de classement). Deux
 * tableaux de tranche identique partagent la même estimation.</p>
 */
public final class CategorieDuree {

    private final PhaseMatch phase;
    private final int trancheNiveau;

    public CategorieDuree(PhaseMatch phase, int trancheNiveau) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.trancheNiveau = trancheNiveau;
    }

    public PhaseMatch getPhase() {
        return phase;
    }

    public int getTrancheNiveau() {
        return trancheNiveau;
    }

    /**
     * Construit une clé à partir d'une phase et d'un niveau de classement, en
     * regroupant le niveau par paliers de {@code taillePalier} points.
     *
     * @param phase        la phase du match
     * @param niveauPoints niveau de classement représentatif du tableau
     *                     (par exemple sa borne haute), 0 si inconnu
     * @param taillePalier largeur d'un palier de niveau (par ex. 200)
     */
    public static CategorieDuree depuisNiveau(PhaseMatch phase, int niveauPoints, int taillePalier) {
        int tranche = (taillePalier <= 0) ? 0 : (niveauPoints / taillePalier);
        return new CategorieDuree(phase, tranche);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategorieDuree autre)) return false;
        return trancheNiveau == autre.trancheNiveau && phase == autre.phase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(phase, trancheNiveau);
    }

    @Override
    public String toString() {
        return phase + "/niveau" + trancheNiveau;
    }
}
