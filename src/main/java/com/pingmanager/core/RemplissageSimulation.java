package com.pingmanager.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Taux de remplissage des tableaux pour la simulation (B050) : un taux global
 * (100 %, 95 % ou une autre valeur) et, pour affiner, un taux par tableau
 * <b>qui prime</b> sur le taux global.
 *
 * <p>Pour un tableau limité en places, la simulation joue
 * {@code arrondi(capacité × taux)} joueurs : elle <b>complète</b> avec des
 * joueurs fictifs s'il y a moins d'inscrits, elle <b>réduit</b> en ne gardant que
 * les premiers inscrits s'il y en a plus. Sans taux (ni global ni propre au
 * tableau), ou pour un tableau sans limite de places, les inscrits réels sont
 * joués tels quels. Rien n'est modifié dans le tournoi : ce n'est qu'une
 * hypothèse de prévision.</p>
 *
 * <p>Immuable. Un taux va de 0 à 1 (1 = tableau plein).</p>
 */
public final class RemplissageSimulation {

    /** Tableaux pleins. */
    public static final double TAUX_PLEIN = 1.0;
    /** Hypothèse prudente courante : 95 % des places. */
    public static final double TAUX_95 = 0.95;

    /** Aucun taux : les inscrits réels, sans compléter ni réduire. */
    public static final RemplissageSimulation INSCRITS_REELS = new RemplissageSimulation(null, Map.of());

    private final Double tauxGlobal;
    private final Map<String, Double> parTableau;

    private RemplissageSimulation(Double tauxGlobal, Map<String, Double> parTableau) {
        this.tauxGlobal = tauxGlobal;
        this.parTableau = Collections.unmodifiableMap(new LinkedHashMap<>(parTableau));
    }

    /** Même taux pour tous les tableaux limités en places. */
    public static RemplissageSimulation global(double taux) {
        return new RemplissageSimulation(valide(taux), Map.of());
    }

    /** Copie avec un taux propre au tableau {@code nom} (il prime sur le taux global). */
    public RemplissageSimulation avecTableau(String nom, double taux) {
        Objects.requireNonNull(nom, "nom");
        Map<String, Double> copie = new LinkedHashMap<>(parTableau);
        copie.put(nom, valide(taux));
        return new RemplissageSimulation(tauxGlobal, copie);
    }

    /** Copie sans taux propre au tableau {@code nom} (le taux global s'applique de nouveau). */
    public RemplissageSimulation sansTableau(String nom) {
        Map<String, Double> copie = new LinkedHashMap<>(parTableau);
        copie.remove(nom);
        return new RemplissageSimulation(tauxGlobal, copie);
    }

    /** @return le taux global, s'il y en a un. */
    public OptionalDouble tauxGlobal() {
        return tauxGlobal == null ? OptionalDouble.empty() : OptionalDouble.of(tauxGlobal);
    }

    /** @return les taux propres à un tableau, par nom de tableau. */
    public Map<String, Double> tauxParTableau() {
        return parTableau;
    }

    /** @return le taux appliqué au tableau : le sien s'il en a un, sinon le taux global, sinon rien. */
    public OptionalDouble tauxPour(String nomTableau) {
        Double propre = parTableau.get(nomTableau);
        return propre != null ? OptionalDouble.of(propre) : tauxGlobal();
    }

    /**
     * Nombre de joueurs simulés dans un tableau.
     *
     * @param nomTableau nom du tableau
     * @param capacite   places maximales (0 ou moins : tableau sans limite)
     * @param inscrits   inscrits réels
     * @return {@code arrondi(capacité × taux)}, ou {@code inscrits} sans taux ou sans limite de places
     */
    public int effectif(String nomTableau, int capacite, int inscrits) {
        OptionalDouble taux = tauxPour(nomTableau);
        if (taux.isEmpty() || capacite <= 0) {
            return inscrits;
        }
        return (int) Math.round(capacite * taux.getAsDouble());
    }

    private static double valide(double taux) {
        if (Double.isNaN(taux) || taux < 0 || taux > 1) {
            throw new IllegalArgumentException("Le taux de remplissage doit être entre 0 et 100 %.");
        }
        return taux;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RemplissageSimulation r && Objects.equals(tauxGlobal, r.tauxGlobal)
                && parTableau.equals(r.parTableau);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tauxGlobal, parTableau);
    }
}
