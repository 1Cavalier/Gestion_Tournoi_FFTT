package com.pingmanager.core.feuilles;

import java.util.List;
import java.util.Objects;

/**
 * Contenu d'une <b>feuille de match</b> (la « feuille de partie » des
 * règlements pour une compétition individuelle), au format provisoire demandé
 * par Quentin (cahier des charges v0, section 9) : les deux joueurs face à
 * face (nom, prénom, club, points) et une grille de 3 lignes × 5 colonnes dont
 * la première ligne porte « Manche 1 » à « Manche 5 », les autres cases étant
 * vides pour que les joueurs y notent les points.
 *
 * <p>S'y ajoutent ce qu'il faut pour rapporter la feuille à la table de marque :
 * tableau, phase, table, vainqueur, une zone « Cartons » (Règlement
 * disciplinaire, art. 3 : en compétition individuelle, le carton est inscrit
 * sur la feuille de partie et le joueur signe) et les signatures.</p>
 *
 * @param entete    tournoi, date, lieu, tableau
 * @param phase     « Poule B · tour 2 », « Quarts de finale », « Petite finale »
 * @param table     « Table 4 », ou vide si le match n'est pas encore sur une table
 * @param format    « Au meilleur des 5 manches (3 manches gagnantes) »
 * @param joueurA   premier joueur
 * @param joueurB   second joueur
 * @param grille    grille des manches, 3 lignes × 5 colonnes ({@link GrilleManches})
 * @param signatures libellés des cases de signature
 */
public record FeuilleMatch(EnTeteFeuille entete, String phase, String table, String format,
                           JoueurFeuille joueurA, JoueurFeuille joueurB, List<List<String>> grille,
                           List<String> signatures) {

    /** Titre imprimé en haut de la feuille. */
    public static final String TITRE = "Feuille de match";

    public FeuilleMatch {
        Objects.requireNonNull(entete, "entete");
        Objects.requireNonNull(joueurA, "joueurA");
        Objects.requireNonNull(joueurB, "joueurB");
        phase = phase == null ? "" : phase;
        table = table == null ? "" : table;
        format = format == null ? "" : format;
        grille = GrilleManches.copie(grille);
        signatures = List.copyOf(signatures);
    }

    /** Étiquettes des lignes de la grille, à gauche (la ligne d'en-tête n'en a pas). */
    public List<String> etiquettesLignes() {
        return List.of("", joueurA.nomComplet(), joueurB.nomComplet());
    }
}
