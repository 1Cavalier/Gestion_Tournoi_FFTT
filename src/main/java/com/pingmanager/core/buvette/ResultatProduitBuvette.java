package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.Produit;

/**
 * Résultat d'une opération sur le catalogue de la buvette (B046), avec un motif
 * précis plutôt qu'un booléen.
 *
 * @param motif   ce qui s'est passé
 * @param produit le produit concerné (enregistré, désactivé…), ou {@code null}
 */
public record ResultatProduitBuvette(Motif motif, Produit produit) {

    /** Motifs possibles. */
    public enum Motif {
        ENREGISTRE(true, "Produit enregistré."),
        SUPPRIME(true, "Produit retiré du catalogue."),
        DESACTIVE_DEJA_VENDU(true, "Ce produit a déjà été vendu : il est désactivé plutôt que supprimé."),
        NOM_MANQUANT(false, "Indiquez le nom du produit."),
        NOM_EN_DOUBLE(false, "Un produit porte déjà ce nom dans le catalogue."),
        PRIX_INVALIDE(false, "Le prix doit être un montant positif, par exemple 1,50."),
        ALCOOL_NON_ACTIVE(false, "Activez d'abord la catégorie « Boissons alcoolisées »."),
        DEJA_PRESENT(false, "Cette suggestion est déjà dans le catalogue."),
        INCONNU(false, "Produit introuvable.");

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

    static ResultatProduitBuvette refus(Motif m) {
        return new ResultatProduitBuvette(m, null);
    }
}
