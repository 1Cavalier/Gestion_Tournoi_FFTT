package com.pingmanager.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Une salle du tournoi (B024) : « Gymnase », « Dojo »… avec son étage (pour la
 * mini-carte des sites à plusieurs salles ou étages), son {@link PlanSalle} et
 * ses réservations.
 *
 * <ul>
 *   <li><b>Salle réservée</b> : si {@link #getTableauxReserves()} n'est pas vide,
 *       ses tables ne reçoivent que les matchs de ces tableaux (sauf une table
 *       qui a sa propre réservation).</li>
 *   <li><b>Préférence</b> : les tableaux de {@link #getTableauxPreferes()} jouent
 *       de préférence dans cette salle ; s'il n'y a pas de table libre, ils
 *       jouent ailleurs.</li>
 * </ul>
 *
 * <p>Ce sont des clauses d'organisateur, pas des règles fédérales : le
 * juge-arbitre programme les parties « en fonction des tables disponibles »
 * (3.3.1.2.2). Les tableaux sont désignés par leur nom. Objet immuable.</p>
 */
public final class SalleTournoi {

    private final String nom;
    private final int etage;
    private final PlanSalle plan;
    private final Set<String> tableauxReserves;
    private final Set<String> tableauxPreferes;

    /**
     * @param nom   nom de la salle (obligatoire)
     * @param etage 0 = rez-de-chaussée, 1 = premier étage, -1 = sous-sol…
     */
    public SalleTournoi(String nom, int etage, PlanSalle plan, Set<String> tableauxReserves,
                        Set<String> tableauxPreferes) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Une salle doit avoir un nom.");
        }
        this.nom = nom.trim();
        this.etage = etage;
        this.plan = Objects.requireNonNull(plan, "plan");
        this.tableauxReserves = nettoyer(tableauxReserves);
        this.tableauxPreferes = nettoyer(tableauxPreferes);
    }

    public SalleTournoi(String nom, PlanSalle plan) {
        this(nom, 0, plan, Set.of(), Set.of());
    }

    private static Set<String> nettoyer(Set<String> noms) {
        Set<String> s = new LinkedHashSet<>();
        if (noms != null) {
            for (String n : noms) {
                if (n != null && !n.isBlank()) {
                    s.add(n.trim());
                }
            }
        }
        return Collections.unmodifiableSet(s);
    }

    public String getNom() {
        return nom;
    }

    public int getEtage() {
        return etage;
    }

    public PlanSalle getPlan() {
        return plan;
    }

    public Set<String> getTableauxReserves() {
        return tableauxReserves;
    }

    public Set<String> getTableauxPreferes() {
        return tableauxPreferes;
    }

    public SalleTournoi avecNom(String nouveauNom) {
        return new SalleTournoi(nouveauNom, etage, plan, tableauxReserves, tableauxPreferes);
    }

    public SalleTournoi avecEtage(int nouvelEtage) {
        return new SalleTournoi(nom, nouvelEtage, plan, tableauxReserves, tableauxPreferes);
    }

    public SalleTournoi avecPlan(PlanSalle nouveauPlan) {
        return new SalleTournoi(nom, etage, nouveauPlan, tableauxReserves, tableauxPreferes);
    }

    public SalleTournoi avecTableauxReserves(Set<String> tableaux) {
        return new SalleTournoi(nom, etage, plan, tableaux, tableauxPreferes);
    }

    public SalleTournoi avecTableauxPreferes(Set<String> tableaux) {
        return new SalleTournoi(nom, etage, plan, tableauxReserves, tableaux);
    }

    /** @return libellé de l'étage (« Rez-de-chaussée », « 1er étage », « Sous-sol 1 »). */
    public static String libelleEtage(int etage) {
        if (etage == 0) {
            return "Rez-de-chaussée";
        }
        if (etage < 0) {
            return "Sous-sol " + (-etage);
        }
        return etage == 1 ? "1er étage" : etage + "e étage";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SalleTournoi s && s.nom.equals(nom) && s.etage == etage && s.plan.equals(plan)
                && s.tableauxReserves.equals(tableauxReserves) && s.tableauxPreferes.equals(tableauxPreferes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom, etage, plan, tableauxReserves, tableauxPreferes);
    }

    @Override
    public String toString() {
        return "SalleTournoi[" + nom + ", " + plan + "]";
    }
}
