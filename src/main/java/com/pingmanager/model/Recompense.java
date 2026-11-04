package com.pingmanager.model;

import java.util.Objects;

/**
 * Une récompense attribuée à une place (ou une plage de places) d'un tableau.
 *
 * <p>Objet valeur immuable. Selon son {@link TypeRecompense} :</p>
 * <ul>
 *   <li>{@link TypeRecompense#ARGENT} : {@link #getMontant()} porte le montant en
 *       euros ; {@link #getLibelle()} est vide.</li>
 *   <li>{@link TypeRecompense#REDUCTION_POURCENTAGE} : {@link #getPourcentage()}
 *       porte le pourcentage brut (0 à 100) ; la base d'application sera définie
 *       plus tard.</li>
 *   <li>{@link TypeRecompense#AUTRE} : {@link #getLibelle()} décrit la récompense.</li>
 * </ul>
 *
 * <p>On utilise les fabriques {@link #argent(double)},
 * {@link #reductionPourcentage(double)} et {@link #autre(String)} plutôt que le
 * constructeur, pour garantir des instances cohérentes.</p>
 */
public final class Recompense {

    private final TypeRecompense type;
    private final double montant;      // euros, si ARGENT
    private final double pourcentage;  // 0..100, si REDUCTION_POURCENTAGE
    private final String libelle;      // texte, si AUTRE

    private Recompense(TypeRecompense type, double montant, double pourcentage, String libelle) {
        this.type = Objects.requireNonNull(type, "type");
        this.montant = montant;
        this.pourcentage = pourcentage;
        this.libelle = libelle == null ? "" : libelle;
    }

    /** Récompense en argent (euros). */
    public static Recompense argent(double montantEuros) {
        if (montantEuros < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif : " + montantEuros);
        }
        return new Recompense(TypeRecompense.ARGENT, montantEuros, 0, "");
    }

    /** Récompense sous forme de réduction en pourcentage brut (0 à 100). */
    public static Recompense reductionPourcentage(double pourcentage) {
        if (pourcentage < 0 || pourcentage > 100) {
            throw new IllegalArgumentException("Le pourcentage doit être compris entre 0 et 100 : " + pourcentage);
        }
        return new Recompense(TypeRecompense.REDUCTION_POURCENTAGE, 0, pourcentage, "");
    }

    /**
     * Part des droits d'engagement perçus sur le tableau (0 &lt; % ≤ 100) : la
     * dotation suit le remplissage, ce qui évite un tableau à perte (courant pour
     * le tableau dames : 50 % au vainqueur, 30 % au finaliste, 10 % à chaque
     * demi-finaliste).
     */
    public static Recompense partInscriptions(double pourcentage) {
        if (pourcentage <= 0 || pourcentage > 100) {
            throw new IllegalArgumentException("La part des inscriptions doit être comprise entre 0 et 100 % : "
                    + pourcentage);
        }
        return new Recompense(TypeRecompense.PART_INSCRIPTIONS, 0, pourcentage, "");
    }

    /** Récompense décrite librement (coupe, lot…). */
    public static Recompense autre(String libelle) {
        Objects.requireNonNull(libelle, "libelle");
        if (libelle.isBlank()) {
            throw new IllegalArgumentException("Le libellé d'une récompense « autre » ne peut pas être vide.");
        }
        return new Recompense(TypeRecompense.AUTRE, 0, 0, libelle);
    }

    public TypeRecompense getType() {
        return type;
    }

    public double getMontant() {
        return montant;
    }

    public double getPourcentage() {
        return pourcentage;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        // Pas de switch sur l'énumération (classe annexe générée par javac).
        if (type == TypeRecompense.ARGENT) {
            return montant + " €";
        }
        if (type == TypeRecompense.REDUCTION_POURCENTAGE) {
            return "-" + pourcentage + " %";
        }
        if (type == TypeRecompense.PART_INSCRIPTIONS) {
            return pourcentage + " % des inscriptions";
        }
        return libelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recompense autre)) return false;
        return Double.compare(montant, autre.montant) == 0
                && Double.compare(pourcentage, autre.pourcentage) == 0
                && type == autre.type
                && libelle.equals(autre.libelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, montant, pourcentage, libelle);
    }
}
