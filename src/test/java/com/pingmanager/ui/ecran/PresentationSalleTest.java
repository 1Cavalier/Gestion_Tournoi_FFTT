package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Textes de la salle : durées, motifs de refus, compte rendu de « Proposer ». */
class PresentationSalleTest {

    private static Joueur j(String licence, String prenom, String nom) {
        return new Joueur(licence, nom, prenom, Sexe.HOMME, "Club", 1000);
    }

    private static Tournoi tournoi(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = new Tableau("C – Toutes séries",
                ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (int i = 1; i <= 6; i++) {
            Joueur joueur = j("L" + i, "Prénom" + i, "NOM" + i);
            a.inscrire(joueur);
            a.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(a);
        new ServiceLancementTableau(t).lancer(a, 1L);
        return t;
    }

    private final ServiceSalle salle = new ServiceSalle(() -> Instant.parse("2026-11-14T10:00:00Z"));

    @Test
    void duree_ecoulee_lisible_de_loin() {
        assertEquals("", PresentationSalle.duree(null));
        assertEquals("moins d'1 min", PresentationSalle.duree(Duration.ofSeconds(40)));
        assertEquals("12 min", PresentationSalle.duree(Duration.ofMinutes(12)));
        assertEquals("1 h 05", PresentationSalle.duree(Duration.ofMinutes(65)));
        assertEquals("2 h 30", PresentationSalle.duree(Duration.ofMinutes(150)));
        assertFalse(PresentationSalle.estLongue(Duration.ofMinutes(39)));
        assertTrue(PresentationSalle.estLongue(Duration.ofMinutes(40)));
        assertFalse(PresentationSalle.estLongue(null));
    }

    @Test
    void numero_de_table_seulement_pour_un_match_en_cours() {
        Match m = new Match(j("A", "Ana", "AUBERT"), j("B", "Bob", "BLANC"));
        assertEquals("", PresentationSalle.libelleTable(m));
        m.setNumeroTable(4);
        assertEquals("", PresentationSalle.libelleTable(m), "à jouer : pas encore sur une table");
        m.setStatut(StatutMatch.EN_COURS);
        assertEquals("Table 4", PresentationSalle.libelleTable(m));
        m.setStatut(StatutMatch.TERMINE);
        assertEquals("", PresentationSalle.libelleTable(m));
        assertEquals("", PresentationSalle.libelleTable(null));
    }

    @Test
    void joueurs_et_contexte_du_match() {
        Tournoi t = tournoi(4);
        TableauDeBordSalle.MatchSalle m = salle.tableauDeBord(t).getPrets().get(0);
        assertTrue(PresentationSalle.joueurs(m.match()).contains("  –  "));
        assertTrue(PresentationSalle.contexte(m).startsWith("C – Toutes séries · Poule "), PresentationSalle.contexte(m));
        assertEquals("À déterminer", PresentationSalle.nom(Joueur.aDeterminer()));
    }

    @Test
    void motif_d_un_match_bloque_nomme_le_joueur_et_sa_table() {
        Tournoi t = tournoi(4);
        Match enJeu = t.getTableaux().get(0).getPoules().get(0).getMatchs().get(0);
        salle.affecter(t, enJeu, 3);
        TableauDeBordSalle.MatchBloque b = salle.tableauDeBord(t).getBloques().get(0);
        String motif = PresentationSalle.motifBlocage(b);
        assertTrue(motif.endsWith("déjà en jeu à la table 3"), motif);
        assertTrue(motif.startsWith("Prénom"), motif);
    }

    @Test
    void chaque_refus_explique_pourquoi() {
        assertEquals("La table 9 n'existe pas : la salle compte les tables 1 à 4.", PresentationSalle.message(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, 9), 4));
        assertTrue(PresentationSalle.message(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, 1), 0)
                .contains("nombre de tables"));
        assertTrue(PresentationSalle.message(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_OCCUPEE, 2), 4)
                .startsWith("La table 2 est occupée"));
        String joueur = PresentationSalle.message(
                ResultatAffectationTable.joueurEnJeu(5, j("X", "Xavier", "DURAND"), 2), 4);
        assertTrue(joueur.startsWith("Xavier DURAND joue déjà à la table 2"), joueur);
        assertEquals("Match envoyé à la table 5.", PresentationSalle.message(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, 5), 4));
        // Aucun motif ne retombe sur son nom brut d'énumération.
        for (ResultatAffectationTable.Motif motif : ResultatAffectationTable.Motif.values()) {
            String texte = PresentationSalle.message(ResultatAffectationTable.de(motif, 1), 4);
            assertNotEquals(motif.name(), texte, "texte manquant pour " + motif);
        }
    }

    @Test
    void compte_rendu_de_proposer() {
        Tournoi t = tournoi(4);
        assertEquals("2 matchs envoyés aux tables 1 et 2.", PresentationSalle.messageProposition(salle.proposer(t)));
        assertTrue(PresentationSalle.messageProposition(salle.proposer(t)).startsWith("Aucun match prêt"));
        Tournoi une = tournoi(1);
        assertEquals("1 match envoyé à la table 1.", PresentationSalle.messageProposition(salle.proposer(une)));
        assertTrue(PresentationSalle.messageProposition(salle.proposer(une)).startsWith("Toutes les tables"));
        assertEquals("1, 2 et 4", PresentationSalle.listeLisible(List.of("1", "2", "4")));
    }

    @Test
    void resume_pour_la_page_jour_j() {
        Tournoi t = tournoi(16);
        assertEquals("0 table occupée sur 16 · 6 matchs prêts", PresentationSalle.resume(salle.tableauDeBord(t)));
        salle.proposer(t);
        assertEquals("2 tables occupées sur 16 · 0 match prêt", PresentationSalle.resume(salle.tableauDeBord(t)));
        assertEquals("Nombre de tables non renseigné.", PresentationSalle.resume(salle.tableauDeBord(tournoi(0))));
    }

    @Test
    void cle_de_glisser_deposer() {
        assertEquals(7, PresentationSalle.lireCleGlisser(PresentationSalle.cleGlisser(7)));
        assertEquals(-1, PresentationSalle.lireCleGlisser("texte quelconque"));
        assertEquals(-1, PresentationSalle.lireCleGlisser(PresentationSalle.PREFIXE_GLISSER + "x"));
        assertEquals(-1, PresentationSalle.lireCleGlisser(null));
    }

    @Test
    void salle_proposee_des_qu_un_tableau_est_lance() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        assertFalse(PresentationSalle.salleOuverte(t));
        t.ajouterTableau(new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build()));
        assertFalse(PresentationSalle.salleOuverte(t), "tableau en inscription");
        Tournoi lance = tournoi(4);
        assertTrue(PresentationSalle.salleOuverte(lance));
        lance.changerStatut(com.pingmanager.model.StatutTournoi.ANNULE);
        assertFalse(PresentationSalle.salleOuverte(lance));
    }

    // ------------------------------------------------------------------
    // Écran principal du jour J (B011)
    // ------------------------------------------------------------------

    @Test
    void ligne_de_chiffres_de_l_ecran_principal() {
        Tournoi t = tournoi(4);
        assertEquals("4 tables libres · 0 en cours · 6 matchs proposés",
                PresentationSalle.resumeLigne(salle.tableauDeBord(t)));
        salle.proposer(t);
        String apres = PresentationSalle.resumeLigne(salle.tableauDeBord(t));
        assertTrue(apres.startsWith("2 tables libres · 2 en cours · "), apres);
        assertTrue(apres.contains(" bloqués"), "les joueurs en jeu bloquent leurs autres parties : " + apres);
        assertEquals("Nombre de tables non renseigné", PresentationSalle.resumeLigne(salle.tableauDeBord(tournoi(0))));
    }

    @Test
    void envoi_en_un_clic_sur_la_premiere_table_libre() {
        Tournoi t = tournoi(3);
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertEquals(1, PresentationSalle.premiereTableLibre(bord).orElseThrow());
        Match premier = bord.getPrets().get(0).match();
        salle.affecter(t, premier, 1);
        assertEquals(2, PresentationSalle.premiereTableLibre(salle.tableauDeBord(t)).orElseThrow());
        Tournoi une = tournoi(1);
        salle.proposer(une);
        assertTrue(PresentationSalle.premiereTableLibre(salle.tableauDeBord(une)).isEmpty());
    }

    /**
     * Le geste répété du juge-arbitre sans changer de page : envoyer un match,
     * saisir le score (la table se libère), enchaîner le match suivant sur la
     * même table ; puis corriger le score sans que la salle ne bouge.
     */
    @Test
    void envoyer_saisir_le_score_puis_enchainer_le_match_suivant_sur_la_table_liberee() {
        Tournoi t = tournoi(1);
        Tableau tab = t.getTableaux().get(0);
        com.pingmanager.core.SaisieResultatMatch saisie =
                new com.pingmanager.core.SaisieResultatMatch(() -> Instant.parse("2026-11-14T10:20:00Z"));

        salle.proposer(t);
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        Match enJeu = bord.getTables().get(0).match().match();
        assertTrue(PresentationSalle.suiteSurTable(bord, 1).isEmpty(), "table occupée : rien à enchaîner");

        List<com.pingmanager.model.Manche> score = new java.util.ArrayList<>();
        for (int i = 0; i < enJeu.getNombreManchesGagnantes(); i++) {
            score.add(new com.pingmanager.model.Manche(11, 7));
        }
        assertTrue(saisie.enregistrerScore(tab, enJeu, score).estEnregistre());
        ResultatAffectationTable liberee = salle.libererApresResultat(t, enJeu);
        assertEquals(1, liberee.getNumeroTable());

        bord = salle.tableauDeBord(t);
        TableauDeBordSalle.MatchSalle suite = PresentationSalle.suiteSurTable(bord, 1).orElseThrow();
        assertSame(bord.getPrets().get(0), suite, "ordre de l'ordonnanceur respecté");
        assertTrue(salle.affecter(t, suite.match(), 1).estReussi());
        assertTrue(PresentationSalle.suiteSurTable(salle.tableauDeBord(t), 1).isEmpty());
        assertTrue(PresentationSalle.suiteSurTable(salle.tableauDeBord(t), 0).isEmpty(), "aucune table libérée");

        // Correction du premier score : la partie reste jouée, la salle est inchangée.
        List<com.pingmanager.model.Manche> corrige = new java.util.ArrayList<>();
        for (int i = 0; i < enJeu.getNombreManchesGagnantes(); i++) {
            corrige.add(new com.pingmanager.model.Manche(7, 11));
        }
        assertTrue(saisie.enregistrerScore(tab, enJeu, corrige).estEnregistre());
        assertEquals(enJeu.getJoueurB(), enJeu.getVainqueur());
        assertEquals(1, salle.tableauDeBord(t).nombreTablesOccupees());
    }

    @Test
    void joueur_engage_sur_deux_tableaux_jamais_propose_pendant_qu_il_joue() {
        // 3.3.1.2.2 / règle de la salle : un joueur n'est jamais sur deux tables.
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(2);
        Joueur commun = j("L1", "Prénom1", "NOM1");
        for (String nom : List.of("A", "B")) {
            Tableau tab = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
            t.ajouterTableau(tab);
            tab.ouvrirPointage();
            tab.inscrire(commun);
            tab.pointer(commun);
            for (int i = 2; i <= 3; i++) {
                Joueur autre = j(nom + i, "Prénom" + nom + i, "NOM" + nom + i);
                tab.inscrire(autre);
                tab.pointer(autre);
            }
            new ServicePointage(t).cloturerPointage(tab);
            new ServiceLancementTableau(t).lancer(tab, 1L);
        }
        Match avecCommun = null;
        for (TableauDeBordSalle.MatchSalle m : salle.tableauDeBord(t).getPrets()) {
            if (m.match().getJoueurA().equals(commun) || m.match().getJoueurB().equals(commun)) {
                avecCommun = m.match();
                break;
            }
        }
        assertNotNull(avecCommun);
        assertTrue(salle.affecter(t, avecCommun, 1).estReussi());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        TableauDeBordSalle.MatchSalle suite = PresentationSalle.suiteSurTable(bord, 2).orElseThrow();
        assertNotEquals(commun, suite.match().getJoueurA());
        assertNotEquals(commun, suite.match().getJoueurB());
        assertEquals(2, PresentationSalle.premiereTableLibre(bord).orElseThrow());
    }
}
