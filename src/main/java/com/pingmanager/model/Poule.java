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

    /**
     * Lancement de la poule entière sur une table (jour J) : la table et l'heure,
     * posées par le service de la salle, effacées à la libération (dernier
     * résultat) ou à l'annulation. {@code null} : poule pas lancée en bloc
     * (anciens fichiers : matchs lancés un par un).
     */
    private Integer numeroTable;
    private java.time.LocalDateTime instantLancement;

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

    /** @return la table de la poule lancée en bloc, ou {@code null}. */
    public Integer getNumeroTable() {
        return numeroTable;
    }

    /** @return l'heure de lancement de la poule entière, ou {@code null}. */
    public java.time.LocalDateTime getInstantLancement() {
        return instantLancement;
    }

    /** @return true si la poule a été lancée en bloc sur une table et n'est pas encore libérée. */
    public boolean estLanceeEnBloc() {
        return numeroTable != null && instantLancement != null;
    }

    /**
     * Pose (ou efface, avec deux {@code null}) le lancement de la poule entière.
     * Réservé au service de la salle et à la relecture d'un fichier : aucune
     * vérification ici (le cœur vérifie table libre et joueurs libres).
     */
    public void definirLancement(Integer numeroTable, java.time.LocalDateTime instantLancement) {
        if ((numeroTable == null) != (instantLancement == null)) {
            throw new IllegalArgumentException("Table et heure de lancement vont ensemble.");
        }
        this.numeroTable = numeroTable;
        this.instantLancement = instantLancement;
    }

    @Override
    public String toString() {
        return "Poule " + nom + " (" + joueurs.size() + " joueurs)";
    }
}
