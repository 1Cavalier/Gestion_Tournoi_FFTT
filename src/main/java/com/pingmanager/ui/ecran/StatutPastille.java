package com.pingmanager.ui.ecran;

import com.pingmanager.model.StatutTournoi;
import java.util.EnumMap;
import java.util.Map;

/**
 * Statut d'une pastille (charte v2) : chaque valeur porte sa classe de style,
 * définie dans {@code theme.css} (fond doux + texte foncé, dans les deux thèmes).
 *
 * <p>Pas de {@code switch} sur cette énumération (ni sur {@link StatutTournoi}) :
 * la classe CSS est portée par la constante et les correspondances passent par
 * des chaînes de {@code if}. Aucune dépendance JavaFX : testable seule.</p>
 */
enum StatutPastille {

    SUCCES("pastille-succes"),
    ATTENTION("pastille-attention"),
    ERREUR("pastille-erreur"),
    INFO("pastille-info"),
    NEUTRE("pastille-neutre");

    private final String classeCss;

    StatutPastille(String classeCss) {
        this.classeCss = classeCss;
    }

    /** Classe de style à poser sur la pastille. */
    String classeCss() {
        return classeCss;
    }

    /** Couleur de pastille d'un statut de tournoi (cycle brouillon → publié). */
    static StatutPastille pourTournoi(StatutTournoi statut) {
        if (statut == StatutTournoi.PUBLIE) {
            return SUCCES;
        }
        if (statut == StatutTournoi.HOMOLOGUE || statut == StatutTournoi.DEMANDE_HOMOLOGATION) {
            return INFO;
        }
        if (statut == StatutTournoi.ANNULE) {
            return ERREUR;
        }
        return NEUTRE;
    }

    private static final Map<StatutTournoi, String> LIBELLES_TOURNOI = new EnumMap<>(Map.of(
            StatutTournoi.PUBLIE, "Publié",
            StatutTournoi.HOMOLOGUE, "Homologué",
            StatutTournoi.DEMANDE_HOMOLOGATION, "Homologation demandée",
            StatutTournoi.ANNULE, "Annulé"));

    /** Libellé court d'un statut de tournoi, pour une pastille. */
    static String libelleTournoi(StatutTournoi statut) {
        return LIBELLES_TOURNOI.getOrDefault(statut, "Brouillon");
    }
}
