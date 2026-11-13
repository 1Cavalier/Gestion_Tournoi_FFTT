package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceTournoiEnCours;
import com.pingmanager.model.Tableau;

/**
 * B043 : textes des pages du groupe « Tournoi en cours », sans JavaFX (testés).
 * Pas de {@code switch} sur une énumération : des chaînes de {@code if}.
 */
final class PresentationTournoiEnCours {

    static final String GROUPE = "Tournoi en cours";
    static final String TITRE_TABLEAUX = "Tableaux du tournoi";
    static final String AUCUN = "Aucun tournoi de votre club n'est en cours.";
    static final String RETOUR_FICHE = "← Fiche du tournoi";
    static final String RETOUR_JOUR_J = "← Gestion du jour J";

    private PresentationTournoiEnCours() {
    }

    /** Statut court d'un tableau : Inscriptions, Pointage, Prêt à lancer, Poules, Tableau final ou Terminé. */
    static String statut(Tableau t) {
        if (ServiceTournoiEnCours.estTermine(t)) {
            return "Terminé";
        }
        PresentationJourJ.Etape e = PresentationJourJ.etape(t);
        if (e == PresentationJourJ.Etape.POINTAGE_OUVERT) {
            return "Pointage";
        }
        if (e == PresentationJourJ.Etape.POINTAGE_CLOTURE) {
            return "Tirage à faire";
        }
        if (e == PresentationJourJ.Etape.POULES || e == PresentationJourJ.Etape.POULES_TERMINEES) {
            return "Poules";
        }
        if (e == PresentationJourJ.Etape.PHASE_FINALE) {
            return "Tableau final";
        }
        return "Inscriptions";
    }

    /** Couleur de la pastille de statut. */
    static StatutPastille pastille(Tableau t) {
        if (ServiceTournoiEnCours.estTermine(t)) {
            return StatutPastille.NEUTRE;
        }
        PresentationJourJ.Etape e = PresentationJourJ.etape(t);
        if (e == PresentationJourJ.Etape.INSCRIPTION) {
            return StatutPastille.ATTENTION;
        }
        if (e == PresentationJourJ.Etape.POINTAGE_OUVERT || e == PresentationJourJ.Etape.POINTAGE_CLOTURE) {
            return StatutPastille.INFO;
        }
        return StatutPastille.SUCCES;
    }
}
