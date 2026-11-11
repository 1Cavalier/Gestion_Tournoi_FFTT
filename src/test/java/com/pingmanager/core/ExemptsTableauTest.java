package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Qualifie;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.TableauElimination;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Place des exempts dans le tableau final (décision de Quentin, 11 novembre
 * 2026) : un tableau incomplet ne fait sauter qu'un seul tour, le 2e tour est
 * toujours complet, et les exempts vont d'abord aux 1ers de poule.
 */
class ExemptsTableauTest {

    private final PlacementQualifiesService service = new PlacementQualifiesService();

    private List<Qualifie> qualifies(int poules, int parPoule, Set<String> premiers) {
        List<Qualifie> q = new ArrayList<>();
        for (int p = 0; p < poules; p++) {
            String nom = "P" + (p + 1);
            for (int r = 1; r <= parPoule; r++) {
                Joueur j = new Joueur("L" + p + "-" + r, "N" + p, "R" + r, Sexe.HOMME,
                        "Club" + ((p + r * 3) % 7), 2000 - p - r * 300);
                if (r == 1) {
                    premiers.add(j.getNumeroLicence());
                }
                q.add(new Qualifie(j, nom, r));
            }
        }
        return q;
    }

    private void verifier(int poules, int parPoule, long graine) {
        Set<String> premiers = new HashSet<>();
        List<Qualifie> q = qualifies(poules, parPoule, premiers);
        int n = q.size();
        TableauElimination t = service.placer(q, graine);
        int cadre = t.getCadre();
        int exempts = cadre - n;
        if (exempts == 0 || cadre < 4) {
            return;
        }
        String cas = poules + " poules x " + parPoule + ", graine " + graine;
        assertEquals(n - cadre / 2, t.getTours().get(0).getNombreMatchs(),
                "1er tour : seuls les joueurs non exempts jouent (" + cas + ")");
        assertEquals(cadre / 4, t.getTours().get(1).getNombreMatchs(),
                "2e tour complet, aucun saut de deux tours (" + cas + ")");
        Set<String> jouentLe1erTour = new HashSet<>();
        for (Match m : t.getTours().get(0).getMatchs()) {
            jouentLe1erTour.add(m.getJoueurA().getNumeroLicence());
            jouentLe1erTour.add(m.getJoueurB().getNumeroLicence());
        }
        int premiersExempts = 0;
        for (String l : premiers) {
            if (!jouentLe1erTour.contains(l)) {
                premiersExempts++;
            }
        }
        assertEquals(Math.min(exempts, poules), premiersExempts,
                "les exempts vont d'abord aux 1ers de poule (" + cas + ")");
    }

    @Test
    void deux_qualifies_par_poule_le_deuxieme_tour_est_toujours_complet() {
        for (int poules = 2; poules <= 40; poules++) {
            for (long graine = 0; graine < 15; graine++) {
                verifier(poules, 2, graine);
            }
        }
    }

    @Test
    void un_ou_trois_qualifies_par_poule_le_deuxieme_tour_est_toujours_complet() {
        for (int poules = 2; poules <= 24; poules++) {
            for (long graine = 0; graine < 8; graine++) {
                verifier(poules, 1, graine);
                verifier(poules, 3, graine);
            }
        }
    }

    @Test
    void dix_huit_qualifies_un_seul_tour_de_32es_puis_des_16es_pleins() {
        // 9 poules, 18 qualifiés, cadre 32 : 14 exempts (les 9 1ers + 5 2es),
        // 2 matchs de 32es, 8 matchs de 16es.
        Set<String> premiers = new HashSet<>();
        TableauElimination t = service.placer(qualifies(9, 2, premiers), 7L);
        assertEquals(32, t.getCadre());
        assertEquals("16es de finale", t.getTours().get(0).getNom());
        assertEquals(2, t.getTours().get(0).getNombreMatchs());
        assertEquals(8, t.getTours().get(1).getNombreMatchs());
        for (Match m : t.getTours().get(0).getMatchs()) {
            assertFalse(premiers.contains(m.getJoueurA().getNumeroLicence()));
            assertFalse(premiers.contains(m.getJoueurB().getNumeroLicence()));
        }
    }
}
