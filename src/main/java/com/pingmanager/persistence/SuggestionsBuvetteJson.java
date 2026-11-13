package com.pingmanager.persistence;

import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture des suggestions de la buvette (B046) depuis la ressource versionnée
 * {@value #RESSOURCE}, fichier JSON lisible et modifiable sans recompiler la
 * logique. Le cœur ne reçoit que l'objet {@link SuggestionsBuvette} (règle d'or :
 * il ne connaît pas le JSON de la persistance).
 *
 * <p>Ressource absente ou illisible : aucune suggestion, jamais d'erreur bloquante.
 * Les boissons alcoolisées éventuellement présentes sont écartées : elles ne
 * sont jamais suggérées par défaut.</p>
 */
public final class SuggestionsBuvetteJson {

    /** Chemin de la ressource dans le classpath. */
    public static final String RESSOURCE = "/com/pingmanager/buvette/suggestions-buvette.json";

    private SuggestionsBuvetteJson() {
    }

    /** @return les suggestions embarquées dans le logiciel. */
    public static SuggestionsBuvette charger() {
        try (InputStream in = SuggestionsBuvetteJson.class.getResourceAsStream(RESSOURCE)) {
            if (in == null) {
                return SuggestionsBuvette.aucune();
            }
            return lire(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException illisible) {
            return SuggestionsBuvette.aucune();
        }
    }

    /** Analyse un texte JSON de suggestions (lignes illisibles écartées). */
    public static SuggestionsBuvette lire(String texte) {
        Json doc = JsonParser.analyser(texte);
        List<Produit> produits = new ArrayList<>();
        java.util.Map<String, Integer> consommations = new java.util.LinkedHashMap<>();
        for (Json j : BuvetteMapper.objetsOuValeurs(doc, "produits")) {
            try {
                Produit p = BuvetteMapper.lireProduit(j);
                if (!p.getCategorie().estAlcoolisee()) {
                    produits.add(p);
                    if (j.has("consommation")) {
                        consommations.put(p.getId(), Math.max(0, j.get("consommation").asInt()));
                    }
                }
            } catch (RuntimeException illisible) {
                // suggestion abîmée : écartée
            }
        }
        List<com.pingmanager.model.buvette.Ingredient> ingredients = new ArrayList<>();
        for (Json j : BuvetteMapper.objetsOuValeurs(doc, "ingredients")) {
            try {
                ingredients.add(BuvetteMapper.lireIngredient(j));
            } catch (RuntimeException illisible) {
                // suggestion abîmée : écartée
            }
        }
        com.pingmanager.model.buvette.HypothesesConsommation hypotheses;
        try {
            hypotheses = BuvetteMapper.lireHypotheses(doc.get("hypotheses").estObjet() ? doc.get("hypotheses")
                    : com.pingmanager.persistence.json.Json.objet(), consommations);
        } catch (RuntimeException horsBornes) {
            hypotheses = new com.pingmanager.model.buvette.HypothesesConsommation(30, 15, 75, 125, consommations);
        }
        return new SuggestionsBuvette(doc.getInt("version", 1), doc.getString("avertissement", null), produits,
                ingredients, hypotheses);
    }
}
