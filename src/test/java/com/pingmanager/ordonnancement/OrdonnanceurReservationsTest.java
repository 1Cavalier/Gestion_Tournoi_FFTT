package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Réservations de tables (B024) : table ou salle réservée à un tableau
 * (contrainte dure), préférence d'un tableau pour une salle (souple).
 * Clauses d'organisateur : le juge-arbitre programme « en fonction des tables
 * disponibles » (3.3.1.2.2).
 */
class OrdonnanceurReservationsTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    private static MatchOrdonnancable mo(String tableau, String a, String b) {
        return new MatchOrdonnancable(new Match(j(a), j(b)), tableau, tableau, PhaseMatch.POULE);
    }

    private static Map<String, Integer> tablesParTableau(List<PropositionLancement> props) {
        return props.stream().collect(Collectors.toMap(
                p -> p.getMatch().getMatch().getJoueurA().getNumeroLicence(), PropositionLancement::getNumeroTable));
    }

    @Test
    void sans_reservation_les_propositions_restent_dans_l_ordre_des_tables() {
        Ordonnanceur ord = new Ordonnanceur(3);
        ord.ajouterMatch(mo("A", "a1", "a2"));
        ord.ajouterMatch(mo("B", "b1", "b2"));
        ord.ajouterMatch(mo("C", "c1", "c2"));
        assertTrue(ord.getReservations().estVide());
        List<Integer> tables = ord.proposerLancements(t0).stream().map(PropositionLancement::getNumeroTable).toList();
        assertEquals(List.of(1, 2, 3), tables);
    }

    @Test
    void une_table_reservee_ne_recoit_que_son_tableau_meme_s_il_reste_des_matchs_prets() {
        Ordonnanceur ord = new Ordonnanceur(2);
        ord.setReservations(ReservationsTables.builder().restreindre(1, Set.of("Dames")).construire());
        ord.ajouterMatch(mo("Open", "o1", "o2"));
        ord.ajouterMatch(mo("Open", "o3", "o4"));

        List<PropositionLancement> props = ord.proposerLancements(t0);

        assertEquals(1, props.size(), "la table 1 reste libre pour le tableau Dames");
        assertEquals(2, props.get(0).getNumeroTable());
    }

    @Test
    void un_tableau_qui_a_ses_tables_commence_par_elles() {
        Ordonnanceur ord = new Ordonnanceur(4);
        ord.setReservations(ReservationsTables.builder().restreindre(4, Set.of("Dames")).construire());
        ord.ajouterMatch(mo("Dames", "d1", "d2"));
        ord.ajouterMatch(mo("Dames", "d3", "d4"));
        ord.ajouterMatch(mo("Open", "o1", "o2"));

        Map<String, Integer> tables = tablesParTableau(ord.proposerLancements(t0));

        assertEquals(4, tables.get("d1"), "la table réservée d'abord");
        assertEquals(1, tables.get("d3"), "puis une table ordinaire : la réservation ne bloque pas le tableau");
        assertEquals(2, tables.get("o1"));
    }

    @Test
    void la_preference_envoie_le_tableau_dans_sa_salle_quand_une_table_y_est_libre() {
        // Salle 2 = tables 3 et 4 ; le tableau « 1500 » y joue de préférence.
        Ordonnanceur ord = new Ordonnanceur(4);
        ord.setReservations(ReservationsTables.builder().preferer("1500", 3).preferer("1500", 4).construire());
        ord.ajouterMatch(mo("1500", "x1", "x2"));
        ord.ajouterMatch(mo("Open", "o1", "o2"));
        ord.ajouterMatch(mo("1500", "x3", "x4"));
        ord.ajouterMatch(mo("1500", "x5", "x6"));

        Map<String, Integer> tables = tablesParTableau(ord.proposerLancements(t0));

        assertEquals(3, tables.get("x1"));
        assertEquals(1, tables.get("o1"), "les autres tableaux ne sont pas gênés");
        assertEquals(4, tables.get("x3"));
        assertEquals(2, tables.get("x5"), "salle préférée pleine : le tableau joue ailleurs");
    }

    @Test
    void l_affectation_manuelle_sur_une_table_reservee_a_un_autre_tableau_est_refusee_avec_le_tableau() {
        Ordonnanceur ord = new Ordonnanceur(3);
        ord.setReservations(ReservationsTables.builder().restreindre(2, Set.of("Dames", "Benjamins")).construire());
        MatchOrdonnancable open = mo("Open", "o1", "o2");
        MatchOrdonnancable dames = mo("Dames", "d1", "d2");
        ord.ajouterMatch(open);
        ord.ajouterMatch(dames);

        ResultatAffectationTable refus = ord.affecter(open, 2, t0);

        assertEquals(ResultatAffectationTable.Motif.TABLE_RESERVEE, refus.getMotif());
        assertFalse(refus.estReussi());
        assertEquals(Set.of("Benjamins", "Dames"), refus.getTableauxAdmis());
        assertEquals(List.of("Benjamins", "Dames"), List.copyOf(refus.getTableauxAdmis()), "ordre alphabétique");
        assertNull(open.getMatch().getNumeroTable(), "rien n'est modifié en cas de refus");
        assertTrue(ord.affecter(dames, 2, t0).estReussi());
        assertTrue(ord.affecter(open, 3, t0).estReussi());
    }

    @Test
    void une_table_occupee_et_reservee_annonce_d_abord_qu_elle_est_occupee() {
        Ordonnanceur ord = new Ordonnanceur(2);
        ord.setReservations(ReservationsTables.builder().restreindre(1, Set.of("Dames")).construire());
        MatchOrdonnancable dames = mo("Dames", "d1", "d2");
        MatchOrdonnancable open = mo("Open", "o1", "o2");
        ord.ajouterMatch(dames);
        ord.ajouterMatch(open);
        assertTrue(ord.affecter(dames, 1, t0).estReussi());
        assertEquals(ResultatAffectationTable.Motif.TABLE_OCCUPEE, ord.verifierAffectation(open, 1).getMotif());
    }

    @Test
    void restreindre_sans_tableau_est_refuse() {
        assertThrows(IllegalArgumentException.class,
                () -> ReservationsTables.builder().restreindre(1, Set.of()));
    }

    @Test
    void reservations_lues_table_par_table() {
        ReservationsTables r = ReservationsTables.builder()
                .restreindre(1, Set.of("Dames")).preferer("Open", 2).construire();
        assertTrue(r.admet(1, "Dames"));
        assertFalse(r.admet(1, "Open"));
        assertTrue(r.admet(2, "Open"));
        assertTrue(r.estRestreinte(1));
        assertFalse(r.estRestreinte(2));
        assertTrue(r.prefere("Dames", 1), "un tableau préfère ses tables réservées");
        assertTrue(r.prefere("Open", 2));
        assertEquals(Set.of(), r.tableauxAdmis(2));
        assertFalse(r.estVide());
    }
}
