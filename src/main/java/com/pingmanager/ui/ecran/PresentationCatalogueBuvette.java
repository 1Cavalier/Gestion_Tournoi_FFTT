package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.CalculCoutBuvette;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.CoutProduit;
import com.pingmanager.core.buvette.ServiceIngredientsBuvette;
import com.pingmanager.core.buvette.ServiceCatalogueBuvette;
import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) de l'onglet « Catalogue » de la page Buvette (B046) :
 * tuiles de synthèse, produits regroupés par catégorie (avec coût de revient et
 * marge), ingrédients, recette du produit choisi, suggestions encore absentes du
 * catalogue et textes des confirmations.
 */
final class PresentationCatalogueBuvette {

    /** Une tuile de chiffre clé. */
    record Tuile(String valeur, String libelle) {
    }

    /**
     * Une ligne de produit.
     *
     * @param actionRetrait « Retirer » (jamais vendu) ou « Désactiver » (déjà vendu)
     */
    record LigneProduit(String id, String nom, String detail, boolean actif, String actionRetrait) {
    }

    /** Les produits d'une catégorie. */
    record Groupe(String titre, List<LigneProduit> lignes) {
    }

    /** Une suggestion pas encore ajoutée. */
    record LigneSuggestion(String id, String nom, String detail) {
    }

    /** Une ligne d'ingrédient (actionRetrait : « Retirer » ou « Désactiver »). */
    record LigneIngredient(String id, String nom, String detail, boolean actif, String actionRetrait) {
    }

    /** Une ligne de la recette affichée : ingrédient, quantité et explication du coût. */
    record LigneRecetteAffichee(String idIngredient, int quantite, String explication) {
    }

    /** Recette d'un produit : lignes, phrase de synthèse et ingrédients proposables. */
    record Recette(String titre, List<LigneRecetteAffichee> lignes, String synthese, List<String> manquants) {
    }

    private static final CalculCoutBuvette CALCUL = new CalculCoutBuvette();
    private final ServiceIngredientsBuvette serviceIngredients = new ServiceIngredientsBuvette();

    private final CatalogueBuvette catalogue;
    private final SuggestionsBuvette suggestions;
    private final ServiceCatalogueBuvette service;

