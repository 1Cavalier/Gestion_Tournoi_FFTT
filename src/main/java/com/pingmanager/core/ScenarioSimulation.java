package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.ordonnancement.EstimateurDuree;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Données d'entrée d'une simulation de tournoi (B025) : les tableaux à jouer,
 * leurs joueurs et leur heure de début, le nombre de tables, la durée estimée
 * des parties et la graine qui rend la simulation reproductible.
 *
 * <p>Immuable ; construit par {@link #builder()} ou depuis un tournoi par
 * {@link SimulateurTournoi#scenarioDepuis}.</p>
 */
public final class ScenarioSimulation {

    /** Heure au-delà de laquelle un tableau sans fin théorique est réputé déborder. */
    public static final LocalTime HEURE_LIMITE_PAR_DEFAUT = LocalTime.of(20, 0);

    /** Durée de départ d'une partie quand rien d'autre n'est connu. */
    public static final Duration DUREE_PARTIE_PAR_DEFAUT = Duration.ofMinutes(20);

    /**
     * Un tableau à simuler.
     *
     * @param nom          nom du tableau (identifiant dans la simulation)
     * @param debut        heure de début des matchs
     * @param joueurs      joueurs présents (une même licence peut figurer dans plusieurs tableaux)
     * @param format       format (taille des poules, qualifiés, phase finale, best-of)
     * @param petiteFinale vrai pour jouer la 3e place
     * @param finSouhaitee heure de fin souhaitée (fin théorique), ou {@code null}
     * @param niveauPoints niveau représentatif (borne haute de points, 0 si inconnu) pour la durée
     */
    public record TableauSimule(String nom, LocalDateTime debut, List<Joueur> joueurs, FormatTournoi format,
                                boolean petiteFinale, LocalDateTime finSouhaitee, int niveauPoints) {
        public TableauSimule {
            Objects.requireNonNull(nom, "nom");
            Objects.requireNonNull(debut, "debut");
            Objects.requireNonNull(format, "format");
            joueurs = List.copyOf(Objects.requireNonNull(joueurs, "joueurs"));
        }
    }

    private final List<TableauSimule> tableaux;
    private final int nombreDeTables;
    private final long graine;
    private final EstimateurDuree estimateur;
    private final double variabilite;
    private final LocalTime heureLimite;

    private ScenarioSimulation(Builder b) {
        this.tableaux = List.copyOf(b.tableaux);
        this.nombreDeTables = b.nombreDeTables;
        this.graine = b.graine;
        this.estimateur = b.estimateur != null ? b.estimateur : new EstimateurDuree(DUREE_PARTIE_PAR_DEFAUT);
        this.variabilite = b.variabilite;
        this.heureLimite = b.heureLimite;
    }

    public List<TableauSimule> getTableaux() {
        return tableaux;
    }

    public int getNombreDeTables() {
        return nombreDeTables;
    }

    public long getGraine() {
        return graine;
    }

    public EstimateurDuree getEstimateur() {
        return estimateur;
    }

    /** Écart relatif maximal de la durée réelle d'une partie autour de l'estimation (0,2 = ±20 %). */
    public double getVariabilite() {
        return variabilite;
    }

    /** Heure limite de la journée pour un tableau sans fin théorique. */
    public LocalTime getHeureLimite() {
        return heureLimite;
    }

    /** Copie avec un autre nombre de tables. */
    public ScenarioSimulation avecTables(int tables) {
        Builder b = copie();
        b.nombreDeTables(tables);
        return b.build();
    }

    /** Copie avec une autre graine. */
    public ScenarioSimulation avecGraine(long nouvelleGraine) {
        Builder b = copie();
        b.graine(nouvelleGraine);
        return b.build();
    }

    private Builder copie() {
        Builder b = builder().nombreDeTables(nombreDeTables).graine(graine).estimateur(estimateur)
                .variabilite(variabilite).heureLimite(heureLimite);
        tableaux.forEach(b::tableau);
        return b;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Construction pas à pas d'un scénario. */
    public static final class Builder {
        private final List<TableauSimule> tableaux = new ArrayList<>();
        private int nombreDeTables = 16;
        private long graine = 1L;
        private EstimateurDuree estimateur;
        private double variabilite = 0.2;
        private LocalTime heureLimite = HEURE_LIMITE_PAR_DEFAUT;

        public Builder tableau(TableauSimule t) {
            tableaux.add(Objects.requireNonNull(t, "t"));
            return this;
        }

        public Builder nombreDeTables(int v) {
            if (v < 1) {
                throw new IllegalArgumentException("Il faut au moins une table.");
            }
            this.nombreDeTables = v;
            return this;
        }

        public Builder graine(long v) {
            this.graine = v;
            return this;
        }

        public Builder estimateur(EstimateurDuree v) {
            this.estimateur = v;
            return this;
        }

        public Builder variabilite(double v) {
            if (v < 0 || v >= 1) {
                throw new IllegalArgumentException("La variabilité doit être dans [0, 1[.");
            }
            this.variabilite = v;
            return this;
        }

        public Builder heureLimite(LocalTime v) {
            this.heureLimite = Objects.requireNonNull(v, "heureLimite");
            return this;
        }

        public ScenarioSimulation build() {
            return new ScenarioSimulation(this);
        }
    }
}
