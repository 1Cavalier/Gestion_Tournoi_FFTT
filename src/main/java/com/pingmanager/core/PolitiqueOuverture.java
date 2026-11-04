package com.pingmanager.core;

import com.pingmanager.model.StatutTournoi;

/**
 * Détermine, selon le {@link StatutTournoi statut d'un tournoi}, si les
 * opérations de déroulé (inscription, pointage, lancement) sont autorisées.
 *
 * <p>Politique partagée par les services du déroulé afin d'employer un seul
 * vocabulaire dans tout le projet.</p>
 */
public enum PolitiqueOuverture {

    /**
     * Mode par défaut, rétrocompatible : les opérations sont autorisées dans tous
     * les statuts <b>sauf</b> {@link StatutTournoi#ANNULE}. Convient au montage et
     * aux tests (tournoi en brouillon), tout en garantissant qu'un tournoi annulé
     * n'accepte plus aucune opération.
     */
    REFUSER_SI_ANNULE,

    /**
     * Mode strict pour les flux publics : les opérations ne sont autorisées que si
     * le tournoi est {@link StatutTournoi#PUBLIE}.
     */
    EXIGER_PUBLIE;

    /** @return true si une opération est permise dans ce statut. */
    public boolean autorise(StatutTournoi statut) {
        return this == EXIGER_PUBLIE ? statut == StatutTournoi.PUBLIE : statut != StatutTournoi.ANNULE;
    }
}
