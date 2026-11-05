package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatPropositionSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Logique d'affichage de la salle (textes, durées, messages de refus), sans
 * JavaFX : testée directement. La vue {@link EcranSalle} ne fait que poser ces
 * textes dans ses composants.
 */
final class PresentationSalle {

    /** Au-delà, la durée d'une partie est signalée (pastille ambre) : à surveiller. */
    static final Duration DUREE_LONGUE = Duration.ofMinutes(40);

    /** Préfixe des données glissées d'un match prêt vers une table. */
    static final String PREFIXE_GLISSER = "pingmanager-match:";

    private PresentationSalle() {
    }

    /** Durée écoulée : « moins d'1 min », « 12 min », « 1 h 05 » ; vide si inconnue. */
    static String duree(Duration d) {
        if (d == null) {
            return "";
        }
        long minutes = Math.max(0, d.toMinutes());
        if (minutes < 1) {
            return "moins d'1 min";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + " h " + String.format("%02d", minutes % 60);
    }

    /** La partie dure-t-elle anormalement longtemps ? */
    static boolean estLongue(Duration d) {
        return d != null && d.compareTo(DUREE_LONGUE) >= 0;
    }

    /** « Prénom NOM » ou « À déterminer ». */
    static String nom(Joueur j) {
        return j == null || j.estADeterminer() ? "À déterminer" : j.getNomComplet();
    }

    /** Les deux adversaires : « Prénom NOM – Prénom NOM ». */
    static String joueurs(Match m) {
        return nom(m.getJoueurA()) + "  –  " + nom(m.getJoueurB());
    }

    /** Tableau et tour : « C – Toutes séries · Poule B · tour 2 ». */
    static String contexte(TableauDeBordSalle.MatchSalle m) {
        return m.tableau() + " · " + m.tour();
    }

    /** Pastille d'une table : « Libre » ou « En cours ». */
    static String etat(TableauDeBordSalle.CarteTable c) {
        return c.estLibre() ? "Libre" : "En cours";
    }

    /**
     * Numéro de table d'un match en cours, pour l'écran des poules et l'arbre :
     * « Table 4 » ; vide si le match n'est pas sur une table.
     */
    static String libelleTable(Match m) {
        if (m == null || m.getStatut() != StatutMatch.EN_COURS || m.getNumeroTable() == null) {
            return "";
        }
        return "Table " + m.getNumeroTable();
    }

    /** Motif d'un match bloqué : « Prénom NOM déjà en jeu à la table 4 ». */
    static String motifBlocage(TableauDeBordSalle.MatchBloque b) {
        ResultatAffectationTable r = b.motif();
        if (r.getMotif() == ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU) {
            return nom(r.getJoueurBloquant()) + " déjà en jeu à la table " + r.getTableDuJoueurBloquant();
        }
        return message(r, 0);
    }

    /**
     * Message d'une opération sur une table, réussie ou refusée, qui dit
     * pourquoi et quoi faire.
     *
     * @param nombreDeTables nombre de tables de la salle (pour « hors limites »)
     */
    static String message(ResultatAffectationTable r, int nombreDeTables) {
        ResultatAffectationTable.Motif motif = r.getMotif();
        int table = r.getNumeroTable();
        if (motif == ResultatAffectationTable.Motif.AFFECTE) {
            return "Match envoyé à la table " + table + ".";
        }
        if (motif == ResultatAffectationTable.Motif.AFFECTATION_ANNULEE) {
            return table > 0
                    ? "Affectation annulée : la table " + table + " est libre, le match revient dans les matchs prêts."
                    : "Affectation annulée : le match revient dans les matchs prêts.";
        }
        if (motif == ResultatAffectationTable.Motif.TABLE_LIBEREE) {
            return table > 0 ? "Résultat enregistré : la table " + table + " est libre." : "Résultat enregistré.";
        }
        if (motif == ResultatAffectationTable.Motif.TABLE_HORS_LIMITES) {
            return nombreDeTables > 0
                    ? "La table " + table + " n'existe pas : la salle compte les tables 1 à " + nombreDeTables + "."
                    : "Le nombre de tables du tournoi n'est pas renseigné (Informations sportives et techniques).";
        }
        if (motif == ResultatAffectationTable.Motif.TABLE_OCCUPEE) {
            return "La table " + table + " est occupée : terminez ou annulez d'abord le match en cours.";
        }
        if (motif == ResultatAffectationTable.Motif.MATCH_INCONNU) {
            return "Ce match n'appartient à aucun tableau en cours de jeu.";
        }
        if (motif == ResultatAffectationTable.Motif.TOURNOI_FERME) {
            return "Le tournoi est annulé : la salle est fermée.";
        }
        if (motif == ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS) {
            return table > 0 ? "Ce match est déjà en cours à la table " + table + "." : "Ce match est déjà en cours.";
        }
        if (motif == ResultatAffectationTable.Motif.MATCH_TERMINE) {
            return "Ce match est déjà joué.";
        }
        if (motif == ResultatAffectationTable.Motif.MATCH_EXEMPT) {
            return "Ce joueur est exempt : il n'y a pas de match à jouer.";
        }
        if (motif == ResultatAffectationTable.Motif.JOUEURS_NON_CONNUS) {
            return "Les deux joueurs ne sont pas encore connus : le tour précédent n'est pas joué.";
        }
        if (motif == ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU) {
            return nom(r.getJoueurBloquant()) + " joue déjà à la table " + r.getTableDuJoueurBloquant()
                    + " : un joueur ne peut pas être sur deux tables. Attendez la fin de son match.";
        }
        if (motif == ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS) {
            return "Ce match n'est pas en cours sur une table.";
        }
        if (motif == ResultatAffectationTable.Motif.RESULTAT_NON_SAISI) {
            return "Saisissez d'abord le résultat : la table se libère à l'enregistrement du score.";
        }
        return motif.name();
    }

    /** Compte rendu du bouton « Proposer ». */
    static String messageProposition(ResultatPropositionSalle r) {
        ResultatPropositionSalle.Motif motif = r.getMotif();
        if (motif == ResultatPropositionSalle.Motif.LANCES) {
            List<String> tables = new ArrayList<>();
            for (ResultatPropositionSalle.Lancement l : r.getLancements()) {
                tables.add(String.valueOf(l.table()));
            }
            int n = tables.size();
            return (n > 1 ? n + " matchs envoyés aux tables " : "1 match envoyé à la table ")
                    + listeLisible(tables) + ".";
        }
        if (motif == ResultatPropositionSalle.Motif.AUCUNE_TABLE_LIBRE) {
            return "Toutes les tables sont occupées : terminez un match pour libérer une table.";
        }
        if (motif == ResultatPropositionSalle.Motif.AUCUN_MATCH_PRET) {
            return "Aucun match prêt : les joueurs attendus sont déjà en jeu, ou il ne reste rien à jouer.";
        }
        if (motif == ResultatPropositionSalle.Motif.SALLE_SANS_TABLE) {
            return "Le nombre de tables du tournoi n'est pas renseigné (Informations sportives et techniques).";
        }
        return "Le tournoi est annulé : la salle est fermée.";
    }

    /** « 1, 2 et 4 ». */
    static String listeLisible(List<String> elements) {
        if (elements.isEmpty()) {
            return "";
        }
        if (elements.size() == 1) {
            return elements.get(0);
        }
        return String.join(", ", elements.subList(0, elements.size() - 1)) + " et " + elements.get(elements.size() - 1);
    }

    /** Résumé pour la page Jour J : « 3 tables occupées sur 16 · 5 matchs prêts ». */
    static String resume(TableauDeBordSalle bord) {
        if (bord.getNombreDeTables() == 0) {
            return "Nombre de tables non renseigné.";
        }
        int occupees = bord.nombreTablesOccupees();
        int prets = bord.getPrets().size();
        return occupees + (occupees > 1 ? " tables occupées" : " table occupée") + " sur " + bord.getNombreDeTables()
                + " · " + prets + (prets > 1 ? " matchs prêts" : " match prêt");
    }

    /** Clé glissée-déposée d'un match prêt (son rang dans la liste des prêts). */
    static String cleGlisser(int rang) {
        return PREFIXE_GLISSER + rang;
    }

    /** Rang lu dans une clé glissée, ou -1 si la donnée ne vient pas de la salle. */
    static int lireCleGlisser(String cle) {
        if (cle == null || !cle.startsWith(PREFIXE_GLISSER)) {
            return -1;
        }
        try {
            return Integer.parseInt(cle.substring(PREFIXE_GLISSER.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * La salle est-elle proposée sur la page Jour J ? Dès qu'un tableau est lancé
     * (poules ou tableau final), tant que le tournoi n'est pas annulé.
     */
    static boolean salleOuverte(Tournoi tournoi) {
        if (tournoi.getStatut() == StatutTournoi.ANNULE) {
            return false;
        }
        for (Tableau t : tournoi.getTableaux()) {
            if (t.getStatut() == StatutTableau.LANCE || t.getStatut() == StatutTableau.PHASE_FINALE) {
                return true;
            }
        }
        return false;
    }
}
