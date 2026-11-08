package com.pingmanager.core;

import java.util.Objects;

/**
 * Issue de la diffusion d'une annonce (B017) : dite par la voix, ou affichée à
 * l'écran seulement (repli silencieux) avec le motif.
 *
 * @param annonce annonce traitée
 * @param dite    dite par la voix ?
 * @param motif   pourquoi elle n'a pas été dite (vide si dite)
 */
public record ResultatAnnonceVocale(AnnonceVocale annonce, boolean dite, String motif) {

    public ResultatAnnonceVocale {
        Objects.requireNonNull(annonce, "annonce");
        Objects.requireNonNull(motif, "motif");
    }

    /** Annonce seulement affichée (à lire au micro). */
    public boolean estAffichee() {
        return !dite;
    }
}
