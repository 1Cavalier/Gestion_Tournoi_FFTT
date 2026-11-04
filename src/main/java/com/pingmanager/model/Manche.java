package com.pingmanager.model;

/**
 * Score d'une manche (set) de tennis de table.
 *
 * <p>Un set se joue en 11 points avec 2 points d'écart. On stocke les points
 * bruts des deux joueurs (ex. 11-9, 13-11, 8-11). La validation de la règle
 * des 11 points / 2 d'écart n'est pas imposée ici afin de rester tolérant à
 * la saisie ; elle pourra être vérifiée par une couche de validation dédiée.</p>
 *
 * @param pointsJoueurA points marqués par le joueur A dans cette manche
 * @param pointsJoueurB points marqués par le joueur B dans cette manche
 */
public record Manche(int pointsJoueurA, int pointsJoueurB) {

    public Manche {
        if (pointsJoueurA < 0 || pointsJoueurB < 0) {
            throw new IllegalArgumentException("Les points d'une manche ne peuvent pas être négatifs.");
        }
    }

    /** @return true si le joueur A a gagné cette manche. */
    public boolean gagneeParA() {
        return pointsJoueurA > pointsJoueurB;
    }

    /** @return true si le joueur B a gagné cette manche. */
    public boolean gagneeParB() {
        return pointsJoueurB > pointsJoueurA;
    }
}
