package com.pingmanager.model.buvette;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Ligne d'une vente de la buvette (B026). Le nom, le prix et le coût sont
 * <b>figés</b> au moment de la vente : modifier l'article ensuite ne change
 * pas les ventes passées.
 *
 * @param idArticle           article vendu
 * @param nom                 son nom au moment de la vente
 * @param quantite            nombre de portions (au moins 1)
 * @param prixUnitaireCentimes prix de vente appliqué, en centimes
 * @param coutUnitaire        coût complet d'une portion (produit + emballage), en
 *                            centimes exacts (non arrondis)
 */
public record LigneVente(String idArticle, String nom, int quantite, long prixUnitaireCentimes,
                         BigDecimal coutUnitaire) {

    public LigneVente {
        if (idArticle == null || idArticle.isBlank()) {
            throw new IllegalArgumentException("article obligatoire");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom obligatoire");
        }
        if (quantite < 1) {
            throw new IllegalArgumentException("quantité au moins 1");
        }
        if (prixUnitaireCentimes < 0) {
            throw new IllegalArgumentException("prix négatif");
        }
        Objects.requireNonNull(coutUnitaire, "coût");
        if (coutUnitaire.signum() < 0) {
            throw new IllegalArgumentException("coût négatif");
        }
    }

    /** Montant de la ligne, en centimes. */
    public long montantCentimes() {
        return prixUnitaireCentimes * quantite;
    }

    /** Coût de la ligne, en centimes exacts. */
    public BigDecimal cout() {
        return coutUnitaire.multiply(BigDecimal.valueOf(quantite));
    }
}
