package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Interface « Lancer un match » (B055) : liste des matchs proposés et du choix
 * manuel calculée par le cœur, et règle qui ne se contourne jamais — les deux
 * joueurs disponibles, la table libre — vérifiée par {@link ServiceSalle}.
 */
class ServiceSalleLancementTest {

    private Instant instant = Instant.parse("2026-11-14T10:00:00Z");
    private final ServiceSalle salle = new ServiceSalle(() -> instant);

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    /** Tournoi de {@code tables} tables, un tableau « A » de six joueurs : deux poules de trois. */
    private static Tournoi tournoiSixJoueurs(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (String l : List.of("L1", "L2", "L3", "L4", "L5", "L6")) {
            Joueur joueur = j(l);
            a.inscrire(joueur);
            a.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(a);
        new ServiceLancementTableau(t).lancer(a, 1L);
        return t;
    }

    private static Poule poule(Tournoi t, int i) {
        return t.getTableaux().get(0).getPoules().get(i);
    }

    private static ListeLancements.Ligne ligne(ListeLancements liste, Match m) {
        return liste.getProposes().stream().filter(l -> l.match().match() == m).findFirst().orElseThrow();
    }

    /** Ligne d'une poule (phase de poules : on lance une poule, pas un match). */
    private static ListeLancements.Ligne ligne(ListeLancements liste, Poule p) {
        return liste.getProposes().stream().filter(l -> l.estPoule() && l.poule().poule() == p).findFirst()
                .orElseThrow();
    }

    /** Le tournoi à six joueurs, plus un tableau « B » (L1, M2, M3) : L1 joue deux tableaux. */
    private static Tournoi tournoiAvecB(int tables) {
        Tournoi t = tournoiSixJoueurs(tables);
        Tableau b = new Tableau("B", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(b);
        ServiceSallePoulesTest.lancer(t, b, List.of(j("L1"), j("M2"), j("M3")));
        return t;
    }

    private static Poule pouleDeL1(Tournoi t) {
        return t.getTableaux().get(0).getPoules().stream().filter(p -> p.getJoueurs().contains(j("L1")))
                .findFirst().orElseThrow();
    }

    private static Poule pouleB(Tournoi t) {
        return t.getTableaux().get(1).getPoules().get(0);
    }

    @Test
    void liste_par_priorite_avec_table_proposee_et_tables_libres_possibles() {
        Tournoi t = tournoiSixJoueurs(4);

        ListeLancements liste = salle.listeLancements(t);

        assertEquals(2, liste.getProposes().size(), "2 poules de 3 : 2 poules à lancer");
        assertEquals(2, liste.getParTableau().size());
        assertEquals(List.of(1, 2, 3, 4), liste.getTablesLibres());
        assertEquals(2, liste.nombreLancables());
        ListeLancements.Ligne premiere = liste.getProposes().get(0);
        assertTrue(premiere.propose(), "la première est celle que la stratégie enverrait");
        assertTrue(premiere.estLancable());
        assertEquals(List.of(1, 2, 3, 4), premiere.tablesPossibles());
        assertNotNull(premiere.tableProposee());
        // Priorité de l'ordonnanceur, décroissante.
        for (int i = 1; i < liste.getProposes().size(); i++) {
            assertTrue(liste.getProposes().get(i - 1).match().priorite() >= liste.getProposes().get(i).match().priorite());
        }
        // Les tables proposées par la stratégie ne servent qu'une fois.
        List<Integer> tablesStrategie = liste.getProposes().stream().filter(ListeLancements.Ligne::propose)
                .map(ListeLancements.Ligne::tableProposee).toList();
        assertEquals(tablesStrategie.size(), tablesStrategie.stream().distinct().count());
    }


    @Test
    void lancement_avec_un_joueur_occupe_refuse_avec_son_motif() {
        Tournoi t = tournoiAvecB(4);
        assertTrue(salle.lancerPoule(t, pouleDeL1(t), 4).estReussi());
        Poule memeJoueur = pouleB(t);

        ResultatAffectationTable verification = salle.verifierLancementPoule(t, memeJoueur, 1);
        ResultatAffectationTable r = salle.lancerPoule(t, memeJoueur, 1);

        for (ResultatAffectationTable x : List.of(verification, r)) {
            assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, x.getMotif());
            assertEquals(Integer.valueOf(4), x.getTableDuJoueurBloquant());
            assertEquals(j("L1"), x.getJoueurBloquant());
        }
        assertFalse(memeJoueur.estLanceeEnBloc(), "rien n'est modifié en cas de refus");
        assertTrue(memeJoueur.getMatchs().stream().allMatch(m -> m.getStatut() == StatutMatch.A_JOUER
                && m.getNumeroTable() == null));
        // Dans la liste, la poule reste visible, désactivée, avec le même motif.
        ListeLancements.Ligne l = ligne(salle.listeLancements(t), memeJoueur);
        assertFalse(l.estLancable());
        assertFalse(l.estPret());
        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, l.refus().getMotif());
        assertEquals(Integer.valueOf(4), l.refus().getTableDuJoueurBloquant());
        assertNull(l.tableProposee());
        assertTrue(l.tablesPossibles().isEmpty());
    }


