package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fournit des <b>prévisions à court terme</b> sur le déroulé en salle, à partir
 * des matchs en cours, de leur heure de lancement et de l'{@link EstimateurDuree}.
 *
 * <p>Permet de répondre à : quand cette table va-t-elle se libérer ? quand ce
 * joueur occupé sera-t-il de nouveau disponible ? C'est la base de l'affichage
 * (fins probables) et, ensuite, de l'anticipation (préparer le prochain match
 * d'un joueur clé dès qu'il a fini).</p>
 *
 * <p><b>Poules lancées en bloc.</b> Une poule occupe sa table jusqu'à son
 * dernier match : sa durée prévue est la somme des durées prévues de ses
 * matchs, et ses matchs se jouent à la suite (I.301). La table et tous les
 * joueurs de la poule sont libres à la fin estimée du dernier match, comptée à
 * partir du dernier résultat saisi (sinon de l'heure de lancement).</p>
 */
public final class PrevisionSalle {

    private final Ordonnanceur ordonnanceur;
    private final EstimateurDuree estimateur;

    /** Fonction associant un match à sa catégorie de durée (fournie par l'appelant). */
    private final java.util.function.Function<MatchOrdonnancable, CategorieDuree> categorieDe;

    public PrevisionSalle(Ordonnanceur ordonnanceur, EstimateurDuree estimateur,
                          java.util.function.Function<MatchOrdonnancable, CategorieDuree> categorieDe) {
        this.ordonnanceur = Objects.requireNonNull(ordonnanceur, "ordonnanceur");
        this.estimateur = Objects.requireNonNull(estimateur, "estimateur");
        this.categorieDe = Objects.requireNonNull(categorieDe, "categorieDe");
    }

    /**
     * Estime l'heure de fin d'un match en cours : heure de lancement + durée
     * estimée de sa catégorie. Retourne {@code null} si le match n'est pas
     * lancé.
     */
    public LocalDateTime finEstimee(MatchOrdonnancable mo) {
        Objects.requireNonNull(mo, "mo");
        Match m = mo.getMatch();
        if (m.getInstantLancement() == null || m.getStatut() != StatutMatch.EN_COURS) {
            return null;
        }
        PouleOrdonnancable p = ordonnanceur.pouleDe(mo);
        if (p != null && p.estEnCours()) {
            // Matchs à la suite sur la table de la poule : fin de celui-ci dans l'enchaînement.
            LocalDateTime t = debutRestant(p);
            for (MatchOrdonnancable x : p.sansResultat()) {
                t = t.plus(duree(x));
                if (x == mo) {
                    return t;
                }
            }
            return null;
        }
        return m.getInstantLancement().plus(duree(mo));
    }

    private Duration duree(MatchOrdonnancable mo) {
        return estimateur.estimer(categorieDe.apply(mo));
    }

    /** Durée prévue d'une poule : somme des durées prévues de ses matchs sans résultat. */
    public Duration dureePrevue(PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        return p.dureePrevue(this::duree);
    }

    /**
     * Fin estimée d'une poule lancée : à partir de son dernier résultat saisi
     * (sinon de son lancement), la somme des durées prévues des matchs qui
     * restent. {@code null} si la poule n'est pas en cours.
     */
    public LocalDateTime finEstimee(PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        if (!p.estEnCours()) {
            return null;
        }
        return debutRestant(p).plus(dureePrevue(p));
    }

    /** Lancement de la poule, ou fin du dernier résultat saisi depuis s'il est plus tard. */
    private static LocalDateTime debutRestant(PouleOrdonnancable p) {
        LocalDateTime debut = p.getPoule().getInstantLancement();
        for (MatchOrdonnancable mo : p.getMatchs()) {
            LocalDateTime fin = mo.getMatch().getInstantFin();
            if (mo.getMatch().getStatut() == StatutMatch.TERMINE && fin != null && fin.isAfter(debut)) {
                debut = fin;
            }
        }
        return debut;
    }

    /**
     * Temps restant estimé avant la fin d'un match en cours, à l'instant donné.
     * Peut être négatif si le match dépasse déjà son estimation (retourne alors
     * une durée nulle bornée à zéro).
     */
    public Duration tempsRestant(MatchOrdonnancable mo, LocalDateTime maintenant) {
        LocalDateTime fin = finEstimee(mo);
        if (fin == null) {
            return null;
        }
        Duration restant = Duration.between(maintenant, fin);
        return restant.isNegative() ? Duration.ZERO : restant;
    }

    /**
     * Pour chaque table occupée, l'heure de libération estimée. Les tables
     * libres ne figurent pas dans la carte (elles sont déjà disponibles).
     */
    public Map<Integer, LocalDateTime> liberationDesTables() {
        Map<Integer, LocalDateTime> resultat = new HashMap<>();
        for (int table = 1; table <= ordonnanceur.getNombreDeTables(); table++) {
            PouleOrdonnancable p = ordonnanceur.pouleSurTable(table);
            if (p != null) {
                LocalDateTime fin = finEstimee(p);
                if (fin != null) {
                    resultat.put(table, fin);
                }
                continue;
            }
            MatchOrdonnancable mo = ordonnanceur.matchSurTable(table);
            if (mo != null) {
                LocalDateTime fin = finEstimee(mo);
                if (fin != null) {
                    resultat.put(table, fin);
                }
            }
        }
        return resultat;
    }

    /**
     * Heure estimée à laquelle un joueur occupé redeviendra disponible (fin de
     * son match en cours). {@code null} si le joueur n'est pas en train de jouer.
     */
    public LocalDateTime disponibiliteJoueur(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        for (int table = 1; table <= ordonnanceur.getNombreDeTables(); table++) {
            PouleOrdonnancable p = ordonnanceur.pouleSurTable(table);
            if (p != null) {
                if (p.contient(joueur.getNumeroLicence())) {
                    // Tous les joueurs de la poule restent occupés jusqu'à son dernier match.
                    return finEstimee(p);
                }
                continue;
            }
            MatchOrdonnancable mo = ordonnanceur.matchSurTable(table);
            if (mo == null) continue;
            Match m = mo.getMatch();
            boolean joueDansCeMatch =
                    (m.getJoueurA() != null && m.getJoueurA().equals(joueur))
                    || (m.getJoueurB() != null && m.getJoueurB().equals(joueur));
            if (joueDansCeMatch) {
                return finEstimee(mo);
            }
        }
        return null;
    }
}
