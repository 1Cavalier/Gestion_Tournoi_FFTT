package com.pingmanager.core;

import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.TypeRepere;


/**
 * Plans types de salle (B024) : les tables en une, deux, trois ou quatre
 * rangées, séparées par des allées, avec la table d'arbitrage et l'entrée en
 * haut du plan. Point de départ que le club ajuste ensuite dans l'éditeur.
 *
 * <p>Disposition : ligne 0 pour les repères, ligne 1 d'allée, puis chaque
 * rangée de tables (2 cases de haut) suivie d'une ligne d'allée ; une colonne
 * vide entre deux tables. Les tables sont numérotées rangée par rangée, de
 * gauche à droite, à partir du premier numéro demandé.</p>
 */
public final class PlansTypesSalle {

    /** Plans types proposés à l'écran. Pas de {@code switch} sur cette énumération. */
    public enum PlanType {
        UNE_RANGEE("1 rangée", 1),
        DEUX_RANGEES("2 rangées", 2),
        TROIS_RANGEES("3 rangées", 3),
        QUATRE_RANGEES("4 rangées", 4);

        private final String libelle;
        private final int rangees;

        PlanType(String libelle, int rangees) {
            this.libelle = libelle;
            this.rangees = rangees;
        }

        public String getLibelle() {
            return libelle;
        }

        public int getRangees() {
            return rangees;
        }
    }

    private PlansTypesSalle() {
    }

    /** Plan type pour {@code nombreDeTables} tables numérotées à partir de 1. */
    public static PlanSalle generer(PlanType type, int nombreDeTables) {
        return generer(type.getRangees(), nombreDeTables, 1);
    }

    /**
     * Tables en {@code rangees} rangées (le nombre de rangées est ramené au nombre
     * de tables s'il le dépasse), numérotées à partir de {@code premierNumero}.
     * Sans table : un plan vide de 8 × 6 cases avec ses repères.
     *
     * @throws IllegalArgumentException si les tables ne tiennent pas dans la grille maximale
     */
    public static PlanSalle generer(int rangees, int nombreDeTables, int premierNumero) {
        if (rangees < 1 || nombreDeTables < 0 || premierNumero < 1) {
            throw new IllegalArgumentException("Plan type impossible : " + rangees + " rangées, "
                    + nombreDeTables + " tables à partir du numéro " + premierNumero + ".");
        }
        int nbRangees = Math.max(1, Math.min(rangees, nombreDeTables));
        int parRangee = nombreDeTables == 0 ? 0 : (nombreDeTables + nbRangees - 1) / nbRangees;
        int colonnes = Math.max(8, 3 * parRangee + 1);
        int lignes = Math.max(6, 2 + 3 * nbRangees);
        if (colonnes > PlanSalle.COTE_MAXIMAL || lignes > PlanSalle.COTE_MAXIMAL) {
            throw new IllegalArgumentException("Trop de tables par rangée pour la grille ("
                    + parRangee + ") : choisissez plus de rangées.");
        }
        PlanSalle plan = PlanSalle.vide(colonnes, lignes);
        plan = plan.ajouter(new ReperePlan(TypeRepere.TABLE_ARBITRAGE, 0, 0)).getPlan();
        plan = plan.ajouter(new ReperePlan(TypeRepere.ENTREE, colonnes - 1, 0)).getPlan();
        if (nombreDeTables > 0) {
            for (int c = 0; c < colonnes; c++) {
                plan = plan.ajouter(new AlleePlan(c, 1)).getPlan();
            }
        }
        int numero = premierNumero;
        for (int r = 0; r < nbRangees && numero < premierNumero + nombreDeTables; r++) {
            int ligne = 2 + 3 * r;
            for (int i = 0; i < parRangee && numero < premierNumero + nombreDeTables; i++) {
                plan = plan.ajouter(new TablePlan(numero++, 1 + 3 * i, ligne)).getPlan();
            }
            for (int c = 0; c < colonnes; c++) {
                plan = plan.ajouter(new AlleePlan(c, ligne + 2)).getPlan();
            }
        }
        return plan;
    }

    /**
     * Plan d'un tournoi qui n'a pas encore de plan (ancien fichier) : ses tables
     * en grille, à peu près carrée (une rangée jusqu'à 4 tables, deux jusqu'à 12…).
     */
    public static PlanSalle grilleParDefaut(int nombreDeTables) {
        int n = Math.max(0, nombreDeTables);
        int rangees = Math.max(1, (int) Math.round(Math.sqrt(n / 2.0)));
        return generer(rangees, n, 1);
    }
}
