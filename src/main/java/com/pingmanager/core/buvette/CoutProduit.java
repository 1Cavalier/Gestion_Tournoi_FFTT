package com.pingmanager.core.buvette;

import java.util.List;

/**
 * Coût de revient d'un produit de la buvette (B046), calculé à partir de sa
 * recette par {@link CalculCoutBuvette}. Montants en centimes, arrondis une
 * seule fois, sur la somme exacte des lignes.
 *
 * @param prixVenteCentimes prix de vente du produit
 * @param coutCentimes      coût retenu, <b>prudent</b> (rendement minimal de chaque ingrédient)
 * @param coutOptimisteCentimes coût avec le rendement maximal
 * @param lignes            détail par ingrédient, avec sa phrase d'explication
 * @param recetteRenseignee faux si le produit n'a pas de recette (coût inconnu)
 * @param ingredientsManquants identifiants de la recette absents du catalogue
 */
public record CoutProduit(long prixVenteCentimes, long coutCentimes, long coutOptimisteCentimes,
                          List<LigneCout> lignes, boolean recetteRenseignee, List<String> ingredientsManquants) {

    /**
     * Une ligne : un ingrédient de la recette.
     *
     * @param explication par exemple « Baguette : 1,10 € ÷ 3 sandwichs = 0,37 € »
     */
    public record LigneCout(String idIngredient, String nom, int quantite, long coutCentimes, String explication) {
    }

    public CoutProduit {
        lignes = List.copyOf(lignes);
        ingredientsManquants = List.copyOf(ingredientsManquants);
    }

    /** @return vrai si le coût est connu : recette renseignée et complète. */
    public boolean connu() {
        return recetteRenseignee && ingredientsManquants.isEmpty();
    }

    /** Marge unitaire = prix de vente − coût retenu (peut être négative). */
    public long margeCentimes() {
        return prixVenteCentimes - coutCentimes;
    }

    /**
     * Taux de marge en pour cent (marge ÷ coût de revient), arrondi ; vide si le
     * coût est nul ou inconnu.
     */
    public java.util.OptionalLong tauxDeMargePourcent() {
        if (!connu() || coutCentimes == 0) {
            return java.util.OptionalLong.empty();
        }
        return java.util.OptionalLong.of(Math.round(margeCentimes() * 100.0 / coutCentimes));
    }

    /**
     * Taux de marque en pour cent (marge ÷ prix de vente), arrondi ; vide si le prix
     * est nul ou le coût inconnu.
     */
    public java.util.OptionalLong tauxDeMarquePourcent() {
        if (!connu() || prixVenteCentimes == 0) {
            return java.util.OptionalLong.empty();
        }
        return java.util.OptionalLong.of(Math.round(margeCentimes() * 100.0 / prixVenteCentimes));
    }
}
