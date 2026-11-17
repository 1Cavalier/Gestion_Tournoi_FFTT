package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.RayonBuvette;

import java.util.Map;

/**
 * Saisie du formulaire d'un article de la buvette (B026), telle que tapée à
 * l'écran ; contrôlée par {@link ServiceArticlesBuvette#enregistrer}.
 *
 * @param id          identifiant de l'article modifié, {@code null} pour un nouvel article
 * @param nom         nom (« Café »)
 * @param famille     produit ou contenant
 * @param unite       unité comptée (« dose », « gobelet ») ; vide : « unité »
 * @param prixLot     prix d'un lot en euros (« 4,50 ») ; vide : 0
 * @param unitesParLot nombre d'unités dans le lot (« 100 ») ; vide : 1
 * @param stock       quantité en stock ; vide : 0 (ignoré en modification : le stock
 *                    se change par entrée, sortie ou recomptage)
 * @param seuil       seuil d'alerte ; vide : 0
 * @param contenants  pour un produit : identifiant du contenant → quantité par portion
 * @param prixVente   prix de vente d'une portion en euros (« 1,00 ») ; vide : 0 (pas encore fixé)
 * @param rayon       rayon ({@code null} : celui de l'article modifié, sinon déduit de la famille)
 */
public record SaisieArticle(String id, String nom, FamilleArticle famille, String unite, String prixLot,
                            String unitesParLot, String stock, String seuil, Map<String, Integer> contenants,
                            String prixVente, RayonBuvette rayon) {

    public SaisieArticle {
        contenants = contenants == null ? Map.of() : Map.copyOf(contenants);
    }

    /** Saisie sans prix de vente ni rayon (prix et rayon de l'article modifié gardés). */
    public SaisieArticle(String id, String nom, FamilleArticle famille, String unite, String prixLot,
                         String unitesParLot, String stock, String seuil, Map<String, Integer> contenants) {
        this(id, nom, famille, unite, prixLot, unitesParLot, stock, seuil, contenants, null, null);
    }
}
