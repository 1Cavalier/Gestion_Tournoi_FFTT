package com.pingmanager.model.buvette;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Mouvement de stock d'un ingrédient pendant un tournoi (B046). Les mouvements
 * ne se suppriment jamais : une erreur se corrige par un mouvement inverse
 * ({@link TypeMouvementStock#CORRECTION}) qui cite le numéro corrigé.
 *
 * @param numero        numéro d'ordre dans le tournoi (1, 2, 3…)
 * @param type          nature du mouvement
 * @param idIngredient  ingrédient concerné
 * @param quantite      portions, signées (entrée positive, sortie négative)
 * @param prixPayeCentimes montant payé pour un achat (négatif pour sa correction), 0 sinon
 * @param dateAchat     date de l'achat, ou {@code null}
 * @param horodatage    instant de la saisie
 * @param auteur        qui a saisi
 * @param commentaire   précision libre (vide si aucune)
 * @param numeroLie     numéro du mouvement corrigé, ou de la vente à l'origine ; 0 si aucun
 * @param compte        pour un inventaire, nombre de portions comptées ; -1 sinon
 */
public record MouvementStock(int numero, TypeMouvementStock type, String idIngredient, long quantite,
                             long prixPayeCentimes, LocalDate dateAchat, Instant horodatage, String auteur,
                             String commentaire, int numeroLie, long compte) {

    public MouvementStock {
        if (numero < 1) {
            throw new IllegalArgumentException("numéro au moins 1");
        }
        Objects.requireNonNull(type, "type");
        if (idIngredient == null || idIngredient.isBlank()) {
            throw new IllegalArgumentException("ingrédient obligatoire");
        }
        Objects.requireNonNull(horodatage, "horodatage");
        auteur = auteur == null || auteur.isBlank() ? "inconnu" : auteur.trim();
        commentaire = commentaire == null ? "" : commentaire.trim();
    }
}
