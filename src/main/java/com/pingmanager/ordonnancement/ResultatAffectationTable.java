package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;

import java.util.Objects;

/**
 * Résultat détaillé d'une opération sur une table de la salle : envoyer un
 * match sur une table, annuler une affectation faite par erreur, ou constater
 * qu'un match ne peut pas encore être joué.
 *
 * <p>Un refus porte toujours un {@link Motif} précis et, quand c'est utile, le
 * joueur qui bloque et la table où il joue déjà : l'écran peut ainsi expliquer
 * « Prénom NOM joue déjà à la table 4 » plutôt qu'un simple refus.</p>
 */
public final class ResultatAffectationTable {

    /** Issue de l'opération. Les deux premiers motifs sont des réussites. */
    public enum Motif {
        /** Le match est lancé sur la table. */
        AFFECTE,
        /** L'affectation est annulée : la table est libre, le match redevient à jouer. */
        AFFECTATION_ANNULEE,
        /** Le match a été joué : sa table est libérée. */
        TABLE_LIBEREE,
        /** Le numéro de table n'existe pas dans la salle (1 à N). */
        TABLE_HORS_LIMITES,
        /** Un autre match est déjà en cours sur cette table. */
        TABLE_OCCUPEE,
        /** Le match n'appartient à aucun tableau en cours de jeu du tournoi. */
        MATCH_INCONNU,
        /** Le tournoi est annulé : la salle est fermée. */
        TOURNOI_FERME,
        /** Le match est déjà en cours sur une table. */
        MATCH_DEJA_EN_COURS,
        /** Le match est déjà joué. */
        MATCH_TERMINE,
        /** Le match oppose un joueur à un exempt : il n'est pas joué. */
        MATCH_EXEMPT,
        /** Un des deux joueurs n'est pas encore connu (tour précédent non joué). */
        JOUEURS_NON_CONNUS,
        /** Un des deux joueurs joue déjà sur une autre table. */
        JOUEUR_DEJA_EN_JEU,
        /** Annulation ou libération demandée pour un match qui n'est pas en cours. */
        MATCH_PAS_EN_COURS,
        /** Libération demandée alors que le résultat du match n'est pas saisi. */
        RESULTAT_NON_SAISI
    }

    private final Motif motif;
    private final int numeroTable;
    private final Joueur joueurBloquant;
    private final Integer tableDuJoueurBloquant;

    private ResultatAffectationTable(Motif motif, int numeroTable, Joueur joueurBloquant,
                                     Integer tableDuJoueurBloquant) {
        this.motif = Objects.requireNonNull(motif, "motif");
        this.numeroTable = numeroTable;
        this.joueurBloquant = joueurBloquant;
        this.tableDuJoueurBloquant = tableDuJoueurBloquant;
    }

    /** Réussite ou refus simple, sans joueur bloquant. */
    public static ResultatAffectationTable de(Motif motif, int numeroTable) {
        return new ResultatAffectationTable(motif, numeroTable, null, null);
    }

    /** Refus parce qu'un joueur du match joue déjà sur une table. */
    public static ResultatAffectationTable joueurEnJeu(int numeroTable, Joueur joueur, int tableDuJoueur) {
        return new ResultatAffectationTable(Motif.JOUEUR_DEJA_EN_JEU, numeroTable,
                Objects.requireNonNull(joueur, "joueur"), tableDuJoueur);
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return true si l'opération a été faite (affectation, annulation ou libération). */
    public boolean estReussi() {
        return motif == Motif.AFFECTE || motif == Motif.AFFECTATION_ANNULEE || motif == Motif.TABLE_LIBEREE;
    }

    /** @return la table visée par l'opération (0 si aucune table n'est en jeu). */
    public int getNumeroTable() {
        return numeroTable;
    }

    /** @return le joueur déjà en jeu ailleurs (motif {@link Motif#JOUEUR_DEJA_EN_JEU}), sinon null. */
    public Joueur getJoueurBloquant() {
        return joueurBloquant;
    }

    /** @return la table où joue le joueur bloquant, sinon null. */
    public Integer getTableDuJoueurBloquant() {
        return tableDuJoueurBloquant;
    }

    @Override
    public String toString() {
        return motif + (numeroTable > 0 ? " (table " + numeroTable + ")" : "")
                + (joueurBloquant == null ? "" : " : " + joueurBloquant.getNomComplet()
                + " à la table " + tableDuJoueurBloquant);
    }
}
