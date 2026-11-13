package com.pingmanager.persistence;

import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;
import com.pingmanager.persistence.json.JsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B046 : catalogue de la buvette dans le fichier d'application, et anciens fichiers. */
class BuvetteCatalogueRoundTripTest {

    @TempDir
    Path dossier;

    @Test
    void ancien_fichier_sans_buvette_se_relit_et_se_reecrit_a_l_identique() {
        String ancien = "{\"version\":1,\"kind\":\"application\",\"clubs\":[],\"comptes\":[],"
                + "\"comptesUtilisateurs\":[],\"acces\":[],\"agendas\":[],"
                + "\"prononciations\":{\"joueurs\":[],\"clubs\":[]},\"recents\":[]}";
        DepotApplication depot = DepotApplication.lire(JsonParser.analyser(ancien));
        Json reecrit = depot.ecrire();
        assertFalse(reecrit.has("buvettes"));
        assertEquals(JsonWriter.ecrire(JsonParser.analyser(ancien), true), JsonWriter.ecrire(reecrit, true));
    }

    @Test
    void catalogue_consulte_mais_vide_n_est_pas_ecrit() {
        DepotApplication depot = new DepotApplication();
        depot.catalogueBuvette("08910012");
        assertFalse(depot.ecrire().has("buvettes"));
    }

    @Test
    void aller_retour_du_catalogue_par_fichier() throws Exception {
        DepotApplication depot = new DepotApplication();
        CatalogueBuvette c = depot.catalogueBuvette("08910012");
        c.setAlcoolActive(true);
        c.enregistrerProduit(Produit.builder().id("cafe").nom("Café").categorie(CategorieProduit.BOISSONS_CHAUDES)
                .unite(UniteVente.GOBELET).prixVenteCentimes(100).build());
        c.enregistrerProduit(Produit.builder().id("biere").nom("Bière").categorie(CategorieProduit.ALCOOLISEES)
                .unite(UniteVente.GOBELET).prixVenteCentimes(250).actif(false).build());
        c.marquerVendu("cafe");
        Path f = dossier.resolve("app.json");
        depot.sauvegarder(f);
        CatalogueBuvette relu = DepotApplication.charger(f).catalogueBuvette("08910012");
        assertTrue(relu.isAlcoolActive());
        assertEquals(c.getProduits(), relu.getProduits());
        assertTrue(relu.estVendu("cafe"));
        assertTrue(Files.readString(f).contains("\"prix\": 100"));
    }

    @Test
    void ligne_de_produit_abimee_est_ecartee_sans_bloquer() {
        String texte = "{\"version\":1,\"buvettes\":[{\"club\":\"1\",\"produits\":["
                + "{\"id\":\"cafe\",\"nom\":\"Café\",\"prix\":100},{\"nom\":\"sans id\"},"
                + "{\"id\":\"x\",\"nom\":\"X\",\"prix\":\"cher\"}]},{\"pas\":\"de club\"}]}";
        CatalogueBuvette c = DepotApplication.lire(JsonParser.analyser(texte)).catalogueBuvette("1");
        assertEquals(1, c.getProduits().size());
        assertEquals(CategorieProduit.AUTRE, c.getProduits().get(0).getCategorie());
    }

    @Test
    void suggestions_embarquees_lisibles_sans_alcool_et_avec_prix_positifs() {
        SuggestionsBuvette s = SuggestionsBuvetteJson.charger();
        assertTrue(s.produits().size() >= 10);
        assertTrue(s.produits().stream().noneMatch(p -> p.getCategorie().estAlcoolisee()));
        assertTrue(s.produits().stream().allMatch(p -> p.getPrixVenteCentimes() > 0));
        assertTrue(s.produit("cafe").isPresent());
        assertTrue(s.produit("sandwich-jambon-beurre").isPresent());
    }

    @Test
    void suggestion_alcoolisee_d_une_ressource_modifiee_est_ecartee() {
        SuggestionsBuvette s = SuggestionsBuvetteJson.lire("{\"version\":2,\"produits\":["
                + "{\"id\":\"biere\",\"nom\":\"Bière\",\"categorie\":\"ALCOOLISEES\",\"prix\":250},"
                + "{\"id\":\"eau\",\"nom\":\"Eau\",\"prix\":100}]}");
        assertEquals(1, s.produits().size());
        assertEquals(SuggestionsBuvette.MENTION, s.avertissement());
    }

    @Test
    void aller_retour_des_ingredients_et_des_recettes() {
        DepotApplication depot = new DepotApplication();
        CatalogueBuvette c = depot.catalogueBuvette("1");
        c.enregistrerIngredient(com.pingmanager.model.buvette.Ingredient.builder().id("baguette").nom("Baguette")
                .uniteAchat("baguette").unitePortion("sandwich").prixAchatCentimes(110).rendement(2, 3)
                .pertePourcent(5).actif(false).build());
        c.enregistrerProduit(Produit.builder().id("s").nom("Sandwich").prixVenteCentimes(300)
                .recette(java.util.List.of(new com.pingmanager.model.buvette.LigneRecette("baguette", 2))).build());
        c.marquerIngredientUtilise("baguette");
        CatalogueBuvette relu = DepotApplication.lire(depot.ecrire()).catalogueBuvette("1");
        assertEquals(c.getIngredients(), relu.getIngredients());
        assertEquals(c.getProduits(), relu.getProduits());
        assertTrue(relu.getIngredientsUtilises().contains("baguette"));
    }

    @Test
    void les_recettes_suggerees_citent_des_ingredients_suggeres() {
        SuggestionsBuvette s = SuggestionsBuvetteJson.charger();
        assertTrue(s.ingredients().size() >= 15);
        for (Produit p : s.produits()) {
            assertFalse(p.getRecette().isEmpty(), p.getNom());
            p.getRecette().forEach(l -> assertTrue(s.ingredient(l.idIngredient()).isPresent(), l.idIngredient()));
        }
        com.pingmanager.model.buvette.Ingredient baguette = s.ingredient("baguette").orElseThrow();
        assertEquals(2, baguette.getRendementMin());
        assertEquals(3, baguette.getRendementMax());
    }
}
