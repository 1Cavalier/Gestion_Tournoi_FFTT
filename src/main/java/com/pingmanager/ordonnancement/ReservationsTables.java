package com.pingmanager.ordonnancement;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Réservations des tables (B024) que l'{@link Ordonnanceur} respecte :
 *
 * <ul>
 *   <li><b>table restreinte</b> (contrainte dure) : elle ne reçoit que les
 *       matchs des tableaux indiqués (table réservée à un tableau, ou table d'une
 *       salle réservée) ;</li>
 *   <li><b>table préférée</b> (préférence) : les matchs d'un tableau y vont en
 *       premier quand elle est libre (« tableau X en salle 2 »), sinon ailleurs.</li>
 * </ul>
 *
 * <p>Les tableaux sont désignés par l'identifiant de tableau des
 * {@link MatchOrdonnancable} (le nom du tableau). Clauses d'organisateur, pas
 * règles fédérales : le juge-arbitre garde la main (3.3.1.2.2). Objet immuable,
 * construit par {@link #builder()}.</p>
 */
public final class ReservationsTables {

    /** Aucune réservation : toutes les tables reçoivent tous les tableaux. */
    public static final ReservationsTables AUCUNE = builder().construire();

    private final Map<Integer, Set<String>> restrictions;
    private final Map<String, Set<Integer>> preferences;

    private ReservationsTables(Map<Integer, Set<String>> restrictions, Map<String, Set<Integer>> preferences) {
        this.restrictions = restrictions;
        this.preferences = preferences;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return vrai si la table peut recevoir un match de ce tableau. */
    public boolean admet(int table, String tableau) {
        Set<String> admis = restrictions.get(table);
        return admis == null || admis.contains(tableau);
    }

    /** @return vrai si la table ne reçoit que certains tableaux. */
    public boolean estRestreinte(int table) {
        return restrictions.containsKey(table);
    }

    /** @return les tableaux admis sur une table restreinte, vide si elle reçoit tout le monde. */
    public Set<String> tableauxAdmis(int table) {
        Set<String> admis = restrictions.get(table);
        return admis == null ? Set.of() : admis;
    }

    /** @return vrai si les matchs de ce tableau vont de préférence sur cette table. */
    public boolean prefere(String tableau, int table) {
        Set<Integer> tables = preferences.get(tableau);
        return tables != null && tables.contains(table);
    }

    /** @return vrai s'il n'y a aucune réservation ni préférence. */
    public boolean estVide() {
        return restrictions.isEmpty() && preferences.isEmpty();
    }

    /**
     * Rang d'une table pour un match d'un tableau : 0 table préférée, 1 table
     * ordinaire, -1 table interdite. L'ordonnanceur prend la table libre de plus
     * petit rang, puis de plus petit numéro.
     */
    int rang(int table, String tableau) {
        if (!admet(table, tableau)) {
            return -1;
        }
        return prefere(tableau, table) ? 0 : 1;
    }

    /** Construction pas à pas. */
    public static final class Builder {
        private final Map<Integer, Set<String>> restrictions = new HashMap<>();
        private final Map<String, Set<Integer>> preferences = new HashMap<>();

        private Builder() {
        }

        /**
         * La table ne reçoit que les matchs de ces tableaux (ajoutés à ceux déjà
         * admis). Les tableaux admis la préfèrent aussi : un tableau qui a des
         * tables à lui commence par elles.
         */
        public Builder restreindre(int table, Set<String> tableaux) {
            Objects.requireNonNull(tableaux, "tableaux");
            if (tableaux.isEmpty()) {
                throw new IllegalArgumentException("Une table restreinte admet au moins un tableau.");
            }
            restrictions.computeIfAbsent(table, k -> new LinkedHashSet<>()).addAll(tableaux);
            tableaux.forEach(t -> preferer(t, table));
            return this;
        }

        /** Les matchs du tableau vont de préférence sur cette table. */
        public Builder preferer(String tableau, int table) {
            Objects.requireNonNull(tableau, "tableau");
            preferences.computeIfAbsent(tableau, k -> new LinkedHashSet<>()).add(table);
            return this;
        }

        public ReservationsTables construire() {
            Map<Integer, Set<String>> r = new HashMap<>();
            restrictions.forEach((k, v) -> r.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v))));
            Map<String, Set<Integer>> p = new HashMap<>();
            preferences.forEach((k, v) -> p.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v))));
            return new ReservationsTables(Collections.unmodifiableMap(r), Collections.unmodifiableMap(p));
        }
    }
}
