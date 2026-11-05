package com.pingmanager.core;

import com.pingmanager.model.Manche;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lecture et écriture du score d'une manche tel qu'on le tape en salle.
 *
 * <p>Deux notations sont acceptées :</p>
 * <ul>
 *   <li><b>complète</b> : « 11-9 », « 11/9 », « 11:9 » ou « 11 9 » (points du
 *       joueur A puis du joueur B) ;</li>
 *   <li><b>courte</b>, celle des feuilles de partie : les seuls points du perdant,
 *       précédés d'un signe moins si c'est le joueur B qui gagne la manche.
 *       « 9 » = 11-9 pour A, « -9 » = 9-11 pour B, « 12 » = 14-12 pour A,
 *       « -0 » = 0-11 pour B. Le score du vainqueur se déduit de l'article
 *       2.11.1 (11 points, ou 2 points d'écart après 10-10).</li>
 * </ul>
 *
 * <p>La lecture ne vérifie que la forme ; la validité d'une manche (2.11.1) est
 * contrôlée par {@link SaisieResultatMatch#mancheValide(Manche)}.</p>
 */
public final class NotationManche {

    private static final Pattern COMPLETE = Pattern.compile("^\\s*(\\d{1,2})\\s*[-/: ]\\s*(\\d{1,2})\\s*$");
    private static final Pattern COURTE = Pattern.compile("^\\s*([+-]?)\\s*(\\d{1,2})\\s*$");

    private NotationManche() {
    }

    /**
     * @param saisie le texte tapé (notation complète ou courte)
     * @return la manche lue, ou vide si le texte est vide ou illisible
     */
    public static Optional<Manche> lire(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return Optional.empty();
        }
        Matcher c = COMPLETE.matcher(saisie);
        if (c.matches()) {
            return Optional.of(new Manche(Integer.parseInt(c.group(1)), Integer.parseInt(c.group(2))));
        }
        Matcher m = COURTE.matcher(saisie);
        if (m.matches()) {
            int perdant = Integer.parseInt(m.group(2));
            // 2.11.1 : 11 points, ou 2 points d'écart dès que les deux joueurs ont 10.
            int vainqueur = perdant <= 9 ? 11 : perdant + 2;
            boolean gagneParB = "-".equals(m.group(1));
            return Optional.of(gagneParB ? new Manche(perdant, vainqueur) : new Manche(vainqueur, perdant));
        }
        return Optional.empty();
    }

    /** @return la notation complète « 11-9 » (points de A puis de B). */
    public static String ecrire(Manche manche) {
        return manche.pointsJoueurA() + "-" + manche.pointsJoueurB();
    }

    /**
     * @return la notation courte (points du perdant, « - » si B gagne), ou la
     *         notation complète si la manche n'a pas de vainqueur
     */
    public static String ecrireCourt(Manche manche) {
        if (manche.gagneeParA()) {
            return String.valueOf(manche.pointsJoueurB());
        }
        if (manche.gagneeParB()) {
            return "-" + manche.pointsJoueurA();
        }
        return ecrire(manche);
    }
}
