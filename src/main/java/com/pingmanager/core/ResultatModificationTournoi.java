package com.pingmanager.core;

import com.pingmanager.model.ChampHomologation;

import java.util.List;
import java.util.Objects;

/**
 * Réponse de {@link ServiceVerrousHomologation} à une demande de modification
 * d'un champ du tournoi. Objet valeur immuable.
 */
public final class ResultatModificationTournoi {

    /** Raison pour laquelle la modification a été faite ou refusée. */
    public enum Motif {
        /** La modification est appliquée (ou la valeur était déjà celle demandée). */
        APPLIQUEE,
        /** Le champ fait partie du dossier d'homologation, verrouillé depuis la demande. */
        CHAMP_VERROUILLE
    }

    private static final ResultatModificationTournoi APPLIQUEE =
            new ResultatModificationTournoi(Motif.APPLIQUEE, List.of(), null);

    private final Motif motif;
    private final List<ChampHomologation> champs;
    private final String message;

    private ResultatModificationTournoi(Motif motif, List<ChampHomologation> champs, String message) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.champs = List.copyOf(champs);
        this.message = message;
    }

    static ResultatModificationTournoi appliquee() {
        return APPLIQUEE;
    }

    static ResultatModificationTournoi verrouille(List<ChampHomologation> champs, String message) {
        return new ResultatModificationTournoi(Motif.CHAMP_VERROUILLE, champs, message);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return vrai si la modification a été appliquée. */
    public boolean estAppliquee() {
        return motif == Motif.APPLIQUEE;
    }

    /** @return les champs verrouillés que la modification aurait changés (vide si appliquée). */
    public List<ChampHomologation> getChamps() {
        return champs;
    }

    /** @return le motif du refus en clair, ou {@code null} si la modification est appliquée. */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ResultatModificationTournoi[" + motif + (champs.isEmpty() ? "" : ", " + champs) + "]";
    }
}
