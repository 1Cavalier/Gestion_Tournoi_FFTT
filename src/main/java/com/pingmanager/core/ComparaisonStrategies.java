package com.pingmanager.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Comparaison de deux stratégies simulées sur le même scénario et la même
 * graine (B025) : mêmes poules, mêmes résultats et mêmes durées de parties,
 * seul l'ordre de lancement change.
 *
 * @param premiere la première stratégie simulée
 * @param seconde  la seconde
 */
public record ComparaisonStrategies(ResultatSimulation premiere, ResultatSimulation seconde) {

    public ComparaisonStrategies {
        Objects.requireNonNull(premiere, "premiere");
        Objects.requireNonNull(seconde, "seconde");
    }

    /**
     * @return minutes de fin de tournoi gagnées par la seconde stratégie sur la
     *         première (négatif si elle finit plus tard)
     */
    public long minutesFinGagneesParLaSeconde() {
        if (premiere.fin() == null || seconde.fin() == null) {
            return 0;
        }
        return Duration.between(seconde.fin(), premiere.fin()).toMinutes();
    }

    /**
     * @return minutes d'attente moyenne gagnées par la seconde stratégie
     *         (négatif si ses joueurs attendent plus)
     */
    public double attenteMoyenneGagneeParLaSeconde() {
        return premiere.attenteMoyenne() - seconde.attenteMoyenne();
    }
}
