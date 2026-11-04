package com.pingmanager.model;

import java.util.Objects;

/**
 * Un joueur qualifié pour le tableau à élimination, avec la trace de sa poule
 * d'origine et de son rang dans cette poule.
 *
 * <p>Cette information est indispensable au placement FFTT (article I.305) :
 * elle permet de rattacher un 2e de poule à son 1er (pour l'envoyer dans le
 * demi-tableau opposé) et d'appliquer les contraintes d'association.</p>
 *
 * @param joueur       le joueur qualifié
 * @param nomPoule     le nom de la poule d'origine (ex. « A »)
 * @param rangDansPoule le rang obtenu dans la poule (1 = premier, 2 = deuxième…)
 */
public record Qualifie(Joueur joueur, String nomPoule, int rangDansPoule) {

    public Qualifie {
        Objects.requireNonNull(joueur, "joueur");
        Objects.requireNonNull(nomPoule, "nomPoule");
        if (rangDansPoule < 1) {
            throw new IllegalArgumentException("Le rang dans la poule doit être >= 1.");
        }
    }

    @Override
    public String toString() {
        return joueur.getNomComplet() + " (" + rangDansPoule + "e poule " + nomPoule + ")";
    }
}
