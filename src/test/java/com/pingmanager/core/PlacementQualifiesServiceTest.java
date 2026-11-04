package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Qualifie;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlacementQualifiesServiceTest {

    private final PlacementQualifiesService service = new PlacementQualifiesService();
    private final GenerateurTableauElimination generateur = new GenerateurTableauElimination();

    private Joueur joueur(String lic, int pts, String club) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, club, pts);
    }

    /** Poule de 3 déjà jouée : a bat b et c, b bat c. Classement a > b > c. */
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

    private List<Poule> quatrePoules() {
        List<Poule> poules = new ArrayList<>();
        poules.add(pouleJouee("A", joueur("A1", 2000, "Cl1"), joueur("A2", 1500, "Cl2"), joueur("A3", 1000, "Cl3")));
        poules.add(pouleJouee("B", joueur("B1", 1900, "Cl4"), joueur("B2", 1450, "Cl5"), joueur("B3", 1000, "Cl6")));
        poules.add(pouleJouee("C", joueur("C1", 1800, "Cl7"), joueur("C2", 1400, "Cl8"), joueur("C3", 1000, "Cl9")));
        poules.add(pouleJouee("D", joueur("D1", 1700, "Cl10"), joueur("D2", 1350, "Cl11"), joueur("D3", 1000, "Cl12")));
        return poules;
    }

    private boolean[] demiParSeed(int cadre) {
        int[] ordre = generateur.ordreDuCadre(cadre);
        boolean[] h = new boolean[cadre + 1];
        for (int pos = 0; pos < cadre; pos++) {
            h[ordre[pos]] = (pos < cadre / 2);
        }
        return h;
    }

    private int[] quartParSeed(int cadre) {
        int[] ordre = generateur.ordreDuCadre(cadre);
        int[] q = new int[cadre + 1];
        for (int pos = 0; pos < cadre; pos++) {
            q[ordre[pos]] = pos / (cadre / 4);
        }
        return q;
    }

    private Map<String, Integer> seedParJoueur(Joueur[] parSeed, int cadre) {
        Map<String, Integer> m = new HashMap<>();
        for (int s = 1; s <= cadre; s++) {
            if (parSeed[s] != null) {
                m.put(parSeed[s].getNumeroLicence(), s);
            }
        }
        return m;
    }

    @Test
    void extrait_le_bon_nombre_de_qualifies() {
        List<Qualifie> quals = service.extraireQualifies(quatrePoules(), 2);
        assertEquals(8, quals.size());
        assertEquals(4, quals.stream().filter(q -> q.rangDansPoule() == 1).count());
        assertEquals(4, quals.stream().filter(q -> q.rangDansPoule() == 2).count());
    }

    @Test
    void le_premier_de_poule_est_bien_le_vainqueur() {
        List<Qualifie> quals = service.extraireQualifies(quatrePoules(), 2);
        boolean a1Premier = quals.stream()
                .anyMatch(q -> q.joueur().getNumeroLicence().equals("A1") && q.rangDansPoule() == 1);
        assertTrue(a1Premier);
    }

    @Test
    void chaque_deuxieme_est_dans_le_demi_oppose_a_son_premier() {
        List<Qualifie> quals = service.extraireQualifies(quatrePoules(), 2);
        boolean[] enHaut = demiParSeed(8);

        // Invariant vérifié sur de nombreuses graines : le tirage est aléatoire
        // mais la contrainte I.305 doit toujours tenir.
        for (long graine = 0; graine < 100; graine++) {
            Joueur[] parSeed = service.calculerPlacement(quals, graine);
            Map<String, Integer> seedDe = seedParJoueur(parSeed, 8);
            for (String pl : new String[]{"A", "B", "C", "D"}) {
                Integer s1 = seedDe.get(pl + "1");
                Integer s2 = seedDe.get(pl + "2");
                assertNotNull(s1);
                assertNotNull(s2);
                assertNotEquals(enHaut[s1], enHaut[s2],
                        "graine " + graine + " : le 2e de " + pl + " doit être dans le demi opposé");
            }
        }
    }

    @Test
    void les_premiers_sont_tetes_de_serie() {
        List<Qualifie> quals = service.extraireQualifies(quatrePoules(), 2);
        for (long graine = 0; graine < 50; graine++) {
            Joueur[] parSeed = service.calculerPlacement(quals, graine);
            Map<String, Integer> seedDe = seedParJoueur(parSeed, 8);
            for (String pl : new String[]{"A", "B", "C", "D"}) {
                assertTrue(seedDe.get(pl + "1") <= 4, "les 1ers occupent les seeds 1 à 4");
            }
        }
    }

    @Test
    void les_troisiemes_sont_dans_le_meme_demi_que_leur_deuxieme_quart_oppose() {
        List<Qualifie> quals = service.extraireQualifies(quatrePoules(), 3);
        assertEquals(12, quals.size());
        boolean[] enHaut = demiParSeed(16);
        int[] quart = quartParSeed(16);

        for (long graine = 0; graine < 100; graine++) {
            Joueur[] parSeed = service.calculerPlacement(quals, graine);
            Map<String, Integer> seedDe = seedParJoueur(parSeed, 16);
            for (String pl : new String[]{"A", "B", "C", "D"}) {
                Integer s2 = seedDe.get(pl + "2");
                Integer s3 = seedDe.get(pl + "3");
                if (s2 != null && s3 != null) {
                    // Même demi que le 2e.
                    assertEquals(enHaut[s2], enHaut[s3],
                            "le 3e de " + pl + " doit être dans le même demi que son 2e");
                    // Quart opposé à celui du 2e.
                    assertNotEquals(quart[s2], quart[s3],
                            "le 3e de " + pl + " doit être dans le quart opposé à son 2e");
                }
            }
        }
    }

    @Test
    void le_tirage_est_reproductible_avec_la_meme_graine() {
        List<Poule> poules = quatrePoules();
        TableauElimination t1 = service.construireTableau(poules, 2, 42L);
        TableauElimination t2 = service.construireTableau(poules, 2, 42L);

        Tour p1 = t1.getPremierTour();
        Tour p2 = t2.getPremierTour();
        assertEquals(p1.getNombreMatchs(), p2.getNombreMatchs());
        for (int i = 0; i < p1.getNombreMatchs(); i++) {
            assertEquals(p1.getMatchs().get(i).getJoueurA().getNumeroLicence(),
                    p2.getMatchs().get(i).getJoueurA().getNumeroLicence());
        }
    }

    @Test
    void construit_un_tableau_complet_jouable_de_bout_en_bout() {
        ProgressionTableau progression = new ProgressionTableau();
        TableauElimination t = service.construireTableau(quatrePoules(), 2, 7L);

        int garde = 0;
        for (Tour tour : t.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.getStatut() == StatutMatch.A_JOUER && !m.estBye()) {
                    m.definirScore(List.of(new Manche(11, 5), new Manche(11, 7), new Manche(11, 9)));
                }
            }
            progression.propager(t);
            if (garde++ > 20) break;
        }
        Match finale = t.getFinale().getMatchs().get(0);
        assertEquals(StatutMatch.TERMINE, finale.getStatut());
        assertNotNull(finale.getVainqueur());
        assertFalse(finale.getVainqueur().estADeterminer());
    }

    @Test
    void refuse_moins_de_deux_qualifies() {
        List<Qualifie> unSeul = List.of(
                new Qualifie(joueur("X", 1000, "C"), "A", 1));
        assertThrows(IllegalArgumentException.class, () -> service.placer(unSeul, 0L));
    }
}
