package com.pingmanager.core;

import com.pingmanager.model.Manche;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Une partie jouée, telle qu'elle figure dans un export de résultats (B023).
 * Construite avec un {@link Builder}. Les parties « exempt » ne sont jamais
 * exportées : le joueur B est toujours renseigné.
 */
public final class PartieExportee {

    /** Phase du tableau où la partie s'est jouée. */
    public enum Phase {
        POULE,
        TABLEAU
    }

    private final String tableau;
    private final Phase phase;
    private final String etape;
    private final int numeroPartie;
    private final LocalDate date;
    private final LocalTime heure;
    private final Integer table;
    private final JoueurExporte joueurA;
    private final JoueurExporte joueurB;
    private final List<Manche> manches;
    private final String licenceVainqueur;
    private final String issue;
    private final String licenceFautif;

    private PartieExportee(Builder b) {
        this.tableau = Objects.requireNonNull(b.tableau, "tableau");
        this.phase = Objects.requireNonNull(b.phase, "phase");
        this.etape = b.etape == null ? "" : b.etape;
        this.numeroPartie = b.numeroPartie;
        this.date = b.date;
        this.heure = b.heure;
        this.table = b.table;
        this.joueurA = Objects.requireNonNull(b.joueurA, "joueurA");
        this.joueurB = Objects.requireNonNull(b.joueurB, "joueurB");
        this.manches = b.manches == null ? List.of() : List.copyOf(b.manches);
        this.licenceVainqueur = b.licenceVainqueur == null ? "" : b.licenceVainqueur;
        this.issue = b.issue == null ? "" : b.issue;
        this.licenceFautif = b.licenceFautif == null ? "" : b.licenceFautif;
    }

    public String getTableau() {
        return tableau;
    }

    public Phase getPhase() {
        return phase;
    }

    /** @return « Poule 3 », « Quarts de finale »… */
    public String getEtape() {
        return etape;
    }

    /** @return numéro d'ordre de la partie dans le tableau (à partir de 1). */
    public int getNumeroPartie() {
        return numeroPartie;
    }

    /** @return date de la partie, ou {@code null} si inconnue. */
    public LocalDate getDate() {
        return date;
    }

    /** @return heure de la partie, ou {@code null} si inconnue. */
    public LocalTime getHeure() {
        return heure;
    }

    /** @return numéro de table, ou {@code null} si inconnu. */
    public Integer getTable() {
        return table;
    }

    public JoueurExporte getJoueurA() {
        return joueurA;
    }

    public JoueurExporte getJoueurB() {
        return joueurB;
    }

    /** @return les manches, du point de vue du joueur A (vide si partie non jouée : forfait…). */
    public List<Manche> getManches() {
        return manches;
    }

    /** @return nombre de manches gagnées par A. */
    public int manchesA() {
        return (int) manches.stream().filter(Manche::gagneeParA).count();
    }

    /** @return nombre de manches gagnées par B. */
    public int manchesB() {
        return (int) manches.stream().filter(Manche::gagneeParB).count();
    }

    public String getLicenceVainqueur() {
        return licenceVainqueur;
    }

    /** @return nom de l'issue ({@code IssueMatch#name()}). */
    public String getIssue() {
        return issue;
    }

    /** @return licence du joueur fautif, vide si l'issue est normale. */
    public String getLicenceFautif() {
        return licenceFautif;
    }

    /** Constructeur pas à pas d'une {@link PartieExportee}. */
    public static final class Builder {
        private final String tableau;
        private final Phase phase;
        private String etape;
        private int numeroPartie;
        private LocalDate date;
        private LocalTime heure;
        private Integer table;
        private JoueurExporte joueurA;
        private JoueurExporte joueurB;
        private List<Manche> manches;
        private String licenceVainqueur;
        private String issue;
        private String licenceFautif;

        public Builder(String tableau, Phase phase) {
            this.tableau = tableau;
            this.phase = phase;
        }

        public Builder etape(String v) {
            this.etape = v;
            return this;
        }

        public Builder numeroPartie(int v) {
            this.numeroPartie = v;
            return this;
        }

        public Builder moment(LocalDate d, LocalTime h) {
            this.date = d;
            this.heure = h;
            return this;
        }

        public Builder table(Integer v) {
            this.table = v;
            return this;
        }

        public Builder joueurs(JoueurExporte a, JoueurExporte b) {
            this.joueurA = a;
            this.joueurB = b;
            return this;
        }

        public Builder manches(List<Manche> v) {
            this.manches = v;
            return this;
        }

        public Builder resultat(String licenceVainqueur, String issue, String licenceFautif) {
            this.licenceVainqueur = licenceVainqueur;
            this.issue = issue;
            this.licenceFautif = licenceFautif;
            return this;
        }

        public PartieExportee construire() {
            return new PartieExportee(this);
        }
    }
}
