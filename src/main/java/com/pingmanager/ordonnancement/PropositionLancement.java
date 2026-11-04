package com.pingmanager.ordonnancement;

import java.util.Objects;

/**
 * Proposition faite à l'organisateur de lancer un match donné sur une table
 * donnée, avec un score de priorité et une explication lisible.
 *
 * <p>En mode semi-automatique, l'ordonnanceur produit une liste de propositions
 * classées ; l'organisateur valide (ou ajuste). L'explication rend la
 * suggestion transparente : l'humain comprend pourquoi ce match passe en
 * priorité (attente longue, déblocage d'un autre tableau, vague en cours…).</p>
 */
public final class PropositionLancement {

    private final MatchOrdonnancable match;
    private final int numeroTable;
    private final double score;
    private final String explication;

    public PropositionLancement(MatchOrdonnancable match, int numeroTable,
                                double score, String explication) {
        this.match = Objects.requireNonNull(match, "match");
        this.numeroTable = numeroTable;
        this.score = score;
        this.explication = explication == null ? "" : explication;
    }

    public MatchOrdonnancable getMatch() {
        return match;
    }

    public int getNumeroTable() {
        return numeroTable;
    }

    public double getScore() {
        return score;
    }

    public String getExplication() {
        return explication;
    }

    @Override
    public String toString() {
        return String.format("Table %d ← %s [%.1f] (%s)",
                numeroTable, match.getNomTableau(), score, explication);
    }
}
