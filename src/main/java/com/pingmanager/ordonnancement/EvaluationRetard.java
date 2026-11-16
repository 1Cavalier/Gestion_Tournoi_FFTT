package com.pingmanager.ordonnancement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Photographie du retard d'une journée à un instant (B054), calculée par
 * l'appelant (simulation ou salle du jour J) et lue par {@link PiloteRenfort}.
 *
 * @param maintenant           instant de la mesure
 * @param retardDebutMinutes   plus grand retard d'un tableau dont l'heure de début est passée
 *                             et dont toutes les poules n'ont pas commencé (0 si aucun)
 * @param chargeRestanteMinutes travail restant de la journée, en minutes de table : parties à
 *                             jouer (durée estimée, temps autour du match compris) et reste
 *                             estimé des parties en cours
 * @param finVisee             instant à ne pas dépasser pour la journée ({@code null} : inconnu,
 *                             seul le retard de début compte)
 */
public record EvaluationRetard(LocalDateTime maintenant, long retardDebutMinutes, double chargeRestanteMinutes,
                               LocalDateTime finVisee) {

    public EvaluationRetard {
        Objects.requireNonNull(maintenant, "maintenant");
        retardDebutMinutes = Math.max(0, retardDebutMinutes);
        chargeRestanteMinutes = Math.max(0, chargeRestanteMinutes);
    }

    /**
     * Fin projetée avec {@code tables} tables : maintenant + charge restante ÷ tables.
     */
    public LocalDateTime finProjetee(int tables) {
        long secondes = Math.round(chargeRestanteMinutes * 60 / Math.max(1, tables));
        return maintenant.plusSeconds(secondes);
    }

    /** Retard de fin projeté avec {@code tables} tables, en minutes (négatif : en avance). */
    public long retardFinMinutes(int tables) {
        if (finVisee == null) {
            return Long.MIN_VALUE;
        }
        return Duration.between(finVisee, finProjetee(tables)).toMinutes();
    }

    /**
     * Retard prévu avec les seules tables de compétition : le plus grand du
     * retard de début et du retard de fin projeté, en minutes (négatif : en avance).
     */
    public long retardPrevuMinutes(int tablesCompetition) {
        if (finVisee == null) {
            return retardDebutMinutes;
        }
        long fin = retardFinMinutes(tablesCompetition);
        // Un tableau qui attend de commencer est un retard ; sinon l'avance se lit sur la fin.
        return retardDebutMinutes > 0 ? Math.max(retardDebutMinutes, fin) : fin;
    }

    /**
     * Nombre de tables de renfort juste suffisant pour finir à la fin visée :
     * le plus petit {@code k} tel que la fin projetée avec
     * {@code tablesCompetition + k} tables ne dépasse pas la fin visée, borné à
     * {@code max}. Le maximum si la fin visée est passée ou inconnue, ou si un
     * tableau attend de commencer au-delà du seuil.
     */
    public int tablesNecessaires(int tablesCompetition, int max, int seuilMinutes) {
        if (max <= 0) {
            return 0;
        }
        if (finVisee == null || retardDebutMinutes > seuilMinutes || !finVisee.isAfter(maintenant)) {
            return max;
        }
        double minutesRestantes = Duration.between(maintenant, finVisee).toSeconds() / 60.0;
        int besoin = (int) Math.ceil(chargeRestanteMinutes / minutesRestantes - tablesCompetition - 1e-9);
        return Math.max(0, Math.min(max, besoin));
    }
}
