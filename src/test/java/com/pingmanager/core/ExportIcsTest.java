package com.pingmanager.core;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Export iCalendar (RFC 5545) écrit à la main : en-têtes, CRLF, échappement, pliage, UID, DTSTAMP. */
class ExportIcsTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-05T14:30:05Z");
    private final ExportIcs export = new ExportIcs(() -> MAINTENANT);

    private static AgendaCompte agenda() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("p1").titre("Entraînement; salle 2, Brunoy")
                .date(LocalDate.of(2027, 1, 12)).heure(LocalTime.of(18, 30)).note("Ligne 1\nLigne \\ 2").construire());
        a.enregistrer(EvenementAgenda.builder().id("tournoi-x").titre("National B de Brunoy")
                .date(LocalDate.of(2027, 4, 10)).dateFin(LocalDate.of(2027, 4, 11)).lieu("Gymnase, 91800 Brunoy")
                .origine(OrigineEvenement.TOURNOI).cleTournoi("k").construire());
        return a;
    }

    @Test
    void en_tetes_du_calendrier_et_d_un_evenement() {
        String ics = export.exporter(agenda());
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//PingManager//Agenda//FR\r\n"), ics);
        assertTrue(ics.endsWith("END:VEVENT\r\nEND:VCALENDAR\r\n"));
        assertEquals(2, ics.split("BEGIN:VEVENT", -1).length - 1);
        assertTrue(ics.contains("\r\nUID:p1@pingmanager\r\n"));
        assertTrue(ics.contains("\r\nUID:tournoi-x@pingmanager\r\n"));
        assertTrue(ics.contains("\r\nDTSTAMP:20261105T143005Z\r\n"));
    }

    @Test
    void toutes_les_lignes_finissent_par_crlf_sans_lf_isole() {
        String ics = export.exporter(agenda());
        assertFalse(ics.replace("\r\n", "").contains("\n"), "aucun LF isolé");
        assertFalse(ics.replace("\r\n", "").contains("\r"), "aucun CR isolé");
    }

    @Test
    void date_avec_heure_en_heure_locale_et_tournoi_en_journees_entieres_fin_exclusive() {
        String ics = export.exporter(agenda());
        assertTrue(ics.contains("\r\nDTSTART:20270112T183000\r\n"));
        assertTrue(ics.contains("\r\nDTSTART;VALUE=DATE:20270410\r\nDTEND;VALUE=DATE:20270412\r\n"));
        assertTrue(ics.contains("\r\nLOCATION:Gymnase\\, 91800 Brunoy\r\n"));
        assertTrue(ics.contains("\r\nCATEGORIES:Tournoi\r\n"));
    }

    @Test
    void echappement_des_textes() {
        assertEquals("a\\;b\\,c\\\\d\\ne", ExportIcs.echapper("a;b,c\\d\ne"));
        assertEquals("x\\ny\\nz", ExportIcs.echapper("x\r\ny\rz"));
        assertEquals("", ExportIcs.echapper(null));
        String ics = export.exporter(agenda());
        assertTrue(ics.contains("\r\nSUMMARY:Entraînement\\; salle 2\\, Brunoy\r\n"));
        assertTrue(ics.contains("\r\nDESCRIPTION:Ligne 1\\nLigne \\\\ 2\r\n"));
    }

    @Test
    void lignes_longues_pliees_a_75_octets_sans_couper_un_caractere() {
        String titre = "é".repeat(60) + " tournoi";
        AgendaCompte a = new AgendaCompte("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("long").titre(titre).date(LocalDate.of(2027, 1, 1)).construire());
        String ics = export.exporter(a);
        for (String ligne : ics.split("\r\n")) {
            assertTrue(ligne.getBytes(StandardCharsets.UTF_8).length <= 75, ligne);
            assertFalse(ligne.contains("�"));
        }
        // déplier (CRLF + espace) restitue la ligne d'origine
        String deplie = ics.replace("\r\n ", "");
        assertTrue(deplie.contains("\r\nSUMMARY:" + titre + "\r\n"));
    }

    @Test
    void pliage_d_une_ligne_courte_inchange_et_suite_commence_par_une_espace() {
        assertEquals("SUMMARY:court", ExportIcs.plier("SUMMARY:court"));
        String plie = ExportIcs.plier("X".repeat(80));
        assertEquals("X".repeat(75) + "\r\n " + "X".repeat(5), plie);
    }

    @Test
    void agenda_vide_reste_un_calendrier_valide() {
        String ics = export.exporter(new AgendaCompte("a@b.fr"));
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertFalse(ics.contains("VEVENT"));
        assertEquals("agenda-pingmanager.ics", ExportIcs.nomFichier());
    }
}
