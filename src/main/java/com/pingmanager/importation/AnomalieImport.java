package com.pingmanager.importation;

/**
 * Anomalie relevée sur une ligne d'un fichier d'échange (licenciés, clubs).
 *
 * @param ligne     numéro de ligne dans le fichier (1 = en-têtes)
 * @param motif     message destiné à l'utilisateur
 * @param bloquante true si la ligne n'a pas été importée ; false si seule une
 *                  valeur facultative a été ignorée
 */
public record AnomalieImport(int ligne, String motif, boolean bloquante) {

    @Override
    public String toString() {
        return "Ligne " + ligne + " : " + motif + (bloquante ? " (non importée)" : "");
    }
}
