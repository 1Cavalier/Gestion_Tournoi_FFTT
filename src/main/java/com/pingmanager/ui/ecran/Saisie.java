package com.pingmanager.ui.ecran;

import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lecture tolérante des saisies courantes (heures, montants, entiers) et
 * formatage à la française. Les méthodes {@code lire…} rendent {@code null}
 * pour une saisie vide et lèvent {@link IllegalArgumentException} (message
 * prêt à afficher) pour une saisie invalide.
 */
final class Saisie {

    static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter JOUR_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH);

    private static final Pattern HEURE = Pattern.compile("^(\\d{1,2})\\s*(?:[:hH.]\\s*(\\d{2})?)?$");

    private Saisie() {
    }

    /** « 9 », « 9h », « 9h30 », « 09:30 », « 9.30 » → 09:30. */
    static LocalTime lireHeure(String texte, String nomChamp) {
        String s = nettoyer(texte);
        if (s == null) {
            return null;
        }
        Matcher m = HEURE.matcher(s);
        if (m.matches()) {
            int h = Integer.parseInt(m.group(1));
            int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            if (h <= 23 && min <= 59) {
                return LocalTime.of(h, min);
            }
        }
        throw new IllegalArgumentException(nomChamp + " : heure attendue, par exemple 9h30 ou 09:30.");
    }

    /** « 8 », « 8,50 », « 8.5 », « 8 € » → 8.5. */
    static Double lireMontant(String texte, String nomChamp) {
        String s = nettoyer(texte);
        if (s == null) {
            return null;
        }
        s = s.replace("€", "").replace(" ", "").replace(',', '.');
        try {
            double v = Double.parseDouble(s);
            if (v >= 0) {
                return v;
            }
        } catch (NumberFormatException ignore) {
            // message ci-dessous
        }
        throw new IllegalArgumentException(nomChamp + " : montant attendu en euros, par exemple 8 ou 8,50.");
    }

    /** Entier positif ou nul. */
    static Integer lireEntier(String texte, String nomChamp) {
        String s = nettoyer(texte);
        if (s == null) {
            return null;
        }
        try {
            int v = Integer.parseInt(s.replace(" ", ""));
            if (v >= 0) {
                return v;
            }
        } catch (NumberFormatException ignore) {
            // message ci-dessous
        }
        throw new IllegalArgumentException(nomChamp + " : nombre entier attendu.");
    }

    static String heure(LocalTime t) {
        return com.pingmanager.core.reglement.Formats.heure(t);
    }

    static String montant(double v) {
        return com.pingmanager.core.reglement.Formats.montant(v);
    }

    /** Montant pour un champ de saisie (sans le symbole). */
    static String montantSaisie(double v) {
        return v == Math.rint(v) ? String.format(Locale.FRENCH, "%.0f", v)
                : String.format(Locale.FRENCH, "%.2f", v);
    }

    static String jourLong(LocalDate d) {
        return com.pingmanager.core.reglement.Formats.jourLong(d);
    }

    static String jourCourt(LocalDate d) {
        return com.pingmanager.core.reglement.Formats.jourCourt(d);
    }

    /**
     * Sélecteur de date au format jj/mm/aaaa. Si {@code debut} et {@code fin}
     * sont donnés, les jours hors de cet intervalle sont grisés et refusés.
     */
    static DatePicker selecteurDate(LocalDate valeur, LocalDate debut, LocalDate fin) {
        DatePicker p = new DatePicker(valeur);
        p.setPromptText("jj/mm/aaaa");
        p.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : d.format(JJ_MM_AAAA);
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(s.trim(), JJ_MM_AAAA);
                } catch (RuntimeException e) {
                    return null;
                }
            }
        });
        if (debut != null && fin != null) {
            p.setDayCellFactory(dp -> new DateCell() {
                @Override
                public void updateItem(LocalDate d, boolean vide) {
                    super.updateItem(d, vide);
                    if (d != null && (d.isBefore(debut) || d.isAfter(fin))) {
                        setDisable(true);
                    }
                }
            });
        }
        return p;
    }

    private static String nettoyer(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
