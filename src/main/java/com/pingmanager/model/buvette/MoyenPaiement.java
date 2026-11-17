package com.pingmanager.model.buvette;

/** Moyen de paiement noté sur une vente de la buvette (B026). */
public enum MoyenPaiement {
    ESPECES("Espèces"),
    CARTE("Carte"),
    AUTRE("Autre");

    private final String libelle;

    MoyenPaiement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Relecture tolérante : {@link #AUTRE} si inconnu (la vente reste comptée). */
    public static MoyenPaiement lire(String nom) {
        for (MoyenPaiement m : values()) {
            if (m.name().equals(nom)) {
                return m;
            }
        }
        return AUTRE;
    }
}
