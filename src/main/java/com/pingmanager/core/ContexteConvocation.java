package com.pingmanager.core;

import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Où se joue un match, pour le texte d'une convocation par SMS (B019) :
 * « Tableau C, poule B », « Tableau C, 8es de finale » ou « Tableau C, match de
 * classement (places 17 à 32) ».
 *
 * @param type    match de poule, tour de tableau ou match de classement
 * @param tableau nom du tableau
 * @param detail  poule, tour ou places
 */
public record ContexteConvocation(TypeConvocation type, String tableau, String detail) {

    public ContexteConvocation {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(detail, "detail");
        if (type == TypeConvocation.ARBITRAGE) {
            throw new IllegalArgumentException("Le contexte d'un match ne peut pas être « arbitrage ».");
        }
    }

    /** Match de poule : « poule B ». */
    public static ContexteConvocation poule(String tableau, String nomPoule) {
        String n = nomPoule.trim();
        String detail = n.toLowerCase(Locale.ROOT).startsWith("poule") ? "poule" + n.substring(5) : "poule " + n;
        return new ContexteConvocation(TypeConvocation.MATCH_POULE, tableau, detail);
    }

    /** Tour d'un tableau à élimination : « 8es de finale », « finale ». */
    public static ContexteConvocation tour(String tableau, String nomTour) {
        String n = nomTour.trim();
        String detail = n.isEmpty() ? n : Character.toLowerCase(n.charAt(0)) + n.substring(1);
        return new ContexteConvocation(TypeConvocation.TOUR_TABLEAU, tableau, detail);
    }

    /** Match de classement : « match de classement (places 17 à 32) ». */
    public static ContexteConvocation classement(String tableau, int placeMin, int placeMax) {
        if (placeMin < 1 || placeMax < placeMin) {
            throw new IllegalArgumentException("Places de classement incohérentes : " + placeMin + " à " + placeMax);
        }
        return new ContexteConvocation(TypeConvocation.MATCH_CLASSEMENT, tableau,
                "match de classement (places " + placeMin + " à " + placeMax + ")");
    }

    /**
     * Retrouve le tableau et la poule ou le tour d'un match du tournoi ; vide si
     * le match n'appartient à aucun tableau. Les matchs de classement ne sont pas
     * encore modélisés : ils seront reconnus quand ils le seront.
     */
    public static Optional<ContexteConvocation> de(Tournoi tournoi, Match match) {
        for (Tableau t : tournoi.getTableaux()) {
            for (Poule p : t.getPoules()) {
                if (p.getMatchs().contains(match)) {
                    return Optional.of(poule(t.getNom(), p.getNom()));
                }
            }
            if (t.aPhaseFinale()) {
                for (Tour tour : t.getPhaseFinale().getTours()) {
                    if (tour.getMatchs().contains(match)) {
                        return Optional.of(tour(t.getNom(), tour.getNom()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** « Tableau C, poule B » (sans répéter « Tableau » si le nom le contient déjà). */
    public String libelle() {
        return libelle(tableau);
    }

    String libelle(String nomTableau) {
        String t = nomTableau.trim();
        String prefixe = t.toLowerCase(Locale.ROOT).startsWith("tableau") ? "" : "Tableau ";
        return prefixe + t + (detail.isEmpty() ? "" : ", " + detail);
    }
}
