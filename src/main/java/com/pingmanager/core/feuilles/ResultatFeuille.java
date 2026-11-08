package com.pingmanager.core.feuilles;

import java.util.Objects;

/**
 * Résultat de la préparation d'une feuille : la feuille, ou le motif précis du
 * refus.
 *
 * @param feuille la feuille préparée, ou {@code null} si refusée
 * @param motif   le motif du refus, ou {@code null} si acceptée
 * @param <T>     type de feuille
 */
public record ResultatFeuille<T>(T feuille, MotifRefusFeuille motif) {

    public ResultatFeuille {
        if ((feuille == null) == (motif == null)) {
            throw new IllegalArgumentException("Une feuille ou un motif, exactement l'un des deux.");
        }
    }

    public static <T> ResultatFeuille<T> prete(T feuille) {
        return new ResultatFeuille<>(Objects.requireNonNull(feuille, "feuille"), null);
    }

    public static <T> ResultatFeuille<T> refusee(MotifRefusFeuille motif) {
        return new ResultatFeuille<>(null, Objects.requireNonNull(motif, "motif"));
    }

    /** La feuille est-elle prête à imprimer ? */
    public boolean estPrete() {
        return feuille != null;
    }

    /** Explication du refus, ou chaîne vide. */
    public String message() {
        return motif == null ? "" : motif.getLibelle();
    }
}
