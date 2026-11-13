package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ServiceCatalogueBuvette;
import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) de l'onglet « Catalogue » de la page Buvette (B046) :
 * tuiles de synthèse, produits regroupés par catégorie, suggestions encore
 * absentes du catalogue et textes des confirmations.
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
        int sugg = service.suggestionsDisponibles(catalogue, suggestions).size();
        return List.of(
                new Tuile(String.valueOf(actifs), actifs > 1 ? "produits en vente" : "produit en vente"),
                new Tuile(String.valueOf(inactifs), inactifs > 1 ? "produits désactivés" : "produit désactivé"),
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
        String detail = com.pingmanager.core.reglement.Formats.capitale(Ui.libelle(p.getUnite())) + " · " + Centimes.euros(p.getPrixVenteCentimes())
                + (vendu ? " · déjà vendu" : "");
        return new LigneProduit(p.getId(), p.getNom(), detail, p.isActif(), vendu ? "Désactiver" : "Retirer");
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
