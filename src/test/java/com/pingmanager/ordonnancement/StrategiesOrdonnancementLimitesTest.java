package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StrategieOrdonnancement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Non-régression de l'ordonnanceur indexé (B025) et cas limites. */
class StrategiesOrdonnancementLimitesTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2027, 4, 10, 9, 0);

    private static Joueur j(String l) {
        return new Joueur(l, "N" + l, "P" + l, Sexe.HOMME, "C", 1000);
    }

    @Test
    void sans_match_ni_table_libre_aucune_proposition() {
        Ordonnanceur ord = new Ordonnanceur(1);
        assertTrue(ord.proposerLancements(T0).isEmpty());
        ord.ajouterMatch(new MatchOrdonnancable(new Match(j("A"), j("B")), "T", null, PhaseMatch.POULE));
        ord.ajouterMatch(new MatchOrdonnancable(new Match(j("C"), j("D")), "T", null, PhaseMatch.POULE));
        var p = ord.proposerLancements(T0);
        assertEquals(1, p.size(), "une seule table : une seule proposition");
    }

    @Test
    void zero_table_refuse_par_l_ordonnanceur() {
        assertThrows(IllegalArgumentException.class, () -> new Ordonnanceur(0));
    }

    @Test
    void a_score_egal_l_ordre_d_ajout_est_conserve_et_aucun_joueur_n_est_propose_deux_fois() {
        List<MatchOrdonnancable> ajoutes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            // matchs disjoints, mêmes critères : scores égaux
            MatchOrdonnancable mo = new MatchOrdonnancable(new Match(j("A" + i), j("B" + i)), "T", null, PhaseMatch.POULE);
            ajoutes.add(mo);
        }
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            Ordonnanceur o = new Ordonnanceur(6, PoidsOrdonnancement.pour(s));
            ajoutes.forEach(o::ajouterMatch);
            var props = o.proposerLancements(T0);
            var vus = new java.util.HashSet<String>();
            for (PropositionLancement p : props) {
                assertTrue(vus.add(p.getMatch().getMatch().getJoueurA().getNumeroLicence()), s.name());
                assertTrue(vus.add(p.getMatch().getMatch().getJoueurB().getNumeroLicence()), s.name());
            }
            assertEquals(6, props.size(), s.name());
            for (int i = 0; i < 6; i++) {
                assertSame(ajoutes.get(i), props.get(i).getMatch(), "égalité : ordre d'ajout, " + s);
            }
        }
    }

    /** Les propositions d'une vague sont triées par score décroissant, pour toutes les stratégies. */
    @Test
    void propositions_triees_par_score_decroissant_et_tables_distinctes() {
        Random r = new Random(5);
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            Ordonnanceur ord = new Ordonnanceur(5, PoidsOrdonnancement.pour(s));
            for (int i = 0; i < 40; i++) {
                MatchOrdonnancable mo = new MatchOrdonnancable(new Match(j("X" + i), j("Y" + i)),
                        "T" + r.nextInt(3), null, PhaseMatch.values()[r.nextInt(3)]);
                mo.setPretDepuis(T0.minusMinutes(r.nextInt(60)));
                ord.ajouterMatch(mo);
            }
            var props = ord.proposerLancements(T0);
            assertEquals(5, props.size());
            for (int i = 1; i < props.size(); i++) {
                assertTrue(props.get(i - 1).getScore() >= props.get(i).getScore(), s.name());
            }
            assertEquals(5, props.stream().map(PropositionLancement::getNumeroTable).distinct().count());
        }
    }

    @Test
    void poids_par_defaut_et_strategie_par_defaut_donnent_les_memes_propositions() {
        Random r = new Random(11);
        List<MatchOrdonnancable> ms = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            MatchOrdonnancable mo = new MatchOrdonnancable(new Match(j("L" + r.nextInt(12)), j("M" + r.nextInt(12))),
                    "T" + r.nextInt(3), null, PhaseMatch.values()[r.nextInt(3)]);
            mo.setPretDepuis(T0.minusMinutes(r.nextInt(40)));
            ms.add(mo);
        }
        Ordonnanceur a = new Ordonnanceur(4);
        Ordonnanceur b = new Ordonnanceur(4, PoidsOrdonnancement.pour(StrategieOrdonnancement.PAR_DEFAUT));
        ms.forEach(a::ajouterMatch);
        ms.forEach(b::ajouterMatch);
        var pa = a.proposerLancements(T0);
        var pb = b.proposerLancements(T0);
        assertEquals(pa.size(), pb.size());
        for (int i = 0; i < pa.size(); i++) {
            assertSame(pa.get(i).getMatch(), pb.get(i).getMatch());
            assertEquals(pa.get(i).getScore(), pb.get(i).getScore(), 0.0);
            assertEquals(pa.get(i).getNumeroTable(), pb.get(i).getNumeroTable());
        }
    }
}
