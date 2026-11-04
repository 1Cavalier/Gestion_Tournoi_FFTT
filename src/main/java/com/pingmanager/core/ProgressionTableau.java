package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;

import java.util.Objects;

/**
 * Fait progresser un {@link TableauElimination} : lorsqu'un match est terminé,
 * son vainqueur est propagé vers le match qui l'attend au tour suivant.
 *
 * <p>La propagation s'appuie sur les liens explicites établis à la génération :
 * chaque match connaît, pour ses deux places, le match source dont le vainqueur
 * doit y venir ({@link Match#getSourceA()} / {@link Match#getSourceB()}). Cette
 * approche est robuste à la présence d'exemptés et ne dépend d'aucune hypothèse
 * d'indexation.</p>
 *
 * <p>Idempotente : peut être appelée à tout moment ; ne modifie que ce qui est
 * devenu décidable.</p>
 */
public final class ProgressionTableau {

    /**
     * Propage tous les vainqueurs décidables sur l'ensemble du tableau.
     *
     * @param tableau le tableau à faire progresser
     * @return le nombre d'emplacements nouvellement renseignés
     */
    public int propager(TableauElimination tableau) {
        Objects.requireNonNull(tableau, "tableau");
        int modifications = 0;

        for (Tour tour : tableau.getTours()) {
            boolean estPetiteFinale = "Petite finale".equals(tour.getNom());
            for (Match match : tour.getMatchs()) {
                if (estPetiteFinale) {
                    // La petite finale prend les PERDANTS des demi-finales.
                    modifications += resoudrePlacePerdant(match, true);
                    modifications += resoudrePlacePerdant(match, false);
                } else {
                    modifications += resoudrePlace(match, true);   // place A (vainqueur)
                    modifications += resoudrePlace(match, false);  // place B (vainqueur)
                }
            }
        }

        // Un match dont les deux joueurs sont désormais connus devient jouable.
        for (Tour tour : tableau.getTours()) {
            for (Match match : tour.getMatchs()) {
                if (match.getStatut() == StatutMatch.EN_ATTENTE && estComplet(match)) {
                    match.setStatut(StatutMatch.A_JOUER);
                }
            }
        }
        return modifications;
    }

    /**
     * Renseigne la place A ou B d'un match avec le <b>perdant</b> de son match
     * source (utilisé pour la petite finale). Symétrique de {@link #resoudrePlace}
     * mais propage le perdant au lieu du vainqueur.
     *
     * @return 1 si un emplacement a été renseigné, 0 sinon
     */
    private int resoudrePlacePerdant(Match match, boolean placeA) {
        Match source = placeA ? match.getSourceA() : match.getSourceB();
        if (source == null || source.getStatut() != StatutMatch.TERMINE) {
            return 0;
        }
        Joueur perdant = source.getPerdant();
        if (perdant == null) {
            return 0;
        }
        Joueur actuelA = match.getJoueurA();
        Joueur actuelB = match.getJoueurB();
        if (placeA) {
            if (actuelA != null && actuelA.estADeterminer()) {
                match.remplacerJoueurs(perdant, actuelB);
                return 1;
            }
        } else {
            if (actuelB != null && actuelB.estADeterminer()) {
                match.remplacerJoueurs(actuelA, perdant);
                return 1;
            }
        }
        return 0;
    }

    /**
     * Renseigne la place A ou B d'un match à partir du vainqueur de son match
     * source, si celui-ci est terminé et que la place est encore « à déterminer ».
     *
     * @return 1 si un emplacement a été renseigné, 0 sinon
     */
    private int resoudrePlace(Match match, boolean placeA) {
        Match source = placeA ? match.getSourceA() : match.getSourceB();
        if (source == null || source.getStatut() != StatutMatch.TERMINE) {
            return 0;
        }
        Joueur vainqueur = source.getVainqueur();
        if (vainqueur == null) {
            return 0;
        }

        Joueur actuelA = match.getJoueurA();
        Joueur actuelB = match.getJoueurB();

        if (placeA) {
            if (actuelA != null && actuelA.estADeterminer()) {
                match.remplacerJoueurs(vainqueur, actuelB);
                return 1;
            }
        } else {
            if (actuelB != null && actuelB.estADeterminer()) {
                match.remplacerJoueurs(actuelA, vainqueur);
                return 1;
            }
        }
        return 0;
    }

    /** Indique si les deux joueurs d'un match sont connus (non « à déterminer »). */
    private boolean estComplet(Match m) {
        Joueur a = m.getJoueurA();
        Joueur b = m.getJoueurB();
        boolean aOk = a != null && !a.estADeterminer();
        boolean bOk = b != null && !b.estADeterminer();
        return aOk && bOk;
    }
}
