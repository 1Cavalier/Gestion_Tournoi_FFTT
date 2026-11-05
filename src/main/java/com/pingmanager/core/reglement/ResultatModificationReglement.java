package com.pingmanager.core.reglement;

import java.util.Objects;

/**
 * Réponse de {@link ServiceReglementTournoi} à une demande de modification du
 * règlement (mode généré / déposé, articles ajoutés, fichier déposé). Objet
 * valeur immuable.
 */
public final class ResultatModificationReglement {

    /** Raison pour laquelle la modification a été faite ou refusée. */
    public enum Motif {
        /** La modification est appliquée au tournoi. */
        APPLIQUEE,
        /** Le tournoi est publié : règlement et feuille d'engagement sont figés. */
        TOURNOI_PUBLIE
    }

    private static final ResultatModificationReglement APPLIQUEE =
            new ResultatModificationReglement(Motif.APPLIQUEE, null);

    private final Motif motif;
    private final String message;

    private ResultatModificationReglement(Motif motif, String message) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.message = message;
    }

    static ResultatModificationReglement appliquee() {
        return APPLIQUEE;
    }

    static ResultatModificationReglement refusee(Motif motif, String message) {
        return new ResultatModificationReglement(motif, message);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return vrai si la modification a été appliquée. */
    public boolean estAppliquee() {
        return motif == Motif.APPLIQUEE;
    }

    /** @return le motif du refus en clair, ou {@code null} si la modification est appliquée. */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ResultatModificationReglement[" + motif + "]";
    }
}
