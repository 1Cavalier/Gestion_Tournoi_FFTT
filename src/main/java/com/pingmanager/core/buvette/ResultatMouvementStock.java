package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.MouvementStock;

import java.util.List;

/**
 * Résultat d'une saisie de stock de la buvette (B046).
 *
 * @param motif      ce qui s'est passé
 * @param mouvements les mouvements enregistrés (vide en cas de refus)
 */
public record ResultatMouvementStock(Motif motif, List<MouvementStock> mouvements) {

    /** Motifs possibles. */
    public enum Motif {
        ENREGISTRE(true, "Mouvement enregistré."),
        INGREDIENT_INCONNU(false, "Choisissez un ingrédient du catalogue."),
        QUANTITE_INVALIDE(false, "La quantité doit être un nombre entier positif."),
        PRIX_INVALIDE(false, "Le prix payé doit être un montant positif, par exemple 11,00."),
        MOUVEMENT_INCONNU(false, "Mouvement introuvable."),
        DEJA_CORRIGE(false, "Ce mouvement a déjà été corrigé."),
        CORRECTION_NON_CORRIGEABLE(false, "Une correction ne se corrige pas : saisissez un nouveau mouvement."),
        VENTE_A_ANNULER_EN_CAISSE(false, "Une vente se corrige en l'annulant depuis l'onglet Ventes."),
        RIEN_A_REPORTER(false, "Ce tournoi n'a aucun stock restant à reporter."),
        DEJA_REPORTE(false, "Le stock de ce tournoi a déjà été reporté ici.");

        private final boolean accepte;
        private final String explication;

        Motif(boolean accepte, String explication) {
            this.accepte = accepte;
            this.explication = explication;
        }

        public boolean accepte() {
            return accepte;
        }

        public String explication() {
            return explication;
        }
    }

    public ResultatMouvementStock {
        mouvements = List.copyOf(mouvements);
    }

    public boolean accepte() {
        return motif.accepte();
    }

    public String explication() {
        return motif.explication();
    }

    static ResultatMouvementStock refus(Motif m) {
        return new ResultatMouvementStock(m, List.of());
    }

    static ResultatMouvementStock ok(List<MouvementStock> m) {
        return new ResultatMouvementStock(Motif.ENREGISTRE, m);
    }
}
