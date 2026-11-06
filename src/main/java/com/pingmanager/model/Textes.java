package com.pingmanager.model;

/**
 * Nettoyage des textes saisis, commun aux entités du modèle (auparavant recopié
 * dans chaque classe).
 */
final class Textes {

    private Textes() {
    }

    /**
     * @return le texte sans espaces autour
     * @throws IllegalArgumentException « Le champ « champ » est obligatoire. » si le
     *                                  texte est absent ou blanc
     */
    static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » est obligatoire.");
        }
        return valeur.trim();
    }

    /** @return le texte sans espaces autour, ou {@code null} s'il est absent ou vide après {@code trim()} */
    static String optionnel(String valeur) {
        if (valeur == null) {
            return null;
        }
        String t = valeur.trim();
        return t.isEmpty() ? null : t;
    }
}
