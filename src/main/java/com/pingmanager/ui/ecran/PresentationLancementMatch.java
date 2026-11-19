package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeLancements;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.util.List;
import java.util.Optional;

/**
 * Logique d'affichage de l'interface « Lancer un match » (B057,
 * {@link PanneauLancementMatch}), sans JavaFX : elle se teste directement.
 * Les règles (joueurs libres, table libre) et l'ordre de priorité restent
 * dans le cœur ({@link com.pingmanager.core.ServiceSalle#listeLancements},
 * {@link ListeLancements#getJouables()}, {@link ListeLancements#propositionApres}) ;
 * ici, on ne fait que les dire. Les textes fixes sont dans {@link TextesLancementMatch}.
 */
final class PresentationLancementMatch {

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
            return TextesLancementMatch.AUCUNE_TABLE;
        }
        if (m == ResultatAffectationTable.Motif.TABLE_OCCUPEE) {
            return "la table " + r.getNumeroTable() + " vient d'être prise";
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
        if (m == ResultatAffectationTable.Motif.JOUEUR_ABSENT) {
            return PresentationSalle.nom(r.getJoueurBloquant()) + " n'est pas pointé";
        }
        return PresentationSalle.message(r, nombreDeTables);
    }

    /** Message de refus d'un lancement : « Match non lancé : Paul MARTIN joue en table 4. ». */
    static String refus(ResultatAffectationTable r, int nombreDeTables) {
        String m = motif(r, nombreDeTables);
        if (m.endsWith(".")) {
            m = m.substring(0, m.length() - 1);
        }
        return String.format(TextesLancementMatch.REFUS, m);
    }

    /** Confirmation après un lancement : « MARTIN – DUPONT lancé en table 5 ». */
    static String lance(Match m, int table) {
        return TextesLancementMatch.lance(nomCourt(m.getJoueurA()), nomCourt(m.getJoueurB()), table);
    }

    /** Confirmation après le lancement d'une poule : « Tableau B, poule 3 lancée en table 5 ». */
    static String pouleLancee(String tableau, com.pingmanager.model.Poule poule, int table) {
        return TextesLancementMatch.pouleLancee(tableau, poule.getNom(), table);
    }

    /** Message de refus du lancement d'une poule : « Poule non lancée : Paul MARTIN joue en table 4. ». */
    static String refusPoule(ResultatAffectationTable r, int nombreDeTables) {
        String m = motif(r, nombreDeTables);
        if (m.endsWith(".")) {
            m = m.substring(0, m.length() - 1);
        }
        return String.format(TextesLancementMatch.REFUS_POULE, m);
    }

    /**
     * Joueurs d'une poule dans l'ordre de classement, un par ligne :
     * « 1. Pierre DUPONT · 1520 pts ».
     */
    static List<String> joueursPoule(com.pingmanager.core.PouleALancer p) {
        List<String> r = new java.util.ArrayList<>();
        for (int i = 0; i < p.joueurs().size(); i++) {
            Joueur j = p.joueurs().get(i);
            r.add((i + 1) + ". " + PresentationSalle.nom(j) + (j == null || j.estADeterminer() ? ""
                    : " · " + j.getPoints() + " pts"));
        }
        return r;
    }

    /**
     * Joueurs d'une poule sur deux lignes au plus, pour une ligne de la liste :
     * la première moitié, puis la seconde (« 1. DUPONT 1520 · 2. MARTIN 1480 »).
     */
    static String joueursPouleCompact(com.pingmanager.core.PouleALancer p) {
        List<String> noms = new java.util.ArrayList<>();
        for (int i = 0; i < p.joueurs().size(); i++) {
            Joueur j = p.joueurs().get(i);
            noms.add((i + 1) + ". " + nomCourt(j) + (j == null || j.estADeterminer() ? "" : " " + j.getPoints()));
        }
        int moitie = (noms.size() + 1) / 2;
        return String.join(" · ", noms.subList(0, moitie))
                + (noms.size() > moitie ? "\n" + String.join(" · ", noms.subList(moitie, noms.size())) : "");
    }

    /** « 3 matchs · environ 1 h 00 » : matchs à jouer et durée prévue (somme des durées de ses matchs). */
    static String matchsEtDuree(com.pingmanager.core.PouleALancer p) {
        int n = p.nombreMatchs();
        return n + (n > 1 ? " matchs" : " match") + " · environ " + PresentationSalle.duree(p.dureePrevue());
    }

    /**
     * Ordre des matchs d'une poule, tel qu'ils se jouent sur la table (I.301) :
     * « 1. 1–3 DUPONT – DURAND ».
     */
    static List<String> ordreDesMatchs(com.pingmanager.core.PouleALancer p) {
        List<String> r = new java.util.ArrayList<>();
        int k = 1;
        for (Match m : p.matchs()) {
            r.add(k++ + ". " + p.numero(m.getJoueurA()) + "–" + p.numero(m.getJoueurB()) + "  "
                    + nomCourt(m.getJoueurA()) + " – " + nomCourt(m.getJoueurB())
                    + (m.getStatut() == com.pingmanager.model.StatutMatch.TERMINE ? " (joué)" : ""));
        }
        return r;
    }

    /** Nom de famille seul (« MARTIN »), « À déterminer » si inconnu. */
    static String nomCourt(Joueur j) {
        return j == null || j.estADeterminer() ? "À déterminer" : j.getNom();
    }

    /** « Table 3 » dans les listes de tables libres. */
    static String libelleTable(int table) {
        return "Table " + table;
    }

    /** Colonne « Tableau » : « Simple messieurs · Poule 3 », « Open · 1/8 de finale ». */
    static String tableauEtEchelon(ListeLancements.Ligne l) {
        return l.echelon().isEmpty() ? l.match().tableau() : l.match().tableau() + " · " + l.echelon();
    }

    /**
     * Table retenue pour une ligne : celle choisie par le juge-arbitre si elle
     * est toujours libre pour ce match, sinon la table pré-choisie par le cœur
     * (null s'il n'y a aucune table libre).
     */
    static Integer tableRetenue(ListeLancements.Ligne l, Integer choix) {
        if (choix != null && l.tablesPossibles().contains(choix)) {
            return choix;
        }
        return l.tableProposee();
    }

    /** « Proposition 2 sur 6 » : rang de la proposition affichée parmi les matchs jouables. */
    static String rang(ListeLancements liste, Match propose) {
        List<ListeLancements.Ligne> jouables = liste.getJouables();
        for (int i = 0; i < jouables.size(); i++) {
            if (jouables.get(i).match().match() == propose) {
                return String.format(TextesLancementMatch.PROPOSITION_RANG, i + 1, jouables.size());
            }
        }
        return "";
    }

    /**
     * Ce que montre le bandeau de proposition.
     *
     * @param ligne         la proposition affichée, ou null (zone vide ou aucun match)
     * @param texte         texte de la zone quand il n'y a pas de proposition
     * @param lancerActif   « Lancer » utilisable
     * @param autreActif    « Autre » utilisable (au moins deux matchs jouables)
     * @param motifLancer   pourquoi « Lancer » est désactivé (« Aucune table libre »), vide sinon
     */
    record Bandeau(ListeLancements.Ligne ligne, String texte, boolean lancerActif, boolean autreActif,
                   String motifLancer) {
    }

    /**
     * État du bandeau : vide tant qu'on n'a pas demandé de proposition ;
     * « Aucun match à lancer pour l'instant » sans match jouable ; sinon la
     * proposition, avec « Lancer » désactivé s'il n'y a aucune table libre.
     *
     * @param demande  « Proposer un match » a été appuyé (et rien n'a été lancé depuis)
     * @param propose  match proposé, ou null si le cœur n'avait aucun match jouable
     */
    static Bandeau bandeau(ListeLancements liste, boolean demande, Match propose, boolean ferme,
                           int nombreDeTables) {
        if (!demande) {
            return new Bandeau(null, TextesLancementMatch.ZONE_VIDE, false, false, "");
        }
        if (propose == null) {
            return new Bandeau(null, TextesLancementMatch.AUCUN_MATCH, false, false, "");
        }
        Optional<ListeLancements.Ligne> l = liste.jouable(propose);
        if (l.isEmpty()) {
            // Lancé depuis la liste, ou un joueur est parti jouer ailleurs : la zone se vide.
            return new Bandeau(null, TextesLancementMatch.ZONE_VIDE, false, false, "");
        }
        boolean autre = liste.getJouables().size() > 1;
        if (ferme) {
            return new Bandeau(l.get(), "", false, autre, TextesLancementMatch.FERME);
        }
        if (!l.get().estLancable()) {
            return new Bandeau(l.get(), "", false, autre, motif(l.get().refus(), nombreDeTables));
        }
        return new Bandeau(l.get(), "", true, autre, "");
    }

    /**
     * Pourquoi « Lancer » est désactivé sur une ligne de la liste
     * (« Aucune table libre »), vide si la ligne peut être lancée.
     */
    static String motifLigne(ListeLancements.Ligne l, boolean ferme, int nombreDeTables) {
        if (ferme) {
            return TextesLancementMatch.FERME;
        }
        return l.estLancable() ? "" : motif(l.refus(), nombreDeTables);
    }

    /**
     * Empreinte de la situation de la salle pour l'interface : tables libres et
     * matchs jouables avec leurs tables possibles. Si elle change (match terminé
     * ailleurs, table libérée), l'interface se redessine d'elle-même.
     */
    static String empreinte(ListeLancements liste) {
        StringBuilder sb = new StringBuilder(liste.getTablesLibres().toString());
        for (ListeLancements.Ligne l : liste.getJouables()) {
            sb.append('|').append(System.identityHashCode(l.match().match())).append(l.tablesPossibles());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // « Voir plus » : où se trouve le match
    // ------------------------------------------------------------------

    /**
     * Où se trouve un match dans le tournoi : son tableau et sa poule, ou le
     * tour du tableau final (avec son rang, pour l'arbre).
     */
    record Situation(Tableau tableau, Poule poule, Tour tour, int indexTour) {
        boolean estPoule() {
            return poule != null;
        }
    }

    /** Retrouve la poule ou le tour d'un match (identité de l'objet), ou vide. */
    static Optional<Situation> situer(Tournoi tournoi, Match match) {
        for (Tableau t : tournoi.getTableaux()) {
            for (Poule p : t.getPoules()) {
                if (p.getMatchs().stream().anyMatch(m -> m == match)) {
                    return Optional.of(new Situation(t, p, null, -1));
                }
            }
            if (t.aPhaseFinale()) {
                List<Tour> tours = t.getPhaseFinale().getTours();
                for (int k = 0; k < tours.size(); k++) {
                    if (tours.get(k).getMatchs().stream().anyMatch(m -> m == match)) {
                        return Optional.of(new Situation(t, null, tours.get(k), k));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Titre de la vue « Voir plus » : « Simple messieurs · Poule 3 », « Open · 1/8 de finale ». */
    static String titreSituation(Situation s) {
        return s.tableau().getNom() + " · " + (s.estPoule() ? "Poule " + s.poule().getNom() : s.tour().getNom());
    }

    /**
     * Position horizontale de l'arbre (0 à 1) pour montrer le tour concerné :
     * 0 pour le premier tour, 1 pour le dernier.
     */
    static double positionTour(Situation s) {
        int n = s.tableau().getPhaseFinale().getTours().size();
        return n <= 1 ? 0 : (double) s.indexTour() / (n - 1);
    }
}
