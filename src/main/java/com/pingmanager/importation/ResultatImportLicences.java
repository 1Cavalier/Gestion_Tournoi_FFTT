package com.pingmanager.importation;

import com.pingmanager.model.FicheLicence;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Résultat de {@link LecteurLicencesCsv#lire} : fiches valides, anomalies par
 * ligne, situation du certificat par licence et date de relevé la plus ancienne.
 *
 * @param fiches       fiches importées, dans l'ordre du fichier, sans doublon
 * @param anomalies    anomalies par numéro de ligne
 * @param certificats  situation du certificat médical par numéro de licence
 * @param releveLe     date « Relevé le » la plus ancienne du fichier (null si absente)
 */
public record ResultatImportLicences(List<FicheLicence> fiches, List<AnomalieImport> anomalies,
                                     Map<String, SituationCertificat> certificats, LocalDate releveLe) {

    public ResultatImportLicences {
        fiches = List.copyOf(fiches);
        anomalies = List.copyOf(anomalies);
        certificats = Map.copyOf(certificats);
    }

    /** @return le nombre de lignes refusées. */
    public long lignesRefusees() {
        return anomalies.stream().filter(AnomalieImport::bloquante).count();
    }
}
