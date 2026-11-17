package com.pingmanager.model.buvette;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Article suivi en stock par la buvette d'un club (B026) : un produit (une
 * unité = une portion servie : une dose de café, une canette, une part de
 * gâteau) ou un contenant (un gobelet, une touillette). Objet immuable (builder).
 *
 * <p>Le coût se saisit comme on achète : le <b>prix d'un lot</b> (en centimes)
 * et le <b>nombre d'unités dans le lot</b> (« 4,50 € les 100 gobelets ») ; le
 * coût unitaire exact est calculé par {@code core.buvette.CalculCoutBuvette},
 * sans arrondi intermédiaire.</p>
 *
 * <p>Seul un produit a des contenants ; ceux d'un contenant sont ignorés.</p>
 *
 * <p>C'est un <b>article du club</b> : créé de zéro ou copié du catalogue de
 * base ({@link #getIdCatalogue()}), puis modifié librement. Il porte un prix
 * de vente (mode « Stock et ventes ») et peut être <b>archivé</b> : retiré de
 * la vente et des listes, sans perdre son historique.</p>
 */
public final class ArticleBuvette {

    private final String id;
    private final String nom;
    private final FamilleArticle famille;
    private final String unite;
    private final long prixLotCentimes;
    private final int unitesParLot;
    private final long stock;
    private final long seuilAlerte;
    private final List<LienContenant> contenants;
    private final RayonBuvette rayon;
    private final long prixVenteCentimes;
    private final boolean archive;
    private final String idCatalogue;

    private ArticleBuvette(Builder b) {
        this.id = exiger(b.id, "id");
        this.nom = exiger(b.nom, "nom");
        this.famille = Objects.requireNonNull(b.famille, "famille");
        this.unite = b.unite == null || b.unite.isBlank() ? "unité" : b.unite.trim();
        if (b.prixLotCentimes < 0) {
            throw new IllegalArgumentException("prix négatif");
        }
        if (b.unitesParLot < 1) {
            throw new IllegalArgumentException("au moins une unité par lot");
        }
        if (b.stock < 0 || b.seuilAlerte < 0) {
            throw new IllegalArgumentException("stock et seuil positifs ou nuls");
        }
        this.prixLotCentimes = b.prixLotCentimes;
        this.unitesParLot = b.unitesParLot;
        this.stock = b.stock;
        this.seuilAlerte = b.seuilAlerte;
        this.contenants = famille == FamilleArticle.PRODUIT ? sansDoublon(b.contenants) : List.of();
        this.rayon = RayonBuvette.accorder(b.rayon, famille);
        if (b.prixVenteCentimes < 0) {
            throw new IllegalArgumentException("prix de vente négatif");
        }
        this.prixVenteCentimes = b.prixVenteCentimes;
        this.archive = b.archive;
        this.idCatalogue = b.idCatalogue == null ? "" : b.idCatalogue.trim();
    }

    /** Un même contenant cité deux fois : les quantités s'additionnent. */
    private static List<LienContenant> sansDoublon(List<LienContenant> liens) {
        List<LienContenant> res = new ArrayList<>();
        for (LienContenant l : liens) {
            Objects.requireNonNull(l, "contenant");
            int existant = -1;
            for (int i = 0; i < res.size(); i++) {
                if (res.get(i).idContenant().equals(l.idContenant())) {
                    existant = i;
                }
            }
            if (existant < 0) {
                res.add(l);
            } else {
                res.set(existant, new LienContenant(l.idContenant(), res.get(existant).quantite() + l.quantite()));
            }
        }
        return List.copyOf(res);
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

    /** @return un builder prérempli avec les valeurs de cet article. */
    public Builder modifier() {
        Builder b = new Builder();
        b.id = id;
        b.nom = nom;
        b.famille = famille;
        b.unite = unite;
        b.prixLotCentimes = prixLotCentimes;
        b.unitesParLot = unitesParLot;
        b.stock = stock;
        b.seuilAlerte = seuilAlerte;
        b.contenants = contenants;
        b.rayon = rayon;
        b.prixVenteCentimes = prixVenteCentimes;
        b.archive = archive;
        b.idCatalogue = idCatalogue;
        return b;
    }

    /** @return une copie de cet article avec un autre stock. */
    public ArticleBuvette avecStock(long nouveauStock) {
        return modifier().stock(nouveauStock).build();
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public FamilleArticle getFamille() {
        return famille;
    }

    public boolean estProduit() {
        return famille == FamilleArticle.PRODUIT;
    }

    /** Unité comptée en stock (« dose », « gobelet », « canette »…). */
    public String getUnite() {
        return unite;
    }

    public long getPrixLotCentimes() {
        return prixLotCentimes;
    }

    public int getUnitesParLot() {
        return unitesParLot;
    }

    /** Quantité en stock, en unités (jamais négative). */
    public long getStock() {
        return stock;
    }

    /** Seuil d'alerte, en unités : à ce niveau ou en dessous, il faut racheter. */
    public long getSeuilAlerte() {
        return seuilAlerte;
    }

    /** @return les contenants utilisés à chaque portion (vide pour un contenant). */
    public List<LienContenant> getContenants() {
        return contenants;
    }

    /** Rayon (toujours {@link RayonBuvette#CONTENANTS} pour un contenant). */
    public RayonBuvette getRayon() {
        return rayon;
    }

    /** Prix de vente d'une portion, en centimes (0 : pas encore fixé). */
    public long getPrixVenteCentimes() {
        return prixVenteCentimes;
    }

    /** @return vrai si l'article est retiré de la vente (historique gardé). */
    public boolean estArchive() {
        return archive;
    }

    /** Article du catalogue de base dont celui-ci est la copie (vide : créé de zéro). */
    public String getIdCatalogue() {
        return idCatalogue;
    }

    /** @return une copie de cet article, archivée ou remise en vente. */
    public ArticleBuvette avecArchive(boolean v) {
        return modifier().archive(v).build();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArticleBuvette a)) {
            return false;
        }
        return id.equals(a.id) && nom.equals(a.nom) && famille == a.famille && unite.equals(a.unite)
                && prixLotCentimes == a.prixLotCentimes && unitesParLot == a.unitesParLot && stock == a.stock
                && seuilAlerte == a.seuilAlerte && contenants.equals(a.contenants) && rayon == a.rayon
                && prixVenteCentimes == a.prixVenteCentimes && archive == a.archive
                && idCatalogue.equals(a.idCatalogue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, famille, unite, prixLotCentimes, unitesParLot, stock, seuilAlerte, contenants,
                rayon, prixVenteCentimes, archive, idCatalogue);
    }

    @Override
    public String toString() {
        return nom + " (" + stock + " " + unite + ")";
    }

    /** Builder d'{@link ArticleBuvette} (produit, lot d'une unité, stock et seuil à 0). */
    public static final class Builder {
        private String id;
        private String nom;
        private FamilleArticle famille = FamilleArticle.PRODUIT;
        private String unite;
        private long prixLotCentimes;
        private int unitesParLot = 1;
        private long stock;
        private long seuilAlerte;
        private List<LienContenant> contenants = List.of();
        private RayonBuvette rayon;
        private long prixVenteCentimes;
        private boolean archive;
        private String idCatalogue;

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder nom(String v) {
            this.nom = v;
            return this;
        }

        public Builder famille(FamilleArticle v) {
            this.famille = v;
            return this;
        }

        public Builder unite(String v) {
            this.unite = v;
            return this;
        }

        /** Prix payé pour un lot, en centimes. */
        public Builder prixLotCentimes(long v) {
            this.prixLotCentimes = v;
            return this;
        }

        public Builder unitesParLot(int v) {
            this.unitesParLot = v;
            return this;
        }

        /** Raccourci : prix d'un lot et nombre d'unités dans ce lot. */
        public Builder lot(long prixCentimes, int unites) {
            this.prixLotCentimes = prixCentimes;
            this.unitesParLot = unites;
            return this;
        }

        public Builder stock(long v) {
            this.stock = v;
            return this;
        }

        public Builder seuilAlerte(long v) {
            this.seuilAlerte = v;
            return this;
        }

        public Builder contenants(List<LienContenant> v) {
            this.contenants = v == null ? List.of() : v;
            return this;
        }

        /** Rayon ({@code null} : déduit de la famille). */
        public Builder rayon(RayonBuvette v) {
            this.rayon = v;
            return this;
        }

        /** Prix de vente d'une portion, en centimes. */
        public Builder prixVenteCentimes(long v) {
            this.prixVenteCentimes = v;
            return this;
        }

        public Builder archive(boolean v) {
            this.archive = v;
            return this;
        }

        /** Article du catalogue de base copié (vide ou {@code null} : créé de zéro). */
        public Builder idCatalogue(String v) {
            this.idCatalogue = v;
            return this;
        }

        public ArticleBuvette build() {
            return new ArticleBuvette(this);
        }
    }
}
