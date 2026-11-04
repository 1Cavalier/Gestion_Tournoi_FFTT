package com.pingmanager.model;

import java.util.Objects;

/**
 * Ligne de classement d'un joueur au sein d'une poule.
 *
 * <p>Agrège les statistiques nécessaires au classement FFTT : points de
 * classement (2 par victoire, 1 par défaite), bilan de matchs, bilan de manches
 * et bilan de points. Le rang final (1er, 2e…) est renseigné par le service de
 * classement une fois tous les départages appliqués.</p>
 *
 * <p>Objet de données mutable, rempli par {@code ClassementPouleService}.</p>
 */
public final class LigneClassement {

    private final Joueur joueur;

    private int matchsJoues;
    private int matchsGagnes;
    private int matchsPerdus;

    /**
     * Défaites à 0 point (absence, abandon, refus, disqualification — I.303).
     * Comptées à part des défaites normales car elles ne rapportent aucun point
     * de classement, contrairement à une défaite jouée (1 point).
     */
    private int matchsPerdusZeroPoint;

    private int manchesGagnees;
    private int manchesPerdues;

    private int pointsGagnes;
    private int pointsPerdus;

    /** Rang final dans la poule (1 = premier). 0 tant que non déterminé. */
    private int rang;

    public LigneClassement(Joueur joueur) {
        this.joueur = Objects.requireNonNull(joueur, "joueur");
    }

    public Joueur getJoueur() {
        return joueur;
    }

    /**
     * Points de classement FFTT (I.303) : 2 points par victoire, 1 point par
     * défaite <b>jouée</b>, et <b>0 point</b> pour une défaite par absence,
     * abandon, refus de jouer ou disqualification.
     */
    public int getPointsClassement() {
        return matchsGagnes * 2 + matchsPerdus;
    }

    public int getMatchsJoues() {
        return matchsJoues;
    }

    public int getMatchsGagnes() {
        return matchsGagnes;
    }

    public int getMatchsPerdus() {
        return matchsPerdus;
    }

    public int getManchesGagnees() {
        return manchesGagnees;
    }

    public int getManchesPerdues() {
        return manchesPerdues;
    }

    public int getPointsGagnes() {
        return pointsGagnes;
    }

    public int getPointsPerdus() {
        return pointsPerdus;
    }

    public int getRang() {
        return rang;
    }

    public void setRang(int rang) {
        this.rang = rang;
    }

    /** Différence de manches (gagnées - perdues). */
    public int getDifferenceManches() {
        return manchesGagnees - manchesPerdues;
    }

    /** Différence de points (gagnés - perdus). */
    public int getDifferencePoints() {
        return pointsGagnes - pointsPerdus;
    }

    /**
     * Quotient de manches (gagnées / perdues). Sert de critère de départage.
     * Retourne {@link Double#POSITIVE_INFINITY} si aucune manche perdue.
     */
    public double getQuotientManches() {
        return manchesPerdues == 0
                ? Double.POSITIVE_INFINITY
                : (double) manchesGagnees / manchesPerdues;
    }

    /**
     * Quotient de points (gagnés / perdus). Départage plus fin.
     * Retourne {@link Double#POSITIVE_INFINITY} si aucun point perdu.
     */
    public double getQuotientPoints() {
        return pointsPerdus == 0
                ? Double.POSITIVE_INFINITY
                : (double) pointsGagnes / pointsPerdus;
    }

    // --- Méthodes d'accumulation, appelées par le service de classement ---

    public void enregistrerVictoire() {
        matchsJoues++;
        matchsGagnes++;
    }

    public void enregistrerDefaite() {
        matchsJoues++;
        matchsPerdus++;
    }

    /**
     * Enregistre une défaite à 0 point (absence, abandon, refus, disqualification).
     * Le match compte comme joué mais ne rapporte aucun point de classement.
     */
    public void enregistrerDefaiteZeroPoint() {
        matchsJoues++;
        matchsPerdusZeroPoint++;
    }

    public int getMatchsPerdusZeroPoint() {
        return matchsPerdusZeroPoint;
    }

    public void ajouterManches(int gagnees, int perdues) {
        this.manchesGagnees += gagnees;
        this.manchesPerdues += perdues;
    }

    public void ajouterPoints(int gagnes, int perdus) {
        this.pointsGagnes += gagnes;
        this.pointsPerdus += perdus;
    }

    @Override
    public String toString() {
        return String.format("%d. %s [%d pts, %dV-%dD, manches %+d]",
                rang, joueur.getNomComplet(), getPointsClassement(),
                matchsGagnes, matchsPerdus, getDifferenceManches());
    }
}
