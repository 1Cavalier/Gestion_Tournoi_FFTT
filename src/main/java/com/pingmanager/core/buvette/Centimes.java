package com.pingmanager.core.buvette;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalLong;

/**
 * Montants de la buvette (B046) : toujours des <b>centimes entiers</b>
 * ({@code long}), jamais des nombres à virgule flottante. Mise en forme
 * française (« 1,10 € ») et lecture d'une saisie (« 1,1 », « 1.10 », « 2 € »).
 */
public final class Centimes {

    /** Prix maximal accepté à la saisie : 1 000 €. */
    public static final long MAXIMUM = 100_000;

    private Centimes() {
    }

    /** 110 → « 1,10 € » ; -250 → « -2,50 € » ; 1234567 → « 12 345,67 € ». */
    public static String euros(long centimes) {
        return nombre(centimes) + " €";
    }

    /** 110 → « 1,10 » (sans symbole, pour un champ ou un export). */
    public static String nombre(long centimes) {
        String signe = centimes < 0 ? "-" : "";
        long abs = Math.abs(centimes);
        long e = abs / 100;
        long c = abs % 100;
        return signe + groupes(e) + "," + (c < 10 ? "0" : "") + c;
    }

    private static String groupes(long n) {
        String s = Long.toString(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Lit une saisie en euros (« 1,10 », « 1.1 », « 2 », « 2 € ») ; vide si illisible,
     * négative, avec plus de deux décimales ou au-delà de {@link #MAXIMUM}.
     */
    public static OptionalLong lire(String saisie) {
        if (saisie == null) {
            return OptionalLong.empty();
        }
        String s = saisie.replace("€", "").replace(' ', ' ').replace(" ", "").replace(',', '.').trim();
        if (s.isEmpty() || !s.matches("\\d{1,7}(\\.\\d{0,2})?")) {
            return OptionalLong.empty();
        }
        long v = new BigDecimal(s).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        return v > MAXIMUM ? OptionalLong.empty() : OptionalLong.of(v);
    }

    /** Arrondi au centime le plus proche (moitié vers le haut) d'un montant exact. */
    public static long arrondir(BigDecimal centimesExacts) {
        return centimesExacts.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
