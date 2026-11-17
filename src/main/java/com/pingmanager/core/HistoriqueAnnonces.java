package com.pingmanager.core;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Dernières annonces traitées par la voix (B056), la plus récente d'abord, pour
 * les réannoncer depuis le panneau « Annonces » du jour J. Gardé en mémoire
 * seulement (rien n'est enregistré avec le tournoi) et borné à
 * {@link #CAPACITE} annonces. Utilisable depuis plusieurs fils.
 */
public final class HistoriqueAnnonces {

    /** Nombre d'annonces gardées. */
    public static final int CAPACITE = 10;

    /**
     * Une annonce de l'historique.
     *
     * @param instant moment où elle a été traitée
     * @param annonce annonce (rencontre, table, texte)
     * @param dite    dite par la voix, ou seulement affichée à l'écran
     */
    public record Entree(Instant instant, AnnonceVocale annonce, boolean dite) {
        public Entree {
            Objects.requireNonNull(instant, "instant");
            Objects.requireNonNull(annonce, "annonce");
        }
    }

    private final Deque<Entree> entrees = new ArrayDeque<>();

    /** Note l'issue d'une annonce ; la plus ancienne sort au-delà de {@link #CAPACITE}. */
    public synchronized void noter(ResultatAnnonceVocale resultat, Instant instant) {
        Objects.requireNonNull(resultat, "resultat");
        entrees.addFirst(new Entree(instant, resultat.annonce(), resultat.dite()));
        while (entrees.size() > CAPACITE) {
            entrees.removeLast();
        }
    }

    /** @return les annonces gardées, la plus récente d'abord (copie). */
    public synchronized List<Entree> dernieres() {
        return new ArrayList<>(entrees);
    }
}
