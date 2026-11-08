package com.pingmanager.core.feuilles;

import com.pingmanager.model.Joueur;

import java.util.Locale;

/**
 * Un joueur tel qu'il est imprimé sur une feuille : nom, prénom, club et
 * points (format provisoire de Quentin, cahier des charges v0, section 9), plus
 * la licence quand elle est connue. Une place encore inconnue du tableau final
 * est imprimée « À déterminer » avec des champs vides.
 *
 * @param nom     nom de famille, en majuscules (vide si à déterminer)
 * @param prenom  prénom (vide si à déterminer)
 * @param club    club (vide si inconnu)
 * @param points  points (texte « 1520 pts », vide si à déterminer)
 * @param licence numéro de licence, vide si inconnu
 */
public record JoueurFeuille(String nom, String prenom, String club, String points, String licence) {

    /** Libellé imprimé à la place d'un joueur pas encore connu. */
    public static final String A_DETERMINER = "À déterminer";

    public JoueurFeuille {
        nom = nom == null ? "" : nom;
        prenom = prenom == null ? "" : prenom;
        club = club == null ? "" : club;
        points = points == null ? "" : points;
        licence = licence == null ? "" : licence;
    }

    /** Joueur d'une feuille, depuis le modèle ({@code null} ou « à déterminer » → place vide). */
    public static JoueurFeuille de(Joueur j) {
        if (j == null || j.estADeterminer()) {
            return new JoueurFeuille("", "", "", "", "");
        }
        String licence = j.getNumeroLicence() == null || j.getNumeroLicence().startsWith("__")
                ? "" : j.getNumeroLicence();
        return new JoueurFeuille(j.getNom() == null ? "" : j.getNom().trim().toUpperCase(Locale.FRENCH),
                j.getPrenom() == null ? "" : j.getPrenom().trim(),
                j.getClub() == null ? "" : j.getClub().trim(),
                j.getPoints() + " pts", licence);
    }

    /** La place est-elle encore inconnue ? */
    public boolean estADeterminer() {
        return nom.isEmpty() && prenom.isEmpty();
    }

    /** « NOM Prénom », ou « À déterminer ». */
    public String nomComplet() {
        return estADeterminer() ? A_DETERMINER : (nom + " " + prenom).trim();
    }

    /** « Club · 1520 pts » (sans les parties vides). */
    public String detail() {
        if (club.isEmpty()) {
            return points;
        }
        return points.isEmpty() ? club : club + " · " + points;
    }
}
