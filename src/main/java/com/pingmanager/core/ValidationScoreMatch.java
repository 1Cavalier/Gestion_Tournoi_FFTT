package com.pingmanager.core;

import com.pingmanager.model.Manche;

import java.util.List;
import java.util.Objects;

/**
 * Vérifie qu'un score saisi est un score de tennis de table possible, avant de
 * l'enregistrer sur un {@link com.pingmanager.model.Match}.
 *
 * <p>Le modèle {@link Manche} reste volontairement tolérant ; c'est ici que
 * s'appliquent les règles du jeu (Règlements sportifs 2026-2027, chapitre 2) :</p>
 * <ul>
 *   <li><b>2.11.1</b> : « Une manche est gagnée par le joueur […] marquant le
 *       premier 11 points. Toutefois, si les deux joueurs […] ont marqué l'un et
 *       l'autre 10 points, la manche est gagnée par le joueur […] qui, par la
 *       suite, marque en premier deux points de plus » : scores possibles 11-0 à
 *       11-9, puis 12-10, 13-11, etc. ;</li>
 *   <li><b>2.12.1</b> : une partie se dispute au meilleur d'un nombre impair de
 *       manches ; elle s'arrête dès qu'un joueur a gagné le nombre de manches
 *       requis (3 au meilleur des 5, 4 au meilleur des 7).</li>
 * </ul>
 *
 * <p>Service sans état, sans dépendance UI ni persistance.</p>
 */
public final class ValidationScoreMatch {

    /** Points à atteindre pour gagner une manche (2.11.1). */
    public static final int POINTS_MANCHE = 11;

    /**
     * Valide une suite de manches pour un match où il faut gagner
     * {@code nombreManchesGagnantes} manches.
     *
     * @param manches                manches dans l'ordre où elles ont été jouées
     * @param nombreManchesGagnantes manches à gagner (3 = meilleur des 5)
     * @return {@link ResultatSaisieScore.Motif#VALIDE} ou le premier motif de refus
     */
    public ResultatSaisieScore valider(List<Manche> manches, int nombreManchesGagnantes) {
        Objects.requireNonNull(manches, "manches");
        if (nombreManchesGagnantes < 1) {
            throw new IllegalArgumentException("Il faut au moins une manche gagnante.");
        }
        if (manches.isEmpty()) {
            return ResultatSaisieScore.refus(ResultatSaisieScore.Motif.AUCUNE_MANCHE);
        }
        int gagneesA = 0;
        int gagneesB = 0;
        for (int i = 0; i < manches.size(); i++) {
            int numero = i + 1;
            if (gagneesA >= nombreManchesGagnantes || gagneesB >= nombreManchesGagnantes) {
                // 2.12.1 : la partie est finie dès que le nombre de manches est atteint.
                return ResultatSaisieScore.refusManche(ResultatSaisieScore.Motif.MANCHE_EN_TROP, numero);
            }
            Manche m = Objects.requireNonNull(manches.get(i), "manche");
            ResultatSaisieScore.Motif motifManche = controlerManche(m);
            if (motifManche != null) {
                return ResultatSaisieScore.refusManche(motifManche, numero);
            }
            if (m.gagneeParA()) {
                gagneesA++;
            } else {
                gagneesB++;
            }
        }
        if (gagneesA < nombreManchesGagnantes && gagneesB < nombreManchesGagnantes) {
            return ResultatSaisieScore.refus(ResultatSaisieScore.Motif.MATCH_NON_DECIDE);
        }
        return ResultatSaisieScore.valide();
    }

    /**
     * Contrôle une manche seule (2.11.1).
     *
     * @return le motif de refus, ou {@code null} si la manche est possible
     */
    public ResultatSaisieScore.Motif controlerManche(Manche m) {
        int gagnant = Math.max(m.pointsJoueurA(), m.pointsJoueurB());
        int perdant = Math.min(m.pointsJoueurA(), m.pointsJoueurB());
        if (gagnant == perdant) {
            return ResultatSaisieScore.Motif.MANCHE_A_EGALITE;
        }
        if (gagnant < POINTS_MANCHE) {
            return ResultatSaisieScore.Motif.MANCHE_SOUS_ONZE_POINTS;
        }
        if (gagnant == POINTS_MANCHE) {
            // 11-10 impossible : à 10-10, il faut ensuite deux points d'écart.
            return perdant <= POINTS_MANCHE - 2 ? null : ResultatSaisieScore.Motif.MANCHE_ECART_INSUFFISANT;
        }
        // Au-delà de 11 : la manche est passée par 10-10 et s'arrête à deux points d'écart.
        return gagnant - perdant == 2 ? null : ResultatSaisieScore.Motif.MANCHE_PROLONGATION_INCORRECTE;
    }
}
