package com.pingmanager.persistence;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
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
 *                               "unite": "GOBELET", "prix": 100, "actif": true} ] } ]
 * </pre>
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
        return o;
    }

    static Json ecrireProduit(Produit p) {
        Json j = Json.objet();
        j.set("id", p.getId());
        j.set("nom", p.getNom());
        j.set("categorie", p.getCategorie().name());
        j.set("unite", p.getUnite().name());
        j.set("prix", p.getPrixVenteCentimes());
        j.set("actif", p.isActif());
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
                .build();
    }

    /** Éléments d'un tableau facultatif (vide si absent ou d'un autre type). */
    static List<Json> objetsOuValeurs(Json o, String cle) {
        if (!o.has(cle) || !o.get(cle).estTableau()) {
            return List.of();
        }
        return new ArrayList<>(o.get(cle).elements());
    }
}
