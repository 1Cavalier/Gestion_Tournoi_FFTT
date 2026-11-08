package com.pingmanager.core;

import com.pingmanager.model.Club;

import java.util.Optional;

/**
 * Port : référentiel des clubs FFTT.
 *
 * <p>Fournit l'accès en lecture aux clubs par leur numéro. Le service
 * d'inscription s'en sert pour retrouver le club de rattachement et, surtout,
 * son {@link Club#getEmailOfficiel() email officiel}, destinataire du code de
 * vérification.</p>
 *
 * <p>À terme, ce référentiel sera alimenté depuis une source fédérale (import
 * FFTT) ou une base de données. En faire un port permet de brancher cette source
 * ultérieurement sans modifier le service. Une implémentation en mémoire
 * ({@link RegistreClubsMemoire}) est fournie pour l'amorçage et les tests.</p>
 */
public interface RegistreClubs {

    /**
     * Recherche un club par son numéro FFTT.
     *
     * @param numeroClub numéro du club (l'implémentation tolère les espaces de
     *                   bord ; un numéro nul ou vide renvoie un résultat vide)
     * @return le club s'il existe, sinon {@link Optional#empty()}
     */
    Optional<Club> trouverParNumero(String numeroClub);

    /**
     * @param numeroClub numéro du club
     * @return {@code true} si un club porte ce numéro
     */
    default boolean existe(String numeroClub) {
        return trouverParNumero(numeroClub).isPresent();
    }

    /**
     * Recherche des clubs par nom, ville ou début de numéro (sans tenir compte
     * des accents ni de la casse ; plusieurs mots doivent tous correspondre).
     *
     * @param texte saisie libre (au moins 2 caractères utiles)
     * @param max   nombre maximum de résultats
     * @return les clubs correspondants (vide par défaut si la source ne sait pas chercher)
     */
    default java.util.List<Club> rechercher(String texte, int max) {
        return java.util.List.of();
    }
}
