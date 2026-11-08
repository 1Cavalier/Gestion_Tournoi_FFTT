package com.pingmanager.core;

import com.pingmanager.model.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Annonce vocale d'une rencontre envoyée sur une table (B017), découpée en
 * morceaux ({@link SegmentAnnonce}) pour la prononciation.
 *
 * @param match    rencontre annoncée
 * @param table    numéro de la table
 * @param segments morceaux, dans l'ordre de lecture
 * @param rappel   rappel (les joueurs ne sont pas encore à la table) ?
 * @param licences licence du joueur de chaque morceau (même taille que
 *                 {@code segments}, vide pour un texte libre) : sert au
 *                 registre des prononciations (B018)
 */
public record AnnonceVocale(Match match, int table, List<SegmentAnnonce> segments, boolean rappel,
                            List<String> licences) {

    public AnnonceVocale {
        Objects.requireNonNull(match, "match");
        segments = List.copyOf(segments);
        if (licences == null) {
            licences = java.util.Collections.nCopies(segments.size(), "");
        } else if (licences.size() != segments.size()) {
            throw new IllegalArgumentException("une licence (éventuellement vide) par morceau");
        } else {
            licences = licences.stream().map(l -> l == null ? "" : l).toList();
        }
    }

    /** Annonce sans licence connue pour ses morceaux. */
    public AnnonceVocale(Match match, int table, List<SegmentAnnonce> segments, boolean rappel) {
        this(match, table, segments, rappel, null);
    }

    /** Licence du joueur du morceau n° {@code index} (vide si texte libre ou inconnue). */
    public String licenceDu(int index) {
        return licences.get(index);
    }

    /** Texte complet, tel qu'il est affiché à l'écran (repli) et dit par défaut. */
    public String texte() {
        StringBuilder sb = new StringBuilder();
        segments.forEach(s -> sb.append(s.texte()));
        return sb.toString();
    }

    /** La même annonce, précédée de « Rappel : ». */
    public AnnonceVocale enRappel() {
        if (rappel) {
            return this;
        }
        List<SegmentAnnonce> s = new ArrayList<>();
        s.add(SegmentAnnonce.libre("Rappel : "));
        s.addAll(segments);
        List<String> l = new ArrayList<>();
        l.add("");
        l.addAll(licences);
        return new AnnonceVocale(match, table, s, true, l);
    }
}
