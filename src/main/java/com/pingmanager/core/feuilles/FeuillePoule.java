package com.pingmanager.core.feuilles;

import java.util.List;
import java.util.Objects;

/**
 * Contenu de la <b>feuille de poule</b> : les joueurs numérotés comme dans le
 * tirage, l'ordre des parties (I.301), la grille des résultats (une case par
 * paire de joueurs, diagonale barrée) et le classement à remplir (I.303).
 *
 * <p>Elle est suivie de la <b>feuille de rencontre</b> de la poule : chaque
 * partie, dans l'ordre officiel, avec sa grille de manches 3 × 5 (format
 * provisoire de Quentin, cahier des charges v0, section 9), pour que les
 * joueurs notent les scores sur papier et la rapportent à la table de
 * marque.</p>
 *
 * @param entete              tournoi, date, lieu, tableau
 * @param poule               « Poule B »
 * @param format              « Au meilleur des 5 manches (3 manches gagnantes) »
 * @param qualifies           nombre de qualifiés de la poule (0 si inconnu)
 * @param joueurs             joueurs dans l'ordre de la poule (position 1, 2, 3…)
 * @param parties             parties dans l'ordre officiel I.301
 * @param grilleResultats     grille carrée (n + 1) × (n + 1) : en-têtes « 1 »…« n », diagonale « X »
 * @param colonnesClassement  en-têtes du classement à remplir
 * @param noteClassement      rappel de I.303, imprimé sous le classement
 */
public record FeuillePoule(EnTeteFeuille entete, String poule, String format, int qualifies,
                           List<JoueurFeuille> joueurs, List<PartiePoule> parties,
                           List<List<String>> grilleResultats, List<String> colonnesClassement,
                           String noteClassement) {

    /** Titre imprimé en haut de la feuille. */
    public static final String TITRE = "Feuille de poule";
    /** Titre des pages de parties avec leurs grilles de manches. */
    public static final String TITRE_RENCONTRE = "Feuille de rencontre";
    /** Case de la diagonale (un joueur ne se rencontre pas). */
    public static final String CASE_BARREE = "X";

    public FeuillePoule {
        Objects.requireNonNull(entete, "entete");
        poule = poule == null ? "" : poule;
        format = format == null ? "" : format;
        joueurs = List.copyOf(joueurs);
        parties = List.copyOf(parties);
        grilleResultats = grilleResultats.stream().map(List::copyOf).toList();
        colonnesClassement = List.copyOf(colonnesClassement);
        noteClassement = noteClassement == null ? "" : noteClassement;
    }

    /** Nombre de lignes du classement à remplir : une par joueur. */
    public int lignesClassement() {
        return joueurs.size();
    }

    /**
     * Une partie de la poule.
     *
     * @param numero    numéro d'ordre (1, 2, 3…) dans l'ordre I.301
     * @param tour      tour de la partie (I.301 : parties regroupées par tour)
     * @param positionA position du premier joueur dans la poule (1 = tête de poule)
     * @param positionB position du second joueur, 0 pour un exempt
     * @param joueurA   premier joueur
     * @param joueurB   second joueur (place vide pour un exempt)
     * @param exempt    partie sans adversaire (rien à jouer)
     * @param grille    grille des manches 3 × 5
     */
    public record PartiePoule(int numero, int tour, int positionA, int positionB, JoueurFeuille joueurA,
                              JoueurFeuille joueurB, boolean exempt, List<List<String>> grille) {
        public PartiePoule {
            Objects.requireNonNull(joueurA, "joueurA");
            Objects.requireNonNull(joueurB, "joueurB");
            grille = GrilleManches.copie(grille);
        }

        /** « 1 – 3 » (positions dans la poule), ou « 2 – exempt ». */
        public String libellePositions() {
            return exempt ? positionA + " – exempt" : positionA + " – " + positionB;
        }
    }
}
