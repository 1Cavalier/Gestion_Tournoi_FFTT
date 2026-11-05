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

    @Test
    void une_carte_par_table_et_tous_les_matchs_de_poule_prets_au_depart() {
        Tournoi t = tournoiSixJoueurs(4);
        TableauDeBordSalle bord = salle.tableauDeBord(t);

        assertEquals(4, bord.getTables().size());
        assertEquals(4, bord.nombreTablesLibres());
        assertEquals(List.of(1, 2, 3, 4), bord.tablesLibres());
        assertEquals(6, bord.getPrets().size(), "2 poules de 3 : 6 parties");
        assertTrue(bord.getBloques().isEmpty());
        assertTrue(bord.getAnomalies().isEmpty());
        TableauDeBordSalle.MatchSalle premier = bord.getPrets().get(0);
        assertEquals("A", premier.tableau());
        assertTrue(premier.tour().startsWith("Poule "), premier.tour());
        assertTrue(premier.tour().contains("· tour "), premier.tour());
        assertEquals(PhaseMatch.POULE, premier.phase());
        assertFalse(premier.explication().isEmpty());
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
    void affectation_manuelle_valide_occupe_la_table_et_bloque_les_autres_parties_des_joueurs() {
        Tournoi t = tournoiSixJoueurs(4);
        Match m = poule(t, 0).getMatchs().get(0);

        ResultatAffectationTable r = salle.affecter(t, m, 3);

        assertEquals(ResultatAffectationTable.Motif.AFFECTE, r.getMotif());
        assertEquals(StatutMatch.EN_COURS, m.getStatut());
        assertEquals(Integer.valueOf(3), m.getNumeroTable());
        assertEquals(dixHeuresPlus(0), m.getInstantLancement());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertSame(m, bord.table(3).match().match());
        assertEquals(Integer.valueOf(3), bord.tableDe(m));
        assertEquals(3, bord.nombreTablesLibres());
        // Poule de 3 : les deux autres parties partagent un joueur avec celle en cours.
        assertEquals(2, bord.getBloques().size());
        for (TableauDeBordSalle.MatchBloque b : bord.getBloques()) {
            assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, b.motif().getMotif());
            assertEquals(Integer.valueOf(3), b.motif().getTableDuJoueurBloquant());
            Joueur bloquant = b.motif().getJoueurBloquant();
            assertTrue(bloquant.equals(m.getJoueurA()) || bloquant.equals(m.getJoueurB()));
        }
        assertEquals(3, bord.getPrets().size(), "l'autre poule reste jouable");
    }

    @Test
    void affectation_refusee_joueur_occupe_table_occupee_hors_limites_ou_match_non_pret() {
        Tournoi t = tournoiSixJoueurs(4);
        List<Match> p1 = poule(t, 0).getMatchs();
        List<Match> p2 = poule(t, 1).getMatchs();
        assertTrue(salle.affecter(t, p1.get(0), 1).estReussi());

        ResultatAffectationTable joueurOccupe = salle.affecter(t, p1.get(1), 2);
        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, joueurOccupe.getMotif());
        assertEquals(Integer.valueOf(1), joueurOccupe.getTableDuJoueurBloquant());
        assertEquals(StatutMatch.A_JOUER, p1.get(1).getStatut());

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, salle.affecter(t, p2.get(0), 1).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, salle.affecter(t, p2.get(0), 0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, salle.affecter(t, p2.get(0), 5).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS, salle.affecter(t, p1.get(0), 2).getMotif());

        p2.get(0).definirScore(troisZero());
        assertEquals(ResultatAffectationTable.Motif.MATCH_TERMINE, salle.affecter(t, p2.get(0), 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_INCONNU,
                salle.affecter(t, new Match(j("X1"), j("X2")), 2).getMotif());

        t.changerStatut(StatutTournoi.ANNULE);
        assertEquals(ResultatAffectationTable.Motif.TOURNOI_FERME, salle.affecter(t, p2.get(1), 2).getMotif());
        assertEquals(ResultatPropositionSalle.Motif.TOURNOI_FERME, salle.proposer(t).getMotif());
    }

    @Test
    void joueur_engage_sur_deux_tableaux_jamais_sur_deux_tables() {
        Tournoi t = tournoiSixJoueurs(6);
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L1"); // même licence que dans le tableau A
        lancer(t, b, List.of(commun, j("M2"), j("M3")));

        Match deA = null;
        for (Poule p : tab(t, "A").getPoules()) {
            for (Match m : p.getMatchs()) {
                if (deA == null && (m.getJoueurA().equals(commun) || m.getJoueurB().equals(commun))) {
                    deA = m;
                }
            }
        }
        assertNotNull(deA);
        assertTrue(salle.affecter(t, deA, 2).estReussi());

        Match deB = null;
        for (Match m : b.getPoules().get(0).getMatchs()) {
            if (m.getJoueurA().equals(commun) || m.getJoueurB().equals(commun)) {
                deB = m;
            }
        }
        ResultatAffectationTable r = salle.affecter(t, deB, 5);
        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertEquals(commun, r.getJoueurBloquant());
        assertEquals(Integer.valueOf(2), r.getTableDuJoueurBloquant());

        // « Proposer » non plus ne le met pas sur une deuxième table.
        salle.proposer(t);
        licencesEnJeu(salle.tableauDeBord(t));
    }

    // ------------------------------------------------------------------
    // Bouton « Proposer »
    // ------------------------------------------------------------------

    @Test
    void proposer_remplit_les_tables_libres_sans_doublon_de_joueur() {
        Tournoi t = tournoiSixJoueurs(6);

        ResultatPropositionSalle r = salle.proposer(t);

        assertTrue(r.aLance());
        // Deux poules de 3 : une seule partie par poule peut se jouer à la fois.
        assertEquals(2, r.getLancements().size());
        assertEquals(List.of(1, 2), r.getLancements().stream().map(ResultatPropositionSalle.Lancement::table).toList());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(4, licencesEnJeu(bord).size());
        assertEquals(4, bord.nombreTablesLibres());
        assertTrue(bord.getPrets().isEmpty());
        assertEquals(4, bord.getBloques().size());

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
    void prets_tries_par_priorite_le_joueur_engage_ailleurs_passe_devant() {
        Tournoi t = tournoiSixJoueurs(6);
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L4");
        lancer(t, b, List.of(commun, j("M2"), j("M3")));

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        List<TableauDeBordSalle.MatchSalle> prets = bord.getPrets();
        for (int i = 1; i < prets.size(); i++) {
            assertTrue(prets.get(i - 1).priorite() >= prets.get(i).priorite(), "tri décroissant");
        }
        Match premier = prets.get(0).match();
        assertTrue(premier.getJoueurA().equals(commun) || premier.getJoueurB().equals(commun),
                "un joueur engagé dans deux tableaux débloque plus de parties");
        assertTrue(prets.get(0).explication().contains("débloque"), prets.get(0).explication());
    }

    @Test
    void l_attente_compte_depuis_la_fin_du_dernier_match_des_joueurs() {
        Tournoi t = tournoiSixJoueurs(6);
        List<Match> p1 = poule(t, 0).getMatchs();
        List<Match> p2 = poule(t, 1).getMatchs();
        // Poule 1 : première partie jouée et terminée à 10 h 05.
        salle.affecter(t, p1.get(0), 1);
        avancer(5);
        new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), p1.get(0), troisZero());
        salle.libererApresResultat(t, p1.get(0));
        // Poule 2 : première partie lancée à 10 h 05, terminée à 10 h 25.
        salle.affecter(t, p2.get(0), 2);
        avancer(20);
        new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), p2.get(0), troisZero());
        salle.libererApresResultat(t, p2.get(0));

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        // Les parties de la poule 1 attendent depuis 10 h 05, celles de la poule 2 depuis 10 h 25.
        assertSame(poule(t, 0), pouleDe(t, bord.getPrets().get(0).match()));
        assertTrue(bord.getPrets().get(0).explication().contains("attend 20 min"),
                bord.getPrets().get(0).explication());
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
    void terminer_libere_la_table_et_debloque_la_suite_de_la_poule() {
        Tournoi t = tournoiSixJoueurs(4);
        Match m = poule(t, 0).getMatchs().get(0);
        salle.affecter(t, m, 2);
        assertEquals(ResultatAffectationTable.Motif.RESULTAT_NON_SAISI, salle.libererApresResultat(t, m).getMotif());
        avancer(18);

        ResultatSaisieMatch saisie = new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), m, troisZero());
        assertTrue(saisie.estEnregistre());
        ResultatAffectationTable r = salle.libererApresResultat(t, m);

        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, r.getMotif());
        assertEquals(2, r.getNumeroTable());
        assertEquals(dixHeuresPlus(18), m.getInstantFin());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(4, bord.nombreTablesLibres());
        assertTrue(bord.getBloques().isEmpty());
        assertEquals(5, bord.getPrets().size());
        assertTrue(bord.getPrets().stream().noneMatch(p -> p.match() == m));
        assertEquals(1, SuiviPoule.matchsTermines(poule(t, 0)), "la poule voit le résultat");
        assertEquals(Integer.valueOf(2), m.getNumeroTable(), "la table jouée reste notée sur le match");
    }

    @Test
    void abandon_en_cours_de_match_libere_aussi_la_table() {
        Tournoi t = tournoiSixJoueurs(2);
        Match m = poule(t, 1).getMatchs().get(0);
        salle.affecter(t, m, 1);
        avancer(7);
        new SaisieResultatMatch(horloge).enregistrerIssue(tab(t, "A"), m, IssueMatch.ABANDON, m.getJoueurB());
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, salle.libererApresResultat(t, m).getMotif());
        assertTrue(salle.tableauDeBord(t).table(1).estLibre());
    }

    @Test
    void un_score_saisi_depuis_l_ecran_des_poules_libere_la_table_sans_autre_action() {
        Tournoi t = tournoiSixJoueurs(2);
        Match m = poule(t, 0).getMatchs().get(0);
        salle.affecter(t, m, 2);
        new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), m, troisZero());
        assertTrue(salle.tableauDeBord(t).table(2).estLibre());
    }

    @Test
    void annuler_une_affectation_erronee() {
        Tournoi t = tournoiSixJoueurs(3);
        Match m = poule(t, 0).getMatchs().get(0);
        salle.affecter(t, m, 3);

        ResultatAffectationTable r = salle.annulerAffectation(t, m);

        assertEquals(ResultatAffectationTable.Motif.AFFECTATION_ANNULEE, r.getMotif());
        assertEquals(3, r.getNumeroTable());
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertNull(m.getNumeroTable());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(3, bord.nombreTablesLibres());
        assertEquals(6, bord.getPrets().size());
        assertEquals(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS, salle.annulerAffectation(t, m).getMotif());
    }

    // ------------------------------------------------------------------
    // Durée écoulée
    // ------------------------------------------------------------------

    @Test
    void duree_ecoulee_selon_l_horloge() {
        Tournoi t = tournoiSixJoueurs(2);
        Match m = poule(t, 0).getMatchs().get(0);
        salle.affecter(t, m, 1);
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
        Match a = poule(t, 1).getMatchs().get(0);
        salle.affecter(t, a, 1);
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
