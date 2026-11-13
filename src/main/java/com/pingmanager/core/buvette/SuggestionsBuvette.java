package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.HypothesesConsommation;
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
 * @param hypotheses    hypothèses de consommation (part des accompagnants, présence,
 *                      scénarios, consommation de chaque produit suggéré)
 */
public record SuggestionsBuvette(int version, String avertissement, List<Produit> produits,
                                 List<Ingredient> ingredients, HypothesesConsommation hypotheses) {

    /** Mention affichée à côté de toute suggestion. */
    public static final String MENTION = "Prix indicatifs à valider par le club";

    public SuggestionsBuvette {
        avertissement = avertissement == null || avertissement.isBlank() ? MENTION : avertissement;
        produits = List.copyOf(produits == null ? List.of() : produits);
        ingredients = List.copyOf(ingredients == null ? List.of() : ingredients);
        hypotheses = hypotheses == null ? HypothesesConsommation.PAR_DEFAUT : hypotheses;
    }

    /** Suggestions sans ingrédients. */
    public SuggestionsBuvette(int version, String avertissement, List<Produit> produits) {
        this(version, avertissement, produits, List.of(), null);
    }

    /** Suggestions sans hypothèses de consommation propres. */
    public SuggestionsBuvette(int version, String avertissement, List<Produit> produits, List<Ingredient> ingredients) {
        this(version, avertissement, produits, ingredients, null);
    }

    /** Mention affichée à côté des hypothèses de consommation. */
    public static final String MENTION_HYPOTHESE = "Hypothèse à ajuster";

    /** @return des suggestions vides (ressource absente ou illisible). */
    public static SuggestionsBuvette aucune() {
        return new SuggestionsBuvette(0, MENTION, List.of(), List.of(), null);
    }

    public Optional<Produit> produit(String id) {
        return produits.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public Optional<Ingredient> ingredient(String id) {
        return ingredients.stream().filter(i -> i.getId().equals(id)).findFirst();
    }
}
