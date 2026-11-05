package com.pingmanager.core;

import com.pingmanager.model.Joueur;

import java.util.List;
import java.util.Objects;

/**
 * Réponse de {@link ServiceLancementTableau#verifierLancement} : le tableau
 * peut-il être lancé maintenant, et sinon pourquoi (motif précis). Objet valeur
 * immuable, sans effet sur le tableau.
 */
public final class ResultatVerificationLancement {

    /** Raison pour laquelle le lancement est possible ou non. */
    public enum Motif {
        /** Tout est prêt : le tirage peut être fait. */
        PRET,
        /** Le tableau n'appartient pas au tournoi du service. */
        HORS_TOURNOI,
        /** Le statut du tournoi interdit le lancement (annulé, ou non publié en mode strict). */
        TOURNOI_FERME,
        /** Le tableau est encore en inscription : le pointage n'a pas été ouvert. */
        POINTAGE_NON_OUVERT,
        /** Le pointage est ouvert mais pas clôturé. */
        POINTAGE_NON_CLOTURE,
        /** Le tableau est déjà lancé (poules ou phase finale). */
        DEJA_LANCE,
        /** Le tableau n'a pas de format de compétition. */
        SANS_FORMAT,
        /** Moins de présents que la taille d'une poule du format. */
        PAS_ASSEZ_DE_PRESENTS
    }

    private final Motif motif;
    private final int presents;
    private final int minimum;
    private final List<Joueur> nonPointes;

    ResultatVerificationLancement(Motif motif, int presents, int minimum, List<Joueur> nonPointes) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.presents = presents;
        this.minimum = minimum;
        this.nonPointes = List.copyOf(nonPointes);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return true si {@link ServiceLancementTableau#lancer} accepterait le tableau. */
    public boolean estPret() {
        return motif == Motif.PRET;
    }

    /** @return le nombre d'inscrits restants (ceux qui seraient placés en poule). */
    public int getPresents() {
        return presents;
    }

    /** @return le nombre minimal de présents exigé par le format (taille d'une poule). */
    public int getMinimum() {
        return minimum;
    }

    /**
     * @return les inscrits qui n'ont pas pointé (en pratique, les repêchés « à
     *         pointer » après la clôture). Le lancement les placerait en poule :
     *         le juge-arbitre devrait les confirmer ou les retirer avant.
     */
    public List<Joueur> getNonPointes() {
        return nonPointes;
    }

    @Override
    public String toString() {
        return "ResultatVerificationLancement[" + motif + ", présents=" + presents
                + ", non pointés=" + nonPointes.size() + "]";
    }
}
