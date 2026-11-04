package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassementPouleServiceTest {

    private final ClassementPouleService service = new ClassementPouleService();

    private Joueur joueur(String licence) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence, Sexe.HOMME, "Club", 1000);
    }

    private Match matchTermine(Joueur a, Joueur b, int[][] sets) {
        Match m = new Match(a, b);
        List<Manche> manches = new ArrayList<>();
        for (int[] s : sets) {
            manches.add(new Manche(s[0], s[1]));
        }
        m.definirScore(manches);
        return m;
    }

    @Test
    void classe_une_poule_sans_egalite_par_victoires() {
        Joueur x = joueur("X"), y = joueur("Y"), z = joueur("Z");
        Poule p = new Poule("A", List.of(x, y, z));
        p.ajouterMatch(matchTermine(x, y, new int[][]{{11, 5}, {11, 5}, {11, 5}}));
        p.ajouterMatch(matchTermine(x, z, new int[][]{{11, 5}, {11, 5}, {11, 5}}));
        p.ajouterMatch(matchTermine(y, z, new int[][]{{11, 5}, {11, 5}, {11, 5}}));

        List<LigneClassement> c = service.calculer(p);

        assertEquals("X", c.get(0).getJoueur().getNumeroLicence());
        assertEquals("Y", c.get(1).getJoueur().getNumeroLicence());
        assertEquals("Z", c.get(2).getJoueur().getNumeroLicence());
    }

    @Test
    void une_defaite_rapporte_un_point_de_classement() {
        Joueur x = joueur("X"), y = joueur("Y");
        Poule p = new Poule("A", List.of(x, y));
        p.ajouterMatch(matchTermine(x, y, new int[][]{{11, 5}, {11, 5}, {11, 5}}));

        List<LigneClassement> c = service.calculer(p);
        // X : 1 victoire = 2 pts ; Y : 1 défaite = 1 pt
        assertEquals(2, c.get(0).getPointsClassement());
        assertEquals(1, c.get(1).getPointsClassement());
    }

    @Test
    void triangulaire_est_departage_par_les_manches_entre_ex_aequo() {
        // U bat V 3-0, V bat W 3-0, W bat U 3-2 : tous 1V-1D.
        Joueur u = joueur("U"), v = joueur("V"), w = joueur("W");
        Poule p = new Poule("B", List.of(u, v, w));
        p.ajouterMatch(matchTermine(u, v, new int[][]{{11, 5}, {11, 5}, {11, 5}}));
        p.ajouterMatch(matchTermine(v, w, new int[][]{{11, 5}, {11, 5}, {11, 5}}));
        p.ajouterMatch(matchTermine(w, u, new int[][]{{11, 9}, {9, 11}, {11, 9}, {9, 11}, {11, 9}}));

        List<LigneClassement> c = service.calculer(p);

        // Départage par quotient de manches : U devant V devant W.
        assertEquals("U", c.get(0).getJoueur().getNumeroLicence());
        assertEquals("V", c.get(1).getJoueur().getNumeroLicence());
        assertEquals("W", c.get(2).getJoueur().getNumeroLicence());
    }

    @Test
    void attribue_les_rangs_de_un_a_n() {
        Joueur x = joueur("X"), y = joueur("Y"), z = joueur("Z");
        Poule p = new Poule("A", List.of(x, y, z));
        p.ajouterMatch(matchTermine(x, y, new int[][]{{11, 5}, {11, 5}, {11, 5}}));
        p.ajouterMatch(matchTermine(x, z, new int[][]{{11, 5}, {11, 5}, {11, 5}}));
        p.ajouterMatch(matchTermine(y, z, new int[][]{{11, 5}, {11, 5}, {11, 5}}));

        List<LigneClassement> c = service.calculer(p);
        assertEquals(1, c.get(0).getRang());
        assertEquals(2, c.get(1).getRang());
        assertEquals(3, c.get(2).getRang());
    }
}
