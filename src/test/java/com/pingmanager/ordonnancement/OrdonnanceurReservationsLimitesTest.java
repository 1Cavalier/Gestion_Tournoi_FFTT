package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Limites des réservations de tables (B024) : tout réservé, tableau inexistant, jamais de violation. */
class OrdonnanceurReservationsLimitesTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 11, 14, 10, 0);

    private static Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    private static MatchOrdonnancable mo(String tableau, String a, String b) {
        return new MatchOrdonnancable(new Match(j(a), j(b)), tableau, tableau, PhaseMatch.POULE);
    }

    @Test
    void toutes_les_tables_reservees_a_un_autre_tableau_aucune_proposition() {
        Ordonnanceur ord = new Ordonnanceur(2);
        ord.setReservations(ReservationsTables.builder()
                .restreindre(1, Set.of("Dames")).restreindre(2, Set.of("Dames")).construire());
        ord.ajouterMatch(mo("Open", "o1", "o2"));
        assertTrue(ord.proposerLancements(t0).isEmpty());
    }

    @Test
    void reservation_vers_un_tableau_inexistant_ou_une_table_hors_plage_ne_plante_pas() {
        Ordonnanceur ord = new Ordonnanceur(2);
        ord.setReservations(ReservationsTables.builder()
                .restreindre(1, Set.of("Fantome")).restreindre(99, Set.of("Open")).construire());
        ord.ajouterMatch(mo("Open", "o1", "o2"));
        ord.ajouterMatch(mo("Open", "o3", "o4"));
        List<PropositionLancement> props = ord.proposerLancements(t0);
        assertEquals(1, props.size());
        assertEquals(2, props.get(0).getNumeroTable());
    }

    @Test
    void jamais_de_match_sur_une_table_interdite_ni_deux_matchs_sur_la_meme_table() {
        Ordonnanceur ord = new Ordonnanceur(6);
        ord.setReservations(ReservationsTables.builder()
                .restreindre(1, Set.of("A")).restreindre(2, Set.of("B"))
                .restreindre(3, Set.of("A", "B")).preferer("C", 5).construire());
        String[] tableaux = {"A", "B", "C", "A", "B", "C", "A", "B"};
        for (int i = 0; i < tableaux.length; i++) {
            ord.ajouterMatch(mo(tableaux[i], "p" + i, "q" + i));
        }
        List<PropositionLancement> props = ord.proposerLancements(t0);
        Set<Integer> vues = new HashSet<>();
        for (PropositionLancement p : props) {
            assertTrue(vues.add(p.getNumeroTable()), "table utilisée deux fois");
            assertTrue(ord.getReservations().admet(p.getNumeroTable(), p.getMatch().getNomTableau()),
                    "table " + p.getNumeroTable() + " interdite à " + p.getMatch().getNomTableau());
        }
        assertFalse(props.isEmpty());
    }

    @Test
    void reservations_aucune_est_vide_et_admet_tout() {
        assertTrue(ReservationsTables.AUCUNE.estVide());
        assertTrue(ReservationsTables.AUCUNE.admet(7, "X"));
        assertFalse(ReservationsTables.AUCUNE.estRestreinte(7));
        assertTrue(ReservationsTables.AUCUNE.tableauxAdmis(7).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> ReservationsTables.builder().restreindre(1, Set.of()));
        assertThrows(NullPointerException.class, () -> new Ordonnanceur(1).setReservations(null));
    }

    @Test
    void zero_table_ou_aucun_match_donne_aucune_proposition_meme_avec_reservations() {
        Ordonnanceur ord = new Ordonnanceur(1);
        ord.setReservations(ReservationsTables.builder().restreindre(1, Set.of("A")).construire());
        assertTrue(ord.proposerLancements(t0).isEmpty());
    }
}
