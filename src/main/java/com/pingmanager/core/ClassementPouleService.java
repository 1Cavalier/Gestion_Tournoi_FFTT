package com.pingmanager.core;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calcule le classement d'une poule selon les règles FFTT.
 *
 * <p><b>Critère principal :</b> les points de classement (2 par victoire,
 * 1 par défaite). Le joueur avec le plus de points est devant.</p>
 *
 * <p><b>Départage des ex æquo (règle FFTT importante) :</b> lorsque plusieurs
 * joueurs sont à égalité de points de classement, ils ne sont PAS départagés
 * sur leurs statistiques globales, mais sur un « mini-classement » établi
 * uniquement à partir des rencontres qui les ont opposés entre eux. Dans ce
 * sous-groupe on compare successivement :</p>
 * <ol>
 *   <li>le nombre de victoires entre eux ;</li>
 *   <li>le quotient de manches (gagnées / perdues) entre eux ;</li>
 *   <li>le quotient de points entre eux.</li>
 * </ol>
 *
 * <p>Si l'égalité subsiste après tous ces critères, l'ordre relatif est laissé
 * stable (les joueurs restent réellement ex æquo ; un tirage au sort serait
 * décidé par l'organisateur, hors de ce service déterministe).</p>
 *
 * <p>Service sans état, sans dépendance UI/persistance. Ne considère que les
 * matchs {@link StatutMatch#TERMINE}.</p>
 */
public final class ClassementPouleService {

    /**
     * Calcule le classement complet de la poule.
     *
     * @param poule la poule à classer (ses matchs doivent être terminés pour un
     *              classement définitif ; les matchs non terminés sont ignorés)
     * @return les lignes de classement, triées du 1er au dernier, avec le rang
     *         renseigné sur chaque ligne
     */
    public List<LigneClassement> calculer(Poule poule) {
        Objects.requireNonNull(poule, "poule");

        Map<Joueur, LigneClassement> lignes = accumulerStats(poule.getJoueurs(), poule.getMatchs());

        List<LigneClassement> resultat = new ArrayList<>(lignes.values());
        resultat.sort(comparateurAvecDepartage(poule, lignes));

        for (int i = 0; i < resultat.size(); i++) {
            resultat.get(i).setRang(i + 1);
        }
        return resultat;
    }

    /**
     * Construit une ligne de classement par joueur en accumulant les statistiques
     * des matchs terminés fournis (les byes et les matchs non terminés sont ignorés).
     */
    private Map<Joueur, LigneClassement> accumulerStats(List<Joueur> joueurs, List<Match> matchs) {
        Map<Joueur, LigneClassement> lignes = new LinkedHashMap<>();
        for (Joueur j : joueurs) {
            lignes.put(j, new LigneClassement(j));
        }
        for (Match m : matchs) {
            if (m.getStatut() == StatutMatch.TERMINE && !m.estBye()) {
                appliquerMatch(m, lignes);
            }
        }
        return lignes;
    }

    /** Met à jour les deux lignes concernées par un match terminé. */
    private void appliquerMatch(Match m, Map<Joueur, LigneClassement> lignes) {
        LigneClassement la = lignes.get(m.getJoueurA());
        LigneClassement lb = lignes.get(m.getJoueurB());
        if (la == null || lb == null) {
            return; // joueur hors poule : ignoré par sécurité
        }

        int manchesA = m.manchesGagneesParA();
        int manchesB = m.manchesGagneesParB();
        la.ajouterManches(manchesA, manchesB);
        lb.ajouterManches(manchesB, manchesA);

        int pointsA = 0;
        int pointsB = 0;
        for (Manche manche : m.getManches()) {
            pointsA += manche.pointsJoueurA();
            pointsB += manche.pointsJoueurB();
        }
        la.ajouterPoints(pointsA, pointsB);
        lb.ajouterPoints(pointsB, pointsA);

        Joueur vainqueur = m.getVainqueur();
        if (vainqueur == null) {
            return; // match nul impossible en TT, mais on reste prudent
        }

        // Issue non normale : le fautif marque 0 point (I.303) au lieu de 1.
        boolean fautifZeroPoint = m.getIssue() != IssueMatch.NORMALE;

        if (vainqueur.equals(m.getJoueurA())) {
            la.enregistrerVictoire();
            if (fautifZeroPoint) {
                lb.enregistrerDefaiteZeroPoint();
            } else {
                lb.enregistrerDefaite();
            }
        } else {
            lb.enregistrerVictoire();
            if (fautifZeroPoint) {
                la.enregistrerDefaiteZeroPoint();
            } else {
                la.enregistrerDefaite();
            }
        }
    }

    /**
     * Comparateur triant par points de classement décroissants, puis, à égalité,
     * appliquant le mini-classement FFTT restreint aux confrontations entre ex æquo.
     * Les statistiques globales sont fournies une fois (pas recalculées par appel).
     */
    private Comparator<LigneClassement> comparateurAvecDepartage(Poule poule,
                                                                 Map<Joueur, LigneClassement> statsGlobales) {
        return (x, y) -> {
            int cmp = Integer.compare(y.getPointsClassement(), x.getPointsClassement());
            if (cmp != 0) {
                return cmp;
            }
            return departagerParConfrontations(poule, statsGlobales, x, y);
        };
    }

    /**
     * Départage deux joueurs à égalité de points via le mini-classement du
     * sous-groupe des joueurs partageant le même total de points, établi sur les
     * seuls matchs internes à ce groupe. Applique successivement (I.303.c) le total
     * des points de confrontation, puis le quotient de manches, puis le quotient de
     * points.
     */
    private int departagerParConfrontations(Poule poule,
                                            Map<Joueur, LigneClassement> statsGlobales,
                                            LigneClassement x, LigneClassement y) {
        int niveau = x.getPointsClassement();

        Map<Joueur, LigneClassement> internes = new LinkedHashMap<>();
        for (LigneClassement l : statsGlobales.values()) {
            if (l.getPointsClassement() == niveau) {
                internes.put(l.getJoueur(), new LigneClassement(l.getJoueur()));
            }
        }

        for (Match m : poule.getMatchs()) {
            if (m.getStatut() == StatutMatch.TERMINE && !m.estBye()
                    && internes.containsKey(m.getJoueurA())
                    && internes.containsKey(m.getJoueurB())) {
                appliquerMatch(m, internes);
            }
        }

        LigneClassement ix = internes.get(x.getJoueur());
        LigneClassement iy = internes.get(y.getJoueur());
        if (ix == null || iy == null) {
            return 0;
        }

        int cmp = Integer.compare(iy.getPointsClassement(), ix.getPointsClassement());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Double.compare(iy.getQuotientManches(), ix.getQuotientManches());
        if (cmp != 0) {
            return cmp;
        }
        return Double.compare(iy.getQuotientPoints(), ix.getQuotientPoints());
    }
}
