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
 */
public record AnnonceVocale(Match match, int table, List<SegmentAnnonce> segments, boolean rappel) {

    public AnnonceVocale {
        Objects.requireNonNull(match, "match");
        segments = List.copyOf(segments);
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
        return new AnnonceVocale(match, table, s, true);
    }
}
