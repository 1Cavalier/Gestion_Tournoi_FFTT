package com.pingmanager.ordonnancement;

import com.pingmanager.core.GenerateurTableauElimination;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.TableauElimination;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * « Petit à petit » garde les finales pour la fin de la journée ; « tableaux
 * commencés tôt » laisse chaque tableau jouer sa finale à son heure.
 */
class FinalesEnFinDeJourneeTest {

    private static final LocalDateTime T = LocalDateTime.of(2027, 4, 10, 15, 0);

    /** Tableau A : une finale prête (2 joueurs). Tableau B : un premier tour de 4 joueurs. */
    private Ordonnanceur salle(StrategieOrdonnancement s) {
        Ordonnanceur ord = new Ordonnanceur(10, PoidsOrdonnancement.pour(s));
        ajouter(ord, "A", tableau("A", 2));
        ajouter(ord, "B", tableau("B", 4));
        return ord;
    }

    private static TableauElimination tableau(String prefixe, int n) {
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            js.add(new Joueur(prefixe + i, "N" + i, "P", Sexe.HOMME, "C" + prefixe + i, 2000 - i * 50));
        }
        return new GenerateurTableauElimination().genererTableau(js);
    }

    private static void ajouter(Ordonnanceur ord, String nom, TableauElimination t) {
        for (int k = 0; k < t.getTours().size(); k++) {
            for (Match m : t.getTours().get(k).getMatchs()) {
                ord.ajouterMatch(new MatchOrdonnancable(m, nom, nom,
                        k == 0 ? PhaseMatch.PREMIER_TOUR_TABLEAU : PhaseMatch.TOUR_SUIVANT_TABLEAU));
            }
        }
    }

    private static boolean finaleDeAProposee(Ordonnanceur ord) {
        return ord.proposerLancements(T).stream().anyMatch(p -> p.getMatch().getIdentifiantTableau().equals("A"));
    }

    @Test
    void petit_a_petit_la_finale_attend_que_les_autres_tableaux_aient_fini() {
        Ordonnanceur ord = salle(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT);
        assertFalse(finaleDeAProposee(ord), "finale de A gardée : le tableau B joue encore");
        assertEquals(2, ord.proposerLancements(T).size(), "les deux demies de B sont proposées");
    }

    @Test
    void petit_a_petit_la_finale_attend_aussi_un_tableau_du_jour_pas_encore_commence() {
        Ordonnanceur ord = new Ordonnanceur(10, PoidsOrdonnancement.pour(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT));
        ajouter(ord, "A", tableau("A", 2));
        ord.setTableauxAttendus(Set.of("C"));
        assertFalse(finaleDeAProposee(ord), "le tableau C de l'après-midi n'a pas commencé");
        ord.setTableauxAttendus(Set.of());
        assertTrue(finaleDeAProposee(ord), "plus rien d'autre à jouer : la finale se joue");
    }

    @Test
    void tableaux_commences_tot_chaque_tableau_joue_sa_finale_des_qu_elle_est_prete() {
        for (StrategieOrdonnancement s : List.of(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES,
                StrategieOrdonnancement.MINIMISER_ATTENTE)) {
            assertTrue(finaleDeAProposee(salle(s)), s.name());
        }
    }
}
