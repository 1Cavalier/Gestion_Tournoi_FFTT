package com.pingmanager.model.buvette;

import java.util.List;
import java.util.Objects;

/**
 * Produit vendu à la buvette (B046) : nom, catégorie, unité de vente, prix de
 * vente <b>en centimes</b> et état actif. Objet immuable (builder).
 *
 * <p>Sa <b>recette</b> dit quels ingrédients il consomme (vide si le club ne la
 * renseigne pas : le coût de revient est alors inconnu).</p>
 *
 * <p>Un produit déjà vendu n'est jamais supprimé du catalogue : il est
 * désactivé, pour que l'historique des ventes garde son nom et son prix.</p>
 */
public final class Produit {

    private final String id;
    private final String nom;
    private final CategorieProduit categorie;
    private final UniteVente unite;
    private final long prixVenteCentimes;
    private final boolean actif;
    private final List<LigneRecette> recette;

    private Produit(Builder b) {
        this.id = exiger(b.id, "id");
        this.nom = exiger(b.nom, "nom");
        this.categorie = Objects.requireNonNull(b.categorie, "categorie");
        this.unite = Objects.requireNonNull(b.unite, "unite");
        if (b.prixVenteCentimes < 0) {
            throw new IllegalArgumentException("prix négatif");
        }
        this.prixVenteCentimes = b.prixVenteCentimes;
        this.actif = b.actif;
        this.recette = List.copyOf(b.recette);
    }

    private static String exiger(String s, String champ) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(champ + " obligatoire");
        }
        return s.trim();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return un builder prérempli avec les valeurs de ce produit. */
    public Builder modifier() {
        Builder b = new Builder();
        b.id = id;
        b.nom = nom;
        b.categorie = categorie;
        b.unite = unite;
        b.prixVenteCentimes = prixVenteCentimes;
        b.actif = actif;
        b.recette = recette;
        return b;
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public CategorieProduit getCategorie() {
        return categorie;
    }

    public UniteVente getUnite() {
        return unite;
    }

    public long getPrixVenteCentimes() {
        return prixVenteCentimes;
    }

    public boolean isActif() {
        return actif;
    }

    /** @return la recette (lecture seule, vide si non renseignée). */
    public List<LigneRecette> getRecette() {
        return recette;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Produit p)) {
            return false;
        }
        return id.equals(p.id) && nom.equals(p.nom) && categorie == p.categorie && unite == p.unite
                && prixVenteCentimes == p.prixVenteCentimes && actif == p.actif && recette.equals(p.recette);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, categorie, unite, prixVenteCentimes, actif, recette);
    }

    @Override
    public String toString() {
        return nom + " (" + prixVenteCentimes + " c)";
    }

    /** Builder de {@link Produit} (actif par défaut, unité « portion », catégorie « autre »). */
    public static final class Builder {
        private String id;
        private String nom;
        private CategorieProduit categorie = CategorieProduit.AUTRE;
        private UniteVente unite = UniteVente.PORTION;
        private long prixVenteCentimes;
        private boolean actif = true;
        private List<LigneRecette> recette = List.of();

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder nom(String v) {
            this.nom = v;
            return this;
        }

        public Builder categorie(CategorieProduit v) {
            this.categorie = v;
            return this;
        }

        public Builder unite(UniteVente v) {
            this.unite = v;
            return this;
        }

        public Builder prixVenteCentimes(long v) {
            this.prixVenteCentimes = v;
            return this;
        }

        public Builder actif(boolean v) {
            this.actif = v;
            return this;
        }

        public Builder recette(List<LigneRecette> v) {
            this.recette = v == null ? List.of() : v;
            return this;
        }

        public Produit build() {
            return new Produit(this);
        }
    }
}
