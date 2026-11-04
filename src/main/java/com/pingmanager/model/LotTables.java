package com.pingmanager.model;

/**
 * Un lot de tables identiques : nombre, marque, couleur. Un tournoi peut en
 * compter plusieurs (marques ou couleurs différentes). Règles du jeu 2.1.4 :
 * surface de jeu de couleur uniformément foncée et mate ; 3.2.1.1 : seules les
 * marques agréées ITTF/FFTT sont admises.
 */
public final class LotTables {

    private final int nombre;
    private final String marque;
    private final String couleur;

    public LotTables(int nombre, String marque, String couleur) {
        if (nombre <= 0) {
            throw new IllegalArgumentException("Le nombre de tables d'un lot doit être positif.");
        }
        this.nombre = nombre;
        this.marque = marque == null || marque.isBlank() ? null : marque.trim();
        this.couleur = couleur == null || couleur.isBlank() ? null : couleur.trim();
    }

    public int getNombre() {
        return nombre;
    }

    public String getMarque() {
        return marque;
    }

    public String getCouleur() {
        return couleur;
    }
}
