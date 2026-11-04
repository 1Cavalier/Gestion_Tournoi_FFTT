package com.pingmanager.model;

/**
 * Caractéristiques de l'aire de jeu d'un tournoi : dimensions d'un court
 * individuel (en mètres) et niveau d'éclairage (en lux).
 *
 * <p>Value-class immuable. Toutes les valeurs sont optionnelles au sens métier
 * (une valeur nulle signifie « non renseigné ») ; l'objet vérifie seulement, si
 * une valeur est fournie, qu'elle est strictement positive.</p>
 */
public final class AireDeJeu {

    private final Double longueurCourtM;
    private final Double largeurCourtM;
    private final Integer eclairageLux;

    /**
     * @param longueurCourtM longueur d'un court en mètres (null = non renseigné)
     * @param largeurCourtM  largeur d'un court en mètres (null = non renseigné)
     * @param eclairageLux   éclairage en lux (null = non renseigné)
     */
    public AireDeJeu(Double longueurCourtM, Double largeurCourtM, Integer eclairageLux) {
        exigerPositifSiPresent(longueurCourtM, "longueurCourtM");
        exigerPositifSiPresent(largeurCourtM, "largeurCourtM");
        if (eclairageLux != null && eclairageLux <= 0) {
            throw new IllegalArgumentException("L'éclairage (lux) doit être strictement positif.");
        }
        this.longueurCourtM = longueurCourtM;
        this.largeurCourtM = largeurCourtM;
        this.eclairageLux = eclairageLux;
    }

    public Double getLongueurCourtM() {
        return longueurCourtM;
    }

    public Double getLargeurCourtM() {
        return largeurCourtM;
    }

    public Integer getEclairageLux() {
        return eclairageLux;
    }

    @Override
    public String toString() {
        return "AireDeJeu[" + longueurCourtM + "m x " + largeurCourtM + "m, "
                + eclairageLux + " lux]";
    }

    private static void exigerPositifSiPresent(Double valeur, String champ) {
        if (valeur != null && valeur <= 0) {
            throw new IllegalArgumentException("La valeur « " + champ + " » doit être strictement positive.");
        }
    }
}
