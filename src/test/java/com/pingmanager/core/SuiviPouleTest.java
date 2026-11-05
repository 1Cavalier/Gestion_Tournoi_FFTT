package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuiviPouleTest {

    private static Poule poule(int taille, int qualifies) {
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 1; i <= taille; i++) {
            joueurs.add(new Joueur("L" + i, "N" + i, "P" + i, Sexe.HOMME, "Club" + i, 2000 - i * 10));
        }
        Poule p = new Poule("A", joueurs);
        new GenerateurMatchsPoule().genererMatchs(p, qualifies);
        return p;
    }

    @Test
    void tours_selon_l_ordre_officiel_i_301() {
        // I.301.2 : poule de 3, un match par tour.
        Poule p3 = poule(3, 2);
        assertEquals(List.of(1, 2, 3), tours(p3));
        // I.301.4 : poule de 4, deux matchs par tour.
        assertEquals(List.of(1, 1, 2, 2, 3, 3), tours(poule(4, 2)));
        // I.301.6 : poule de 5, cinq tours de deux matchs.
        assertEquals(List.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5), tours(poule(5, 2)));
        // I.301.8 : poule de 6, cinq tours de trois matchs.
        assertEquals(15, poule(6, 2).getMatchs().size());
        assertEquals(5, SuiviPoule.tourDuMatch(poule(6, 2), 14));
    }

    private static List<Integer> tours(Poule p) {
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < p.getMatchs().size(); i++) {
            res.add(SuiviPoule.tourDuMatch(p, i));
        }
        return res;
    }

    @Test
    void place_qualificative_en_cours_puis_qualifies_acquis_a_la_fin() {
        Poule p = poule(3, 2);
        SuiviPoule suivi = new SuiviPoule();
        Match premier = p.getMatchs().get(0); // I.301.2 : 1-3
        premier.definirScore(List.of(new Manche(11, 3), new Manche(11, 3), new Manche(11, 3)));
        assertEquals(1, SuiviPoule.matchsTermines(p));

        List<SuiviPoule.Ligne> enCours = suivi.classementEnDirect(p, 2);
        assertEquals(SuiviPoule.Qualification.PLACE_QUALIFICATIVE, enCours.get(0).qualification());
        assertEquals(premier.getJoueurA(), enCours.get(0).classement().getJoueur());
        assertEquals(SuiviPoule.Qualification.HORS_PLACE_QUALIFICATIVE, enCours.get(2).qualification());

        for (Match m : p.getMatchs()) {
            if (m != premier) {
                m.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
            }
        }
        List<SuiviPoule.Ligne> fin = suivi.classementEnDirect(p, 2);
        assertEquals(3, SuiviPoule.matchsTermines(p));
        assertEquals(SuiviPoule.Qualification.QUALIFIE, fin.get(0).qualification());
        assertEquals(SuiviPoule.Qualification.QUALIFIE, fin.get(1).qualification());
        assertTrue(fin.get(1).enPlaceQualificative());
        assertEquals(SuiviPoule.Qualification.NON_QUALIFIE, fin.get(2).qualification());
        assertFalse(fin.get(2).enPlaceQualificative());
    }
}
