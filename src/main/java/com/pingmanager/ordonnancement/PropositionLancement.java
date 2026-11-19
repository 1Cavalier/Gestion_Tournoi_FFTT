package com.pingmanager.ordonnancement;

import java.util.List;
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
    /** Poule entière proposée (phase de poules), ou null pour un match seul. */
    private final PouleOrdonnancable poule;

    public PropositionLancement(MatchOrdonnancable match, int numeroTable,
                                double score, String explication) {
        this(match, null, numeroTable, score, explication);
    }

    /**
     * Proposition de lancer une poule entière sur une table : {@link #getMatch()}
     * est alors son premier match à jouer (ordre I.301).
     */
    public PropositionLancement(PouleOrdonnancable poule, int numeroTable, double score, String explication) {
        this(premierAJouer(poule), poule, numeroTable, score, explication);
    }

    private PropositionLancement(MatchOrdonnancable match, PouleOrdonnancable poule, int numeroTable,
                                 double score, String explication) {
        this.match = Objects.requireNonNull(match, "match");
        this.poule = poule;
        this.numeroTable = numeroTable;
        this.score = score;
        this.explication = explication == null ? "" : explication;
    }

    private static MatchOrdonnancable premierAJouer(PouleOrdonnancable poule) {
        Objects.requireNonNull(poule, "poule");
        List<MatchOrdonnancable> a = poule.aJouer();
        if (a.isEmpty()) {
            throw new IllegalArgumentException("La poule n'a plus de match à jouer.");
        }
        return a.get(0);
    }

    /** @return la poule proposée en bloc, ou null pour un match seul. */
    public PouleOrdonnancable getPoule() {
        return poule;
    }

    /** @return true si la proposition porte sur une poule entière. */
    public boolean estPoule() {
        return poule != null;
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
