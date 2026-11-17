package com.pingmanager.model.buvette;

import java.time.Instant;
import java.util.Objects;

/**
 * Annulation d'une vente de la buvette (B026). La vente n'est jamais effacée :
 * elle reste dans l'historique, marquée annulée.
 *
 * @param horodatage instant de l'annulation
 * @param auteur     qui a annulé
 */
public record AnnulationVente(Instant horodatage, String auteur) {

    public AnnulationVente {
        Objects.requireNonNull(horodatage, "horodatage");
        auteur = auteur == null || auteur.isBlank() ? "inconnu" : auteur.trim();
    }
}
