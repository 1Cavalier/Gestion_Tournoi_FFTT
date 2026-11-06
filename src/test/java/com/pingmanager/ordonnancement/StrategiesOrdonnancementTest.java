package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StrategieOrdonnancement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stratégies du club (B025) : libellés et explications en clair, traduction en
 * poids de l'ordonnanceur, effet sur l'ordre des propositions, et calcul indexé
 * identique au calcul match par match.
 */
class StrategiesOrdonnancementTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2027, 4, 10, 9, 0);

    private static Joueur j(String l) {
        return new Joueur(l, "N" + l, "P" + l, Sexe.HOMME, "C", 1000);
    }

    @Test
    void trois_strategies_chacune_avec_libelle_explication_et_contrepartie() {
        assertEquals(3, StrategieOrdonnancement.values().length);
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            assertFalse(s.getLibelle().isBlank());
            assertTrue(s.getExplication().length() > 60, "explication en clair : " + s);
            assertFalse(s.getContrepartie().isBlank());
        }
        assertEquals("Avancer les tableaux commencés tôt", StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES.getLibelle());
        assertEquals("Tout faire avancer petit à petit", StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT.getLibelle());
        assertEquals("Faire attendre le moins possible les joueurs", StrategieOrdonnancement.MINIMISER_ATTENTE.getLibelle());
    }

    @Test
    void strategie_par_defaut_garde_exactement_les_poids_historiques() {
        PoidsOrdonnancement historique = new PoidsOrdonnancement();
        PoidsOrdonnancement p = PoidsOrdonnancement.pour(StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals(StrategieOrdonnancement.MINIMISER_ATTENTE, StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals(historique.getPoidsVague(), p.getPoidsVague());
        assertEquals(historique.getPoidsAttente(), p.getPoidsAttente());
        assertEquals(historique.getPoidsDeblocage(), p.getPoidsDeblocage());
        assertEquals(historique.getPointsParMinuteAttente(), p.getPointsParMinuteAttente());
        assertEquals(0.0, p.getPoidsTableauCommence());
        assertEquals(0.0, historique.getPoidsTableauCommence(), "critère inactif par défaut");
    }

    @Test
    void petit_a_petit_la_poule_passe_devant_un_tour_suivant_qui_attend() {
        Ordonnanceur ord = new Ordonnanceur(1, PoidsOrdonnancement.pour(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT));
        MatchOrdonnancable tour = new MatchOrdonnancable(new Match(j("1"), j("2")), "A", "A", PhaseMatch.TOUR_SUIVANT_TABLEAU);
        MatchOrdonnancable poule = new MatchOrdonnancable(new Match(j("3"), j("4")), "B", "B", PhaseMatch.POULE);
        ord.ajouterMatch(tour);
        ord.ajouterMatch(poule);
        tour.setPretDepuis(T0.minusMinutes(60));
        poule.setPretDepuis(T0);
        assertSame(poule, ord.proposerLancements(T0).get(0).getMatch());

        // Avec les poids historiques, l'heure d'attente l'emporte.
        Ordonnanceur defaut = new Ordonnanceur(1);
        defaut.ajouterMatch(tour);
        defaut.ajouterMatch(poule);
        assertSame(tour, defaut.proposerLancements(T0).get(0).getMatch());
    }

    @Test
    void avancer_les_tableaux_commences_le_tableau_le_plus_ancien_passe_devant() {
        Ordonnanceur ord = new Ordonnanceur(3, PoidsOrdonnancement.pour(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES));
        // Le tableau A a lancé un match à 9 h ; le B à 10 h 30.
        MatchOrdonnancable enCoursA = new MatchOrdonnancable(new Match(j("1"), j("2")), "A", "A", PhaseMatch.POULE);
        MatchOrdonnancable enCoursB = new MatchOrdonnancable(new Match(j("3"), j("4")), "B", "B", PhaseMatch.POULE);
        MatchOrdonnancable suiteA = new MatchOrdonnancable(new Match(j("5"), j("6")), "A", "A", PhaseMatch.TOUR_SUIVANT_TABLEAU);
        MatchOrdonnancable pouleB = new MatchOrdonnancable(new Match(j("7"), j("8")), "B", "B", PhaseMatch.POULE);
        for (MatchOrdonnancable m : List.of(enCoursA, enCoursB, pouleB, suiteA)) {
            ord.ajouterMatch(m);
        }
        ord.affecter(enCoursA, 1, T0);
        ord.affecter(enCoursB, 2, T0.plusMinutes(90));
        LocalDateTime maintenant = T0.plusMinutes(100);
        pouleB.setPretDepuis(T0.plusMinutes(90));
        suiteA.setPretDepuis(maintenant);
        List<PropositionLancement> props = ord.proposerLancements(maintenant);
        assertEquals(1, props.size(), "une seule table libre");
        assertSame(suiteA, props.get(0).getMatch());
        assertTrue(props.get(0).getExplication().contains("tableau commencé depuis 100 min"),
                props.get(0).getExplication());
        assertEquals(100, ord.minutesDepuisDebutTableau(suiteA, maintenant));
        assertEquals(10, ord.minutesDepuisDebutTableau(pouleB, maintenant));
    }

    /** Le calcul indexé d'une vague donne les mêmes scores et explications que le calcul match par match. */
    @Test
    void calcul_indexe_identique_au_calcul_match_par_match() {
        Random r = new Random(17);
        List<Joueur> js = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            js.add(j("L" + i));
        }
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            Ordonnanceur ord = new Ordonnanceur(8, PoidsOrdonnancement.pour(s));
            List<MatchOrdonnancable> tous = new ArrayList<>();
            for (int i = 0; i < 120; i++) {
                Joueur a = js.get(r.nextInt(js.size()));
                Joueur b = r.nextInt(10) == 0 ? Joueur.aDeterminer() : js.get(r.nextInt(js.size()));
                if (a.equals(b)) {
                    continue;
                }
                Match m = new Match(a, b);
                if (r.nextInt(5) == 0) {
                    m.setStatut(StatutMatch.TERMINE);
                }
                MatchOrdonnancable mo = new MatchOrdonnancable(m, "T" + r.nextInt(4), null, PhaseMatch.values()[r.nextInt(3)]);
                mo.setPretDepuis(T0.minusMinutes(r.nextInt(50)));
                tous.add(mo);
                ord.ajouterMatch(mo);
            }
            // Même paire rejouée dans deux tableaux (poule puis tableau final).
            MatchOrdonnancable revanche = new MatchOrdonnancable(new Match(js.get(0), js.get(1)), "T9", null, PhaseMatch.POULE);
            ord.ajouterMatch(new MatchOrdonnancable(new Match(js.get(1), js.get(0)), "T8", null, PhaseMatch.POULE));
            ord.ajouterMatch(revanche);
            ord.affecter(tous.get(0), 1, T0.minusMinutes(30));
            ord.affecter(tous.get(1), 2, T0.minusMinutes(5));
            LocalDateTime maintenant = T0.plusMinutes(3);
            List<PropositionLancement> props = ord.proposerLancements(maintenant);
            assertFalse(props.isEmpty());
            for (PropositionLancement p : props) {
                assertEquals(ord.scorePriorite(p.getMatch(), maintenant), p.getScore(), 1e-9, s.name());
                assertEquals(ord.expliquer(p.getMatch(), maintenant), p.getExplication(), s.name());
            }
        }
    }
}
