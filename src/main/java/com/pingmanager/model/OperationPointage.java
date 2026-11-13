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
 * <p>B043 : le <b>jour</b> du tableau (facultatif : les opérations des anciens
 * fichiers n'en ont pas, {@link #getJour()} rend alors {@code null}).</p>
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
    private final java.time.LocalDate jour;
    /** B043 : motif d'une désinscription ({@code null} : ancien fichier ou autre type). */
    private final MotifRetrait motifRetrait;
    /** B043 : le joueur avait-il payé ce tableau au moment du retrait ? */
    private final boolean avaitPaye;

    private OperationPointage(Builder b) {
        this.jour = b.jour;
        boolean retrait = b.type == TypeOperationPointage.DESINSCRIRE || b.type == TypeOperationPointage.NON_POINTE;
        this.motifRetrait = b.type == TypeOperationPointage.DESINSCRIRE ? b.motifRetrait : null;
        this.avaitPaye = retrait && b.avaitPaye;
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
            if (v == null && motifRetrait != null && !motifRetrait.exigePrecision()) {
                v = motifRetrait.getLibelle(); // lisible aussi par une version d'avant B043
            }
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

    /**
     * @return le motif d'une désinscription ; une désinscription d'avant B043 (sans
     *         motif choisi) se lit comme {@link MotifRetrait#DECISION_JOUEUR} ;
     *         {@code null} pour les autres types
     */
    public MotifRetrait getMotifRetrait() {
        if (type != TypeOperationPointage.DESINSCRIRE) {
            return null;
        }
        return motifRetrait != null ? motifRetrait : MotifRetrait.DECISION_JOUEUR;
    }

    /** @return vrai si le motif a été choisi dans la liste (B043), faux pour un ancien désistement. */
    public boolean motifRetraitNote() {
        return motifRetrait != null;
    }

    /**
     * @return la précision en texte libre d'une désinscription, ou {@code null} si
     *         la valeur n'est que le libellé du motif
     */
    public String getPrecision() {
        if (type != TypeOperationPointage.DESINSCRIRE) {
            return null;
        }
        MotifRetrait m = getMotifRetrait();
        return valeur == null || (motifRetrait != null && valeur.equals(m.getLibelle())) ? null : valeur;
    }

    /** @return vrai si le joueur avait payé ce tableau au moment du retrait (B043). */
    public boolean avaitPaye() {
        return avaitPaye;
    }

    /** @return le jour du tableau au moment de l'opération (B043), ou {@code null} (ancien fichier). */
    public java.time.LocalDate getJour() {
        return jour;
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
                && Objects.equals(valeur, x.valeur) && horodatage.equals(x.horodatage) && poste.equals(x.poste)
                && Objects.equals(jour, x.jour) && motifRetrait == x.motifRetrait && avaitPaye == x.avaitPaye;
    }

    @Override
    public int hashCode() {
        return Objects.hash(licence, tableau, type, valeur, horodatage, poste, jour, motifRetrait, avaitPaye);
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
        private java.time.LocalDate jour;
        private MotifRetrait motifRetrait;
        private boolean avaitPaye;

        private Builder() {
        }

        /** B043 : motif d'une désinscription (facultatif : sans motif = ancien désistement). */
        public Builder motifRetrait(MotifRetrait v) {
            this.motifRetrait = v;
            return this;
        }

        /** B043 : le joueur avait payé ce tableau au moment du retrait. */
        public Builder avaitPaye(boolean v) {
            this.avaitPaye = v;
            return this;
        }

        /** B043 : jour du tableau (facultatif). */
        public Builder jour(java.time.LocalDate v) {
            this.jour = v;
            return this;
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
