package com.pingmanager.model;

import java.util.Objects;

/**
 * Juge-arbitre affecté à un tournoi, en <b>saisie libre</b>.
 *
 * <p>Un JA est nécessairement licencié FFTT ; on conserve donc son
 * {@link #getNumeroLicence() numéro de licence}, mais sans exiger qu'il soit
 * déjà présent dans la base des {@link Joueur joueurs} : au moment du montage du
 * dossier, le JA n'est pas un participant. Ses coordonnées (nom, prénom, email,
 * téléphone) et son {@link GradeJugeArbitre grade} sont saisis directement,
 * comme dans le formulaire d'homologation SPID.</p>
 *
 * <p>Value-class immuable. L'identité repose sur le numéro de licence et le
 * grade.</p>
 */
public final class AffectationJugeArbitre {

    private final String nom;
    private final String prenom;
    private final String email;
    private final String numeroLicence;
    private final GradeJugeArbitre grade;
    private final String telephone; // facultatif
    private final RoleJugeArbitre role;

    /**
     * @param nom           nom du juge-arbitre (obligatoire)
     * @param prenom        prénom (obligatoire)
     * @param email         email (facultatif : la base des licences ne le fournit pas)
     * @param numeroLicence numéro de licence FFTT (obligatoire)
     * @param grade         grade (obligatoire)
     * @param telephone     téléphone (facultatif, peut être nul/vide)
     */
    public AffectationJugeArbitre(String nom, String prenom, String email,
                                  String numeroLicence, GradeJugeArbitre grade,
                                  String telephone) {
        this(nom, prenom, email, numeroLicence, grade, telephone, RoleJugeArbitre.PRINCIPAL);
    }

    /** Constructeur complet, avec le rôle (principal ou adjoint). */
    public AffectationJugeArbitre(String nom, String prenom, String email,
                                  String numeroLicence, GradeJugeArbitre grade,
                                  String telephone, RoleJugeArbitre role) {
        this.nom = exigerNonVide(nom, "nom");
        this.prenom = exigerNonVide(prenom, "prenom");
        this.email = normaliserOptionnel(email);
        this.numeroLicence = exigerNonVide(numeroLicence, "numeroLicence");
        this.grade = Objects.requireNonNull(grade, "grade");
        this.telephone = normaliserOptionnel(telephone);
        this.role = role == null ? RoleJugeArbitre.PRINCIPAL : role;
    }

    public RoleJugeArbitre getRole() {
        return role;
    }

    /** Constructeur sans téléphone (facultatif). */
    public AffectationJugeArbitre(String nom, String prenom, String email,
                                  String numeroLicence, GradeJugeArbitre grade) {
        this(nom, prenom, email, numeroLicence, grade, null);
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public String getNumeroLicence() {
        return numeroLicence;
    }

    public GradeJugeArbitre getGrade() {
        return grade;
    }

    public String getTelephone() {
        return telephone;
    }

    /**
     * @param gradeRequis grade minimal demandé
     * @return {@code true} si ce juge-arbitre a un grade suffisant
     */
    public boolean couvre(GradeJugeArbitre gradeRequis) {
        return grade.couvre(gradeRequis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AffectationJugeArbitre autre)) {
            return false;
        }
        return grade == autre.grade && numeroLicence.equals(autre.numeroLicence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroLicence, grade);
    }

    @Override
    public String toString() {
        return "JA " + prenom + " " + nom + " (" + grade + ", lic. " + numeroLicence + ")";
    }

    private static String exigerNonVide(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » du juge-arbitre est obligatoire.");
        }
        return valeur.trim();
    }

    private static String normaliserOptionnel(String valeur) {
        if (valeur == null) {
            return null;
        }
        String t = valeur.trim();
        return t.isEmpty() ? null : t;
    }
}
