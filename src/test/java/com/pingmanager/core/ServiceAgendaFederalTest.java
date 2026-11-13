package com.pingmanager.core;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.DateFederale;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import com.pingmanager.model.StatutDateFederale;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Copie des dates du calendrier fédéral dans l'agenda (B045) : à la demande, sans doublon. */
class ServiceAgendaFederalTest {

    private final ServiceAgenda service = new ServiceAgenda(() -> "id-perso");

    static DateFederale date(String id, int jour, StatutDateFederale statut) {
        return DateFederale.builder().id(id).categorie("Critérium fédéral").titre("Tour " + id)
                .date(LocalDate.of(2026, 11, jour)).dateFin(LocalDate.of(2026, 11, jour + 2)).statut(statut)
                .source("https://www.fftt.com/x").reluLe(LocalDate.of(2026, 11, 13)).note("Échelon national")
                .construire();
    }

    @Test
    void ajouter_a_mon_agenda_copie_la_date_en_origine_federal_modifiable() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        DateFederale d = date("t2", 13, StatutDateFederale.A_VERIFIER);
        assertFalse(ServiceAgenda.estDansAgenda(a, d));

        ResultatAgenda r = service.ajouterDateFederale(a, d);
        assertEquals(ResultatAgenda.Motif.AJOUTE, r.getMotif());
        EvenementAgenda e = r.getEvenement();
        assertEquals("federal-t2", e.getId());
        assertEquals(OrigineEvenement.FEDERAL, e.getOrigine());
        assertFalse(e.estAutomatique());
        assertEquals(LocalDate.of(2026, 11, 15), e.getDateFin());
        assertTrue(e.getNote().contains("à vérifier"), e.getNote());
        assertTrue(e.getNote().contains("Source : https://www.fftt.com/x (relue le 13/11/2026)"), e.getNote());
        assertTrue(ServiceAgenda.estDansAgenda(a, d));

        // modifiable et retirable comme une date personnelle, en gardant son origine
        ResultatAgenda m = service.modifierDate(a, e.getId(), "Tour 2 (à Tours)", e.getDate(), e.getDateFin(),
                null, null);
        assertEquals(ResultatAgenda.Motif.MODIFIE, m.getMotif());
        assertEquals(OrigineEvenement.FEDERAL, m.getEvenement().getOrigine());
        assertEquals(ResultatAgenda.Motif.RETIRE, service.retirerDate(a, e.getId()).getMotif());
        assertFalse(ServiceAgenda.estDansAgenda(a, d));
    }

    @Test
    void une_date_federale_deja_ajoutee_ne_se_duplique_pas() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        DateFederale d = date("t1", 9, StatutDateFederale.CONFIRMEE);
        service.ajouterDateFederale(a, d);
        ResultatAgenda r = service.ajouterDateFederale(a, d);
        assertEquals(ResultatAgenda.Motif.DEJA_DANS_L_AGENDA, r.getMotif());
        assertTrue(r.estAccepte());
        assertEquals(1, a.taille());
    }

    @Test
    void tout_ajouter_ne_copie_que_les_dates_absentes() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        List<DateFederale> cat = List.of(date("t1", 9, StatutDateFederale.CONFIRMEE),
                date("t2", 13, StatutDateFederale.A_VERIFIER), date("t3", 20, StatutDateFederale.A_VERIFIER));
        service.ajouterDateFederale(a, cat.get(1));
        assertEquals(2, service.ajouterDatesFederales(a, cat));
        assertEquals(3, a.taille());
        assertEquals(0, service.ajouterDatesFederales(a, cat));
        assertEquals(3, a.taille());
    }

    @Test
    void rien_n_est_ajoute_automatiquement_a_la_synchronisation() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        assertTrue(a.estVide(), "aucune date fédérale sans action du titulaire");
        assertEquals("federal-t9", ServiceAgenda.identifiantFederal(date("t9", 1, StatutDateFederale.CONFIRMEE)));
    }

    @Test
    void export_ics_classe_la_copie_en_categorie_federal() {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        service.ajouterDateFederale(a, date("t1", 9, StatutDateFederale.CONFIRMEE));
        String ics = new ExportIcs(() -> java.time.Instant.parse("2026-11-13T10:00:00Z")).exporter(a);
        assertTrue(ics.contains("\r\nCATEGORIES:Fédéral\r\n"), ics);
        assertTrue(ics.contains("\r\nUID:federal-t1@pingmanager\r\n"));
        assertFalse(new ExportIcs(() -> java.time.Instant.EPOCH).exporterTournois(a).contains("VEVENT"));
    }
}
