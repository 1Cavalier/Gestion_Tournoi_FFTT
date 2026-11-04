package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Un tour d'un tableau à élimination directe (ex. « 64es de finale »,
 * « quarts de finale », « finale »).
 *
 * <p>Un tour contient la liste ordonnée de ses matchs. L'ordre des matchs est
 * significatif : il définit la structure de l'arbre (le vainqueur du match i
 * du tour courant affronte le vainqueur du match i+1 au tour suivant, selon la
 * règle d'appariement classique).</p>
 */
public final class Tour {

    private final String nom;
    private final int numero; // 1 = premier tour joué, croissant jusqu'à la finale
    private final List<Match> matchs = new ArrayList<>();

    public Tour(int numero, String nom) {
        if (numero < 1) {
            throw new IllegalArgumentException("Le numéro de tour doit être >= 1.");
        }
        this.numero = numero;
        this.nom = Objects.requireNonNull(nom, "nom");
    }

    public int getNumero() {
        return numero;
    }

    public String getNom() {
        return nom;
    }

    public List<Match> getMatchs() {
        return Collections.unmodifiableList(matchs);
    }

    public void ajouterMatch(Match match) {
        matchs.add(Objects.requireNonNull(match, "match"));
    }

    public int getNombreMatchs() {
        return matchs.size();
    }

    /** @return true si tous les matchs du tour sont terminés. */
    public boolean estTermine() {
        return !matchs.isEmpty()
                && matchs.stream().allMatch(m -> m.getStatut() == StatutMatch.TERMINE);
    }

    @Override
    public String toString() {
        return nom + " (" + matchs.size() + " matchs)";
    }
}
