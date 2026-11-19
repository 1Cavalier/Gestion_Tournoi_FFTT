package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ordonnanceur et prévisions de salle avec des poules comme unités de lancement :
 * une poule occupe une table et tous ses joueurs jusqu'à son dernier résultat ;
 * sa durée prévue est la somme des durées prévues de ses matchs.
 */
class OrdonnanceurPoulesTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Joueur j(String l) {
        return new Joueur(l, "N" + l, "P" + l, Sexe.HOMME, "C", 1000);
    }

    /** Poule de 3 dans l'ordre I.301 à deux qualifiés (1-3, 1-2, 2-3), déjà calculé par le cœur. */
    private static PouleOrdonnancable poule(String nom, String a, String b, String c) {
        Joueur ja = j(a);
        Joueur jb = j(b);
        Joueur jc = j(c);
        Poule p = new Poule(nom, List.of(ja, jb, jc));
        p.ajouterMatch(new Match(ja, jc));
        p.ajouterMatch(new Match(ja, jb));
        p.ajouterMatch(new Match(jb, jc));
        List<MatchOrdonnancable> mos = new ArrayList<>();
        p.getMatchs().forEach(m -> mos.add(new MatchOrdonnancable(m, "A", "A", PhaseMatch.POULE)));
        return new PouleOrdonnancable(p, "A", "A", mos);
    }

    @Test
    void une_proposition_par_poule_jamais_un_match_de_poule_seul() {
        Ordonnanceur o = new Ordonnanceur(4);
        PouleOrdonnancable p1 = poule("1", "a", "b", "c");
        PouleOrdonnancable p2 = poule("2", "d", "e", "f");
        o.ajouterPoule(p1);
        o.ajouterPoule(p2);

        List<PropositionLancement> props = o.proposerLancements(T0);

        assertEquals(2, props.size());
        assertTrue(props.stream().allMatch(PropositionLancement::estPoule));
        assertFalse(o.estJouableMaintenant(p1.getMatchs().get(0)));
        assertEquals(ResultatAffectationTable.Motif.LANCEMENT_PAR_POULE,
                o.verifierMatch(p1.getMatchs().get(0)).getMotif());
    }

    @Test
    void poule_lancee_occupe_sa_table_et_tous_ses_joueurs_jusqu_au_dernier_resultat() {
        Ordonnanceur o = new Ordonnanceur(2);
        PouleOrdonnancable p = poule("1", "a", "b", "c");
        o.ajouterPoule(p);
        assertTrue(o.affecterPoule(p, 2, T0).estReussi());

        assertEquals(List.of(1), o.tablesLibres());
        assertEquals(java.util.Set.of("a", "b", "c"), o.joueursOccupes());
        assertEquals(Integer.valueOf(2), o.tableDuJoueur("b"));
        assertSame(p.getMatchs().get(0), o.matchSurTable(2), "le match qui se joue : 1-3");

        for (MatchOrdonnancable mo : p.getMatchs()) {
            mo.getMatch().setStatut(StatutMatch.TERMINE);
            o.terminer(mo, T0.plusMinutes(10));
        }
        assertEquals(List.of(1, 2), o.tablesLibres());
        assertTrue(o.joueursOccupes().isEmpty());
        assertFalse(p.getPoule().estLanceeEnBloc());
    }

    @Test
    void meme_formule_de_priorite_pour_une_poule_et_un_match() {
        Ordonnanceur o = new Ordonnanceur(4);
        PouleOrdonnancable p = poule("1", "a", "b", "c");
        o.ajouterPoule(p);
        p.setPretDepuis(T0);
        MatchOrdonnancable seul = new MatchOrdonnancable(new Match(j("x"), j("y")), "B", "B", PhaseMatch.POULE);
        o.ajouterMatch(seul);
        seul.setPretDepuis(T0);
        // Même phase, même attente, aucun engagement ailleurs : même score.
        assertEquals(o.scorePriorite(seul, T0.plusMinutes(7)), o.scorePriorite(p, T0.plusMinutes(7)), 1e-9);
        assertTrue(o.expliquer(p, T0.plusMinutes(7)).contains("attend 7 min"));
    }

    @Test
    void prevision_fin_de_table_et_joueurs_libres_somme_des_matchs_de_la_poule() {
        Ordonnanceur o = new Ordonnanceur(2);
        PouleOrdonnancable p = poule("1", "a", "b", "c");
        o.ajouterPoule(p);
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(20));
        PrevisionSalle prev = new PrevisionSalle(o, est, mo -> new CategorieDuree(mo.getPhase(), 0));
        assertEquals(Duration.ofMinutes(60), prev.dureePrevue(p));

        o.affecterPoule(p, 1, T0);
        assertEquals(T0.plusMinutes(60), prev.liberationDesTables().get(1));
        assertEquals(T0.plusMinutes(60), prev.disponibiliteJoueur(p.joueurs().get(1)));
        assertEquals(T0.plusMinutes(40), prev.finEstimee(p.getMatchs().get(1)), "deuxième match, à la suite");

        // Premier résultat à 10 h 25 : le reste se compte depuis ce résultat.
        Match premier = p.getMatchs().get(0).getMatch();
        premier.setStatut(StatutMatch.TERMINE);
        premier.setInstantFin(T0.plusMinutes(25));
        assertEquals(T0.plusMinutes(65), prev.finEstimee(p));
    }
}
