package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Une poule vue depuis la salle, comme <b>unité de lancement</b> (phase de
 * poules) : à lancer (interface « Lancer un match ») ou en cours sur sa table
 * (carte de la table). Calculée par {@link ServiceSalle}, immuable et sans JavaFX.
 *
 * <p>Les joueurs sont dans l'ordre de la poule (numéro 1 = tête de poule, le
 * mieux classé au placement) ; les matchs dans l'ordre officiel I.301 où ils se
 * jouent à la suite sur la table (exempts exclus).</p>
 *
 * @param poule       la poule du tournoi
 * @param tableau     nom du tableau
 * @param joueurs     joueurs dans l'ordre de la poule (numéro = rang + 1)
 * @param matchs      matchs dans l'ordre où ils se jouent (I.301), exempts exclus
 * @param table       table de la poule lancée, ou null si elle est à lancer
 * @param lancement   heure de lancement, ou null
 * @param dureePrevue somme des durées prévues de ses matchs sans résultat
 * @param finEstimee  fin estimée d'une poule en cours (table et joueurs libres), ou null
 */
public record PouleALancer(Poule poule, String tableau, List<Joueur> joueurs, List<Match> matchs, Integer table,
                           LocalDateTime lancement, Duration dureePrevue, LocalDateTime finEstimee) {

    public PouleALancer {
        Objects.requireNonNull(poule, "poule");
        tableau = tableau == null ? "" : tableau;
        joueurs = List.copyOf(joueurs);
        matchs = List.copyOf(matchs);
        dureePrevue = dureePrevue == null ? Duration.ZERO : dureePrevue;
    }

    /** « Poule 3 ». */
    public String libelle() {
        return "Poule " + poule.getNom();
    }

    /** Numéro du joueur dans la poule (1, 2…), 0 s'il n'y est pas. */
    public int numero(Joueur j) {
        return joueurs.indexOf(j) + 1;
    }

    /** Nombre de matchs qui restent à jouer (sans résultat). */
    public int nombreMatchs() {
        return (int) matchs.stream().filter(m -> m.getStatut() != StatutMatch.TERMINE).count();
    }

    /** Nombre de matchs déjà joués (avec un résultat). */
    public int nombreJoues() {
        return matchs.size() - nombreMatchs();
    }

    /** Match qui se joue sur la table : le premier sans résultat dans l'ordre I.301, ou null. */
    public Match matchEnJeu() {
        return matchs.stream().filter(m -> m.getStatut() != StatutMatch.TERMINE).findFirst().orElse(null);
    }

    /** La poule est-elle lancée sur une table ? */
    public boolean estLancee() {
        return table != null;
    }
}
