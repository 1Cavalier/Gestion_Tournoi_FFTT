package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GenerateurMatchsPouleTest {

    private final GenerateurMatchsPoule generateur = new GenerateurMatchsPoule();

    private Poule pouleDe(int taille) {
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 0; i < taille; i++) {
            joueurs.add(new Joueur("L" + i, "Nom" + i, "Prenom" + i,
                    Sexe.HOMME, "Club", 1000 + i));
        }
        return new Poule("A", joueurs);
    }

    private String clePaire(Match m) {
        String a = m.getJoueurA().getNumeroLicence();
        String b = m.getJoueurB().getNumeroLicence();
        return a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
    }

    @Test
    void poule_de_trois_a_trois_matchs_uniques() {
        Poule poule = pouleDe(3);
        List<Match> matchs = generateur.genererMatchs(poule);
        assertEquals(3, matchs.size());
        assertEquals(3, matchs.stream().map(this::clePaire).distinct().count());
    }

    @Test
    void poule_de_quatre_a_six_matchs_uniques() {
        Poule poule = pouleDe(4);
        List<Match> matchs = generateur.genererMatchs(poule);
        assertEquals(6, matchs.size());
        assertEquals(6, matchs.stream().map(this::clePaire).distinct().count());
    }

    @Test
    void chaque_joueur_rencontre_tous_les_autres() {
        // Vérifie le "toutes rondes" pour plusieurs tailles, y compris génériques.
        for (int taille : new int[]{3, 4, 5, 6, 7, 8}) {
            Poule poule = pouleDe(taille);
            List<Match> matchs = generateur.genererMatchs(poule);

            int attendus = taille * (taille - 1) / 2;
            Set<String> paires = new HashSet<>();
            for (Match m : matchs) {
                paires.add(clePaire(m));
            }
            assertEquals(attendus, paires.size(),
                    "Taille " + taille + " : chaque paire doit apparaître une seule fois.");
        }
    }

    @Test
    void aucun_joueur_ne_se_rencontre_lui_meme() {
        Poule poule = pouleDe(6);
        List<Match> matchs = generateur.genererMatchs(poule);
        for (Match m : matchs) {
            assertNotEquals(m.getJoueurA(), m.getJoueurB());
        }
    }

    // --- Règle FFTT anti-arrangement : joueurs d'un même club au 1er tour ---

    private Joueur joueurClub(String licence, String club) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, club, 1000);
    }

    /** Tour (1-based) auquel deux joueurs se rencontrent, ou -1. */
    private int tourRencontre(List<Match> matchs, String lic1, String lic2) {
        for (int i = 0; i < matchs.size(); i++) {
            Match m = matchs.get(i);
            String a = m.getJoueurA().getNumeroLicence();
            String b = m.getJoueurB().getNumeroLicence();
            if ((a.equals(lic1) && b.equals(lic2)) || (a.equals(lic2) && b.equals(lic1))) {
                return i + 1;
            }
        }
        return -1;
    }

    @Test
    void poule_de_trois_deux_memes_clubs_se_rencontrent_au_premier_tour() {
        Poule poule = new Poule("A", List.of(
                joueurClub("X", "DUP"),
                joueurClub("Z", "AUTRE"),
                joueurClub("Y", "DUP")));
        generateur.genererMatchs(poule);
        assertEquals(1, tourRencontre(poule.getMatchs(), "X", "Y"));
    }

    @Test
    void poule_de_quatre_deux_memes_clubs_se_rencontrent_au_premier_tour() {
        Poule poule = new Poule("D", List.of(
                joueurClub("P", "DUP"),
                joueurClub("Q", "A"),
                joueurClub("R", "B"),
                joueurClub("S", "DUP")));
        generateur.genererMatchs(poule);
        assertEquals(1, tourRencontre(poule.getMatchs(), "P", "S"));
    }

    @Test
    void poule_de_trois_a_deux_qualifies_suit_l_ordre_1_3_1_2_2_3() {
        // Clubs distincts pour ne pas déclencher l'anti-arrangement.
        Poule poule = new Poule("A", List.of(
                joueurClub("P1", "C1"),
                joueurClub("P2", "C2"),
                joueurClub("P3", "C3")));
        List<Match> m = generateur.genererMatchs(poule, 2);
        assertEquals("P1", m.get(0).getJoueurA().getNumeroLicence());
        assertEquals("P3", m.get(0).getJoueurB().getNumeroLicence());
        assertEquals("P2", m.get(1).getJoueurB().getNumeroLicence());
        assertEquals("P2", m.get(2).getJoueurA().getNumeroLicence());
        assertEquals("P3", m.get(2).getJoueurB().getNumeroLicence());
    }

    @Test
    void poule_de_trois_a_un_qualifie_suit_l_ordre_1_3_2_3_1_2() {
        // Ordre officiel I.301.1 : 1-3, 2-3, 1-2.
        Poule poule = new Poule("B", List.of(
                joueurClub("Q1", "C1"),
                joueurClub("Q2", "C2"),
                joueurClub("Q3", "C3")));
        List<Match> m = generateur.genererMatchs(poule, 1);
        assertEquals("Q1-Q3", paire(m.get(0)));
        assertEquals("Q2-Q3", paire(m.get(1)));
        assertEquals("Q1-Q2", paire(m.get(2)));
    }

    private String paire(Match m) {
        return m.getJoueurA().getNumeroLicence() + "-" + m.getJoueurB().getNumeroLicence();
    }

    /** Construit une poule de joueurs J1..Jn (clubs distincts) et rend l'ordre obtenu. */
    private String ordreObtenu(int taille, int qualifies) {
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= taille; i++) {
            js.add(new Joueur("J" + i, "N" + i, "P" + i, Sexe.HOMME, "Club" + i, 2000 - i));
        }
        Poule p = new Poule("A", js);
        generateur.genererMatchs(p, qualifies);
        StringBuilder sb = new StringBuilder();
        for (Match m : p.getMatchs()) {
            int a = Integer.parseInt(m.getJoueurA().getNumeroLicence().substring(1));
            int b = Integer.parseInt(m.getJoueurB().getNumeroLicence().substring(1));
            sb.append(a).append("-").append(b).append(" ");
        }
        return sb.toString().trim();
    }

    @Test
    void ordres_officiels_i301_poule_de_quatre() {
        assertEquals("1-4 2-3 1-3 2-4 1-2 3-4", ordreObtenu(4, 1)); // I.301.3
        assertEquals("1-3 2-4 1-2 3-4 1-4 2-3", ordreObtenu(4, 2)); // I.301.4
    }

    @Test
    void ordres_officiels_i301_poule_de_cinq() {
        assertEquals("2-5 3-4 1-5 2-3 1-4 3-5 1-3 2-4 1-2 4-5", ordreObtenu(5, 1)); // I.301.5
        assertEquals("2-5 3-4 1-4 3-5 1-3 2-4 1-2 4-5 1-5 2-3", ordreObtenu(5, 2)); // I.301.6
    }

    @Test
    void ordres_officiels_i301_poule_de_six() {
        assertEquals("1-6 2-5 3-4 1-5 4-6 2-3 1-4 3-5 2-6 1-3 2-4 5-6 1-2 3-6 4-5",
                ordreObtenu(6, 1)); // I.301.7
        assertEquals("1-6 2-5 3-4 1-4 3-5 2-6 1-3 2-4 5-6 1-2 3-6 4-5 1-5 4-6 2-3",
                ordreObtenu(6, 2)); // I.301.8
    }
}
