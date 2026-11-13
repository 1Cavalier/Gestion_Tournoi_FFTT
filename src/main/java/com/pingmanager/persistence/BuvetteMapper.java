package com.pingmanager.persistence;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import com.pingmanager.persistence.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Buvette (B046) en JSON : catalogue du club (dans le fichier d'application,
 * section facultative {@code buvettes}).
 *
 * <pre>
 * "buvettes": [ {"club": "08910012", "alcoolActive": false, "vendus": ["cafe"],
 *                "produits": [ {"id": "cafe", "nom": "Café", "categorie": "BOISSONS_CHAUDES",
 *                               "unite": "GOBELET", "prix": 100, "actif": true,
 *                               "recette": [ {"ingredient": "cafe-moulu", "quantite": 1} ]} ],
 *                "ingredients": [ {"id": "baguette", "nom": "Baguette", "uniteAchat": "baguette",
 *                                  "portion": "sandwich", "prixAchat": 110, "rendementMin": 2,
 *                                  "rendementMax": 3, "perte": 5, "actif": true} ],
 *                "ingredientsUtilises": ["baguette"] } ]
 * </pre>
 *
 * <p>Les sections {@code recette}, {@code ingredients} et {@code ingredientsUtilises}
 * (partie 2) sont facultatives.</p>
 *
 * <p>Montants en centimes entiers. Relecture tolérante : une ligne illisible
 * (fichier abîmé ou d'une version plus récente) est écartée, le reste est relu.</p>
 */
final class BuvetteMapper {

    private BuvetteMapper() {
    }

    // ------------------------------------------------------------------
    // Catalogue du club
    // ------------------------------------------------------------------

    static Json ecrireCatalogue(CatalogueBuvette c) {
        Json o = Json.objet();
        o.set("club", c.getNumeroClub());
        o.set("alcoolActive", c.isAlcoolActive());
        Json vendus = Json.tableau();
        c.getProduitsVendus().forEach(id -> vendus.add(Json.of(id)));
        o.set("vendus", vendus);
        Json produits = Json.tableau();
        for (Produit p : c.getProduits()) {
            produits.add(ecrireProduit(p));
        }
        o.set("produits", produits);
        Json ingredients = Json.tableau();
        for (Ingredient i : c.getIngredients()) {
            ingredients.add(ecrireIngredient(i));
        }
        o.set("ingredients", ingredients);
        Json utilises = Json.tableau();
        c.getIngredientsUtilises().forEach(id -> utilises.add(Json.of(id)));
        o.set("ingredientsUtilises", utilises);
        return o;
    }

    static Json ecrireIngredient(Ingredient i) {
        Json j = Json.objet();
        j.set("id", i.getId());
        j.set("nom", i.getNom());
        j.set("uniteAchat", i.getUniteAchat());
        j.set("portion", i.getUnitePortion());
        j.set("prixAchat", i.getPrixAchatCentimes());
        j.set("rendementMin", i.getRendementMin());
        j.set("rendementMax", i.getRendementMax());
        j.set("perte", i.getPertePourcent());
        j.set("actif", i.isActif());
        return j;
    }

    static Ingredient lireIngredient(Json j) {
        int min = j.get("rendementMin").asInt();
        return Ingredient.builder()
                .id(j.get("id").asString())
                .nom(j.get("nom").asString())
                .uniteAchat(j.getString("uniteAchat", null))
                .unitePortion(j.getString("portion", null))
                .prixAchatCentimes(j.get("prixAchat").asLong())
                .rendement(min, j.getInt("rendementMax", min))
                .pertePourcent(j.getInt("perte", 0))
                .actif(j.getBoolean("actif", true))
                .build();
    }

    static Json ecrireProduit(Produit p) {
        Json j = Json.objet();
        j.set("id", p.getId());
        j.set("nom", p.getNom());
        j.set("categorie", p.getCategorie().name());
        j.set("unite", p.getUnite().name());
        j.set("prix", p.getPrixVenteCentimes());
        j.set("actif", p.isActif());
        if (!p.getRecette().isEmpty()) {
            Json r = Json.tableau();
            for (LigneRecette l : p.getRecette()) {
                r.add(Json.objet().set("ingredient", l.idIngredient()).set("quantite", l.quantite()));
            }
            j.set("recette", r);
        }
        return j;
    }

    static CatalogueBuvette lireCatalogue(Json o) {
        CatalogueBuvette c = new CatalogueBuvette(o.get("club").asString());
        c.setAlcoolActive(o.getBoolean("alcoolActive", false));
        for (Json v : objetsOuValeurs(o, "vendus")) {
            try {
                c.marquerVendu(v.asString());
            } catch (RuntimeException illisible) {
                // valeur d'un autre type : ignorée
            }
        }
        for (Json j : objetsOuValeurs(o, "produits")) {
            try {
                c.enregistrerProduit(lireProduit(j));
            } catch (RuntimeException illisible) {
                // ligne abîmée : écartée
            }
        }
        for (Json j : objetsOuValeurs(o, "ingredients")) {
            try {
                c.enregistrerIngredient(lireIngredient(j));
            } catch (RuntimeException illisible) {
                // ligne abîmée : écartée
            }
        }
        for (Json v : objetsOuValeurs(o, "ingredientsUtilises")) {
            try {
                c.marquerIngredientUtilise(v.asString());
            } catch (RuntimeException illisible) {
                // valeur d'un autre type : ignorée
            }
        }
        return c;
    }

    static Produit lireProduit(Json j) {
        return Produit.builder()
                .id(j.get("id").asString())
                .nom(j.get("nom").asString())
                .categorie(CategorieProduit.lire(j.getString("categorie", null)))
                .unite(UniteVente.lire(j.getString("unite", null)))
                .prixVenteCentimes(Math.max(0, j.get("prix").asLong()))
                .actif(j.getBoolean("actif", true))
                .recette(lireRecette(j))
                .build();
    }

    /** Recette facultative ; une ligne illisible est écartée. */
    static List<LigneRecette> lireRecette(Json j) {
        List<LigneRecette> res = new ArrayList<>();
        for (Json l : objetsOuValeurs(j, "recette")) {
            try {
                res.add(new LigneRecette(l.get("ingredient").asString(), l.getInt("quantite", 1)));
            } catch (RuntimeException illisible) {
                // ligne abîmée : écartée
            }
        }
        return res;
    }

    /** Éléments d'un tableau facultatif (vide si absent ou d'un autre type). */
    static List<Json> objetsOuValeurs(Json o, String cle) {
        if (!o.has(cle) || !o.get(cle).estTableau()) {
            return List.of();
        }
        return new ArrayList<>(o.get(cle).elements());
    }
}
