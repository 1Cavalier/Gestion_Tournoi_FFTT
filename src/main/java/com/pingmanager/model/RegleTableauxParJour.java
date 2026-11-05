package com.pingmanager.model;

import java.util.Objects;

/**
 * Règle, définie au niveau du {@link Tournoi}, limitant le nombre de tableaux
 * auxquels un joueur peut s'inscrire sur une même journée.
 *
 * <p>Un quota de base s'applique à tous les joueurs
 * ({@link #getMaxTableauxParJour()}). Une <b>exception féminine</b> configurable
 * permet d'accorder aux joueuses un tableau supplémentaire, selon deux formes
 * rencontrées en pratique :</p>
 * <ul>
 *   <li>{@link ExceptionFeminine#AUCUNE} : pas de bonus, les joueuses suivent le
 *       quota de base.</li>
 *   <li>{@link ExceptionFeminine#TABLEAU_SUPPLEMENTAIRE_SANS_CONDITION} : une
 *       joueuse peut s'inscrire à un tableau de plus, sans condition.</li>
 *   <li>{@link ExceptionFeminine#TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL} : le
 *       tableau supplémentaire n'est accordé que si la joueuse est inscrite à un
 *       tableau « spécial » désigné (typiquement un tableau réservé aux dames).</li>
 * </ul>
 *
 * <p>Objet immuable. Le quota effectif d'un joueur donné se calcule via
 * {@link #maxPour(Joueur, boolean)}.</p>
 */
public final class RegleTableauxParJour {

    /** Forme de l'exception accordée aux joueuses. */
    public enum ExceptionFeminine {
        AUCUNE,
        TABLEAU_SUPPLEMENTAIRE_SANS_CONDITION,
        TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL
    }

    /** 0 = pas de limite de nombre de tableaux par jour. */
    private final int maxTableauxParJour;
    private final ExceptionFeminine exceptionFeminine;
    private final Tableau tableauSpecial; // requis si TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL

    private RegleTableauxParJour(int maxTableauxParJour,
                                 ExceptionFeminine exceptionFeminine,
                                 Tableau tableauSpecial) {
        this.maxTableauxParJour = maxTableauxParJour;
        this.exceptionFeminine = exceptionFeminine;
        this.tableauSpecial = tableauSpecial;
    }

    /** Règle sans aucune limite (quota illimité, pas d'exception). */
    public static RegleTableauxParJour illimitee() {
        return new RegleTableauxParJour(0, ExceptionFeminine.AUCUNE, null);
    }

    /** Quota de base identique pour tous, sans exception féminine. */
    public static RegleTableauxParJour quota(int maxTableauxParJour) {
        if (maxTableauxParJour < 0) {
            throw new IllegalArgumentException("Le quota ne peut pas être négatif : " + maxTableauxParJour);
        }
        return new RegleTableauxParJour(maxTableauxParJour, ExceptionFeminine.AUCUNE, null);
    }

    /**
     * Quota de base + un tableau supplémentaire pour toutes les joueuses, sans
     * condition.
     */
    public static RegleTableauxParJour quotaAvecBonusFemininSansCondition(int maxTableauxParJour) {
        if (maxTableauxParJour < 0) {
            throw new IllegalArgumentException("Le quota ne peut pas être négatif : " + maxTableauxParJour);
        }
        return new RegleTableauxParJour(maxTableauxParJour,
                ExceptionFeminine.TABLEAU_SUPPLEMENTAIRE_SANS_CONDITION, null);
    }

    /**
     * Quota de base + un tableau supplémentaire pour les joueuses inscrites au
     * tableau spécial désigné.
     */
    public static RegleTableauxParJour quotaAvecBonusFemininSiTableauSpecial(int maxTableauxParJour,
                                                                             Tableau tableauSpecial) {
        if (maxTableauxParJour < 0) {
            throw new IllegalArgumentException("Le quota ne peut pas être négatif : " + maxTableauxParJour);
        }
        Objects.requireNonNull(tableauSpecial, "tableauSpecial");
        return new RegleTableauxParJour(maxTableauxParJour,
                ExceptionFeminine.TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL, tableauSpecial);
    }

    public int getMaxTableauxParJour() {
        return maxTableauxParJour;
    }

    public boolean aLimite() {
        return maxTableauxParJour > 0;
    }

    public ExceptionFeminine getExceptionFeminine() {
        return exceptionFeminine;
    }

    public Tableau getTableauSpecial() {
        return tableauSpecial;
    }

    /**
     * Calcule le nombre maximum de tableaux autorisé pour un joueur donné.
     *
     * @param joueur                le joueur concerné
     * @param inscritTableauSpecial true si le joueur est (ou serait) inscrit au
     *                              tableau spécial ; n'a d'effet que pour
     *                              l'exception conditionnée
     * @return le quota effectif, ou {@link Integer#MAX_VALUE} si aucune limite
     */
    public int maxPour(Joueur joueur, boolean inscritTableauSpecial) {
        Objects.requireNonNull(joueur, "joueur");
        if (!aLimite()) {
            return Integer.MAX_VALUE;
        }
        int max = maxTableauxParJour;
        if (joueur.getSexe() == Sexe.FEMME) {
            switch (exceptionFeminine.name()) {
                case "TABLEAU_SUPPLEMENTAIRE_SANS_CONDITION" -> max += 1;
                case "TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL" -> {
                    if (inscritTableauSpecial) {
                        max += 1;
                    }
                }
                case "AUCUNE" -> { /* pas de bonus */ }
            }
        }
        return max;
    }
}
