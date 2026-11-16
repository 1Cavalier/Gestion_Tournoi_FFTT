package com.pingmanager.ui.ecran;

import com.pingmanager.core.PolitiqueOuverture;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;

import java.util.Optional;

/**
 * B049 : partie « Inscriptions » vue par le club (carte de la vue d'ensemble et
 * écran « Inscriptions »). Au statut homologué, elle est en <b>consultation</b> :
 * on voit les inscrits, sans import ni inscription manuelle. La règle vient du
 * cœur ({@link ServiceVerrousHomologation#inscriptionsEnConsultation(Tournoi)}) ;
 * cette classe porte les décisions d'affichage et les textes. Sans JavaFX.
 */
final class PresentationInscriptionsClub {

    /** Libellé du bouton d'import, là où il est proposé. */
    static final String LIBELLE_IMPORT = "Importer des joueurs…";

    /** Rappel affiché quand les inscriptions sont en consultation. */
    static final String RAPPEL_CONSULTATION = "Tournoi homologué : les inscriptions sont en consultation. "
            + "Elles ouvriront à la publication du tournoi.";

    /** Raison posée à côté d'une entrée désactivée avant la publication. */
    static final String RAISON_AVANT_PUBLICATION = PolitiqueOuverture.RAISON_AVANT_PUBLICATION;

    /** Phrase de la carte « Inscriptions » d'un tournoi pas encore publié (aucun compteur). */
    static final String PHRASE_AVANT_PUBLICATION = "Les inscriptions ouvriront à la publication du tournoi.";

    /** Aide de la carte quand l'import est proposé. */
    static final String AIDE_IMPORT = "Tournoi préparé hors de PingManager ? « Importer des joueurs » crée les "
            + "tableaux et les inscrits à partir d'un fichier Excel, LibreOffice, CSV ou JSON.";

    private static final ServiceVerrousHomologation VERROUS = new ServiceVerrousHomologation();

    private PresentationInscriptionsClub() {
    }

    /** @return vrai si le club ne peut que voir les inscrits (tournoi homologué, non publié). */
    static boolean enConsultation(Tournoi tournoi) {
        return VERROUS.inscriptionsEnConsultation(tournoi);
    }

    /**
     * @return vrai si le bouton « Importer des joueurs… » est actif : seulement quand le
     *         tournoi accepte des joueurs (publié, ou tournoi importé), et hors consultation
     */
    static boolean importPropose(Tournoi tournoi) {
        return !enConsultation(tournoi) && PolitiqueOuverture.accepteJoueurs(tournoi);
    }

    /**
     * @return vrai si les entrées d'inscription (« Voir les inscriptions », « Importer
     *         des joueurs », « Inscrire sur place ») sont <b>désactivées</b> avec la raison
     *         {@link #RAISON_AVANT_PUBLICATION} : tournoi pas encore publié (hors tournoi importé)
     */
    static boolean avantPublication(Tournoi tournoi) {
        return PolitiqueOuverture.avantPublication(tournoi.getStatut()) && !PolitiqueOuverture.accepteJoueurs(tournoi);
    }

    /**
     * « Voir les inscriptions » est actif une fois publié ; avant, seulement si un ancien
     * fichier contient déjà des joueurs (consultation, rien n'est supprimé).
     */
    static boolean voirInscriptionsActif(Tournoi tournoi, int participants) {
        return !avantPublication(tournoi) || participants > 0;
    }

    /**
     * @return la phrase sous le titre de la carte : avant la publication, jamais de
     *         compteur (« … ouvriront à la publication »), sinon le remplissage
     */
    static String phraseCarte(Tournoi tournoi, int participants, int inscrits, int places, int attente) {
        if (avantPublication(tournoi)) {
            return participants == 0 ? PHRASE_AVANT_PUBLICATION
                    : participants + (participants > 1 ? " joueurs conservés" : " joueur conservé")
                    + " d'un ancien fichier, en consultation seule. " + PHRASE_AVANT_PUBLICATION;
        }
        if (participants == 0) {
            return tournoi.getStatut() == StatutTournoi.PUBLIE ? "Aucune inscription pour l'instant."
                    : PHRASE_AVANT_PUBLICATION;
        }
        return participants + (participants > 1 ? " joueurs" : " joueur") + " · " + inscrits + " / " + places
                + " places occupées" + (attente > 0 ? " · " + attente + " en liste d'attente" : "");
    }

    /** @return vrai si la jauge de remplissage est affichée (pas avant la publication). */
    static boolean jaugeAffichee(Tournoi tournoi) {
        return !avantPublication(tournoi);
    }

    /**
     * Lien « Tournoi déjà préparé ailleurs ? » de la vue d'ensemble (bascule vers
     * l'import) : tant qu'aucun joueur n'est inscrit, hors demande d'homologation
     * (B048), hors consultation (B049) et avant la publication.
     */
    static boolean basculeImportProposee(Tournoi tournoi, int participants) {
        return participants == 0 && tournoi.getStatut() != StatutTournoi.PUBLIE
                && PresentationDossierFige.basculeImportProposee(tournoi) && !enConsultation(tournoi);
    }

    /** @return le texte sous la jauge de la carte : aide à l'import, ou rappel de consultation. */
    static String aideCarte(Tournoi tournoi) {
        return enConsultation(tournoi) ? RAPPEL_CONSULTATION : AIDE_IMPORT;
    }

    /** @return le rappel à afficher en tête de l'écran « Inscriptions », s'il y en a un. */
    static Optional<String> rappelEcran(Tournoi tournoi) {
        Optional<String> ancienFichier = PolitiqueOuverture.constatJoueursAvantPublication(tournoi);
        if (ancienFichier.isPresent()) {
            return ancienFichier; // B052 : joueurs d'un ancien fichier, conservés et signalés
        }
        return enConsultation(tournoi) ? Optional.of(RAPPEL_CONSULTATION) : Optional.empty();
    }
}
