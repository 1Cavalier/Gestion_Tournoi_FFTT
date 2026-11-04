package com.pingmanager.ui.ecran;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Valeurs proposées par défaut lors de la saisie d'un tournoi (toujours
 * modifiables). Logique pure, sans JavaFX, pour pouvoir la tester.
 */
final class PropositionsTournoi {

    /** Mots de tête qui appellent « du » : « Tournoi du TT Paris 15 ». */
    private static final Set<String> MASCULINS = Set.of(
            "club", "cercle", "stade", "racing", "sporting", "foyer", "ping", "comite", "patronage");

    /** Mots de tête qui appellent « de l' » : « Tournoi de l'Entente… ». */
    private static final Set<String> ELIDES_ARTICLE = Set.of(
            "entente", "union", "association", "amicale", "avenir", "elan", "etoile",
            "olympique", "esperance", "alliance", "energie");

    private PropositionsTournoi() {
    }

    /**
     * Nom proposé : « Tournoi de Brunoy », « Tournoi d'Évry », « Tournoi du TT
     * Paris 15 », « Tournoi de l'AS Fontenay »…
     *
     * <ul>
     *   <li>nom commençant par un sigle (tout en majuscules) : « du » s'il
     *       commence par une consonne (TT, CTT, CS), « de l' » sinon (AS, US) ;</li>
     *   <li>nom commençant par un mot connu (Club, Entente…) : « du » / « de l' » ;</li>
     *   <li>sinon c'est un nom de lieu : « d' » devant une voyelle, « de » ailleurs.</li>
     * </ul>
     *
     * @return la proposition, ou « Tournoi » si le nom du club est vide
     */
    static String nomTournoi(String nomClub) {
        if (nomClub == null || nomClub.isBlank()) {
            return "Tournoi";
        }
        String nom = nomClub.trim();
        String premier = nom.split("[\\s\\-']+")[0];
        String cle = sansAccents(premier).toLowerCase(Locale.FRENCH);
        boolean voyelle = !cle.isEmpty() && "aeiouy".indexOf(cle.charAt(0)) >= 0;
        boolean sigle = premier.length() >= 2 && premier.chars().allMatch(c -> Character.isUpperCase(c) || c == '.');

        if (sigle) {
            return (voyelle ? "Tournoi de l'" : "Tournoi du ") + nom;
        }
        if (MASCULINS.contains(cle)) {
            return "Tournoi du " + nom;
        }
        if (ELIDES_ARTICLE.contains(cle)) {
            return "Tournoi de l'" + nom;
        }
        return (voyelle ? "Tournoi d'" : "Tournoi de ") + nom;
    }

    private static String sansAccents(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
