package com.pingmanager.model;

import java.util.Objects;

/**
 * Représente un joueur licencié FFTT.
 *
 * <p>Classe de données pure (aucune logique métier). Les champs reprennent
 * les informations FFTT nécessaires à une future remontée / export :
 * numéro de licence, classement officiel (points), club d'appartenance.</p>
 *
 * <p>L'identité d'un joueur repose sur son numéro de licence, qui est unique
 * au sein de la fédération.</p>
 */
public final class Joueur {

    private final String numeroLicence;
    private String nom;
    private String prenom;
    private Sexe sexe;
    private String club;

    /** Nombre de points officiels (classement mensuel FFTT). */
    private int points;

    /**
     * Rang au classement national français (numérotation) : 1 pour le meilleur
     * joueur français, 2 pour le suivant, etc. Sert aux tableaux qui excluent les
     * mieux numérotés (ex. « pas les 500 premiers Français »). Distinct des points.
     * Vaut {@link #RANG_NATIONAL_INCONNU} tant qu'il n'a pas été renseigné : ce rang
     * sera importé ou calculé lors d'une étape ultérieure.
     */
    private int rangNational = RANG_NATIONAL_INCONNU;

    /**
     * Catégorie d'âge (II.302 / II.303), tirée de la date de naissance de la
     * licence ; {@code null} si inconnue (aucun contrôle de catégorie possible).
     */
    private CategorieAge categorie;

    /** Valeur signifiant que le rang national du joueur n'est pas connu. */
    public static final int RANG_NATIONAL_INCONNU = 0;

    /** Licence conventionnelle du joueur sentinelle « à déterminer ». */
    public static final String LICENCE_A_DETERMINER = "__A_DETERMINER__";

    /**
     * Crée un joueur sentinelle représentant une place « à déterminer » dans un
     * tableau à élimination (emplacement du vainqueur d'un match encore non joué).
     * Utilisé en interne par la génération des tableaux ; à ne pas confondre avec
     * un vrai licencié.
     */
    public static Joueur aDeterminer() {
        return new Joueur(LICENCE_A_DETERMINER, "à déterminer", "", Sexe.HOMME, "", 0);
    }

    /** @return true si ce joueur est le sentinelle « à déterminer ». */
    public boolean estADeterminer() {
        return LICENCE_A_DETERMINER.equals(numeroLicence);
    }

    public Joueur(String numeroLicence, String nom, String prenom,
                  Sexe sexe, String club, int points) {
        this.numeroLicence = Objects.requireNonNull(numeroLicence, "numeroLicence");
        this.nom = Objects.requireNonNull(nom, "nom");
        this.prenom = Objects.requireNonNull(prenom, "prenom");
        this.sexe = Objects.requireNonNull(sexe, "sexe");
        this.club = club == null ? "" : club;
        if (points < 0) {
            throw new IllegalArgumentException("Les points ne peuvent pas être négatifs : " + points);
        }
        this.points = points;
    }

    public String getNumeroLicence() {
        return numeroLicence;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = Objects.requireNonNull(nom, "nom");
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = Objects.requireNonNull(prenom, "prenom");
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = Objects.requireNonNull(sexe, "sexe");
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club == null ? "" : club;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Les points ne peuvent pas être négatifs : " + points);
        }
        this.points = points;
    }

    /**
     * @return le rang national (numérotation française), ou
     *         {@link #RANG_NATIONAL_INCONNU} si non renseigné.
     */
    public int getRangNational() {
        return rangNational;
    }

    /**
     * Définit le rang national du joueur.
     *
     * @param rangNational rang &gt;= 1, ou {@link #RANG_NATIONAL_INCONNU} pour
     *                     « non renseigné »
     */
    public void setRangNational(int rangNational) {
        if (rangNational < 0) {
            throw new IllegalArgumentException("Le rang national ne peut pas être négatif : " + rangNational);
        }
        this.rangNational = rangNational;
    }

    /** @return true si le rang national du joueur est connu. */
    public boolean aRangNationalConnu() {
        return rangNational != RANG_NATIONAL_INCONNU;
    }

    /** Nom complet formaté "PRENOM NOM" pour affichage. */
    /** @return la catégorie d'âge, ou {@code null} si elle n'est pas connue. */
    public CategorieAge getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieAge categorie) {
        this.categorie = categorie;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Joueur autre)) return false;
        return numeroLicence.equals(autre.numeroLicence);
    }

    @Override
    public int hashCode() {
        return numeroLicence.hashCode();
    }

    @Override
    public String toString() {
        return getNomComplet() + " (" + points + " pts, " + club + ")";
    }
}
