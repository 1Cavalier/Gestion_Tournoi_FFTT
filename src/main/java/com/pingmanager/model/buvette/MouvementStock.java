package com.pingmanager.model.buvette;

import java.time.Instant;
import java.util.Objects;

/**
 * Ligne du journal de stock de la buvette (B026). Une même opération (sortir
 * un café) produit une ligne par article touché (le café, le gobelet, la
 * touillette), toutes avec le même numéro d'opération.
 *
 * @param operation  numéro de l'opération (1, 2, 3…)
 * @param horodatage instant de la saisie
 * @param idArticle  article touché
 * @param nature     entrée, sortie ou recomptage
 * @param variation  variation réellement appliquée au stock (signée)
 * @param stockApres stock de l'article après le mouvement
 * @param origine    d'où vient le mouvement (saisie à la main aujourd'hui)
 * @param reference  référence de l'origine (vide pour une saisie à la main ;
 *                   un jour, le numéro de la commande)
 * @param auteur     qui a saisi
 */
public record MouvementStock(int operation, Instant horodatage, String idArticle, NatureMouvement nature,
                             long variation, long stockApres, OrigineMouvement origine, String reference,
                             String auteur) {

    public MouvementStock {
        if (operation < 1) {
            throw new IllegalArgumentException("opération au moins 1");
        }
        Objects.requireNonNull(horodatage, "horodatage");
        if (idArticle == null || idArticle.isBlank()) {
            throw new IllegalArgumentException("article obligatoire");
        }
        Objects.requireNonNull(nature, "nature");
        Objects.requireNonNull(origine, "origine");
        if (stockApres < 0) {
            throw new IllegalArgumentException("stock négatif");
        }
        reference = reference == null ? "" : reference.trim();
        auteur = auteur == null || auteur.isBlank() ? "inconnu" : auteur.trim();
    }
}
