package com.pingmanager.ui.diffusion;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.diffusion.InstantaneSuivi.JoueurPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.MatchPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.ProchainePartie;
import com.pingmanager.ui.diffusion.InstantaneSuivi.TableauPublic;
import com.pingmanager.ui.infra.DonneesDemo;
import com.pingmanager.ui.infra.TournoiDemoJourJ;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstantaneSuiviTest {

    static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");
    static final Horloge HORLOGE = () -> MAINTENANT;

    static Tournoi demo() {
        return TournoiDemoJourJ.construire(DonneesDemo.JOUEURS_FICTIFS, MAINTENANT.minusSeconds(600));
    }

    static Tableau tab(Tournoi t, String lettre) {
        return t.getTableaux().stream().filter(x -> x.getNom().startsWith(lettre + " ")).findFirst().orElseThrow();
    }

    static Match enCoursTable(Tableau t, int table) {
        for (Poule p : t.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (m.getStatut() == StatutMatch.EN_COURS && Integer.valueOf(table).equals(m.getNumeroTable())) {
                    return m;
                }
            }
        }
        throw new AssertionError("Pas de match en cours sur la table " + table);
    }

    static JoueurPublic joueur(InstantaneSuivi s, String licence) {
        return s.joueurs().stream().filter(j -> j.licence().equals(licence)).findFirst().orElseThrow();
    }

    @Test
    void matchs_en_cours_de_la_demo_tries_par_table() {
        InstantaneSuivi s = InstantaneSuivi.depuis(demo(), HORLOGE);
        List<String> tables = s.enCours().stream().map(MatchPublic::table).toList();
        assertEquals(List.of("Table 2", "Table " + TournoiDemoJourJ.TABLE_EN_COURS_F, "Table 5"), tables);
        assertTrue(s.enCours().stream().allMatch(MatchPublic::enCours));
        MatchPublic f = s.enCours().get(1);
        assertTrue(f.tableau().startsWith("F "), f.tableau());
        assertEquals("Quarts de finale", f.situation());
        assertFalse(s.aVenir().isEmpty(), "des matchs prêts attendent une table");
        assertTrue(s.aVenir().size() <= InstantaneSuivi.MAX_A_VENIR);
        assertTrue(s.aVenir().stream().allMatch(m -> m.resultat().equals("À jouer")));
    }

    @Test
    void tableaux_dans_l_ordre_de_la_journee_avec_poules_et_tableau_final() {
        InstantaneSuivi s = InstantaneSuivi.depuis(demo(), HORLOGE);
        assertEquals(6, s.tableaux().size());
        assertTrue(s.tableaux().get(0).nom().startsWith("F "), "F commence à 8 h 30");
        TableauPublic c = s.tableaux().stream().filter(t -> t.nom().startsWith("C ")).findFirst().orElseThrow();
        assertFalse(c.poules().isEmpty());
        assertTrue(c.etape().startsWith("Poules : "), c.etape());
        assertFalse(c.poules().get(0).classement().isEmpty());
        TableauPublic f = s.tableaux().get(0);
        assertFalse(f.tours().isEmpty());
        assertEquals("Tableau final : Quarts de finale", f.etape());
        TableauPublic b = s.tableaux().stream().filter(t -> t.nom().startsWith("B ")).findFirst().orElseThrow();
        assertEquals("Pointage", b.etape());
        assertTrue(b.poules().isEmpty());
        assertTrue(b.note().contains("pointage"), b.note());
    }

    @Test
    void ma_prochaine_partie_juste_pour_un_joueur_sur_la_table_2() {
        Tournoi t = demo();
        Match m = enCoursTable(tab(t, "C"), 2);
        InstantaneSuivi s = InstantaneSuivi.depuis(t, HORLOGE);
        ProchainePartie p = joueur(s, m.getJoueurA().getNumeroLicence()).prochainePartie().orElseThrow();
        assertTrue(p.enCours());
        assertEquals("Vous jouez en ce moment", p.titre());
        assertEquals("Table 2", p.table());
        assertEquals("En ce moment", p.quand());
        assertTrue(p.adversaire().startsWith("Contre " + InstantaneSuivi.nomJoueur(m.getJoueurB())), p.adversaire());
        assertTrue(p.detail().startsWith(tab(t, "C").getNom() + " · Poule "), p.detail());
        // Et pour son adversaire, la même table.
        ProchainePartie q = joueur(s, m.getJoueurB().getNumeroLicence()).prochainePartie().orElseThrow();
        assertEquals("Table 2", q.table());
    }

    @Test
    void ma_prochaine_partie_place_dans_la_file_des_tables() {
        InstantaneSuivi s = InstantaneSuivi.depuis(demo(), HORLOGE);
        long enFile = s.joueurs().stream().map(JoueurPublic::prochaine)
                .filter(p -> p != null && !p.enCours()).peek(p -> {
                    assertEquals("Votre prochaine partie", p.titre());
                    assertEquals("Table pas encore attribuée", p.table());
                    assertFalse(p.quand().isEmpty());
                })
                .filter(p -> p.quand().contains("file d'attente") || p.quand().startsWith("Prochain match"))
                .count();
        assertTrue(enFile > 0, "des joueurs connaissent leur place dans la file");
        long premiers = s.joueurs().stream().map(JoueurPublic::prochaine)
                .filter(p -> p != null && p.quand().startsWith("Prochain match")).count();
        assertTrue(premiers <= 2, "le premier match prêt concerne au plus ses deux joueurs");
    }

    @Test
    void joueur_d_un_tableau_pas_encore_commence_sans_partie() {
        Tournoi t = demo();
        Tableau a = tab(t, "A");
        InstantaneSuivi s = InstantaneSuivi.depuis(t, HORLOGE);
        boolean vu = false;
        for (var j : a.getInscrits()) {
            JoueurPublic jp = joueur(s, j.getNumeroLicence());
            if (jp.prochaine() == null) {
                assertFalse(jp.sansPartie().isEmpty());
                vu = true;
            }
        }
        assertTrue(vu);
    }

    @Test
    void instantane_immuable_et_detache_du_tournoi() {
        Tournoi t = demo();
        InstantaneSuivi s = InstantaneSuivi.depuis(t, HORLOGE);
        assertThrows(UnsupportedOperationException.class, () -> s.tableaux().add(null));
        assertThrows(UnsupportedOperationException.class, () -> s.joueurs().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.tableaux().get(0).tours().clear());
        String nom = s.nomTournoi();
        int enCours = s.enCours().size();
        // Le tournoi change ensuite (fil JavaFX) : l'instantané déjà publié ne bouge pas.
        Match m = enCoursTable(tab(t, "C"), 2);
        m.setStatut(StatutMatch.A_JOUER);
        m.setNumeroTable(null);
        t.setNom("Autre nom");
        assertEquals(nom, s.nomTournoi());
        assertEquals(enCours, s.enCours().size());
        assertEquals(enCours - 1, InstantaneSuivi.depuis(t, HORLOGE).enCours().size());
    }

    @Test
    void recherche_normalisee_sans_accents() {
        assertEquals("helene d arc", InstantaneSuivi.normaliser("  Hélène  D'ARC "));
        assertEquals("", InstantaneSuivi.normaliser(null));
    }
}
