package com.pingmanager.core;

import java.util.Optional;

/**
 * Numéro de portable saisi pour les convocations par SMS (B019).
 *
 * <p>Formats acceptés : portable français à 10 chiffres commençant par 06 ou 07,
 * {@code +33 6…} / {@code +33 7…}, {@code 0033 6…}, ou un numéro international
 * {@code +} suivi de 8 à 15 chiffres. Espaces, points, tirets et parenthèses
 * sont ignorés. Le numéro gardé est au format international
 * ({@code +33612345678}), celui qu'attendent les services d'envoi.</p>
 */
public final class NumeroPortable {

    private NumeroPortable() {
    }

    /** Numéro au format international, ou vide s'il n'est pas reconnu comme un portable. */
    public static Optional<String> normaliser(String saisie) {
        if (saisie == null) {
            return Optional.empty();
        }
        String s = saisie.replaceAll("[\\s.\\-()\\u00a0\\u202f]", "");
        if (s.startsWith("00")) {
            s = "+" + s.substring(2);
        }
        if (s.matches("0[67]\\d{8}")) {
            return Optional.of("+33" + s.substring(1));
        }
        if (s.startsWith("+33")) {
            return s.matches("\\+33[67]\\d{8}") ? Optional.of(s) : Optional.empty();
        }
        if (s.matches("\\+[1-9]\\d{7,14}")) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    /** Numéro masqué pour l'affichage : « +33 6 •• •• •• 78 » (les deux derniers chiffres restent). */
    public static String masquer(String numero) {
        if (numero == null || numero.length() < 4) {
            return "••";
        }
        if (numero.matches("\\+33[67]\\d{8}")) {
            return "+33 " + numero.charAt(3) + " •• •• •• " + numero.substring(numero.length() - 2);
        }
        return numero.substring(0, 3) + " •••• " + numero.substring(numero.length() - 2);
    }
}
