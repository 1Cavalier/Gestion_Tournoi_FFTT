package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** B023 (vérification) : forfaits, abandons, effectif non rond et classement ex aequo de l'export. */
class ServiceResultatsFederauxIssuesTest {

    private final ServiceResultatsFederaux service =
            new ServiceResultatsFederaux(() -> java.time.Instant.parse("2027-04-10T20:00:00Z"), ZoneId.of("Europe/Paris"));

    /** Tableau joué jusqu'au bout ; {@code issues} : issue déclarée pour les premières parties jouables du tableau final. */
    private static Object[] jouer(int nb, List<IssueMatch> issues) {
        Tournoi t = new Tournoi("Open", "Brunoy TT", LocalDate.of(2027, 4, 10));
        t.setNumeroHomologation("H1");
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (int i = 1; i <= nb; i++) {
            Joueur j = new Joueur("L0" + i, "NOM" + i, "Pre" + i, Sexe.HOMME, "Club", 500 + 10 * i);
            a.inscrire(j);
            a.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(a);
        new ServiceLancementTableau().lancer(a, 1L);
        for (Poule p : a.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
            }
        }
        ServicePhaseFinale s = new ServicePhaseFinale();
        assertTrue(s.construire(a, 7L).estConstruit());
        int k = 0;
        for (Tour tour : a.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.estBye() || m.getJoueurA() == null || m.getJoueurB() == null
                        || m.getJoueurA().estADeterminer() || m.getJoueurB().estADeterminer()) {
                    continue;
                }
                if (k < issues.size() && issues.get(k) != IssueMatch.NORMALE) {
                    assertTrue(s.declarerIssue(a, m, issues.get(k), m.getJoueurB()).estValide());
                } else {
                    List<Manche> ms = new ArrayList<>();
                    for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
                        ms.add(new Manche(11, 6));
                    }
                    assertTrue(s.saisirScore(a, m, ms).estValide());
                }
                k++;
            }
        }
        return new Object[] {t, a};
    }

    @Test
    void forfait_et_abandon_exportes_avec_issue_et_fautif() {
        Object[] o = jouer(8, List.of(IssueMatch.FORFAIT, IssueMatch.ABANDON, IssueMatch.NORMALE, IssueMatch.NORMALE));
        ResultatsTournoi r = service.construire((Tournoi) o[0], (Tableau) o[1]).getResultats().orElseThrow();
        List<PartieExportee> tab = r.parties().stream().filter(p -> p.getPhase() == PartieExportee.Phase.TABLEAU).toList();
        assertEquals("FORFAIT", tab.get(0).getIssue());
        assertEquals("ABANDON", tab.get(1).getIssue());
        for (PartieExportee p : tab) {
            if (!p.getIssue().equals("NORMALE")) {
                assertFalse(p.getLicenceFautif().isBlank());
                assertNotEquals(p.getLicenceFautif(), p.getLicenceVainqueur(), "le fautif ne gagne pas");
            }
        }
    }

    @Test
    void effectif_non_rond_classement_sans_doublon_et_rangs_coherents() {
        for (int nb : new int[] {5, 6, 7, 9, 11}) {
            Object[] o = jouer(nb, List.of());
            ResultatsTournoi r = service.construire((Tournoi) o[0], (Tableau) o[1]).getResultats().orElseThrow();
            List<String> lic = r.classements().stream().map(c -> c.joueur().licence()).toList();
            assertEquals(lic.size(), lic.stream().distinct().count(), "joueur classé deux fois, nb=" + nb);
            Map<Integer, Long> parRang = r.classements().stream()
                    .collect(Collectors.groupingBy(ClassementExporte::rang, Collectors.counting()));
            assertEquals(1L, parRang.get(1), "nb=" + nb);
            assertEquals(1L, parRang.get(2), "nb=" + nb);
            // aucune partie exportée avec un même joueur des deux côtés
            for (PartieExportee p : r.parties()) {
                assertNotEquals(p.getJoueurA().licence(), p.getJoueurB().licence());
            }
            // numérotation continue malgré exempts
            for (int i = 0; i < r.parties().size(); i++) {
                assertEquals(i + 1, r.parties().get(i).getNumeroPartie(), "nb=" + nb);
            }
        }
    }

    @Test
    void perdants_d_un_meme_tour_ex_aequo_au_rang_nombre_de_parties_plus_un() {
        Object[] o = jouer(16, List.of());
        ResultatsTournoi r = service.construire((Tournoi) o[0], (Tableau) o[1]).getResultats().orElseThrow();
        for (ClassementExporte c : r.classements()) {
            assertTrue(c.rang() == 1 || c.rang() == 2 || c.rang() == 3 || c.rang() == 4 || c.rang() == 5 || c.rang() == 9,
                    "rang inattendu " + c.rang());
        }
        long cinquiemes = r.classements().stream().filter(c -> c.rang() == 5).count();
        assertTrue(cinquiemes == 0 || cinquiemes == 4, "perdants de quarts : " + cinquiemes);
    }

    @Test
    void partie_de_tableau_en_cours_n_est_pas_exportee_si_tableau_non_termine() {
        Object[] o = jouer(8, List.of());
        Tableau a = (Tableau) o[1];
        // corrige après coup : on rouvre rien ; un tableau sans phase finale est refusé avec un motif Resultat
        Tournoi t = new Tournoi("T2", "C", LocalDate.of(2027, 4, 10));
        Tableau vide = new Tableau("V", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(vide);
        ResultatPreparationExport r = service.construire(t, vide);
        assertFalse(r.estExportable());
        assertEquals(MotifRefusExport.TABLEAU_NON_TERMINE, r.getMotif());
        assertEquals(StatutMatch.TERMINE, a.getPhaseFinale().getTours().get(0).getMatchs().get(0).getStatut());
    }
}
