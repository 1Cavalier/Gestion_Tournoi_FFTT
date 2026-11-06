package com.pingmanager.model;

import java.util.Objects;

/**
 * Élément posé sur la grille d'un {@link PlanSalle} (B024) : une table de
 * 2 × 2 cases, une case d'allée (1 × 1) ou un repère (table d'arbitrage,
 * entrée, WC…). La position est celle de la case en haut à gauche, colonnes et
 * lignes numérotées à partir de 0.
 */
public sealed interface ElementPlan permits ElementPlan.TablePlan, ElementPlan.AlleePlan, ElementPlan.ReperePlan {

    int colonne();

    int ligne();

    int largeur();

    int hauteur();

    /** @return le même élément posé ailleurs. */
    ElementPlan placeEn(int colonne, int ligne);

    /** @return vrai si l'élément recouvre la case (colonne, ligne). */
    default boolean couvre(int c, int l) {
        return c >= colonne() && c < colonne() + largeur() && l >= ligne() && l < ligne() + hauteur();
    }

    /** @return vrai si les deux éléments ont au moins une case en commun. */
    default boolean chevauche(ElementPlan autre) {
        return colonne() < autre.colonne() + autre.largeur() && autre.colonne() < colonne() + largeur()
                && ligne() < autre.ligne() + autre.hauteur() && autre.ligne() < ligne() + hauteur();
    }

    /** Libellé court pour les messages (« la table 4 », « l'allée », « l'entrée »). */
    String designation();

    /**
     * Table de compétition : toujours 2 × 2 cases (cahier des charges v0, section 6).
     *
     * @param numero         numéro de la table, celui des matchs et de l'ordonnanceur (≥ 1)
     * @param tableauReserve nom du tableau auquel la table est réservée, ou null
     */
    record TablePlan(int numero, int colonne, int ligne, OrientationTable orientation, String tableauReserve)
            implements ElementPlan {
        public static final int TAILLE = 2;

        public TablePlan {
            orientation = orientation == null ? OrientationTable.HORIZONTALE : orientation;
            tableauReserve = tableauReserve == null || tableauReserve.isBlank() ? null : tableauReserve.trim();
        }

        public TablePlan(int numero, int colonne, int ligne) {
            this(numero, colonne, ligne, OrientationTable.HORIZONTALE, null);
        }

        @Override
        public int largeur() {
            return TAILLE;
        }

        @Override
        public int hauteur() {
            return TAILLE;
        }

        @Override
        public TablePlan placeEn(int c, int l) {
            return new TablePlan(numero, c, l, orientation, tableauReserve);
        }

        public TablePlan numerotee(int nouveauNumero) {
            return new TablePlan(nouveauNumero, colonne, ligne, orientation, tableauReserve);
        }

        public TablePlan orientee(OrientationTable o) {
            return new TablePlan(numero, colonne, ligne, o, tableauReserve);
        }

        public TablePlan reservee(String tableau) {
            return new TablePlan(numero, colonne, ligne, orientation, tableau);
        }

        @Override
        public String designation() {
            return "la table " + numero;
        }
    }

    /** Case d'allée ou de couloir (1 × 1) : rien ne s'y pose. */
    record AlleePlan(int colonne, int ligne) implements ElementPlan {
        @Override
        public int largeur() {
            return 1;
        }

        @Override
        public int hauteur() {
            return 1;
        }

        @Override
        public AlleePlan placeEn(int c, int l) {
            return new AlleePlan(c, l);
        }

        @Override
        public String designation() {
            return "l'allée";
        }
    }

    /**
     * Repère (table d'arbitrage, entrée, WC…), d'une taille libre en cases.
     *
     * @param libelle texte affiché ; le libellé du type s'il est vide
     */
    record ReperePlan(TypeRepere type, String libelle, int colonne, int ligne, int largeur, int hauteur)
            implements ElementPlan {
        public ReperePlan {
            Objects.requireNonNull(type, "type");
            libelle = libelle == null || libelle.isBlank() ? type.getLibelle() : libelle.trim();
        }

        /** Repère à sa taille par défaut. */
        public ReperePlan(TypeRepere type, int colonne, int ligne) {
            this(type, null, colonne, ligne, type.getLargeurParDefaut(), type.getHauteurParDefaut());
        }

        @Override
        public ReperePlan placeEn(int c, int l) {
            return new ReperePlan(type, libelle, c, l, largeur, hauteur);
        }

        @Override
        public String designation() {
            return "le repère « " + libelle + " »";
        }
    }
}
