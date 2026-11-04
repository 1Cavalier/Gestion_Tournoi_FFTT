package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Une poule : un petit groupe de joueurs (typiquement 3 ou 4) qui se
 * rencontrent tous entre eux (championnat toutes rondes).
 *
 * <p>Conteneur de données : la Poule connaît ses joueurs et ses matchs mais
 * ne calcule pas elle-même le classement. Le calcul du classement est la
 * responsabilité d'un service du package {@code core}, afin de garder la
 * logique métier séparée des données.</p>
 */
public final class Poule {

    private final String nom;
    private final List<Joueur> joueurs = new ArrayList<>();
    private final List<Match> matchs = new ArrayList<>();

    public Poule(String nom, List<Joueur> joueurs) {
        this.nom = Objects.requireNonNull(nom, "nom");
        this.joueurs.addAll(Objects.requireNonNull(joueurs, "joueurs"));
    }

    public String getNom() {
        return nom;
    }

    public List<Joueur> getJoueurs() {
        return Collections.unmodifiableList(joueurs);
    }

    public List<Match> getMatchs() {
        return Collections.unmodifiableList(matchs);
    }

    public void ajouterMatch(Match match) {
        matchs.add(Objects.requireNonNull(match, "match"));
    }

    public int getTaille() {
        return joueurs.size();
    }

    /** @return true si tous les matchs de la poule sont terminés. */
    public boolean estTerminee() {
        return !matchs.isEmpty()
                && matchs.stream().allMatch(m -> m.getStatut() == StatutMatch.TERMINE);
    }

    @Override
    public String toString() {
        return "Poule " + nom + " (" + joueurs.size() + " joueurs)";
    }
}
