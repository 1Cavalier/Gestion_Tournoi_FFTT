package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.ordonnancement.MatchOrdonnancable;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import com.pingmanager.ordonnancement.PropositionLancement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de charge mesurés (B025) : 400 joueurs, 15 tableaux sur deux jours, un
 * Open au tableau final de 128 et un de 256, 40 tables. Les temps sont affichés
 * (sortie standard) et reportés dans le journal de la tâche ; les seuils sont
 * larges pour ne pas dépendre de la machine.
 */
class ChargeSimulationTest {

    @Test
    void grand_tournoi_simule_en_entier_et_en_temps_raisonnable_pour_chaque_strategie() {
        ScenarioSimulation sc = JeuDeSimulation.grandTournoi(40, 11);
        long joueurs = sc.getTableaux().stream().flatMap(t -> t.joueurs().stream())
                .map(Joueur::getNumeroLicence).distinct().count();
        assertEquals(400, joueurs);
        assertEquals(15, sc.getTableaux().size());
        assertTrue(sc.getTableaux().stream().anyMatch(t -> t.joueurs().size() > 97 && t.joueurs().size() <= 192),
                "au moins un tableau final de 128 places");

        SimulateurTournoi simulateur = new SimulateurTournoi();
        simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT); // chauffe de la JVM
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            long debut = System.nanoTime();
            ResultatSimulation r = simulateur.simuler(sc, s);
            long ms = (System.nanoTime() - debut) / 1_000_000;
            System.out.println("[B025] simulation 400 joueurs / 15 tableaux / 40 tables, " + s + " : " + ms
                    + " ms, " + r.nombreParties() + " parties, fin " + r.fin() + ", attente moyenne "
                    + Math.round(r.attenteMoyenne()) + " min, max " + r.attenteMaximale() + " min, occupation "
                    + Math.round(r.occupationTables() * 100) + " %, " + r.tableauxQuiDebordent().size()
                    + " tableau(x) qui débordent");
            assertTrue(r.complete(), s.name());
            assertEquals(15, r.tableaux().size());
            assertTrue(r.nombreParties() > 1000);
            assertTrue(ms < 20_000, "simulation trop lente : " + ms + " ms");
        }
    }

    @Test
    void ordonnanceur_propose_vite_avec_deux_mille_matchs_prets() {
        Ordonnanceur ord = new Ordonnanceur(40, PoidsOrdonnancement.pour(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES));
        List<Joueur> js = JeuDeSimulation.joueurs("P", 400, 500, 3000, 3);
        java.util.Random r = new java.util.Random(5);
        for (int i = 0; i < 2000; i++) {
            Joueur a = js.get(r.nextInt(400));
            Joueur b = js.get(r.nextInt(400));
            if (a.equals(b)) {
                continue;
            }
            PhaseMatch phase = PhaseMatch.values()[i % 3];
            ord.ajouterMatch(new MatchOrdonnancable(new Match(a, b), "T" + (i % 15), "T" + (i % 15), phase));
        }
        LocalDateTime maintenant = LocalDateTime.of(2027, 4, 10, 9, 0);
        ord.proposerLancements(maintenant); // chauffe
        long debut = System.nanoTime();
        int vagues = 100;
        List<PropositionLancement> props = List.of();
        for (int i = 0; i < vagues; i++) {
            props = ord.proposerLancements(maintenant.plusMinutes(i));
        }
        double msParVague = (System.nanoTime() - debut) / 1e6 / vagues;
        System.out.printf("[B025] ordonnanceur, 2000 matchs, 40 tables libres : %.2f ms par vague de propositions%n",
                msParVague);
        assertEquals(40, props.size());
        assertTrue(msParVague < 500, "proposition trop lente : " + msParVague + " ms");
    }
}
