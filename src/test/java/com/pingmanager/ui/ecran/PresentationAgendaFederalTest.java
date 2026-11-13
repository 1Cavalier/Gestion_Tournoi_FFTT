package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.DateFederale;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import com.pingmanager.model.StatutDateFederale;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Suggestions du calendrier fédéral dans la page Agenda (B045), sans JavaFX. */
class PresentationAgendaFederalTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);

    private static DateFederale date(String id, String categorie, LocalDate debut, StatutDateFederale statut) {
        return DateFederale.builder().id(id).categorie(categorie).titre("Date " + id).date(debut)
                .statut(statut).source("https://www.fftt.com/site/competition/criterium-federal")
                .reluLe(AUJOURDHUI).construire();
    }

    private static final List<DateFederale> FEDERALES = List.of(
            date("t1", "Critérium fédéral", LocalDate.of(2026, 10, 9), StatutDateFederale.A_VERIFIER),
            date("t2", "Critérium fédéral", LocalDate.of(2026, 11, 14), StatutDateFederale.A_VERIFIER),
            date("e1", "Championnat par équipes", LocalDate.of(2026, 11, 21), StatutDateFederale.CONFIRMEE));

    @Test
    void dates_non_ajoutees_proposees_en_suggestions_non_modifiables() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        List<PresentationAgenda.Element> e = PresentationAgenda.avecCalendrier(
                PresentationAgenda.elements(a, AUJOURDHUI), FEDERALES, a, AUJOURDHUI);
        assertEquals(3, e.size());
        assertTrue(a.estVide(), "rien n'est ajouté à l'agenda");
        PresentationAgenda.Element t2 = e.get(1);
        assertEquals("t2", t2.id());
        assertTrue(t2.suggestion());
        assertFalse(t2.modifiable());
        assertEquals(PresentationAgenda.TypeDate.FEDERAL, t2.type());
        assertEquals("À vérifier", t2.statut());
        assertTrue(t2.aVerifier());
        assertEquals("Suggestion · source fftt.com, relue le 13/11/2026", t2.detail());
        assertTrue(e.get(0).passee(), "1er tour passé : masqué par défaut");
        assertEquals(List.of("t2", "e1"), PresentationAgenda.filtrer(e,
                EnumSet.allOf(PresentationAgenda.TypeDate.class), false).stream()
                .map(PresentationAgenda.Element::id).toList());
        assertTrue(PresentationAgenda.filtrer(e, EnumSet.of(PresentationAgenda.TypeDate.PERSONNEL), true).isEmpty());
        assertTrue(PresentationAgenda.prochain(e, AUJOURDHUI).isEmpty(), "une suggestion n'est pas le prochain");
    }

    @Test
    void date_deja_ajoutee_marquee_dans_mon_agenda_sans_doublon() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        ServiceAgenda.parDefaut().ajouterDateFederale(a, FEDERALES.get(1));
        List<PresentationAgenda.Element> e = PresentationAgenda.avecCalendrier(
                PresentationAgenda.elements(a, AUJOURDHUI), FEDERALES, a, AUJOURDHUI);
        assertEquals(3, e.size(), "la copie remplace la suggestion");
        PresentationAgenda.Element copie = e.get(1);
        assertEquals("federal-t2", copie.id());
        assertFalse(copie.suggestion());
        assertTrue(copie.dansMonAgenda());
        assertTrue(copie.modifiable());
        assertEquals("À vérifier", copie.statut(), "le statut reste visible une fois ajoutée");
        assertEquals("federal-t2", PresentationAgenda.prochain(e, AUJOURDHUI).orElseThrow().id());
    }

    @Test
    void categories_pour_tout_ajouter_avec_compteurs_et_textes() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        ServiceAgenda.parDefaut().ajouterDateFederale(a, FEDERALES.get(0));
        List<PresentationAgenda.CategorieFederale> c = PresentationAgenda.categories(FEDERALES, a);
        assertEquals(List.of("Critérium fédéral", "Championnat par équipes"),
                c.stream().map(PresentationAgenda.CategorieFederale::nom).toList());
        assertEquals(1, c.get(0).dejaAjoutees());
        assertEquals(1, c.get(0).restantes());
        assertEquals("2 dates, 1 dans mon agenda, 2 à vérifier", PresentationAgenda.resumeCategorie(c.get(0)));
        assertEquals("1 date", PresentationAgenda.resumeCategorie(c.get(1)));
        assertTrue(PresentationAgenda.texteConfirmationToutAjouter(c.get(0)).startsWith(
                "Ajouter à votre agenda 1 date « Critérium fédéral » ?"));
        assertEquals("2 dates ajoutées à votre agenda.", PresentationAgenda.texteApresToutAjouter(2));
        assertTrue(PresentationAgenda.texteApresToutAjouter(0).contains("déjà"));
        assertEquals("Calendrier fédéral 2026-2027", PresentationAgenda.titreCalendrier(FEDERALES));
    }

    @Test
    void grille_du_mois_avec_point_federal_et_type_d_une_copie() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        List<PresentationAgenda.Element> e = PresentationAgenda.avecCalendrier(
                PresentationAgenda.elements(a, AUJOURDHUI), FEDERALES, a, AUJOURDHUI);
        assertTrue(PresentationAgenda.grille(java.time.YearMonth.of(2026, 11), e, AUJOURDHUI).stream()
                .anyMatch(c -> c.types().contains(PresentationAgenda.TypeDate.FEDERAL)));
        assertEquals(PresentationAgenda.TypeDate.FEDERAL, PresentationAgenda.TypeDate.de(OrigineEvenement.FEDERAL));
        EvenementAgenda copie = EvenementAgenda.builder().id("f").titre("X").date(AUJOURDHUI)
                .origine(OrigineEvenement.FEDERAL).construire();
        assertEquals("Date fédérale", PresentationAgenda.etiquette(copie));
        assertEquals("fftt.com", PresentationAgenda.domaine("https://www.fftt.com/a/b"));
        assertEquals("ligue.fr", PresentationAgenda.domaine("ligue.fr"));
    }
}
