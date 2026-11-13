package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.Ingredient;

/**
 * Résultat d'une opération sur les ingrédients de la buvette (B046).
 *
 * @param motif      ce qui s'est passé
 * @param ingredient l'ingrédient concerné, ou {@code null}
 */
public record ResultatIngredientBuvette(Motif motif, Ingredient ingredient) {

    /** Motifs possibles. */
    public enum Motif {
        ENREGISTRE(true, "Ingrédient enregistré."),
        SUPPRIME(true, "Ingrédient retiré."),
        DESACTIVE_DEJA_UTILISE(true, "Cet ingrédient sert déjà (recette ou stock) : il est désactivé plutôt que supprimé."),
        NOM_MANQUANT(false, "Indiquez le nom de l'ingrédient."),
        NOM_EN_DOUBLE(false, "Un ingrédient porte déjà ce nom."),
        PRIX_INVALIDE(false, "Le prix d'achat doit être un montant positif, par exemple 1,10."),
        RENDEMENT_INVALIDE(false, "Le rendement est un nombre entier d'au moins 1, le minimum ne dépassant pas le maximum."),
        PERTE_INVALIDE(false, "La perte est un pourcentage entier entre 0 et 90."),
        DEJA_PRESENT(false, "Cette suggestion est déjà dans vos ingrédients."),
        INCONNU(false, "Ingrédient introuvable.");

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

    public boolean accepte() {
        return motif.accepte();
    }

    public String explication() {
        return motif.explication();
    }

    static ResultatIngredientBuvette refus(Motif m) {
        return new ResultatIngredientBuvette(m, null);
    }
}
