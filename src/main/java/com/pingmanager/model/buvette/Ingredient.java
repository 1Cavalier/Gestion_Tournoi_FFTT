package com.pingmanager.model.buvette;

import java.util.Objects;

/**
 * Ingrédient acheté pour la buvette (B046) : une <b>unité d'achat</b> (baguette,
 * carton de 24 canettes, paquet de 1 kg), son prix d'achat en centimes, son
 * <b>rendement</b> (nombre de portions vendables par unité d'achat, en fourchette
 * min-max : « 1 baguette = 2 ou 3 sandwichs ») et une perte estimée en pour cent.
 * Objet immuable (builder).
 *
 * <p>Le stock d'un ingrédient se compte en portions ; le rendement minimal sert
 * aux calculs prudents (coût de revient retenu, quantités à acheter).</p>
 */
public final class Ingredient {

    /** Perte maximale admise : 90 %. */
    public static final int PERTE_MAX = 90;

    private final String id;
    private final String nom;
    private final String uniteAchat;
    private final String unitePortion;
    private final long prixAchatCentimes;
    private final int rendementMin;
    private final int rendementMax;
    private final int pertePourcent;
    private final boolean actif;

    private Ingredient(Builder b) {
        this.id = exiger(b.id, "id");
        this.nom = exiger(b.nom, "nom");
        this.uniteAchat = b.uniteAchat == null || b.uniteAchat.isBlank() ? "unité" : b.uniteAchat.trim();
        this.unitePortion = b.unitePortion == null || b.unitePortion.isBlank() ? "portion" : b.unitePortion.trim();
        if (b.prixAchatCentimes < 0) {
            throw new IllegalArgumentException("prix négatif");
        }
        if (b.rendementMin < 1 || b.rendementMax < b.rendementMin) {
            throw new IllegalArgumentException("rendement : au moins 1, minimum ≤ maximum");
        }
        if (b.pertePourcent < 0 || b.pertePourcent > PERTE_MAX) {
            throw new IllegalArgumentException("perte entre 0 et " + PERTE_MAX + " %");
        }
        this.prixAchatCentimes = b.prixAchatCentimes;
        this.rendementMin = b.rendementMin;
        this.rendementMax = b.rendementMax;
        this.pertePourcent = b.pertePourcent;
        this.actif = b.actif;
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

    /** @return un builder prérempli avec les valeurs de cet ingrédient. */
    public Builder modifier() {
        Builder b = new Builder();
        b.id = id;
        b.nom = nom;
        b.uniteAchat = uniteAchat;
        b.unitePortion = unitePortion;
        b.prixAchatCentimes = prixAchatCentimes;
        b.rendementMin = rendementMin;
        b.rendementMax = rendementMax;
        b.pertePourcent = pertePourcent;
        b.actif = actif;
        return b;
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getUniteAchat() {
        return uniteAchat;
    }

    public String getUnitePortion() {
        return unitePortion;
    }

    public long getPrixAchatCentimes() {
        return prixAchatCentimes;
    }

    public int getRendementMin() {
        return rendementMin;
    }

    public int getRendementMax() {
        return rendementMax;
    }

    public int getPertePourcent() {
        return pertePourcent;
    }

    public boolean isActif() {
        return actif;
    }

    /** « 2 à 3 sandwichs » ou « 24 canettes ». */
    public String libelleRendement() {
        return (rendementMin == rendementMax ? String.valueOf(rendementMin) : rendementMin + " à " + rendementMax)
                + " " + unitePortion;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ingredient i)) {
            return false;
        }
        return id.equals(i.id) && nom.equals(i.nom) && uniteAchat.equals(i.uniteAchat)
                && unitePortion.equals(i.unitePortion) && prixAchatCentimes == i.prixAchatCentimes
                && rendementMin == i.rendementMin && rendementMax == i.rendementMax
                && pertePourcent == i.pertePourcent && actif == i.actif;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, uniteAchat, unitePortion, prixAchatCentimes, rendementMin, rendementMax,
                pertePourcent, actif);
    }

    @Override
    public String toString() {
        return nom;
    }

    /** Builder d'{@link Ingredient} (rendement 1, aucune perte, actif). */
    public static final class Builder {
        private String id;
        private String nom;
        private String uniteAchat;
        private String unitePortion;
        private long prixAchatCentimes;
        private int rendementMin = 1;
        private int rendementMax = 1;
        private int pertePourcent;
        private boolean actif = true;

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder nom(String v) {
            this.nom = v;
            return this;
        }

        public Builder uniteAchat(String v) {
            this.uniteAchat = v;
            return this;
        }

        public Builder unitePortion(String v) {
            this.unitePortion = v;
            return this;
        }

        public Builder prixAchatCentimes(long v) {
            this.prixAchatCentimes = v;
            return this;
        }

        /** Rendement fixe (minimum = maximum). */
        public Builder rendement(int v) {
            this.rendementMin = v;
            this.rendementMax = v;
            return this;
        }

        public Builder rendement(int min, int max) {
            this.rendementMin = min;
            this.rendementMax = max;
            return this;
        }

        public Builder pertePourcent(int v) {
            this.pertePourcent = v;
            return this;
        }

        public Builder actif(boolean v) {
            this.actif = v;
            return this;
        }

        public Ingredient build() {
            return new Ingredient(this);
        }
    }
}
