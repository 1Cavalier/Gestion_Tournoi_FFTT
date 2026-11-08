package com.pingmanager.model;

import java.util.Objects;

/**
 * Prononciation phonétique du nom d'un club pour les appels au micro (B018), en
 * alphabet phonétique international (API / IPA), identifiée par le numéro FFTT
 * du club.
 *
 * @param numero numéro du club
 * @param nom    nom du club au moment de la saisie (affichage seulement, peut être vide)
 * @param ipa    prononciation (vide si non renseignée)
 */
public record PrononciationClub(String numero, String nom, String ipa) {

    public PrononciationClub {
        Objects.requireNonNull(numero, "numero");
        numero = numero.trim();
        nom = nom == null ? "" : nom.trim();
        ipa = ipa == null ? "" : ipa;
    }
}
