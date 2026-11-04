package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Grille des récompenses d'un {@link Tableau} : à quelle place (ou plage de
 * places) correspond quelle {@link Recompense}.
 *
 * <p>Le nombre de places dotées est <b>libre</b> : le plus souvent les 4 premiers,
 * mais parfois seulement les 2 premiers, parfois jusqu'aux 8 premiers, etc. Chaque
 * entrée couvre soit une place exacte (« 1er »), soit une plage de places
 * consécutives partageant la même récompense (« 3e-4e », « 5e-8e »).</p>
 *
 * <p>Exemple typique d'un tableau doté aux 4 premiers :</p>
 * <pre>
 *   GrilleRecompenses.builder()
 *       .place(1, Recompense.argent(150))
 *       .place(2, Recompense.argent(80))
 *       .plage(3, 4, Recompense.argent(40))
 *       .build();
 * </pre>
 *
 * <p>La grille valide sa cohérence à la construction : les plages ne se
 * chevauchent pas, commencent à la place 1 et sont <b>contiguës</b> (pas de trou),
 * de sorte que les places dotées vont de 1 à N sans interruption. Une grille vide
 * (aucune dotation) est autorisée.</p>
 */
public final class GrilleRecompenses {

    /** Une entrée de la grille : de la place {@code placeDebut} à {@code placeFin} (incluses). */
    public static final class Entree {
        private final int placeDebut;
        private final int placeFin;
        private final Recompense recompense;

        Entree(int placeDebut, int placeFin, Recompense recompense) {
            this.placeDebut = placeDebut;
            this.placeFin = placeFin;
            this.recompense = recompense;
        }

        public int getPlaceDebut() {
            return placeDebut;
        }

        public int getPlaceFin() {
            return placeFin;
        }

        public Recompense getRecompense() {
            return recompense;
        }

        /** @return true si cette entrée couvre une seule place. */
        public boolean estPlaceUnique() {
            return placeDebut == placeFin;
        }

        public boolean couvre(int place) {
            return place >= placeDebut && place <= placeFin;
        }

        @Override
        public String toString() {
            String pos = estPlaceUnique() ? String.valueOf(placeDebut) : (placeDebut + "-" + placeFin);
            return pos + " : " + recompense;
        }
    }

    private final List<Entree> entrees;

    private GrilleRecompenses(List<Entree> entrees) {
        this.entrees = entrees;
    }

    /** Grille sans aucune dotation. */
    public static GrilleRecompenses vide() {
        return new GrilleRecompenses(Collections.emptyList());
    }

    public List<Entree> getEntrees() {
        return Collections.unmodifiableList(entrees);
    }

    public boolean estVide() {
        return entrees.isEmpty();
    }

    /** @return la dernière place dotée (0 si la grille est vide). */
    public int nombreDePlacesDotees() {
        int max = 0;
        for (Entree e : entrees) {
            max = Math.max(max, e.placeFin);
        }
        return max;
    }

    /**
     * @return la récompense attribuée à une place donnée, ou {@code null} si cette
     *         place n'est pas dotée.
     */
    public Recompense recompensePour(int place) {
        for (Entree e : entrees) {
            if (e.couvre(place)) {
                return e.recompense;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (entrees.isEmpty()) {
            return "GrilleRecompenses(vide)";
        }
        StringBuilder sb = new StringBuilder("GrilleRecompenses[");
        for (int i = 0; i < entrees.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(entrees.get(i));
        }
        return sb.append(']').toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construit une grille en ajoutant les places/plages dans l'ordre croissant.
     * La cohérence (contiguïté depuis la place 1, absence de chevauchement) est
     * vérifiée à {@link #build()}.
     */
    public static final class Builder {
        private final List<Entree> entrees = new ArrayList<>();

        /** Dote une place exacte. */
        public Builder place(int place, Recompense recompense) {
            return plage(place, place, recompense);
        }

        /** Dote une plage de places [placeDebut, placeFin] avec la même récompense. */
        public Builder plage(int placeDebut, int placeFin, Recompense recompense) {
            Objects.requireNonNull(recompense, "recompense");
            if (placeDebut < 1) {
                throw new IllegalArgumentException("La première place dotée doit être >= 1 : " + placeDebut);
            }
            if (placeFin < placeDebut) {
                throw new IllegalArgumentException(
                        "placeFin (" + placeFin + ") ne peut pas être avant placeDebut (" + placeDebut + ").");
            }
            entrees.add(new Entree(placeDebut, placeFin, recompense));
            return this;
        }

        public GrilleRecompenses build() {
            List<Entree> triees = new ArrayList<>(entrees);
            triees.sort((a, b) -> Integer.compare(a.placeDebut, b.placeDebut));

            int attendue = 1;
            for (Entree e : triees) {
                if (e.placeDebut != attendue) {
                    if (e.placeDebut < attendue) {
                        throw new IllegalArgumentException(
                                "Chevauchement de places dans la grille de récompenses autour de la place "
                                        + e.placeDebut + ".");
                    }
                    throw new IllegalArgumentException(
                            "Trou dans la grille de récompenses : la place " + attendue
                                    + " n'est pas dotée alors que la place " + e.placeDebut + " l'est.");
                }
                attendue = e.placeFin + 1;
            }
            return new GrilleRecompenses(triees);
        }
    }
}
