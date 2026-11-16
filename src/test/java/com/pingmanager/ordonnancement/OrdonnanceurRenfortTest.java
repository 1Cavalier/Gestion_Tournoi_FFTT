package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tables de renfort (B054) : réserve distincte des tables de compétition,
 * numérotée après elles, jamais utilisée tant qu'elle n'est pas ouverte.
 * 3.3.1.2.2 : le juge-arbitre programme les parties selon les tables disponibles.
 */
class OrdonnanceurRenfortTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    private static MatchOrdonnancable mo(String a, String b) {
        return new MatchOrdonnancable(new Match(j(a), j(b)), "Open", "Open", PhaseMatch.POULE);
    }

    private static Ordonnanceur avecSixMatchs(int tables, int renfort) {
        Ordonnanceur ord = new Ordonnanceur(tables, renfort, new PoidsOrdonnancement());
        for (int i = 1; i <= 6; i++) {
            ord.ajouterMatch(mo("a" + i, "b" + i));
        }
        return ord;
    }

    private static List<Integer> tables(List<PropositionLancement> props) {
        return props.stream().map(PropositionLancement::getNumeroTable).toList();
    }

    @Test
    void reserve_non_utilisee_tant_qu_elle_n_est_pas_ouverte() {
        Ordonnanceur ord = avecSixMatchs(2, 3);
        assertEquals(0, ord.getTablesRenfortEnService());
        assertEquals(List.of(1, 2), tables(ord.proposerLancements(t0)));
    }

    @Test
    void tables_ouvertes_numerotees_apres_les_tables_de_competition() {
        Ordonnanceur ord = avecSixMatchs(2, 3);
        ord.setTablesRenfortEnService(2);
        assertEquals(List.of(1, 2, 3, 4), tables(ord.proposerLancements(t0)));
        assertTrue(ord.estTableDeRenfort(3));
        assertTrue(ord.estTableDeRenfort(5));
        assertFalse(ord.estTableDeRenfort(2));
        assertFalse(ord.estTableDeRenfort(6));
    }

    @Test
    void jamais_plus_de_tables_de_renfort_que_le_maximum() {
        Ordonnanceur ord = avecSixMatchs(2, 3);
        assertThrows(IllegalArgumentException.class, () -> ord.setTablesRenfortEnService(4));
        assertThrows(IllegalArgumentException.class, () -> ord.setTablesRenfortEnService(-1));
        ord.setTablesRenfortEnService(3);
        assertEquals(5, ord.proposerLancements(t0).size(), "2 tables + 3 de renfort, jamais 6");
    }

    @Test
    void les_tables_de_competition_sont_servies_d_abord() {
        Ordonnanceur ord = new Ordonnanceur(2, 2, new PoidsOrdonnancement());
        ord.ajouterMatch(mo("a1", "b1"));
        ord.setTablesRenfortEnService(2);
        assertEquals(List.of(1), tables(ord.proposerLancements(t0)));
    }

    @Test
    void table_rendue_garde_son_match_jusqu_a_la_fin_puis_ne_recoit_plus_rien() {
        Ordonnanceur ord = avecSixMatchs(1, 1);
        ord.setTablesRenfortEnService(1);
        List<PropositionLancement> props = ord.proposerLancements(t0);
        props.forEach(p -> ord.lancer(p, t0));
        assertEquals(1, ord.tablesRenfortOccupees());

        ord.setTablesRenfortEnService(0);
        MatchOrdonnancable surRenfort = ord.matchSurTable(2);
        assertNotNull(surRenfort, "le match en cours continue sur la table d'échauffement");

        ord.terminer(surRenfort, t0.plusMinutes(20));
        ord.terminer(ord.matchSurTable(1), t0.plusMinutes(20));
        assertEquals(List.of(1), tables(ord.proposerLancements(t0.plusMinutes(20))));
        assertEquals(0, ord.tablesRenfortOccupees());
    }

    @Test
    void choix_manuel_sur_une_table_de_renfort_fermee_refuse_avec_motif() {
        Ordonnanceur ord = avecSixMatchs(2, 2);
        MatchOrdonnancable m = ord.tousLesMatchs().get(0);
        assertEquals(ResultatAffectationTable.Motif.TABLE_RENFORT_HORS_SERVICE,
                ord.affecter(m, 3, t0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, ord.affecter(m, 5, t0).getMotif());

        ord.setTablesRenfortEnService(1);
        assertTrue(ord.affecter(m, 3, t0).estReussi());
        assertEquals(3, m.getMatch().getNumeroTable());
    }

    @Test
    void sans_renfort_comportement_inchange() {
        Ordonnanceur ord = new Ordonnanceur(2);
        assertEquals(0, ord.getTablesRenfortMax());
        assertThrows(IllegalArgumentException.class, () -> ord.setTablesRenfortEnService(1));
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES,
                ord.affecter(mo("x", "y"), 3, t0).getMotif());
    }
}
