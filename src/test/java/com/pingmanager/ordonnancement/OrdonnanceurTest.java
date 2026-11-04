package com.pingmanager.ordonnancement;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdonnanceurTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 9, 0);

    private Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    private MatchOrdonnancable mo(Match m, String tab, PhaseMatch ph) {
        return new MatchOrdonnancable(m, tab, tab, ph);
    }

    @Test
    void un_joueur_nest_jamais_propose_sur_deux_tables_a_la_fois() {
        Ordonnanceur ord = new Ordonnanceur(4);
        Joueur x = j("X");
        Match m1 = new Match(x, j("Y"));
        Match m2 = new Match(x, j("Z"));
        Match m3 = new Match(j("W"), j("V"));
        ord.ajouterMatch(mo(m1, "T1", PhaseMatch.POULE));
        ord.ajouterMatch(mo(m2, "T2", PhaseMatch.POULE));
        ord.ajouterMatch(mo(m3, "T1", PhaseMatch.POULE));

        List<PropositionLancement> props = ord.proposerLancements(t0);
        long avecX = props.stream().filter(p -> {
            Match m = p.getMatch().getMatch();
            return m.getJoueurA().getNumeroLicence().equals("X")
                    || m.getJoueurB().getNumeroLicence().equals("X");
        }).count();
        assertTrue(avecX <= 1, "X ne doit pas être proposé sur deux tables");
    }

    @Test
    void lancer_occupe_la_table_et_le_joueur() {
        Ordonnanceur ord = new Ordonnanceur(4);
        Match m = new Match(j("A"), j("B"));
        ord.ajouterMatch(mo(m, "T1", PhaseMatch.POULE));

        List<PropositionLancement> props = ord.proposerLancements(t0);
        assertEquals(1, props.size());
        ord.lancer(props.get(0), t0);

        assertEquals(3, ord.tablesLibres().size());
        assertEquals(2, ord.joueursOccupes().size());
        assertEquals(StatutMatch.EN_COURS, m.getStatut());
        assertNotNull(m.getInstantLancement());
    }

    @Test
    void terminer_libere_la_table_et_horodate_la_fin() {
        Ordonnanceur ord = new Ordonnanceur(2);
        Match m = new Match(j("A"), j("B"));
        MatchOrdonnancable moA = mo(m, "T1", PhaseMatch.POULE);
        ord.ajouterMatch(moA);
        ord.lancer(ord.proposerLancements(t0).get(0), t0);
        assertEquals(1, ord.tablesLibres().size());

        ord.terminer(moA, t0.plusMinutes(20));
        assertEquals(2, ord.tablesLibres().size());
        assertNotNull(m.getInstantFin());
    }

    @Test
    void les_poules_passent_avant_le_tableau() {
        Ordonnanceur ord = new Ordonnanceur(1); // 1 table force le choix
        Match poule = new Match(j("A"), j("B"));
        Match tabl = new Match(j("C"), j("D"));
        ord.ajouterMatch(mo(poule, "T1", PhaseMatch.POULE));
        ord.ajouterMatch(mo(tabl, "T1", PhaseMatch.PREMIER_TOUR_TABLEAU));

        List<PropositionLancement> props = ord.proposerLancements(t0);
        assertEquals(1, props.size());
        assertEquals(PhaseMatch.POULE, props.get(0).getMatch().getPhase());
    }

    @Test
    void un_match_qui_attend_depuis_longtemps_est_prioritaire() {
        Ordonnanceur ord = new Ordonnanceur(1);
        Match recent = new Match(j("R1"), j("R2"));
        Match vieux = new Match(j("V1"), j("V2"));
        MatchOrdonnancable moRecent = mo(recent, "T2", PhaseMatch.PREMIER_TOUR_TABLEAU);
        MatchOrdonnancable moVieux = mo(vieux, "T2", PhaseMatch.PREMIER_TOUR_TABLEAU);
        ord.ajouterMatch(moRecent);
        ord.ajouterMatch(moVieux);

        ord.rafraichirDisponibilites(t0);
        moVieux.setPretDepuis(t0.minusMinutes(40));
        moRecent.setPretDepuis(t0.minusMinutes(1));

        List<PropositionLancement> props = ord.proposerLancements(t0);
        assertEquals("V1", props.get(0).getMatch().getMatch().getJoueurA().getNumeroLicence());
    }

    @Test
    void un_joueur_engage_sur_deux_tableaux_est_prioritaire_pour_debloquer() {
        Ordonnanceur ord = new Ordonnanceur(1);
        Joueur bloc = j("BLOC");
        Match mb1 = new Match(bloc, j("Q1"));
        Match mb2 = new Match(bloc, j("Q2")); // engagement ailleurs
        Match indep = new Match(j("I1"), j("I2"));
        ord.ajouterMatch(mo(mb1, "TA", PhaseMatch.PREMIER_TOUR_TABLEAU));
        ord.ajouterMatch(mo(mb2, "TB", PhaseMatch.PREMIER_TOUR_TABLEAU));
        ord.ajouterMatch(mo(indep, "TA", PhaseMatch.PREMIER_TOUR_TABLEAU));

        List<PropositionLancement> props = ord.proposerLancements(t0);
        Match tete = props.get(0).getMatch().getMatch();
        boolean blocEnTete = tete.getJoueurA().getNumeroLicence().equals("BLOC")
                || tete.getJoueurB().getNumeroLicence().equals("BLOC");
        assertTrue(blocEnTete, "le match du joueur engagé ailleurs doit être prioritaire");
    }

    // --- Validation d'inscription (règle configurable) ---

    private Tableau tableau(String nom, LocalDateTime debut) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().build());
        t.setHoraires(new HorairesTableau(debut.minusMinutes(45), debut.minusMinutes(15), debut));
        return t;
    }

    @Test
    void regle_desactivee_autorise_tout() {
        Joueur x = j("X");
        Tableau t9 = tableau("9h", t0);
        Tableau t11 = tableau("11h", t0.plusHours(2));
        ValidationInscription v = ValidationInscription.desactivee();
        assertTrue(v.verifier(x, t11, List.of(t9)).estAutorise());
    }

    @Test
    void interdit_deux_tableaux_consecutifs() {
        Joueur x = j("X");
        Tableau t9 = tableau("9h", t0);
        Tableau t11 = tableau("11h", t0.plusHours(2));
        Tableau t14 = tableau("14h", t0.plusHours(5));
        ValidationInscription v = new ValidationInscription(true, null);
        v.definirTousLesTableaux(List.of(t9, t11, t14));

        var refus = v.verifier(x, t11, List.of(t9));
        assertFalse(refus.estAutorise());
        assertEquals(t9, refus.getTableauEnConflit());

        // t9 et t14 ne sont pas consécutifs (t11 est entre les deux).
        assertTrue(v.verifier(x, t14, List.of(t9)).estAutorise());
    }

    @Test
    void seuil_de_proximite_configurable() {
        Joueur x = j("X");
        Tableau t9 = tableau("9h", t0);
        Tableau t11 = tableau("11h", t0.plusHours(2));
        Tableau t14 = tableau("14h", t0.plusHours(5));
        ValidationInscription v = new ValidationInscription(true, Duration.ofHours(3));

        assertFalse(v.verifier(x, t11, List.of(t9)).estAutorise(), "écart 2h < 3h : refusé");
        assertTrue(v.verifier(x, t14, List.of(t9)).estAutorise(), "écart 5h > 3h : autorisé");
    }
}
