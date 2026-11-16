package com.pingmanager.core;

import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ordonnancement.ReglageRenfort;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Réglages d'une prévision de tournoi (B050) : ce que le club peut régler avant
 * de lancer {@link SimulateurTournoi#prevoir}. Ni nombre de tirages ni graine :
 * ils sont fixés par le simulateur ({@link SimulateurTournoi#NOMBRE_DE_TIRAGES},
 * {@link SimulateurTournoi#GRAINE_INTERNE}).
 *
 * <p>La <b>fin visée</b> d'une journée est un instant, jour <b>et</b> heure : elle
 * peut tomber le lendemain (un samedi qui doit être fini à 1 h du matin vise
 * « dimanche 1 h »). Sans réglage, la simulation prend la fin proposée par
 * {@link SimulateurTournoi#finsViseesProposees}.</p>
 *
 * <p>Immuable ; construit par {@link #builder()}.</p>
 */
public final class ReglagesPrevision {

    /** Nombre de tables proposé quand rien n'est connu. */
    public static final int TABLES_PAR_DEFAUT = 16;

    private final int nombreDeTables;
    private final LoiDureeMatch loiDuree;
    private final TempsAnnexesMatch tempsAnnexes;
    private final RemplissageSimulation remplissage;
    private final SortedMap<LocalDate, LocalDateTime> finsVisees;
    private final ReglageRenfort renfort;

    private ReglagesPrevision(Builder b) {
        this.nombreDeTables = b.nombreDeTables;
        this.loiDuree = b.loiDuree != null ? b.loiDuree
                : LoiDureeMatch.autour(ScenarioSimulation.DUREE_PARTIE_PAR_DEFAUT);
        this.tempsAnnexes = b.tempsAnnexes != null ? b.tempsAnnexes : TempsAnnexesMatch.PAR_DEFAUT;
        this.remplissage = b.remplissage;
        this.finsVisees = Collections.unmodifiableSortedMap(new TreeMap<>(b.finsVisees));
        this.renfort = b.renfort;
    }

    /**
     * Tables d'échauffement utilisables en renfort et seuils de déclenchement
     * (B054) ; {@link ReglageRenfort#AUCUN} par défaut.
     */
    public ReglageRenfort getRenfort() {
        return renfort;
    }

    public int getNombreDeTables() {
        return nombreDeTables;
    }

    /** Loi de la durée du jeu d'un match en cinq manches (minimum, moyenne, maximum). */
    public LoiDureeMatch getLoiDuree() {
        return loiDuree;
    }

    /** Temps autour du match (appel, échauffement, pauses, temps morts, feuille). */
    public TempsAnnexesMatch getTempsAnnexes() {
        return tempsAnnexes;
    }

    /** Taux de remplissage des tableaux (global et par tableau). */
    public RemplissageSimulation getRemplissage() {
        return remplissage;
    }

    /** Fins visées réglées, par journée (jour de début des matchs). */
    public Map<LocalDate, LocalDateTime> getFinsVisees() {
        return finsVisees;
    }

    /** @return la fin visée réglée pour la journée, s'il y en a une. */
    public Optional<LocalDateTime> finVisee(LocalDate journee) {
        return Optional.ofNullable(finsVisees.get(journee));
    }

    /**
     * Compose l'instant de fin visée d'une journée à partir de l'heure saisie.
     *
     * @param journee   jour de la journée de compétition
     * @param heure     heure de fin visée
     * @param lendemain vrai si cette heure tombe le lendemain (samedi fini à 1 h = dimanche 1 h)
     */
    public static LocalDateTime instantFinVisee(LocalDate journee, LocalTime heure, boolean lendemain) {
        Objects.requireNonNull(journee, "journee");
        Objects.requireNonNull(heure, "heure");
        return (lendemain ? journee.plusDays(1) : journee).atTime(heure);
    }

    /** @return vrai si l'instant tombe le jour de la journée ou le lendemain (seules fins visées admises). */
    public static boolean finViseeAdmise(LocalDate journee, LocalDateTime instant) {
        if (journee == null || instant == null) {
            return false;
        }
        LocalDate jour = instant.toLocalDate();
        return jour.equals(journee) || jour.equals(journee.plusDays(1));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Réglage pas à pas. */
    public static final class Builder {
        private int nombreDeTables = TABLES_PAR_DEFAUT;
        private LoiDureeMatch loiDuree;
        private TempsAnnexesMatch tempsAnnexes;
        private RemplissageSimulation remplissage = RemplissageSimulation.INSCRITS_REELS;
        private final SortedMap<LocalDate, LocalDateTime> finsVisees = new TreeMap<>();
        private ReglageRenfort renfort = ReglageRenfort.AUCUN;

        /** Renfort par les tables d'échauffement (B054). */
        public Builder renfort(ReglageRenfort v) {
            this.renfort = Objects.requireNonNull(v, "renfort");
            return this;
        }

        public Builder nombreDeTables(int v) {
            if (v < 1) {
                throw new IllegalArgumentException("Il faut au moins une table.");
            }
            this.nombreDeTables = v;
            return this;
        }

        /** Loi des durées ({@code null} : autour de la durée de départ). */
        public Builder loiDuree(LoiDureeMatch v) {
            this.loiDuree = v;
            return this;
        }

        /** Temps autour du match ({@code null} : {@link TempsAnnexesMatch#PAR_DEFAUT}). */
        public Builder tempsAnnexes(TempsAnnexesMatch v) {
            this.tempsAnnexes = v;
            return this;
        }

        public Builder remplissage(RemplissageSimulation v) {
            this.remplissage = Objects.requireNonNull(v, "remplissage");
            return this;
        }

        /**
         * Fin visée d'une journée : un instant, le jour même ou le lendemain.
         *
         * @throws IllegalArgumentException si l'instant tombe avant la journée ou après le lendemain
         */
        public Builder finVisee(LocalDate journee, LocalDateTime instant) {
            Objects.requireNonNull(journee, "journee");
            Objects.requireNonNull(instant, "instant");
            if (!finViseeAdmise(journee, instant)) {
                throw new IllegalArgumentException(
                        "La fin visée d'une journée tombe le jour même ou le lendemain.");
            }
            finsVisees.put(journee, instant);
            return this;
        }

        public ReglagesPrevision build() {
            return new ReglagesPrevision(this);
        }
    }
}
