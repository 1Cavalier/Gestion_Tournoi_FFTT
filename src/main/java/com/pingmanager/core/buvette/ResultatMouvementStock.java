package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.MouvementStock;

import java.util.List;

/**
 * Résultat d'une entrée, d'une sortie ou d'un recomptage du stock de la
 * buvette (B026).
 *
 * @param motif      ce qui s'est passé
 * @param mouvements les lignes ajoutées au journal (vide en cas de refus)
 * @param message    phrase à afficher (« Café : 1 portion sortie… »)
 * @param epuises    noms des articles qui manquaient : leur stock est ramené à 0
 *                   au lieu de devenir négatif (vide si aucun)
 */
public record ResultatMouvementStock(Motif motif, List<MouvementStock> mouvements, String message,
                                     List<String> epuises) {

    /** Motifs possibles. */
    public enum Motif {
        ENREGISTRE(true, "Stock mis à jour."),
        INCONNU(false, "Article introuvable."),
        QUANTITE_INVALIDE(false, "La quantité doit être un nombre entier d'au moins 1."),
        COMPTE_INVALIDE(false, "Le recomptage doit être un nombre entier positif ou nul.");

        private final boolean accepte;
        private final String explication;

        Motif(boolean accepte, String explication) {
            this.accepte = accepte;
            this.explication = explication;
        }
    }

    public ResultatMouvementStock {
        mouvements = List.copyOf(mouvements);
        epuises = List.copyOf(epuises);
    }

    static ResultatMouvementStock refus(Motif m) {
        return new ResultatMouvementStock(m, List.of(), m.explication, List.of());
    }

    public boolean accepte() {
        return motif.accepte;
    }

    /** Phrase à afficher, avec l'alerte de recomptage si un article manquait. */
    public String explication() {
        if (epuises.isEmpty()) {
            return message;
        }
        return message + " Plus assez de " + String.join(", ", epuises)
                + " en stock (ramené à 0) : pensez à recompter.";
    }
}
