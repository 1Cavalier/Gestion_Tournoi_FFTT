package com.pingmanager.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Un carton reçu par un joueur, conservé dans l'historique disciplinaire.
 *
 * <p>Enregistre la couleur, le joueur sanctionné, un éventuel nombre de points
 * de pénalité accordés à l'adversaire (pour un jaune+rouge), le motif, l'instant,
 * et si le carton a été reçu <b>hors d'un match</b> (dans la salle, entre deux
 * parties…). Les cartons hors match existent : le juge-arbitre peut sanctionner
 * un comportement même en dehors d'une partie.</p>
 */
public final class Carton {

    private final Joueur joueur;
    private final CouleurCarton couleur;
    private final int pointsPenalite;   // points accordés à l'adversaire (0 pour un simple jaune)
    private final boolean horsMatch;
    private final String motif;
    private final LocalDateTime instant;

    public Carton(Joueur joueur, CouleurCarton couleur, int pointsPenalite,
                  boolean horsMatch, String motif, LocalDateTime instant) {
        this.joueur = Objects.requireNonNull(joueur, "joueur");
        this.couleur = Objects.requireNonNull(couleur, "couleur");
        if (pointsPenalite < 0) {
            throw new IllegalArgumentException("Les points de pénalité ne peuvent pas être négatifs.");
        }
        this.pointsPenalite = pointsPenalite;
        this.horsMatch = horsMatch;
        this.motif = motif == null ? "" : motif;
        this.instant = Objects.requireNonNull(instant, "instant");
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public CouleurCarton getCouleur() {
        return couleur;
    }

    public int getPointsPenalite() {
        return pointsPenalite;
    }

    public boolean estHorsMatch() {
        return horsMatch;
    }

    public String getMotif() {
        return motif;
    }

    public LocalDateTime getInstant() {
        return instant;
    }

    /** @return true si ce carton est une disqualification (rouge du juge-arbitre). */
    public boolean estDisqualifiant() {
        return couleur == CouleurCarton.ROUGE;
    }

    @Override
    public String toString() {
        String lieu = horsMatch ? "hors match" : "en match";
        return couleur + " pour " + joueur.getNomComplet() + " (" + lieu + ")"
                + (pointsPenalite > 0 ? " +" + pointsPenalite + " pt(s) adverse" : "");
    }
}
