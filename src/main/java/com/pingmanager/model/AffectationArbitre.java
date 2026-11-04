package com.pingmanager.model;

import java.util.Objects;

/**
 * Arbitre officiel affecté au tournoi, repris de sa fiche de licence. Le grade
 * est conservé tel que la fiche l'indique (« AR », « AN »…).
 */
public final class AffectationArbitre {

    private final String numeroLicence;
    private final String nom;
    private final String prenom;
    private final String grade;

    public AffectationArbitre(String numeroLicence, String nom, String prenom, String grade) {
        this.numeroLicence = exiger(numeroLicence, "numeroLicence");
        this.nom = exiger(nom, "nom");
        this.prenom = exiger(prenom, "prenom");
        this.grade = exiger(grade, "grade");
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

    public String getGrade() {
        return grade;
    }

    /** @return un libellé lisible du grade d'arbitre (code conservé si inconnu). */
    public static String libelleGrade(String code) {
        if (code == null) {
            return "";
        }
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (c.equals("AR")) {
            return "Arbitre régional (AR)";
        }
        if (c.equals("AN")) {
            return "Arbitre national (AN)";
        }
        if (c.equals("AI")) {
            return "Arbitre international (AI)";
        }
        return code.trim();
    }

    private static String exiger(String v, String nom) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(nom + " est obligatoire.");
        }
        return v.trim();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AffectationArbitre a && a.numeroLicence.equals(numeroLicence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroLicence);
    }
}
