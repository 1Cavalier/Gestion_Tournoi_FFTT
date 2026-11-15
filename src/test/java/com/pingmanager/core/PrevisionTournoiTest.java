package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatPoule3Qualifie2;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prévision d'un tournoi (B050) : 100 tirages fixes et graine interne, fin visée
 * jour et heure (éventuellement le lendemain), remplissage, statistiques
 * détaillées par stratégie.
 */
class PrevisionTournoiTest {

    private static final LocalDate SAMEDI = JeuDeSimulation.JOUR;
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    private final SimulateurTournoi simulateur = new SimulateurTournoi();

    private static Tableau tableau(String nom, int placesMax, int inscrits, LocalDateTime debut, LocalDateTime fin) {
        Tableau t = new Tableau(nom, ConfigurationTableau.depuisFormat(FormatPoule3Qualifie2.INSTANCE)
                .placesMax(placesMax).pointsMax(1499).build());
        t.setHoraires(new HorairesTableau(null, null, debut, fin));
        JeuDeSimulation.joueurs(nom, inscrits, 500, 1400, nom.hashCode()).forEach(t::inscrire);
        return t;
    }

    /** Deux tableaux le samedi (9 h et 10 h), un le dimanche (9 h). */
    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Test", "Club", SAMEDI);
        t.ajouterTableau(tableau("A", 16, 12, SAMEDI.atTime(9, 0), SAMEDI.atTime(14, 0)));
        t.ajouterTableau(tableau("B", 16, 9, SAMEDI.atTime(10, 0), SAMEDI.atTime(18, 30)));
        t.ajouterTableau(tableau("C", 12, 12, DIMANCHE.atTime(9, 0), null));
        return t;
    }

    private static ReglagesPrevision.Builder reglages() {
        return ReglagesPrevision.builder().nombreDeTables(4);
    }

    @Test
    void cent_tirages_fixes_et_graine_interne_resultat_reproductible() {
        assertEquals(100, SimulateurTournoi.NOMBRE_DE_TIRAGES);
        PrevisionTournoi a = simulateur.prevoir(tournoi(), reglages().build());
        PrevisionTournoi b = simulateur.prevoir(tournoi(), reglages().build());
        assertEquals(a, b, "mêmes réglages, même tournoi : exactement les mêmes chiffres");
        assertEquals(100, a.tirages());
        assertEquals(List.of(SAMEDI, DIMANCHE), List.copyOf(a.journees().keySet()), "une prévision par journée");
        for (PrevisionTournoi.PrevisionJournee j : a.journees().values()) {
            assertEquals(StrategieOrdonnancement.values().length, j.strategies().size());
            assertEquals(4, j.nombreDeTables());
            for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
                StatistiquesSimulation stat = j.pour(s).orElseThrow();
                assertEquals(100, stat.tirages());
                assertEquals(0, stat.tiragesIncomplets());
                assertNotSame(StatistiquesDetaillees.VIDE, stat.detail());
            }
        }
        assertEquals(List.of("A", "B"), a.journees().get(SAMEDI).effectifs().stream()
                .map(PrevisionTournoi.EffectifTableau::nom).toList());
        assertEquals(List.of("C"), a.journees().get(DIMANCHE).effectifs().stream()
                .map(PrevisionTournoi.EffectifTableau::nom).toList());
    }

    @Test
    void les_reglages_changent_la_prevision() {
        PrevisionTournoi base = simulateur.prevoir(tournoi(), reglages().build());
        PrevisionTournoi lente = simulateur.prevoir(tournoi(), reglages()
                .loiDuree(new LoiDureeMatch(Duration.ofMinutes(35), Duration.ofMinutes(20), Duration.ofMinutes(60)))
                .build());
        PrevisionTournoi sansAnnexes = simulateur.prevoir(tournoi(), reglages()
                .tempsAnnexes(TempsAnnexesMatch.AUCUN).build());
        StrategieOrdonnancement s = StrategieOrdonnancement.PAR_DEFAUT;
        LocalDateTime fin = base.journees().get(SAMEDI).pour(s).orElseThrow().finMoyenne();
        assertTrue(lente.journees().get(SAMEDI).pour(s).orElseThrow().finMoyenne().isAfter(fin));
        assertTrue(sansAnnexes.journees().get(SAMEDI).pour(s).orElseThrow().finMoyenne().isBefore(fin));
        assertEquals(0, sansAnnexes.journees().get(SAMEDI).pour(s).orElseThrow().tempsAnnexesMoyen());
    }

    @Test
    void fin_visee_proposee_la_fin_theorique_la_plus_tardive_sinon_vingt_heures() {
        SortedMap<LocalDate, LocalDateTime> fins = SimulateurTournoi.finsViseesProposees(tournoi());
        assertEquals(SAMEDI.atTime(18, 30), fins.get(SAMEDI));
        assertEquals(DIMANCHE.atTime(ScenarioSimulation.HEURE_LIMITE_PAR_DEFAUT), fins.get(DIMANCHE));
        PrevisionTournoi p = simulateur.prevoir(tournoi(), reglages().build());
        assertEquals(SAMEDI.atTime(18, 30), p.journees().get(SAMEDI).finVisee(), "sans réglage : la fin proposée");
        assertEquals(DIMANCHE.atTime(20, 0), p.journees().get(DIMANCHE).finVisee());

        // Une fin théorique déjà enregistrée le lendemain est reprise telle quelle.
        Tournoi nuit = new Tournoi("Nuit", "Club", SAMEDI);
        nuit.ajouterTableau(tableau("N", 12, 9, SAMEDI.atTime(20, 0), DIMANCHE.atTime(0, 30)));
        assertEquals(DIMANCHE.atTime(0, 30), SimulateurTournoi.finsViseesProposees(nuit).get(SAMEDI));
    }

    @Test
    void fin_visee_jour_et_heure_pouvant_tomber_le_lendemain() {
        LocalDateTime dimancheUneHeure = ReglagesPrevision.instantFinVisee(SAMEDI, LocalTime.of(1, 0), true);
        assertEquals(DIMANCHE.atTime(1, 0), dimancheUneHeure);
        assertEquals(SAMEDI.atTime(19, 0), ReglagesPrevision.instantFinVisee(SAMEDI, LocalTime.of(19, 0), false));

        PrevisionTournoi large = simulateur.prevoir(tournoi(), reglages().finVisee(SAMEDI, dimancheUneHeure).build());
        PrevisionTournoi.PrevisionJournee samedi = large.journees().get(SAMEDI);
        assertEquals(dimancheUneHeure, samedi.finVisee());
        for (StatistiquesSimulation s : samedi.strategies()) {
            assertEquals(dimancheUneHeure, s.detail().finVisee());
            assertEquals(0, s.detail().probabiliteDepassement(), "samedi visé dimanche 1 h : aucun dépassement");
            assertEquals(0, s.probabiliteDebordement());
            assertEquals(0, s.detail().depassementMoyen());
            assertEquals(0, s.detail().depassementMax());
            assertTrue(s.tableaux().stream().allMatch(t -> dimancheUneHeure.equals(t.finSouhaitee())),
                    "tous les tableaux de la journée visent le même instant");
        }
        // La fin visée du samedi ne touche pas le dimanche.
        assertEquals(DIMANCHE.atTime(20, 0), large.journees().get(DIMANCHE).finVisee());

        // Fin visée intenable : le risque et le dépassement se calculent sur l'instant visé.
        LocalDateTime tot = SAMEDI.atTime(10, 30);
        for (StatistiquesSimulation s : simulateur.prevoir(tournoi(), reglages().finVisee(SAMEDI, tot).build())
                .journees().get(SAMEDI).strategies()) {
            assertEquals(1.0, s.detail().probabiliteDepassement());
            long moyen = Duration.between(tot, s.finMoyenne()).toMinutes();
            assertEquals(moyen, s.detail().depassementMoyen(), 1.0, "dépassement moyen = fin moyenne − fin visée");
            assertEquals(Duration.between(tot, s.detail().finAuPlusTard()).toMinutes(), s.detail().depassementMax());
        }
    }

    @Test
    void fin_visee_avant_la_journee_ou_apres_le_lendemain_refusee() {
        assertTrue(ReglagesPrevision.finViseeAdmise(SAMEDI, SAMEDI.atTime(23, 59)));
        assertTrue(ReglagesPrevision.finViseeAdmise(SAMEDI, DIMANCHE.atTime(23, 59)));
        assertFalse(ReglagesPrevision.finViseeAdmise(SAMEDI, SAMEDI.minusDays(1).atTime(23, 0)));
        assertFalse(ReglagesPrevision.finViseeAdmise(SAMEDI, SAMEDI.plusDays(2).atTime(0, 0)));
        assertFalse(ReglagesPrevision.finViseeAdmise(SAMEDI, null));
        assertThrows(IllegalArgumentException.class,
                () -> reglages().finVisee(SAMEDI, SAMEDI.plusDays(2).atTime(1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> reglages().finVisee(SAMEDI, SAMEDI.minusDays(1).atTime(20, 0)));
        assertThrows(IllegalArgumentException.class, () -> reglages().nombreDeTables(0));
        assertTrue(reglages().build().finVisee(SAMEDI).isEmpty());
        assertEquals(RemplissageSimulation.INSCRITS_REELS, reglages().build().getRemplissage());
        assertEquals(TempsAnnexesMatch.PAR_DEFAUT, reglages().build().getTempsAnnexes());
    }

    @Test
    void scenario_avec_fin_visee_instant_remplace_les_fins_des_tableaux() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(4, 5).avecFinVisee(DIMANCHE.atTime(1, 0));
        assertTrue(sc.getTableaux().stream().allMatch(t -> DIMANCHE.atTime(1, 0).equals(t.finSouhaitee())));
        assertEquals(JeuDeSimulation.petitTournoi(4, 5).getTableaux().get(0).joueurs(), sc.getTableaux().get(0).joueurs());
        assertEquals(5, sc.getGraine());
        assertThrows(NullPointerException.class,
                () -> JeuDeSimulation.petitTournoi(4, 5).avecFinVisee((LocalDateTime) null));
    }

    @Test
    void le_remplissage_complete_ou_reduit_les_tableaux_de_la_prevision() {
        PrevisionTournoi reel = simulateur.prevoir(tournoi(), reglages().build());
        PrevisionTournoi regle = simulateur.prevoir(tournoi(), reglages()
                .remplissage(RemplissageSimulation.global(1.0).avecTableau("B", 0.5)).build());
        List<PrevisionTournoi.EffectifTableau> e = regle.journees().get(SAMEDI).effectifs();
        assertEquals(16, e.get(0).simules(), "A : 16 × 100 %");
        assertEquals(8, e.get(1).simules(), "B : 16 × 50 %, le taux du tableau prime");
        assertEquals(List.of(12, 9), reel.journees().get(SAMEDI).effectifs().stream()
                .map(PrevisionTournoi.EffectifTableau::simules).toList());
        StrategieOrdonnancement s = StrategieOrdonnancement.PAR_DEFAUT;
        assertNotEquals(reel.journees().get(SAMEDI).pour(s).orElseThrow().finMoyenne(),
                regle.journees().get(SAMEDI).pour(s).orElseThrow().finMoyenne());
        assertEquals(reel.journees().get(DIMANCHE).strategies(), regle.journees().get(DIMANCHE).strategies(),
                "C est déjà plein : les chiffres de sa journée ne changent pas");
    }

    @Test
    void mesures_d_un_tirage_matchs_par_heure_pic_pauses_et_finales() {
        ResultatSimulation r = simulateur.simuler(JeuDeSimulation.petitTournoi(4, 5), StrategieOrdonnancement.PAR_DEFAUT);
        MesuresTirage m = r.mesures();
        assertEquals(r.nombreParties(), m.matchsParHeure().values().stream().mapToInt(Integer::intValue).sum(),
                "chaque partie est comptée dans son heure de lancement");
        assertTrue(m.matchsParHeure().keySet().stream().allMatch(h -> h.getMinute() == 0 && h.getSecond() == 0));
        assertEquals(r.debut().truncatedTo(java.time.temporal.ChronoUnit.HOURS), m.matchsParHeure().firstKey());
        assertTrue(m.picTables() >= 1 && m.picTables() <= 4, "jamais plus de tables occupées que de tables");
        assertEquals(4, m.picTables(), "63 joueurs sur 4 tables : toutes occupées à un moment");
        assertTrue(m.attenteMediane() >= 0 && m.attenteMediane() <= r.attenteMaximale());
        assertTrue(m.plusLonguePause() > 0);
        assertNotNull(m.joueurPlusLonguePause());
        assertEquals(Duration.between(r.debut(), r.fin()).toMinutes(), m.dureeJournee());
        assertFalse(m.debutPremiereFinale().isAfter(m.debutDerniereFinale()));
        assertTrue(m.debutDerniereFinale().isBefore(r.fin()), "la dernière finale est lancée avant la fin");
        assertTrue(m.debutPremiereFinale().isAfter(r.debut()));
        assertThrows(UnsupportedOperationException.class, () -> m.matchsParHeure().clear());
    }

    @Test
    void la_pause_compte_le_temps_reel_entre_deux_tableaux_pas_l_attente() {
        // Les mêmes joueurs jouent A à 9 h puis B à 15 h : longue pause, attente courte.
        List<com.pingmanager.model.Joueur> js = JeuDeSimulation.joueurs("P", 6, 500, 1200, 1);
        ScenarioSimulation sc = ScenarioSimulation.builder().nombreDeTables(6).graine(3)
                .tableau(JeuDeSimulation.tableau("A", LocalTime.of(9, 0), js))
                .tableau(JeuDeSimulation.tableau("B", LocalTime.of(15, 0), js)).build();
        ResultatSimulation r = simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT);
        LocalDateTime finA = r.tableau("A").orElseThrow().fin();
        long trou = Duration.between(finA, JeuDeSimulation.JOUR.atTime(15, 0)).toMinutes();
        assertTrue(trou > 120, "A est fini bien avant 15 h");
        assertTrue(r.mesures().plusLonguePause() >= trou, "la pause couvre le trou entre les deux tableaux");
        assertTrue(r.attenteMaximale() < trou, "l'attente ne court qu'à partir du début du tableau");
    }

    @Test
    void statistiques_detaillees_coherentes_avec_les_indicateurs_existants() {
        ScenarioSimulation sc = JeuDeSimulation.petitTournoi(4, 5);
        int parties = simulateur.simuler(sc, StrategieOrdonnancement.PAR_DEFAUT).nombreParties();
        for (StatistiquesSimulation s : simulateur.comparerToutes(sc, 30)) {
            StatistiquesDetaillees d = s.detail();
            // Fin : au plus tôt ≤ 5 % ≤ médiane ≤ 95 % ≤ au plus tard.
            assertFalse(d.finAuPlusTot().isAfter(s.finTot()));
            assertFalse(s.finTot().isAfter(s.finMediane()));
            assertFalse(s.finTard().isAfter(d.finAuPlusTard()));
            assertEquals(s.finMax(), d.finAuPlusTard());
            assertTrue(d.ecartTypeFin() > 0, "les fins varient d'un tirage à l'autre");
            assertTrue(d.ecartTypeFin() <= Duration.between(d.finAuPlusTot(), d.finAuPlusTard()).toMinutes());
            // Fin visée : la plus tardive des fins souhaitées (ici 20 h, heure limite par défaut).
            assertEquals(JeuDeSimulation.JOUR.atTime(20, 0), d.finVisee());
            assertTrue(d.probabiliteDepassement() >= 0 && d.probabiliteDepassement() <= 1);
            assertEquals(s.probabiliteDebordement(), d.probabiliteDepassement(),
                    "tous les tableaux visent le même instant : même risque");
            // Retards de début.
            assertTrue(d.retardDebutMoyen() >= 0 && d.retardDebutMoyen() <= d.retardDebutMax());
            // Attentes et pauses.
            assertTrue(d.attenteMediane() >= 0 && d.attenteMediane() <= d.attenteMaximale());
            assertTrue(s.plusLongueAttente() <= d.attenteMaximale(), "moyenne des maximums ≤ maximum observé");
            assertTrue(d.plusLonguePause() > 0 && d.plusLonguePause() <= d.plusLonguePauseMax());
            // Tables.
            assertTrue(d.picTablesMoyen() <= d.picTablesMax() && d.picTablesMax() <= 4);
            assertTrue(d.picTablesMoyen() >= s.occupationMoyenne() * 4 - 1e-9, "le pic dépasse la moyenne");
            // Courbe horaire : la somme fait le nombre de parties d'un tirage.
            assertEquals(parties, d.matchsParHeure().stream()
                    .mapToDouble(StatistiquesDetaillees.PointHoraire::matchs).sum(), 1e-6);
            for (int i = 1; i < d.matchsParHeure().size(); i++) {
                assertTrue(d.matchsParHeure().get(i - 1).heure().isBefore(d.matchsParHeure().get(i).heure()));
            }
            assertTrue(d.heureLaPlusChargee().matchs() >= d.matchsParHeure().get(0).matchs());
            // Finales et durée de la journée.
            assertFalse(d.debutPremiereFinale().isAfter(d.debutDerniereFinale()));
            assertTrue(d.debutDerniereFinale().isBefore(s.finMoyenne()));
            assertEquals(Duration.between(JeuDeSimulation.JOUR.atTime(9, 0), s.finMoyenne()).toMinutes(),
                    d.dureeJourneeMoyenne(), 1.0);
        }
    }

    @Test
    void tableaux_les_plus_a_risque_classes_du_plus_risque_au_moins_risque() {
        // A doit finir à 10 h (intenable), B à 23 h (large), C à 14 h.
        ScenarioSimulation base = JeuDeSimulation.petitTournoi(4, 5);
        LocalTime[] fins = {LocalTime.of(10, 0), LocalTime.of(23, 0), LocalTime.of(14, 0)};
        ScenarioSimulation.Builder b = ScenarioSimulation.builder().nombreDeTables(4).graine(5);
        for (int i = 0; i < 3; i++) {
            ScenarioSimulation.TableauSimule t = base.getTableaux().get(i);
            b.tableau(new ScenarioSimulation.TableauSimule(t.nom(), t.debut(), t.joueurs(), t.format(), false,
                    JeuDeSimulation.JOUR.atTime(fins[i]), 0));
        }
        StatistiquesSimulation s = simulateur.simulerPlusieurs(b.build(), StrategieOrdonnancement.PAR_DEFAUT, 20);
        List<StatistiquesDetaillees.RisqueTableau> risques = s.detail().tableauxARisque();
        assertEquals(3, risques.size(), "tous les tableaux sont classés");
        assertEquals("A", risques.get(0).nom());
        assertEquals(1.0, risques.get(0).probabiliteDebordement());
        assertTrue(risques.get(0).depassementMoyen() > 0);
        assertEquals(JeuDeSimulation.JOUR.atTime(10, 0), risques.get(0).finSouhaitee());
        assertEquals("B", risques.get(2).nom());
        assertEquals(0.0, risques.get(2).probabiliteDebordement());
        assertEquals(0.0, risques.get(2).depassementMoyen());
        for (int i = 1; i < risques.size(); i++) {
            assertTrue(risques.get(i - 1).probabiliteDebordement() >= risques.get(i).probabiliteDebordement());
        }
        for (StatistiquesDetaillees.RisqueTableau r : risques) {
            StatistiquesSimulation.StatTableau st = s.tableaux().stream()
                    .filter(t -> t.nom().equals(r.nom())).findFirst().orElseThrow();
            assertEquals(st.probabiliteDebordement(), r.probabiliteDebordement());
            assertEquals(st.retardDebutMoyen(), r.retardDebutMoyen());
        }
        // Fin visée de la journée : la plus tardive des fins souhaitées (23 h), jamais dépassée ici.
        assertEquals(JeuDeSimulation.JOUR.atTime(23, 0), s.detail().finVisee());
        assertEquals(0, s.detail().probabiliteDepassement());
        assertEquals(1.0, s.probabiliteDebordement(), "l'indicateur existant : au moins un tableau déborde");
    }

    @Test
    void anciens_constructeurs_et_scenario_vide_sans_detail() {
        ResultatSimulation vide = simulateur.simuler(ScenarioSimulation.builder().build(),
                StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals(-1, vide.mesures().dureeJournee());
        assertEquals(0, vide.mesures().picTables());
        assertNull(vide.mesures().debutPremiereFinale());
        assertTrue(vide.mesures().matchsParHeure().isEmpty());
        ResultatSimulation ancien = new ResultatSimulation(StrategieOrdonnancement.PAR_DEFAUT, 1, 4, null, null,
                List.of(), List.of(), 0, 0, 0, null, 0, true);
        assertSame(MesuresTirage.VIDE, ancien.mesures());

        StatistiquesSimulation s = simulateur.simulerPlusieurs(ScenarioSimulation.builder().build(),
                StrategieOrdonnancement.PAR_DEFAUT, 3);
        assertNull(s.detail().finAuPlusTot());
        assertNull(s.detail().finVisee());
        assertEquals(0, s.detail().probabiliteDepassement());
        assertEquals(-1, s.detail().dureeJourneeMoyenne());
        assertTrue(s.detail().matchsParHeure().isEmpty());
        assertNull(s.detail().heureLaPlusChargee());
        assertTrue(s.detail().tableauxARisque().isEmpty());
        assertTrue(simulateur.prevoir(new Tournoi("Vide", "Club", SAMEDI), reglages().build()).journees().isEmpty());
    }
}
