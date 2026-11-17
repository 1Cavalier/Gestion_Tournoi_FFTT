package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;

/**
 * Résultat de l'enregistrement ou du retrait d'un article de la buvette (B026).
 *
 * @param motif   ce qui s'est passé
 * @param article l'article enregistré ({@code null} en cas de refus ou de retrait)
 * @param detail  précision ajoutée à l'explication (nom de l'article, du produit qui
 *                bloque…), vide si aucune
 */
public record ResultatArticleBuvette(Motif motif, ArticleBuvette article, String detail) {

    /** Motifs possibles. */
    public enum Motif {
        AJOUTE(true, "Article ajouté"),
        MODIFIE(true, "Article enregistré"),
        RETIRE(true, "Article supprimé"),
        ARCHIVE(true, "Article retiré de la vente (historique gardé)"),
        REMIS_EN_VENTE(true, "Article remis en vente"),
        PRIX_VENTE_INVALIDE(false, "Le prix de vente doit être un montant positif, par exemple 1,50."),
        A_UN_HISTORIQUE(false, "Cet article a déjà des mouvements ou des ventes : retirez-le de la vente "
                + "(archivage) au lieu de le supprimer"),
        NOM_OBLIGATOIRE(false, "Donnez un nom à l'article."),
        NOM_EN_DOUBLE(false, "Un article porte déjà ce nom"),
        PRIX_INVALIDE(false, "Le prix du lot doit être un montant positif, par exemple 4,50."),
        LOT_INVALIDE(false, "Le nombre d'unités par lot doit être un entier d'au moins 1."),
        STOCK_INVALIDE(false, "Le stock doit être un nombre entier positif ou nul."),
        SEUIL_INVALIDE(false, "Le seuil d'alerte doit être un nombre entier positif ou nul."),
        CONTENANT_INVALIDE(false, "Chaque contenant doit être un contenant du stock, au moins 1 par portion."),
        CONTENANT_UTILISE(false, "Ce contenant sert encore à un produit"),
        DEJA_ARCHIVE(false, "Cet article est déjà retiré de la vente."),
        NON_ARCHIVE(false, "Cet article est déjà en vente."),
        FAMILLE_VERROUILLEE(false, "Ce contenant sert encore à un produit : il ne peut pas devenir un produit"),
        INCONNU(false, "Article introuvable.");

        private final boolean accepte;
        private final String explication;

        Motif(boolean accepte, String explication) {
            this.accepte = accepte;
            this.explication = explication;
        }
    }

    static ResultatArticleBuvette refus(Motif m) {
        return new ResultatArticleBuvette(m, null, "");
    }

    static ResultatArticleBuvette refus(Motif m, String detail) {
        return new ResultatArticleBuvette(m, null, detail);
    }

    public boolean accepte() {
        return motif.accepte;
    }

    /** Phrase à afficher (« Article ajouté : Café. »). */
    public String explication() {
        String base = motif.explication;
        if (detail == null || detail.isBlank()) {
            return base.endsWith(".") ? base : base + ".";
        }
        return base + " : " + detail + ".";
    }
}
