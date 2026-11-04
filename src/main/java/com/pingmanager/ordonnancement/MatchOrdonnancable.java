package com.pingmanager.ordonnancement;

import com.pingmanager.model.Match;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Enveloppe un {@link Match} avec le contexte nécessaire à l'ordonnancement :
 * le tableau auquel il appartient, sa phase (poule / 1er tour / tour suivant),
 * et l'instant où il est devenu prêt à jouer (pour mesurer l'attente).
 *
 * <p>Le modèle {@link Match} reste indépendant de l'ordonnancement : c'est cette
 * classe, côté service, qui lui associe les informations de pilotage en salle.</p>
 */
public final class MatchOrdonnancable {

    private final Match match;
    private final String identifiantTableau;
    private final String nomTableau;
    private final PhaseMatch phase;

    /**
     * Instant où le match est devenu prêt (ses deux joueurs connus et le match
     * jouable). Sert à mesurer depuis combien de temps il attend une table.
     */
    private LocalDateTime pretDepuis;

    public MatchOrdonnancable(Match match, String identifiantTableau,
                              String nomTableau, PhaseMatch phase) {
        this.match = Objects.requireNonNull(match, "match");
        this.identifiantTableau = Objects.requireNonNull(identifiantTableau, "identifiantTableau");
        this.nomTableau = nomTableau == null ? identifiantTableau : nomTableau;
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    public Match getMatch() {
        return match;
    }

    public String getIdentifiantTableau() {
        return identifiantTableau;
    }

    public String getNomTableau() {
        return nomTableau;
    }

    public PhaseMatch getPhase() {
        return phase;
    }

    public LocalDateTime getPretDepuis() {
        return pretDepuis;
    }

    public void setPretDepuis(LocalDateTime pretDepuis) {
        this.pretDepuis = pretDepuis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchOrdonnancable autre)) return false;
        return match.equals(autre.match);
    }

    @Override
    public int hashCode() {
        return match.hashCode();
    }
}
