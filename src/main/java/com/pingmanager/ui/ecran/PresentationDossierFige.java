package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatModificationTournoi;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.Tournoi;

import java.util.Objects;

/**
 * B048 : pendant la demande d'homologation, les quatre fiches du dossier
 * (Général, tableaux, sportif, complémentaire) s'ouvrent en <b>consultation</b>.
 * Cette classe porte les libellés et les décisions d'affichage ; la règle
 * elle-même vient du cœur ({@link ServiceVerrousHomologation#dossierFige(Tournoi)}).
 * Sans JavaFX : les écrans ne font qu'appliquer ce qu'elle décide.
 */
final class PresentationDossierFige {

    /** Titre du bandeau affiché en tête de chaque fiche. */
    static final String TITRE_BANDEAU = "Demande en cours : dossier figé";

    /**
     * Classe de style posée sur les boutons qui restent utilisables en
     * consultation (retour, navigation, aperçus, exports PDF).
     */
    static final String CLASSE_ACTION_CONSULTATION = "action-consultation";

    /** Ce qu'une fiche figée fait d'un contrôle. */
    enum Traitement {
        /** Laissé tel quel (texte, mise en page, action de consultation). */
        LAISSER,
        /** Champ de texte : lisible et sélectionnable, non modifiable. */
        LECTURE_SEULE,
        /** Choix (liste, date, case, bouton radio, segment) : désactivé, valeur visible. */
        DESACTIVER,
        /** Action de modification (enregistrer, ajouter, retirer, déposer) : retirée de l'écran. */
        MASQUER
    }

    /** Nature d'un contrôle, vue par l'écran. */
    enum Nature {
        SAISIE_TEXTE,
        CHOIX,
        ACTION,
        AUTRE
    }

    private static final ServiceVerrousHomologation VERROUS = new ServiceVerrousHomologation();

    private PresentationDossierFige() {
    }

    /** @return vrai si les fiches de ce tournoi s'ouvrent en consultation. */
    static boolean estFige(Tournoi tournoi) {
        return VERROUS.dossierFige(tournoi);
    }

    /** @return le texte complet du bandeau d'une fiche figée. */
    static String bandeau() {
        return TITRE_BANDEAU + ". Cette fiche est en consultation : rien ne peut être modifié, ajouté, "
                + "retiré ni déposé. Les aperçus et les exports PDF restent disponibles. Enregistrez la "
                + "réponse à la demande depuis la vue d'ensemble : un refus rend le dossier de nouveau "
                + "modifiable.";
    }

    /**
     * @param actionConsultation vrai si le contrôle porte {@link #CLASSE_ACTION_CONSULTATION}
     * @return ce qu'une fiche figée fait d'un contrôle de cette nature
     */
    static Traitement traitement(Nature nature, boolean actionConsultation) {
        Objects.requireNonNull(nature, "nature");
        // Pas de switch sur l'énumération (classe annexe générée par javac).
        if (actionConsultation || nature == Nature.AUTRE) {
            return Traitement.LAISSER;
        }
        if (nature == Nature.SAISIE_TEXTE) {
            return Traitement.LECTURE_SEULE;
        }
        if (nature == Nature.CHOIX) {
            return Traitement.DESACTIVER;
        }
        return Traitement.MASQUER;
    }

    /** @return le libellé du bouton d'accès d'un bloc de la vue d'ensemble. */
    static String libelleOuvrirSection(Tournoi tournoi, int index) {
        if (index == PresentationPreparationTournoi.SECTION_TABLEAUX) {
            return estFige(tournoi) ? "Voir les tableaux" : "Gérer les tableaux";
        }
        return estFige(tournoi) ? "Consulter" : "Ouvrir";
    }

    /** @return le libellé du bouton qui quitte un formulaire (rien à annuler en consultation). */
    static String libelleQuitter(Tournoi tournoi) {
        return estFige(tournoi) ? "Retour" : "Annuler";
    }

    /**
     * Bascule vers l'import (« tournoi préparé ailleurs ») : c'est une modification
     * du dossier, elle n'est donc pas proposée pendant la demande.
     */
    static boolean basculeImportProposee(Tournoi tournoi) {
        return !estFige(tournoi);
    }

    /** @return le motif à afficher si l'enregistrement d'une fiche a été refusé, sinon {@code null}. */
    static String messageRefus(ResultatModificationTournoi r) {
        return r.estAppliquee() ? null : r.getMessage();
    }
}
