package com.pingmanager.core;

/**
 * Levée quand le compte connecté n'a pas le droit d'effectuer une
 * {@link OperationProtegee} (point 16 du README). Son message est prêt à afficher.
 */
public final class DroitRefuseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Motif précis du refus. */
    public enum Motif {
        /** Personne n'est connecté. */
        NON_CONNECTE,
        /** L'email du compte n'est pas vérifié (point 13). */
        EMAIL_NON_VERIFIE,
        /** Le tournoi ne porte pas de numéro de club : aucun club ne peut le revendiquer. */
        CLUB_NON_DESIGNE,
        /** Le compte n'a pas d'accès sur ce club. */
        AUCUN_DROIT_SUR_LE_CLUB,
        /** Réservé au compte de supervision (mode démo). */
        SUPERVISION_REQUISE
    }

    private final Motif motif;
    private final OperationProtegee operation;
    private final String numeroClub;

    public DroitRefuseException(Motif motif, OperationProtegee operation, String numeroClub) {
        super(message(motif, operation, numeroClub));
        this.motif = motif;
        this.operation = operation;
        this.numeroClub = numeroClub;
    }

    private static String message(Motif motif, OperationProtegee operation, String numeroClub) {
        String debut = "Action refusée : vous n'avez pas le droit de " + operation.getLibelle();
        String m = motif.name();
        if ("NON_CONNECTE".equals(m)) {
            return debut + " sans être connecté.";
        }
        if ("EMAIL_NON_VERIFIE".equals(m)) {
            return debut + " tant que votre email n'est pas vérifié (Mon profil).";
        }
        if ("CLUB_NON_DESIGNE".equals(m)) {
            return debut + " : aucun numéro de club n'y est indiqué.";
        }
        if ("AUCUN_DROIT_SUR_LE_CLUB".equals(m)) {
            return debut + " : votre compte n'est pas rattaché au club n° " + numeroClub + ".";
        }
        return debut + " : réservé à la supervision.";
    }

    public Motif getMotif() {
        return motif;
    }

    public OperationProtegee getOperation() {
        return operation;
    }

    /** @return le numéro du club visé, ou {@code null}. */
    public String getNumeroClub() {
        return numeroClub;
    }
}
