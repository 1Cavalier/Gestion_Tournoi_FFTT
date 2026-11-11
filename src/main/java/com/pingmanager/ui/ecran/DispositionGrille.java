package com.pingmanager.ui.ecran;

/**
 * Calcul pur (sans JavaFX) du nombre de colonnes d'une grille adaptative
 * ({@link Ui#grilleAdaptative}) : le plus grand diviseur de {@code colonnesMax}
 * pour lequel chaque colonne garde au moins {@code largeurMin} pixels. Avec 4 au
 * maximum, on passe de 4 à 2 puis à 1 colonne ; avec 3, de 3 à 1 (jamais une
 * rangée bancale de 2 + 1).
 */
final class DispositionGrille {

    private DispositionGrille() {
    }

    /**
     * @param largeur      largeur disponible (pixels), 0 si pas encore connue
     * @param ecart        écart horizontal entre deux colonnes
     * @param colonnesMax  nombre de colonnes quand la place le permet (au moins 1)
     * @param largeurMin   largeur minimale d'une colonne
     * @return le nombre de colonnes à utiliser, au moins 1
     */
    static int colonnes(double largeur, double ecart, int colonnesMax, double largeurMin) {
        for (int n = Math.max(1, colonnesMax); n > 1; n--) {
            if (colonnesMax % n != 0) {
                continue;
            }
            if (largeur >= n * largeurMin + (n - 1) * ecart) {
                return n;
            }
        }
        return 1;
    }
}
