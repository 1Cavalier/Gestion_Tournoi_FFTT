package com.pingmanager.ordonnancement;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pilote du renfort (B054) : l'application propose d'ouvrir des tables
 * d'échauffement au-delà du seuil de retard, de les rendre une fois le retard
 * résorbé ; le juge-arbitre accepte, refuse ou force (3.3.1.2.2).
 */
class PiloteRenfortTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 14, 15, 0);
    private static final LocalDateTime FIN_VISEE = LocalDateTime.of(2026, 11, 14, 20, 0);

    /** Charge telle que la fin projetée sur 10 tables tombe {@code retard} minutes après 20 h. */
    private static EvaluationRetard retard(long retard) {
        double charge = (300 + retard) * 10.0;
        return new EvaluationRetard(MAINTENANT, 0, charge, FIN_VISEE);
    }

    @Test
    void reglage_par_defaut_seuils_quinze_et_zero() {
        ReglageRenfort r = ReglageRenfort.tables(4);
        assertEquals(4, r.getTablesMax());
        assertEquals(15, r.getSeuilDeploiementMinutes());
        assertEquals(0, r.getSeuilRetraitMinutes());
        assertFalse(ReglageRenfort.AUCUN.estActif());
        assertThrows(IllegalArgumentException.class,
                () -> ReglageRenfort.builder().seuilDeploiementMinutes(5).seuilRetraitMinutes(10).build());
        assertThrows(IllegalArgumentException.class, () -> ReglageRenfort.tables(-1));
    }

    @Test
    void evaluation_retard_de_fin_et_de_debut() {
        assertEquals(30, retard(30).retardPrevuMinutes(10));
        assertEquals(-60, retard(-60).retardPrevuMinutes(10));
        EvaluationRetard debut = new EvaluationRetard(MAINTENANT, 40, 0, FIN_VISEE);
        assertEquals(40, debut.retardPrevuMinutes(10), "un tableau qui attend de commencer compte");
        EvaluationRetard sansFin = new EvaluationRetard(MAINTENANT, 0, 1000, null);
        assertEquals(0, sansFin.retardPrevuMinutes(10));
    }

    @Test
    void pas_de_proposition_sous_le_seuil() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(4), 10);
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(15)).action(), "15 min : pas au-delà");
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(0)).action());
    }

    @Test
    void deploiement_au_dela_du_seuil_juste_le_nombre_necessaire() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(4), 10);
        // 30 min de retard : 3 300 min de table à finir en 300 min = 11 tables, soit 1 de renfort.
        PropositionRenfort prop = p.proposer(retard(30));
        assertEquals(PropositionRenfort.Action.DEPLOYER, prop.action());
        assertEquals(1, prop.tablesCible());
        assertEquals(30, prop.retardPrevu());
        p.accepter(prop);
        assertEquals(1, p.getTablesEnService());
    }

    @Test
    void deploiement_jamais_au_dela_du_maximum() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(2), 10);
        PropositionRenfort prop = p.proposer(retard(600));
        assertEquals(2, prop.tablesCible());
        p.accepter(prop);
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(900)).action(),
                "déjà au maximum : rien de plus à proposer");
        assertEquals(2, p.getTablesEnService());
    }

    @Test
    void tableau_en_retard_de_debut_ouvre_tout_le_renfort() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(3), 10);
        PropositionRenfort prop = p.proposer(new EvaluationRetard(MAINTENANT, 25, 0, FIN_VISEE));
        assertEquals(PropositionRenfort.Action.DEPLOYER, prop.action());
        assertEquals(3, prop.tablesCible());
    }

    @Test
    void retrait_quand_le_retard_est_resorbe_pas_avant() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(4), 10);
        p.accepter(p.proposer(retard(600)));
        assertEquals(4, p.getTablesEnService());
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(10)).action(),
                "entre les deux seuils : on garde le renfort");
        PropositionRenfort retrait = p.proposer(retard(0));
        assertEquals(PropositionRenfort.Action.RETIRER, retrait.action());
        assertEquals(0, retrait.tablesCible());
        p.accepter(retrait);
        assertEquals(0, p.getTablesEnService());
    }

    @Test
    void refus_du_juge_arbitre_tant_que_la_situation_ne_change_pas() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(4), 10);
        PropositionRenfort prop = p.proposer(retard(30));
        p.refuser(prop);
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(40)).action());
        assertEquals(0, p.getTablesEnService());
        // Le retard se résorbe puis revient : nouvelle proposition.
        p.proposer(retard(0));
        assertEquals(PropositionRenfort.Action.DEPLOYER, p.proposer(retard(30)).action());
    }

    @Test
    void forcage_du_juge_arbitre_suspend_les_propositions() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.tables(4), 10);
        p.forcer(3);
        assertTrue(p.estForce());
        assertEquals(3, p.getTablesEnService());
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(-100)).action(),
                "forcé : pas de retrait proposé");
        assertThrows(IllegalArgumentException.class, () -> p.forcer(5));
        assertThrows(IllegalArgumentException.class, () -> p.forcer(-1));
        p.reprendreLesPropositions();
        assertEquals(PropositionRenfort.Action.RETIRER, p.proposer(retard(-100)).action());
    }

    @Test
    void sans_renfort_jamais_de_proposition() {
        PiloteRenfort p = new PiloteRenfort(ReglageRenfort.AUCUN, 10);
        assertEquals(PropositionRenfort.Action.AUCUNE, p.proposer(retard(600)).action());
    }
}
