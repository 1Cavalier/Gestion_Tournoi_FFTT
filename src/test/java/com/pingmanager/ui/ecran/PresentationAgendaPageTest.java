package com.pingmanager.ui.ecran;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Logique d'affichage de la page Agenda (B045) : types, filtres, mois, grille, prochain. */
class PresentationAgendaPageTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);

    private static AgendaCompte agenda() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("passe").titre("Réunion du club")
                .date(LocalDate.of(2026, 11, 2)).construire());
        a.enregistrer(EvenementAgenda.builder().id("p1").titre("Entraînement").date(LocalDate.of(2026, 11, 20))
                .heure(LocalTime.of(18, 30)).note("Salle 2\nBalles").construire());
        a.enregistrer(EvenementAgenda.builder().id("p0").titre("Stage").date(LocalDate.of(2026, 11, 20))
                .construire());
        a.enregistrer(EvenementAgenda.builder().id("t").titre("Tournoi de Brunoy").date(LocalDate.of(2026, 12, 5))
                .dateFin(LocalDate.of(2026, 12, 6)).lieu("Brunoy").origine(OrigineEvenement.TOURNOI)
                .cleTournoi("k").construire());
        return a;
    }

    private static List<String> ids(List<PresentationAgenda.Element> l) {
        return l.stream().map(PresentationAgenda.Element::id).toList();
    }

    @Test
    void elements_types_modifiables_et_passes() {
        List<PresentationAgenda.Element> e = PresentationAgenda.elements(agenda(), AUJOURDHUI);
        assertEquals(List.of("passe", "p0", "p1", "t"), ids(e), "ordre : jour, sans heure d'abord, titre");
        PresentationAgenda.Element passe = e.get(0);
        assertTrue(passe.passee());
        assertEquals(PresentationAgenda.TypeDate.PERSONNEL, passe.type());
        assertTrue(passe.modifiable());
        PresentationAgenda.Element tournoi = e.get(3);
        assertEquals(PresentationAgenda.TypeDate.TOURNOI, tournoi.type());
        assertFalse(tournoi.modifiable(), "date de tournoi automatique, non modifiable à la main");
        assertEquals("18h30", e.get(2).heure());
        assertNull(e.get(1).heure());
        assertTrue(PresentationAgenda.elements(null, AUJOURDHUI).isEmpty());
    }

    @Test
    void filtres_par_type_et_dates_passees_masquees_par_defaut() {
        List<PresentationAgenda.Element> e = PresentationAgenda.elements(agenda(), AUJOURDHUI);
        assertEquals(List.of("p0", "p1", "t"),
                ids(PresentationAgenda.filtrer(e, EnumSet.allOf(PresentationAgenda.TypeDate.class), false)));
        assertEquals(List.of("passe", "p0", "p1", "t"),
                ids(PresentationAgenda.filtrer(e, EnumSet.allOf(PresentationAgenda.TypeDate.class), true)));
        assertEquals(List.of("t"),
                ids(PresentationAgenda.filtrer(e, EnumSet.of(PresentationAgenda.TypeDate.TOURNOI), false)));
        assertTrue(PresentationAgenda.filtrer(e, EnumSet.noneOf(PresentationAgenda.TypeDate.class), true).isEmpty());
    }

    @Test
    void liste_groupee_par_mois_avec_libelle_en_francais() {
        List<PresentationAgenda.GroupeMois> g = PresentationAgenda.parMois(
                PresentationAgenda.elements(agenda(), AUJOURDHUI));
        assertEquals(2, g.size());
        assertEquals("Novembre 2026", g.get(0).libelle());
        assertEquals(List.of("passe", "p0", "p1"), ids(g.get(0).elements()));
        assertEquals("Décembre 2026", g.get(1).libelle());
        assertEquals(YearMonth.of(2026, 12), g.get(1).mois());
    }

    @Test
    void grille_du_mois_en_semaines_completes_du_lundi_au_dimanche() {
        List<PresentationAgenda.Element> e = PresentationAgenda.elements(agenda(), AUJOURDHUI);
        List<PresentationAgenda.CaseJour> cases = PresentationAgenda.grille(YearMonth.of(2026, 12), e, AUJOURDHUI);
        // décembre 2026 : du mardi 1er au jeudi 31 → du lundi 30 novembre au dimanche 3 janvier
        assertEquals(35, cases.size());
        assertEquals(LocalDate.of(2026, 11, 30), cases.get(0).jour());
        assertFalse(cases.get(0).dansLeMois());
        assertEquals(LocalDate.of(2027, 1, 3), cases.get(34).jour());
        PresentationAgenda.CaseJour samedi5 = cases.get(5);
        PresentationAgenda.CaseJour dimanche6 = cases.get(6);
        assertEquals(List.of(PresentationAgenda.TypeDate.TOURNOI), samedi5.types());
        assertTrue(dimanche6.occupe(), "un tournoi sur deux jours marque les deux jours");
        assertFalse(cases.get(7).occupe());
        assertEquals(7, PresentationAgenda.entetesSemaine().size());
        assertEquals("Lun", PresentationAgenda.entetesSemaine().get(0));
    }

    @Test
    void grille_marque_aujourd_hui_et_un_seul_point_par_type() {
        List<PresentationAgenda.Element> e = PresentationAgenda.elements(agenda(), AUJOURDHUI);
        List<PresentationAgenda.CaseJour> cases = PresentationAgenda.grille(YearMonth.of(2026, 11), e, AUJOURDHUI);
        assertEquals(42, cases.size(), "novembre 2026 commence un dimanche : six semaines");
        PresentationAgenda.CaseJour le13 = cases.stream().filter(c -> c.jour().equals(AUJOURDHUI)).findFirst()
                .orElseThrow();
        assertTrue(le13.aujourdhui());
        PresentationAgenda.CaseJour le20 = cases.stream()
                .filter(c -> c.jour().equals(LocalDate.of(2026, 11, 20))).findFirst().orElseThrow();
        assertEquals(List.of(PresentationAgenda.TypeDate.PERSONNEL), le20.types(), "deux dates perso, un seul point");
    }

    @Test
    void detail_du_jour_et_prochain_evenement() {
        List<PresentationAgenda.Element> e = PresentationAgenda.elements(agenda(), AUJOURDHUI);
        assertEquals(List.of("t"), ids(PresentationAgenda.duJour(e, LocalDate.of(2026, 12, 6))));
        assertEquals(List.of("p0", "p1"), ids(PresentationAgenda.duJour(e, LocalDate.of(2026, 11, 20))));
        assertTrue(PresentationAgenda.duJour(e, LocalDate.of(2026, 11, 21)).isEmpty());

        PresentationAgenda.Element prochain = PresentationAgenda.prochain(e, AUJOURDHUI).orElseThrow();
        assertEquals("p0", prochain.id());
        assertEquals("Prochaine date : Stage, vendredi 20 novembre 2026", PresentationAgenda.texteProchain(prochain));
        assertTrue(PresentationAgenda.prochain(e, LocalDate.of(2027, 1, 1)).isEmpty());
        // un tournoi en cours (deuxième jour) reste le prochain
        assertEquals("t", PresentationAgenda.prochain(e, LocalDate.of(2026, 12, 6)).orElseThrow().id());
    }

    @Test
    void textes_de_ligne_jour_court_et_etats_vides() {
        List<PresentationAgenda.Element> e = PresentationAgenda.elements(agenda(), AUJOURDHUI);
        assertEquals("ven. 20 · 18h30 · Salle 2", PresentationAgenda.detailLigne(e.get(2)));
        assertEquals("sam. 5 → dim. 6 · Brunoy · Tableau : rien", PresentationAgenda.detailLigne(
                new PresentationAgenda.Element("x", LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 6), "X", null,
                        "Brunoy · Tableau : rien", PresentationAgenda.TypeDate.TOURNOI, false, false, false, null,
                        false, false)));
        assertEquals("Vendredi 20 novembre 2026", PresentationAgenda.titreJour(LocalDate.of(2026, 11, 20)));
        assertNotEquals(PresentationAgenda.texteVidePage(true), PresentationAgenda.texteVidePage(false));
        assertTrue(PresentationAgenda.texteVidePage(false).contains("filtres"));
    }

    @Test
    void type_d_une_date_d_apres_son_origine() {
        assertEquals(PresentationAgenda.TypeDate.TOURNOI, PresentationAgenda.TypeDate.de(OrigineEvenement.TOURNOI));
        assertEquals(PresentationAgenda.TypeDate.PERSONNEL,
                PresentationAgenda.TypeDate.de(OrigineEvenement.PERSONNEL));
        assertEquals(PresentationAgenda.TypeDate.PERSONNEL, PresentationAgenda.TypeDate.de(null));
        assertEquals("agenda-type-tournoi", PresentationAgenda.TypeDate.TOURNOI.classeCss());
    }
}
