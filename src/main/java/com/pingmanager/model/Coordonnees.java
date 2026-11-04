package com.pingmanager.model;

/**
 * Position géographique (WGS84, degrés décimaux) : sert à placer un tournoi sur la
 * carte et à calculer une distance. Value-class immuable.
 *
 * @param latitude  entre -90 et 90
 * @param longitude entre -180 et 180
 */
public record Coordonnees(double latitude, double longitude) {

    private static final double RAYON_TERRE_KM = 6371.0;

    public Coordonnees {
        if (Double.isNaN(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalide : " + latitude);
        }
        if (Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalide : " + longitude);
        }
    }

    /** @return la distance à vol d'oiseau, en kilomètres (formule de haversine). */
    public double distanceKm(Coordonnees autre) {
        double p1 = Math.toRadians(latitude);
        double p2 = Math.toRadians(autre.latitude);
        double dp = p2 - p1;
        double dl = Math.toRadians(autre.longitude - longitude);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
