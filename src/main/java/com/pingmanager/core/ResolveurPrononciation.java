package com.pingmanager.core;

import java.util.Optional;

/**
 * Point d'accroche de la prononciation des noms dans les annonces vocales
 * (B017, complété par B018) : pour un morceau d'annonce (prénom, nom, club),
 * rend sa prononciation en alphabet phonétique international (API / IPA), ou
 * rien pour laisser la voix lire le texte normal.
 *
 * <p>Par défaut ({@link #TEXTE_NORMAL}) rien n'est personnalisé.</p>
 */
@FunctionalInterface
public interface ResolveurPrononciation {

    /** Aucune prononciation personnalisée : la voix lit le texte. */
    ResolveurPrononciation TEXTE_NORMAL = segment -> Optional.empty();

    /**
     * @param segment morceau d'annonce
     * @return la prononciation en IPA (par exemple « ʒɑ̃ dy.pɔ̃ »), ou vide
     */
    Optional<String> phonemesIpa(SegmentAnnonce segment);

    /**
     * Variante qui connaît la licence du joueur auquel appartient le morceau
     * (B018 : registre des prononciations). Par défaut, ignore la licence.
     *
     * @param licence licence du joueur du morceau, ou vide (texte libre)
     */
    default Optional<String> phonemesIpa(SegmentAnnonce segment, String licence) {
        return phonemesIpa(segment);
    }
}
