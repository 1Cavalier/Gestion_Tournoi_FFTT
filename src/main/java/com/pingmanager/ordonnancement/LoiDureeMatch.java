package com.pingmanager.ordonnancement;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

/**
 * Loi de la durée d'un match pour la simulation (B025) : une durée minimale,
 * une durée moyenne et une durée maximale, entre lesquelles la durée réelle est
 * tirée au hasard selon une <b>loi exponentielle décalée et tronquée</b>.
 *
 * <p>Concrètement : aucun match ne dure moins que le minimum ni plus que le
 * maximum ; la plupart durent un peu plus que le minimum, et les matchs longs
 * (cinq manches serrées, litige) sont de plus en plus rares à mesure qu'on
 * s'approche du maximum. Le paramètre de l'exponentielle est calculé pour que
 * la moyenne des durées tirées soit exactement la moyenne demandée, malgré la
 * troncature au maximum.</p>
 *
 * <p>La forme exponentielle impose que la moyenne soit plus proche du minimum
 * que du maximum ({@code minimum < moyenne < (minimum + maximum) / 2}) : c'est
 * ce qui traduit « les matchs longs sont rares ».</p>
 *
 * <p>Immuable. Les durées sont manipulées en minutes (double).</p>
 */
public final class LoiDureeMatch {

    /** Rapports par défaut à la moyenne : minimum = 50 %, maximum = 250 %. */
    public static final double RAPPORT_MINIMUM = 0.5;
    public static final double RAPPORT_MAXIMUM = 2.5;

    private final double moyenne;
    private final double minimum;
    private final double maximum;
    /** Moyenne de l'exponentielle avant troncature (minutes au-delà du minimum). */
    private final double theta;

    /**
     * @param moyenne durée moyenne d'un match
     * @param minimum durée la plus courte possible
     * @param maximum durée la plus longue possible
     * @throws IllegalArgumentException si {@code 0 < minimum < moyenne < (minimum + maximum) / 2} n'est pas respecté
     */
    public LoiDureeMatch(Duration moyenne, Duration minimum, Duration maximum) {
        this(minutes(moyenne, "moyenne"), minutes(minimum, "minimum"), minutes(maximum, "maximum"));
    }

    private LoiDureeMatch(double moyenne, double minimum, double maximum) {
        if (!(minimum > 0)) {
            throw new IllegalArgumentException("La durée minimale doit être positive.");
        }
        if (!(moyenne > minimum)) {
            throw new IllegalArgumentException("La durée moyenne doit dépasser la durée minimale.");
        }
        if (!(maximum > moyenne)) {
            throw new IllegalArgumentException("La durée maximale doit dépasser la durée moyenne.");
        }
        if (!(moyenne < (minimum + maximum) / 2)) {
            throw new IllegalArgumentException("La durée moyenne doit être plus proche du minimum que du "
                    + "maximum (les matchs longs sont rares) : moins de "
                    + Math.round((minimum + maximum) / 2) + " min ici.");
        }
        this.moyenne = moyenne;
        this.minimum = minimum;
        this.maximum = maximum;
        this.theta = calibrer(moyenne - minimum, maximum - minimum);
    }

    /** Loi « autour » d'une moyenne : minimum à 50 %, maximum à 250 %. */
    public static LoiDureeMatch autour(Duration moyenne) {
        double m = minutes(moyenne, "moyenne");
        return new LoiDureeMatch(m, m * RAPPORT_MINIMUM, m * RAPPORT_MAXIMUM);
    }

    /** Même forme, toutes les durées multipliées par {@code facteur} (> 0). */
    public LoiDureeMatch echelle(double facteur) {
        if (!(facteur > 0)) {
            throw new IllegalArgumentException("Le facteur doit être positif.");
        }
        return new LoiDureeMatch(moyenne * facteur, minimum * facteur, maximum * facteur);
    }

    /**
     * Durée au quantile {@code u} (0 ≤ u &lt; 1) : 0 donne le minimum, et la
     * durée croît avec u jusqu'au maximum. Tirer u uniformément donne la loi.
     */
    public double quantileMinutes(double u) {
        if (u < 0 || u >= 1) {
            throw new IllegalArgumentException("u doit être dans [0, 1[.");
        }
        double l = maximum - minimum;
        double x = -theta * Math.log(1 - u * (1 - Math.exp(-l / theta)));
        return Math.min(maximum, minimum + x);
    }

    /** Tire une durée selon la loi. */
    public Duration tirer(Random r) {
        return Duration.ofSeconds(Math.round(quantileMinutes(r.nextDouble()) * 60));
    }

    public Duration getMoyenne() {
        return Duration.ofSeconds(Math.round(moyenne * 60));
    }

    public Duration getMinimum() {
        return Duration.ofSeconds(Math.round(minimum * 60));
    }

    public Duration getMaximum() {
        return Duration.ofSeconds(Math.round(maximum * 60));
    }

    double getMoyenneMinutes() {
        return moyenne;
    }

    /**
     * Moyenne θ de l'exponentielle telle que, tronquée à {@code l}, sa moyenne
     * vaille {@code cible} (0 &lt; cible &lt; l / 2). La moyenne tronquée
     * θ − l·e^(−l/θ) / (1 − e^(−l/θ)) croît de 0 à l / 2 : dichotomie.
     */
    private static double calibrer(double cible, double l) {
        double bas = 1e-9;
        double haut = 1.0;
        while (moyenneTronquee(haut, l) < cible) {
            haut *= 2;
        }
        for (int i = 0; i < 200; i++) {
            double milieu = (bas + haut) / 2;
            if (moyenneTronquee(milieu, l) < cible) {
                bas = milieu;
            } else {
                haut = milieu;
            }
        }
        return (bas + haut) / 2;
    }

    private static double moyenneTronquee(double theta, double l) {
        double e = Math.exp(-l / theta);
        return theta - l * e / (1 - e);
    }

    private static double minutes(Duration d, String nom) {
        Objects.requireNonNull(d, nom);
        return d.toMillis() / 60000.0;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "moyenne %.0f min (de %.0f à %.0f)", moyenne, minimum, maximum);
    }
}
