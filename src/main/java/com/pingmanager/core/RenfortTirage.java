package com.pingmanager.core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Usage des tables de renfort pendant un tirage de simulation (B054).
 *
 * <p><b>Utilisation</b> : temps de jeu (temps autour du match compris) sur les
 * tables de renfort, rapporté à ce qu'elles offriraient si les {@code tablesMax}
 * tables servaient de la première partie à la dernière fin de la journée.</p>
 *
 * @param tablesMax          tables de renfort permises (0 : pas de renfort)
 * @param minutesDeJeu       minutes de jeu sur les tables de renfort
 * @param utilisation        part d'utilisation, de 0 à 1
 * @param premiereMiseEnService première ouverture d'une table de renfort ({@code null} : jamais)
 * @param tablesEnServiceMax plus grand nombre de tables de renfort ouvertes en même temps
 * @param plages             plages où au moins une table de renfort est ouverte (ou occupée)
 */
public record RenfortTirage(int tablesMax, double minutesDeJeu, double utilisation,
                            LocalDateTime premiereMiseEnService, int tablesEnServiceMax, List<Plage> plages) {

    /** Pas de renfort. */
    public static final RenfortTirage AUCUN = new RenfortTirage(0, 0, 0, null, 0, List.of());

    public RenfortTirage {
        plages = List.copyOf(plages);
    }

    /** @return vrai si au moins une table de renfort a été ouverte. */
    public boolean deploye() {
        return premiereMiseEnService != null;
    }

    /**
     * Plage horaire de renfort : de l'ouverture à la fin de la dernière partie
     * jouée sur les tables de renfort après leur retour à l'échauffement.
     *
     * @param debut       ouverture
     * @param fin         tables rendues (dernière partie finie)
     * @param tablesMax   plus grand nombre de tables ouvertes pendant la plage
     */
    public record Plage(LocalDateTime debut, LocalDateTime fin, int tablesMax) {
        public Plage {
            Objects.requireNonNull(debut, "debut");
            Objects.requireNonNull(fin, "fin");
        }

        public long minutes() {
            return Duration.between(debut, fin).toMinutes();
        }
    }
}
