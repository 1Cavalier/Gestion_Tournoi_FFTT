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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Audit B040 : tirage des poules (I.302) et du tableau final (I.304, I.305),
 * confronté au texte des règlements sportifs 2026-2027.
 *
 * <p>Les tests marqués « ECART » prouvent un écart par rapport au texte ; ils
 * sont signalés dans {@code docs/journal/2026-11-06-tache-B040-conformite-tirage.md}
 * pour le correcteur.</p>
 */
class ConformiteTirageI302I305Test {

    private final GenerateurPoules generateurPoules = new GenerateurPoules();
    private final PlacementQualifiesService placement = new PlacementQualifiesService();
    private final GenerateurTableauElimination tableau = new GenerateurTableauElimination();

    private Joueur j(String lic, int pts, String club) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, club, pts);
    }

    private List<String> licences(Poule p) {
        List<String> r = new ArrayList<>();
        for (Joueur x : p.getJoueurs()) r.add(x.getNumeroLicence());
        return r;
    }

    private Poule pouleJouee(String nom, Joueur a, Joueur b, Joueur c) {
        Poule p = new Poule(nom, List.of(a, b, c));
        Joueur[][] paires = {{a, b}, {a, c}, {b, c}};
        for (Joueur[] pr : paires) {
            Match m = new Match(pr[0], pr[1]);
            m.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
            p.ajouterMatch(m);
        }
        return p;
    }

    // --- I.302 : serpentin ---

    @Test
    void i302_serpentin_exact_sans_conflit_de_club() {
        // 12 joueurs, 4 poules de 3 : rangs 1..12 -> A:1,8,9 B:2,7,10 C:3,6,11 D:4,5,12
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= 12; i++) js.add(j("R" + i, 3000 - i * 100, "club" + i));
        List<Poule> poules = generateurPoules.genererPoules(js, 3, true);
        assertEquals(4, poules.size());
        assertEquals(List.of("R1", "R8", "R9"), licences(poules.get(0)));
        assertEquals(List.of("R2", "R7", "R10"), licences(poules.get(1)));
        assertEquals(List.of("R3", "R6", "R11"), licences(poules.get(2)));
        assertEquals(List.of("R4", "R5", "R12"), licences(poules.get(3)));
    }

    @Test
    void i302_deux_joueurs_du_meme_club_sont_dans_des_poules_differentes_quand_possible() {
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= 12; i++) js.add(j("R" + i, 3000 - i * 100, (i % 6 == 0 ? "X" : "club" + i)));
        // R6 et R12 : même club X
        List<Poule> poules = generateurPoules.genererPoules(js, 3, true);
        for (Poule p : poules) {
            long nb = p.getJoueurs().stream().filter(x -> "X".equals(x.getClub())).count();
            assertTrue(nb <= 1, "poule " + p.getNom() + " : deux joueurs du club X");
        }
    }

    /**
     * ECART (I.302) : la séparation des clubs déplace le joueur d'un autre rang
     * en fin de poule ; une poule n'est plus ordonnée par points décroissants,
     * donc le numéro de place dans la poule (utilisé par I.301) ne suit plus les
     * points (I.302 : « les joueurs sont placés [...] en tenant compte des
     * points »).
     */
    @Test
    void i302_ECART_apres_separation_des_clubs_chaque_poule_reste_ordonnee_par_points() {
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            // R1 et R8 (tous deux en poule A après serpentin) partagent le club X.
            js.add(j("R" + i, 3000 - i * 100, (i == 1 || i == 8) ? "X" : "club" + i));
        }
        List<Poule> poules = generateurPoules.genererPoules(js, 3, true);
        for (Poule p : poules) {
            List<Joueur> l = p.getJoueurs();
            for (int k = 1; k < l.size(); k++) {
                assertTrue(l.get(k - 1).getPoints() >= l.get(k).getPoints(),
                        "poule " + p.getNom() + " non ordonnée : " + licences(p));
            }
        }
    }

    /**
     * ECART (I.302 / principe des têtes de série de poule) : la séparation des
     * clubs peut déplacer le numéro 1 hors de la tête de sa poule ; les N
     * meilleurs joueurs ne sont plus chacun en tête d'une poule distincte.
     */
    @Test
    void i302_ECART_les_meilleurs_joueurs_restent_tetes_de_poule_apres_separation() {
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            js.add(j("R" + i, 3000 - i * 100, (i == 1 || i == 8) ? "X" : "club" + i));
        }
        List<Poule> poules = generateurPoules.genererPoules(js, 3, true);
        List<String> tetes = new ArrayList<>();
        for (Poule p : poules) tetes.add(p.getJoueurs().get(0).getNumeroLicence());
        assertEquals(List.of("R1", "R2", "R3", "R4"), tetes.stream().sorted().toList(),
                "les 4 meilleurs doivent être têtes de poule, obtenu : " + tetes);
    }

    // --- I.304.2 : structure du cadre ---

    @Test
    void i304_2_les_2_puissance_k_premiers_ne_se_rencontrent_qu_au_tour_attendu() {
        for (int cadre : new int[]{4, 8, 16, 32, 64}) {
            int[] ordre = tableau.ordreDuCadre(cadre);
            int[] pos = new int[cadre + 1];
            for (int i = 0; i < cadre; i++) pos[ordre[i]] = i;
            // seeds a<b se rencontrent au tour t (1 = premier tour) où
            // pos_a / 2^t == pos_b / 2^t pour la première fois.
            int tours = Integer.numberOfTrailingZeros(cadre);
            for (int a = 1; a <= cadre; a++) {
                for (int b = a + 1; b <= cadre; b++) {
                    int t = 1;
                    while ((pos[a] >> t) != (pos[b] >> t)) t++;
                    int restants = tours - t + 1; // 1 = finale
                    int groupe = Integer.highestOneBit(b - 1) * 2; // plus petite puissance de 2 >= b
                    // Les `groupe` premiers ne se rencontrent qu'à partir du tour
                    // dont il reste log2(groupe) tours.
                    int minRestants = Integer.numberOfTrailingZeros(groupe);
                    assertTrue(restants >= 1 && t >= 1);
                    assertTrue(restants <= minRestants || b > groupe,
                            "cadre " + cadre + " seeds " + a + "/" + b + " se rencontrent trop tôt");
                }
            }
            assertTrue(pos[1] < cadre / 2, "seed 1 en haut");
            assertTrue(pos[2] >= cadre / 2, "seed 2 en bas");
        }
    }

    // --- I.305.2 : vainqueurs placés selon le NUMÉRO de poule ---

    private List<Qualifie> huitPoulesVainqueursInverses(int qualifiesParPoule) {
        // Le vainqueur de la poule A est le plus faible des vainqueurs, celui de
        // la poule H le plus fort : ordre des points inverse de l'ordre des poules.
        List<Poule> poules = new ArrayList<>();
        String[] noms = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (int i = 0; i < 8; i++) {
            int base = 1500 + i * 50;
            poules.add(pouleJouee(noms[i],
                    j(noms[i] + "1", base, "c" + i + "1"),
                    j(noms[i] + "2", base - 400, "c" + i + "2"),
                    j(noms[i] + "3", base - 800, "c" + i + "3")));
        }
        return placement.extraireQualifies(poules, qualifiesParPoule);
    }

    /**
     * ECART (I.305.2) : « Le vainqueur de la poule 1 en haut du tableau ; le
     * vainqueur de la poule 2 en bas du tableau ; [...] les vainqueurs des poules
     * 3 et 4 dans un groupe 3e-4e ; [...] 5 à 8 dans un groupe 5e-8e ». Le texte
     * désigne des numéros de poule ; le code classe les vainqueurs par points.
     */
    @Test
    void i305_2_ECART_vainqueurs_places_selon_le_numero_de_poule_et_non_leurs_points() {
        List<Qualifie> quals = huitPoulesVainqueursInverses(1);
        for (long g = 0; g < 50; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            Map<String, Integer> seedDe = new HashMap<>();
            for (int s = 1; s < ps.length; s++) if (ps[s] != null) seedDe.put(ps[s].getNumeroLicence(), s);
            assertEquals(1, seedDe.get("A1"), "graine " + g + " : vainqueur poule 1 en haut");
            assertEquals(2, seedDe.get("B1"), "graine " + g + " : vainqueur poule 2 en bas");
            for (String p : new String[]{"C1", "D1"}) {
                int s = seedDe.get(p);
                assertTrue(s == 3 || s == 4, "graine " + g + " : " + p + " hors groupe 3-4 (seed " + s + ")");
            }
            for (String p : new String[]{"E1", "F1", "G1", "H1"}) {
                int s = seedDe.get(p);
                assertTrue(s >= 5 && s <= 8, "graine " + g + " : " + p + " hors groupe 5-8 (seed " + s + ")");
            }
        }
    }

    // --- I.305.3 / I.305.4 : 2es et 3es ---

    private void verifierDemisEtQuarts(int nbPoules, int cadre, long graines) {
        List<Poule> poules = new ArrayList<>();
        for (int i = 0; i < nbPoules; i++) {
            String nom = "P" + (char) ('A' + i);
            int base = 3000 - i * 20;
            poules.add(pouleJouee(nom,
                    j(nom + "1", base, "c" + i + "1"),
                    j(nom + "2", base - 500, "c" + i + "2"),
                    j(nom + "3", base - 900, "c" + i + "3")));
        }
        List<Qualifie> quals = placement.extraireQualifies(poules, 3);
        int[] ordre = tableau.ordreDuCadre(cadre);
        int[] pos = new int[cadre + 1];
        for (int i = 0; i < cadre; i++) pos[ordre[i]] = i;
        for (long g = 0; g < graines; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            Map<String, Integer> posDe = new HashMap<>();
            for (int s = 1; s <= cadre; s++) if (ps[s] != null) posDe.put(ps[s].getNumeroLicence(), pos[s]);
            for (Poule p : poules) {
                int p1 = posDe.get(p.getNom() + "1"), p2 = posDe.get(p.getNom() + "2"),
                        p3 = posDe.get(p.getNom() + "3");
                int demi = cadre / 2, quart = cadre / 4;
                assertNotEquals(p1 / demi, p2 / demi, "g" + g + " " + p.getNom() + " : 2e pas dans le demi opposé");
                assertEquals(p2 / demi, p3 / demi, "g" + g + " " + p.getNom() + " : 3e pas dans le demi du 2e");
                assertNotEquals(p2 / quart, p3 / quart, "g" + g + " " + p.getNom() + " : 3e dans le même quart que le 2e");
            }
        }
    }

    @Test
    void i305_4_trois_qualifies_quatre_poules() {
        verifierDemisEtQuarts(4, 16, 200);
    }

    @Test
    void i305_4_trois_qualifies_huit_poules() {
        verifierDemisEtQuarts(8, 32, 200);
    }

    // --- I.305.1 : clubs au premier tour ---

    @Test
    void i305_1_deux_qualifies_du_meme_club_ne_se_rencontrent_pas_au_premier_tour_tableau_complet() {
        // 8 poules x 2 = 16 qualifiés ; les joueurs A1 et E2, B1 et F2... partagent un club.
        List<Poule> poules = new ArrayList<>();
        String[] noms = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (int i = 0; i < 8; i++) {
            int base = 2000 - i * 50;
            String c1 = "K" + (i % 4);          // 1ers : 4 clubs, 2 vainqueurs chacun
            String c2 = "K" + ((i + 1) % 4);    // 2es  : mêmes clubs
            poules.add(pouleJouee(noms[i],
                    j(noms[i] + "1", base, c1), j(noms[i] + "2", base - 500, c2),
                    j(noms[i] + "3", base - 900, "z" + i)));
        }
        List<Qualifie> quals = placement.extraireQualifies(poules, 2);
        int[] ordre = tableau.ordreDuCadre(16);
        for (long g = 0; g < 200; g++) {
            Joueur[] ps = placement.calculerPlacement(quals, g);
            for (int pos = 0; pos < 16; pos += 2) {
                Joueur x = ps[ordre[pos]], y = ps[ordre[pos + 1]];
                assertFalse(x != null && y != null && x.getClub().equals(y.getClub()),
                        "graine " + g + " : " + x.getNumeroLicence() + " contre " + y.getNumeroLicence()
                                + " (club " + x.getClub() + ") au 1er tour");
            }
        }
    }
}
