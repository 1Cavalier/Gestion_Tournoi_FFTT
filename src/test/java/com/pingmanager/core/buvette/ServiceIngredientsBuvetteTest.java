package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceIngredientsBuvetteTest {

    private final ServiceIngredientsBuvette service = new ServiceIngredientsBuvette();

    private static CatalogueBuvette avecSandwich() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerProduit(Produit.builder().id("sandwich").nom("Sandwich").categorie(CategorieProduit.SANDWICHS)
                .unite(UniteVente.PIECE).prixVenteCentimes(300).build());
        return c;
    }

    @Test
    void saisie_d_un_ingredient_avec_fourchette_de_rendement_et_perte() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        ResultatIngredientBuvette r = service.saisir(c, null, "Baguette", "baguette", "sandwich", "1,10", "2", "3",
                "5 %");
        assertEquals(ResultatIngredientBuvette.Motif.ENREGISTRE, r.motif());
        Ingredient b = c.ingredient("baguette").orElseThrow();
        assertEquals(110, b.getPrixAchatCentimes());
        assertEquals(2, b.getRendementMin());
        assertEquals(3, b.getRendementMax());
        assertEquals(5, b.getPertePourcent());
        assertEquals("2 à 3 sandwich", b.libelleRendement());
    }

    @Test
    void rendement_maximal_vide_vaut_le_minimum_et_perte_vide_vaut_zero() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        service.saisir(c, null, "Soda", "carton de 24", "canette", "12", "24", "", "");
        Ingredient s = c.ingredient("soda").orElseThrow();
        assertEquals(24, s.getRendementMax());
        assertEquals(0, s.getPertePourcent());
    }

    @Test
    void saisies_refusees_avec_motif_precis() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        assertEquals(ResultatIngredientBuvette.Motif.NOM_MANQUANT,
                service.saisir(c, null, "", "u", "p", "1", "1", "1", "0").motif());
        assertEquals(ResultatIngredientBuvette.Motif.PRIX_INVALIDE,
                service.saisir(c, null, "A", "u", "p", "gratuit", "1", "1", "0").motif());
        assertEquals(ResultatIngredientBuvette.Motif.RENDEMENT_INVALIDE,
                service.saisir(c, null, "A", "u", "p", "1", "3", "2", "0").motif());
        assertEquals(ResultatIngredientBuvette.Motif.RENDEMENT_INVALIDE,
                service.saisir(c, null, "A", "u", "p", "1", "0", "", "0").motif());
        assertEquals(ResultatIngredientBuvette.Motif.PERTE_INVALIDE,
                service.saisir(c, null, "A", "u", "p", "1", "1", "1", "95").motif());
        service.saisir(c, null, "Beurre", "plaquette", "noisette", "2,40", "25", "", "");
        assertEquals(ResultatIngredientBuvette.Motif.NOM_EN_DOUBLE,
                service.saisir(c, null, "beurre", "u", "p", "1", "1", "1", "0").motif());
        assertEquals(1, c.getIngredients().size());
    }

    @Test
    void recette_definie_cumule_les_doublons_et_refuse_un_ingredient_inconnu() {
        CatalogueBuvette c = avecSandwich();
        service.saisir(c, null, "Emmental", "paquet", "tranche", "2,50", "10", "", "");
        assertEquals(ResultatProduitBuvette.Motif.INGREDIENT_INCONNU, service.definirRecette(c, "sandwich",
                List.of(new LigneRecette("jambon", 1))).motif());
        assertEquals(ResultatProduitBuvette.Motif.ENREGISTRE, service.definirRecette(c, "sandwich",
                List.of(new LigneRecette("emmental", 1), new LigneRecette("emmental", 1))).motif());
        assertEquals(List.of(new LigneRecette("emmental", 2)), c.produit("sandwich").orElseThrow().getRecette());
        assertEquals(ResultatProduitBuvette.Motif.INCONNU,
                service.definirRecette(c, "absent", List.of()).motif());
    }

    @Test
    void ingredient_d_une_recette_ou_deja_en_stock_est_desactive_et_non_supprime() {
        CatalogueBuvette c = avecSandwich();
        service.saisir(c, null, "Emmental", "paquet", "tranche", "2,50", "10", "", "");
        service.saisir(c, null, "Beurre", "plaquette", "noisette", "2,40", "25", "", "");
        service.saisir(c, null, "Sel", "paquet", "pincée", "0,50", "100", "", "");
        service.definirRecette(c, "sandwich", List.of(new LigneRecette("emmental", 1)));
        c.marquerIngredientUtilise("beurre");
        assertEquals(ResultatIngredientBuvette.Motif.DESACTIVE_DEJA_UTILISE, service.retirer(c, "emmental").motif());
        assertEquals(ResultatIngredientBuvette.Motif.DESACTIVE_DEJA_UTILISE, service.retirer(c, "beurre").motif());
        assertFalse(c.ingredient("beurre").orElseThrow().isActif());
        assertEquals(ResultatIngredientBuvette.Motif.SUPPRIME, service.retirer(c, "sel").motif());
        assertTrue(c.ingredient("sel").isEmpty());
        service.changerActif(c, "beurre", true);
        assertTrue(c.ingredient("beurre").orElseThrow().isActif());
    }

    @Test
    void produit_suggere_amene_les_ingredients_suggeres_de_sa_recette() {
        Ingredient cafe = Ingredient.builder().id("cafe-moulu").nom("Café moulu").prixAchatCentimes(1200)
                .rendement(120, 140).build();
        Ingredient gobelet = Ingredient.builder().id("gobelets").nom("Gobelets").prixAchatCentimes(300)
                .rendement(50).build();
        Produit suggestion = Produit.builder().id("cafe").nom("Café").categorie(CategorieProduit.BOISSONS_CHAUDES)
                .prixVenteCentimes(100).recette(List.of(new LigneRecette("cafe-moulu", 1),
                        new LigneRecette("gobelets", 1), new LigneRecette("introuvable", 1))).build();
        SuggestionsBuvette s = new SuggestionsBuvette(1, null, List.of(suggestion), List.of(cafe, gobelet));
        CatalogueBuvette c = new CatalogueBuvette("1");
        assertEquals(2, service.suggestionsDisponibles(c, s).size());
        new ServiceCatalogueBuvette().ajouterSuggestion(c, s, "cafe");
        assertEquals(2, c.getIngredients().size());
        assertEquals(2, c.produit("cafe").orElseThrow().getRecette().size(), "la ligne introuvable est écartée");
        assertEquals(0, service.suggestionsDisponibles(c, s).size());
        assertEquals(ResultatIngredientBuvette.Motif.DEJA_PRESENT, service.ajouterSuggestion(c, s, "gobelets").motif());
    }
}
