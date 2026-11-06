package com.pingmanager.core.feuilles;

import java.util.ArrayList;
import java.util.List;

/**
 * Grille des manches d'une feuille de match, au format provisoire de Quentin
 * (cahier des charges v0, section 9) : <b>3 lignes × 5 colonnes</b>, la
 * première ligne porte « Manche 1 » à « Manche 5 », toutes les autres cases
 * sont vides.
 *
 * <p>Cinq colonnes quel que soit le format de la partie : une partie au
 * meilleur des cinq manches (2.12.1) en a au plus cinq ; au meilleur des trois,
 * les deux dernières restent vides.</p>
 */
public final class GrilleManches {

    /** Nombre de lignes de la grille, en-tête compris. */
    public static final int LIGNES = 3;
    /** Nombre de colonnes (manches). */
    public static final int COLONNES = 5;

    private GrilleManches() {
    }

    /** La grille vide : « Manche 1 » … « Manche 5 » puis deux lignes de cases vides. */
    public static List<List<String>> vide() {
        List<List<String>> g = new ArrayList<>(LIGNES);
        List<String> entetes = new ArrayList<>(COLONNES);
        for (int c = 1; c <= COLONNES; c++) {
            entetes.add("Manche " + c);
        }
        g.add(List.copyOf(entetes));
        for (int l = 1; l < LIGNES; l++) {
            g.add(List.of("", "", "", "", ""));
        }
        return List.copyOf(g);
    }

    /** Copie immuable, en vérifiant la forme 3 × 5. */
    static List<List<String>> copie(List<List<String>> grille) {
        if (grille == null || grille.size() != LIGNES) {
            throw new IllegalArgumentException("La grille des manches doit avoir " + LIGNES + " lignes.");
        }
        List<List<String>> r = new ArrayList<>(LIGNES);
        for (List<String> ligne : grille) {
            if (ligne == null || ligne.size() != COLONNES) {
                throw new IllegalArgumentException("Chaque ligne de la grille doit avoir " + COLONNES + " cases.");
            }
            r.add(List.copyOf(ligne));
        }
        return List.copyOf(r);
    }
}
