package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Affectation manuelle d'un match à une table, avec motif de refus détaillé. */
class OrdonnanceurAffectationTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    private static MatchOrdonnancable mo(Match m) {
        return new MatchOrdonnancable(m, "T", "T", PhaseMatch.POULE);
    }

    @Test
    void affectation_manuelle_valide_lance_le_match_sur_la_table_choisie() {
        Ordonnanceur ord = new Ordonnanceur(4);
        Match m = new Match(j("A"), j("B"));
        MatchOrdonnancable x = mo(m);
        ord.ajouterMatch(x);

        ResultatAffectationTable r = ord.affecter(x, 3, t0);

        assertEquals(ResultatAffectationTable.Motif.AFFECTE, r.getMotif());
        assertTrue(r.estReussi());
        assertEquals(3, r.getNumeroTable());
        assertEquals(StatutMatch.EN_COURS, m.getStatut());
        assertEquals(Integer.valueOf(3), m.getNumeroTable());
        assertEquals(t0, m.getInstantLancement());
        assertSame(x, ord.matchSurTable(3));
        assertEquals(Integer.valueOf(3), ord.tableDuJoueur("A"));
        assertEquals(List.of(1, 2, 4), ord.tablesLibres());
    }

    @Test
    void un_joueur_deja_en_jeu_bloque_l_affectation_manuelle_avec_sa_table() {
        Ordonnanceur ord = new Ordonnanceur(4);
        Joueur x = j("X");
        Match m1 = new Match(x, j("Y"));
        Match m2 = new Match(j("Z"), x);
        MatchOrdonnancable a = mo(m1);
        MatchOrdonnancable b = mo(m2);
        ord.ajouterMatch(a);
        ord.ajouterMatch(b);
        assertTrue(ord.affecter(a, 2, t0).estReussi());

        ResultatAffectationTable r = ord.affecter(b, 4, t0);

        assertEquals(ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU, r.getMotif());
        assertEquals(x, r.getJoueurBloquant());
        assertEquals(Integer.valueOf(2), r.getTableDuJoueurBloquant());
        assertEquals(4, r.getNumeroTable());
        assertEquals(StatutMatch.A_JOUER, m2.getStatut(), "rien n'est modifié en cas de refus");
        assertNull(m2.getNumeroTable());
        assertNull(ord.matchSurTable(4));
    }

    @Test
    void table_occupee_ou_hors_limites_refusee() {
        Ordonnanceur ord = new Ordonnanceur(2);
        MatchOrdonnancable a = mo(new Match(j("A"), j("B")));
        MatchOrdonnancable c = mo(new Match(j("C"), j("D")));
        ord.ajouterMatch(a);
        ord.ajouterMatch(c);
        ord.affecter(a, 1, t0);

        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, ord.affecter(c, 1, t0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, ord.affecter(c, 0, t0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, ord.affecter(c, 3, t0).getMotif());
        assertEquals(StatutMatch.A_JOUER, c.getMatch().getStatut());
    }

    @Test
    void match_non_pret_refuse_avec_le_bon_motif() {
        Ordonnanceur ord = new Ordonnanceur(4);
        Match termine = new Match(j("A"), j("B"));
        termine.definirScore(List.of(new Manche(11, 3), new Manche(11, 3), new Manche(11, 3)));
        Match inconnu = new Match(j("C"), Joueur.aDeterminer());
        Match exempt = new Match(j("E"), null);
        Match enCours = new Match(j("F"), j("G"));
        Match horsSalle = new Match(j("H"), j("I"));
        MatchOrdonnancable moEnCours = mo(enCours);
        for (MatchOrdonnancable x : List.of(mo(termine), mo(inconnu), mo(exempt), moEnCours)) {
            ord.ajouterMatch(x);
        }
        ord.affecter(moEnCours, 1, t0);

        assertEquals(ResultatAffectationTable.Motif.MATCH_TERMINE, ord.affecter(mo(termine), 2, t0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.JOUEURS_NON_CONNUS, ord.affecter(mo(inconnu), 2, t0).getMotif());
        assertEquals(ResultatAffectationTable.Motif.MATCH_EXEMPT, ord.affecter(mo(exempt), 2, t0).getMotif());
        ResultatAffectationTable deja = ord.affecter(moEnCours, 2, t0);
        assertEquals(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS, deja.getMotif());
        assertEquals(Integer.valueOf(1), enCours.getNumeroTable(), "le match reste sur sa table");
        assertEquals(ResultatAffectationTable.Motif.MATCH_INCONNU, ord.affecter(mo(horsSalle), 2, t0).getMotif());
    }

    @Test
    void annuler_un_lancement_libere_la_table_et_remet_le_match_a_jouer() {
        Ordonnanceur ord = new Ordonnanceur(2);
        Match m = new Match(j("A"), j("B"));
        MatchOrdonnancable x = mo(m);
        ord.ajouterMatch(x);
        ord.affecter(x, 2, t0);

        ResultatAffectationTable r = ord.annulerLancement(x);

        assertEquals(ResultatAffectationTable.Motif.AFFECTATION_ANNULEE, r.getMotif());
        assertEquals(2, r.getNumeroTable());
        assertEquals(StatutMatch.A_JOUER, m.getStatut());
        assertNull(m.getNumeroTable());
        assertNull(m.getInstantLancement());
        assertEquals(List.of(1, 2), ord.tablesLibres());
        assertEquals(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS, ord.annulerLancement(x).getMotif());
    }
}
