package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;

/**
 * Logique d'affichage (sans JavaFX) du tournoi <b>préparé hors de PingManager</b>.
 * Décision de Quentin (11 novembre 2026) : trois étapes suffisent pour le rendre
 * opérationnel — déposer le règlement, importer les tableaux et les joueurs, vérifier
 * l'import (dates du tournoi, horaire de chaque tableau). Pas d'homologation ni de
 * publication dans l'application : le club les a faites de son côté.
 */
final class PresentationTournoiImporte {

    static final int NOMBRE_ETAPES = 3;

    private final Tournoi tournoi;

    PresentationTournoiImporte(Tournoi tournoi) {
        this.tournoi = tournoi;
    }

    /** Étape 1 : le règlement (PDF) est déposé. */
    boolean reglementDepose() {
        return tournoi.getReglementDocument() != null
                && tournoi.getReglementDocument().getEmplacement() != null
                && !tournoi.getReglementDocument().getEmplacement().isBlank();
    }

    /** Étape 2 : des tableaux et des joueurs sont importés. */
    boolean importFait() {
        return !tournoi.getTableaux().isEmpty() && RemplissageTableaux.nombreDeParticipants(tournoi) > 0;
    }

    /** Les tableaux encore sans jour ni heure de début. */
    List<String> tableauxSansHoraire() {
        List<String> noms = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (t.getHoraires() == null || t.getHoraires().getDebutMatchs() == null) {
                noms.add(t.getNom());
            }
        }
        return noms;
    }

    /** Les dates du tournoi sont connues. */
    boolean datesRenseignees() {
        return CalendrierTournoi.debut(tournoi) != null;
    }

    /** Étape 3 : l'import est vérifié (dates connues, chaque tableau a son horaire). */
    boolean verificationFaite() {
        return importFait() && datesRenseignees() && tableauxSansHoraire().isEmpty();
    }

    /** Les points à corriger, en phrases courtes, pour l'étape 3. */
    List<String> pointsAVerifier() {
        List<String> l = new ArrayList<>();
        if (!importFait()) {
            l.add("Importez d'abord le classeur des tableaux et des joueurs.");
            return l;
        }
        if (!datesRenseignees()) {
            l.add("Dates du tournoi à renseigner.");
        }
        List<String> sansHoraire = tableauxSansHoraire();
        if (sansHoraire.size() == 1) {
            l.add("Le tableau « " + sansHoraire.get(0) + " » n'a pas d'horaire.");
        } else if (sansHoraire.size() > 1) {
            l.add(sansHoraire.size() + " tableaux sans horaire : " + String.join(", ", sansHoraire) + ".");
        }
        return l;
    }

    /** Nombre d'étapes faites, de 0 à {@link #NOMBRE_ETAPES}. */
    int etapesFaites() {
        return (reglementDepose() ? 1 : 0) + (importFait() ? 1 : 0) + (verificationFaite() ? 1 : 0);
    }

    /** Le tournoi est prêt pour le jour J : les trois étapes sont faites. */
    boolean pret() {
        return etapesFaites() == NOMBRE_ETAPES;
    }

    /** Phrase du bandeau « jour J » : prêt, ou ce qui reste à faire. */
    String phraseJourJ() {
        if (pret()) {
            return "Tout est prêt : pointage, lancement des tableaux et gestion des tables sont disponibles.";
        }
        List<String> reste = new ArrayList<>();
        if (!reglementDepose()) {
            reste.add("déposer le règlement");
        }
        if (!importFait()) {
            reste.add("importer les tableaux et les joueurs");
        } else if (!verificationFaite()) {
            reste.add("vérifier l'import");
        }
        return "Reste à " + String.join(", puis ", reste) + ".";
    }
}
