package com.pingmanager.core;

import com.pingmanager.model.Joueur;

/**
 * Joueur tel qu'il figure dans un export de résultats : uniquement ce que le
 * tournoi contient déjà (ni date de naissance, ni certificat médical).
 *
 * @param licence numéro de licence (vide si le joueur n'en a pas)
 * @param nom     nom
 * @param prenom  prénom
 * @param club    club
 * @param points  points retenus pour le tournoi
 */
public record JoueurExporte(String licence, String nom, String prenom, String club, int points) {

    public JoueurExporte {
        licence = licence == null ? "" : licence;
        nom = nom == null ? "" : nom;
        prenom = prenom == null ? "" : prenom;
        club = club == null ? "" : club;
    }

    /** @return la vue export d'un joueur du tournoi. */
    public static JoueurExporte de(Joueur j) {
        return new JoueurExporte(j.getNumeroLicence(), j.getNom(), j.getPrenom(), j.getClub(), j.getPoints());
    }
}
