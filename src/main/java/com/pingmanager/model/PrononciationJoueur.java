package com.pingmanager.model;

import java.util.Objects;

/**
 * Prononciation phonétique d'un joueur pour les appels au micro (B018), en
 * alphabet phonétique international (API / IPA), identifiée par la licence.
 *
 * <p>Simple donnée : la validation de la saisie (caractères autorisés, longueur)
 * est faite par {@code core.ValidationPrononciation} avant l'enregistrement dans
 * {@code core.RegistrePrononciations}.</p>
 *
 * @param licence numéro de licence du joueur
 * @param prenom  prononciation du prénom (vide si non renseignée)
 * @param nom     prononciation du nom (vide si non renseignée)
 */
public record PrononciationJoueur(String licence, String prenom, String nom) {

    public PrononciationJoueur {
        Objects.requireNonNull(licence, "licence");
        licence = licence.trim();
        prenom = prenom == null ? "" : prenom;
        nom = nom == null ? "" : nom;
    }

    /** Aucune prononciation renseignée ? */
    public boolean estVide() {
        return prenom.isEmpty() && nom.isEmpty();
    }
}
