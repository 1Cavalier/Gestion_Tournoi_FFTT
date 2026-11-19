package com.pingmanager.ui.ecran;

/**
 * Tous les textes affichés par l'interface « Lancer un match » du jour J
 * (B057, {@link PanneauLancementMatch}), réunis ici pour être relus au même
 * endroit. Phrases courtes, dans les mots d'un juge-arbitre. Les motifs de
 * refus (« Paul MARTIN joue en table 4 ») sont dans
 * {@link PresentationLancementMatch#motif}.
 */
final class TextesLancementMatch {

    private TextesLancementMatch() {
    }

    // --- En-tête ---
    static final String RETOUR = "← Retour jour J";
    static final String TITRE = "Lancer un match";

    // --- Bandeau de proposition ---
    static final String PROPOSER = "Proposer un match";
    static final String LANCER = "Lancer";
    static final String AUTRE = "Autre";
    static final String ZONE_VIDE = "Appuyez sur « Proposer un match » : le match le plus urgent s'affiche ici.";
    static final String AUCUN_MATCH = "Aucun match à lancer pour l'instant";
    static final String AUCUNE_TABLE = "Aucune table libre";
    static final String JOUEUR_1 = "Joueur 1";
    static final String JOUEUR_2 = "Joueur 2";
    static final String TABLEAU = "Tableau";
    static final String TABLE = "Table";
    static final String AUTRE_AIDE = "Proposition suivante";
    static final String PROPOSITION_RANG = "Proposition %d sur %d";

    // --- Liste des matchs à lancer ---
    static final String LISTE_TITRE = "Matchs à lancer";
    static final String LISTE_AIDE = "Du plus urgent au moins urgent, selon la stratégie en place. Seuls les "
            + "matchs dont les deux joueurs sont libres sont listés.";
    static final String COLONNE_NUMERO = "N°";
    static final String COLONNE_LANCER = "Lancer";
    static final String VOIR_PLUS = "Voir plus";
    static final String LISTE_VIDE = "Aucun match à lancer pour l'instant. Les matchs apparaissent ici dès que "
            + "leurs deux joueurs sont connus et libres.";

    // --- Après un lancement ---
    /** « MARTIN – DUPONT lancé en table 5 ». */
    static String lance(String nomA, String nomB, int table) {
        return nomA + " – " + nomB + " lancé en table " + table;
    }

    static final String REFUS = "Match non lancé : %s.";

    // --- Feuille de match ---
    static final String FEUILLE_A_L_ENVOI = "Imprimer la feuille de match à chaque lancement";
    static final String FEUILLE_OUI = "La feuille de match sera imprimée à chaque lancement.";
    static final String FEUILLE_NON = "Les feuilles ne s'impriment plus au lancement (bouton « Feuille » sur "
            + "chaque table).";

    // --- Vue « Voir plus » (lecture seule) ---
    static final String FERMER = "Fermer";
    static final String VUE_JOUEURS = "Joueurs";
    static final String VUE_MATCHS = "Matchs";
    static final String VUE_CLASSEMENT = "Classement";
    static final String VUE_ARBRE_AIDE = "Lecture seule. Faites glisser l'arbre pour le parcourir.";
    static final String VUE_INTROUVABLE = "Ce match n'est plus dans le tableau.";

    // --- Tournoi fermé ---
    static final String FERME = "Tournoi annulé : la salle est fermée.";
}
