package com.pingmanager.core;

import com.pingmanager.model.Carton;
import com.pingmanager.model.CouleurCarton;
import com.pingmanager.model.Joueur;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Registre disciplinaire d'une compétition : conserve l'historique des cartons
 * (en match et hors match) et applique la règle FFTT/ITTF <b>3.5.2.9</b> :
 * <i>un joueur disqualifié lors de deux parties d'une épreuve individuelle est
 * automatiquement disqualifié pour le restant de la compétition.</i>
 *
 * <p>Ce registre est à l'échelle d'une compétition (il agrège les cartons de
 * toutes les épreuves/tableaux). Il permet à l'interface et au moteur de savoir
 * si un joueur est encore autorisé à jouer.</p>
 */
public final class RegistreDisciplinaire {

    private final List<Carton> cartons = new ArrayList<>();

    /** Joueurs exclus du reste de la compétition (2 disqualifications atteintes). */
    private final Set<String> joueursExclus = new HashSet<>();

    /**
     * Enregistre un carton dans l'historique. Si ce carton est une
     * disqualification (rouge du juge-arbitre) et qu'il porte le total de
     * disqualifications du joueur à 2, le joueur est exclu du reste de la
     * compétition (règle 3.5.2.9).
     *
     * @param carton le carton à enregistrer
     * @return true si ce carton déclenche l'exclusion du joueur de la compétition
     */
    public boolean enregistrer(Carton carton) {
        Objects.requireNonNull(carton, "carton");
        cartons.add(carton);

        if (carton.estDisqualifiant()) {
            long nbDisqualifications = compterDisqualifications(carton.getJoueur());
            if (nbDisqualifications >= 2) {
                joueursExclus.add(carton.getJoueur().getNumeroLicence());
                return true;
            }
        }
        return false;
    }

    /** Nombre de disqualifications (cartons rouges) reçues par un joueur. */
    public long compterDisqualifications(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        return cartons.stream()
                .filter(c -> c.getJoueur().equals(joueur))
                .filter(Carton::estDisqualifiant)
                .count();
    }

    /**
     * @return true si le joueur est exclu du reste de la compétition (2
     *         disqualifications atteintes).
     */
    public boolean estExcluDeLaCompetition(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        return joueursExclus.contains(joueur.getNumeroLicence());
    }

    /** Nombre total de cartons d'une couleur reçus par un joueur. */
    public long compterCartons(Joueur joueur, CouleurCarton couleur) {
        Objects.requireNonNull(joueur, "joueur");
        Objects.requireNonNull(couleur, "couleur");
        return cartons.stream()
                .filter(c -> c.getJoueur().equals(joueur))
                .filter(c -> c.getCouleur() == couleur)
                .count();
    }

    /** Historique complet des cartons de la compétition. */
    public List<Carton> tousLesCartons() {
        return new ArrayList<>(cartons);
    }

}
