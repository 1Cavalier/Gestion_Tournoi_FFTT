package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeLancements;
import com.pingmanager.core.ReglePrioriteSalle;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mots et filtres de l'interface « Lancer un match » (B055,
 * {@link InterfaceLancementMatch}), sans JavaFX : ils se testent directement.
 * Les règles (joueurs disponibles, table libre) restent dans le cœur
 * ({@link com.pingmanager.core.ServiceSalle}) ; ici, on ne fait que les dire.
 */
final class PresentationLancementMatch {

    /** Choix « tous les tableaux » du filtre du lancement manuel. */
    static final String TOUS_LES_TABLEAUX = "Tous les tableaux";

    private PresentationLancementMatch() {
    }

    /**
     * Raison courte, en clair, pour laquelle un match ne peut pas partir :
     * « Prénom NOM joue en table 4 », « Aucune table libre »…
     */
    static String motif(ResultatAffectationTable r, int nombreDeTables) {
        if (r == null) {
            return "";
        }
        ResultatAffectationTable.Motif m = r.getMotif();
        if (m == ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU) {
            String nom = PresentationSalle.nom(r.getJoueurBloquant());
            return r.getTableDuJoueurBloquant() == null ? nom + " est déjà en jeu (table inconnue)"
                    : nom + " joue en table " + r.getTableDuJoueurBloquant();
        }
        if (m == ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE) {
            return "Aucune table libre";
        }
        if (m == ResultatAffectationTable.Motif.TABLE_RESERVEE) {
            return "Tables libres réservées à d'autres tableaux";
        }
        if (m == ResultatAffectationTable.Motif.TOURNOI_FERME) {
            return "Tournoi annulé : salle fermée";
        }
        if (m == ResultatAffectationTable.Motif.TABLE_HORS_LIMITES && nombreDeTables == 0) {
            return "Nombre de tables non renseigné";
        }
        if (m == ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS) {
            return "Déjà en cours";
        }
        return PresentationSalle.message(r, nombreDeTables);
    }

    /** Libellé du bouton d'envoi : « Lancer · table 3 », ou « Lancer » sans table. */
    static String libelleLancer(Integer table) {
        return table == null ? "Lancer" : "Lancer · table " + table;
    }

    /** « Table 3 » pour la liste des tables possibles. */
    static String libelleTable(int table) {
        return "Table " + table;
    }

    /** Attente des joueurs : « Attend depuis 12 min », vide si inconnue. */
    static String attente(Duration d) {
        return d == null ? "" : "Attend depuis " + PresentationSalle.duree(d);
    }

    /** L'attente est-elle longue (rouge) ? Même seuil que la couleur des matchs proposés. */
    static boolean attenteLongue(Duration d) {
        return d != null && d.compareTo(ReglePrioriteSalle.ATTENTE_LONGUE) >= 0;
    }

    /** Résumé en tête : « 4 matchs prêts · 2 tables libres », « Aucune table libre ». */
    static String resume(ListeLancements liste) {
        int prets = liste.nombrePrets();
        int libres = liste.getTablesLibres().size();
        String tables = libres == 0 ? "aucune table libre"
                : libres + (libres > 1 ? " tables libres" : " table libre");
        String matchs = prets == 0 ? "aucun match prêt" : prets + (prets > 1 ? " matchs prêts" : " match prêt");
        String s = matchs + " · " + tables;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Noms des tableaux présents dans la liste, dans l'ordre du tournoi, précédés de « Tous les tableaux ». */
    static List<String> tableaux(ListeLancements liste) {
        Set<String> noms = new LinkedHashSet<>();
        noms.add(TOUS_LES_TABLEAUX);
        for (ListeLancements.Ligne l : liste.getParTableau()) {
            noms.add(l.match().tableau());
        }
        return new ArrayList<>(noms);
    }

    /**
     * Lancement manuel : lignes dans l'ordre du tournoi, filtrées par tableau
     * (null ou « Tous les tableaux » : aucun filtre) et par texte (nom ou
     * prénom d'un joueur, tableau ou tour ; sans accents ni majuscules).
     */
    static List<ListeLancements.Ligne> filtrer(ListeLancements liste, String recherche, String tableau) {
        String filtre = PresentationSalle.normaliser(recherche);
        boolean tous = tableau == null || TOUS_LES_TABLEAUX.equals(tableau);
        List<ListeLancements.Ligne> res = new ArrayList<>();
        for (ListeLancements.Ligne l : liste.getParTableau()) {
            if (!tous && !tableau.equals(l.match().tableau())) {
                continue;
            }
            if (!filtre.isEmpty() && !PresentationSalle.normaliser(PresentationSalle.joueurs(l.match().match())
                    + " " + PresentationSalle.contexte(l.match())).contains(filtre)) {
                continue;
            }
            res.add(l);
        }
        return res;
    }

    /**
     * Table retenue pour une ligne : celle choisie par le juge-arbitre si elle
     * est toujours possible, sinon la table proposée par le cœur (null si refus).
     */
    static Integer tableRetenue(ListeLancements.Ligne l, Integer choix) {
        if (choix != null && l.tablesPossibles().contains(choix)) {
            return choix;
        }
        return l.tableProposee();
    }

    /** Message vide de la liste des matchs proposés. */
    static String videProposes() {
        return "Aucun match à proposer. Les parties des poules lancées et du tableau final apparaissent ici dès "
                + "que leurs deux joueurs sont connus.";
    }

    /** Message vide du lancement manuel, selon la recherche. */
    static String videManuel(String recherche) {
        return recherche == null || recherche.isBlank()
                ? "Aucun match à jouer dont les deux joueurs sont connus."
                : "Aucun match ne correspond à « " + recherche.trim() + " ».";
    }
}
