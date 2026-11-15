package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatPoule3Qualifie2;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cas limites de la prévision (B050) : minuit, remplissage, reproductibilité, cohérence, tournoi vide. */
class PrevisionTournoiLimitesTest {

    private static final LocalDate SAMEDI = JeuDeSimulation.JOUR;
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);
    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    private static Tableau tableau(String nom, Integer placesMax, int inscrits, LocalDateTime debut,
                                   LocalDateTime fin) {
        ConfigurationTableau.Builder cb = ConfigurationTableau.depuisFormat(FormatPoule3Qualifie2.INSTANCE)
                .pointsMax(1499);
        if (placesMax != null) {
            cb.placesMax(placesMax);
        }
        Tableau t = new Tableau(nom, cb.build());
        if (debut != null || fin != null) {
            t.setHoraires(new HorairesTableau(null, null, debut, fin));
        }
        JeuDeSimulation.joueurs(nom, inscrits, 500, 1400, nom.hashCode()).forEach(t::inscrire);
        return t;
    }

    private static Tournoi tournoi(Tableau... tableaux) {
        Tournoi t = new Tournoi("Test", "Club", SAMEDI);
        for (Tableau x : tableaux) {
            t.ajouterTableau(x);
        }
        return t;
    }

    private static ReglagesPrevision.Builder reglages() {
        return ReglagesPrevision.builder().nombreDeTables(3);
    }

    private PrevisionTournoi.EffectifTableau effectif(Tournoi t, RemplissageSimulation r, int i) {
        return simulateur.prevoir(t, reglages().remplissage(r).build()).journees().get(SAMEDI).effectifs().get(i);
    }

    @Test
    void depassement_autour_de_minuit_se_mesure_sur_l_instant_du_lendemain() {
        Tournoi t = tournoi(tableau("A", 16, 16, SAMEDI.atTime(20, 0), null));
        PrevisionTournoi libre = simulateur.prevoir(t, reglages().build());
        StatistiquesSimulation s0 = libre.journees().get(SAMEDI).pour(StrategieOrdonnancement.PAR_DEFAUT)
                .orElseThrow();
        assertTrue(s0.finMoyenne().isAfter(SAMEDI.atTime(23, 0)), "scénario choisi : fin proche de minuit");
        LocalDateTime avantMinuit = SAMEDI.atTime(23, 30);
        LocalDateTime apresMinuit = DIMANCHE.atTime(1, 0);
        for (StrategieOrdonnancement strat : StrategieOrdonnancement.values()) {
            StatistiquesDetaillees court = simulateur.prevoir(t, reglages().finVisee(SAMEDI, avantMinuit).build())
                    .journees().get(SAMEDI).pour(strat).orElseThrow().detail();
            StatistiquesDetaillees long1 = simulateur.prevoir(t, reglages().finVisee(SAMEDI, apresMinuit).build())
                    .journees().get(SAMEDI).pour(strat).orElseThrow().detail();
            // Le dépassement ne devient jamais négatif ni énorme à cause du changement de jour.
            assertTrue(court.depassementMoyen() >= 0 && long1.depassementMoyen() >= 0);
            assertTrue(court.depassementMoyen() >= long1.depassementMoyen());
            assertTrue(court.probabiliteDepassement() >= long1.probabiliteDepassement());
            assertEquals(Duration.between(avantMinuit, court.finAuPlusTard()).toMinutes(),
                    Math.max(0, court.depassementMax()), "dépassement max = fin la plus tardive - 23 h 30");
            if (long1.finAuPlusTard().isBefore(apresMinuit)) {
                assertEquals(0, long1.depassementMax());
                assertEquals(0, long1.probabiliteDepassement());
            }
        }
    }

    @Test
    void fin_visee_exactement_a_minuit_et_un_instant_hors_journee_refuse() {
        LocalDateTime minuit = DIMANCHE.atStartOfDay();
        assertTrue(ReglagesPrevision.finViseeAdmise(SAMEDI, minuit));
        assertTrue(ReglagesPrevision.finViseeAdmise(SAMEDI, DIMANCHE.atTime(23, 59)));
        assertFalse(ReglagesPrevision.finViseeAdmise(SAMEDI, DIMANCHE.plusDays(1).atStartOfDay()));
        assertFalse(ReglagesPrevision.finViseeAdmise(SAMEDI, SAMEDI.minusDays(1).atTime(23, 59)));
        assertFalse(ReglagesPrevision.finViseeAdmise(null, minuit));
        assertFalse(ReglagesPrevision.finViseeAdmise(SAMEDI, null));
        assertEquals(DIMANCHE.atTime(1, 0), ReglagesPrevision.instantFinVisee(SAMEDI, LocalTime.of(1, 0), true));
    }

    @Test
    void taux_zero_aucun_joueur_simule_et_pas_d_exception() {
        Tournoi t = tournoi(tableau("A", 16, 10, SAMEDI.atTime(9, 0), null));
        PrevisionTournoi.EffectifTableau e = effectif(t, RemplissageSimulation.global(0), 0);
        assertEquals(0, e.simules());
        assertEquals(10, e.ecartes());
        assertEquals(0, e.fictifs());
    }

    @Test
    void taux_cent_pour_cent_remplit_a_la_capacite_et_vaut_la_capacite_meme_deja_plein() {
        Tournoi t = tournoi(tableau("A", 16, 10, SAMEDI.atTime(9, 0), null),
                tableau("B", 16, 16, SAMEDI.atTime(9, 0), null));
        PrevisionTournoi p = simulateur.prevoir(t, reglages().remplissage(RemplissageSimulation.global(1)).build());
        List<PrevisionTournoi.EffectifTableau> e = p.journees().get(SAMEDI).effectifs();
        assertEquals(16, e.get(0).simules());
        assertEquals(6, e.get(0).fictifs());
        assertEquals(16, e.get(1).simules());
        assertEquals(0, e.get(1).fictifs());
        assertEquals(0, e.get(1).ecartes());
    }

    @Test
    void taux_par_tableau_prime_sur_le_global_y_compris_zero_contre_cent() {
        Tournoi t = tournoi(tableau("A", 16, 8, SAMEDI.atTime(9, 0), null),
                tableau("B", 16, 8, SAMEDI.atTime(9, 0), null));
        RemplissageSimulation r = RemplissageSimulation.global(1).avecTableau("A", 0);
        assertEquals(0, effectif(t, r, 0).simules());
        assertEquals(16, effectif(t, r, 1).simules());
        RemplissageSimulation r2 = RemplissageSimulation.INSCRITS_REELS.avecTableau("B", 0.5);
        assertEquals(8, effectif(t, r2, 0).simules(), "sans taux global : inscrits réels");
        assertEquals(8, effectif(t, r2, 1).simules());
    }

    @Test
    void tableau_sans_capacite_garde_ses_inscrits_quel_que_soit_le_taux() {
        Tournoi t = tournoi(tableau("A", null, 7, SAMEDI.atTime(9, 0), null));
        for (double taux : new double[] {0, 0.5, 1}) {
            PrevisionTournoi.EffectifTableau e = effectif(t, RemplissageSimulation.global(taux)
                    .avecTableau("A", taux), 0);
            assertEquals(7, e.simules(), "pas de capacité : rien à compléter ni à réduire (taux " + taux + ")");
            assertEquals(0, e.capacite());
        }
    }

    @Test
    void tableau_deja_plus_rempli_que_le_taux_est_reduit_sans_perdre_les_premiers_inscrits() {
        Tournoi t = tournoi(tableau("A", 16, 14, SAMEDI.atTime(9, 0), null));
        PrevisionTournoi.EffectifTableau e = effectif(t, RemplissageSimulation.global(0.5), 0);
        assertEquals(14, e.inscrits());
        assertEquals(8, e.simules());
        assertEquals(6, e.ecartes());
        assertEquals(0, e.fictifs());
        assertEquals(e.simules(), RemplissageSimulation.global(0.5).effectif("A", 16, 14));
    }

    @Test
    void deux_appels_donnent_les_memes_chiffres_meme_avec_fictifs_et_sur_un_tournoi_neuf() {
        ReglagesPrevision r = reglages().remplissage(RemplissageSimulation.global(0.9)).build();
        PrevisionTournoi a = simulateur.prevoir(tournoi(tableau("A", 16, 5, SAMEDI.atTime(9, 0), null)), r);
        PrevisionTournoi b = new SimulateurTournoi().prevoir(
                tournoi(tableau("A", 16, 5, SAMEDI.atTime(9, 0), null)), r);
        assertEquals(a, b);
        assertEquals(a, simulateur.prevoir(tournoi(tableau("A", 16, 5, SAMEDI.atTime(9, 0), null)), r));
    }

    @Test
    void statistiques_coherentes_pour_toutes_les_strategies_et_les_deux_journees() {
        Tournoi t = tournoi(tableau("A", 16, 12, SAMEDI.atTime(9, 0), SAMEDI.atTime(14, 0)),
                tableau("B", 16, 9, SAMEDI.atTime(10, 0), SAMEDI.atTime(18, 0)),
                tableau("C", 12, 12, DIMANCHE.atTime(9, 0), null));
        PrevisionTournoi p = simulateur.prevoir(t, reglages().build());
        for (PrevisionTournoi.PrevisionJournee j : p.journees().values()) {
            for (StatistiquesSimulation s : j.strategies()) {
                StatistiquesDetaillees d = s.detail();
                assertFalse(d.finAuPlusTot().isAfter(s.finTot()), "plus tôt <= 5 %");
                assertFalse(s.finTot().isAfter(s.finMediane()));
                assertFalse(s.finMediane().isAfter(s.finTard()));
                assertFalse(s.finTard().isAfter(d.finAuPlusTard()));
                assertFalse(s.finMoyenne().isBefore(d.finAuPlusTot()));
                assertFalse(s.finMoyenne().isAfter(d.finAuPlusTard()));
                assertFalse(s.finMediane().isBefore(d.finAuPlusTot()));
                for (double pr : new double[] {s.probabiliteDebordement(), d.probabiliteDepassement(),
                        s.debutALHeure(), s.debutLegerRetard(), s.debutGrosRetard()}) {
                    assertTrue(pr >= 0 && pr <= 1, "probabilité hors [0,1] : " + pr);
                }
                assertEquals(1.0, s.debutALHeure() + s.debutLegerRetard() + s.debutGrosRetard(), 1e-9);
                for (StatistiquesDetaillees.RisqueTableau r : d.tableauxARisque()) {
                    assertTrue(r.probabiliteDebordement() >= 0 && r.probabiliteDebordement() <= 1);
                }
                for (StatistiquesSimulation.StatTableau st : s.tableaux()) {
                    assertTrue(st.probabiliteDebordement() >= 0 && st.probabiliteDebordement() <= 1);
                }
                assertTrue(d.ecartTypeFin() >= 0);
                assertTrue(d.depassementMoyen() >= 0 && d.depassementMoyen() <= d.depassementMax());
                assertTrue(d.picTablesMoyen() <= d.picTablesMax() && d.picTablesMax() <= j.nombreDeTables());
                assertTrue(d.retardDebutMoyen() <= d.retardDebutMax());
                // Somme des matchs par heure = nombre de matchs : entier, identique d'une stratégie à l'autre.
                double somme = d.matchsParHeure().stream().mapToDouble(StatistiquesDetaillees.PointHoraire::matchs)
                        .sum();
                double reference = j.strategies().get(0).detail().matchsParHeure().stream()
                        .mapToDouble(StatistiquesDetaillees.PointHoraire::matchs).sum();
                assertTrue(somme > 0);
                assertEquals(reference, somme, 1e-6, "même nombre de matchs quelle que soit la stratégie");
                assertEquals(Math.rint(somme), somme, 1e-6, "nombre de matchs entier (tous tirages identiques)");
                assertTrue(d.matchsParHeure().stream().allMatch(x -> x.matchs() >= 0
                        && x.heure().getMinute() == 0));
            }
        }
    }

    @Test
    void somme_des_matchs_par_heure_egale_le_nombre_de_parties_d_un_tirage() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(4, 5);
        ResultatSimulation r = simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT);
        StatistiquesSimulation s = simulateur.comparerToutes(sc, 20).stream()
                .filter(x -> x.strategie() == StrategieOrdonnancement.PAR_DEFAUT).findFirst().orElseThrow();
        double somme = s.detail().matchsParHeure().stream().mapToDouble(StatistiquesDetaillees.PointHoraire::matchs)
                .sum();
        assertEquals(r.nombreParties(), somme, 1e-6);
    }

    @Test
    void tournoi_sans_tableau_ni_inscrit_prevision_vide_sans_exception() {
        PrevisionTournoi vide = simulateur.prevoir(tournoi(), reglages().build());
        assertTrue(vide.journees().isEmpty());
        assertTrue(SimulateurTournoi.finsViseesProposees(tournoi()).isEmpty());
        assertTrue(SimulateurTournoi.effectifs(tournoi(), RemplissageSimulation.global(1)).isEmpty());
    }

    @Test
    void tableau_sans_inscrit_ou_avec_un_seul_inscrit_sans_exception() {
        for (int inscrits : new int[] {0, 1}) {
            Tournoi t = tournoi(tableau("A", 16, inscrits, SAMEDI.atTime(9, 0), null));
            PrevisionTournoi p = assertDoesNotThrow(() -> simulateur.prevoir(t, reglages().build()));
            for (PrevisionTournoi.PrevisionJournee j : p.journees().values()) {
                for (StatistiquesSimulation s : j.strategies()) {
                    assertNotNull(s.detail());
                    assertFalse(Double.isNaN(s.detail().ecartTypeFin()));
                    assertFalse(Double.isNaN(s.detail().probabiliteDepassement()));
                }
            }
        }
    }

    @Test
    void tournoi_sans_horaire_utilise_la_date_du_tournoi_et_neuf_heures() {
        Tournoi t = tournoi(tableau("A", 16, 9, null, null));
        PrevisionTournoi p = assertDoesNotThrow(() -> simulateur.prevoir(t, reglages().build()));
        assertEquals(List.of(SAMEDI), List.copyOf(p.journees().keySet()));
        assertNotNull(p.journees().get(SAMEDI).finVisee());
        assertEquals(SAMEDI, p.journees().get(SAMEDI).finVisee().toLocalDate());
    }

    @Test
    void reglages_refus_des_valeurs_nulles_et_des_tables_nulles() {
        assertThrows(NullPointerException.class, () -> simulateur.prevoir(null, reglages().build()));
        assertThrows(NullPointerException.class, () -> simulateur.prevoir(tournoi(), null));
        assertThrows(IllegalArgumentException.class, () -> ReglagesPrevision.builder().nombreDeTables(0));
        assertThrows(NullPointerException.class, () -> ReglagesPrevision.builder().remplissage(null));
        assertThrows(IllegalArgumentException.class, () -> RemplissageSimulation.global(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> RemplissageSimulation.global(1.0001));
        assertThrows(IllegalArgumentException.class, () -> RemplissageSimulation.global(-0.0001));
    }
}
