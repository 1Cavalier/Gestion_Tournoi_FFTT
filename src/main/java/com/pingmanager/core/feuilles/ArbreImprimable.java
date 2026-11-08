package com.pingmanager.core.feuilles;

import java.util.List;
import java.util.Objects;

/**
 * Contenu du <b>tableau final imprimable</b> (arbre des rencontres) pour
 * l'affichage en salle : une colonne par tour, du premier tour à gauche à la
 * finale à droite, chaque colonne découpée en autant de places que l'arbre en
 * compte à ce tour (ce qui garde l'alignement : la case d'un match est à
 * mi-hauteur des deux cases qui l'alimentent). Les résultats déjà saisis sont
 * imprimés ; les places inconnues restent « À déterminer ».
 *
 * @param entete       tournoi, date, lieu, tableau
 * @param resume       « 10 qualifiés · tableau de 16 · 6 exempts au premier tour »
 * @param colonnes     tours, du premier à la finale
 * @param petiteFinale case de la petite finale (3e place), ou {@code null}
 */
public record ArbreImprimable(EnTeteFeuille entete, String resume, List<ColonneArbre> colonnes,
                              CaseArbre petiteFinale) {

    /** Titre imprimé en haut de la feuille. */
    public static final String TITRE = "Tableau final";
    /** Libellé de la case de petite finale. */
    public static final String TITRE_PETITE_FINALE = "Petite finale (3e place)";

    public ArbreImprimable {
        Objects.requireNonNull(entete, "entete");
        resume = resume == null ? "" : resume;
        colonnes = List.copyOf(colonnes);
    }

    /** Nombre de places de la première colonne (cadre du tableau ÷ 2), 0 si vide. */
    public int casesPremierTour() {
        return colonnes.isEmpty() ? 0 : colonnes.get(0).cases().size();
    }

    /** Un tour de l'arbre. */
    public record ColonneArbre(String libelle, List<CaseArbre> cases) {
        public ColonneArbre {
            libelle = libelle == null ? "" : libelle;
            cases = List.copyOf(cases);
        }
    }

    /** Nature d'une case. Pas de {@code switch} sur cette énumération. */
    public enum TypeCase {
        /** Un match (joué, en cours ou à jouer). */
        MATCH,
        /** Un joueur qui passe ce tour sans jouer (exempt). */
        EXEMPT,
        /** Place vide (deux exempts face à face) : garde l'alignement. */
        VIDE
    }

    /**
     * Une case de l'arbre.
     *
     * @param type       match, exempt ou vide
     * @param joueurA    premier joueur (ou l'exempt)
     * @param joueurB    second joueur (place vide pour un exempt)
     * @param scoreA     manches gagnées par A (« 3 »), vide si non joué ou issue non normale
     * @param scoreB     manches gagnées par B
     * @param vainqueurA A a gagné
     * @param vainqueurB B a gagné
     * @param mention    « Table 4 », « Forfait », « Abandon »… ou vide
     */
    public record CaseArbre(TypeCase type, JoueurFeuille joueurA, JoueurFeuille joueurB, String scoreA,
                            String scoreB, boolean vainqueurA, boolean vainqueurB, String mention) {
        public CaseArbre {
            Objects.requireNonNull(type, "type");
            joueurA = joueurA == null ? JoueurFeuille.de(null) : joueurA;
            joueurB = joueurB == null ? JoueurFeuille.de(null) : joueurB;
            scoreA = scoreA == null ? "" : scoreA;
            scoreB = scoreB == null ? "" : scoreB;
            mention = mention == null ? "" : mention;
        }

        static CaseArbre vide() {
            return new CaseArbre(TypeCase.VIDE, null, null, "", "", false, false, "");
        }

        static CaseArbre exempt(JoueurFeuille j) {
            return new CaseArbre(TypeCase.EXEMPT, j, null, "", "", false, false, "exempt");
        }
    }
}
