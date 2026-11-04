package com.pingmanager.ui.infra;

import com.pingmanager.core.GenerateurCode;

import java.security.SecureRandom;

/**
 * Implémentation concrète du port {@link GenerateurCode} : un code numérique à
 * six chiffres tiré au hasard (000000 à 999999), via {@link SecureRandom}.
 */
public final class GenerateurCodeAleatoire implements GenerateurCode {

    private final SecureRandom alea = new SecureRandom();

    @Override
    public String genererCode() {
        int valeur = alea.nextInt(1_000_000);
        return String.format("%06d", valeur);
    }
}