    @Test
    void verifier_un_lancement_ne_modifie_rien() {
        Tournoi t = tournoiSixJoueurs(4);
        Poule p = poule(t, 1);

        ResultatAffectationTable r = salle.verifierLancementPoule(t, p, 2);

        assertTrue(r.estReussi());
        assertEquals(2, r.getNumeroTable());
        assertFalse(p.estLanceeEnBloc());
        assertEquals(StatutMatch.A_JOUER, p.getMatchs().get(0).getStatut());
        assertTrue(salle.tableauDeBord(t).table(2).estLibre());
    }


    @Test
    void table_occupee_refusee_en_manuel_meme_si_les_joueurs_sont_libres() {
        Tournoi t = tournoiSixJoueurs(4);
        assertTrue(salle.lancerPoule(t, poule(t, 0), 2).estReussi());
        Poule autrePoule = poule(t, 1);

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE,
                salle.verifierLancementPoule(t, autrePoule, 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, salle.lancerPoule(t, autrePoule, 2).getMotif());
        ListeLancements.Ligne l = ligne(salle.listeLancements(t), autrePoule);
        assertEquals(List.of(1, 3, 4), l.tablesPossibles(), "la table 2 n'est plus proposée");
        assertNotEquals(Integer.valueOf(2), l.tableProposee());
    }


    @Test
    void aucune_table_libre_la_poule_reste_visible_avec_son_motif() {
        Tournoi t = tournoiSixJoueurs(1);
        assertTrue(salle.lancerPoule(t, poule(t, 0), 1).estReussi());

        ListeLancements liste = salle.listeLancements(t);

        assertTrue(liste.getTablesLibres().isEmpty());
        assertEquals(0, liste.nombreLancables());
        assertEquals(1, liste.nombrePrets(), "l'autre poule est prête, il ne manque qu'une table");
        ListeLancements.Ligne l = ligne(liste, poule(t, 1));
        assertFalse(l.estLancable());
        assertEquals(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, l.refus().getMotif());
        assertNotNull(l.attenteDepuis(), "le temps d'attente reste affiché");
    }


    @Test
    void lancer_la_proposition_puis_la_liste_se_met_a_jour() {
        Tournoi t = tournoiAvecB(4);
        ListeLancements.Ligne premiere = salle.listeLancements(t).getProposes().get(0);
        assertTrue(premiere.estPoule());
        Poule p = premiere.poule().poule();

        ResultatAffectationTable r = salle.lancerPoule(t, p, premiere.tableProposee());

        assertTrue(r.estReussi());
        assertEquals(premiere.tableProposee(), p.getNumeroTable());
        ListeLancements apres = salle.listeLancements(t);
        assertEquals(2, apres.getProposes().size(), "la poule lancée quitte la liste");
        assertFalse(apres.getTablesLibres().contains(premiere.tableProposee()));
        // L1 joue deux tableaux : si sa poule est partie, l'autre est désactivée.
        assertEquals(p.getJoueurs().contains(j("L1")) ? 1 : 2, apres.nombreLancables());
    }


    @Test
    void lancement_manuel_d_une_poule_choisie_sur_une_table_choisie() {
        Tournoi t = tournoiSixJoueurs(4);
        ListeLancements liste = salle.listeLancements(t);
        // Ordre du tournoi : la seconde poule, sur la dernière table.
        ListeLancements.Ligne choisie = liste.getParTableau().get(liste.getParTableau().size() - 1);
        assertSame(poule(t, 1), choisie.poule().poule());

        assertTrue(salle.lancerPoule(t, poule(t, 1), 4).estReussi());
        for (Match m : poule(t, 1).getMatchs()) {
            assertEquals(StatutMatch.EN_COURS, m.getStatut());
            assertEquals(Integer.valueOf(4), m.getNumeroTable());
        }
    }


