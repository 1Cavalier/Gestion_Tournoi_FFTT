package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.Produit;

import java.util.List;
import java.util.Optional;

/**
 * Suggestions de départ de la buvette (B046), lues depuis une ressource
 * versionnée (le lecteur JSON est dans la persistance). Ce sont des
 * <b>hypothèses éditables</b>, jamais des prix de marché : elles ne rejoignent
 * le catalogue du club qu'après un clic (« Ajouter » ou « Tout ajouter »).
 *
 * @param version       version de la ressource
 * @param avertissement phrase affichée avec les suggestions
 * @param produits      produits proposés (jamais de boisson alcoolisée), avec leur recette
 * @param ingredients   ingrédients proposés (prix d'achat, rendement, perte)
 */
public record SuggestionsBuvette(int version, String avertissement, List<Produit> produits,
                                 List<Ingredient> ingredients) {

    /** Mention affichée à côté de toute suggestion. */
    public static final String MENTION = "Prix indicatifs à valider par le club";

    public SuggestionsBuvette {
        avertissement = avertissement == null || avertissement.isBlank() ? MENTION : avertissement;
        produits = List.copyOf(produits == null ? List.of() : produits);
        ingredients = List.copyOf(ingredients == null ? List.of() : ingredients);
    }

    /** Suggestions sans ingrédients. */
    public SuggestionsBuvette(int version, String avertissement, List<Produit> produits) {
        this(version, avertissement, produits, List.of());
    }

    /** @return des suggestions vides (ressource absente ou illisible). */
    public static SuggestionsBuvette aucune() {
        return new SuggestionsBuvette(0, MENTION, List.of(), List.of());
    }

    public Optional<Produit> produit(String id) {
        return produits.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public Optional<Ingredient> ingredient(String id) {
        return ingredients.stream().filter(i -> i.getId().equals(id)).findFirst();
    }
}
