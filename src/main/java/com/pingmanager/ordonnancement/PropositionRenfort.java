package com.pingmanager.ordonnancement;

/**
 * Ce que l'application propose au juge-arbitre pour les tables de renfort
 * (B054). Il l'accepte ou la refuse ({@link PiloteRenfort}).
 *
 * @param action         ouvrir, rendre, ou rien à faire
 * @param tablesCible    tables de renfort en service si la proposition est acceptée
 * @param tablesEnService tables de renfort en service au moment de la proposition
 * @param retardPrevu    retard prévu sans renfort, en minutes (négatif : en avance)
 * @param motif          pourquoi, en clair
 */
public record PropositionRenfort(Action action, int tablesCible, int tablesEnService, long retardPrevu,
                                 String motif) {

    /** Ce que la proposition demande. */
    public enum Action {
        /** Rien à changer. */
        AUCUNE,
        /** Ouvrir des tables d'échauffement comme tables de match (ou en ouvrir davantage). */
        DEPLOYER,
        /** Rendre les tables de renfort à l'échauffement. */
        RETIRER
    }

    /** @return vrai si la proposition demande un changement. */
    public boolean demandeUnChangement() {
        return action != Action.AUCUNE;
    }
}
