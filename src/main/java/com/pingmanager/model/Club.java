package com.pingmanager.model;

import java.util.Objects;

/**
 * Représente un club de tennis de table affilié à la FFTT.
 *
 * <p>Classe de données pure (aucune logique métier, aucun effet de bord).
 * Elle sert de référentiel : les comptes organisateurs et les tournois s'y
 * rattachent par le <b>numéro de club</b>, qui est l'identité stable et unique
 * d'un club au sein de la fédération.</p>
 *
 * <p>Le champ {@link #getEmailOfficiel()} désigne l'adresse de contact
 * officielle du club (généralement celle du responsable). C'est vers cette
 * adresse que sera envoyé, lors d'une étape ultérieure, le code de vérification
 * permettant de confirmer qu'une personne est bien autorisée à administrer ce
 * club. Le modèle ne fait qu'en porter la valeur : ni génération de code, ni
 * envoi d'email ne relèvent de cette classe.</p>
 *
 * <p>La provenance de ce référentiel (import depuis une base FFTT) sera traitée
 * plus tard ; pour l'instant les clubs sont alimentés à la main.</p>
 */
public final class Club {

    private final String numero;
    private String nom;
    private String departement;
    private String ville;

    /** Adresse postale, lignes optionnelles. */
    private String adresse1;
    private String adresse2;
    /** Code postal de l'adresse du club (salle). Facultatif. */
    private String codePostal;

    /** Adresse email officielle du club (contact / responsable). */
    private String emailOfficiel;

    /** Identité du responsable / contact principal (optionnelle). */
    private String responsableNom;
    private String responsablePrenom;

    /**
     * @param numero        numéro officiel FFTT du club (identité, non vide) ;
     *                      ex. « 08911132 »
     * @param nom           dénomination officielle du club (non vide)
     * @param departement   code du département FFTT (non vide) ; ex. « 91 »
     * @param ville         ville du club (non vide)
     * @param emailOfficiel adresse email officielle du club (non vide)
     */
    public Club(String numero, String nom, String departement, String ville,
                String emailOfficiel) {
        this.numero = exigerNonVide(numero, "numero");
        this.nom = exigerNonVide(nom, "nom");
        this.departement = exigerNonVide(departement, "departement");
        this.ville = exigerNonVide(ville, "ville");
        this.emailOfficiel = exigerNonVide(emailOfficiel, "emailOfficiel");
    }

    public String getNumero() {
        return numero;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = exigerNonVide(nom, "nom");
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = exigerNonVide(departement, "departement");
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = exigerNonVide(ville, "ville");
    }

    public String getAdresse1() {
        return adresse1;
    }

    public void setAdresse1(String adresse1) {
        this.adresse1 = normaliserOptionnel(adresse1);
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = normaliserOptionnel(codePostal);
    }

    public String getAdresse2() {
        return adresse2;
    }

    public void setAdresse2(String adresse2) {
        this.adresse2 = normaliserOptionnel(adresse2);
    }

    public String getEmailOfficiel() {
        return emailOfficiel;
    }

    public void setEmailOfficiel(String emailOfficiel) {
        this.emailOfficiel = exigerNonVide(emailOfficiel, "emailOfficiel");
    }

    public String getResponsableNom() {
        return responsableNom;
    }

    public void setResponsableNom(String responsableNom) {
        this.responsableNom = normaliserOptionnel(responsableNom);
    }

    public String getResponsablePrenom() {
        return responsablePrenom;
    }

    public void setResponsablePrenom(String responsablePrenom) {
        this.responsablePrenom = normaliserOptionnel(responsablePrenom);
    }

    // -------------------------------------------------------------------------
    // Identité : deux clubs sont égaux s'ils ont le même numéro FFTT.
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Club autre)) {
            return false;
        }
        return numero.equals(autre.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return nom + " (" + numero + ", " + ville + ")";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String exigerNonVide(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » est obligatoire.");
        }
        return valeur.trim();
    }

    private static String normaliserOptionnel(String valeur) {
        if (valeur == null) {
            return null;
        }
        String nettoye = valeur.trim();
        return nettoye.isEmpty() ? null : nettoye;
    }
}
