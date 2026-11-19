package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Salle du jour J : cartes des tables, matchs prêts et bloqués, affectation
 * manuelle, bouton « Proposer », terminaison après saisie du score. Une seule
 * règle dure : un joueur n'est jamais sur deux tables à la fois.
 */
class ServiceSalleTest {

    /**
     * Horloge factice réglable (14 novembre 2026, 10 h 00 UTC). Le fuseau du poste est
     * utilisé, comme {@link SaisieResultatMatch} : les heures attendues sont converties.
     */
    private Instant instant = Instant.parse("2026-11-14T10:00:00Z");
    private final Horloge horloge = () -> instant;
    private final ServiceSalle salle = new ServiceSalle(horloge);

    /** Heure locale correspondant à 10 h 00 UTC plus quelques minutes. */
    private static LocalDateTime dixHeuresPlus(int minutes) {
        return LocalDateTime.ofInstant(Instant.parse("2026-11-14T10:00:00Z").plusSeconds(60L * minutes),
                ZoneId.systemDefault());
    }

    private void avancer(int minutes) {
        instant = instant.plusSeconds(60L * minutes);
    }

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private static Tableau tableau(String nom) {
        return new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
    }

    /** Lance un tableau du tournoi avec les joueurs donnés (pointés, clôturé, tiré avec la graine 1). */
    private static void lancer(Tournoi t, Tableau tab, List<Joueur> joueurs) {
        tab.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 1L);
    }

    /** Tournoi de {@code tables} tables, un tableau « A » de six joueurs : deux poules de trois. */
    private static Tournoi tournoiSixJoueurs(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        lancer(t, a, List.of(j("L1"), j("L2"), j("L3"), j("L4"), j("L5"), j("L6")));
        return t;
    }

    private static Tableau tab(Tournoi t, String nom) {
        return t.getTableaux().stream().filter(x -> x.getNom().equals(nom)).findFirst().orElseThrow();
    }

    private static Poule poule(Tournoi t, int i) {
        return tab(t, "A").getPoules().get(i);
    }

    private static List<Manche> troisZero() {
        return List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5));
    }

    private static Set<String> licencesEnJeu(TableauDeBordSalle bord) {
        Set<String> vus = new HashSet<>();
        for (TableauDeBordSalle.CarteTable c : bord.getTables()) {
            if (!c.estLibre()) {
                Match m = c.match().match();
                assertTrue(vus.add(m.getJoueurA().getNumeroLicence()), "joueur sur deux tables");
                assertTrue(vus.add(m.getJoueurB().getNumeroLicence()), "joueur sur deux tables");
            }
        }
        return vus;
    }

    // ------------------------------------------------------------------
    // Photographie de la salle
    // ------------------------------------------------------------------

    /**
     * Phase de poules : on lance une poule, pas un match. Au départ, aucune partie
     * n'est « prête » seule ; les deux poules de 3 sont prêtes à partir.
     */
    @Test
    void une_carte_par_table_et_toutes_les_poules_pretes_au_depart() {
        Tournoi t = tournoiSixJoueurs(4);
        TableauDeBordSalle bord = salle.tableauDeBord(t);

        assertEquals(4, bord.getTables().size());
        assertEquals(4, bord.nombreTablesLibres());
        assertEquals(List.of(1, 2, 3, 4), bord.tablesLibres());
        assertTrue(bord.getPrets().isEmpty(), "aucune partie de poule ne part seule");
        assertTrue(bord.getBloques().isEmpty());
        assertTrue(bord.getAnomalies().isEmpty());
        List<ListeLancements.Ligne> jouables = salle.listeLancements(t).getJouables();
        assertEquals(2, jouables.size(), "2 poules de 3 : 2 poules à lancer");
        ListeLancements.Ligne premiere = jouables.get(0);
        assertTrue(premiere.estPoule());
        assertEquals("A", premiere.match().tableau());
        assertTrue(premiere.echelon().startsWith("Poule "), premiere.echelon());
        assertEquals(3, premiere.poule().nombreMatchs());
        assertEquals(PhaseMatch.POULE, premiere.match().phase());
        assertFalse(premiere.match().explication().isEmpty());
    }


    @Test
    void tableaux_non_lances_et_salle_sans_table() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        a.inscrire(j("L1"));
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(0, bord.getNombreDeTables());
        assertTrue(bord.getTables().isEmpty());
        assertTrue(bord.getPrets().isEmpty(), "un tableau en inscription n'a pas de match");
        assertEquals(ResultatPropositionSalle.Motif.SALLE_SANS_TABLE, salle.proposer(tournoiSixJoueurs(0)).getMotif());
    }

    // ------------------------------------------------------------------
    // Affectation manuelle
    // ------------------------------------------------------------------

    @Test
    void lancement_manuel_d_une_poule_occupe_la_table_et_tous_ses_joueurs() {
        Tournoi t = tournoiSixJoueurs(4);
        Poule p = poule(t, 0);
        Match m = p.getMatchs().get(0);
        assertEquals(ResultatAffectationTable.Motif.LANCEMENT_PAR_POULE, salle.affecter(t, m, 3).getMotif(),
                "phase de poules : un match ne part pas seul");

        ResultatAffectationTable r = salle.lancerPoule(t, p, 3);

        assertEquals(ResultatAffectationTable.Motif.AFFECTE, r.getMotif());
        for (Match x : p.getMatchs()) {
            assertEquals(StatutMatch.EN_COURS, x.getStatut());
            assertEquals(Integer.valueOf(3), x.getNumeroTable());
            assertEquals(dixHeuresPlus(0), x.getInstantLancement());
        }
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertSame(m, bord.table(3).match().match(), "la première partie (I.301) se joue d'abord");
        assertEquals(Integer.valueOf(3), bord.tableDe(m));
        assertEquals(3, bord.nombreTablesLibres());
        assertTrue(bord.getBloques().isEmpty(), "les parties de la poule sont en cours sur sa table");
        List<ListeLancements.Ligne> jouables = salle.listeLancements(t).getJouables();
        assertEquals(1, jouables.size(), "l'autre poule reste jouable");
        assertSame(poule(t, 1), jouables.get(0).poule().poule());
    }


    @Test
    void lancement_refuse_poule_en_cours_table_occupee_hors_limites_ou_poule_jouee() {
        Tournoi t = tournoiSixJoueurs(4);
        Poule p1 = poule(t, 0);
        Poule p2 = poule(t, 1);
        assertTrue(salle.lancerPoule(t, p1, 1).estReussi());

        assertEquals(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS, salle.lancerPoule(t, p1, 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, salle.lancerPoule(t, p2, 1).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, salle.lancerPoule(t, p2, 0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, salle.lancerPoule(t, p2, 5).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS, salle.affecter(t, p1.getMatchs().get(1), 2)
                .getMotif());
        assertNull(p2.getNumeroTable(), "rien n'est modifié");

        p2.getMatchs().forEach(m -> m.definirScore(troisZero()));
        assertEquals(ResultatAffectationTable.Motif.MATCH_TERMINE, salle.lancerPoule(t, p2, 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_INCONNU,
                salle.lancerPoule(t, new Poule("X", List.of(j("X1"), j("X2"))), 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_INCONNU,
                salle.affecter(t, new Match(j("X1"), j("X2")), 2).getMotif());

        t.changerStatut(StatutTournoi.ANNULE);
        assertEquals(ResultatAffectationTable.Motif.TOURNOI_FERME, salle.lancerPoule(t, p2, 2).getMotif());
        assertEquals(ResultatPropositionSalle.Motif.TOURNOI_FERME, salle.proposer(t).getMotif());
    }


    @Test
    void joueur_engage_sur_deux_tableaux_jamais_sur_deux_tables() {
        Tournoi t = tournoiSixJoueurs(6);
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L1"); // même licence que dans le tableau A
        lancer(t, b, List.of(commun, j("M2"), j("M3")));

        Poule deA = tab(t, "A").getPoules().stream().filter(p -> p.getJoueurs().contains(commun)).findFirst()
                .orElseThrow();
        assertTrue(salle.lancerPoule(t, deA, 2).estReussi());

        ResultatAffectationTable r = salle.lancerPoule(t, b.getPoules().get(0), 5);
        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertEquals(commun, r.getJoueurBloquant());
        assertEquals(Integer.valueOf(2), r.getTableDuJoueurBloquant());

        // « Proposer » non plus ne le met pas sur une deuxième table.
        salle.proposer(t);
        licencesEnJeu(salle.tableauDeBord(t));
        assertNull(b.getPoules().get(0).getNumeroTable());
    }


    // ------------------------------------------------------------------
    // Bouton « Proposer »
    // ------------------------------------------------------------------

    @Test
    void proposer_remplit_les_tables_libres_sans_doublon_de_joueur() {
        Tournoi t = tournoiSixJoueurs(6);

        ResultatPropositionSalle r = salle.proposer(t);

        assertTrue(r.aLance());
        // Deux poules de 3 : chacune part en bloc sur sa table.
        assertEquals(2, r.getLancements().size());
        assertEquals(List.of(1, 2), r.getLancements().stream().map(ResultatPropositionSalle.Lancement::table).toList());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(4, licencesEnJeu(bord).size(), "une partie se joue sur chaque table");
        assertEquals(4, bord.nombreTablesLibres());
        assertTrue(bord.getPrets().isEmpty());
        assertTrue(tab(t, "A").getPoules().stream().allMatch(Poule::estLanceeEnBloc));

        assertEquals(ResultatPropositionSalle.Motif.AUCUN_MATCH_PRET, salle.proposer(t).getMotif());
    }


    @Test
    void proposer_s_arrete_au_nombre_de_tables_libres() {
        Tournoi t = tournoiSixJoueurs(1);
        ResultatPropositionSalle r = salle.proposer(t);
        assertEquals(1, r.getLancements().size());
        assertEquals(1, r.getLancements().get(0).table());
        assertEquals(ResultatPropositionSalle.Motif.AUCUNE_TABLE_LIBRE, salle.proposer(t).getMotif());
    }

    // ------------------------------------------------------------------
    // Tri et motifs
    // ------------------------------------------------------------------

    @Test
    void poules_triees_par_priorite_le_joueur_engage_ailleurs_passe_devant() {
        Tournoi t = tournoiSixJoueurs(6);
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L4");
        lancer(t, b, List.of(commun, j("M2"), j("M3")));

        List<ListeLancements.Ligne> jouables = salle.listeLancements(t).getJouables();
        for (int i = 1; i < jouables.size(); i++) {
            assertTrue(jouables.get(i - 1).match().priorite() >= jouables.get(i).match().priorite(), "tri décroissant");
        }
        ListeLancements.Ligne premiere = jouables.get(0);
        assertTrue(premiere.poule().joueurs().contains(commun),
                "une poule dont un joueur est engagé dans deux tableaux débloque plus de parties");
        assertTrue(premiere.match().explication().contains("débloque"), premiere.match().explication());
    }


    @Test
    void l_attente_compte_depuis_la_fin_du_dernier_match_des_joueurs() {
        Tournoi t = tournoiSixJoueurs(6);
        Poule p1 = poule(t, 0);
        Poule p2 = poule(t, 1);
        // Tableau B : un joueur de chaque poule de A.
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        lancer(t, b, List.of(p1.getJoueurs().get(0), p2.getJoueurs().get(0), j("M3")));
        SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);
        // Poule 1 : jouée de 10 h 00 à 10 h 05.
        salle.lancerPoule(t, p1, 1);
        avancer(5);
        p1.getMatchs().forEach(m -> saisie.enregistrerScore(tab(t, "A"), m, troisZero()));
        // Poule 2 : lancée à 10 h 05, terminée à 10 h 25.
        salle.lancerPoule(t, p2, 2);
        avancer(20);
        p2.getMatchs().forEach(m -> saisie.enregistrerScore(tab(t, "A"), m, troisZero()));
        avancer(20);

        // La poule de B attend depuis que son dernier joueur est libre : 10 h 25.
        ListeLancements.Ligne l = salle.listeLancements(t).getJouables().get(0);
        assertSame(b.getPoules().get(0), l.poule().poule());
        assertTrue(l.match().explication().contains("attend 20 min"), l.match().explication());
    }


    private static Poule pouleDe(Tournoi t, Match m) {
        for (Poule p : tab(t, "A").getPoules()) {
            if (p.getMatchs().contains(m)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Terminer : saisie du score puis libération
    // ------------------------------------------------------------------

    @Test
    void terminer_la_derniere_partie_libere_la_table_de_la_poule() {
        Tournoi t = tournoiSixJoueurs(4);
        List<Match> matchs = poule(t, 0).getMatchs();
        Match m = matchs.get(0);
        salle.lancerPoule(t, poule(t, 0), 2);
        assertEquals(ResultatAffectationTable.Motif.RESULTAT_NON_SAISI, salle.libererApresResultat(t, m).getMotif());
        avancer(18);

        ResultatSaisieMatch saisie = new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), m, troisZero());
        assertTrue(saisie.estEnregistre());
        assertEquals(ResultatAffectationTable.Motif.POULE_CONTINUE, salle.libererApresResultat(t, m).getMotif());
        assertEquals(dixHeuresPlus(18), m.getInstantFin());
        assertFalse(salle.tableauDeBord(t).table(2).estLibre(), "la poule continue sur sa table");
        for (Match x : matchs.subList(1, 3)) {
            avancer(15);
            new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), x, troisZero());
        }
        ResultatAffectationTable r = salle.libererApresResultat(t, matchs.get(2));

        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, r.getMotif());
        assertEquals(2, r.getNumeroTable());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(4, bord.nombreTablesLibres());
        assertTrue(bord.getBloques().isEmpty());
        assertEquals(1, salle.listeLancements(t).getJouables().size(), "reste l'autre poule");
        assertEquals(3, SuiviPoule.matchsTermines(poule(t, 0)), "la poule voit les résultats");
        assertEquals(Integer.valueOf(2), m.getNumeroTable(), "la table jouée reste notée sur le match");
    }


    @Test
    void abandon_en_cours_de_match_libere_aussi_la_table() {
        Tournoi t = tournoiSixJoueurs(2);
        List<Match> matchs = poule(t, 1).getMatchs();
        salle.lancerPoule(t, poule(t, 1), 1);
        avancer(7);
        SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);
        saisie.enregistrerScore(tab(t, "A"), matchs.get(0), troisZero());
        saisie.enregistrerScore(tab(t, "A"), matchs.get(1), troisZero());
        Match m = matchs.get(2);
        saisie.enregistrerIssue(tab(t, "A"), m, IssueMatch.ABANDON, m.getJoueurB());
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, salle.libererApresResultat(t, m).getMotif());
        assertTrue(salle.tableauDeBord(t).table(1).estLibre());
    }


    @Test
    void un_score_saisi_depuis_l_ecran_des_poules_libere_la_table_sans_autre_action() {
        Tournoi t = tournoiSixJoueurs(2);
        List<Match> matchs = poule(t, 0).getMatchs();
        assertTrue(salle.lancerPoule(t, poule(t, 0), 2).estReussi());
        SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);
        saisie.enregistrerScore(tab(t, "A"), matchs.get(0), troisZero());
        saisie.enregistrerScore(tab(t, "A"), matchs.get(1), troisZero());
        assertFalse(salle.tableauDeBord(t).table(2).estLibre());
        saisie.enregistrerScore(tab(t, "A"), matchs.get(2), troisZero());
        assertTrue(salle.tableauDeBord(t).table(2).estLibre());
    }


    @Test
    void annuler_une_affectation_erronee() {
        Tournoi t = tournoiSixJoueurs(3);
        Match m = poule(t, 0).getMatchs().get(0);
        salle.lancerPoule(t, poule(t, 0), 3);

        // « Annuler » sur la table : c'est le lancement de la poule entière qui s'annule.
        ResultatAffectationTable r = salle.annulerAffectation(t, m);

        assertEquals(ResultatAffectationTable.Motif.AFFECTATION_ANNULEE, r.getMotif());
        assertEquals(3, r.getNumeroTable());
        assertTrue(poule(t, 0).getMatchs().stream().allMatch(x -> x.getStatut() == StatutMatch.A_JOUER
                && x.getNumeroTable() == null));
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(3, bord.nombreTablesLibres());
        assertEquals(2, salle.listeLancements(t).getJouables().size());
        assertEquals(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS, salle.annulerAffectation(t, m).getMotif());
    }


    // ------------------------------------------------------------------
    // Durée écoulée
    // ------------------------------------------------------------------

    @Test
    void duree_ecoulee_selon_l_horloge() {
        Tournoi t = tournoiSixJoueurs(2);
        salle.lancerPoule(t, poule(t, 0), 1);
        assertEquals(Duration.ZERO, salle.tableauDeBord(t).table(1).ecoule());
        avancer(17);
        assertEquals(Duration.ofMinutes(17), salle.tableauDeBord(t).table(1).ecoule());
        assertNull(salle.tableauDeBord(t).table(2).ecoule(), "table libre : pas de durée");
    }


    // ------------------------------------------------------------------
    // Anomalies (fichier modifié, tables retirées)
    // ------------------------------------------------------------------

    @Test
    void match_en_cours_sans_table_valable_signale_en_anomalie() {
        Tournoi t = tournoiSixJoueurs(2);
        Match surTable9 = poule(t, 0).getMatchs().get(0);
        surTable9.setStatut(StatutMatch.EN_COURS);
        surTable9.setNumeroTable(9);
        // Ancien fichier : matchs de poule lancés un par un.
        Match a = poule(t, 1).getMatchs().get(0);
        a.setStatut(StatutMatch.EN_COURS);
        a.setNumeroTable(1);
        a.setInstantLancement(dixHeuresPlus(0));
        // Un second match « en cours » sur la même table, lancé plus tard.
        Match b = poule(t, 1).getMatchs().get(1);
        b.setStatut(StatutMatch.EN_COURS);
        b.setNumeroTable(2);
        b.setInstantLancement(dixHeuresPlus(30));

        TableauDeBordSalle bord = salle.tableauDeBord(t);

        assertSame(a, bord.table(1).match().match());
        assertTrue(bord.table(2).estLibre(), "b partage un joueur avec a : il ne peut pas occuper la table 2");
        assertEquals(2, bord.getAnomalies().size());
        assertTrue(salle.annulerAffectation(t, surTable9).estReussi(), "une anomalie se régularise");
        assertEquals(StatutMatch.A_JOUER, surTable9.getStatut());
    }

    // ------------------------------------------------------------------
    // Tableau final
    // ------------------------------------------------------------------

    @Test
    void matchs_du_tableau_final_prets_puis_tours_suivants_en_attente() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(8);
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        lancer(t, a, List.of(j("L1"), j("L2"), j("L3"), j("L4"), j("L5"), j("L6"), j("L7"), j("L8"), j("L9")));
        for (Poule p : a.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(troisZero());
            }
        }
        ServicePhaseFinale pf = new ServicePhaseFinale();
        assertTrue(pf.construire(a, 7L).estConstruit());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        Tour premierTour = a.getPhaseFinale().getTours().get(0);
        long jouables = premierTour.getMatchs().stream()
                .filter(m -> !m.estBye() && !m.getJoueurA().estADeterminer() && !m.getJoueurB().estADeterminer())
                .count();
        assertEquals(jouables, bord.getPrets().size());
        assertTrue(bord.getPrets().stream().allMatch(p -> p.phase() == PhaseMatch.PREMIER_TOUR_TABLEAU));
        assertTrue(bord.getPrets().stream().allMatch(p -> p.tour().equals(premierTour.getNom())));
        assertTrue(bord.getEnAttenteDesJoueurs() > 0, "les tours suivants attendent leurs joueurs");

        Match m = bord.getPrets().get(0).match();
        assertTrue(salle.affecter(t, m, 4).estReussi());
        avancer(25);
        assertTrue(pf.saisirScore(a, m, troisZero()).estValide());
        ResultatAffectationTable r = salle.libererApresResultat(t, m);
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, r.getMotif());
        assertEquals(dixHeuresPlus(25), m.getInstantFin(),
                "la saisie du tableau final ne pose pas l'heure de fin : la salle la pose");
        assertTrue(salle.tableauDeBord(t).table(4).estLibre());
    }
}
