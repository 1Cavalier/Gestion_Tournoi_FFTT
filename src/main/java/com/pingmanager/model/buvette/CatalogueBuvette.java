package com.pingmanager.model.buvette;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Catalogue de la buvette d'un club (B046), réutilisé d'un tournoi à l'autre :
 * produits vendus et réglages du club.
 *
 * <p>Entité mutable gardée : les règles (nom en double, alcool non activé,
 * suppression d'un produit déjà vendu) sont contrôlées par
 * {@code core.buvette.ServiceCatalogueBuvette}, qui renvoie un résultat
 * détaillé. Le catalogue retient les produits déjà vendus pour qu'ils ne soient
 * jamais supprimés, seulement désactivés.</p>
 */
public final class CatalogueBuvette {

    private final String numeroClub;
    private final Map<String, Produit> produits = new LinkedHashMap<>();
    private final Set<String> produitsVendus = new LinkedHashSet<>();
    private boolean alcoolActive;

    public CatalogueBuvette(String numeroClub) {
        if (numeroClub == null || numeroClub.isBlank()) {
            throw new IllegalArgumentException("numéro de club obligatoire");
        }
        this.numeroClub = numeroClub.trim();
    }

    public String getNumeroClub() {
        return numeroClub;
    }

    /** @return les produits, dans l'ordre d'ajout (lecture seule). */
    public List<Produit> getProduits() {
        return Collections.unmodifiableList(new ArrayList<>(produits.values()));
    }

    public Optional<Produit> produit(String id) {
        return Optional.ofNullable(id == null ? null : produits.get(id));
    }

    /** Ajoute ou remplace un produit (même identifiant). */
    public void enregistrerProduit(Produit p) {
        produits.put(Objects.requireNonNull(p, "produit").getId(), p);
    }

    /**
     * Retire un produit. Refusé (exception) pour un produit déjà vendu : passer par
     * le service, qui le désactive à la place.
     */
    public void retirerProduit(String id) {
        if (produitsVendus.contains(id)) {
            throw new IllegalStateException("Un produit déjà vendu ne se supprime pas : il se désactive.");
        }
        produits.remove(id);
    }

    /** Retient qu'un produit a été vendu au moins une fois. */
    public void marquerVendu(String id) {
        if (id != null && !id.isBlank()) {
            produitsVendus.add(id);
        }
    }

    public boolean estVendu(String id) {
        return produitsVendus.contains(id);
    }

    public Set<String> getProduitsVendus() {
        return Collections.unmodifiableSet(produitsVendus);
    }

    /** @return vrai si la catégorie « boissons alcoolisées » a été activée à la main. */
    public boolean isAlcoolActive() {
        return alcoolActive;
    }

    public void setAlcoolActive(boolean alcoolActive) {
        this.alcoolActive = alcoolActive;
    }

    /** @return vrai si rien n'a été saisi (le dépôt ne l'écrit alors pas). */
    public boolean estVide() {
        return produits.isEmpty() && produitsVendus.isEmpty() && !alcoolActive;
    }

    /**
     * Identifiant libre dérivé d'un nom (« Croque-monsieur » → {@code croque-monsieur},
     * puis {@code croque-monsieur-2} s'il est pris).
     */
    public String idLibre(String nom) {
        String base = slug(nom);
        String id = base;
        int i = 2;
        while (produits.containsKey(id)) {
            id = base + "-" + i++;
        }
        return id;
    }

    /** Forme normalisée d'un nom : minuscules sans accents, tirets. */
    public static String slug(String nom) {
        String s = Normalizer.normalize(nom == null ? "" : nom, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "produit" : s;
    }
}
