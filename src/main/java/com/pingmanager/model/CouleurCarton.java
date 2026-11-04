package com.pingmanager.model;

/**
 * Couleur d'un carton, au sens du règlement (section 3.5.2).
 *
 * <p>Rappel du mécanisme :</p>
 * <ul>
 *   <li><b>Jaune</b> : avertissement à la première faute, sans point de pénalité.</li>
 *   <li><b>Jaune + rouge</b> : à chaque récidive après avertissement, l'arbitre
 *       accorde des points de pénalité à l'adversaire (1 à la première récidive,
 *       2 aux suivantes).</li>
 *   <li><b>Rouge</b> (du juge-arbitre) : disqualification pour la partie,
 *       l'épreuve ou toute la compétition selon la gravité.</li>
 * </ul>
 */
public enum CouleurCarton {
    /** Avertissement, aucun point de pénalité. */
    JAUNE,
    /** Récidive : points de pénalité accordés à l'adversaire (jaune + rouge). */
    JAUNE_ROUGE,
    /** Disqualification prononcée par le juge-arbitre. */
    ROUGE
}
