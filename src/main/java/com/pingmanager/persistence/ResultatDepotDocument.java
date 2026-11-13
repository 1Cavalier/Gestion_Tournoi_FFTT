package com.pingmanager.persistence;

import com.pingmanager.model.DocumentReference;

import java.util.Objects;

/**
 * Issue du dépôt d'un document (PDF du règlement) dans le dossier de données :
 * la référence relative créée, ou le motif précis du refus.
 */
public final class ResultatDepotDocument {

    /** Motif du refus (ou {@code DEPOSE}). */
    public enum Motif {
        DEPOSE,
        FICHIER_INTROUVABLE,
        PAS_UN_PDF,
        /** Image promotionnelle : ni PNG ni JPEG (signature du contenu). */
        PAS_UNE_IMAGE,
        TROP_VOLUMINEUX,
        ECHEC_COPIE
    }

    private final Motif motif;
    private final DocumentReference reference;
    private final String message;

    private ResultatDepotDocument(Motif motif, DocumentReference reference, String message) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.reference = reference;
        this.message = message;
    }

    static ResultatDepotDocument depose(DocumentReference reference) {
        return new ResultatDepotDocument(Motif.DEPOSE, Objects.requireNonNull(reference, "reference"), null);
    }

    static ResultatDepotDocument refuse(Motif motif, String message) {
        if (motif == Motif.DEPOSE) {
            throw new IllegalArgumentException("Un refus a un motif de refus.");
        }
        return new ResultatDepotDocument(motif, null, message);
    }

    public boolean estDepose() {
        return motif == Motif.DEPOSE;
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return la référence relative au dossier de données, ou {@code null} si refusé */
    public DocumentReference getReference() {
        return reference;
    }

    /** @return le message destiné à l'utilisateur, ou {@code null} si déposé */
    public String getMessage() {
        return message;
    }
}
