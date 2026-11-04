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
        Duration duree = estimateur.estimer(categorieDe.apply(mo));
        return m.getInstantLancement().plus(duree);
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
