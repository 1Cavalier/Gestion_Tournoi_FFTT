package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculCoutBuvetteTest {

    private final CalculCoutBuvette calcul = new CalculCoutBuvette();

    private static Ingredient ing(String id, String nom, String portion, long prix, int min, int max, int perte) {
        return Ingredient.builder().id(id).nom(nom).unitePortion(portion).prixAchatCentimes(prix)
                .rendement(min, max).pertePourcent(perte).build();
    }

    private static Produit sandwich(long prix, LigneRecette... recette) {
        return Produit.builder().id("sandwich").nom("Sandwich").categorie(CategorieProduit.SANDWICHS)
                .unite(UniteVente.PIECE).prixVenteCentimes(prix).recette(List.of(recette)).build();
    }

    @Test
    void baguette_a_1_10_pour_3_sandwichs_coute_37_centimes_le_sandwich() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(ing("baguette", "Baguette", "sandwichs", 110, 3, 3, 0));
        CoutProduit cout = calcul.cout(sandwich(300, new LigneRecette("baguette", 1)), c);
        assertEquals(37, cout.coutCentimes());
        assertEquals(263, cout.margeCentimes());
        assertEquals(OptionalLong.of(711), cout.tauxDeMargePourcent(), "263 / 37");
        assertEquals(OptionalLong.of(88), cout.tauxDeMarquePourcent(), "263 / 300");
        assertEquals("Baguette : 1,10 € ÷ 3 sandwichs = 0,37 €", cout.lignes().get(0).explication());
    }

    @Test
    void jambon_beurre_somme_exacte_arrondie_une_seule_fois() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(ing("baguette", "Baguette", "sandwichs", 110, 3, 3, 0));   // 36,667
        c.enregistrerIngredient(ing("jambon", "Jambon", "tranches", 260, 4, 4, 0));        // 65
        c.enregistrerIngredient(ing("beurre", "Beurre", "noisettes", 240, 25, 25, 0));     // 9,6
        CoutProduit cout = calcul.cout(sandwich(300, new LigneRecette("baguette", 1),
                new LigneRecette("jambon", 1), new LigneRecette("beurre", 1)), c);
        assertEquals(111, cout.coutCentimes(), "36,667 + 65 + 9,6 = 111,267");
        assertEquals(189, cout.margeCentimes());
        assertEquals(OptionalLong.of(170), cout.tauxDeMargePourcent());
        assertEquals(OptionalLong.of(63), cout.tauxDeMarquePourcent());
    }

    @Test
    void fourchette_de_rendement_cout_prudent_et_optimiste() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(ing("baguette", "Baguette", "sandwichs", 110, 2, 3, 0));
        CoutProduit cout = calcul.cout(sandwich(300, new LigneRecette("baguette", 1)), c);
        assertEquals(55, cout.coutCentimes(), "prudent : 2 sandwichs par baguette");
        assertEquals(37, cout.coutOptimisteCentimes(), "optimiste : 3 sandwichs");
    }

    @Test
    void la_perte_augmente_le_cout_de_la_portion() {
        Ingredient baguette = ing("baguette", "Baguette", "sandwichs", 110, 3, 3, 5);
        assertEquals(0, new BigDecimal("38.596").compareTo(
                CalculCoutBuvette.coutPortion(baguette, true).setScale(3, java.math.RoundingMode.HALF_UP)),
                "110 ÷ (3 × 0,95)");
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(baguette);
        CoutProduit cout = calcul.cout(sandwich(300, new LigneRecette("baguette", 1)), c);
        assertEquals(39, cout.coutCentimes());
        assertEquals("Baguette : 1,10 € ÷ 3 sandwichs, perte 5 % = 0,39 €", cout.lignes().get(0).explication());
    }

    @Test
    void quantite_multiplie_la_portion_et_s_explique() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(ing("emmental", "Emmental", "tranches", 250, 10, 10, 0));
        CoutProduit cout = calcul.cout(sandwich(250, new LigneRecette("emmental", 2)), c);
        assertEquals(50, cout.coutCentimes());
        assertEquals("Emmental : 2 × (2,50 € ÷ 10 tranches) = 0,50 €", cout.lignes().get(0).explication());
    }

    @Test
    void sans_recette_ou_avec_ingredient_disparu_le_cout_est_inconnu() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        CoutProduit sans = calcul.cout(sandwich(300), c);
        assertFalse(sans.recetteRenseignee());
        assertFalse(sans.connu());
        assertEquals(OptionalLong.empty(), sans.tauxDeMargePourcent());
        CoutProduit incomplet = calcul.cout(sandwich(300, new LigneRecette("disparu", 1)), c);
        assertTrue(incomplet.recetteRenseignee());
        assertFalse(incomplet.connu());
        assertEquals(List.of("disparu"), incomplet.ingredientsManquants());
    }

    @Test
    void produit_vendu_a_perte_a_une_marge_negative() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(ing("soda", "Soda", "canettes", 1200, 24, 24, 0));
        CoutProduit cout = calcul.cout(sandwich(40, new LigneRecette("soda", 1)), c);
        assertEquals(50, cout.coutCentimes());
        assertEquals(-10, cout.margeCentimes());
        assertEquals(OptionalLong.of(-20), cout.tauxDeMargePourcent());
    }
}
