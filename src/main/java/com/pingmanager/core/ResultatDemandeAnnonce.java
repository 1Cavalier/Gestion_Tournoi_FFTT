package com.pingmanager.core;

import java.util.Objects;

/**
 * Réponse à une demande d'annonce vocale (B017) : mise en file (avec sa place)
 * ou refusée avec un motif.
 *
 * @param acceptee annonce mise en file ?
 * @param motif    pourquoi elle est refusée (vide si acceptée)
 * @param annonce  annonce mise en file, ou null
 * @param position place dans la file (1 = la prochaine), 0 si refusée
 */
public record ResultatDemandeAnnonce(boolean acceptee, String motif, AnnonceVocale annonce, int position) {

    public ResultatDemandeAnnonce {
        Objects.requireNonNull(motif, "motif");
    }

    static ResultatDemandeAnnonce acceptee(AnnonceVocale annonce, int position) {
        return new ResultatDemandeAnnonce(true, "", Objects.requireNonNull(annonce), position);
    }

    static ResultatDemandeAnnonce refusee(String motif) {
        return new ResultatDemandeAnnonce(false, motif, null, 0);
    }
}
