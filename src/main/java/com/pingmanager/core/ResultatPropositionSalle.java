package com.pingmanager.core;

import java.util.List;
import java.util.Objects;

/**
 * Résultat du bouton « Proposer » de la salle : les matchs envoyés sur les
 * tables libres, ou la raison pour laquelle aucun ne l'a été.
 */
public final class ResultatPropositionSalle {

    /** Issue de la proposition. */
    public enum Motif {
        /** Au moins un match a été envoyé sur une table. */
        LANCES,
        /** Toutes les tables sont occupées. */
        AUCUNE_TABLE_LIBRE,
        /** Aucun match n'est jouable (rien à jouer, ou joueurs tous déjà en jeu). */
        AUCUN_MATCH_PRET,
        /** Le nombre de tables du tournoi n'est pas renseigné. */
        SALLE_SANS_TABLE,
        /** Le tournoi est annulé. */
        TOURNOI_FERME
    }

    /** Un match envoyé sur une table. */
    public record Lancement(TableauDeBordSalle.MatchSalle match, int table) {
        public Lancement {
            Objects.requireNonNull(match, "match");
        }
    }

    private final Motif motif;
    private final List<Lancement> lancements;

    private ResultatPropositionSalle(Motif motif, List<Lancement> lancements) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.lancements = List.copyOf(lancements);
    }

    static ResultatPropositionSalle lances(List<Lancement> lancements) {
        if (lancements.isEmpty()) {
            throw new IllegalArgumentException("Aucun lancement : utiliser un motif de refus.");
        }
        return new ResultatPropositionSalle(Motif.LANCES, lancements);
    }

    static ResultatPropositionSalle aucun(Motif motif) {
        if (motif == Motif.LANCES) {
            throw new IllegalArgumentException("LANCES exige au moins un lancement.");
        }
        return new ResultatPropositionSalle(motif, List.of());
    }

    public Motif getMotif() {
        return motif;
    }

    public boolean aLance() {
        return motif == Motif.LANCES;
    }

    /** Matchs envoyés, dans l'ordre de priorité (le plus prioritaire en premier). */
    public List<Lancement> getLancements() {
        return lancements;
    }
}
