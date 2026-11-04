package com.pingmanager.core.reglement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatage à la française partagé par l'interface et le règlement généré. */
public final class Formats {

    public static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JOUR_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);

    private Formats() {
    }

    /** 9h, 9h05. */
    public static String heure(LocalTime t) {
        if (t == null) {
            return "";
        }
        return t.getMinute() == 0 ? t.getHour() + "h" : t.getHour() + "h" + String.format("%02d", t.getMinute());
    }

    /** 8 €, 8,50 €. */
    public static String montant(double v) {
        return v == Math.rint(v) ? String.format(Locale.FRENCH, "%,.0f €", v).replace(' ', ' ').replace(' ', ' ')
                : String.format(Locale.FRENCH, "%,.2f €", v).replace(' ', ' ').replace(' ', ' ');
    }

    /** samedi 17 avril 2027 ; lundi 1er février 2027. */
    public static String jourLong(LocalDate d) {
        return d == null ? "" : premier(d, d.format(JOUR_LONG));
    }

    /** Le 1er du mois s'écrit « 1er » (« lundi 1er février »). */
    private static String premier(LocalDate d, String texte) {
        return d.getDayOfMonth() == 1 ? texte.replaceFirst("(^|\\s)1(\\s)", "$11er$2") : texte;
    }

    /** sam. 17 avr. */
    public static String jourCourt(LocalDate d) {
        return d == null ? "" : d.format(JOUR_COURT);
    }

    /** samedi 17 avril 2027 à 20h. */
    public static String dateHeure(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        String j = jourLong(dt.toLocalDate());
        return dt.toLocalTime().equals(LocalTime.MIDNIGHT) ? j : j + " à " + heure(dt.toLocalTime());
    }

    /**
     * Période d'un tournoi en toutes lettres : « le samedi 17 avril 2027 »,
     * « les samedi 17 et dimanche 18 avril 2027 », « du vendredi 30 avril au
     * dimanche 2 mai 2027 ».
     */
    public static String periode(LocalDate debut, LocalDate fin) {
        if (debut == null) {
            return null;
        }
        if (fin == null || fin.equals(debut)) {
            return "le " + jourLong(debut);
        }
        boolean memeMois = debut.getMonth() == fin.getMonth() && debut.getYear() == fin.getYear();
        if (memeMois && fin.equals(debut.plusDays(1))) {
            return "les " + premier(debut, debut.format(DateTimeFormatter.ofPattern("EEEE d", Locale.FRENCH)) + " ")
                    .trim() + " et " + jourLong(fin);
        }
        return "du " + (debut.getYear() == fin.getYear() ? premier(debut, debut.format(JOUR_MOIS)) : jourLong(debut))
                + " au " + jourLong(fin);
    }

    /** Saison FFTT d'une date (septembre à août) : « 2026-2027 ». */
    public static String saison(LocalDate d) {
        int debut = d.getMonthValue() >= 9 ? d.getYear() : d.getYear() - 1;
        return debut + "-" + (debut + 1);
    }

    /** Première lettre en capitale. */
    public static String capitale(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
