package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Génère les rencontres d'une poule en championnat toutes rondes : chaque
 * joueur affronte tous les autres exactement une fois.
 *
 * <p>L'ordre des rencontres suit, pour les poules de 3 et 4, les ordres
 * recommandés par la FFTT (qui minimisent le temps d'attente entre deux matchs
 * d'un même joueur). Pour les autres tailles, un ordre "cercle" générique est
 * produit.</p>
 *
 * <p><b>Règle anti-arrangement (FFTT).</b> Lorsque deux joueurs d'une même
 * association se retrouvent dans la même poule (cas normalement évité par le
 * générateur de poules, mais parfois inévitable), le règlement impose qu'ils
 * <i>se rencontrent au premier tour s'ils sont deux, et le plus rapidement
 * possible s'ils sont trois ou plus</i>. Ceci empêche tout arrangement sur un
 * résultat qui influencerait le classement de la poule. Le générateur applique
 * cette règle en réordonnant les joueurs dans la poule (donc en choisissant qui
 * occupe quelle position) de façon à faire tomber les rencontres intra-club le
 * plus tôt possible dans l'ordre des matchs.</p>
 */
public final class GenerateurMatchsPoule {

    // ===================================================================
    // Ordres officiels des parties (article I.301 des règlements sportifs
    // FFTT, édition juillet 2026). L'ordre dépend à la fois de la TAILLE de
    // la poule et du NOMBRE DE QUALIFIÉS. Les indices sont 1-based (position
    // du joueur dans la poule selon le classement).
    // ===================================================================

    /** I.301.1 — Poule de 3, 1 ou 3 qualifié(s) : 1-3, 2-3, 1-2. */
    private static final int[][] P3_UN_OU_TROIS = {
            {1, 3}, {2, 3}, {1, 2}
    };
    /** I.301.2 — Poule de 3, 2 qualifiés : 1-3, 1-2, 2-3. */
    private static final int[][] P3_DEUX = {
            {1, 3}, {1, 2}, {2, 3}
    };

    /** I.301.3 — Poule de 4, 1 ou 3 qualifié(s) : (1-4,2-3)(1-3,2-4)(1-2,3-4). */
    private static final int[][] P4_UN_OU_TROIS = {
            {1, 4}, {2, 3}, {1, 3}, {2, 4}, {1, 2}, {3, 4}
    };
    /** I.301.4 — Poule de 4, 2 qualifiés : (1-3,2-4)(1-2,3-4)(1-4,2-3). */
    private static final int[][] P4_DEUX = {
            {1, 3}, {2, 4}, {1, 2}, {3, 4}, {1, 4}, {2, 3}
    };

    /** I.301.5 — Poule de 5, 1 qualifié : (2-5,3-4)(1-5,2-3)(1-4,3-5)(1-3,2-4)(1-2,4-5). */
    private static final int[][] P5_UN = {
            {2, 5}, {3, 4}, {1, 5}, {2, 3}, {1, 4},
            {3, 5}, {1, 3}, {2, 4}, {1, 2}, {4, 5}
    };
    /** I.301.6 — Poule de 5, 2 qualifiés : (2-5,3-4)(1-4,3-5)(1-3,2-4)(1-2,4-5)(1-5,2-3). */
    private static final int[][] P5_DEUX = {
            {2, 5}, {3, 4}, {1, 4}, {3, 5}, {1, 3},
            {2, 4}, {1, 2}, {4, 5}, {1, 5}, {2, 3}
    };

    /** I.301.7 — Poule de 6, 1 qualifié. */
    private static final int[][] P6_UN = {
            {1, 6}, {2, 5}, {3, 4}, {1, 5}, {4, 6}, {2, 3},
            {1, 4}, {3, 5}, {2, 6}, {1, 3}, {2, 4}, {5, 6},
            {1, 2}, {3, 6}, {4, 5}
    };
    /** I.301.8 — Poule de 6, 2 qualifiés. */
    private static final int[][] P6_DEUX = {
            {1, 6}, {2, 5}, {3, 4}, {1, 4}, {3, 5}, {2, 6},
            {1, 3}, {2, 4}, {5, 6}, {1, 2}, {3, 6}, {4, 5},
            {1, 5}, {4, 6}, {2, 3}
    };

    /**
     * Génère et ajoute les matchs à la poule fournie, avec l'ordre par défaut
     * (cas « 1 ou 3 qualifié(s) »). Pour l'ordre exact selon le nombre de
     * qualifiés, utiliser {@link #genererMatchs(Poule, int)}.
     *
     * @param poule la poule dont on veut créer les rencontres
     * @return la liste des matchs créés (également ajoutés à la poule)
     */
    public List<Match> genererMatchs(Poule poule) {
        return genererMatchs(poule, 1);
    }

    /**
     * Génère et ajoute les matchs à la poule fournie, selon l'ordre officiel
     * I.301 correspondant à la taille de poule ET au nombre de qualifiés.
     *
     * @param poule             la poule dont on veut créer les rencontres
     * @param qualifiesParPoule nombre de qualifiés par poule (détermine l'ordre)
     *                          poule de 3 : 2 qualifiés → ordre I.301.2)
     * @return la liste des matchs créés (également ajoutés à la poule)
     */
    public List<Match> genererMatchs(Poule poule, int qualifiesParPoule) {
        Objects.requireNonNull(poule, "poule");
        List<Joueur> joueurs = poule.getJoueurs();
        int taille = joueurs.size();

        int[][] ordre = choisirOrdre(taille, qualifiesParPoule);

        // Réordonne les joueurs pour que les rencontres intra-club aient lieu le
        // plus tôt possible (règle FFTT anti-arrangement). Sans conflit de club,
        // l'ordre d'origine est conservé.
        List<Joueur> ordonnes = reordonnerPourRencontrePrecoce(joueurs, ordre);

        for (int[] paire : ordre) {
            Joueur a = ordonnes.get(paire[0] - 1);
            Joueur b = ordonnes.get(paire[1] - 1);
            poule.ajouterMatch(new Match(a, b));
        }
        return poule.getMatchs();
    }

    /**
     * Sélectionne l'ordre officiel I.301 selon la taille de poule et le nombre
     * de qualifiés. Le cas « 2 qualifiés » a son propre ordre pour les tailles
     * 3 à 6 ; les autres cas (1 ou 3 qualifiés) partagent le même ordre. Pour
     * les tailles non tabulées, un ordre du cercle générique est produit.
     */
    private int[][] choisirOrdre(int taille, int qualifiesParPoule) {
        boolean deuxQualifies = (qualifiesParPoule == 2);
        return switch (taille) {
            case 3 -> deuxQualifies ? P3_DEUX : P3_UN_OU_TROIS;
            case 4 -> deuxQualifies ? P4_DEUX : P4_UN_OU_TROIS;
            case 5 -> deuxQualifies ? P5_DEUX : P5_UN;
            case 6 -> deuxQualifies ? P6_DEUX : P6_UN;
            default -> ordreCercleGenerique(taille);
        };
    }

    /**
     * Choisit l'affectation des joueurs aux positions de la poule qui fait
     * survenir les rencontres entre joueurs d'un même club le plus tôt possible.
     *
     * <p>La taille des poules étant petite (3 à 5), on évalue toutes les
     * permutations et on retient celle de <i>coût</i> minimal. Le coût privilégie
     * d'abord la précocité de la première rencontre intra-club, puis la somme des
     * tours de toutes les rencontres intra-club (utile quand trois joueurs ou
     * plus partagent un club). En l'absence de tout conflit de club, l'ordre
     * initial (coût nul) est conservé tel quel.</p>
     */
    private List<Joueur> reordonnerPourRencontrePrecoce(List<Joueur> joueurs, int[][] ordre) {
        // Tour (1-based) auquel chaque paire de positions se rencontre.
        // tourDePosition[i][j] = index (1-based) du match opposant positions i et j.
        int taille = joueurs.size();
        int[][] tourDePosition = new int[taille][taille];
        for (int t = 0; t < ordre.length; t++) {
            int p1 = ordre[t][0] - 1;
            int p2 = ordre[t][1] - 1;
            tourDePosition[p1][p2] = t + 1;
            tourDePosition[p2][p1] = t + 1;
        }

        // S'il n'y a aucun conflit de club, rien à faire.
        if (coutAffectation(joueurs, tourDePosition) == 0) {
            return joueurs;
        }

        List<Joueur> meilleur = joueurs;
        long meilleurCout = coutAffectation(joueurs, tourDePosition);

        List<List<Joueur>> permutations = new ArrayList<>();
        permuter(new ArrayList<>(joueurs), 0, permutations);
        for (List<Joueur> perm : permutations) {
            long cout = coutAffectation(perm, tourDePosition);
            if (cout < meilleurCout) {
                meilleurCout = cout;
                meilleur = perm;
            }
        }
        return meilleur;
    }

    /**
     * Coût d'une affectation joueurs→positions vis-à-vis de la règle intra-club.
     *
     * <p>Pour chaque paire de joueurs d'un même club, on additionne le tour
     * auquel ils se rencontrent, en pondérant très fortement le tour de la
     * <i>première</i> rencontre intra-club afin de la rendre prioritairement
     * précoce. Coût 0 signifie « aucun joueur de même club dans la poule ».</p>
     */
    private long coutAffectation(List<Joueur> ordre, int[][] tourDePosition) {
        long premiereRencontreMin = Long.MAX_VALUE;
        long sommeTours = 0;
        boolean conflit = false;

        for (int i = 0; i < ordre.size(); i++) {
            for (int j = i + 1; j < ordre.size(); j++) {
                if (memeClub(ordre.get(i), ordre.get(j))) {
                    conflit = true;
                    int tour = tourDePosition[i][j];
                    sommeTours += tour;
                    premiereRencontreMin = Math.min(premiereRencontreMin, tour);
                }
            }
        }
        if (!conflit) {
            return 0;
        }
        // Pondération : la précocité de la première rencontre domine la somme.
        return premiereRencontreMin * 1000L + sommeTours;
    }

    private boolean memeClub(Joueur a, Joueur b) {
        String ca = a.getClub();
        String cb = b.getClub();
        return ca != null && !ca.isBlank() && ca.equalsIgnoreCase(cb);
    }

    /** Génère toutes les permutations d'une liste (tailles de poule petites). */
    private void permuter(List<Joueur> liste, int debut, List<List<Joueur>> sortie) {
        if (debut == liste.size() - 1) {
            sortie.add(new ArrayList<>(liste));
            return;
        }
        for (int i = debut; i < liste.size(); i++) {
            java.util.Collections.swap(liste, debut, i);
            permuter(liste, debut + 1, sortie);
            java.util.Collections.swap(liste, debut, i);
        }
    }

    /**
     * Produit un ordre de rencontres "toutes rondes" générique pour une taille
     * quelconque via l'algorithme du cercle (round-robin). Renvoie des paires
     * en indices 1-based pour rester cohérent avec les tables officielles.
     */
    private int[][] ordreCercleGenerique(int taille) {
        boolean impair = (taille % 2 != 0);
        int n = impair ? taille + 1 : taille; // ajout d'un "fantôme" si impair
        int rondes = n - 1;
        int matchsParRonde = n / 2;

        int[][] resultat = new int[rondes * matchsParRonde][2];
        int idx = 0;

        // Positions : 0 fixe, les autres tournent.
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) {
            pos[i] = i;
        }

        for (int r = 0; r < rondes; r++) {
            for (int m = 0; m < matchsParRonde; m++) {
                int j1 = pos[m];
                int j2 = pos[n - 1 - m];
                // On saute les paires impliquant le joueur fantôme (indice n-1 quand impair).
                if (impair && (j1 == n - 1 || j2 == n - 1)) {
                    continue;
                }
                resultat[idx][0] = j1 + 1;
                resultat[idx][1] = j2 + 1;
                idx++;
            }
            // Rotation : on garde pos[0] fixe et on fait tourner le reste.
            int dernier = pos[n - 1];
            for (int i = n - 1; i > 1; i--) {
                pos[i] = pos[i - 1];
            }
            pos[1] = dernier;
        }

        // Recompacter (des lignes vides peuvent subsister si taille impaire).
        int[][] compact = new int[idx][2];
        System.arraycopy(resultat, 0, compact, 0, idx);
        return compact;
    }
}
