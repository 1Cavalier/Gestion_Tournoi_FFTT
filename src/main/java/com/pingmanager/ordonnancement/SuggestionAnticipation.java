package com.pingmanager.ordonnancement;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Suggestion d'anticipation : un match qui n'est pas encore jouable (l'un de ses
 * joueurs est occupé) mais qui le deviendra <b>bientôt</b>, et qu'il est
 * intéressant de préparer pour enchaîner sans temps mort.
 *
 * <p>Porte l'heure estimée à laquelle le match deviendra jouable et une mesure
 * de son intérêt (pouvoir de déblocage), afin que l'organisateur sache lesquels
 * préparer en priorité.</p>
 */
public final class SuggestionAnticipation {

    private final MatchOrdonnancable match;
    private final LocalDateTime jouableVers;
    private final int pouvoirDeblocage;
    private final String explication;

    public SuggestionAnticipation(MatchOrdonnancable match, LocalDateTime jouableVers,
                                  int pouvoirDeblocage, String explication) {
        this.match = Objects.requireNonNull(match, "match");
        this.jouableVers = Objects.requireNonNull(jouableVers, "jouableVers");
        this.pouvoirDeblocage = pouvoirDeblocage;
        this.explication = explication == null ? "" : explication;
    }

    public MatchOrdonnancable getMatch() {
        return match;
    }

    /** Heure estimée à laquelle le match deviendra jouable. */
    public LocalDateTime getJouableVers() {
        return jouableVers;
    }

    public int getPouvoirDeblocage() {
        return pouvoirDeblocage;
    }

    public String getExplication() {
        return explication;
    }

    @Override
    public String toString() {
        return String.format("%s jouable vers %s (%s)",
                match.getNomTableau(), jouableVers.toLocalTime(), explication);
    }
}
