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

    @Test
    void liste_par_priorite_avec_table_proposee_et_tables_libres_possibles() {
        Tournoi t = tournoiSixJoueurs(4);

        ListeLancements liste = salle.listeLancements(t);

        assertEquals(6, liste.getProposes().size(), "2 poules de 3 : 6 parties");
        assertEquals(6, liste.getParTableau().size());
        assertEquals(List.of(1, 2, 3, 4), liste.getTablesLibres());
        assertEquals(6, liste.nombreLancables());
        ListeLancements.Ligne premiere = liste.getProposes().get(0);
        assertTrue(premiere.propose(), "le premier est celui que la stratégie enverrait");
        assertTrue(premiere.estLancable());
        assertEquals(List.of(1, 2, 3, 4), premiere.tablesPossibles());
        assertNotNull(premiere.tableProposee());
        // Même ordre que les matchs prêts de la salle (priorité de l'ordonnanceur).
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        for (int i = 0; i < bord.getPrets().size(); i++) {
            assertSame(bord.getPrets().get(i).match(), liste.getProposes().get(i).match().match());
        }
        // Les tables proposées par la stratégie ne servent qu'une fois.
        List<Integer> tablesStrategie = liste.getProposes().stream().filter(ListeLancements.Ligne::propose)
                .map(ListeLancements.Ligne::tableProposee).toList();
        assertEquals(tablesStrategie.size(), tablesStrategie.stream().distinct().count());
    }

    @Test
    void lancement_avec_un_joueur_occupe_refuse_avec_son_motif() {
        Tournoi t = tournoiSixJoueurs(4);
        Match enJeu = poule(t, 0).getMatchs().get(0);
        assertTrue(salle.affecter(t, enJeu, 4).estReussi());
        Match memeJoueur = poule(t, 0).getMatchs().stream().filter(m -> m != enJeu).findFirst().orElseThrow();

        ResultatAffectationTable verification = salle.verifierLancement(t, memeJoueur, 1);
        ResultatAffectationTable r = salle.affecter(t, memeJoueur, 1);

        for (ResultatAffectationTable x : List.of(verification, r)) {
            assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, x.getMotif());
            assertEquals(Integer.valueOf(4), x.getTableDuJoueurBloquant());
            Joueur bloquant = x.getJoueurBloquant();
            assertTrue(bloquant.equals(enJeu.getJoueurA()) || bloquant.equals(enJeu.getJoueurB()));
        }
        assertEquals(StatutMatch.A_JOUER, memeJoueur.getStatut(), "rien n'est modifié en cas de refus");
        assertNull(memeJoueur.getNumeroTable());
        // Dans la liste, le match reste visible, désactivé, avec le même motif.
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
        Match m = poule(t, 1).getMatchs().get(0);

        ResultatAffectationTable r = salle.verifierLancement(t, m, 2);

        assertTrue(r.estReussi());
        assertEquals(2, r.getNumeroTable());
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertTrue(salle.tableauDeBord(t).table(2).estLibre());
    }

    @Test
    void table_occupee_refusee_en_manuel_meme_si_les_joueurs_sont_libres() {
        Tournoi t = tournoiSixJoueurs(4);
        assertTrue(salle.affecter(t, poule(t, 0).getMatchs().get(0), 2).estReussi());
        Match autrePoule = poule(t, 1).getMatchs().get(0);

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, salle.verifierLancement(t, autrePoule, 2).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, salle.affecter(t, autrePoule, 2).getMotif());
        ListeLancements.Ligne l = ligne(salle.listeLancements(t), autrePoule);
        assertEquals(List.of(1, 3, 4), l.tablesPossibles(), "la table 2 n'est plus proposée");
        assertNotEquals(Integer.valueOf(2), l.tableProposee());
    }

    @Test
    void aucune_table_libre_le_match_reste_visible_avec_son_motif() {
        Tournoi t = tournoiSixJoueurs(1);
        assertTrue(salle.affecter(t, poule(t, 0).getMatchs().get(0), 1).estReussi());

        ListeLancements liste = salle.listeLancements(t);

        assertTrue(liste.getTablesLibres().isEmpty());
        assertEquals(0, liste.nombreLancables());
        assertEquals(3, liste.nombrePrets(), "l'autre poule est prête, il ne manque qu'une table");
        Match autrePoule = poule(t, 1).getMatchs().get(0);
        ListeLancements.Ligne l = ligne(liste, autrePoule);
        assertFalse(l.estLancable());
        assertEquals(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, l.refus().getMotif());
        assertNotNull(l.attenteDepuis(), "le temps d'attente reste affiché");
    }

    @Test
    void lancer_la_proposition_puis_la_liste_se_met_a_jour() {
        Tournoi t = tournoiSixJoueurs(4);
        ListeLancements.Ligne premiere = salle.listeLancements(t).getProposes().get(0);
        Match m = premiere.match().match();

        ResultatAffectationTable r = salle.affecter(t, m, premiere.tableProposee());

        assertTrue(r.estReussi());
        assertEquals(premiere.tableProposee(), m.getNumeroTable());
        ListeLancements apres = salle.listeLancements(t);
        assertEquals(5, apres.getProposes().size(), "le match lancé quitte la liste");
        assertFalse(apres.getTablesLibres().contains(premiere.tableProposee()));
        // Poule de 3 : les deux autres parties des joueurs en jeu sont désactivées.
        assertEquals(3, apres.nombreLancables());
    }

    @Test
    void lancement_manuel_d_un_match_choisi_sur_une_table_choisie() {
        Tournoi t = tournoiSixJoueurs(4);
        ListeLancements liste = salle.listeLancements(t);
        // Ordre du tournoi : la dernière partie de la seconde poule, sur la dernière table.
        ListeLancements.Ligne choisi = liste.getParTableau().get(liste.getParTableau().size() - 1);
        Match m = choisi.match().match();
        assertSame(poule(t, 1).getMatchs().get(2), m);

        assertTrue(salle.affecter(t, m, 4).estReussi());
        assertEquals(StatutMatch.EN_COURS, m.getStatut());
        assertEquals(Integer.valueOf(4), m.getNumeroTable());
    }

    @Test
    void joueur_d_un_match_en_cours_sans_table_valable_reste_indisponible() {
        Tournoi t = tournoiSixJoueurs(4);
        Match sansTable = poule(t, 0).getMatchs().get(0);
        sansTable.setStatut(StatutMatch.EN_COURS); // table absente : anomalie de reprise
        Match memeJoueur = poule(t, 0).getMatchs().get(1);

        ResultatAffectationTable r = salle.affecter(t, memeJoueur, 1);

        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertNull(r.getTableDuJoueurBloquant(), "table inconnue");
        assertEquals(StatutMatch.A_JOUER, memeJoueur.getStatut());
        assertFalse(ligne(salle.listeLancements(t), memeJoueur).estLancable());
        // « Proposer » ne l'envoie pas non plus.
        ResultatPropositionSalle p = salle.proposer(t);
        assertTrue(p.getLancements().stream().noneMatch(l -> l.match().match() == memeJoueur));
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
    void jouables_par_priorite_sans_les_matchs_d_un_joueur_qui_joue_ailleurs() {
        Tournoi t = tournoiSixJoueurs(4);
        Match enJeu = poule(t, 0).getMatchs().get(0);
        assertTrue(salle.affecter(t, enJeu, 1).estReussi());

        ListeLancements liste = salle.listeLancements(t);
        List<ListeLancements.Ligne> jouables = liste.getJouables();

        assertEquals(3, jouables.size(), "poule de 3 : les deux autres parties attendent un joueur en jeu");
        assertTrue(jouables.stream().allMatch(l -> l.match().match().getJoueurA() != enJeu.getJoueurA()
                && l.match().match().getJoueurB() != enJeu.getJoueurA()
                && l.match().match().getJoueurA() != enJeu.getJoueurB()
                && l.match().match().getJoueurB() != enJeu.getJoueurB()));
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
        assertEquals(6, jouables.size());

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
    void sans_table_libre_les_matchs_restent_jouables_mais_pas_lancables() {
        Tournoi t = tournoiSixJoueurs(1);
        assertTrue(salle.affecter(t, poule(t, 0).getMatchs().get(0), 1).estReussi());

        ListeLancements liste = salle.listeLancements(t);

        assertEquals(3, liste.getJouables().size());
        assertTrue(liste.getJouables().stream().noneMatch(ListeLancements.Ligne::estLancable));
        assertTrue(liste.getJouables().stream().allMatch(l -> l.tablesPossibles().isEmpty()));
        assertTrue(liste.propositionApres(null).isPresent(), "la proposition s'affiche, « Lancer » est désactivé");
    }

    @Test
    void numero_du_match_dans_son_tableau_et_echelon() {
        Tournoi t = tournoiSixJoueurs(4);
        ListeLancements liste = salle.listeLancements(t);

        List<Integer> numeros = liste.getParTableau().stream().map(ListeLancements.Ligne::numero).toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6), numeros, "poules dans l'ordre officiel (I.301)");
        ListeLancements.Ligne premiere = ligne(liste, poule(t, 0).getMatchs().get(0));
        assertEquals("Poule " + poule(t, 0).getNom(), premiere.echelon());
        assertEquals("Poule " + poule(t, 1).getNom(), ligne(liste, poule(t, 1).getMatchs().get(2)).echelon());
        assertEquals(6, ligne(liste, poule(t, 1).getMatchs().get(2)).numero());
    }

    @Test
    void meme_table_choisie_deux_fois_le_second_lancement_est_refuse() {
        Tournoi t = tournoiSixJoueurs(4);
        // Le bandeau et une ligne de la liste affichent tous deux la table 3.
        Match bandeau = poule(t, 0).getMatchs().get(0);
        Match ligneListe = poule(t, 1).getMatchs().get(0);
        assertTrue(salle.verifierLancement(t, bandeau, 3).estReussi());
        assertTrue(salle.verifierLancement(t, ligneListe, 3).estReussi());

        assertTrue(salle.affecter(t, ligneListe, 3).estReussi());
        ResultatAffectationTable r = salle.affecter(t, bandeau, 3);

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, r.getMotif());
        assertEquals(3, r.getNumeroTable());
        assertEquals(StatutMatch.A_JOUER, bandeau.getStatut(), "rien n'est modifié en cas de refus");
        assertNull(bandeau.getNumeroTable());
    }

    @Test
    void joueur_lance_entre_l_affichage_et_l_appui_le_lancement_est_refuse() {
        Tournoi t = tournoiSixJoueurs(4);
        Match affiche = poule(t, 0).getMatchs().get(0);
        ListeLancements avant = salle.listeLancements(t);
        assertTrue(avant.jouable(affiche).isPresent());
        // Entre-temps, un autre match d'un de ses joueurs part sur la table 2.
        Match memeJoueur = poule(t, 0).getMatchs().get(1);
        assertTrue(salle.affecter(t, memeJoueur, 2).estReussi());

        ResultatAffectationTable r = salle.affecter(t, affiche, 1);

        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertEquals(Integer.valueOf(2), r.getTableDuJoueurBloquant());
        assertEquals(StatutMatch.A_JOUER, affiche.getStatut());
        assertTrue(salle.listeLancements(t).jouable(affiche).isEmpty(), "il quitte la liste des jouables");
    }

    @Test
    void match_termine_ailleurs_ses_joueurs_et_sa_table_redeviennent_libres() {
        Tournoi t = tournoiSixJoueurs(4);
        Match enJeu = poule(t, 0).getMatchs().get(0);
        assertTrue(salle.affecter(t, enJeu, 1).estReussi());
        assertEquals(3, salle.listeLancements(t).getJouables().size());

        enJeu.setStatut(StatutMatch.TERMINE); // score saisi sur un autre écran

        ListeLancements apres = salle.listeLancements(t);
        assertTrue(apres.getTablesLibres().contains(1));
        assertEquals(5, apres.getJouables().size());
    }
}
