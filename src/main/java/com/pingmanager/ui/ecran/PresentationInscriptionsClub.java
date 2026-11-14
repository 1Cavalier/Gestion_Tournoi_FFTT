package com.pingmanager.ui.ecran;

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

    /** @return vrai si le bouton « Importer des joueurs… » est proposé. */
    static boolean importPropose(Tournoi tournoi) {
        return !enConsultation(tournoi);
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
        return enConsultation(tournoi) ? Optional.of(RAPPEL_CONSULTATION) : Optional.empty();
    }
}
