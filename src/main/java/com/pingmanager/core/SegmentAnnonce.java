package com.pingmanager.core;

import java.util.Objects;

/**
 * Morceau d'une annonce vocale (B017) : texte libre, prénom, nom ou club d'un
 * joueur. Le découpage permet de prononcer un nom à part (B018 : prononciation
 * phonétique personnalisée) sans toucher au reste de la phrase.
 *
 * @param texte  texte affiché et dit par défaut
 * @param nature nature du morceau
 */
public record SegmentAnnonce(String texte, Nature nature) {

    /** Nature d'un morceau d'annonce. */
    public enum Nature {
        /** Mots de liaison, numéro de table, tableau et tour. */
        LIBRE,
        /** Prénom d'un joueur. */
        PRENOM,
        /** Nom de famille d'un joueur. */
        NOM,
        /** Nom du club d'un joueur. */
        CLUB
    }

    public SegmentAnnonce {
        Objects.requireNonNull(texte, "texte");
        Objects.requireNonNull(nature, "nature");
    }

    static SegmentAnnonce libre(String texte) {
        return new SegmentAnnonce(texte, Nature.LIBRE);
    }
}
