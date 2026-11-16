package com.pingmanager.core;

import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;

/**
 * Détermine, selon le {@link StatutTournoi statut d'un tournoi}, si les
 * opérations de déroulé (inscription, pointage, lancement) sont autorisées.
 *
 * <p>Politique partagée par les services du déroulé afin d'employer un seul
 * vocabulaire dans tout le projet.</p>
 *
 * <p><b>Inscriptions</b> : tant qu'un tournoi n'est pas publié, ses inscriptions ne
 * sont pas ouvertes. Inscription (en ligne, manuelle, sur place) et import de
 * joueurs appliquent donc {@link #EXIGER_PUBLIE} ({@link #INSCRIPTIONS}) : un
 * tournoi en brouillon, en demande d'homologation ou homologué n'a aucun joueur.
 * Le pointage et le lancement gardent {@link #REFUSER_SI_ANNULE}.</p>
 */
public enum PolitiqueOuverture {

    /**
     * Mode par défaut du pointage, du lancement et de la salle : les opérations
     * sont autorisées dans tous les statuts <b>sauf</b> {@link StatutTournoi#ANNULE}.
     * Ne convient plus aux inscriptions (voir {@link #INSCRIPTIONS}).
     */
    REFUSER_SI_ANNULE,

    /**
     * Mode strict : les opérations ne sont autorisées que si le tournoi est
     * {@link StatutTournoi#PUBLIE}. Mode par défaut de toutes les inscriptions et
     * de l'import de joueurs.
     */
    EXIGER_PUBLIE;

    /** Politique des inscriptions et de l'import de joueurs : il faut un tournoi publié. */
    public static final PolitiqueOuverture INSCRIPTIONS = EXIGER_PUBLIE;

    /** Raison affichée à côté des entrées d'inscription tant que le tournoi n'est pas publié. */
    public static final String RAISON_AVANT_PUBLICATION = "Disponible après la publication";

    /** Motif complet d'un refus d'inscription ou d'import avant la publication. */
    public static final String MOTIF_AVANT_PUBLICATION =
            "Les inscriptions ne sont pas encore ouvertes : elles ouvrent à la publication du tournoi.";

    /**
     * @return vrai si le tournoi, dans ce statut, accepte des joueurs (inscription
     *         ou import) : seulement une fois publié
     */
    public static boolean inscriptionsOuvertes(StatutTournoi statut) {
        return INSCRIPTIONS.autorise(statut);
    }

    /**
     * @return vrai si le statut précède la publication (brouillon, demande
     *         d'homologation, homologué) : inscriptions pas encore ouvertes
     */
    public static boolean avantPublication(StatutTournoi statut) {
        return statut == StatutTournoi.BROUILLON || statut == StatutTournoi.DEMANDE_HOMOLOGATION
                || statut == StatutTournoi.HOMOLOGUE;
    }

    /**
     * Comme {@link #autorise(StatutTournoi)}, pour un tournoi. Exception laissée en
     * l'état, en attente d'une décision de Quentin (README section 6) : un tournoi
     * <b>importé</b> ({@link ModePreparation#IMPORT}, préparé hors de PingManager)
     * reçoit ses joueurs dès le brouillon, même en mode {@link #EXIGER_PUBLIE}.
     *
     * @return true si une opération est permise pour ce tournoi
     */
    public boolean autorise(Tournoi tournoi) {
        StatutTournoi statut = tournoi.getStatut();
        if (this == EXIGER_PUBLIE && tournoi.getModePreparation() == ModePreparation.IMPORT) {
            return statut != StatutTournoi.ANNULE;
        }
        return autorise(statut);
    }

    /**
     * @return vrai si ce tournoi accepte des joueurs (inscription ou import) :
     *         publié, ou tournoi importé non annulé
     */
    public static boolean accepteJoueurs(Tournoi tournoi) {
        return INSCRIPTIONS.autorise(tournoi);
    }

    /**
     * Fichier antérieur à la règle : un tournoi non publié (hors tournoi importé) qui
     * contient déjà des joueurs. Ils sont conservés, rien n'est supprimé ; on le signale.
     *
     * @return le constat à afficher à l'ouverture, ou vide si le tournoi est cohérent
     */
    public static java.util.Optional<String> constatJoueursAvantPublication(Tournoi tournoi) {
        if (!avantPublication(tournoi.getStatut()) || accepteJoueurs(tournoi)) {
            return java.util.Optional.empty();
        }
        int n = RemplissageTableaux.nombreDeParticipants(tournoi);
        if (n == 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of("Ce tournoi n'est pas encore publié, mais son fichier contient " + n
                + (n > 1 ? " joueurs enregistrés" : " joueur enregistré")
                + " avant l'entrée en vigueur de la règle « inscriptions à la publication ». "
                + (n > 1 ? "Ils sont conservés" : "Il est conservé")
                + " ; aucune nouvelle inscription ni aucun import n'est possible avant la publication.");
    }

    /** @return true si une opération est permise dans ce statut. */
    public boolean autorise(StatutTournoi statut) {
        return this == EXIGER_PUBLIE ? statut == StatutTournoi.PUBLIE : statut != StatutTournoi.ANNULE;
    }
}
