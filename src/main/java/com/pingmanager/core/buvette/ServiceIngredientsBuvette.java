package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Ingrédients et recettes de la buvette (B046) : saisie contrôlée d'un
 * ingrédient (prix, rendement min-max, perte), retrait (jamais d'un ingrédient
 * utilisé : il est désactivé), définition de la recette d'un produit et
 * suggestions d'ingrédients.
 */
public final class ServiceIngredientsBuvette {

    /**
     * Saisie d'un ingrédient depuis le formulaire (nouvel ingrédient si {@code id} vide).
     *
     * @param prixSaisi      prix d'achat d'une unité, en euros (« 1,10 »)
     * @param rendementMin   nombre de portions par unité d'achat, minimum
     * @param rendementMax   maximum (vide : égal au minimum)
     * @param perteSaisie    perte en pour cent (vide : 0)
     */
    public ResultatIngredientBuvette saisir(CatalogueBuvette catalogue, String id, String nom, String uniteAchat,
                                            String unitePortion, String prixSaisi, String rendementMin,
                                            String rendementMax, String perteSaisie) {
        if (nom == null || nom.isBlank()) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.NOM_MANQUANT);
        }
        OptionalLong prix = Centimes.lire(prixSaisi);
        if (prix.isEmpty()) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.PRIX_INVALIDE);
        }
        Integer min = entier(rendementMin);
        Integer max = rendementMax == null || rendementMax.isBlank() ? min : entier(rendementMax);
        if (min == null || max == null || min < 1 || max < min || max > 10_000) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.RENDEMENT_INVALIDE);
        }
        Integer perte = perteSaisie == null || perteSaisie.isBlank() ? Integer.valueOf(0)
                : entier(perteSaisie.replace("%", ""));
        if (perte == null || perte < 0 || perte > Ingredient.PERTE_MAX) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.PERTE_INVALIDE);
        }
        boolean nouveau = id == null || id.isBlank() || catalogue.ingredient(id).isEmpty();
        Ingredient.Builder b = nouveau ? Ingredient.builder().id(catalogue.idLibreIngredient(nom))
                : catalogue.ingredient(id).get().modifier();
        return enregistrer(catalogue, b.nom(nom.trim()).uniteAchat(uniteAchat).unitePortion(unitePortion)
                .prixAchatCentimes(prix.getAsLong()).rendement(min, max).pertePourcent(perte).build());
    }

    /** Enregistre un ingrédient (nom unique, prix plafonné). */
    public ResultatIngredientBuvette enregistrer(CatalogueBuvette catalogue, Ingredient i) {
        if (i.getPrixAchatCentimes() > Centimes.MAXIMUM) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.PRIX_INVALIDE);
        }
        String cle = ServiceCatalogueBuvette.cleNom(i.getNom());
        for (Ingredient autre : catalogue.getIngredients()) {
            if (!autre.getId().equals(i.getId()) && ServiceCatalogueBuvette.cleNom(autre.getNom()).equals(cle)) {
                return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.NOM_EN_DOUBLE);
            }
        }
        catalogue.enregistrerIngredient(i);
        return new ResultatIngredientBuvette(ResultatIngredientBuvette.Motif.ENREGISTRE, i);
    }

    /** Retire un ingrédient, ou le désactive s'il sert déjà (recette ou stock). */
    public ResultatIngredientBuvette retirer(CatalogueBuvette catalogue, String id) {
        Ingredient i = catalogue.ingredient(id).orElse(null);
        if (i == null) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.INCONNU);
        }
        if (catalogue.estIngredientUtilise(id)) {
            Ingredient desactive = i.modifier().actif(false).build();
            catalogue.enregistrerIngredient(desactive);
            return new ResultatIngredientBuvette(ResultatIngredientBuvette.Motif.DESACTIVE_DEJA_UTILISE, desactive);
        }
        catalogue.retirerIngredient(id);
        return new ResultatIngredientBuvette(ResultatIngredientBuvette.Motif.SUPPRIME, i);
    }

    /** Réactive (ou désactive) un ingrédient. */
    public ResultatIngredientBuvette changerActif(CatalogueBuvette catalogue, String id, boolean actif) {
        Ingredient i = catalogue.ingredient(id).orElse(null);
        if (i == null) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.INCONNU);
        }
        Ingredient m = i.modifier().actif(actif).build();
        catalogue.enregistrerIngredient(m);
        return new ResultatIngredientBuvette(ResultatIngredientBuvette.Motif.ENREGISTRE, m);
    }

    /**
     * Remplace la recette d'un produit. Chaque ingrédient doit exister au catalogue ;
     * deux lignes du même ingrédient sont additionnées.
     */
    public ResultatProduitBuvette definirRecette(CatalogueBuvette catalogue, String idProduit,
                                                 List<LigneRecette> lignes) {
        Produit p = catalogue.produit(idProduit).orElse(null);
        if (p == null) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.INCONNU);
        }
        java.util.Map<String, Integer> cumul = new java.util.LinkedHashMap<>();
        for (LigneRecette l : lignes) {
            if (catalogue.ingredient(l.idIngredient()).isEmpty()) {
                return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.INGREDIENT_INCONNU);
            }
            cumul.merge(l.idIngredient(), l.quantite(), Integer::sum);
        }
        List<LigneRecette> recette = new ArrayList<>();
        cumul.forEach((id, q) -> recette.add(new LigneRecette(id, q)));
        Produit m = p.modifier().recette(recette).build();
        catalogue.enregistrerProduit(m);
        return new ResultatProduitBuvette(ResultatProduitBuvette.Motif.ENREGISTRE, m);
    }

    /** @return les ingrédients suggérés absents du catalogue (ni par id, ni par nom). */
    public List<Ingredient> suggestionsDisponibles(CatalogueBuvette catalogue, SuggestionsBuvette suggestions) {
        List<Ingredient> res = new ArrayList<>();
        for (Ingredient s : suggestions.ingredients()) {
            if (!dejaPresent(catalogue, s)) {
                res.add(s);
            }
        }
        return res;
    }

    /** Ajoute un ingrédient suggéré (après un clic). */
    public ResultatIngredientBuvette ajouterSuggestion(CatalogueBuvette catalogue, SuggestionsBuvette suggestions,
                                                       String id) {
        Ingredient s = suggestions.ingredient(id).orElse(null);
        if (s == null) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.INCONNU);
        }
        if (dejaPresent(catalogue, s)) {
            return ResultatIngredientBuvette.refus(ResultatIngredientBuvette.Motif.DEJA_PRESENT);
        }
        return enregistrer(catalogue, s);
    }

    static boolean dejaPresent(CatalogueBuvette catalogue, Ingredient s) {
        if (catalogue.ingredient(s.getId()).isPresent()) {
            return true;
        }
        String cle = ServiceCatalogueBuvette.cleNom(s.getNom());
        return catalogue.getIngredients().stream().anyMatch(i -> ServiceCatalogueBuvette.cleNom(i.getNom()).equals(cle));
    }

    private static Integer entier(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (!t.matches("\\d{1,6}")) {
            return null;
        }
        return Integer.valueOf(t);
    }
}
