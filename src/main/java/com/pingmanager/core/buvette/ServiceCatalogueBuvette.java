package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Règles du catalogue de la buvette (B046) : enregistrement d'un produit
 * contrôlé, désactivation, retrait (jamais d'un produit déjà vendu),
 * activation manuelle de la catégorie « alcoolisées », et acceptation des
 * suggestions (une par une ou toutes ; un produit suggéré amène les ingrédients
 * suggérés de sa recette).
 */
public final class ServiceCatalogueBuvette {

    /**
     * Avertissement montré avant d'activer la catégorie alcoolisée. Il n'affirme
     * aucune règle : les conditions légales sont une question ouverte (README, section 6).
     */
    public static final String AVERTISSEMENT_ALCOOL = "La vente de boissons alcoolisées dans une buvette "
            + "temporaire peut nécessiter une autorisation de la mairie et obéit à des règles propres. "
            + "Renseignez-vous avant de vendre : le logiciel ne vérifie rien à votre place.";

    /**
     * Enregistre un produit nouveau ou modifié (même identifiant).
     *
     * @return {@code ENREGISTRE}, ou le motif du refus (nom manquant ou en double,
     *         prix hors bornes, catégorie alcoolisée non activée)
     */
    public ResultatProduitBuvette enregistrer(CatalogueBuvette catalogue, Produit produit) {
        Objects.requireNonNull(catalogue, "catalogue");
        Objects.requireNonNull(produit, "produit");
        if (produit.getPrixVenteCentimes() > Centimes.MAXIMUM) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.PRIX_INVALIDE);
        }
        if (produit.getCategorie().estAlcoolisee() && !catalogue.isAlcoolActive()) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.ALCOOL_NON_ACTIVE);
        }
        String cle = cleNom(produit.getNom());
        for (Produit p : catalogue.getProduits()) {
            if (!p.getId().equals(produit.getId()) && cleNom(p.getNom()).equals(cle)) {
                return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.NOM_EN_DOUBLE);
            }
        }
        catalogue.enregistrerProduit(produit);
        return new ResultatProduitBuvette(ResultatProduitBuvette.Motif.ENREGISTRE, produit);
    }

    /**
     * Saisie d'un produit depuis le formulaire : contrôle le nom et le prix saisis
     * en euros, puis enregistre (nouveau produit si {@code id} est vide).
     */
    public ResultatProduitBuvette saisir(CatalogueBuvette catalogue, String id, String nom,
                                         com.pingmanager.model.buvette.CategorieProduit categorie,
                                         com.pingmanager.model.buvette.UniteVente unite, String prixSaisi) {
        if (nom == null || nom.isBlank()) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.NOM_MANQUANT);
        }
        java.util.OptionalLong prix = Centimes.lire(prixSaisi);
        if (prix.isEmpty()) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.PRIX_INVALIDE);
        }
        boolean nouveau = id == null || id.isBlank() || catalogue.produit(id).isEmpty();
        Produit.Builder b = nouveau
                ? Produit.builder().id(catalogue.idLibre(nom))
                : catalogue.produit(id).get().modifier();
        return enregistrer(catalogue, b.nom(nom.trim())
                .categorie(categorie == null ? com.pingmanager.model.buvette.CategorieProduit.AUTRE : categorie)
                .unite(unite == null ? com.pingmanager.model.buvette.UniteVente.PORTION : unite)
                .prixVenteCentimes(prix.getAsLong())
                .build());
    }

    /** Active ou désactive un produit (un produit désactivé n'apparaît plus en caisse). */
    public ResultatProduitBuvette changerActif(CatalogueBuvette catalogue, String id, boolean actif) {
        return catalogue.produit(id)
                .map(p -> {
                    Produit modifie = p.modifier().actif(actif).build();
                    catalogue.enregistrerProduit(modifie);
                    return new ResultatProduitBuvette(ResultatProduitBuvette.Motif.ENREGISTRE, modifie);
                })
                .orElse(ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.INCONNU));
    }

    /**
     * Retire un produit du catalogue ; s'il a déjà été vendu, il est seulement
     * désactivé ({@code DESACTIVE_DEJA_VENDU}) pour garder l'historique.
     */
    public ResultatProduitBuvette retirer(CatalogueBuvette catalogue, String id) {
        if (catalogue.produit(id).isEmpty()) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.INCONNU);
        }
        if (catalogue.estVendu(id)) {
            Produit desactive = catalogue.produit(id).get().modifier().actif(false).build();
            catalogue.enregistrerProduit(desactive);
            return new ResultatProduitBuvette(ResultatProduitBuvette.Motif.DESACTIVE_DEJA_VENDU, desactive);
        }
        Produit retire = catalogue.produit(id).get();
        catalogue.retirerProduit(id);
        return new ResultatProduitBuvette(ResultatProduitBuvette.Motif.SUPPRIME, retire);
    }

    /**
     * Active la catégorie « boissons alcoolisées », après que l'utilisateur a lu
     * {@link #AVERTISSEMENT_ALCOOL} (l'écran demande une confirmation).
     */
    public void activerAlcool(CatalogueBuvette catalogue, boolean active) {
        catalogue.setAlcoolActive(active);
    }

    /** @return les suggestions de produits qui ne sont pas encore au catalogue (ni par id, ni par nom). */
    public List<Produit> suggestionsDisponibles(CatalogueBuvette catalogue, SuggestionsBuvette suggestions) {
        List<Produit> res = new ArrayList<>();
        for (Produit s : suggestions.produits()) {
            if (!dejaPresent(catalogue, s)) {
                res.add(s);
            }
        }
        return res;
    }

    /** Ajoute une suggestion au catalogue du club (après le clic « Ajouter »). */
    public ResultatProduitBuvette ajouterSuggestion(CatalogueBuvette catalogue, SuggestionsBuvette suggestions,
                                                     String idSuggestion) {
        Produit s = suggestions.produit(idSuggestion).orElse(null);
        if (s == null) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.INCONNU);
        }
        if (dejaPresent(catalogue, s)) {
            return ResultatProduitBuvette.refus(ResultatProduitBuvette.Motif.DEJA_PRESENT);
        }
        // B046 partie 2 : la recette suggérée amène ses ingrédients suggérés manquants ;
        // une ligne dont l'ingrédient reste introuvable est écartée.
        java.util.List<com.pingmanager.model.buvette.LigneRecette> recette = new ArrayList<>();
        for (com.pingmanager.model.buvette.LigneRecette l : s.getRecette()) {
            if (catalogue.ingredient(l.idIngredient()).isEmpty()) {
                suggestions.ingredient(l.idIngredient())
                        .filter(i -> !ServiceIngredientsBuvette.dejaPresent(catalogue, i))
                        .ifPresent(catalogue::enregistrerIngredient);
            }
            if (catalogue.ingredient(l.idIngredient()).isPresent()) {
                recette.add(l);
            }
        }
        return enregistrer(catalogue, s.modifier().recette(recette).build());
    }

    /**
     * Ajoute toutes les suggestions encore absentes (après confirmation à l'écran).
     *
     * @return le nombre de produits ajoutés
     */
    public int ajouterToutesSuggestions(CatalogueBuvette catalogue, SuggestionsBuvette suggestions) {
        int n = 0;
        for (Produit s : suggestionsDisponibles(catalogue, suggestions)) {
            if (ajouterSuggestion(catalogue, suggestions, s.getId()).accepte()) {
                n++;
            }
        }
        return n;
    }

    private static boolean dejaPresent(CatalogueBuvette catalogue, Produit s) {
        if (catalogue.produit(s.getId()).isPresent()) {
            return true;
        }
        String cle = cleNom(s.getNom());
        return catalogue.getProduits().stream().anyMatch(p -> cleNom(p.getNom()).equals(cle));
    }

    /** Nom comparé sans casse ni accents ni espaces superflus. */
    static String cleNom(String nom) {
        return CatalogueBuvette.slug(nom).toLowerCase(Locale.FRENCH);
    }
}
