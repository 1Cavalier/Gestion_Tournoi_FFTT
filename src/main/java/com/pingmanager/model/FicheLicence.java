package com.pingmanager.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Fiche renvoyée par le référentiel FFTT lorsqu'on recherche une licence par son
 * numéro. C'est la réponse « en lecture seule » de la base fédérale : l'appli ne
 * la gère pas, elle l'interroge.
 *
 * <p>Seule l'identité (numéro, nom, prénom) est obligatoire ; tout le reste est
 * optionnel. Vu le nombre de champs, on construit la fiche avec un {@link Builder}
 * plutôt qu'un constructeur à rallonge.</p>
 *
 * <p>Deux niveaux de points, conformément au règlement FFTT (IV.201.3) : les
 * <b>points de début de phase</b> ({@link #getPoints()}), classement officiel de
 * référence pour toute la phase — c'est lui qui sert aux têtes de série — et les
 * <b>points mensuels</b> ({@link #getPointsMensuels()}), situation courante qui
 * évolue au fil des résultats. Classe de données immuable.</p>
 */
public final class FicheLicence {

    private final String numeroLicence;
    private final String nom;
    private final String prenom;
    private final Sexe sexe;
    private final String numeroClub;
    private final String nomClub;

    /** Points officiels de début de phase (référence). */
    private final int points;
    /** Points de la situation mensuelle courante (0 si inconnu). */
    private final int pointsMensuels;
    /** Classement par tranche (ex. « 9 »), déduit des points officiels. */
    private final String classement;

    private final String categorie;
    private final LocalDate dateNaissance;
    private final String nationalite;
    private final String typeLicence;
    /** Saison sportive de la licence (ex. « 2026-2027 »). */
    private final String saison;
    /** Statut de la licence (ex. « Active »). */
    private final String statut;

    private final String arbitre;
    private final String jugeArbitre;
    private final String technique;

    /** Constructeur simple (identité + club + points de référence). */
    public FicheLicence(String numeroLicence, String nom, String prenom, Sexe sexe,
                        String numeroClub, String nomClub, int points) {
        this(new Builder(numeroLicence, nom, prenom)
                .sexe(sexe).club(numeroClub, nomClub).points(points));
    }

    private FicheLicence(Builder b) {
        this.numeroLicence = Textes.obligatoire(b.numeroLicence, "numeroLicence");
        this.nom = Textes.obligatoire(b.nom, "nom");
        this.prenom = Textes.obligatoire(b.prenom, "prenom");
        this.sexe = b.sexe;
        this.numeroClub = Textes.optionnel(b.numeroClub);
        this.nomClub = Textes.optionnel(b.nomClub);
        this.points = b.points;
        this.pointsMensuels = b.pointsMensuels;
        this.classement = Textes.optionnel(b.classement);
        this.categorie = Textes.optionnel(b.categorie);
        this.dateNaissance = b.dateNaissance;
        this.nationalite = Textes.optionnel(b.nationalite);
        this.typeLicence = Textes.optionnel(b.typeLicence);
        this.saison = Textes.optionnel(b.saison);
        this.statut = Textes.optionnel(b.statut);
        this.arbitre = Textes.optionnel(b.arbitre);
        this.jugeArbitre = Textes.optionnel(b.jugeArbitre);
        this.technique = Textes.optionnel(b.technique);
    }

    public String getNumeroLicence() {
        return numeroLicence;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public String getNumeroClub() {
        return numeroClub;
    }

    public String getNomClub() {
        return nomClub;
    }

    /** @return les points de début de phase (classement officiel de référence). */
    public int getPoints() {
        return points;
    }

    /** @return les points de la situation mensuelle courante (0 si inconnu). */
    public int getPointsMensuels() {
        return pointsMensuels;
    }

    public String getClassement() {
        return classement;
    }

    public String getCategorie() {
        return categorie;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public String getNationalite() {
        return nationalite;
    }

    public String getTypeLicence() {
        return typeLicence;
    }

    public String getSaison() {
        return saison;
    }

    public String getStatut() {
        return statut;
    }

    public String getArbitre() {
        return arbitre;
    }

    public String getJugeArbitre() {
        return jugeArbitre;
    }

    public String getTechnique() {
        return technique;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FicheLicence autre)) {
            return false;
        }
        return numeroLicence.equals(autre.numeroLicence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroLicence);
    }

    @Override
    public String toString() {
        return prenom + " " + nom + " (" + numeroLicence + ", " + points + " pts)";
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    /** Construction fluide d'une {@link FicheLicence} (seule l'identité est requise). */
    public static final class Builder {
        private final String numeroLicence;
        private final String nom;
        private final String prenom;
        private Sexe sexe;
        private String numeroClub;
        private String nomClub;
        private int points;
        private int pointsMensuels;
        private String classement;
        private String categorie;
        private LocalDate dateNaissance;
        private String nationalite;
        private String typeLicence;
        private String saison;
        private String statut;
        private String arbitre;
        private String jugeArbitre;
        private String technique;

        public Builder(String numeroLicence, String nom, String prenom) {
            this.numeroLicence = numeroLicence;
            this.nom = nom;
            this.prenom = prenom;
        }

        public Builder sexe(Sexe v) {
            this.sexe = v;
            return this;
        }

        public Builder club(String numero, String nom) {
            this.numeroClub = numero;
            this.nomClub = nom;
            return this;
        }

        public Builder points(int v) {
            this.points = v;
            return this;
        }

        public Builder pointsMensuels(int v) {
            this.pointsMensuels = v;
            return this;
        }

        public Builder classement(String v) {
            this.classement = v;
            return this;
        }

        public Builder categorie(String v) {
            this.categorie = v;
            return this;
        }

        public Builder dateNaissance(LocalDate v) {
            this.dateNaissance = v;
            return this;
        }

        public Builder nationalite(String v) {
            this.nationalite = v;
            return this;
        }

        public Builder typeLicence(String v) {
            this.typeLicence = v;
            return this;
        }

        public Builder saison(String v) {
            this.saison = v;
            return this;
        }

        public Builder statut(String v) {
            this.statut = v;
            return this;
        }

        public Builder arbitre(String v) {
            this.arbitre = v;
            return this;
        }

        public Builder jugeArbitre(String v) {
            this.jugeArbitre = v;
            return this;
        }

        public Builder technique(String v) {
            this.technique = v;
            return this;
        }

        public FicheLicence build() {
            return new FicheLicence(this);
        }
    }
}
