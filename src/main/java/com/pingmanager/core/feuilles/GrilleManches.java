package com.pingmanager.core.feuilles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grille des manches d'une feuille de match, au format provisoire de Quentin
 * (cahier des charges v0, section 9) : <b>3 lignes × 5 colonnes</b>, la
 * première ligne porte « Manche 1 » à « Manche 5 », toutes les autres cases
 * sont vides.
 *
 * <p>Cinq colonnes par défaut : une partie au meilleur des cinq manches en a
 * au plus cinq ; au meilleur des trois, les deux dernières restent vides.
 * 2.12.1 : « Elle se dispute au meilleur des 5 ou des 7 manches » : une partie
 * au meilleur des sept manches a donc sept colonnes ({@link #vide(int)}).</p>
 */
public final class GrilleManches {

    /** Nombre de lignes de la grille, en-tête compris. */
    public static final int LIGNES = 3;
    /** Nombre de colonnes (manches) par défaut, et minimum. */
    public static final int COLONNES = 5;

    private GrilleManches() {
    }

    /** La grille vide : « Manche 1 » … « Manche 5 » puis deux lignes de cases vides. */
    public static List<List<String>> vide() {
        return vide(0);
    }

    /**
     * Grille vide pour une partie en {@code manchesGagnantes} manches gagnantes :
     * {@code max(5, 2 × manchesGagnantes − 1)} colonnes (2.12.1 : meilleur des 5
     * ou des 7 manches ; 7 colonnes pour 4 manches gagnantes).
     */
    public static List<List<String>> vide(int manchesGagnantes) {
        int colonnes = Math.max(COLONNES, 2 * manchesGagnantes - 1);
        List<List<String>> g = new ArrayList<>(LIGNES);
        List<String> entetes = new ArrayList<>(colonnes);
        for (int c = 1; c <= colonnes; c++) {
            entetes.add("Manche " + c);
        }
        g.add(List.copyOf(entetes));
        List<String> cases = Collections.nCopies(colonnes, "");
        for (int l = 1; l < LIGNES; l++) {
            g.add(List.copyOf(cases));
        }
        return List.copyOf(g);
    }

    /** Copie immuable, en vérifiant la forme : 3 lignes de même taille, au moins 5 cases (2.12.1). */
    static List<List<String>> copie(List<List<String>> grille) {
        if (grille == null || grille.size() != LIGNES) {
            throw new IllegalArgumentException("La grille des manches doit avoir " + LIGNES + " lignes.");
        }
        List<String> premiere = grille.get(0);
        int colonnes = premiere == null ? 0 : premiere.size();
        if (colonnes < COLONNES) {
            throw new IllegalArgumentException("La grille des manches doit avoir au moins " + COLONNES + " colonnes.");
        }
        List<List<String>> r = new ArrayList<>(LIGNES);
        for (List<String> ligne : grille) {
            if (ligne == null || ligne.size() != colonnes) {
                throw new IllegalArgumentException("Chaque ligne de la grille doit avoir " + colonnes + " cases.");
            }
            r.add(List.copyOf(ligne));
        }
        return List.copyOf(r);
    }
}
