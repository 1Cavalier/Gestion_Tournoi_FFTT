package com.pingmanager.persistence;

import com.pingmanager.model.buvette.BudgetBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.TypeMouvementStock;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import com.pingmanager.persistence.json.Json;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Buvette (B046) en JSON : catalogue du club (dans le fichier d'application,
 * section facultative {@code buvettes}) et buvette d'un tournoi (fichier voisin
 * du tournoi, voir {@link DepotBuvette}).
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

    // ------------------------------------------------------------------
    // Buvette d'un tournoi (fichier voisin)
    // ------------------------------------------------------------------

    /**
     * <pre>
     * {"version": 1, "kind": "buvette", "seuils": {"baguette": 10},
     *  "mouvements": [ {"numero": 1, "type": "ACHAT", "ingredient": "baguette", "quantite": 30,
     *                   "prix": 1100, "date": "2026-11-14", "horodatage": "2026-11-14T08:00:00Z",
     *                   "auteur": "Q. S.", "commentaire": "10 × baguette"} ]}
     * </pre>
     */
    static Json ecrireBuvetteTournoi(BuvetteTournoi b) {
        Json o = Json.objet();
        Json seuils = Json.objet();
        b.getSeuils().forEach((id, v) -> seuils.set(id, v.longValue()));
        o.set("seuils", seuils);
        Json mouvements = Json.tableau();
        for (MouvementStock m : b.getMouvements()) {
            Json j = Json.objet();
            j.set("numero", m.numero());
            j.set("type", m.type().name());
            j.set("ingredient", m.idIngredient());
            j.set("quantite", m.quantite());
            if (m.prixPayeCentimes() != 0) {
                j.set("prix", m.prixPayeCentimes());
            }
            if (m.dateAchat() != null) {
                j.set("date", m.dateAchat().toString());
            }
            j.set("horodatage", m.horodatage().toString());
            j.set("auteur", m.auteur());
            if (!m.commentaire().isEmpty()) {
                j.set("commentaire", m.commentaire());
            }
            if (m.numeroLie() != 0) {
                j.set("lie", m.numeroLie());
            }
            if (m.compte() >= 0) {
                j.set("compte", m.compte());
            }
            mouvements.add(j);
        }
        o.set("mouvements", mouvements);
        if (!b.getBudget().estVide()) {
            o.set("budget", ecrireBudget(b.getBudget()));
        }
        return o;
    }

    /**
     * Budget prévisionnel (facultatif).
     *
     * <pre>
     * "budget": {"joueurs": {"2027-01-10": 120}, "deduireStock": true, "origine": "Open 2026",
     *            "quantitesImposees": {"cafe": 80},
     *            "hypotheses": {"accompagnants": 30, "presence": 15, "scenarioBas": 75,
     *                           "scenarioHaut": 125, "consommations": {"cafe": 35}}}
     * </pre>
     */
    static Json ecrireBudget(BudgetBuvette budget) {
        Json o = Json.objet();
        Json joueurs = Json.objet();
        budget.joueursSaisis().forEach((j, n) -> joueurs.set(j.toString(), n.intValue()));
        o.set("joueurs", joueurs);
        o.set("deduireStock", budget.deduireStock());
        if (!budget.origineQuantites().isEmpty()) {
            o.set("origine", budget.origineQuantites());
        }
        Json q = Json.objet();
        budget.quantitesImposees().forEach((id, n) -> q.set(id, n.longValue()));
        o.set("quantitesImposees", q);
        if (budget.hypotheses() != null) {
            o.set("hypotheses", ecrireHypotheses(budget.hypotheses()));
        }
        return o;
    }

    static Json ecrireHypotheses(HypothesesConsommation h) {
        Json o = Json.objet();
        o.set("accompagnants", h.accompagnantsPourcent());
        o.set("presence", h.presenceDixiemes());
        o.set("scenarioBas", h.scenarioBasPourcent());
        o.set("scenarioHaut", h.scenarioHautPourcent());
        Json c = Json.objet();
        h.consommations().forEach((id, v) -> c.set(id, v.intValue()));
        o.set("consommations", c);
        return o;
    }

    /** Hypothèses générales d'un objet JSON ; consommations lues à part (elles peuvent être ailleurs). */
    static HypothesesConsommation lireHypotheses(Json o, java.util.Map<String, Integer> consommations) {
        HypothesesConsommation d = HypothesesConsommation.PAR_DEFAUT;
        return new HypothesesConsommation(o.getInt("accompagnants", d.accompagnantsPourcent()),
                o.getInt("presence", d.presenceDixiemes()), o.getInt("scenarioBas", d.scenarioBasPourcent()),
                o.getInt("scenarioHaut", d.scenarioHautPourcent()), consommations);
    }

    static BudgetBuvette lireBudget(Json o) {
        java.util.Map<LocalDate, Integer> joueurs = new java.util.LinkedHashMap<>();
        if (o.get("joueurs").estObjet()) {
            o.get("joueurs").entrees().forEach((j, n) -> {
                try {
                    joueurs.put(LocalDate.parse(j), Math.max(0, n.asInt()));
                } catch (RuntimeException illisible) {
                    // jour ou nombre illisible : ignoré
                }
            });
        }
        java.util.Map<String, Long> quantites = new java.util.LinkedHashMap<>();
        if (o.get("quantitesImposees").estObjet()) {
            o.get("quantitesImposees").entrees().forEach((id, n) -> {
                try {
                    quantites.put(id, Math.max(0, n.asLong()));
                } catch (RuntimeException illisible) {
                    // quantité illisible : ignorée
                }
            });
        }
        HypothesesConsommation h = null;
        if (o.get("hypotheses").estObjet()) {
            try {
                Json jh = o.get("hypotheses");
                h = lireHypotheses(jh, lireConsommations(jh.get("consommations")));
            } catch (RuntimeException illisibles) {
                h = null; // hypothèses hors bornes : retour aux suggestions
            }
        }
        return new BudgetBuvette(joueurs, h, quantites, o.getString("origine", ""), o.getBoolean("deduireStock", true));
    }

    static java.util.Map<String, Integer> lireConsommations(Json o) {
        java.util.Map<String, Integer> res = new java.util.LinkedHashMap<>();
        if (o.estObjet()) {
            o.entrees().forEach((id, v) -> {
                try {
                    res.put(id, v.asInt());
                } catch (RuntimeException illisible) {
                    // valeur illisible : ignorée
                }
            });
        }
        return res;
    }

    static BuvetteTournoi lireBuvetteTournoi(Json o) {
        BuvetteTournoi b = new BuvetteTournoi();
        if (o.has("seuils") && o.get("seuils").estObjet()) {
            o.get("seuils").entrees().forEach((id, v) -> {
                try {
                    b.definirSeuil(id, v.asLong());
                } catch (RuntimeException illisible) {
                    // seuil d'un autre type : ignoré
                }
            });
        }
        if (o.get("budget").estObjet()) {
            try {
                b.setBudget(lireBudget(o.get("budget")));
            } catch (RuntimeException illisible) {
                // budget abîmé : tout redevient estimé
            }
        }
        for (Json j : objetsOuValeurs(o, "mouvements")) {
            try {
                TypeMouvementStock type = TypeMouvementStock.lire(j.getString("type", null));
                if (type == null) {
                    continue; // type d'une version plus récente : écarté
                }
                b.ajouterMouvement(new MouvementStock(j.getInt("numero", 0), type, j.getString("ingredient", null),
                        j.get("quantite").asLong(), j.has("prix") ? j.get("prix").asLong() : 0,
                        j.has("date") ? LocalDate.parse(j.getString("date", "")) : null,
                        Instant.parse(j.getString("horodatage", "")), j.getString("auteur", null),
                        j.getString("commentaire", ""), j.getInt("lie", 0),
                        j.has("compte") ? j.get("compte").asLong() : -1));
            } catch (RuntimeException illisible) {
                // ligne abîmée ou numéro en double : écartée, le reste du journal est relu
            }
        }
        return b;
    }

    /** Éléments d'un tableau facultatif (vide si absent ou d'un autre type). */
    static List<Json> objetsOuValeurs(Json o, String cle) {
        if (!o.has(cle) || !o.get(cle).estTableau()) {
            return List.of();
        }
        return new ArrayList<>(o.get(cle).elements());
    }
}
