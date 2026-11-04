package com.pingmanager.model;

/** Moyen de paiement en ligne des inscriptions. */
public final class Paiement {

    /** Plateforme de paiement. Seul HelloAsso est disponible pour l'instant. */
    public enum Mode {
        HELLOASSO("HelloAsso", true),
        PLATEFORME("Plateforme PingManager", false),
        AUTRE("Autre lien de paiement", false);

        private final String libelle;
        private final boolean disponible;

        Mode(String libelle, boolean disponible) {
            this.libelle = libelle;
            this.disponible = disponible;
        }

        public String getLibelle() {
            return libelle;
        }

        public boolean estDisponible() {
            return disponible;
        }
    }

    private final Mode mode;
    private final String lien;

    public Paiement(Mode mode, String lien) {
        this.mode = mode;
        this.lien = lien == null || lien.isBlank() ? null : lien.trim();
        if (this.lien != null && !this.lien.matches("(?i)https?://\\S+")) {
            throw new IllegalArgumentException("Le lien de paiement doit être une adresse web (https://…).");
        }
    }

    public static Paiement aucun() {
        return new Paiement(null, null);
    }

    public Mode getMode() {
        return mode;
    }

    public String getLien() {
        return lien;
    }

    /** @return vrai si un mode est choisi et son lien renseigné. */
    public boolean estPret() {
        return mode != null && lien != null;
    }

    /** @return vrai si le lien semble bien être une page HelloAsso. */
    public boolean lienHelloAsso() {
        return lien != null && lien.toLowerCase(java.util.Locale.ROOT).contains("helloasso.");
    }
}
