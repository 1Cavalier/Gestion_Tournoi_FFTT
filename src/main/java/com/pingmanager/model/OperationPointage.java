package com.pingmanager.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Une action faite sur un poste de pointage (B041) : <b>qui</b> (licence du
 * joueur), <b>où</b> (nom du tableau), <b>quoi</b> (type et valeur),
 * <b>quand</b> (horodatage) et <b>depuis quel poste</b>.
 *
 * <p>Objet valeur immuable, construit par {@link #builder()}. Le journal de ces
 * opérations ({@link Tournoi#getJournalPointage()}) prépare la synchronisation
 * entre le poste de pointage et l'ordinateur du juge-arbitre : chaque opération
 * se rejoue sans ambiguïté, car elle désigne le joueur par sa licence et le
 * tableau par son nom.</p>
 *
 * <p>Valeur : le nom du {@link ModeReglement} pour {@link TypeOperationPointage#MARQUER_PAYE}
 * (obligatoire), le motif pour {@link TypeOperationPointage#DESINSCRIRE}
 * (obligatoire), {@code null} pour les autres types.</p>
 */
public final class OperationPointage {

    private final String licence;
    private final String tableau;
    private final TypeOperationPointage type;
    private final String valeur;
    private final Instant horodatage;
    private final String poste;

    private OperationPointage(Builder b) {
        this.licence = exigerTexte(b.licence, "licence");
        this.tableau = exigerTexte(b.tableau, "tableau");
        this.type = Objects.requireNonNull(b.type, "type");
        this.horodatage = Objects.requireNonNull(b.horodatage, "horodatage");
        this.poste = exigerTexte(b.poste, "poste");
        String v = b.valeur == null || b.valeur.isBlank() ? null : b.valeur.trim();
        if (type == TypeOperationPointage.MARQUER_PAYE) {
            if (v == null) {
                throw new IllegalArgumentException("Le mode de règlement est obligatoire.");
            }
            ModeReglement.valueOf(v); // refuse un mode inconnu
        } else if (type == TypeOperationPointage.DESINSCRIRE) {
            if (v == null) {
                throw new IllegalArgumentException("Le motif du désistement est obligatoire.");
            }
        } else {
            v = null;
        }
        this.valeur = v;
    }

    private static String exigerTexte(String s, String champ) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Champ obligatoire : " + champ + ".");
        }
        return s.trim();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLicence() {
        return licence;
    }

    public String getTableau() {
        return tableau;
    }

    public TypeOperationPointage getType() {
        return type;
    }

    /** @return le nom du mode de règlement, le motif du désistement, ou {@code null}. */
    public String getValeur() {
        return valeur;
    }

    /** @return le mode de règlement d'une opération {@code MARQUER_PAYE}, sinon {@code null}. */
    public ModeReglement getModeReglement() {
        return type == TypeOperationPointage.MARQUER_PAYE ? ModeReglement.valueOf(valeur) : null;
    }

    public Instant getHorodatage() {
        return horodatage;
    }

    public String getPoste() {
        return poste;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationPointage)) {
            return false;
        }
        OperationPointage x = (OperationPointage) o;
        return licence.equals(x.licence) && tableau.equals(x.tableau) && type == x.type
                && Objects.equals(valeur, x.valeur) && horodatage.equals(x.horodatage) && poste.equals(x.poste);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licence, tableau, type, valeur, horodatage, poste);
    }

    @Override
    public String toString() {
        return horodatage + " [" + poste + "] " + type + " " + licence + " / " + tableau
                + (valeur == null ? "" : " : " + valeur);
    }

    /** Construction pas à pas d'une {@link OperationPointage}. */
    public static final class Builder {
        private String licence;
        private String tableau;
        private TypeOperationPointage type;
        private String valeur;
        private Instant horodatage;
        private String poste;

        private Builder() {
        }

        public Builder licence(String v) {
            this.licence = v;
            return this;
        }

        public Builder tableau(String v) {
            this.tableau = v;
            return this;
        }

        public Builder type(TypeOperationPointage v) {
            this.type = v;
            return this;
        }

        public Builder valeur(String v) {
            this.valeur = v;
            return this;
        }

        public Builder horodatage(Instant v) {
            this.horodatage = v;
            return this;
        }

        public Builder poste(String v) {
            this.poste = v;
            return this;
        }

        public OperationPointage build() {
            return new OperationPointage(this);
        }
    }
}
