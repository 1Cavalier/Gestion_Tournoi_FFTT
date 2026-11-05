package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatSaisieScore;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Formulaire de saisie du score d'un match de phase finale, sans JavaFX. */
class SaisieScorePhaseFinaleTest {

    private static Match match() {
        return new Match(new Joueur("A", "NomA", "PrenomA", Sexe.HOMME, "C", 1000),
                new Joueur("B", "NomB", "PrenomB", Sexe.HOMME, "C", 900));
    }

    private static String[] l(String a, String b) {
        return new String[] {a, b};
    }

    @Test
    void cinq_lignes_au_meilleur_des_cinq_sept_au_meilleur_des_sept() {
        Match m = match();
        assertEquals(5, SaisieScorePhaseFinale.nombreLignes(m));
        m.setNombreManchesGagnantes(4);
        assertEquals(7, SaisieScorePhaseFinale.nombreLignes(m));
    }

    @Test
    void valeurs_initiales_vides_puis_reprises_pour_une_correction() {
        Match m = match();
        assertTrue(SaisieScorePhaseFinale.valeursInitiales(m).stream().allMatch(v -> v[0].isEmpty() && v[1].isEmpty()));
        m.definirScore(List.of(new Manche(11, 4), new Manche(11, 6), new Manche(13, 11)));
        List<String[]> v = SaisieScorePhaseFinale.valeursInitiales(m);
        assertEquals(5, v.size());
        assertArrayEquals(new String[] {"13", "11"}, v.get(2));
        assertArrayEquals(new String[] {"", ""}, v.get(3));
    }

    @Test
    void lecture_ignore_les_lignes_vides_de_fin() {
        SaisieScorePhaseFinale.Lecture r = SaisieScorePhaseFinale.lire(List.of(
                l("11", "4"), l(" 9 ", "11"), l("11", "7"), l("12", "10"), l("", "")));
        assertTrue(r.estValide());
        assertEquals(List.of(new Manche(11, 4), new Manche(9, 11), new Manche(11, 7), new Manche(12, 10)), r.manches());
    }

    @Test
    void ligne_vide_au_milieu_refusee() {
        SaisieScorePhaseFinale.Lecture r = SaisieScorePhaseFinale.lire(List.of(
                l("11", "4"), l("", ""), l("11", "7")));
        assertFalse(r.estValide());
        assertEquals("Manche 2 : elle est vide alors que la manche 3 est remplie.", r.erreur());
    }

    @Test
    void ligne_a_moitie_remplie_ou_non_numerique_refusee() {
        assertEquals("Manche 1 : indiquez les points des deux joueurs.",
                SaisieScorePhaseFinale.lire(List.<String[]>of(l("11", ""))).erreur());
        assertEquals("Manche 1 : saisissez des nombres entiers de points.",
                SaisieScorePhaseFinale.lire(List.<String[]>of(l("onze", "3"))).erreur());
        assertEquals("Manche 1 : saisissez des nombres entiers de points.",
                SaisieScorePhaseFinale.lire(List.<String[]>of(l("-1", "3"))).erreur());
    }

    @Test
    void aucune_ligne_remplie_donne_une_liste_vide_que_le_coeur_refusera() {
        SaisieScorePhaseFinale.Lecture r = SaisieScorePhaseFinale.lire(List.<String[]>of(l("", ""), l("", "")));
        assertTrue(r.estValide());
        assertTrue(r.manches().isEmpty());
    }

    @Test
    void messages_de_refus_precis() {
        Match m = match();
        assertEquals("Manche 2 : à 10-10, la manche continue jusqu'à deux points d'écart (12-10, 13-11…).",
                SaisieScorePhaseFinale.messageRefus(
                        ResultatSaisieScore.refusManche(ResultatSaisieScore.Motif.MANCHE_ECART_INSUFFISANT, 2), m));
        assertEquals("Le match n'est pas fini : il faut 3 manches gagnées pour l'emporter.",
                SaisieScorePhaseFinale.messageRefus(
                        ResultatSaisieScore.refus(ResultatSaisieScore.Motif.MATCH_NON_DECIDE), m));
        for (ResultatSaisieScore.Motif motif : ResultatSaisieScore.Motif.values()) {
            ResultatSaisieScore r = motif == ResultatSaisieScore.Motif.VALIDE ? ResultatSaisieScore.valide()
                    : ResultatSaisieScore.refusManche(motif, 1);
            assertFalse(SaisieScorePhaseFinale.messageRefus(r, m).isBlank(), motif.name());
        }
    }
}