    PresentationCatalogueBuvette(CatalogueBuvette catalogue, SuggestionsBuvette suggestions,
                                 ServiceCatalogueBuvette service) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.suggestions = Objects.requireNonNull(suggestions, "suggestions");
        this.service = Objects.requireNonNull(service, "service");
    }

    List<Tuile> tuiles() {
        long actifs = catalogue.getProduits().stream().filter(Produit::isActif).count();
        long inactifs = catalogue.getProduits().size() - actifs;
        int sugg = service.suggestionsDisponibles(catalogue, suggestions).size()
                + serviceIngredients.suggestionsDisponibles(catalogue, suggestions).size();
        int ingr = catalogue.getIngredients().size();
        return List.of(
                new Tuile(String.valueOf(actifs), actifs > 1 ? "produits en vente" : "produit en vente"),
                new Tuile(String.valueOf(inactifs), inactifs > 1 ? "produits désactivés" : "produit désactivé"),
                new Tuile(String.valueOf(ingr), ingr > 1 ? "ingrédients" : "ingrédient"),
                new Tuile(String.valueOf(sugg), sugg > 1 ? "suggestions à examiner" : "suggestion à examiner"));
    }

    boolean vide() {
        return catalogue.getProduits().isEmpty();
    }

    String messageVide() {
        return "Aucun produit pour l'instant. Ajoutez vos produits ou partez des suggestions ci-dessous.";
    }

    /** Produits regroupés par catégorie (ordre de l'énumération), catégories vides omises. */
    List<Groupe> groupes() {
        List<Groupe> res = new ArrayList<>();
        for (CategorieProduit c : CategorieProduit.values()) {
            List<LigneProduit> lignes = new ArrayList<>();
            for (Produit p : catalogue.getProduits()) {
                if (p.getCategorie() == c) {
                    lignes.add(ligne(p));
                }
            }
            if (!lignes.isEmpty()) {
                res.add(new Groupe(c.getLibelle(), lignes));
            }
        }
        return res;
    }

    private LigneProduit ligne(Produit p) {
        boolean vendu = catalogue.estVendu(p.getId());
        String detail = com.pingmanager.core.reglement.Formats.capitale(Ui.libelle(p.getUnite())) + " · "
                + Centimes.euros(p.getPrixVenteCentimes()) + " · " + resumeCout(CALCUL.cout(p, catalogue))
                + (vendu ? " · déjà vendu" : "");
        return new LigneProduit(p.getId(), p.getNom(), detail, p.isActif(), vendu ? "Désactiver" : "Retirer");
    }

    /** « coût 1,11 € · marge 1,89 € », ou « recette à renseigner ». */
    static String resumeCout(CoutProduit c) {
        if (!c.recetteRenseignee()) {
            return "recette à renseigner";
        }
        if (!c.connu()) {
            return "recette incomplète";
        }
        return "coût " + Centimes.euros(c.coutCentimes()) + " · marge " + Centimes.euros(c.margeCentimes());
    }

    /** Ingrédients du club, dans l'ordre d'ajout. */
    List<LigneIngredient> ingredients() {
        List<LigneIngredient> res = new ArrayList<>();
        for (Ingredient i : catalogue.getIngredients()) {
            boolean utilise = catalogue.estIngredientUtilise(i.getId());
            res.add(new LigneIngredient(i.getId(), i.getNom(), detailIngredient(i), i.isActif(),
                    utilise ? "Désactiver" : "Retirer"));
        }
        return res;
    }

    /** « Baguette à 1,10 € · 2 à 3 sandwich · perte 5 % · 0,39 € la portion ». */
    static String detailIngredient(Ingredient i) {
        return com.pingmanager.core.reglement.Formats.capitale(i.getUniteAchat()) + " à "
                + Centimes.euros(i.getPrixAchatCentimes()) + " · " + i.libelleRendement()
                + (i.getPertePourcent() > 0 ? " · perte " + i.getPertePourcent() + " %" : "")
                + " · " + Centimes.euros(Centimes.arrondir(CalculCoutBuvette.coutPortion(i, true))) + " la portion";
    }

    /** Suggestions d'ingrédients pas encore ajoutées. */
    List<LigneSuggestion> suggestionsIngredients() {
        List<LigneSuggestion> res = new ArrayList<>();
        for (Ingredient i : serviceIngredients.suggestionsDisponibles(catalogue, suggestions)) {
            res.add(new LigneSuggestion(i.getId(), i.getNom(), detailIngredient(i)));
        }
        return res;
    }

    /** Recette du produit choisi, expliquée ligne à ligne ; vide si le produit n'existe plus. */
    java.util.Optional<Recette> recette(String idProduit) {
        Produit p = catalogue.produit(idProduit).orElse(null);
        if (p == null) {
            return java.util.Optional.empty();
        }
        CoutProduit c = CALCUL.cout(p, catalogue);
        List<LigneRecetteAffichee> lignes = new ArrayList<>();
        for (LigneRecette l : p.getRecette()) {
            String expl = c.lignes().stream().filter(x -> x.idIngredient().equals(l.idIngredient()))
                    .map(CoutProduit.LigneCout::explication).findFirst()
                    .orElse(l.idIngredient() + " : ingrédient introuvable");
            lignes.add(new LigneRecetteAffichee(l.idIngredient(), l.quantite(), expl));
        }
        return java.util.Optional.of(new Recette("Recette : " + p.getNom(), lignes, synthese(c), c.ingredientsManquants()));
    }

    /**
     * « Coût de revient 1,11 € (de 0,98 € à 1,11 € selon le rendement) · marge 1,89 € ·
     * taux de marge 170 % · taux de marque 63 % ».
     */
    static String synthese(CoutProduit c) {
        if (!c.recetteRenseignee()) {
            return "Ajoutez les ingrédients de ce produit pour calculer son coût de revient et sa marge.";
        }
        if (!c.connu()) {
            return "Recette incomplète : un ingrédient a disparu du catalogue.";
        }
        StringBuilder sb = new StringBuilder("Coût de revient ").append(Centimes.euros(c.coutCentimes()));
        if (c.coutOptimisteCentimes() != c.coutCentimes()) {
            sb.append(" (de ").append(Centimes.euros(c.coutOptimisteCentimes())).append(" à ")
                    .append(Centimes.euros(c.coutCentimes())).append(" selon le rendement)");
        }
        sb.append(" · marge ").append(Centimes.euros(c.margeCentimes()));
        c.tauxDeMargePourcent().ifPresent(t -> sb.append(" · taux de marge ").append(t).append(" %"));
        c.tauxDeMarquePourcent().ifPresent(t -> sb.append(" · taux de marque ").append(t).append(" %"));
        return sb.toString();
    }

    /** Ingrédients actifs proposés pour compléter une recette. */
    List<Ingredient> ingredientsProposables() {
        return catalogue.getIngredients().stream().filter(Ingredient::isActif).toList();
    }

    List<LigneSuggestion> suggestions() {
        List<LigneSuggestion> res = new ArrayList<>();
        for (Produit p : service.suggestionsDisponibles(catalogue, suggestions)) {
            res.add(new LigneSuggestion(p.getId(), p.getNom(), p.getCategorie().getLibelle() + " · "
                    + Ui.libelle(p.getUnite()) + " · " + Centimes.euros(p.getPrixVenteCentimes())));
        }
        return res;
    }

    String mentionSuggestions() {
        return SuggestionsBuvette.MENTION + ". " + suggestions.avertissement();
    }

    String confirmationToutAjouter() {
        int n = suggestions().size();
        return "Ajouter " + (n > 1 ? "les " + n + " produits suggérés" : "le produit suggéré")
                + " au catalogue du club ? Les prix sont indicatifs : modifiez-les ensuite si besoin.";
    }

    /** Catégories proposées dans le formulaire : l'alcool seulement s'il a été activé. */
    List<CategorieProduit> categoriesSaisissables() {
        List<CategorieProduit> res = new ArrayList<>();
        for (CategorieProduit c : CategorieProduit.values()) {
            if (!c.estAlcoolisee() || catalogue.isAlcoolActive()) {
                res.add(c);
            }
        }
        return res;
    }

    /** Texte du bouton de bascule de la catégorie alcoolisée. */
    String actionAlcool() {
        return catalogue.isAlcoolActive()
                ? "Désactiver la catégorie « Boissons alcoolisées »"
                : "Activer la catégorie « Boissons alcoolisées »…";
    }
}
