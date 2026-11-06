package com.pingmanager.persistence;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Cas limites de la persistance de l'agenda : caractères spéciaux, champs vides, plusieurs comptes. */
class AgendaRoundTripLimitesTest {

    @Test
    void textes_speciaux_et_plusieurs_comptes_survivent_a_la_sauvegarde() throws Exception {
        DepotApplication depot = new DepotApplication();
        String bizarre = "« Tournoi » \"guillemets\" \\ antislash\n\ttab é€🏓";
        depot.agendaDe("a@b.fr").enregistrer(EvenementAgenda.builder().id("1").titre(bizarre).note(bizarre)
                .lieu(bizarre).date(LocalDate.of(2027, 2, 28)).dateFin(LocalDate.of(2027, 3, 1))
                .heure(LocalTime.of(0, 0)).construire());
        depot.agendaDe("c@d.fr").enregistrer(EvenementAgenda.builder().id("t").titre("T").date(LocalDate.of(2027, 1, 1))
                .origine(OrigineEvenement.TOURNOI).cleTournoi("tournoi:X|2027-01-01|Club").construire());
        Path f = Files.createTempDirectory("ag").resolve(DepotApplication.NOM_FICHIER);
        depot.sauvegarder(f);
        DepotApplication relu = DepotApplication.charger(f);
        assertEquals(2, relu.tousLesAgendas().size());
        assertEquals(depot.agendaDe("a@b.fr").evenements(), relu.agendaDe("a@b.fr").evenements());
        assertEquals(depot.agendaDe("c@d.fr").evenements(), relu.agendaDe("c@d.fr").evenements());
        // deuxième sauvegarde identique : stable
        Path g = f.resolveSibling("bis.json");
        relu.sauvegarder(g);
        assertEquals(Files.readString(f), Files.readString(g));
    }

    @Test
    void agenda_devenu_vide_apres_retrait_n_est_plus_ecrit_et_relu_vide() throws Exception {
        DepotApplication depot = new DepotApplication();
        AgendaCompte a = depot.agendaDe("a@b.fr");
        a.enregistrer(EvenementAgenda.builder().id("1").titre("T").date(LocalDate.of(2027, 1, 1)).construire());
        a.retirer("1");
        Path f = Files.createTempDirectory("ag").resolve(DepotApplication.NOM_FICHIER);
        depot.sauvegarder(f);
        assertTrue(DepotApplication.charger(f).agendaDe("a@b.fr").estVide());
    }
}
