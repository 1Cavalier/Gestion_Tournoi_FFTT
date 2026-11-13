package com.pingmanager.core;

/**
 * Opérations sensibles soumises à {@link ControleDroits} (point 16 du README) : le
 * droit est vérifié au moment de l'opération, pas seulement par la navigation.
 */
public enum OperationProtegee {
    CREER_TOURNOI("créer un tournoi pour ce club"),
    MODIFIER_TOURNOI("modifier ce tournoi"),
    SUPPRIMER_TOURNOI("supprimer ce tournoi"),
    HOMOLOGUER_TOURNOI("changer l'étape d'homologation de ce tournoi"),
    DONNEES_CLUB("consulter les données de ce club"),
    COMPTES_CLUB("gérer les comptes de ce club"),
    SUPERVISION("ouvrir la supervision");

    private final String libelle;

    OperationProtegee(String libelle) {
        this.libelle = libelle;
    }

    /** @return l'opération en clair, pour les messages (« modifier ce tournoi »). */
    public String getLibelle() {
        return libelle;
    }
}
