package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Représente un tableau à élimination directe complet : la suite ordonnée des
 * {@link Tour}, du premier tour joué jusqu'à la finale.
 *
 * <p>Conteneur de données. La construction de l'arbre (placement des têtes de
 * série, gestion des exemptés) est réalisée par
 * {@code GenerateurTableauElimination} dans le package {@code core}.</p>
 */
public final class TableauElimination {

    private final List<Tour> tours = new ArrayList<>();

    /** Nombre de joueurs réels au départ (hors joueurs fictifs / exemptions). */
    private final int nombreJoueurs;

    /** Taille du cadre : puissance de 2 &gt;= nombreJoueurs (ex. 16, 32, 64). */
    private final int cadre;

    public TableauElimination(int nombreJoueurs, int cadre) {
        if (nombreJoueurs < 2) {
            throw new IllegalArgumentException("Un tableau à élimination requiert au moins 2 joueurs.");
        }
        this.nombreJoueurs = nombreJoueurs;
        this.cadre = cadre;
    }

    public List<Tour> getTours() {
        return Collections.unmodifiableList(tours);
    }

    public void ajouterTour(Tour tour) {
        tours.add(Objects.requireNonNull(tour, "tour"));
    }

    public int getNombreJoueurs() {
        return nombreJoueurs;
    }

    public int getCadre() {
        return cadre;
    }

    /** Nombre d'exemptés du premier tour (= cadre - nombreJoueurs). */
    public int getNombreExemptes() {
        return cadre - nombreJoueurs;
    }

    public int getNombreTours() {
        return tours.size();
    }

    public Tour getPremierTour() {
        return tours.isEmpty() ? null : tours.get(0);
    }

    public Tour getFinale() {
        return tours.isEmpty() ? null : tours.get(tours.size() - 1);
    }

    @Override
    public String toString() {
        return "Tableau élimination (" + nombreJoueurs + " joueurs, cadre "
                + cadre + ", " + getNombreExemptes() + " exemptés, "
                + tours.size() + " tours)";
    }
}
