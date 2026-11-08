package com.pingmanager.core;

import com.pingmanager.model.FicheLicence;

import java.util.Optional;

/**
 * Port : référentiel des licences FFTT (en lecture seule).
 *
 * <p>Symétrique de {@link RegistreClubs} : il donne accès aux fiches de licence
 * par leur numéro, pour la démarche de <b>liaison d'un compte à sa licence</b>.
 * L'application <b>n'administre pas</b> cette base — elle l'interroge seulement,
 * puisque la source de vérité reste la fédération.</p>
 *
 * <p>Comme pour les clubs, en faire un port permet de brancher plus tard la
 * vraie source fédérale (import FFTT, service en ligne) sans toucher au cœur.
 * En attendant, une implémentation alimentée à la main tient lieu de base de
 * test.</p>
 */
public interface RegistreLicences {

    /**
     * Recherche une licence par son numéro FFTT.
     *
     * @param numeroLicence numéro de licence (les espaces de bord sont tolérés ;
     *                      un numéro nul ou vide renvoie un résultat vide)
     * @return la fiche si la licence existe, sinon {@link Optional#empty()}
     */
    Optional<FicheLicence> trouverParLicence(String numeroLicence);

    /**
     * @param numeroLicence numéro de licence
     * @return {@code true} si une licence porte ce numéro
     */
    default boolean existe(String numeroLicence) {
        return trouverParLicence(numeroLicence).isPresent();
    }

    /**
     * Recherche des licenciés par nom, prénom ou numéro de licence (début de
     * numéro, ou fragment de nom/prénom, sans tenir compte des accents ni de la
     * casse ; plusieurs mots doivent tous correspondre).
     *
     * @param texte saisie libre (au moins 2 caractères utiles)
     * @param max   nombre maximum de résultats
     * @return les fiches correspondantes (vide par défaut si la source ne sait pas chercher)
     */
    default java.util.List<FicheLicence> rechercher(String texte, int max) {
        return java.util.List.of();
    }

    /**
     * Liste les licenciés d'un club (correspond au service fédéral
     * {@code xml_licence_b?club=}, voir {@code docs/RECHERCHE_FFTT.md} §4.1).
     *
     * @param numeroClub numéro du club
     * @return les fiches du club (vide par défaut si la source ne sait pas lister)
     */
    default java.util.List<FicheLicence> listerParClub(String numeroClub) {
        return java.util.List.of();
    }
}
