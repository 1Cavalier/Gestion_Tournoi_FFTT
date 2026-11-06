package com.pingmanager.core;

import com.pingmanager.model.StrategieOrdonnancement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Résultat détaillé d'une simulation de tournoi (B025) pour une stratégie :
 * heure de fin de chaque tableau, attente des joueurs, occupation des tables,
 * tableaux qui débordent.
 *
 * <p>Attente d'un joueur pour une partie : temps entre le moment où il est
 * disponible (début de son tableau, ou fin de sa partie précédente, tous
 * tableaux confondus) et le lancement de la partie. Elle comprend donc le temps
 * passé à attendre un adversaire, une table ou le tableau final.</p>
 *
 * @param strategie         stratégie simulée
 * @param graine            graine de la simulation
 * @param nombreDeTables    tables disponibles
 * @param debut             premier lancement possible (début du premier tableau)
 * @param fin               fin de la dernière partie ({@code null} si rien n'a été joué)
 * @param tableaux          bilan de chaque tableau joué, dans l'ordre du scénario
 * @param ignores           tableaux non simulés, avec le motif
 * @param nombreParties     parties jouées (exempts non comptés)
 * @param attenteMoyenne    attente moyenne des joueurs par partie, en minutes
 * @param attenteMaximale   attente la plus longue, en minutes
 * @param joueurAttenteMax  nom du joueur qui a le plus attendu ({@code null} si aucun)
 * @param occupationTables  part du temps où les tables sont occupées, de 0 à 1 (journée par journée)
 * @param complete          faux si la simulation s'est arrêtée avant la fin (blocage)
 */
public record ResultatSimulation(StrategieOrdonnancement strategie, long graine, int nombreDeTables,
                                 LocalDateTime debut, LocalDateTime fin, List<BilanTableau> tableaux,
                                 List<TableauIgnore> ignores, int nombreParties, double attenteMoyenne,
                                 long attenteMaximale, String joueurAttenteMax, double occupationTables,
                                 boolean complete) {

    public ResultatSimulation {
        Objects.requireNonNull(strategie, "strategie");
        tableaux = List.copyOf(tableaux);
        ignores = List.copyOf(ignores);
    }

    /**
     * Bilan d'un tableau simulé.
     *
     * @param nom             nom du tableau
     * @param debut           heure de début prévue
     * @param fin             fin de sa dernière partie ({@code null} si non terminé)
     * @param finSouhaitee    heure à ne pas dépasser (fin théorique, sinon heure limite de la journée)
     * @param nombreJoueurs   joueurs du tableau
     * @param nombreParties   parties jouées
     * @param attenteMoyenne  attente moyenne de ses joueurs par partie, en minutes
     * @param attenteMaximale attente la plus longue dans ce tableau, en minutes
     */
    public record BilanTableau(String nom, LocalDateTime debut, LocalDateTime fin, LocalDateTime finSouhaitee,
                               int nombreJoueurs, int nombreParties, double attenteMoyenne, long attenteMaximale) {

        /** @return vrai si le tableau finit après l'heure souhaitée (ou n'a pas pu finir). */
        public boolean deborde() {
            return fin == null || (finSouhaitee != null && fin.isAfter(finSouhaitee));
        }

        /** @return minutes de dépassement de l'heure souhaitée (0 si à l'heure). */
        public long minutesDepassement() {
            if (fin == null || finSouhaitee == null || !fin.isAfter(finSouhaitee)) {
                return 0;
            }
            return java.time.Duration.between(finSouhaitee, fin).toMinutes();
        }
    }

    /** Tableau écarté de la simulation (trop peu de joueurs…). */
    public record TableauIgnore(String nom, String motif) {
    }

    /** @return les tableaux qui débordent, dans l'ordre du scénario. */
    public List<BilanTableau> tableauxQuiDebordent() {
        return tableaux.stream().filter(BilanTableau::deborde).toList();
    }

    /** @return le bilan d'un tableau par son nom. */
    public Optional<BilanTableau> tableau(String nom) {
        return tableaux.stream().filter(b -> b.nom().equals(nom)).findFirst();
    }
}
