package com.pingmanager.core.buvette;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalLong;

/**
 * Lecture et écriture des petits nombres décimaux de la buvette (B046) sans
 * nombre à virgule flottante : « 1,5 » demi-journée = 15 dixièmes, « 0,35 »
 * café par personne = 35 centièmes.
 */
public final class NombresBuvette {

    private NombresBuvette() {
    }

    /**
     * Lit une saisie positive avec au plus {@code decimales} chiffres après la virgule
     * et la renvoie multipliée par 10^decimales (« 1,5 » avec 1 décimale → 15).
     */
    public static OptionalLong lire(String saisie, int decimales) {
        if (saisie == null) {
            return OptionalLong.empty();
        }
        String s = saisie.replace(' ', ' ').replace(" ", "").replace("%", "").replace(',', '.').trim();
        String motif = decimales == 0 ? "\\d{1,7}" : "\\d{1,7}(\\.\\d{0," + decimales + "})?";
        if (s.isEmpty() || !s.matches(motif)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(new BigDecimal(s).movePointRight(decimales).setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact());
    }

    /** 15 avec 1 décimale → « 1,5 » ; 35 avec 2 décimales → « 0,35 » ; 100 avec 2 → « 1 ». */
    public static String ecrire(long valeur, int decimales) {
        return texte(BigDecimal.valueOf(valeur).movePointLeft(decimales));
    }

    /** Nombre exact écrit à la française, sans zéros inutiles, au plus deux décimales. */
    public static String texte(BigDecimal v) {
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        if (r.scale() < 0) {
            r = r.setScale(0);
        }
        return r.toPlainString().replace('.', ',');
    }
}
