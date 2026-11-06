package com.pingmanager.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Refus d'une demande d'homologation : quand il a été enregistré et pourquoi
 * (motif saisi par l'organisateur, tel que communiqué par l'instance qui
 * homologue). Conservé avec le tournoi pour rester visible après le retour au
 * brouillon. Objet valeur immuable.
 *
 * @param date  instant de l'enregistrement du refus
 * @param motif motif du refus, non vide
 */
public record RefusHomologation(Instant date, String motif) {

    public RefusHomologation {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(motif, "motif");
        motif = motif.trim();
        if (motif.isEmpty()) {
            throw new IllegalArgumentException("Le motif du refus est obligatoire.");
        }
    }
}
