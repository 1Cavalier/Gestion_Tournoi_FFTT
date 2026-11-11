package com.pingmanager.ordonnancement;

import com.pingmanager.core.GenerateurTableauElimination;
import com.pingmanager.core.ProgressionTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.TableauElimination;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Stratégie « petit à petit » : un tour ne démarre qu'une fois le précédent fini. */
class TourParTourTest {

    private static final LocalDateTime T = LocalDateTime.of(2027, 4, 10, 10, 0);

    /**
     * 6 joueurs, cadre 8 : seeds 1 et 2 exempts, 1er tour 4-5 et 3-6. On finit
     * 4-5 : la demi « 1 contre vainqueur de 4-5 » devient jouable alors que 3-6
     * n'est pas fini.
     */
    private Ordonnanceur salle(StrategieOrdonnancement s, List<MatchOrdonnancable> mos) {
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            js.add(new Joueur("J" + i, "N" + i, "P", Sexe.HOMME, "C" + i, 2000 - i * 100));
        }
        TableauElimination t = new GenerateurTableauElimination().genererTableau(js);
        Ordonnanceur ord = new Ordonnanceur(10, PoidsOrdonnancement.pour(s));
        for (int k = 0; k < t.getTours().size(); k++) {
            for (Match m : t.getTours().get(k).getMatchs()) {
                MatchOrdonnancable mo = new MatchOrdonnancable(m, "T", "T",
                        k == 0 ? PhaseMatch.PREMIER_TOUR_TABLEAU : PhaseMatch.TOUR_SUIVANT_TABLEAU);
                mos.add(mo);
                ord.ajouterMatch(mo);
            }
        }
        Match premier = t.getTours().get(0).getMatchs().stream()
                .filter(m -> m.getJoueurA().getNumeroLicence().equals("J4")
                        || m.getJoueurB().getNumeroLicence().equals("J4"))
                .findFirst().orElseThrow();
        premier.definirScore(List.of(new Manche(11, 3), new Manche(11, 3), new Manche(11, 3)));
        new ProgressionTableau().propager(t);
        return ord;
    }

    private static long demisProposees(Ordonnanceur ord) {
        return ord.proposerLancements(T).stream()
                .filter(p -> p.getMatch().getPhase() == PhaseMatch.TOUR_SUIVANT_TABLEAU).count();
    }

    @Test
    void petit_a_petit_retient_la_demi_tant_que_le_premier_tour_n_est_pas_fini() {
        List<MatchOrdonnancable> mos = new ArrayList<>();
        Ordonnanceur ord = salle(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, mos);
        assertTrue(mos.stream().anyMatch(mo -> mo.getPhase() == PhaseMatch.TOUR_SUIVANT_TABLEAU
                && mo.getMatch().getStatut() == StatutMatch.A_JOUER), "la demi a ses deux joueurs");
        assertEquals(0, demisProposees(ord), "demi retenue : le 1er tour n'est pas fini");
        assertEquals(1, ord.proposerLancements(T).size(), "seul le match 3-6 est proposé");
    }

    @Test
    void les_autres_strategies_lancent_la_demi_des_qu_elle_est_jouable() {
        for (StrategieOrdonnancement s : List.of(StrategieOrdonnancement.MINIMISER_ATTENTE,
                StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES)) {
            Ordonnanceur ord = salle(s, new ArrayList<>());
            assertEquals(1, demisProposees(ord), s.name());
        }
    }
}
