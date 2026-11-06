package com.pingmanager.model;

import java.util.Objects;

/**
 * Adresse structurée du lieu d'un tournoi, calquée sur le formulaire
 * d'homologation SPID.
 *
 * <p>Champs obligatoires pour l'homologation : {@link #getVoie() n° et libellé
 * de voie}, {@link #getCodePostal() code postal}, {@link #getVille() ville}.
 * Champs facultatifs : {@link #getResidenceBatiment() résidence / bâtiment} et
 * {@link #getLieuDit() lieu-dit}.</p>
 *
 * <p>Value-class immuable. Le modèle n'exige la présence des champs obligatoires
 * qu'à la construction d'une adresse « complète » ; il reste possible de
 * représenter une adresse partielle en cours de saisie via {@link #partielle()}
 * — la vérification de complétude pour l'homologation est faite ailleurs.</p>
 */
public final class AdresseTournoi {

    private final String voie;             // n° et libellé de voie (obligatoire)
    private final String residenceBatiment; // facultatif
    private final String lieuDit;          // facultatif
    private final String codePostal;       // obligatoire
    private final String ville;            // obligatoire
    /** Position du lieu (géocodage), facultative : sert à la carte des tournois. */
    private final Coordonnees coordonnees;

    private AdresseTournoi(String voie, String residenceBatiment, String lieuDit,
                           String codePostal, String ville, boolean exigerObligatoires) {
        this(voie, residenceBatiment, lieuDit, codePostal, ville, exigerObligatoires, null);
    }

    private AdresseTournoi(String voie, String residenceBatiment, String lieuDit,
                           String codePostal, String ville, boolean exigerObligatoires,
                           Coordonnees coordonnees) {
        this.coordonnees = coordonnees;
        if (exigerObligatoires) {
            this.voie = exigerNonVide(voie, "voie");
            this.codePostal = exigerNonVide(codePostal, "codePostal");
            this.ville = exigerNonVide(ville, "ville");
        } else {
            this.voie = Textes.optionnel(voie);
            this.codePostal = Textes.optionnel(codePostal);
            this.ville = Textes.optionnel(ville);
        }
        this.residenceBatiment = Textes.optionnel(residenceBatiment);
        this.lieuDit = Textes.optionnel(lieuDit);
    }

    /**
     * Crée une adresse complète : voie, code postal et ville sont exigés.
     */
    public static AdresseTournoi de(String voie, String codePostal, String ville) {
        return new AdresseTournoi(voie, null, null, codePostal, ville, true);
    }

    /**
     * Crée une adresse complète avec les champs facultatifs.
     */
    public static AdresseTournoi complete(String voie, String residenceBatiment, String lieuDit,
                                          String codePostal, String ville) {
        return new AdresseTournoi(voie, residenceBatiment, lieuDit, codePostal, ville, true);
    }

    /**
     * Crée une adresse partielle (aucun champ exigé), pour une saisie en cours.
     */
    public static AdresseTournoi partielle(String voie, String residenceBatiment, String lieuDit,
                                           String codePostal, String ville) {
        return new AdresseTournoi(voie, residenceBatiment, lieuDit, codePostal, ville, false);
    }

    public String getVoie() {
        return voie;
    }

    public String getResidenceBatiment() {
        return residenceBatiment;
    }

    public String getLieuDit() {
        return lieuDit;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public String getVille() {
        return ville;
    }

    /** @return la position du lieu, ou {@code null} si l'adresse n'a pas été géocodée. */
    public Coordonnees getCoordonnees() {
        return coordonnees;
    }

    /** @return une copie de l'adresse portant ces coordonnées ({@code null} pour les effacer). */
    public AdresseTournoi avecCoordonnees(Coordonnees c) {
        return new AdresseTournoi(voie, residenceBatiment, lieuDit, codePostal, ville, false, c);
    }

    /** @return vrai si les champs postaux (hors coordonnées) sont identiques. */
    public boolean memeLieu(AdresseTournoi autre) {
        return autre != null && Objects.equals(voie, autre.voie)
                && Objects.equals(codePostal, autre.codePostal)
                && Objects.equals(ville, autre.ville);
    }

    /** @return true si les champs obligatoires (voie, CP, ville) sont présents. */
    public boolean estComplete() {
        return !estVide(voie) && !estVide(codePostal) && !estVide(ville);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdresseTournoi a)) {
            return false;
        }
        return Objects.equals(voie, a.voie)
                && Objects.equals(residenceBatiment, a.residenceBatiment)
                && Objects.equals(lieuDit, a.lieuDit)
                && Objects.equals(codePostal, a.codePostal)
                && Objects.equals(ville, a.ville)
                && Objects.equals(coordonnees, a.coordonnees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voie, residenceBatiment, lieuDit, codePostal, ville);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (voie != null) sb.append(voie);
        if (codePostal != null || ville != null) {
            if (sb.length() > 0) sb.append(", ");
            if (codePostal != null) sb.append(codePostal).append(' ');
            if (ville != null) sb.append(ville);
        }
        return sb.toString();
    }

    private static String exigerNonVide(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ d'adresse « " + champ + " » est obligatoire.");
        }
        return valeur.trim();
    }

    private static boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}
