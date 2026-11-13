package com.pingmanager.persistence;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import com.pingmanager.persistence.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Persistance de l'agenda des comptes dans le dépôt applicatif, avec relecture des anciens fichiers. */
class AgendaRoundTripTest {

    @Test
    void agenda_sauvegarde_puis_relu_a_l_identique() throws Exception {
        DepotApplication depot = new DepotApplication();
        AgendaCompte a = depot.agendaDe("Quentin@Exemple.fr");
        a.enregistrer(EvenementAgenda.builder().id("p1").titre("Entraînement").date(LocalDate.of(2027, 1, 12))
                .heure(LocalTime.of(18, 30)).note("Salle 2\nPrévoir; des balles, ok").construire());
        a.enregistrer(EvenementAgenda.builder().id("tournoi-x").titre("National B").date(LocalDate.of(2027, 4, 10))
                .dateFin(LocalDate.of(2027, 4, 11)).lieu("Brunoy").note("Tableau : A")
                .origine(OrigineEvenement.TOURNOI).cleTournoi("homologation:123").construire());
        depot.agendaDe("vide@exemple.fr"); // agenda vide : non écrit

        Path dossier = Files.createTempDirectory("agenda");
        Path fichier = dossier.resolve(DepotApplication.NOM_FICHIER);
        depot.sauvegarder(fichier);
        DepotApplication relu = DepotApplication.charger(fichier);

        assertEquals(1, relu.tousLesAgendas().size());
        AgendaCompte r = relu.agendaDe("quentin@exemple.fr");
        assertEquals(a.evenements(), r.evenements());
        assertTrue(r.dateDuTournoi("homologation:123").orElseThrow().estAutomatique());
    }

    @Test
    void ancien_fichier_sans_agenda_relu_avec_des_agendas_vides() {
        String ancien = "{\"version\":1,\"kind\":\"application\",\"clubs\":[],\"comptes\":[],"
                + "\"comptesUtilisateurs\":[],\"acces\":[],\"recents\":[]}";
        DepotApplication depot = DepotApplication.lire(JsonParser.analyser(ancien));
        assertTrue(depot.tousLesAgendas().isEmpty());
        assertTrue(depot.agendaDe("a@b.fr").estVide());
    }

    @Test
    void origine_inconnue_ou_tournoi_sans_cle_relu_comme_date_personnelle() {
        String json = "{\"version\":1,\"agendas\":[{\"email\":\"a@b.fr\",\"evenements\":["
                + "{\"id\":\"1\",\"titre\":\"X\",\"date\":\"2027-01-01\",\"origine\":\"AUTRE\"},"
                + "{\"id\":\"2\",\"titre\":\"Y\",\"date\":\"2027-01-02\",\"origine\":\"TOURNOI\"},"
                + "{\"id\":\"3\",\"titre\":\"Z\",\"date\":\"2027-01-03\"}]}]}";
        AgendaCompte a = DepotApplication.lire(JsonParser.analyser(json)).agendaDe("a@b.fr");
        assertEquals(3, a.taille());
        assertTrue(a.evenements().stream().noneMatch(EvenementAgenda::estAutomatique));
    }

    @Test
    void date_federale_sauvegardee_puis_relue_avec_son_origine() throws Exception {
        DepotApplication depot = new DepotApplication();
        AgendaCompte a = depot.agendaDe("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("federal-cf-t1").titre("Critérium fédéral : 1er tour")
                .date(LocalDate.of(2026, 10, 9)).dateFin(LocalDate.of(2026, 10, 11))
                .note("Calendrier fédéral : à vérifier.").origine(OrigineEvenement.FEDERAL).construire());
        Path fichier = Files.createTempDirectory("agenda-federal").resolve(DepotApplication.NOM_FICHIER);
        depot.sauvegarder(fichier);
        AgendaCompte r = DepotApplication.charger(fichier).agendaDe("a@b.fr");
        assertEquals(a.evenements(), r.evenements());
        assertEquals(OrigineEvenement.FEDERAL, r.trouver("federal-cf-t1").orElseThrow().getOrigine());
    }

    @Test
    void ancien_fichier_sans_origine_federal_relu_a_l_identique() {
        // fichier écrit avant B045 : seules PERSONNEL et TOURNOI existaient
        String ancien = "{\"version\":1,\"agendas\":[{\"email\":\"a@b.fr\",\"evenements\":["
                + "{\"id\":\"1\",\"titre\":\"X\",\"date\":\"2027-01-01\",\"origine\":\"PERSONNEL\"},"
                + "{\"id\":\"2\",\"titre\":\"Y\",\"date\":\"2027-01-02\",\"origine\":\"TOURNOI\","
                + "\"cleTournoi\":\"k\"}]}]}";
        AgendaCompte a = DepotApplication.lire(JsonParser.analyser(ancien)).agendaDe("a@b.fr");
        assertEquals(OrigineEvenement.PERSONNEL, a.trouver("1").orElseThrow().getOrigine());
        assertEquals(OrigineEvenement.TOURNOI, a.trouver("2").orElseThrow().getOrigine());
    }
}
