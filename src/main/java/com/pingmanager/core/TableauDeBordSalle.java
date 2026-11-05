package com.pingmanager.core;

import com.pingmanager.model.Match;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Photographie de la salle à un instant donné, calculée par
 * {@link ServiceSalle#tableauDeBord} : ce que l'écran « Salle » affiche.
 *
 * <ul>
 *   <li>une carte par table (1 à N, N = nombre de tables du tournoi), libre ou
 *       occupée, avec le match en cours et la durée écoulée ;</li>
 *   <li>les matchs prêts (poules et tableau final de tous les tableaux),
 *       jouables tout de suite, triés par la priorité de l'ordonnanceur ;</li>
 *   <li>les matchs bloqués parce qu'un de leurs joueurs est déjà en jeu, avec
 *       le motif (joueur et table) ;</li>
 *   <li>les anomalies : matchs marqués « en cours » sans table valable (table
 *       supprimée, deux matchs sur la même table, joueur sur deux tables), à
 *       régulariser par le juge-arbitre.</li>
 * </ul>
 *
 * <p>Objet immuable et sans JavaFX : il se teste directement.</p>
 */
public final class TableauDeBordSalle {

    /**
     * Un match vu depuis la salle : le match, son tableau, son tour (« Poule B ·
     * tour 2 », « Quarts de finale »), sa phase, et sa priorité pour
     * l'ordonnanceur (0 et explication vide s'il n'est pas jouable).
     */
    public record MatchSalle(Match match, String tableau, String tour, PhaseMatch phase,
                             double priorite, String explication) {
        public MatchSalle {
            Objects.requireNonNull(match, "match");
            Objects.requireNonNull(tableau, "tableau");
            Objects.requireNonNull(tour, "tour");
            Objects.requireNonNull(phase, "phase");
            explication = explication == null ? "" : explication;
        }
    }

    /** Une table de la salle : libre ({@code match} nul) ou occupée, avec la durée écoulée. */
    public record CarteTable(int numero, MatchSalle match, Duration ecoule) {
        public boolean estLibre() {
            return match == null;
        }
    }

    /** Un match à jouer mais bloqué, et pourquoi (en général : joueur déjà en jeu). */
    public record MatchBloque(MatchSalle match, ResultatAffectationTable motif) {
        public MatchBloque {
            Objects.requireNonNull(match, "match");
            Objects.requireNonNull(motif, "motif");
        }
    }

    private final LocalDateTime maintenant;
    private final int nombreDeTables;
    private final List<CarteTable> tables;
    private final List<MatchSalle> prets;
    private final List<MatchBloque> bloques;
    private final List<MatchSalle> anomalies;
    private final int enAttenteDesJoueurs;
    private final Ordonnanceur ordonnanceur;

    TableauDeBordSalle(LocalDateTime maintenant, int nombreDeTables, List<CarteTable> tables,
                       List<MatchSalle> prets, List<MatchBloque> bloques, List<MatchSalle> anomalies,
                       int enAttenteDesJoueurs, Ordonnanceur ordonnanceur) {
        this.maintenant = maintenant;
        this.nombreDeTables = nombreDeTables;
        this.tables = List.copyOf(tables);
        this.prets = List.copyOf(prets);
        this.bloques = List.copyOf(bloques);
        this.anomalies = List.copyOf(anomalies);
        this.enAttenteDesJoueurs = enAttenteDesJoueurs;
        this.ordonnanceur = ordonnanceur;
    }

    /** Instant de la photographie (horloge du service). */
    public LocalDateTime getMaintenant() {
        return maintenant;
    }

    /** Nombre de tables du tournoi (0 si non renseigné : la salle n'a alors aucune carte). */
    public int getNombreDeTables() {
        return nombreDeTables;
    }

    /** Une carte par table, de la table 1 à la table N. */
    public List<CarteTable> getTables() {
        return tables;
    }

    /** Carte d'une table (1 à N), ou null hors limites. */
    public CarteTable table(int numero) {
        return numero < 1 || numero > tables.size() ? null : tables.get(numero - 1);
    }

    /** Matchs jouables tout de suite, du plus prioritaire au moins prioritaire. */
    public List<MatchSalle> getPrets() {
        return prets;
    }

    /** Matchs à jouer dont un joueur est déjà sur une table, avec le motif. */
    public List<MatchBloque> getBloques() {
        return bloques;
    }

    /** Matchs « en cours » sans table valable, à régulariser. */
    public List<MatchSalle> getAnomalies() {
        return anomalies;
    }

    /** Matchs du tableau final dont les joueurs ne sont pas encore connus. */
    public int getEnAttenteDesJoueurs() {
        return enAttenteDesJoueurs;
    }

    public int nombreTablesLibres() {
        return (int) tables.stream().filter(CarteTable::estLibre).count();
    }

    public int nombreTablesOccupees() {
        return tables.size() - nombreTablesLibres();
    }

    /** Numéros des tables libres, croissants. */
    public List<Integer> tablesLibres() {
        return tables.stream().filter(CarteTable::estLibre).map(CarteTable::numero).toList();
    }

    /** Table où se joue un match, ou null s'il n'est sur aucune table. */
    public Integer tableDe(Match m) {
        for (CarteTable c : tables) {
            if (c.match() != null && c.match().match() == m) {
                return c.numero();
            }
        }
        return null;
    }

    /**
     * Ordonnanceur reconstruit pour cette photographie (matchs, occupation des
     * tables, attentes). Sert à alimenter une {@code SessionSalle} pour
     * l'autosave ; ne pas le modifier directement, passer par {@link ServiceSalle}.
     */
    public Ordonnanceur getOrdonnanceur() {
        return ordonnanceur;
    }
}
