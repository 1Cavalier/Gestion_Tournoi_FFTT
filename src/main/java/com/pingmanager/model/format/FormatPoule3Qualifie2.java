package com.pingmanager.model.format;

import com.pingmanager.model.FormatPhaseFinale;

/**
 * Format « <b>Poule de 3 — 2 qualifiés</b> ».
 *
 * <p>C'est le format des tournois individuels homologués courants (type
 * Maisons-Alfort, Sucy, Lille…) tel que retenu pour la première version :</p>
 * <ul>
 *   <li>poules de 3 joueurs ;</li>
 *   <li>les 2 premiers de chaque poule sont qualifiés pour le tableau ;</li>
 *   <li>le 3e est éliminé ;</li>
 *   <li>phase finale à élimination directe ;</li>
 *   <li>matchs au meilleur des 5 manches (best-of 5).</li>
 * </ul>
 *
 * <p>La règle anti-arrangement (joueurs d'un même club au premier tour de la
 * poule) s'applique à ce format comme à tous les autres : elle est portée par le
 * générateur de matchs, pas par ce fichier.</p>
 */
public final class FormatPoule3Qualifie2 implements FormatTournoi {

    /** Instance partagée (le format est sans état). */
    public static final FormatPoule3Qualifie2 INSTANCE = new FormatPoule3Qualifie2();

    @Override
    public String nom() {
        return "Poule de 3 — 2 qualifiés";
    }

    @Override
    public String identifiant() {
        return "P3Q2";
    }

    @Override
    public int taillePouleSouhaitee() {
        return 3;
    }

    @Override
    public int qualifiesParPoule() {
        return 2;
    }

    @Override
    public SortNonQualifies sortNonQualifies() {
        return SortNonQualifies.ELIMINE;
    }

    @Override
    public FormatPhaseFinale formatPhaseFinale() {
        return FormatPhaseFinale.ELIMINATION_DIRECTE;
    }

    @Override
    public int nombreManchesGagnantes() {
        return 3; // best-of 5
    }
}
