package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.OperationPointage;

import java.util.Objects;

/**
 * Compte rendu d'une action faite sur un poste de pointage (B041), produit par
 * {@link ServicePostePointage}. Objet valeur immuable.
 *
 * <ul>
 *   <li>acceptée : {@link #getOperation()} est l'opération ajoutée au journal
 *       (ou {@code null} si l'état était déjà celui demandé : rien n'a changé) ;
 *       pour un désistement, {@link #getRepeche()} est le joueur repêché de la
 *       liste d'attente, s'il y en a un ;</li>
 *   <li>refusée : {@link #getMotif()} dit pourquoi, et rien n'a été modifié.</li>
 * </ul>
 */
public final class ResultatActionPointage {

    /** Motif précis d'un refus. */
    public enum Motif {
        /** Le tournoi n'accepte plus d'opération (annulé…). */
        TOURNOI_FERME("Le tournoi n'accepte plus de pointage."),
        /** Aucun tableau de ce nom dans le tournoi. */
        TABLEAU_INCONNU("Ce tableau n'existe pas dans le tournoi."),
        /** Le joueur n'est (plus) inscrit à ce tableau : désinscrit ailleurs, ou en liste d'attente. */
        JOUEUR_NON_INSCRIT("Ce joueur n'est pas (ou plus) inscrit à ce tableau."),
        /** Le tableau est lancé : la liste des joueurs ne bouge plus depuis le poste de pointage. */
        TABLEAU_LANCE("Le tableau est lancé : voir le juge-arbitre."),
        /** Le pointage du tableau est clôturé : seul le juge-arbitre peut encore retirer un joueur. */
        POINTAGE_CLOTURE("Le pointage de ce tableau est clôturé : voir le juge-arbitre."),
        /** Désistement sans motif. */
        MOTIF_OBLIGATOIRE("Indiquer le motif du désistement."),
        /** Paiement sans mode de règlement. */
        MODE_OBLIGATOIRE("Indiquer le mode de règlement.");

        private final String message;

        Motif(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Motif motif;
    private final OperationPointage operation;
    private final Joueur repeche;

    private ResultatActionPointage(Motif motif, OperationPointage operation, Joueur repeche) {
        this.motif = motif;
        this.operation = operation;
        this.repeche = repeche;
    }

    static ResultatActionPointage acceptee(OperationPointage operation, Joueur repeche) {
        return new ResultatActionPointage(null, operation, repeche);
    }

    static ResultatActionPointage sansChangement() {
        return new ResultatActionPointage(null, null, null);
    }

    static ResultatActionPointage refusee(Motif motif) {
        return new ResultatActionPointage(Objects.requireNonNull(motif, "motif"), null, null);
    }

    public boolean estAcceptee() {
        return motif == null;
    }

    /** @return vrai si l'action a modifié le tournoi (et doit donc être enregistrée). */
    public boolean aModifie() {
        return operation != null;
    }

    /** @return le motif du refus, ou {@code null} si l'action est acceptée. */
    public Motif getMotif() {
        return motif;
    }

    /** @return l'opération ajoutée au journal, ou {@code null}. */
    public OperationPointage getOperation() {
        return operation;
    }

    /** @return le joueur repêché après un désistement, ou {@code null}. */
    public Joueur getRepeche() {
        return repeche;
    }

    @Override
    public String toString() {
        return estAcceptee() ? "ResultatActionPointage[accepté, " + operation + "]"
                : "ResultatActionPointage[refusé : " + motif + "]";
    }
}
