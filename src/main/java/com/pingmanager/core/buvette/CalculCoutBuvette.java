package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul du coût de revient, de la marge et du taux de marge d'un produit de la
 * buvette (B046), à partir des ingrédients de sa recette.
 *
 * <p>Coût d'une portion d'ingrédient = prix d'achat ÷ (rendement × (1 − perte)).
 * Exemple : une baguette à 1,10 € qui fait 3 sandwichs coûte 0,3667 € par
 * sandwich ; avec 5 % de perte, 0,3860 €. Le calcul se fait en valeurs exactes
 * ({@link BigDecimal}) et n'est arrondi au centime qu'une fois, sur le total.
 * Le coût <b>retenu</b> est prudent : il prend le rendement minimal.</p>
 */
public final class CalculCoutBuvette {

    private static final MathContext PRECISION = MathContext.DECIMAL64;

    /**
     * Coût exact (en centimes, non arrondi) d'une portion d'ingrédient.
     *
     * @param prudent vrai : rendement minimal ; faux : rendement maximal
     */
    public static BigDecimal coutPortion(Ingredient i, boolean prudent) {
        int rendement = prudent ? i.getRendementMin() : i.getRendementMax();
        BigDecimal portionsUtiles = BigDecimal.valueOf((long) rendement * (100 - i.getPertePourcent()))
                .divide(BigDecimal.valueOf(100), PRECISION);
        return BigDecimal.valueOf(i.getPrixAchatCentimes()).divide(portionsUtiles, PRECISION);
    }

    /** Coût de revient d'un produit selon les ingrédients du catalogue. */
    public CoutProduit cout(Produit produit, CatalogueBuvette catalogue) {
        BigDecimal prudent = BigDecimal.ZERO;
        BigDecimal optimiste = BigDecimal.ZERO;
        List<CoutProduit.LigneCout> lignes = new ArrayList<>();
        List<String> manquants = new ArrayList<>();
        for (LigneRecette l : produit.getRecette()) {
            Ingredient i = catalogue.ingredient(l.idIngredient()).orElse(null);
            if (i == null) {
                manquants.add(l.idIngredient());
                continue;
            }
            BigDecimal p = coutPortion(i, true).multiply(BigDecimal.valueOf(l.quantite()));
            prudent = prudent.add(p);
            optimiste = optimiste.add(coutPortion(i, false).multiply(BigDecimal.valueOf(l.quantite())));
            lignes.add(new CoutProduit.LigneCout(i.getId(), i.getNom(), l.quantite(), Centimes.arrondir(p),
                    explication(i, l.quantite(), Centimes.arrondir(p))));
        }
        return new CoutProduit(produit.getPrixVenteCentimes(), Centimes.arrondir(prudent),
                Centimes.arrondir(optimiste), lignes, !produit.getRecette().isEmpty(), manquants);
    }

    /** « Baguette : 1,10 € ÷ 3 sandwichs = 0,37 € » (avec perte et quantité si besoin). */
    static String explication(Ingredient i, int quantite, long coutCentimes) {
        StringBuilder sb = new StringBuilder(i.getNom()).append(" : ");
        if (quantite > 1) {
            sb.append(quantite).append(" × (");
        }
        sb.append(Centimes.euros(i.getPrixAchatCentimes())).append(" ÷ ").append(i.getRendementMin())
                .append(' ').append(i.getUnitePortion());
        if (i.getPertePourcent() > 0) {
            sb.append(", perte ").append(i.getPertePourcent()).append(" %");
        }
        if (quantite > 1) {
            sb.append(')');
        }
        return sb.append(" = ").append(Centimes.euros(coutCentimes)).toString();
    }
}
