package com.pingmanager.core;

import java.util.Collections;
import java.util.List;

/**
 * Résultat d'une tentative de transition dans le cycle de vie d'un tournoi,
 * passée par le {@link ServiceCycleTournoi}.
 *
 * <p>Une transition peut aboutir (le tournoi change de statut) ou être refusée
 * parce que le <b>dossier est incomplet</b>. Dans ce dernier cas, le résultat
 * porte la <b>liste des éléments manquants</b>, en clair, pour que l'appelant
 * (l'UI) puisse guider l'organisateur — c'est bien plus utile qu'une simple
 * exception. Les transitions <i>structurellement</i> impossibles (par exemple
 * publier un brouillon) relèvent, elles, d'une {@link IllegalStateException}
 * levée en amont : ce sont des erreurs de programmation, pas des cas métier.</p>
 *
 * <p>Objet valeur immuable, obtenu via les fabriques statiques.</p>
 */
public final class ResultatTransition {

    /** Motif de l'issue d'une transition. */
    public enum Motif {
        /** La transition a été appliquée : le tournoi a changé de statut. */
        EFFECTUEE,
        /** Refus : des éléments obligatoires manquent (voir {@link #getManquants()}). */
        DOSSIER_INCOMPLET,
        /** Refus d'homologation sans motif : le motif est obligatoire. */
        MOTIF_REFUS_ABSENT
    }

    private final Motif motif;
    private final List<String> manquants;

    private ResultatTransition(Motif motif, List<String> manquants) {
        this.motif = motif;
        this.manquants = manquants == null
                ? Collections.emptyList()
                : List.copyOf(manquants);
    }

    public static ResultatTransition effectuee() {
        return new ResultatTransition(Motif.EFFECTUEE, Collections.emptyList());
    }

    public static ResultatTransition dossierIncomplet(List<String> manquants) {
        return new ResultatTransition(Motif.DOSSIER_INCOMPLET, manquants);
    }

    /** Le refus d'homologation n'a pas été enregistré : aucun motif saisi. */
    public static ResultatTransition motifRefusAbsent() {
        return new ResultatTransition(Motif.MOTIF_REFUS_ABSENT,
                List.of("motif du refus d'homologation"));
    }

    public Motif getMotif() {
        return motif;
    }

    /**
     * @return les éléments manquants (motifs {@link Motif#DOSSIER_INCOMPLET} et
     *         {@link Motif#MOTIF_REFUS_ABSENT}) ;
     *         liste vide sinon. Jamais {@code null}.
     */
    public List<String> getManquants() {
        return manquants;
    }

    /** @return true si la transition a été appliquée. */
    public boolean estEffectuee() {
        return motif == Motif.EFFECTUEE;
    }

    @Override
    public String toString() {
        return motif == Motif.EFFECTUEE
                ? "ResultatTransition[EFFECTUEE]"
                : "ResultatTransition[" + motif.name() + ", manquants=" + manquants + "]";
    }
}
