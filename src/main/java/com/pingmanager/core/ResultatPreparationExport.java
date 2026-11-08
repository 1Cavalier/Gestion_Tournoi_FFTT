package com.pingmanager.core;

import java.util.List;
import java.util.Optional;

/**
 * Résultat de {@link ServiceResultatsFederaux#construire} : soit des résultats
 * exportables (avec d'éventuels avertissements non bloquants : numéro
 * d'homologation absent, tableau non terminé laissé de côté, partie sans
 * résultat…), soit un {@link MotifRefusExport motif précis} de refus.
 */
public final class ResultatPreparationExport {

    private final ResultatsTournoi resultats;
    private final MotifRefusExport motif;
    private final List<String> avertissements;

    private ResultatPreparationExport(ResultatsTournoi resultats, MotifRefusExport motif, List<String> avertissements) {
        this.resultats = resultats;
        this.motif = motif;
        this.avertissements = List.copyOf(avertissements);
    }

    static ResultatPreparationExport exportable(ResultatsTournoi resultats, List<String> avertissements) {
        return new ResultatPreparationExport(resultats, null, avertissements);
    }

    static ResultatPreparationExport refuse(MotifRefusExport motif, List<String> avertissements) {
        return new ResultatPreparationExport(null, motif, avertissements);
    }

    public boolean estExportable() {
        return resultats != null;
    }

    /** @return les résultats, vides si l'export est refusé. */
    public Optional<ResultatsTournoi> getResultats() {
        return Optional.ofNullable(resultats);
    }

    /** @return le motif de refus, ou {@code null} si l'export est possible. */
    public MotifRefusExport getMotif() {
        return motif;
    }

    /** @return avertissements non bloquants, à montrer à l'utilisateur. */
    public List<String> getAvertissements() {
        return avertissements;
    }

    @Override
    public String toString() {
        return estExportable()
                ? "ResultatPreparationExport[exportable, " + resultats.parties().size() + " parties, "
                  + avertissements.size() + " avertissement(s)]"
                : "ResultatPreparationExport[refus=" + motif + "]";
    }
}
