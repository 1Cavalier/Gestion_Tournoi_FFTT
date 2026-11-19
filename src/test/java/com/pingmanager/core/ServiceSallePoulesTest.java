package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase de poules : on lance une poule entière sur une table, pas un match.
 * I.301 : les matchs se jouent à la suite dans l'ordre officiel déjà calculé par
 * {@link GenerateurMatchsPoule} (poule de 3 à deux qualifiés : 1-3, 1-2, 2-3).
 */
class ServiceSallePoulesTest {

    private Instant instant = Instant.parse("2026-11-14T10:00:00Z");
    private final Horloge horloge = () -> instant;
    private final ServiceSalle salle = new ServiceSalle(horloge);
    private final SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);

    private void avancer(int minutes) {
        instant = instant.plusSeconds(60L * minutes);
    }

    private static LocalDateTime local(Instant i) {
        return LocalDateTime.ofInstant(i, ZoneId.systemDefault());
    }

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    private static final List<Manche> TROIS_ZERO = List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5));

    /** Tableau « A » de six joueurs (deux poules de 3, deux qualifiés), lancé, dans un tournoi de {@code tables} tables. */
    private static Tournoi tournoi(int tables, Joueur... joueurs) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(a);
        lancer(t, a, List.of(joueurs));
        return t;
    }

    static void lancer(Tournoi t, Tableau tab, List<Joueur> joueurs) {
        tab.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 1L);
    }

    private static Tournoi six(int tables) {
        return tournoi(tables, j("L1", 1600), j("L2", 1500), j("L3", 1400), j("L4", 1300), j("L5", 1200),
                j("L6", 1100));
    }

    private static Tableau tab(Tournoi t) {
        return t.getTableaux().get(0);
    }

    private static Poule poule(Tournoi t, int i) {
        return tab(t).getPoules().get(i);
    }

    @Test
    void poule_de_3_lancee_sur_une_table_ses_trois_matchs_dans_l_ordre_1_3_1_2_2_3() {
        Tournoi t = six(4);
        Poule p = poule(t, 0);
        assertEquals(3, p.getTaille());

        ResultatAffectationTable r = salle.lancerPoule(t, p, 2);

        assertTrue(r.estReussi(), r.toString());
        assertEquals(Integer.valueOf(2), p.getNumeroTable());
        assertEquals(local(instant), p.getInstantLancement());
        // I.301.2 : poule de 3 à deux qualifiés, 1-3 puis 1-2 puis 2-3 (1 = tête de poule).
        List<Joueur> js = p.getJoueurs();
        int[][] attendu = {{1, 3}, {1, 2}, {2, 3}};
        for (int i = 0; i < 3; i++) {
            Match m = p.getMatchs().get(i);
            assertEquals(js.get(attendu[i][0] - 1), m.getJoueurA(), "match " + (i + 1));
            assertEquals(js.get(attendu[i][1] - 1), m.getJoueurB(), "match " + (i + 1));
            assertEquals(StatutMatch.EN_COURS, m.getStatut());
            assertEquals(Integer.valueOf(2), m.getNumeroTable());
        }
        // La carte de la table montre la poule et son premier match (1-3).
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertTrue(bord.table(2).estPoule());
        assertSame(p.getMatchs().get(0), bord.table(2).match().match());

        // Les matchs se jouent à la suite : le match affiché suit l'ordre I.301.
        avancer(15);
        assertTrue(saisie.enregistrerScore(tab(t), p.getMatchs().get(0), TROIS_ZERO).estEnregistre());
        assertSame(p.getMatchs().get(1), salle.tableauDeBord(t).table(2).match().match());
        avancer(15);
        assertTrue(saisie.enregistrerScore(tab(t), p.getMatchs().get(1), TROIS_ZERO).estEnregistre());
        assertSame(p.getMatchs().get(2), salle.tableauDeBord(t).table(2).match().match());
    }

    @Test
    void un_joueur_d_une_poule_lancee_ne_peut_pas_etre_lance_ailleurs_avant_la_fin_de_sa_poule() {
        // Deux tableaux : L1 joue aussi le tableau B.
        Tournoi t = six(6);
        Tableau b = new Tableau("B", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(b);
        Poule p = poule(t, 0);
        Joueur joueurDeLaPoule = p.getJoueurs().get(0);
        lancer(t, b, List.of(joueurDeLaPoule, j("M2", 900), j("M3", 800)));
        Poule pouleB = b.getPoules().get(0);

        assertTrue(salle.lancerPoule(t, p, 1).estReussi());
        // Entre deux de ses matchs (son premier est fini), il reste occupé par sa poule.
        Match sonMatch = p.getMatchs().stream().filter(m -> m.getJoueurA() == joueurDeLaPoule
                || m.getJoueurB() == joueurDeLaPoule).findFirst().orElseThrow();
        assertTrue(saisie.enregistrerScore(tab(t), sonMatch, TROIS_ZERO).estEnregistre());

        ResultatAffectationTable r = salle.lancerPoule(t, pouleB, 3);
        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertSame(joueurDeLaPoule, r.getJoueurBloquant());
        assertEquals(Integer.valueOf(1), r.getTableDuJoueurBloquant(), "« … joue en table 1 »");
        assertNull(pouleB.getNumeroTable(), "rien n'est modifié");
        // Ni une poule seule, ni la liste « Lancer un match » ne le propose.
        assertTrue(salle.listeLancements(t).getJouables().stream().noneMatch(l -> l.estPoule()
                && l.poule().poule() == pouleB));

        // Fin de sa poule : il redevient libre.
        for (Match m : p.getMatchs()) {
            if (m.getStatut() != StatutMatch.TERMINE) {
                assertTrue(saisie.enregistrerScore(tab(t), m, TROIS_ZERO).estEnregistre());
            }
        }
        assertTrue(salle.lancerPoule(t, pouleB, 3).estReussi());
    }

    @Test
    void un_match_de_poule_ne_se_lance_plus_seul() {
        Tournoi t = six(4);
        Match m = poule(t, 0).getMatchs().get(0);

        ResultatAffectationTable r = salle.affecter(t, m, 1);

        assertEquals(ResultatAffectationTable.Motif.LANCEMENT_PAR_POULE, r.getMotif());
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
    }

    @Test
    void la_table_se_libere_apres_le_dernier_resultat_saisi_un_par_un() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        assertTrue(salle.lancerPoule(t, p, 1).estReussi());
        Tableau a = tab(t);

        assertTrue(saisie.enregistrerScore(a, p.getMatchs().get(0), TROIS_ZERO).estEnregistre());
        assertEquals(ResultatAffectationTable.Motif.POULE_CONTINUE,
                salle.libererApresResultat(t, p.getMatchs().get(0)).getMotif());
        assertFalse(salle.tableauDeBord(t).table(1).estLibre(), "la poule continue sur sa table");
        // Forfait (I.303 a) : un résultat comme un autre.
        Match deux = p.getMatchs().get(1);
        assertTrue(saisie.enregistrerIssue(a, deux, IssueMatch.FORFAIT, deux.getJoueurB()).estEnregistre());
        assertFalse(salle.tableauDeBord(t).table(1).estLibre());
        Match dernier = p.getMatchs().get(2);
        assertTrue(saisie.enregistrerScore(a, dernier, TROIS_ZERO).estEnregistre());

        ResultatAffectationTable r = salle.libererApresResultat(t, dernier);
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, r.getMotif());
        assertEquals(1, r.getNumeroTable());
        assertTrue(salle.tableauDeBord(t).table(1).estLibre());
        assertFalse(p.estLanceeEnBloc(), "libérée au dernier résultat");
        // Joueurs libres : la seconde poule peut partir sur la table 1.
        assertTrue(salle.lancerPoule(t, poule(t, 1), 1).estReussi());
    }

    @Test
    void la_table_se_libere_apres_la_saisie_de_la_poule_entiere_d_un_coup() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        assertTrue(salle.lancerPoule(t, p, 2).estReussi());
        Tableau a = tab(t);
        avancer(10);
        assertTrue(saisie.enregistrerScore(a, p.getMatchs().get(0), TROIS_ZERO).estEnregistre());

        // Retour de la feuille : tout d'un coup, le premier score repris et corrigé.
        avancer(40);
        List<Manche> corrige = List.of(new Manche(11, 9), new Manche(11, 9), new Manche(11, 9));
        SaisiePoule.Resultat r = new SaisiePoule(saisie).enregistrer(a, p, List.of(
                SaisiePoule.Partie.score(p.getMatchs().get(0), corrige),
                SaisiePoule.Partie.score(p.getMatchs().get(1), TROIS_ZERO),
                SaisiePoule.Partie.issue(p.getMatchs().get(2), IssueMatch.ABANDON, p.getMatchs().get(2).getJoueurA())));

        assertTrue(r.estEnregistre(), r.toString());
        assertEquals(3, r.enregistres());
        assertTrue(r.liberee());
        assertEquals(corrige, p.getMatchs().get(0).getManches());
        assertTrue(p.estTerminee());
        assertTrue(salle.tableauDeBord(t).table(2).estLibre());
        assertFalse(p.estLanceeEnBloc());
    }

    @Test
    void saisie_d_un_coup_tout_ou_rien_le_refus_dit_quelle_partie_corriger() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        assertTrue(salle.lancerPoule(t, p, 1).estReussi());

        SaisiePoule.Resultat r = new SaisiePoule(saisie).enregistrer(tab(t), p, List.of(
                SaisiePoule.Partie.score(p.getMatchs().get(0), TROIS_ZERO),
                SaisiePoule.Partie.score(p.getMatchs().get(1), List.of(new Manche(11, 10)))));

        assertFalse(r.estEnregistre());
        assertEquals(2, r.partie());
        assertEquals(ResultatSaisieMatch.Motif.MANCHE_INVALIDE, r.detail().getMotif());
        assertEquals(StatutMatch.EN_COURS, p.getMatchs().get(0).getStatut(), "rien n'est enregistré");
    }

    @Test
    void saisie_d_un_coup_refusee_pour_une_poule_pas_lancee() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        SaisiePoule.Resultat r = new SaisiePoule(saisie).enregistrer(tab(t), p,
                List.of(SaisiePoule.Partie.score(p.getMatchs().get(0), TROIS_ZERO)));
        assertEquals(ResultatSaisieMatch.Motif.MATCH_NON_LANCE, r.detail().getMotif());
        assertEquals(1, r.partie());
    }

    @Test
    void lancement_refuse_table_occupee_aucune_table_libre_ou_hors_limites() {
        Tournoi t = six(1);
        assertTrue(salle.lancerPoule(t, poule(t, 0), 1).estReussi());

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, salle.lancerPoule(t, poule(t, 1), 1).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, salle.lancerPoule(t, poule(t, 1), 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS,
                salle.verifierLancementPoule(t, poule(t, 0), 1).getMotif());
        ListeLancements.Ligne l = salle.listeLancements(t).getJouables().stream().filter(ListeLancements.Ligne::estPoule)
                .findFirst().orElseThrow();
        assertEquals(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, l.refus().getMotif());
    }

    @Test
    void joueur_non_pointe_la_poule_ne_part_pas() {
        Tournoi t = six(4);
        Poule p = poule(t, 0);
        Joueur absent = p.getJoueurs().get(2);
        tab(t).depointer(absent);

        ResultatAffectationTable r = salle.lancerPoule(t, p, 1);

        assertEquals(ResultatAffectationTable.Motif.JOUEUR_ABSENT, r.getMotif());
        assertSame(absent, r.getJoueurBloquant());
    }

    @Test
    void annulation_possible_tant_qu_aucun_score_n_est_saisi_ensuite_le_lancement_reste() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        assertTrue(salle.lancerPoule(t, p, 1).estReussi());

        assertEquals(ResultatAffectationTable.Motif.AFFECTATION_ANNULEE, salle.annulerLancementPoule(t, p).getMotif());
        assertFalse(p.estLanceeEnBloc());
        assertTrue(p.getMatchs().stream().allMatch(m -> m.getStatut() == StatutMatch.A_JOUER
                && m.getNumeroTable() == null && m.getInstantLancement() == null));
        assertTrue(salle.tableauDeBord(t).table(1).estLibre());

        assertTrue(salle.lancerPoule(t, p, 1).estReussi());
        assertTrue(saisie.enregistrerScore(tab(t), p.getMatchs().get(0), TROIS_ZERO).estEnregistre());
        assertEquals(ResultatAffectationTable.Motif.SCORE_DEJA_SAISI, salle.annulerLancementPoule(t, p).getMotif());
        // Même refus par le bouton « Annuler » de la table (annulation du match en jeu).
        assertEquals(ResultatAffectationTable.Motif.SCORE_DEJA_SAISI,
                salle.annulerAffectation(t, p.getMatchs().get(1)).getMotif());
        assertTrue(p.estLanceeEnBloc());
    }

    @Test
    void un_resultat_efface_pendant_la_poule_remet_le_match_en_cours_sur_sa_table() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        assertTrue(salle.lancerPoule(t, p, 2).estReussi());
        Match m = p.getMatchs().get(0);
        assertTrue(saisie.enregistrerScore(tab(t), m, TROIS_ZERO).estEnregistre());

        assertTrue(saisie.effacer(tab(t), m).estEnregistre());

        assertEquals(StatutMatch.EN_COURS, m.getStatut());
        assertEquals(Integer.valueOf(2), m.getNumeroTable());
        assertTrue(SaisieResultatMatch.scoreSaisissable(m), "compte comme lancé : pas de score sans lancement");
    }

    @Test
    void un_match_de_poule_lancee_compte_comme_lance_pour_la_saisie() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        assertFalse(SaisieResultatMatch.scoreSaisissable(p.getMatchs().get(2)));
        assertEquals(ResultatSaisieMatch.Motif.MATCH_NON_LANCE,
                saisie.enregistrerScore(tab(t), p.getMatchs().get(2), TROIS_ZERO).getMotif());

        assertTrue(salle.lancerPoule(t, p, 1).estReussi());

        // Le troisième match (2-3), pas encore joué sur la table, reçoit déjà son score.
        assertTrue(saisie.enregistrerScore(tab(t), p.getMatchs().get(2), TROIS_ZERO).estEnregistre());
    }

    @Test
    void duree_prevue_somme_des_matchs_et_debut_de_chaque_match_a_la_fin_du_precedent() {
        Tournoi t = six(2);
        Poule p = poule(t, 0);
        PouleALancer vue = salle.listeLancements(t).getJouables().stream().filter(ListeLancements.Ligne::estPoule)
                .map(ListeLancements.Ligne::poule).filter(x -> x.poule() == p).findFirst().orElseThrow();
        Duration partie = salle.dureePrevuePartie(t, com.pingmanager.ordonnancement.PhaseMatch.POULE);
        assertEquals(ServiceSalle.DUREE_PARTIE_PAR_DEFAUT, partie);
        assertEquals(partie.multipliedBy(3), vue.dureePrevue(), "somme des durées prévues de ses 3 matchs");
        assertEquals(3, vue.nombreMatchs());

        LocalDateTime lancement = local(instant);
        assertTrue(salle.lancerPoule(t, p, 1).estReussi());
        assertEquals(lancement.plus(partie.multipliedBy(3)), salle.poule(t, p).orElseThrow().finEstimee());
        avancer(12);
        assertTrue(saisie.enregistrerScore(tab(t), p.getMatchs().get(0), TROIS_ZERO).estEnregistre());
        avancer(18);
        assertTrue(saisie.enregistrerScore(tab(t), p.getMatchs().get(1), TROIS_ZERO).estEnregistre());

        // Apprentissage match par match : 12 min puis 18 min (le second commence à la fin du premier).
        assertEquals(lancement, p.getMatchs().get(0).getInstantLancement());
        assertEquals(lancement.plusMinutes(12), p.getMatchs().get(1).getInstantLancement());
        Duration apprise = salle.dureePrevuePartie(t, com.pingmanager.ordonnancement.PhaseMatch.POULE);
        // Fin estimée : dernier résultat + durée prévue du match restant.
        assertEquals(lancement.plusMinutes(30).plus(apprise), salle.poule(t, p).orElseThrow().finEstimee());
    }

    @Test
    void poules_a_lancer_et_matchs_du_tableau_final_cohabitent_dans_les_propositions() {
        Tournoi t = six(8);
        // Tableau F : poules jouées, tableau final construit (à élimination directe).
        Tableau f = new Tableau("F", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(f);
        lancer(t, f, List.of(j("F1", 900), j("F2", 890), j("F3", 880), j("F4", 870), j("F5", 860), j("F6", 850)));
        for (Poule p : f.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(TROIS_ZERO);
            }
        }
        assertTrue(new ServicePhaseFinale().construire(f, 3L).estConstruit());

        ListeLancements liste = salle.listeLancements(t);

        assertEquals(2, liste.getJouables().stream().filter(ListeLancements.Ligne::estPoule).count(),
                "les deux poules du tableau A");
        assertTrue(liste.getJouables().stream().anyMatch(l -> !l.estPoule() && "F".equals(l.match().tableau())),
                "un match du tableau final de F");
        // Le tableau final se lance toujours match par match.
        ListeLancements.Ligne finale = liste.getJouables().stream().filter(l -> !l.estPoule()).findFirst()
                .orElseThrow();
        assertTrue(salle.affecter(t, finale.match().match(), finale.tableProposee()).estReussi());
        // « Proposer » : les poules partent en bloc, sans doublon de joueur.
        ResultatPropositionSalle r = salle.proposer(t);
        assertTrue(r.getLancements().size() >= 2);
        assertTrue(poule(t, 0).estLanceeEnBloc());
        assertTrue(poule(t, 1).estLanceeEnBloc());
    }

    @Test
    void poule_a_moitie_jouee_match_par_match_dans_un_ancien_fichier_continue_sans_erreur() {
        Tournoi t = six(4);
        Poule p = poule(t, 0);
        // Ancien fichier : premier match joué seul, deuxième en cours seul en table 3.
        Match premier = p.getMatchs().get(0);
        premier.setStatut(StatutMatch.EN_COURS);
        premier.setNumeroTable(1);
        premier.setInstantLancement(local(instant.minusSeconds(3600)));
        premier.definirScore(TROIS_ZERO);
        premier.setInstantFin(local(instant.minusSeconds(2400)));
        Match deuxieme = p.getMatchs().get(1);
        deuxieme.setStatut(StatutMatch.EN_COURS);
        deuxieme.setNumeroTable(3);
        deuxieme.setInstantLancement(local(instant.minusSeconds(600)));

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertTrue(bord.getAnomalies().isEmpty());
        assertFalse(bord.table(3).estLibre(), "le match lancé seul garde sa table");
        assertFalse(bord.table(3).estPoule());
        // Ses joueurs jouent : la poule ne part pas encore.
        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, salle.lancerPoule(t, p, 2).getMotif());

        assertTrue(saisie.enregistrerScore(tab(t), deuxieme, TROIS_ZERO).estEnregistre());
        assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE, salle.libererApresResultat(t, deuxieme).getMotif());
        // Le reste de la poule part en bloc, puis se termine.
        assertTrue(salle.lancerPoule(t, p, 2).estReussi());
        assertEquals(StatutMatch.TERMINE, premier.getStatut(), "les matchs joués ne sont pas relancés");
        Match dernier = p.getMatchs().get(2);
        assertEquals(StatutMatch.EN_COURS, dernier.getStatut());
        assertTrue(saisie.enregistrerScore(tab(t), dernier, TROIS_ZERO).estEnregistre());
        assertTrue(p.estTerminee());
        assertTrue(salle.tableauDeBord(t).table(2).estLibre());
    }

    @Test
    void liste_de_lancement_montre_la_poule_joueurs_dans_l_ordre_et_nombre_de_matchs() {
        Tournoi t = six(4);
        ListeLancements liste = salle.listeLancements(t);
        ListeLancements.Ligne l = liste.propositionApres(null).orElseThrow();

        assertTrue(l.estPoule(), "phase de poules : la proposition est une poule");
        PouleALancer p = l.poule();
        assertEquals("A", p.tableau());
        assertEquals(3, p.joueurs().size());
        assertSame(p.poule().getJoueurs().get(0), p.joueurs().get(0), "ordre de la poule (1 = tête de poule)");
        assertEquals(1, p.numero(p.joueurs().get(0)));
        assertEquals(3, p.nombreMatchs());
        assertEquals("Poule " + p.poule().getNom(), l.echelon());
        assertNotNull(l.tableProposee());
        assertEquals(List.of(1, 2, 3, 4), l.tablesPossibles());
        // Le bouton « Autre » passe à la seconde poule.
        assertNotSame(p.poule(), liste.propositionApres(l.match().match()).orElseThrow().poule().poule());
    }
}
