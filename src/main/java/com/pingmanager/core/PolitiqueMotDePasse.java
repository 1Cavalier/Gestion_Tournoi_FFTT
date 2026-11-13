package com.pingmanager.core;

import java.util.Optional;

/**
 * Politique de mot de passe (README section 5, point 15) : au moins
 * {@value #LONGUEUR_MINIMALE} caractères, exigés à la création d'un compte et au
 * changement de mot de passe. Les comptes existants ne sont pas invalidés : la
 * connexion n'applique pas cette règle.
 */
public final class PolitiqueMotDePasse {

    /** Longueur minimale d'un mot de passe. */
    public static final int LONGUEUR_MINIMALE = 10;

    private PolitiqueMotDePasse() {
    }

    /** @return {@code true} si le mot de passe respecte la politique. */
    public static boolean estAcceptable(String motDePasse) {
        return motDePasse != null && motDePasse.codePointCount(0, motDePasse.length()) >= LONGUEUR_MINIMALE;
    }

    /** @return le motif de refus, en clair, ou vide si le mot de passe est acceptable. */
    public static Optional<String> motifRefus(String motDePasse) {
        if (estAcceptable(motDePasse)) {
            return Optional.empty();
        }
        int saisis = motDePasse == null ? 0 : motDePasse.codePointCount(0, motDePasse.length());
        return Optional.of("Le mot de passe doit contenir au moins " + LONGUEUR_MINIMALE
                + " caractères (" + saisis + " saisi" + (saisis > 1 ? "s" : "") + ").");
    }
}
