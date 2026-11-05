package com.pingmanager.ui.ecran;

import com.pingmanager.core.reglement.Formats;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;

/**
 * Présentation du formulaire « Nouveau tournoi » (sans JavaFX) : libellés,
 * texte explicatif, lecture des dates saisies, vérification avant création
 * du brouillon et résumé de la période. La vue {@link FormulaireNouveauTournoi}
 * ne fait qu'afficher ce qui est décidé ici.
 */
final class PresentationNouveauTournoi {

    static final String TITRE = "Nouveau tournoi";

    /** Cahier des charges v0, section 5 : dire pourquoi ces informations sont demandées. */
    static final String TEXTE_EXPLICATIF = "Ces informations sont obligatoires pour demander "
            + "l'homologation du tournoi, ou simplement pour le gérer dans PingManager. "
            + "L'adresse du club et l'administrateur principal sont repris par défaut ; "
            + "vous compléterez le reste à la page suivante.";

    static final String TITRE_CARTE_TOURNOI = "Le tournoi";
    static final String TITRE_CARTE_DATES = "Dates";

    static final String LIBELLE_CLUB = "Club organisateur";
    static final String LIBELLE_NOM = "Nom du tournoi";
    static final String LIBELLE_NIVEAU = "Niveau du tournoi (catégorie)";
    static final String LIBELLE_DEBUT = "Date de début";
    static final String LIBELLE_FIN = "Date de fin";

    static final String FORMAT_DATE = "jj/mm/aaaa";
    static final String AIDE_CALENDRIER = "Choisir dans le calendrier";

    static final String NOTE_DATES = "Du premier au dernier jour de l'ensemble du tournoi, "
            + "toutes journées comprises. Tapez la date ou cliquez sur le calendrier. "
            + "Pour un tournoi d'un jour, laissez la date de fin vide.";

    static final String BOUTON_CREER = "Créer le brouillon";
    static final String BOUTON_ANNULER = "Annuler";

    static final String MOTIF_NOM = "Donnez un nom au tournoi.";
    static final String MOTIF_CLUB = "Aucun club rattaché à votre compte : rattachez-en un "
            + "depuis « Mon profil ».";
    static final String MOTIF_DEBUT_VIDE = "Indiquez la date de début (" + FORMAT_DATE + ").";
    static final String MOTIF_DEBUT_ILLISIBLE = "La date de début n'est pas valide (" + FORMAT_DATE + ").";
    static final String MOTIF_FIN_ILLISIBLE = "La date de fin n'est pas valide (" + FORMAT_DATE + ").";
    static final String MOTIF_FIN_AVANT_DEBUT = "La date de fin ne peut pas précéder la date de début.";

    /** Lecture stricte : le 31/02 est refusé au lieu d'être ramené au 28. */
    private static final DateTimeFormatter SAISIE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Champ à signaler quand la vérification échoue. */
    enum Champ { CLUB, NOM, DEBUT, FIN }

    /**
     * Résultat de la vérification : soit valide (nom nettoyé, dates retenues,
     * la fin vaut le début pour un tournoi d'un jour), soit refusé avec le
     * champ fautif et le motif à afficher.
     */
    record Verification(Champ champEnErreur, String motif, String nom, LocalDate debut, LocalDate fin) {
        boolean valide() {
            return motif == null;
        }

        private static Verification refus(Champ champ, String motif) {
            return new Verification(champ, motif, null, null, null);
        }
    }

    private PresentationNouveauTournoi() {
    }

    /** Date affichée dans le champ (jj/mm/aaaa), vide si aucune. */
    static String formater(LocalDate d) {
        return d == null ? "" : d.format(SAISIE);
    }

    /** Date lue dans le champ, ou {@code null} si vide ou illisible. */
    static LocalDate lireDate(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texte.trim(), SAISIE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Vérifie la saisie avant de créer le brouillon. Le niveau du tournoi
     * reste facultatif à ce stade (il sera exigé pour l'homologation).
     */
    static Verification verifier(String nom, boolean clubPresent, String debut, String fin) {
        String n = nom == null ? "" : nom.trim();
        if (n.isEmpty()) {
            return Verification.refus(Champ.NOM, MOTIF_NOM);
        }
        if (!clubPresent) {
            return Verification.refus(Champ.CLUB, MOTIF_CLUB);
        }
        if (debut == null || debut.isBlank()) {
            return Verification.refus(Champ.DEBUT, MOTIF_DEBUT_VIDE);
        }
        LocalDate d = lireDate(debut);
        if (d == null) {
            return Verification.refus(Champ.DEBUT, MOTIF_DEBUT_ILLISIBLE);
        }
        LocalDate f = d;
        if (fin != null && !fin.isBlank()) {
            f = lireDate(fin);
            if (f == null) {
                return Verification.refus(Champ.FIN, MOTIF_FIN_ILLISIBLE);
            }
            if (f.isBefore(d)) {
                return Verification.refus(Champ.FIN, MOTIF_FIN_AVANT_DEBUT);
            }
        }
        return new Verification(null, null, n, d, f);
    }

    /** Dans le calendrier de la date de fin, les jours avant le début sont grisés. */
    static boolean jourDeFinPossible(LocalDate debut, LocalDate jour) {
        return debut == null || jour == null || !jour.isBefore(debut);
    }

    /**
     * Résumé de la période sous les dates, mis à jour pendant la saisie :
     * vide tant que le début n'est pas lisible ou que la fin est incohérente.
     */
    static String resumePeriode(String debut, String fin) {
        LocalDate d = lireDate(debut);
        if (d == null) {
            return "";
        }
        LocalDate f = fin == null || fin.isBlank() ? d : lireDate(fin);
        if (f == null || f.isBefore(d)) {
            return "";
        }
        if (f.equals(d)) {
            return "Tournoi d'un jour : " + Formats.jourLong(d) + ".";
        }
        long jours = ChronoUnit.DAYS.between(d, f) + 1;
        return "Tournoi sur " + jours + " jours : du " + Formats.jourLong(d)
                + " au " + Formats.jourLong(f) + ".";
    }
}
