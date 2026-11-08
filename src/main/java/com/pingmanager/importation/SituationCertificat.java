package com.pingmanager.importation;

/**
 * Situation déclarée du licencié vis-à-vis du certificat médical, telle que
 * fournie par le club dans {@code licencies.csv} (II.606.1). Donnée liée à la
 * santé : seule cette synthèse est lue, rien de plus (RECHERCHE_FFTT J5).
 */
public enum SituationCertificat {
    EN_REGLE,
    SANS_PRATIQUE,
    INCONNU
}
