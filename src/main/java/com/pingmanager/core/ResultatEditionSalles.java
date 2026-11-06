package com.pingmanager.core;

import java.util.Objects;

/**
 * Résultat d'une opération sur la liste des salles d'un tournoi (B024) :
 * ajouter, renommer, retirer une salle, changer son étage ou ses réservations.
 */
public final class ResultatEditionSalles {

    /** Issue de l'opération. Les trois premiers motifs sont des réussites. */
    public enum Motif {
        SALLE_AJOUTEE,
        SALLE_RETIREE,
        SALLE_MODIFIEE,
        /** Le nom de la salle est vide. */
        NOM_VIDE,
        /** Une autre salle porte déjà ce nom. */
        NOM_DEJA_PRIS,
        /** Le tournoi garde toujours au moins une salle. */
        DERNIERE_SALLE,
        /** Aucune salle à cette position. */
        SALLE_INTROUVABLE,
        /** Les tableaux nommés n'existent pas dans le tournoi. */
        TABLEAU_INCONNU
    }

    private final Motif motif;
    private final String precision;

    private ResultatEditionSalles(Motif motif, String precision) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.precision = precision == null ? "" : precision;
    }

    public static ResultatEditionSalles de(Motif motif) {
        return new ResultatEditionSalles(motif, null);
    }

    /** @param precision nom de la salle ou du tableau en cause */
    public static ResultatEditionSalles de(Motif motif, String precision) {
        return new ResultatEditionSalles(motif, precision);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return le nom de la salle ou du tableau en cause, sinon une chaîne vide. */
    public String getPrecision() {
        return precision;
    }

    public boolean estReussi() {
        return motif == Motif.SALLE_AJOUTEE || motif == Motif.SALLE_RETIREE || motif == Motif.SALLE_MODIFIEE;
    }

    @Override
    public String toString() {
        return motif + (precision.isEmpty() ? "" : " (" + precision + ")");
    }
}
