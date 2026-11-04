package com.pingmanager.model;

/**
 * Catégories d'âge FFTT (règlements généraux 2026-2027, articles II.302 pour les
 * adultes et II.303 pour les jeunes).
 *
 * <p>L'âge de référence est celui atteint au 1er janvier de la saison en cours.
 * Les catégories sont ordonnées de la plus jeune à la plus âgée. Chaque constante
 * porte son libellé officiel et, pour les catégories jeunes, le code « U » associé.</p>
 *
 * <p>Un {@link Tableau} peut être réservé à un ou plusieurs catégories via
 * {@link ConfigurationTableau#getCategoriesAutorisees()} (ensemble vide = toutes
 * catégories admises). La liste est volontairement fidèle au règlement (utile pour
 * un futur export FFTT), même si l'organisateur regroupe souvent plusieurs
 * catégories pour définir un tableau « vétérans » ou « jeunes ».</p>
 */
public enum CategorieAge {

    // --- Jeunes (II.303) ---
    POUSSIN("Poussins", "U9"),
    BENJAMIN("Benjamins", "U11"),
    MINIME("Minimes", "U13"),
    CADET("Cadets", "U15"),
    JUNIOR("Juniors", "U19"),

    // --- Adultes (II.302) ---
    SENIOR("Seniors", null),
    VETERAN_40("Vétérans 40", null),
    VETERAN_45("Vétérans 45", null),
    VETERAN_50("Vétérans 50", null),
    VETERAN_55("Vétérans 55", null),
    VETERAN_60("Vétérans 60", null),
    VETERAN_65("Vétérans 65", null),
    VETERAN_70("Vétérans 70", null),
    VETERAN_75("Vétérans 75", null),
    VETERAN_80("Vétérans 80", null),
    VETERAN_85("Vétérans 85", null),
    VETERAN_90("Vétérans 90", null);

    private final String libelle;
    private final String codeU; // null pour les adultes

    CategorieAge(String libelle, String codeU) {
        this.libelle = libelle;
        this.codeU = codeU;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return le code « U » (U9, U11…) pour les jeunes, ou {@code null} pour les adultes. */
    public String getCodeU() {
        return codeU;
    }

    /** @return true s'il s'agit d'une catégorie jeune (poussin à junior). */
    public boolean estJeune() {
        return codeU != null;
    }

    /** @return true s'il s'agit d'une catégorie vétéran (V40 et au-delà). */
    public boolean estVeteran() {
        return name().startsWith("VETERAN_");
    }

    /**
     * Année de référence d'une saison : la saison court du 1er juillet au 30 juin,
     * et « le 1er janvier de la saison en cours » (II.302 / II.303) s'entend, d'après
     * la grille 2026-2027 des règlements généraux (seniors nés de 1987 à 2007, poussins
     * nés en 2018 et après), comme le 1er janvier de sa première année : 2026 pour
     * 2026-2027.
     *
     * @param date une date de la saison (ex. le premier jour du tournoi)
     * @return la première année de la saison
     */
    public static int anneeDeReference(java.time.LocalDate date) {
        return date.getMonthValue() >= 7 ? date.getYear() : date.getYear() - 1;
    }

    /**
     * Catégorie d'âge d'un joueur pour la saison d'une date donnée (II.302 / II.303).
     *
     * @param naissance date de naissance ({@code null} : catégorie inconnue)
     * @param date      une date de la saison (premier jour du tournoi)
     * @return la catégorie, ou {@code null} si la date de naissance est inconnue
     */
    public static CategorieAge pour(java.time.LocalDate naissance, java.time.LocalDate date) {
        if (naissance == null || date == null) {
            return null;
        }
        int age = anneeDeReference(date) - naissance.getYear();
        if (age < 9) return POUSSIN;
        if (age <= 10) return BENJAMIN;
        if (age <= 12) return MINIME;
        if (age <= 14) return CADET;
        if (age <= 18) return JUNIOR;
        if (age < 40) return SENIOR;
        if (age >= 90) return VETERAN_90;
        CategorieAge[] veterans = {VETERAN_40, VETERAN_45, VETERAN_50, VETERAN_55, VETERAN_60,
                VETERAN_65, VETERAN_70, VETERAN_75, VETERAN_80, VETERAN_85};
        return veterans[(age - 40) / 5];
    }
}
