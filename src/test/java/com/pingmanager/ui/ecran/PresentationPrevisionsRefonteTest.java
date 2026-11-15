package com.pingmanager.ui.ecran;

import com.pingmanager.core.PrevisionTournoi.EffectifTableau;
import com.pingmanager.core.PrevisionTournoi.PrevisionJournee;
import com.pingmanager.core.RemplissageSimulation;
import com.pingmanager.core.StatistiquesDetaillees;
import com.pingmanager.core.StatistiquesDetaillees.PointHoraire;
import com.pingmanager.core.StatistiquesDetaillees.RisqueTableau;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/** Écran « Prévisions et stratégie » refait (B050) : cartes, explication, réglages et statistiques par thème. */
class PresentationPrevisionsRefonteTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 4, 10);
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    // ------------------------------------------------------------------
    // Cartes de stratégie
    // ------------------------------------------------------------------

    @Test
    void trois_cartes_avec_trois_lignes_d_information_chacune() {
        Tournoi t = new Tournoi("Open", "Club", SAMEDI);
        List<PresentationPrevisions.CarteStrategie> cartes = PresentationPrevisions.cartes(t);
        assertEquals(3, cartes.size());
        for (PresentationPrevisions.CarteStrategie c : cartes) {
            assertEquals(c.strategie().getLibelle(), c.nom());
            assertFalse(c.lancement().isBlank());
            assertFalse(c.pourQuelTournoi().isBlank());
            assertEquals(c.strategie().getContrepartie(), c.pointFaible());
            assertNotEquals(c.lancement(), c.pourQuelTournoi());
        }
        assertEquals(3, cartes.stream().map(PresentationPrevisions.CarteStrategie::lancement).distinct().count());
        assertEquals(3, cartes.stream().map(PresentationPrevisions.CarteStrategie::pourQuelTournoi).distinct().count());
        assertEquals(2, PresentationPrevisions.MICRO_DESCRIPTION.split("\n").length, "micro-description de deux lignes");
        assertEquals("Choisir cette stratégie", PresentationPrevisions.BOUTON_CHOISIR);
    }

    @Test
    void sans_choix_la_strategie_par_defaut_est_signalee_sans_etre_marquee_choisie() {
        Tournoi t = new Tournoi("Open", "Club", SAMEDI);
        List<PresentationPrevisions.CarteStrategie> cartes = PresentationPrevisions.cartes(t);
        assertTrue(cartes.stream().noneMatch(PresentationPrevisions.CarteStrategie::choisie));
        PresentationPrevisions.CarteStrategie defaut = cartes.stream()
                .filter(c -> c.strategie() == StrategieOrdonnancement.PAR_DEFAUT).findFirst().orElseThrow();
        assertEquals("Appliquée par défaut", defaut.marque());
        assertEquals(2, cartes.stream().filter(c -> c.marque() == null).count());
    }

    @Test
    void la_carte_choisie_est_marquee_strategie_choisie() {
        Tournoi t = new Tournoi("Open", "Club", SAMEDI);
        t.setStrategieOrdonnancement(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT);
        List<PresentationPrevisions.CarteStrategie> cartes = PresentationPrevisions.cartes(t);
        assertEquals(List.of(false, true, false),
                cartes.stream().map(PresentationPrevisions.CarteStrategie::choisie).toList());
        assertEquals("Stratégie choisie", cartes.get(1).marque());
        assertNull(cartes.get(0).marque());
        // Choisir explicitement la stratégie par défaut la marque comme choisie.
        t.setStrategieOrdonnancement(StrategieOrdonnancement.PAR_DEFAUT);
        assertEquals("Stratégie choisie", PresentationPrevisions.cartes(t).stream()
                .filter(PresentationPrevisions.CarteStrategie::choisie).findFirst().orElseThrow().marque());
    }

    // ------------------------------------------------------------------
    // Explication
    // ------------------------------------------------------------------

    @Test
    void explication_dit_les_cent_tirages_les_durees_et_le_sens_des_chiffres() {
        assertEquals("Comment sont calculées ces prévisions", PresentationPrevisions.TITRE_EXPLICATION);
        List<PresentationPrevisions.Explication> e = PresentationPrevisions.explications();
        assertEquals("Le tournoi est rejoué 100 fois", e.get(0).titre());
        assertTrue(e.get(0).texte().contains("exactement les mêmes 100 tirages"));
        assertTrue(e.get(1).texte().contains("entre le minimum et le maximum"));
        assertTrue(e.get(1).texte().contains("durée moyenne réglée"));
        assertTrue(e.get(2).texte().contains("échauffement"));
        String chiffres = e.get(3).texte();
        for (String mot : List.of("Moyenne", "Médiane", "Au plus tôt et au plus tard", "Risque")) {
            assertTrue(chiffres.contains(mot), mot);
        }
        String tout = e.toString() + PresentationPrevisions.messageSimulationEnCours();
        assertFalse(tout.toLowerCase().contains("graine"), "plus de graine à l'écran");
        assertTrue(PresentationPrevisions.messageSimulationEnCours().contains("100 fois"));
    }

    // ------------------------------------------------------------------
    // Fin visée : jour et heure
    // ------------------------------------------------------------------

    @Test
    void fin_visee_proposee_le_jour_meme_ou_le_lendemain() {
        Map<LocalDate, LocalDateTime> proposees = new TreeMap<>();
        proposees.put(SAMEDI, DIMANCHE.atTime(1, 0));
        proposees.put(DIMANCHE, DIMANCHE.atTime(19, 30));
        List<PresentationPrevisions.FinProposee> fins = PresentationPrevisions.finsProposees(proposees);
        assertEquals(new PresentationPrevisions.FinProposee(SAMEDI, "01:00", true), fins.get(0));
        assertEquals(new PresentationPrevisions.FinProposee(DIMANCHE, "19:30", false), fins.get(1));
        assertEquals("Le jour même (samedi 10 avril)", PresentationPrevisions.libelleChoixJour(SAMEDI, false));
        assertEquals("Le lendemain (dimanche 11 avril)", PresentationPrevisions.libelleChoixJour(SAMEDI, true));
    }

    @Test
    void fin_visee_lue_en_jour_et_heure_et_ecrite_en_clair() {
        PresentationPrevisions.ReglageFinVisee nuit = PresentationPrevisions.lireFinVisee(SAMEDI, "1 h", true);
        assertNull(nuit.erreur());
        assertEquals(DIMANCHE.atTime(1, 0), nuit.instant());
        assertEquals("Samedi 10 avril, fin visée dimanche 11 avril à 1 h",
                PresentationPrevisions.phraseFinVisee(SAMEDI, nuit.instant()));
        assertEquals(SAMEDI.atTime(20, 30), PresentationPrevisions.lireFinVisee(SAMEDI, "20:30", false).instant());
        assertEquals("Samedi 10 avril, fin visée samedi 10 avril à 20 h 30",
                PresentationPrevisions.apercuFinVisee(SAMEDI, "20h30", false));
        assertEquals("Samedi 10 avril", PresentationPrevisions.phraseFinVisee(SAMEDI, null));
    }

    @Test
    void fin_visee_vide_ou_illisible_refusee_en_nommant_la_journee() {
        for (String saisie : new String[]{"", "  ", "tard", "25:00", null}) {
            PresentationPrevisions.ReglageFinVisee r = PresentationPrevisions.lireFinVisee(SAMEDI, saisie, false);
            assertNull(r.instant(), String.valueOf(saisie));
            assertEquals("Samedi 10 avril : " + PresentationPrevisions.ERREUR_FIN_VISEE, r.erreur());
        }
        assertTrue(PresentationPrevisions.apercuFinVisee(SAMEDI, "tard", true).startsWith("Samedi 10 avril : Indiquez"));
    }

    // ------------------------------------------------------------------
    // Temps autour du match
    // ------------------------------------------------------------------

    @Test
    void temps_autour_du_match_presentes_par_groupes_avec_unites() {
        List<PresentationPrevisions.GroupeAnnexes> groupes = PresentationPrevisions.groupesAnnexes();
        assertEquals(List.of("Avant le match", "Pendant le match", "Après le match"),
                groupes.stream().map(PresentationPrevisions.GroupeAnnexes::nom).toList());
        assertEquals(List.of("Appel des joueurs (min)", "Joueur à rappeler (%)", "Retard d'un rappel (min)",
                        "Échauffement (min)"),
                groupes.get(0).champs().stream().map(PresentationPrevisions.ChampAnnexe::libelle).toList());
        assertEquals(List.of("1", "15", "3", "2"),
                groupes.get(0).champs().stream().map(PresentationPrevisions.ChampAnnexe::valeur).toList());
        assertEquals("Pause entre deux manches (s)", groupes.get(1).champs().get(0).libelle());
        assertEquals("30", groupes.get(1).champs().get(0).valeur());
        assertEquals("20", groupes.get(1).champs().get(1).valeur());
        assertEquals("1,5", groupes.get(2).champs().get(0).valeur());
        assertEquals("3 min 50 en moyenne par match", groupes.get(0).moyenne(), "234 s");
        assertEquals("1 min 30 en moyenne par match", groupes.get(2).moyenne());
    }

    @Test
    void les_valeurs_proposees_relues_redonnent_les_temps_par_defaut() {
        Map<String, String> saisies = new LinkedHashMap<>();
        PresentationPrevisions.groupesAnnexes().forEach(g -> g.champs().forEach(c -> saisies.put(c.cle(), c.valeur())));
        TempsAnnexesMatch relu = PresentationPrevisions.lireTempsAnnexes(saisies).temps();
        assertEquals(TempsAnnexesMatch.PAR_DEFAUT.moyenne(5), relu.moyenne(5), 1e-6);
        assertEquals(TempsAnnexesMatch.PAR_DEFAUT.getFeuille(), relu.getFeuille());
        assertEquals(TempsAnnexesMatch.PAR_DEFAUT.getPauseEntreManches(), relu.getPauseEntreManches());
    }

    @Test
    void temps_autour_du_match_lus_dans_leur_unite_avec_erreurs_claires() {
        TempsAnnexesMatch t = PresentationPrevisions.lireTempsAnnexes(Map.of("appel", "0,5", "probabiliteRappel", "40",
                "pauseEntreManches", "45", "feuille", " 2 ")).temps();
        assertEquals(30, t.getAppel(), "0,5 min");
        assertEquals(0.4, t.getProbabiliteRappel(), 1e-9);
        assertEquals(45, t.getPauseEntreManches(), "secondes");
        assertEquals(120, t.getFeuille());
        assertEquals(120, t.getEchauffement(), "non saisi : valeur de départ");
        assertEquals(PresentationPrevisions.ERREUR_ANNEXES,
                PresentationPrevisions.lireTempsAnnexes(Map.of("feuille", "deux")).erreur());
        assertEquals(PresentationPrevisions.ERREUR_ANNEXES,
                PresentationPrevisions.lireTempsAnnexes(Map.of("probabiliteRappel", "NaN")).erreur());
        assertTrue(PresentationPrevisions.lireTempsAnnexes(Map.of("probabiliteTempsMort", "120")).erreur()
                .contains("entre 0 et 100 %"));
        assertTrue(PresentationPrevisions.lireTempsAnnexes(Map.of("appel", "-1")).erreur().contains("négatif"));
    }

    @Test
    void total_par_match_recalcule_selon_la_saisie() {
        PresentationPrevisions.ResumeAnnexes defaut = PresentationPrevisions.resumeAnnexes(Map.of());
        assertEquals(3, defaut.moyennes().size());
        // 234 + (4 × 30 + 2 × 20 % × 60 × 0,75) + 90 = 462 s.
        assertEquals("Total autour d'un match joué en 5 manches : 7 min 40 en moyenne, en plus de la durée du jeu.",
                defaut.total());
        PresentationPrevisions.ResumeAnnexes sansAppel = PresentationPrevisions.resumeAnnexes(Map.of("appel", "0"));
        assertEquals("2 min 50 en moyenne par match", sansAppel.moyennes().get(0));
        assertTrue(sansAppel.total().contains("6 min 40"), sansAppel.total());
        PresentationPrevisions.ResumeAnnexes faux = PresentationPrevisions.resumeAnnexes(Map.of("appel", "x"));
        assertTrue(faux.moyennes().isEmpty());
        assertEquals(PresentationPrevisions.ERREUR_ANNEXES, faux.total());
    }

    // ------------------------------------------------------------------
    // Remplissage
    // ------------------------------------------------------------------

    @Test
    void choix_global_de_remplissage_lu() {
        assertEquals(List.of("Inscrits réels", "100 %", "95 %", "Autre valeur"),
                java.util.Arrays.stream(PresentationPrevisions.ChoixRemplissage.values())
                        .map(PresentationPrevisions.ChoixRemplissage::libelle).toList());
        assertEquals(RemplissageSimulation.INSCRITS_REELS, PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.INSCRITS_REELS, "80", Map.of()).remplissage());
        assertEquals(RemplissageSimulation.global(1.0), PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.PLEIN, "", Map.of()).remplissage());
        assertEquals(RemplissageSimulation.global(0.95), PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.QUATRE_VINGT_QUINZE, "", Map.of()).remplissage());
        assertEquals(RemplissageSimulation.global(0.8), PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.AUTRE, " 80 % ", Map.of()).remplissage());
        assertEquals("Taux global : " + PresentationPrevisions.ERREUR_TAUX, PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.AUTRE, "", Map.of()).erreur());
        assertEquals("Taux global : " + PresentationPrevisions.ERREUR_TAUX, PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.AUTRE, "120", Map.of()).erreur());
    }

    @Test
    void taux_par_tableau_facultatif_et_prioritaire() {
        Map<String, String> parTableau = new LinkedHashMap<>();
        parTableau.put("A", "50");
        parTableau.put("B", "  ");
        RemplissageSimulation r = PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.PLEIN, "", parTableau).remplissage();
        assertEquals(0.5, r.tauxPour("A").getAsDouble(), 1e-9);
        assertEquals(1.0, r.tauxPour("B").getAsDouble(), 1e-9, "champ vide : taux global");
        // Un taux par tableau s'applique même sans taux global.
        RemplissageSimulation seul = PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.INSCRITS_REELS, "", Map.of("A", "87,5")).remplissage();
        assertEquals(0.875, seul.tauxPour("A").getAsDouble(), 1e-9);
        assertTrue(seul.tauxPour("B").isEmpty());
        assertEquals("Tableau « A » : " + PresentationPrevisions.ERREUR_TAUX, PresentationPrevisions.lireRemplissage(
                PresentationPrevisions.ChoixRemplissage.PLEIN, "", Map.of("A", "plein")).erreur());
        assertNull(PresentationPrevisions.lireTaux("-5"));
        assertNull(PresentationPrevisions.lireTaux("NaN"));
        assertEquals(0.0, PresentationPrevisions.lireTaux("0").doubleValue());
    }

    @Test
    void apercu_du_remplissage_fictifs_ajoutes_ou_inscrits_ecartes() {
        List<EffectifTableau> effectifs = List.of(
                new EffectifTableau("A", SAMEDI.atTime(9, 0), 20, 24, 1.0, 24),
                new EffectifTableau("B", SAMEDI.atTime(10, 0), 17, 16, 0.95, 15),
                new EffectifTableau("C", SAMEDI.atTime(11, 0), 12, 0, -1, 12),
                new EffectifTableau("D", SAMEDI.atTime(12, 0), 9, 16, -1, 9),
                new EffectifTableau("E", SAMEDI.atTime(12, 0), 15, 16, 1.0, 16));
        List<PresentationPrevisions.LigneRemplissage> l = PresentationPrevisions.lignesRemplissage(effectifs);
        assertEquals(new PresentationPrevisions.LigneRemplissage("A", "20", "24", "100 %", "24", "+ 4 fictifs",
                StatutPastille.INFO, true), l.get(0));
        assertEquals(new PresentationPrevisions.LigneRemplissage("B", "17", "16", "95 %", "15", "2 inscrits écartés",
                StatutPastille.ATTENTION, true), l.get(1));
        assertEquals(new PresentationPrevisions.LigneRemplissage("C", "12", "Sans limite", "—", "12", "Inscrits réels",
                StatutPastille.NEUTRE, false), l.get(2));
        assertEquals("—", l.get(3).taux());
        assertTrue(l.get(3).reglable());
        assertEquals("+ 1 fictif", l.get(4).effet());
        assertEquals("76 inscriptions simulées pour 73 réelles : 5 fictifs ajoutés, 2 inscrits écartés.",
                PresentationPrevisions.resumeRemplissage(effectifs));
        assertEquals("21 inscriptions simulées pour 21 réelles : les tableaux sont joués avec leurs inscrits.",
                PresentationPrevisions.resumeRemplissage(effectifs.subList(2, 4)));
        assertEquals("Aucun tableau à simuler.", PresentationPrevisions.resumeRemplissage(List.of()));
    }

    // ------------------------------------------------------------------
    // Statistiques par thème
    // ------------------------------------------------------------------

    private static StatistiquesSimulation stat(StrategieOrdonnancement s, int finMinutes, double risque,
                                               List<RisqueTableau> risques) {
        LocalDateTime fin = SAMEDI.atTime(23, 0).plusMinutes(finMinutes);
        StatistiquesDetaillees detail = new StatistiquesDetaillees(fin.minusMinutes(50), fin.plusMinutes(70), 18.4,
                DIMANCHE.atTime(0, 30), risque, 6.2, 44, 7.6, 52, 11.4, 95, 81.5, 140, 13.4, 16,
                List.of(new PointHoraire(SAMEDI.atTime(9, 0), 12.44), new PointHoraire(SAMEDI.atTime(10, 0), 18),
                        new PointHoraire(DIMANCHE.atTime(0, 0), 0.3)),
                fin.minusHours(6), fin.minusMinutes(35), 14 * 60 + finMinutes, risques);
        return new StatistiquesSimulation(s, 100, 16, fin, fin.minusMinutes(30), fin.minusMinutes(2),
                fin.plusMinutes(40), fin.plusMinutes(70), fin.minusHours(5), fin.minusHours(2), 0.6, 0.3, 0.1, 14, 12,
                19, 62, 0.71, risque, 6.4, 0, List.of(), detail);
    }

    private static PrevisionJournee journee() {
        List<RisqueTableau> risques = List.of(
                new RisqueTableau("Toutes séries", DIMANCHE.atTime(0, 30), 0.42, 17.6, 9),
                new RisqueTableau("Moins de 1500", DIMANCHE.atTime(0, 30), 0.05, 0.2, 3),
                new RisqueTableau("Jeunes", DIMANCHE.atTime(0, 30), 0, 0, 0));
        return new PrevisionJournee(SAMEDI, DIMANCHE.atTime(0, 30), 16, List.of(), List.of(
                stat(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, 40, 0.3, risques),
                stat(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, 90, 0.6, risques.subList(0, 1)),
                stat(StrategieOrdonnancement.MINIMISER_ATTENTE, 10, 0, List.of(risques.get(2)))));
    }

    private static PresentationPrevisions.LigneStrategies ligne(PresentationPrevisions.GroupeStatistiques g,
                                                                String debut) {
        return g.lignes().stream().filter(l -> l.indicateur().startsWith(debut)).findFirst()
                .orElseThrow(() -> new AssertionError(debut + " absent de " + g.titre()));
    }

    @Test
    void statistiques_regroupees_par_theme_dans_l_ordre_de_l_ecran() {
        List<PresentationPrevisions.GroupeStatistiques> groupes = PresentationPrevisions.groupesStatistiques(journee());
        assertEquals(List.of("Fin de journée", "Ponctualité des tableaux", "Attente des joueurs", "Tables", "Finales",
                        "Tableaux à risque", "Matchs par heure"),
                groupes.stream().map(PresentationPrevisions.GroupeStatistiques::titre).toList());
        assertEquals("Samedi 10 avril, fin visée dimanche 11 avril à 0 h 30.", groupes.get(0).aide());
        for (PresentationPrevisions.GroupeStatistiques g : groupes) {
            assertFalse(g.lignes().isEmpty(), g.titre());
            assertFalse(g.aide().isBlank(), g.titre());
            g.lignes().forEach(l -> assertEquals(3, l.valeurs().size(), l.indicateur()));
        }
        assertEquals(11, groupes.get(0).lignes().size());
    }

    @Test
    void fin_de_journee_heures_du_lendemain_dispersion_et_risque() {
        PresentationPrevisions.GroupeStatistiques fin = PresentationPrevisions.groupesStatistiques(journee()).get(0);
        PresentationPrevisions.LigneStrategies moyenne = ligne(fin, "Fin moyenne");
        assertEquals(List.of("23 h 40", "0 h 30 (lendemain)", "23 h 10"), moyenne.valeurs());
        assertEquals(2, moyenne.meilleure(), "la plus tôt");
        assertEquals("22 h 50", ligne(fin, "Fin au plus tôt").valeurs().get(0));
        assertEquals("0 h 50 (lendemain)", ligne(fin, "Fin au plus tard").valeurs().get(0));
        assertEquals("23 h 38", ligne(fin, "Fin médiane").valeurs().get(0));
        assertEquals("± 18 min", ligne(fin, "Dispersion").valeurs().get(0));
        PresentationPrevisions.LigneStrategies risque = ligne(fin, "Risque de dépasser la fin visée");
        assertEquals(List.of("30 %", "60 %", "0 %"), risque.valeurs());
        assertEquals(2, risque.meilleure());
        assertEquals("6 min", ligne(fin, "Dépassement moyen").valeurs().get(0));
        assertEquals("44 min", ligne(fin, "Plus grand dépassement").valeurs().get(0));
        assertEquals(List.of("14 h 40", "15 h 30", "14 h 10"), ligne(fin, "Durée de la journée").valeurs());
    }

    @Test
    void ponctualite_attente_tables_et_finales() {
        List<PresentationPrevisions.GroupeStatistiques> g = PresentationPrevisions.groupesStatistiques(journee());
        assertEquals("60 %", ligne(g.get(1), "Début à l'heure").valeurs().get(0));
        assertEquals("8 min", ligne(g.get(1), "Retard moyen au début").valeurs().get(0));
        assertEquals("52 min", ligne(g.get(1), "Plus grand retard").valeurs().get(0));
        assertEquals(-1, ligne(g.get(1), "Plus grand retard").meilleure(), "égalité");

        assertEquals("14 min", ligne(g.get(2), "Attente moyenne avant un match").valeurs().get(0));
        assertEquals("11 min", ligne(g.get(2), "Attente médiane").valeurs().get(0));
        assertEquals("1 h 35", ligne(g.get(2), "Plus longue attente observée").valeurs().get(0));
        assertEquals("1 h 22", ligne(g.get(2), "Plus longue pause d'un joueur").valeurs().get(0));
        assertEquals("2 h 20", ligne(g.get(2), "Plus longue pause observée").valeurs().get(0));

        assertEquals("71 %", ligne(g.get(3), "Occupation").valeurs().get(0));
        assertEquals("13,4 sur 16", ligne(g.get(3), "Pic de tables").valeurs().get(0));
        assertEquals("16 sur 16", ligne(g.get(3), "Pic le plus haut").valeurs().get(0));

        assertEquals(List.of("17 h 40", "18 h 30", "17 h 10"), ligne(g.get(4), "Première finale").valeurs());
        assertEquals("23 h 05", ligne(g.get(4), "Dernière finale").valeurs().get(0));
        assertEquals(-1, ligne(g.get(4), "Première finale").meilleure(), "les finales ne sont pas notées");
    }

    @Test
    void tableaux_a_risque_classes_par_strategie() {
        PresentationPrevisions.GroupeStatistiques risque = PresentationPrevisions.groupesStatistiques(journee()).get(5);
        assertEquals(2, risque.lignes().size(), "le troisième rang n'a aucun tableau à risque");
        assertEquals("Le plus à risque", risque.lignes().get(0).indicateur());
        assertEquals(List.of("Toutes séries : 42 %, + 18 min", "Toutes séries : 42 %, + 18 min", "Aucun"),
                risque.lignes().get(0).valeurs());
        assertEquals("2e plus à risque", risque.lignes().get(1).indicateur());
        assertEquals(List.of("Moins de 1500 : 5 %", "—", "—"), risque.lignes().get(1).valeurs());
    }

    @Test
    void matchs_par_heure_une_ligne_par_heure_et_l_heure_la_plus_chargee() {
        PresentationPrevisions.GroupeStatistiques h = PresentationPrevisions.groupesStatistiques(journee()).get(6);
        assertEquals(List.of("Heure la plus chargée", "9 h – 10 h", "10 h – 11 h", "0 h – 1 h (lendemain)"),
                h.lignes().stream().map(PresentationPrevisions.LigneStrategies::indicateur).toList());
        assertEquals("10 h – 11 h : 18 matchs", h.lignes().get(0).valeurs().get(0));
        assertEquals("12,4", h.lignes().get(1).valeurs().get(0));
        assertEquals("0,3", h.lignes().get(3).valeurs().get(2));
    }

    @Test
    void statistiques_sans_detail_restent_affichables() {
        StatistiquesSimulation sans = new StatistiquesSimulation(StrategieOrdonnancement.PAR_DEFAUT, 100, 8, null,
                null, null, null, null, null, null, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, List.of());
        List<PresentationPrevisions.GroupeStatistiques> g = PresentationPrevisions.groupesStatistiques(
                new PrevisionJournee(SAMEDI, null, 8, List.of(), List.of(sans)));
        assertEquals("—", ligne(g.get(0), "Fin moyenne").valeurs().get(0));
        assertEquals("—", ligne(g.get(0), "Durée de la journée").valeurs().get(0));
        assertEquals("Aucun joueur", ligne(g.get(2), "Attente moyenne d'un joueur inscrit sur 1").valeurs().get(0));
        assertEquals(List.of("Aucun"), g.get(5).lignes().get(0).valeurs());
        assertEquals(List.of("—"), g.get(6).lignes().get(0).valeurs());
        assertEquals("Samedi 10 avril.", g.get(0).aide());
    }

    @Test
    void colonne_de_la_strategie_choisie_et_nombres_a_la_francaise() {
        List<StatistiquesSimulation> stats = journee().strategies();
        assertEquals(1, PresentationPrevisions.colonneChoisie(stats, StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT));
        assertEquals(-1, PresentationPrevisions.colonneChoisie(List.of(), StrategieOrdonnancement.PAR_DEFAUT));
        assertEquals("13", PresentationPrevisions.nombre(12.96));
        assertEquals("12,4", PresentationPrevisions.nombre(12.44));
        assertEquals("0", PresentationPrevisions.nombre(0));
        assertEquals("1 h", PresentationPrevisions.heureCourte(java.time.LocalTime.of(1, 0)));
        assertEquals("20 h 05", PresentationPrevisions.heureCourte(java.time.LocalTime.of(20, 5)));
    }
}
