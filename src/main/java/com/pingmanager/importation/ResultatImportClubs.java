package com.pingmanager.importation;

import com.pingmanager.model.Club;

import java.util.List;

/**
 * Résultat de {@link LecteurClubsCsv#lire} : clubs valides et anomalies par ligne.
 *
 * @param clubs     clubs importés, dans l'ordre du fichier, sans doublon
 * @param anomalies anomalies par numéro de ligne
 */
public record ResultatImportClubs(List<Club> clubs, List<AnomalieImport> anomalies) {

    public ResultatImportClubs {
        clubs = List.copyOf(clubs);
        anomalies = List.copyOf(anomalies);
    }

    /** @return le nombre de lignes refusées. */
    public long lignesRefusees() {
        return anomalies.stream().filter(AnomalieImport::bloquante).count();
    }
}
