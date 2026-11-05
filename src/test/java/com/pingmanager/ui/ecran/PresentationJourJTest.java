package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Présentation des écrans du jour J, testée sans JavaFX. */
class PresentationJourJTest {

    private static Joueur j(String licence, String nom, String club) {
        return new Joueur(licence, nom, "Prénom" + licence, Sexe.HOMME, club, 1000);
    }

    private static Tableau tableau(String nom, int places, LocalDateTime debut) {
        Tableau t = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(places).tailleMaxListeAttente(4).build());
        if (debut != null) {
            t.setHoraires(new HorairesTableau(debut.minusMinutes(60), debut.minusMinutes(20), debut));
        }
        return t;
    }

    @Test
    void etape_suit_le_cycle_du_tableau() {
        Tableau tab = tableau("A", 0, null);
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.ajouterTableau(tab);
        assertEquals(PresentationJourJ.Etape.INSCRIPTION, PresentationJourJ.etape(tab));
        for (int i = 1; i <= 3; i++) {
            tab.inscrire(j("L" + i, "N" + i, "C" + i));
        }
        tab.ouvrirPointage();
        assertEquals("Pointage ouvert", PresentationJourJ.libelle(PresentationJourJ.etape(tab)));
        tab.getInscrits().forEach(tab::pointer);
        new ServicePointage(t).cloturerPointage(tab);
        assertEquals(PresentationJourJ.Etape.POINTAGE_CLOTURE, PresentationJourJ.etape(tab));
        new ServiceLancementTableau(t).lancer(tab, 1L);
        assertEquals(PresentationJourJ.Etape.POULES, PresentationJourJ.etape(tab));
        assertEquals("etape-poules", PresentationJourJ.classe(PresentationJourJ.etape(tab)));
        assertEquals("1 poule · 0 / 3 matchs joués", PresentationJourJ.resumePoules(tab));
        for (Match m : tab.getPoules().get(0).getMatchs()) {
            m.definirScore(List.of(new Manche(11, 1), new Manche(11, 1), new Manche(11, 1)));
        }
        assertEquals(PresentationJourJ.Etape.POULES_TERMINEES, PresentationJourJ.etape(tab));
    }

    @Test
    void tableaux_groupes_par_jour_et_tries_par_heure() {
        Tournoi t = new Tournoi("Open", "Club", List.of(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15)));
        Tableau dimanche = tableau("D", 0, LocalDateTime.of(2026, 11, 15, 9, 0));
        Tableau samediTard = tableau("B", 0, LocalDateTime.of(2026, 11, 14, 14, 0));
        Tableau samediTot = tableau("C", 0, LocalDateTime.of(2026, 11, 14, 9, 0));
        Tableau sansHoraire = tableau("Z", 0, null);
        t.ajouterTableau(dimanche);
        t.ajouterTableau(sansHoraire);
        t.ajouterTableau(samediTard);
        t.ajouterTableau(samediTot);
        Map<LocalDate, List<Tableau>> parJour = PresentationJourJ.tableauxParJour(t);
        assertEquals(Arrays.asList(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15), null),
                new java.util.ArrayList<>(parJour.keySet()));
        assertEquals(List.of(samediTot, samediTard), parJour.get(LocalDate.of(2026, 11, 14)));
        assertEquals("Samedi 14 novembre 2026", PresentationJourJ.jour(LocalDate.of(2026, 11, 14)));
        assertEquals("Sans horaire", PresentationJourJ.jour(null));
        assertEquals("Pointage 8h – 8h40 · début 9h", PresentationJourJ.horaires(samediTot));
        assertEquals("Horaires non renseignés", PresentationJourJ.horaires(sansHoraire));
    }

    @Test
    void resume_et_texte_de_cloture_du_pointage() {
        Tableau tab = tableau("A", 3, null);
        for (int i = 1; i <= 5; i++) {
            tab.inscrire(j("L" + i, "N" + i, "C"));
        }
        tab.ouvrirPointage();
        tab.pointer(j("L1", "", ""));
        tab.marquerPaye(j("L1", "", ""));
        tab.marquerPaye(j("L2", "", ""));
        assertEquals("3 / 3 inscrits · 1 pointé · 2 payés · 2 en liste d'attente", PresentationJourJ.resumePointage(tab));
        assertTrue(PresentationJourJ.texteCloture(tab).startsWith("À la clôture, 2 inscrits non pointés libèrent"));
        tab.getInscrits().forEach(tab::pointer);
        assertEquals("À la clôture, aucune place n'est libérée : tous les inscrits ont pointé.",
                PresentationJourJ.texteCloture(tab));
    }

    @Test
    void recherche_sans_accents_et_tri_par_nom() {
        Joueur eloise = j("111", "Éclair", "Toulouse");
        Joueur bob = j("222", "Bernard", "Brunoy");
        Joueur ana = j("333", "Arnaud", "Créteil Ping");
        assertEquals(List.of(eloise), PresentationJourJ.filtrer(List.of(eloise, bob, ana), "ecl"));
        assertEquals(List.of(ana), PresentationJourJ.filtrer(List.of(eloise, bob, ana), "creteil"));
        assertEquals(List.of(bob), PresentationJourJ.filtrer(List.of(eloise, bob, ana), "222"));
        assertEquals(3, PresentationJourJ.filtrer(List.of(eloise, bob, ana), "  ").size());
        assertEquals(List.of(ana, bob, eloise), PresentationJourJ.parNom(List.of(eloise, bob, ana)));
    }

    @Test
    void graine_courte_et_lecture_tolerante() {
        long g = PresentationJourJ.nouvelleGraine(new Random(3));
        assertTrue(g >= 100_000 && g <= 999_999);
        assertEquals(Long.valueOf(123456L), PresentationJourJ.lireGraine(" 123 456 ").orElseThrow());
        assertTrue(PresentationJourJ.lireGraine("12a").isEmpty());
        assertTrue(PresentationJourJ.lireGraine("-5").isEmpty());
        assertTrue(PresentationJourJ.lireGraine("").isEmpty());
    }

    @Test
    void lecture_des_champs_de_manches() {
        PresentationJourJ.LectureManches ok = PresentationJourJ.lireManches(List.of("9", "-8", "11-5", "", ""));
        assertTrue(ok.estLisible());
        assertEquals(List.of(new Manche(11, 9), new Manche(8, 11), new Manche(11, 5)), ok.manches());
        assertEquals("2-1", PresentationJourJ.manchesGagnees(ok.manches()));

        PresentationJourJ.LectureManches trou = PresentationJourJ.lireManches(List.of("9", "", "7", "", ""));
        assertEquals(2, trou.premiereIllisible());
        PresentationJourJ.LectureManches impossible = PresentationJourJ.lireManches(List.of("9", "11-10", "", "", ""));
        assertEquals(2, impossible.premiereInvalide());
        assertFalse(impossible.estLisible());
    }

    @Test
    void resultat_affiche_score_ou_issue() {
        Joueur a = j("A", "Durand", "C1");
        Joueur b = j("B", "Martin", "C2");
        Match m = new Match(a, b);
        assertEquals("", PresentationJourJ.resultat(m));
        m.definirScore(List.of(new Manche(11, 9), new Manche(8, 11), new Manche(11, 5), new Manche(12, 10)));
        assertEquals("3-1 (11-9, 8-11, 11-5, 12-10)", PresentationJourJ.resultat(m));
        m.declarerIssue(IssueMatch.FORFAIT, b);
        assertEquals("Forfait de " + b.getNomComplet(), PresentationJourJ.resultat(m));
        assertEquals("Forfait (absent à l'appel)", PresentationJourJ.libelleIssue(IssueMatch.FORFAIT));
        assertEquals(4, PresentationJourJ.ISSUES_NON_NORMALES.size());
    }

    @Test
    void libelles_de_qualification_et_de_rang() {
        assertEquals("Qualifié", PresentationJourJ.libelleQualification(SuiviPoule.Qualification.QUALIFIE));
        assertEquals("Place qualificative",
                PresentationJourJ.libelleQualification(SuiviPoule.Qualification.PLACE_QUALIFICATIVE));
        assertEquals("", PresentationJourJ.libelleQualification(SuiviPoule.Qualification.HORS_PLACE_QUALIFICATIVE));
        assertEquals("1er", PresentationJourJ.rang(1));
        assertEquals("3e", PresentationJourJ.rang(3));
    }
}
