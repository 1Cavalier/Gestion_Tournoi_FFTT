package com.pingmanager.core;

import com.pingmanager.model.Carton;
import com.pingmanager.model.CouleurCarton;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Applique la progression officielle des sanctions <b>au cours d'un match</b>
 * (règles 3.5.2.2 et 3.5.2.3) et alimente le {@link RegistreDisciplinaire}.
 *
 * <p>Mécanique :</p>
 * <ol>
 *   <li>1re faute d'un joueur dans la partie → <b>carton jaune</b>
 *       (avertissement), aucun point accordé.</li>
 *   <li>1re récidive → <b>1 point</b> de pénalité à l'adversaire (jaune+rouge).</li>
 *   <li>récidives suivantes → <b>2 points</b> de pénalité à l'adversaire à
 *       chaque fois (jaune+rouge).</li>
 * </ol>
 *
 * <p>Le service mémorise, pour chaque match et chaque joueur, le nombre de
 * sanctions déjà appliquées afin de calculer la sanction suivante. Il crée le
 * {@link Carton} correspondant, l'enregistre dans le registre disciplinaire, et
 * ajoute le cas échéant les points de pénalité au {@link Match}.</p>
 */
public final class GestionCartonsMatch {

    private final RegistreDisciplinaire registre;

    /** Compteur de sanctions par (match, joueur) au sein de la partie courante. */
    private final Map<String, Integer> sanctionsParJoueurMatch = new HashMap<>();

    public GestionCartonsMatch(RegistreDisciplinaire registre) {
        this.registre = Objects.requireNonNull(registre, "registre");
    }

    /**
     * Applique une faute de comportement d'un joueur au cours d'un match et
     * produit la sanction adéquate selon la progression officielle.
     *
     * @param match  le match en cours
     * @param fautif le joueur qui commet la faute
     * @param motif  description de la faute
     * @return le carton produit (jaune, ou jaune+rouge avec points de pénalité)
     */
    public Carton appliquerFauteEnMatch(Match match, Joueur fautif, String motif) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(fautif, "fautif");

        String cle = cle(match, fautif);
        int sanctionsPrecedentes = sanctionsParJoueurMatch.getOrDefault(cle, 0);

        CouleurCarton couleur;
        int pointsPenalite;
        if (sanctionsPrecedentes == 0) {
            // Première faute : avertissement (jaune), pas de point.
            couleur = CouleurCarton.JAUNE;
            pointsPenalite = 0;
        } else if (sanctionsPrecedentes == 1) {
            // Première récidive : 1 point à l'adversaire.
            couleur = CouleurCarton.JAUNE_ROUGE;
            pointsPenalite = 1;
        } else {
            // Récidives suivantes : 2 points à l'adversaire.
            couleur = CouleurCarton.JAUNE_ROUGE;
            pointsPenalite = 2;
        }

        Carton carton = new Carton(fautif, couleur, pointsPenalite, false, motif, LocalDateTime.now());
        registre.enregistrer(carton);
        if (pointsPenalite > 0) {
            match.ajouterPointsPenalite(fautif, pointsPenalite);
        }
        sanctionsParJoueurMatch.put(cle, sanctionsPrecedentes + 1);
        return carton;
    }

    /**
     * Enregistre un carton reçu <b>hors match</b> (comportement dans la salle
     * entre deux parties, par exemple). Un rouge hors match est une
     * disqualification qui compte au registre (règle des 2 disqualifications).
     *
     * @param joueur  le joueur sanctionné
     * @param couleur la couleur du carton
     * @param motif   description
     * @return le carton créé
     */
    public Carton appliquerCartonHorsMatch(Joueur joueur, CouleurCarton couleur, String motif) {
        Objects.requireNonNull(joueur, "joueur");
        Objects.requireNonNull(couleur, "couleur");
        Carton carton = new Carton(joueur, couleur, 0, true, motif, LocalDateTime.now());
        registre.enregistrer(carton);
        return carton;
    }

    private String cle(Match match, Joueur joueur) {
        return System.identityHashCode(match) + ":" + joueur.getNumeroLicence();
    }
}
