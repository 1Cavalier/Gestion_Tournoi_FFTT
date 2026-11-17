package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.VenteBuvette;

import java.util.List;

/**
 * Résultat de l'enregistrement ou de l'annulation d'une vente de la buvette
 * (B026).
 *
 * @param motif      ce qui s'est passé
 * @param vente      la vente enregistrée ou annulée ({@code null} en cas de refus)
 * @param message    phrase à afficher
 * @param aRecompter noms des articles qui manquaient en stock (la vente passe
 *                   quand même ; ils sont signalés « à recompter »)
 */
public record ResultatVente(Motif motif, VenteBuvette vente, String message, List<String> aRecompter) {

    /** Motifs possibles. */
    public enum Motif {
        ENREGISTREE(true, "Vente enregistrée."),
        ANNULEE(true, "Vente annulée."),
        MODE_STOCK_SEUL(false, "La buvette de ce tournoi est réglée sur « Stock seulement » : choisissez "
                + "« Stock et ventes » pour enregistrer des ventes."),
        COMMANDE_VIDE(false, "La commande est vide : touchez un article pour l'ajouter."),
        ARTICLE_INCONNU(false, "Un article de la commande n'existe plus."),
        ARTICLE_ARCHIVE(false, "Un article de la commande est retiré de la vente"),
        PAS_UN_PRODUIT(false, "Seuls les produits se vendent (pas les contenants)"),
        PRIX_MANQUANT(false, "Fixez d'abord le prix de vente dans la fiche de l'article"),
        QUANTITE_INVALIDE(false, "Chaque quantité doit être comprise entre 1 et " + CommandeComptoir.QUANTITE_MAX
                + "."),
        VENTE_INCONNUE(false, "Vente introuvable."),
        DEJA_ANNULEE(false, "Cette vente est déjà annulée."),
        AUCUNE_VENTE(false, "Aucune vente à annuler pour ce tournoi.");

        private final boolean accepte;
        private final String explication;

        Motif(boolean accepte, String explication) {
            this.accepte = accepte;
            this.explication = explication;
        }
    }

    public ResultatVente {
        aRecompter = aRecompter == null ? List.of() : List.copyOf(aRecompter);
    }

    static ResultatVente refus(Motif m) {
        return new ResultatVente(m, null, m.explication, List.of());
    }

    static ResultatVente refus(Motif m, String detail) {
        return new ResultatVente(m, null, m.explication + " : " + detail + ".", List.of());
    }

    public boolean accepte() {
        return motif.accepte;
    }

    /** @return vrai si un article manquait en stock (avertissement à afficher). */
    public boolean avecAvertissement() {
        return !aRecompter.isEmpty();
    }

    /** Phrase à afficher, avec l'avertissement de stock s'il y a lieu. */
    public String explication() {
        if (aRecompter.isEmpty()) {
            return message;
        }
        return message + " Stock insuffisant pour " + String.join(", ", aRecompter)
                + " : la vente est enregistrée, pensez à recompter.";
    }
}
