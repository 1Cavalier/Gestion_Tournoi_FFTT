package com.pingmanager.core;

import java.time.Instant;

/**
 * Port : source de temps.
 *
 * <p>Abstraire l'horloge permet au cœur de raisonner sur des durées (expiration
 * d'un code de vérification, par exemple) sans appeler directement
 * {@link Instant#now()}. Les tests injectent une horloge fixe ou contrôlable,
 * ce qui rend le comportement temporel reproductible.</p>
 */
@FunctionalInterface
public interface Horloge {

    /** @return l'instant courant. */
    Instant maintenant();

    /** @return une horloge s'appuyant sur l'horloge système. */
    static Horloge systeme() {
        return Instant::now;
    }
}
