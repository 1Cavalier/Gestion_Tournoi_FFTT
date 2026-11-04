package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Qualifie;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie la conformité aux articles I.302 (départage des ex æquo à la
 * constitution des poules) et I.304.2 / I.305.2 (tirage au sort des têtes de
 * série par groupes 3-4, 5-8, …).
 */
class ConformiteTirageTest {

    private final GenerateurPoules generateurPoules = new GenerateurPoules();
    private final PlacementQualifiesService placement = new PlacementQualifiesService();

    private Joueur j(String lic, int pts, String club) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, club, pts);
    }

    private Poule pouleJouee(String nom, Joueur a, Joueur b, Joueur c) {
        Poule p = new Poule(nom, List.of(a, b, c));
        Match m1 = new Match(a, b);
        m1.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        Match m2 = new Match(a, c);
        m2.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        Match m3 = new Match(b, c);
        m3.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        p.ajouterMatch(m1);
        p.ajouterMatch(m2);
        p.ajouterMatch(m3);
        return p;
    }

    private boolean memeCompo(List<Poule> a, List<Poule> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            List<String> la = new ArrayList<>();
            List<String> lb = new ArrayList<>();
            for (Joueur jj : a.get(i).getJoueurs()) la.add(jj.getNumeroLicence());
            for (Joueur jj : b.get(i).getJoueurs()) lb.add(jj.getNumeroLicence());
            if (!la.equals(lb)) return false;
        }
        return true;
    }

    // --- I.302 : départage des ex æquo ---

    @Test
    void sans_graine_la_formation_des_poules_est_stable() {
        List<Joueur> egaux = new ArrayList<>();
        for (int i = 0; i < 6; i++) egaux.add(j("E" + i, 1000, "Cl" + i));
        List<Poule> s1 = generateurPoules.genererPoules(new ArrayList<>(egaux), 3, false);
        List<Poule> s2 = generateurPoules.genererPoules(new ArrayList<>(egaux), 3, false);
        assertTrue(memeCompo(s1, s2));
    }

    @Test
    void avec_graine_les_ex_aequo_sont_departages_de_facon_reproductible() {
        List<Joueur> egaux = new ArrayList<>();
        for (int i = 0; i < 6; i++) egaux.add(j("E" + i, 1000, "Cl" + i));
        List<Poule> g1 = generateurPoules.genererPoules(new ArrayList<>(egaux), 3, false, 42L);
        List<Poule> g2 = generateurPoules.genererPoules(new ArrayList<>(egaux), 3, false, 42L);
        assertTrue(memeCompo(g1, g2), "même graine => même composition");

        int differences = 0;
        for (long g = 0; g < 20; g++) {
            List<Poule> ga = generateurPoules.genererPoules(new ArrayList<>(egaux), 3, false, g);
            List<Poule> gb = generateurPoules.genererPoules(new ArrayList<>(egaux), 3, false, g + 100);
            if (!memeCompo(ga, gb)) differences++;
        }
        assertTrue(differences > 0, "des graines différentes doivent varier le tirage");
    }

    @Test
    void points_distincts_la_graine_ne_change_pas_la_composition() {
        List<Joueur> distincts = new ArrayList<>();
        for (int i = 0; i < 6; i++) distincts.add(j("D" + i, 2000 - i * 100, "Cl" + i));
        List<Poule> d1 = generateurPoules.genererPoules(new ArrayList<>(distincts), 3, false, 1L);
        List<Poule> d2 = generateurPoules.genererPoules(new ArrayList<>(distincts), 3, false, 999L);
        assertTrue(memeCompo(d1, d2));
    }

    // --- I.304.2 / I.305.2 : tirage des têtes de série par groupes ---

    private List<Qualifie> huitPoulesQualifies() {
        List<Poule> poules = new ArrayList<>();
        String[] noms = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (int i = 0; i < 8; i++) {
            int base = 2000 - i * 50;
            poules.add(pouleJouee(noms[i],
                    j(noms[i] + "1", base, "c" + i + "1"),
                    j(noms[i] + "2", base - 500, "c" + i + "2"),
                    j(noms[i] + "3", base - 900, "c" + i + "3")));
        }
        return placement.extraireQualifies(poules, 2);
    }

    @Test
    void les_deux_meilleurs_premiers_ont_des_seeds_fixes() {
        List<Qualifie> quals = huitPoulesQualifies();
        for (long g = 0; g < 100; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            assertEquals("A1", ps[1].getNumeroLicence(), "meilleur 1er en seed 1");
            assertEquals("B1", ps[2].getNumeroLicence(), "2e 1er en seed 2");
        }
    }

    @Test
    void les_tetes_de_serie_trois_et_quatre_sont_tirees_au_sort() {
        List<Qualifie> quals = huitPoulesQualifies();
        int c1en3 = 0, c1en4 = 0, horsBloc = 0;
        for (long g = 0; g < 200; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            Map<String, Integer> seedDe = new HashMap<>();
            for (int s = 1; s <= 16; s++) if (ps[s] != null) seedDe.put(ps[s].getNumeroLicence(), s);
            int sc = seedDe.get("C1"), sd = seedDe.get("D1");
            if (!((sc == 3 || sc == 4) && (sd == 3 || sd == 4) && sc != sd)) horsBloc++;
            if (sc == 3) c1en3++;
            else if (sc == 4) c1en4++;
        }
        assertEquals(0, horsBloc, "C1 et D1 toujours dans le bloc {3,4}");
        assertTrue(c1en3 > 0 && c1en4 > 0, "C1 tiré au sort entre seed 3 et seed 4");
    }

    @Test
    void les_tetes_de_serie_cinq_a_huit_sont_tirees_au_sort() {
        List<Qualifie> quals = huitPoulesQualifies();
        Set<Integer> positionsE1 = new HashSet<>();
        boolean toujoursDansBloc = true;
        for (long g = 0; g < 200; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            Map<String, Integer> seedDe = new HashMap<>();
            for (int s = 1; s <= 16; s++) if (ps[s] != null) seedDe.put(ps[s].getNumeroLicence(), s);
            for (String pl : new String[]{"E1", "F1", "G1", "H1"}) {
                int s = seedDe.get(pl);
                if (s < 5 || s > 8) toujoursDansBloc = false;
                if (pl.equals("E1")) positionsE1.add(s);
            }
        }
        assertTrue(toujoursDansBloc, "E1-H1 toujours dans le bloc {5,6,7,8}");
        assertTrue(positionsE1.size() > 1, "E1 occupe plusieurs positions du bloc (tirage au sort)");
    }

    @Test
    void le_deuxieme_reste_dans_le_demi_oppose_malgre_le_tirage_des_tetes() {
        // Invariant I.305.3 : le tirage des têtes de série ne doit pas casser la
        // règle du 2e dans le demi opposé à son 1er.
        List<Qualifie> quals = huitPoulesQualifies();
        GenerateurTableauElimination gen = new GenerateurTableauElimination();
        int cadre = 16;
        int[] ordre = gen.ordreDuCadre(cadre);
        boolean[] enHaut = new boolean[cadre + 1];
        for (int pos = 0; pos < cadre; pos++) enHaut[ordre[pos]] = (pos < cadre / 2);

        for (long g = 0; g < 100; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            Map<String, Integer> seedDe = new HashMap<>();
            for (int s = 1; s <= cadre; s++) if (ps[s] != null) seedDe.put(ps[s].getNumeroLicence(), s);
            for (String pl : new String[]{"A", "B", "C", "D", "E", "F", "G", "H"}) {
                Integer s1 = seedDe.get(pl + "1");
                Integer s2 = seedDe.get(pl + "2");
                assertNotNull(s1);
                assertNotNull(s2);
                assertNotEquals(enHaut[s1], enHaut[s2],
                        "graine " + g + " : 2e de " + pl + " doit être dans le demi opposé");
            }
        }
    }

    @Test
    void separation_des_clubs_au_deuxieme_tour_dans_un_tableau_incomplet() {
        // 5 poules -> 10 qualifiés -> cadre 16 (incomplet, 6 byes). Deux 1ers de
        // poule partagent le club "DUP" : ils ne doivent se rencontrer ni au 1er
        // ni au 2e tour (I.304.1 / I.305.1).
        List<Poule> poules = new ArrayList<>();
        String[] noms = {"A", "B", "C", "D", "E"};
        for (int i = 0; i < 5; i++) {
            int base = 2000 - i * 40;
            String club1 = (i < 2) ? "DUP" : ("c" + i);
            poules.add(pouleJouee(noms[i],
                    j(noms[i] + "1", base, club1),
                    j(noms[i] + "2", base - 400, "d" + i),
                    j(noms[i] + "3", base - 800, "e" + i)));
        }
        List<Qualifie> quals = placement.extraireQualifies(poules, 2);
        GenerateurTableauElimination gen = new GenerateurTableauElimination();
        int cadre = 16;
        int[] ordre = gen.ordreDuCadre(cadre);

        for (long g = 0; g < 100; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            // Conflits au 1er tour.
            for (int pos = 0; pos < cadre; pos += 2) {
                Joueur x = ps[ordre[pos]];
                Joueur y = ps[ordre[pos + 1]];
                if (x != null && y != null && !x.getClub().isBlank()
                        && x.getClub().equalsIgnoreCase(y.getClub())) {
                    fail("graine " + g + " : conflit de club au 1er tour");
                }
            }
            // Conflits au 2e tour entre exemptés d'un même bloc de 4.
            for (int bloc = 0; bloc < cadre; bloc += 4) {
                List<Joueur> exemptes = new ArrayList<>();
                for (int pos = bloc; pos < bloc + 4; pos += 2) {
                    Joueur x = ps[ordre[pos]];
                    Joueur y = ps[ordre[pos + 1]];
                    if (x != null && y == null) exemptes.add(x);
                    else if (x == null && y != null) exemptes.add(y);
                }
                for (int i = 0; i < exemptes.size(); i++) {
                    for (int k = i + 1; k < exemptes.size(); k++) {
                        if (!exemptes.get(i).getClub().isBlank()
                                && exemptes.get(i).getClub().equalsIgnoreCase(exemptes.get(k).getClub())) {
                            fail("graine " + g + " : conflit de club au 2e tour entre exemptés");
                        }
                    }
                }
            }
        }
    }
}