    @Test
    void joueur_d_un_match_en_cours_sans_table_valable_reste_indisponible() {
        Tournoi t = tournoiSixJoueurs(4);
        Match sansTable = poule(t, 0).getMatchs().get(0);
        sansTable.setStatut(StatutMatch.EN_COURS); // table absente : anomalie de reprise (ancien fichier)

        ResultatAffectationTable r = salle.lancerPoule(t, poule(t, 0), 1);

        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertNull(r.getTableDuJoueurBloquant(), "table inconnue");
        assertFalse(poule(t, 0).estLanceeEnBloc());
        assertFalse(ligne(salle.listeLancements(t), poule(t, 0)).estLancable());
        // « Proposer » ne l'envoie pas non plus.
        salle.proposer(t);
        assertFalse(poule(t, 0).estLanceeEnBloc());
        assertEquals(StatutMatch.A_JOUER, poule(t, 0).getMatchs().get(1).getStatut());
    }


    @Test
    void tournoi_annule_ou_salle_sans_table_aucun_lancement() {
        Tournoi t = tournoiSixJoueurs(4);
        t.changerStatut(StatutTournoi.ANNULE);
        ListeLancements liste = salle.listeLancements(t);
        assertEquals(0, liste.nombreLancables());
        assertEquals(ResultatAffectationTable.Motif.TOURNOI_FERME, liste.getProposes().get(0).refus().getMotif());

        Tournoi sansTable = tournoiSixJoueurs(0);
        ListeLancements vide = salle.listeLancements(sansTable);
        assertEquals(0, vide.nombreLancables());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES,
                vide.getProposes().get(0).refus().getMotif());
    }

    // ------------------------------------------------------------------
    // B057 : nouvelle interface « Lancer un match »
    // ------------------------------------------------------------------

    @Test
    void jouables_par_priorite_sans_les_poules_d_un_joueur_qui_joue_ailleurs() {
        Tournoi t = tournoiAvecB(4);
        Poule enJeu = pouleDeL1(t);
        assertTrue(salle.lancerPoule(t, enJeu, 1).estReussi());

        ListeLancements liste = salle.listeLancements(t);
        List<ListeLancements.Ligne> jouables = liste.getJouables();

        assertEquals(1, jouables.size(), "la poule de B attend L1, en jeu dans le tableau A");
        assertTrue(jouables.stream().allMatch(l -> l.poule().joueurs().stream()
                .noneMatch(x -> enJeu.getJoueurs().contains(x))));
        // Même ordre que la liste de l'ordonnanceur : pas de second calcul de priorité.
        List<ListeLancements.Ligne> attendus = liste.getProposes().stream().filter(ListeLancements.Ligne::estPret)
                .toList();
        assertEquals(attendus, jouables);
    }


    @Test
    void autre_passe_a_la_proposition_suivante_puis_revient_a_la_premiere() {
        Tournoi t = tournoiSixJoueurs(4);
        ListeLancements liste = salle.listeLancements(t);
        List<ListeLancements.Ligne> jouables = liste.getJouables();
        assertEquals(2, jouables.size(), "2 poules de 3");

        ListeLancements.Ligne courante = liste.propositionApres(null).orElseThrow();
        assertSame(jouables.get(0), courante, "premier appui : la plus prioritaire");
        for (int i = 1; i < jouables.size(); i++) {
            courante = liste.propositionApres(courante.match().match()).orElseThrow();
            assertSame(jouables.get(i), courante);
        }
        assertSame(jouables.get(0), liste.propositionApres(courante.match().match()).orElseThrow(),
                "après la dernière, retour à la première");
        Match horsListe = new Match(j("X1"), j("X2"));
        assertSame(jouables.get(0), liste.propositionApres(horsListe).orElseThrow(),
                "proposition qui n'est plus jouable : on repart de la plus prioritaire");
    }

    @Test
    void aucune_proposition_sans_match_jouable() {
        Tournoi t = tournoiSixJoueurs(4);
        for (Poule p : t.getTableaux().get(0).getPoules()) {
            for (Match m : p.getMatchs()) {
                m.setStatut(StatutMatch.TERMINE);
            }
        }
        ListeLancements liste = salle.listeLancements(t);
        assertTrue(liste.getJouables().isEmpty());
        assertTrue(liste.propositionApres(null).isEmpty());
    }

    @Test
    void sans_table_libre_les_poules_restent_jouables_mais_pas_lancables() {
        Tournoi t = tournoiSixJoueurs(1);
        assertTrue(salle.lancerPoule(t, poule(t, 0), 1).estReussi());

        ListeLancements liste = salle.listeLancements(t);

        assertEquals(1, liste.getJouables().size());
        assertTrue(liste.getJouables().stream().noneMatch(ListeLancements.Ligne::estLancable));
        assertTrue(liste.getJouables().stream().allMatch(l -> l.tablesPossibles().isEmpty()));
        assertTrue(liste.propositionApres(null).isPresent(), "la proposition s'affiche, « Lancer » est désactivé");
    }


    @Test
    void numero_du_match_dans_son_tableau_et_echelon() {
        Tournoi t = tournoiSixJoueurs(4);
        ListeLancements liste = salle.listeLancements(t);
        // Phase de poules : une ligne par poule, nommée par sa poule (pas de numéro de match).
        assertEquals(List.of(0, 0), liste.getParTableau().stream().map(ListeLancements.Ligne::numero).toList());
        assertEquals("Poule " + poule(t, 0).getNom(), ligne(liste, poule(t, 0)).echelon());
        assertEquals("Poule " + poule(t, 1).getNom(), ligne(liste, poule(t, 1)).echelon());

        // Tableau final : les matchs sont numérotés après les 6 parties de poule (ordre I.301).
        Tableau a = t.getTableaux().get(0);
        a.getPoules().forEach(p -> p.getMatchs().forEach(m -> m.definirScore(List.of(new com.pingmanager.model.Manche(
                11, 5), new com.pingmanager.model.Manche(11, 5), new com.pingmanager.model.Manche(11, 5)))));
        assertTrue(new ServicePhaseFinale().construire(a, 3L).estConstruit());
        ListeLancements finale = salle.listeLancements(t);
        List<Integer> numeros = finale.getParTableau().stream().map(ListeLancements.Ligne::numero).toList();
        assertEquals(List.of(7, 8), numeros);
        assertEquals(a.getPhaseFinale().getTours().get(0).getNom(), finale.getParTableau().get(0).echelon());
    }


    @Test
    void meme_table_choisie_deux_fois_le_second_lancement_est_refuse() {
        Tournoi t = tournoiSixJoueurs(4);
        // Le bandeau et une ligne de la liste affichent tous deux la table 3.
        Poule bandeau = poule(t, 0);
        Poule ligneListe = poule(t, 1);
        assertTrue(salle.verifierLancementPoule(t, bandeau, 3).estReussi());
        assertTrue(salle.verifierLancementPoule(t, ligneListe, 3).estReussi());

        assertTrue(salle.lancerPoule(t, ligneListe, 3).estReussi());
        ResultatAffectationTable r = salle.lancerPoule(t, bandeau, 3);

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, r.getMotif());
        assertEquals(3, r.getNumeroTable());
        assertFalse(bandeau.estLanceeEnBloc(), "rien n'est modifié en cas de refus");
        assertNull(bandeau.getMatchs().get(0).getNumeroTable());
    }


    @Test
    void joueur_lance_entre_l_affichage_et_l_appui_le_lancement_est_refuse() {
        Tournoi t = tournoiAvecB(4);
        Poule affichee = pouleB(t);
        ListeLancements avant = salle.listeLancements(t);
        Match representant = ligne(avant, affichee).match().match();
        assertTrue(avant.jouable(representant).isPresent());
        // Entre-temps, la poule de L1 dans le tableau A part sur la table 2.
        assertTrue(salle.lancerPoule(t, pouleDeL1(t), 2).estReussi());

        ResultatAffectationTable r = salle.lancerPoule(t, affichee, 1);

        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertEquals(Integer.valueOf(2), r.getTableDuJoueurBloquant());
        assertFalse(affichee.estLanceeEnBloc());
        assertTrue(salle.listeLancements(t).jouable(representant).isEmpty(), "elle quitte la liste des jouables");
    }


    @Test
    void poule_terminee_ailleurs_ses_joueurs_et_sa_table_redeviennent_libres() {
        Tournoi t = tournoiAvecB(4);
        Poule enJeu = pouleDeL1(t);
        assertTrue(salle.lancerPoule(t, enJeu, 1).estReussi());
        assertEquals(1, salle.listeLancements(t).getJouables().size());

        enJeu.getMatchs().forEach(m -> m.setStatut(StatutMatch.TERMINE)); // scores saisis sur un autre écran

        ListeLancements apres = salle.listeLancements(t);
        assertTrue(apres.getTablesLibres().contains(1));
        assertEquals(2, apres.getJouables().size(), "la poule de B repart : L1 est libre");
    }

}
