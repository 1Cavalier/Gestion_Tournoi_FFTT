package com.pingmanager.model;

/**
 * Fiabilité d'une date du calendrier fédéral (B045) : relevée dans une source
 * officielle et confirmée, ou encore à vérifier. Le code écrit dans le fichier
 * de calendrier est {@link #code()}.
 */
public enum StatutDateFederale {
    /** Date lue dans une source officielle (fftt.com, ligue, comité). */
    CONFIRMEE("confirmee", "Confirmée"),
    /** Date non confirmée par une source officielle lue : à vérifier avant de s'y fier. */
    A_VERIFIER("a-verifier", "À vérifier");

    private final String code;
    private final String libelle;

    StatutDateFederale(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /** @return le code du fichier de calendrier (« confirmee », « a-verifier »). */
    public String code() {
        return code;
    }

    /** @return le libellé affiché (« Confirmée », « À vérifier »). */
    public String libelle() {
        return libelle;
    }

    /** @return le statut de ce code (casse et espaces ignorés), ou {@code null} s'il est inconnu. */
    public static StatutDateFederale depuisCode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim().toLowerCase(java.util.Locale.ROOT);
        for (StatutDateFederale s : values()) {
            if (s.code.equals(c)) {
                return s;
            }
        }
        return null;
    }
}
