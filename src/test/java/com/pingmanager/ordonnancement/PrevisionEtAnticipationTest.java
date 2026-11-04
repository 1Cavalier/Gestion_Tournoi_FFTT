package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class PrevisionEtAnticipationTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 9, 0);

    private Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    private MatchOrdonnancable mo(Match m, String tab, PhaseMatch ph) {
        return new MatchOrdonnancable(m, tab, tab, ph);
    }

    // --- EstimateurDuree ---

    @Test
    void utilise_la_valeur_de_depart_tant_quil_manque_dobservations() {
        CategorieDuree poule = new CategorieDuree(PhaseMatch.POULE, 0);
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(15), 0.3, 3);
        assertEquals(15, est.estimer(poule).toMinutes());

        est.definirValeurDepart(poule, Duration.ofMinutes(12));
        assertEquals(12, est.estimer(poule).toMinutes());

        est.observer(poule, Duration.ofMinutes(20));
        est.observer(poule, Duration.ofMinutes(20));
        assertFalse(est.estAppris(poule), "2 observations < minimum 3");
        assertEquals(12, est.estimer(poule).toMinutes());
    }

    @Test
    void bascule_sur_lapprentissage_apres_assez_dobservations() {
        CategorieDuree poule = new CategorieDuree(PhaseMatch.POULE, 0);
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(12), 0.3, 3);
        est.observer(poule, Duration.ofMinutes(20));
        est.observer(poule, Duration.ofMinutes(20));
        est.observer(poule, Duration.ofMinutes(20));
        assertTrue(est.estAppris(poule));
        long e = est.estimer(poule).toMinutes();
        assertTrue(e >= 17 && e <= 20, "estimation proche de 20 min");
    }

    @Test
    void une_valeur_aberrante_ne_bouleverse_pas_lestimation() {
        CategorieDuree poule = new CategorieDuree(PhaseMatch.POULE, 0);
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(12), 0.3, 3);
        est.observer(poule, Duration.ofMinutes(20));
        est.observer(poule, Duration.ofMinutes(20));
        est.observer(poule, Duration.ofMinutes(20));
        est.observer(poule, Duration.ofMinutes(90)); // litige
        assertTrue(est.estimer(poule).toMinutes() < 45, "une observation extrême reste amortie");
    }

    @Test
    void les_categories_sont_independantes() {
        CategorieDuree poule = new CategorieDuree(PhaseMatch.POULE, 0);
        CategorieDuree tableauFort = new CategorieDuree(PhaseMatch.TOUR_SUIVANT_TABLEAU, 3);
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(15));
        est.definirValeurDepart(tableauFort, Duration.ofMinutes(25));
        assertEquals(25, est.estimer(tableauFort).toMinutes());
        assertEquals(15, est.estimer(poule).toMinutes());
    }

    @Test
    void la_cle_regroupe_les_niveaux_par_paliers() {
        CategorieDuree c1 = CategorieDuree.depuisNiveau(PhaseMatch.POULE, 1350, 200);
        CategorieDuree c2 = CategorieDuree.depuisNiveau(PhaseMatch.POULE, 1390, 200);
        CategorieDuree c3 = CategorieDuree.depuisNiveau(PhaseMatch.POULE, 1550, 200);
        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
    }

    // --- PrevisionSalle ---

    private PrevisionSalle previsionSimple(Ordonnanceur ord) {
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(20));
        Function<MatchOrdonnancable, CategorieDuree> catDe = x -> new CategorieDuree(x.getPhase(), 0);
        return new PrevisionSalle(ord, est, catDe);
    }

    @Test
    void previsions_fin_liberation_et_disponibilite() {
        Ordonnanceur ord = new Ordonnanceur(2);
        Joueur x = j("X"), y = j("Y");
        MatchOrdonnancable m = mo(new Match(x, y), "T1", PhaseMatch.POULE);
        ord.ajouterMatch(m);
        ord.lancer(ord.proposerLancements(t0).get(0), t0);

        PrevisionSalle prev = previsionSimple(ord);
        assertEquals(t0.plusMinutes(20), prev.finEstimee(m));

        Map<Integer, LocalDateTime> lib = prev.liberationDesTables();
        assertEquals(t0.plusMinutes(20), lib.get(1));
        assertFalse(lib.containsKey(2));

        assertEquals(t0.plusMinutes(20), prev.disponibiliteJoueur(x));
        assertNull(prev.disponibiliteJoueur(j("Z")));

        assertEquals(8, prev.tempsRestant(m, t0.plusMinutes(12)).toMinutes());
    }

    // --- Anticipateur ---

    @Test
    void anticipe_le_match_dun_joueur_qui_finit_bientot() {
        Ordonnanceur ord = new Ordonnanceur(3);
        Joueur x = j("X"), y = j("Y"), w = j("W");
        MatchOrdonnancable enCours = mo(new Match(x, y), "T1", PhaseMatch.POULE);
        ord.ajouterMatch(enCours);
        ord.lancer(ord.proposerLancements(t0).get(0), t0);

        PrevisionSalle prev = previsionSimple(ord);
        // Match bloqué par X, et W est engagé ailleurs (déblocage).
        MatchOrdonnancable futur = mo(new Match(x, w), "TableauA", PhaseMatch.PREMIER_TOUR_TABLEAU);
        ord.ajouterMatch(futur);
        ord.ajouterMatch(mo(new Match(w, j("V")), "TableauB", PhaseMatch.PREMIER_TOUR_TABLEAU));

        Anticipateur ant = new Anticipateur(ord, prev);
        // À 9h15, X finit à 9h20 : dans l'horizon de 10 min.
        List<SuggestionAnticipation> sugg = ant.suggererPreparation(t0.plusMinutes(15), Duration.ofMinutes(10));
        boolean trouve = sugg.stream().anyMatch(s -> {
            Match m = s.getMatch().getMatch();
            Set<String> lics = Set.of(m.getJoueurA().getNumeroLicence(), m.getJoueurB().getNumeroLicence());
            return lics.contains("X") && lics.contains("W");
        });
        assertTrue(trouve, "le match bloqué par X (qui finit bientôt) doit être suggéré");
        assertEquals(t0.plusMinutes(20), sugg.get(0).getJouableVers());
    }

    @Test
    void nanticipe_pas_hors_de_lhorizon() {
        Ordonnanceur ord = new Ordonnanceur(3);
        Joueur x = j("X"), y = j("Y"), w = j("W");
        MatchOrdonnancable enCours = mo(new Match(x, y), "T1", PhaseMatch.POULE);
        ord.ajouterMatch(enCours);
        ord.lancer(ord.proposerLancements(t0).get(0), t0);

        PrevisionSalle prev = previsionSimple(ord);
        ord.ajouterMatch(mo(new Match(x, w), "TableauA", PhaseMatch.PREMIER_TOUR_TABLEAU));

        Anticipateur ant = new Anticipateur(ord, prev);
        // À 9h00, X finit à 9h20 : hors d'un horizon de 5 min.
        List<SuggestionAnticipation> sugg = ant.suggererPreparation(t0, Duration.ofMinutes(5));
        boolean trouve = sugg.stream().anyMatch(s -> {
            Match m = s.getMatch().getMatch();
            Set<String> lics = Set.of(m.getJoueurA().getNumeroLicence(), m.getJoueurB().getNumeroLicence());
            return lics.contains("X") && lics.contains("W");
        });
        assertFalse(trouve, "hors horizon : pas de suggestion");
    }
}
