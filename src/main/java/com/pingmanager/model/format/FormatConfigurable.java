package com.pingmanager.model.format;

import com.pingmanager.model.FormatPhaseFinale;

import java.util.Objects;

/**
 * Format de tournoi <b>paramétrable</b>, construit via un {@link Builder}.
 *
 * <p>Permet de régler finement les options d'un format sans créer une classe
 * dédiée : taille de poule, qualifiés, sort des non-qualifiés, phase finale,
 * best-of standard, et surtout <b>best-of spécifiques à la finale et à la petite
 * finale</b> ainsi que l'activation d'une petite finale (match pour la 3e place).</p>
 *
 * <p>Exemple : le format « poule de 3, 2 qualifiés » avec une finale au meilleur
 * des 7 manches (best-of 7, soit 4 manches gagnantes) et une petite finale :</p>
 * <pre>{@code
 * FormatTournoi f = FormatConfigurable.builder()
 *         .nom("Poule de 3 — finale en 7 manches")
 *         .identifiant("P3Q2-F7")
 *         .taillePouleSouhaitee(3)
 *         .qualifiesParPoule(2)
 *         .nombreManchesGagnantes(3)          // best-of 5 en général
 *         .nombreManchesGagnantesFinale(4)     // best-of 7 en finale
 *         .avecPetiteFinale(true)
 *         .build();
 * }</pre>
 */
public final class FormatConfigurable implements FormatTournoi {

    private final String nom;
    private final String identifiant;
    private final int taillePouleSouhaitee;
    private final int qualifiesParPoule;
    private final SortNonQualifies sortNonQualifies;
    private final FormatPhaseFinale formatPhaseFinale;
    private final int nombreManchesGagnantes;
    private final int nombreManchesGagnantesFinale;
    private final boolean avecPetiteFinale;
    private final int nombreManchesGagnantesPetiteFinale;

    private FormatConfigurable(Builder b) {
        this.nom = b.nom;
        this.identifiant = b.identifiant;
        this.taillePouleSouhaitee = b.taillePouleSouhaitee;
        this.qualifiesParPoule = b.qualifiesParPoule;
        this.sortNonQualifies = b.sortNonQualifies;
        this.formatPhaseFinale = b.formatPhaseFinale;
        this.nombreManchesGagnantes = b.nombreManchesGagnantes;
        this.nombreManchesGagnantesFinale = b.nombreManchesGagnantesFinale;
        this.avecPetiteFinale = b.avecPetiteFinale;
        this.nombreManchesGagnantesPetiteFinale = b.nombreManchesGagnantesPetiteFinale;
    }

    @Override public String nom() { return nom; }
    @Override public String identifiant() { return identifiant; }
    @Override public int taillePouleSouhaitee() { return taillePouleSouhaitee; }
    @Override public int qualifiesParPoule() { return qualifiesParPoule; }
    @Override public SortNonQualifies sortNonQualifies() { return sortNonQualifies; }
    @Override public FormatPhaseFinale formatPhaseFinale() { return formatPhaseFinale; }
    @Override public int nombreManchesGagnantes() { return nombreManchesGagnantes; }
    @Override public int nombreManchesGagnantesFinale() { return nombreManchesGagnantesFinale; }
    @Override public boolean avecPetiteFinale() { return avecPetiteFinale; }
    @Override public int nombreManchesGagnantesPetiteFinale() { return nombreManchesGagnantesPetiteFinale; }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder avec les valeurs par défaut du format standard poule de 3 / 2 qualifiés. */
    public static final class Builder {
        private String nom = "Format personnalisé";
        private String identifiant = "PERSO";
        private int taillePouleSouhaitee = 3;
        private int qualifiesParPoule = 2;
        private SortNonQualifies sortNonQualifies = SortNonQualifies.ELIMINE;
        private FormatPhaseFinale formatPhaseFinale = FormatPhaseFinale.ELIMINATION_DIRECTE;
        private int nombreManchesGagnantes = 3;
        private int nombreManchesGagnantesFinale = 3;
        private boolean avecPetiteFinale = false;
        private int nombreManchesGagnantesPetiteFinale = 3;

        public Builder nom(String v) { this.nom = Objects.requireNonNull(v); return this; }
        public Builder identifiant(String v) { this.identifiant = Objects.requireNonNull(v); return this; }

        public Builder taillePouleSouhaitee(int v) {
            if (v < 3) throw new IllegalArgumentException("Taille de poule >= 3.");
            this.taillePouleSouhaitee = v; return this;
        }
        public Builder qualifiesParPoule(int v) {
            if (v < 1) throw new IllegalArgumentException("Au moins 1 qualifié par poule.");
            this.qualifiesParPoule = v; return this;
        }
        public Builder sortNonQualifies(SortNonQualifies v) {
            this.sortNonQualifies = Objects.requireNonNull(v); return this;
        }
        public Builder formatPhaseFinale(FormatPhaseFinale v) {
            this.formatPhaseFinale = Objects.requireNonNull(v); return this;
        }
        public Builder nombreManchesGagnantes(int v) {
            this.nombreManchesGagnantes = exigerPositif(v); return this;
        }
        public Builder nombreManchesGagnantesFinale(int v) {
            this.nombreManchesGagnantesFinale = exigerPositif(v); return this;
        }
        public Builder avecPetiteFinale(boolean v) {
            this.avecPetiteFinale = v; return this;
        }
        public Builder nombreManchesGagnantesPetiteFinale(int v) {
            this.nombreManchesGagnantesPetiteFinale = exigerPositif(v); return this;
        }

        private int exigerPositif(int v) {
            if (v < 1) throw new IllegalArgumentException("Le best-of doit être d'au moins 1 manche gagnante.");
            return v;
        }

        public FormatConfigurable build() {
            return new FormatConfigurable(this);
        }
    }
}
