package com.pingmanager.ui.ecran;

import com.pingmanager.core.AnnonceRencontre;
import com.pingmanager.core.NiveauPriorite;
import com.pingmanager.core.ResultatPropositionSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
                    ? "Affectation annulée : la table " + table + " est libre, le match revient dans les matchs proposés."
                    : "Affectation annulée : le match revient dans les matchs proposés.";
        }
        if (motif == ResultatAffectationTable.Motif.TABLE_LIBEREE) {
            return table > 0 ? "Résultat enregistré : la table " + table + " est libre." : "Résultat enregistré.";
        }
        if (motif == ResultatAffectationTable.Motif.TABLE_HORS_LIMITES) {
            return nombreDeTables > 0
                    ? "La table " + table + " n'existe pas : la salle compte les tables 1 à " + nombreDeTables + "."
                    : "Le nombre de tables du tournoi n'est pas renseigné (Informations sportives et techniques).";
        }
        if (motif == ResultatAffectationTable.Motif.TABLE_RESERVEE) {
            List<String> admis = new ArrayList<>(r.getTableauxAdmis());
            return admis.isEmpty()
                    ? "La table " + table + " est réservée à d'autres tableaux (plan de la salle)."
                    : "La table " + table + " est réservée " + (admis.size() > 1 ? "aux tableaux " : "au tableau ")
                    + listeLisible(admis) + " (plan de la salle).";
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
        if (motif == ResultatPropositionSalle.Motif.TABLES_LIBRES_RESERVEES) {
            return "Des matchs sont prêts, mais les tables libres sont réservées à d'autres tableaux "
                    + "(plan de la salle).";
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

    /**
     * Ligne de chiffres de l'écran principal du jour J : « 12 tables libres · 4 en
     * cours · 1 match proposé · 4 bloqués ».
     */
    static String resumeLigne(TableauDeBordSalle bord) {
        if (bord.getNombreDeTables() == 0) {
            return "Nombre de tables non renseigné";
        }
        int libres = bord.nombreTablesLibres();
        int occupees = bord.nombreTablesOccupees();
        int prets = bord.getPrets().size();
        int bloques = bord.getBloques().size();
        StringBuilder sb = new StringBuilder();
        sb.append(libres).append(libres > 1 ? " tables libres" : " table libre");
        sb.append(" · ").append(occupees).append(" en cours");
        sb.append(" · ").append(prets).append(prets > 1 ? " matchs proposés" : " match proposé");
        if (bloques > 0) {
            sb.append(" · ").append(bloques).append(bloques > 1 ? " bloqués" : " bloqué");
        }
        return sb.toString();
    }

    /**
     * Après un score saisi, match proposé pour enchaîner sur la table qui vient
     * de se libérer : le premier des matchs proposés (ordre de l'ordonnanceur),
     * si la table est toujours libre et qu'un match attend.
     *
     * @param table numéro de la table libérée (0 ou moins : aucune)
     */
    static Optional<TableauDeBordSalle.MatchSalle> suiteSurTable(TableauDeBordSalle bord, int table) {
        if (table <= 0 || bord.getPrets().isEmpty() || !bord.tablesLibres().contains(table)) {
            return Optional.empty();
        }
        return Optional.of(bord.getPrets().get(0));
    }

    /**
     * Table proposée pour l'envoi en un clic d'un match proposé : la plus petite
     * table libre, ou vide si toutes sont occupées.
     */
    static Optional<Integer> premiereTableLibre(TableauDeBordSalle bord) {
        List<Integer> libres = bord.tablesLibres();
        return libres.isEmpty() ? Optional.empty() : Optional.of(libres.get(0));
    }

    // ==================================================================
    // Jour J v2 (B037)
    // ==================================================================

    /** Classe de style d'un niveau de priorité : rouge, orange ou vert. */
    static String classePriorite(NiveauPriorite n) {
        if (n == NiveauPriorite.URGENT) {
            return "priorite-urgent";
        }
        if (n == NiveauPriorite.IMPORTANT) {
            return "priorite-important";
        }
        return "priorite-normal";
    }

    /** Pastille d'un niveau de priorité (charte v2) : erreur, attention, succès. */
    static StatutPastille pastillePriorite(NiveauPriorite n) {
        if (n == NiveauPriorite.URGENT) {
            return StatutPastille.ERREUR;
        }
        if (n == NiveauPriorite.IMPORTANT) {
            return StatutPastille.ATTENTION;
        }
        return StatutPastille.SUCCES;
    }

    /** Info-bulle de la couleur : « Urgent : attend depuis 25 min ». */
    static String detailPriorite(TableauDeBordSalle.MatchSalle m) {
        String motif = m.motifNiveau();
        return m.niveau().getLibelle() + (motif.isEmpty() ? " : peut attendre" : " : " + motif);
    }

    /**
     * Ligne de chiffres de l'écran principal (B037) : « 12 tables libres · 4 en
     * cours · 1 match proposé · 3 joueurs en attente ».
     */
    static String resumeSalle(TableauDeBordSalle bord) {
        if (bord.getNombreDeTables() == 0) {
            return "Nombre de tables non renseigné";
        }
        int libres = bord.nombreTablesLibres();
        int prets = bord.getPrets().size();
        int attente = bord.getJoueursEnAttente().size();
        return libres + (libres > 1 ? " tables libres" : " table libre")
                + " · " + bord.nombreTablesOccupees() + " en cours"
                + " · " + prets + (prets > 1 ? " matchs proposés" : " match proposé")
                + " · " + attente + (attente > 1 ? " joueurs en attente" : " joueur en attente");
    }

    /** Chrono d'un joueur en attente : « 12 min », ou « pas commencé » s'il est inconnu. */
    static String chrono(TableauDeBordSalle.JoueurEnAttente j) {
        return j.attente() == null ? "pas commencé" : duree(j.attente());
    }

    /** Le chrono d'attente d'un joueur est-il long (rouge) ? Même seuil que les matchs. */
    static boolean attenteLongue(TableauDeBordSalle.JoueurEnAttente j) {
        return j.attente() != null
                && j.attente().compareTo(com.pingmanager.core.ReglePrioriteSalle.ATTENTE_LONGUE) >= 0;
    }

    /**
     * Motif court sous le nom d'un joueur en attente : « Peut jouer », « Attend
     * Léa DURAND (table 4) », « Attend son adversaire ».
     */
    static String motifCourt(TableauDeBordSalle.JoueurEnAttente j) {
        if (j.peutJouer()) {
            return "Peut jouer : match prêt";
        }
        for (TableauDeBordSalle.Engagement e : j.engagements()) {
            if (e.raison() == TableauDeBordSalle.RaisonAttente.ADVERSAIRE_EN_JEU) {
                return "Attend " + nom(e.adversaire())
                        + (e.tableAdversaire() == null ? "" : " (table " + e.tableAdversaire() + ")");
            }
        }
        return "Attend son adversaire (tour précédent)";
    }

    /**
     * Pourquoi un joueur ne joue pas, une phrase par match à jouer :
     * « C – Toutes séries · Poule B · tour 2 : contre Léa DURAND, qui joue à la table 4. »
     */
    static List<String> pourquoi(TableauDeBordSalle.JoueurEnAttente j, TableauDeBordSalle bord) {
        List<String> res = new ArrayList<>();
        boolean tableLibre = bord.nombreTablesLibres() > 0;
        for (TableauDeBordSalle.Engagement e : j.engagements()) {
            String debut = contexte(e.match()) + " : ";
            if (e.raison() == TableauDeBordSalle.RaisonAttente.ADVERSAIRE_EN_JEU) {
                res.add(debut + "contre " + nom(e.adversaire()) + ", qui joue "
                        + (e.tableAdversaire() == null ? "déjà" : "à la table " + e.tableAdversaire())
                        + ". Un joueur ne peut pas être sur deux tables.");
            } else if (e.raison() == TableauDeBordSalle.RaisonAttente.ADVERSAIRE_INCONNU) {
                res.add(debut + "son adversaire n'est pas encore connu : le tour précédent n'est pas joué.");
            } else if (tableLibre) {
                res.add(debut + "contre " + nom(e.adversaire()) + ", match prêt : envoyez-le sur une table.");
            } else {
                res.add(debut + "contre " + nom(e.adversaire()) + ", match prêt : toutes les tables sont occupées.");
            }
        }
        return res;
    }

    /**
     * Une ligne d'un groupe (poule ou tour) pour situer un joueur : les deux noms,
     * lequel est surligné, et l'état du match (« Table 4 », « Joué », « À jouer »).
     */
    record LigneGroupe(String joueurA, String joueurB, boolean surligneA, boolean surligneB, String etat) {
        boolean concerne() {
            return surligneA || surligneB;
        }
    }

    /** Lignes du groupe d'un engagement, le joueur surligné partout où il apparaît. */
    static List<LigneGroupe> lignesGroupe(TableauDeBordSalle.Engagement e, Joueur joueur) {
        List<LigneGroupe> res = new ArrayList<>();
        String licence = joueur.getNumeroLicence();
        for (Match m : e.matchsDuGroupe()) {
            if (m.estBye()) {
                continue;
            }
            boolean a = m.getJoueurA() != null && licence.equals(m.getJoueurA().getNumeroLicence());
            boolean b = m.getJoueurB() != null && licence.equals(m.getJoueurB().getNumeroLicence());
            res.add(new LigneGroupe(nom(m.getJoueurA()), nom(m.getJoueurB()), a, b, etatMatch(m)));
        }
        return res;
    }

    /** « Table 4 », « Joué », « À jouer ». */
    static String etatMatch(Match m) {
        if (m.getStatut() == StatutMatch.EN_COURS) {
            return m.getNumeroTable() == null ? "En cours" : "Table " + m.getNumeroTable();
        }
        if (m.getStatut() == StatutMatch.TERMINE) {
            return "Joué";
        }
        return "À jouer";
    }

    /** Un match qu'on peut choisir dans « Lancer un match », et pourquoi il est refusé (vide sinon). */
    record Lancable(TableauDeBordSalle.MatchSalle match, String refus) {
        boolean possible() {
            return refus.isEmpty();
        }
    }

    /**
     * Matchs proposés au bouton « Lancer un match » : tous les matchs à jouer dont
     * les deux joueurs sont connus (prêts puis bloqués, avec leur motif), filtrés
     * par nom de joueur ou de tableau (sans accents ni majuscules).
     */
    static List<Lancable> lancables(TableauDeBordSalle bord, String recherche) {
        String filtre = normaliser(recherche);
        List<Lancable> res = new ArrayList<>();
        for (TableauDeBordSalle.MatchSalle m : bord.getPrets()) {
            if (correspond(m, filtre)) {
                res.add(new Lancable(m, ""));
            }
        }
        for (TableauDeBordSalle.MatchBloque b : bord.getBloques()) {
            if (correspond(b.match(), filtre)) {
                res.add(new Lancable(b.match(), motifBlocage(b)));
            }
        }
        return res;
    }

    private static boolean correspond(TableauDeBordSalle.MatchSalle m, String filtre) {
        if (filtre.isEmpty()) {
            return true;
        }
        return normaliser(joueurs(m.match()) + " " + contexte(m)).contains(filtre);
    }

    static String normaliser(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Libellé du gros bouton d'une table : « Terminer la poule » quand ce match
     * est la dernière partie à jouer de sa poule, sinon « Terminer la rencontre ».
     */
    static String libelleTerminer(Tournoi tournoi, Match m) {
        for (Tableau t : tournoi.getTableaux()) {
            for (Poule p : t.getPoules()) {
                if (p.getMatchs().contains(m)) {
                    boolean derniere = p.getMatchs().stream()
                            .allMatch(x -> x == m || x.estBye() || x.getStatut() == StatutMatch.TERMINE);
                    return derniere ? "Terminer la poule" : "Terminer la rencontre";
                }
            }
        }
        return "Terminer la rencontre";
    }

    /** Phrase de la confirmation d'« Annuler » sur une table. */
    static String confirmationAnnulation(int table) {
        return "Annuler ce lancement ? La table " + table + " redevient libre et le match revient dans les "
                + "matchs proposés. Le score n'est pas touché.";
    }

    /** Libellé du choix d'annonce mémorisé : « Annonce : SMS », ou « Annonce : à demander ». */
    static String libelleAnnonce(ModeAnnonce mode) {
        return "Annonce : " + (mode == null ? "à demander" : mode.getLibelle());
    }

    /**
     * Annonce affichée à l'écran tant que les envois réels n'existent pas :
     * « SMS et appel vocal (envoi automatique à venir) · Table 4 : … ».
     */
    static String texteAnnonce(AnnonceRencontre a) {
        String mode = a.aEnvoyer()
                ? a.mode().getLibelle() + " (envoi automatique à venir : annoncez au micro)"
                : "À annoncer au micro";
        return mode + " · " + a.texte();
    }
}
