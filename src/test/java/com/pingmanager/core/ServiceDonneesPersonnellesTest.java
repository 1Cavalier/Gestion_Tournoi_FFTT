package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 21 (RGPD) : aperçu de purge et effacement d'un joueur. */
class ServiceDonneesPersonnellesTest {

    private final ServiceDonneesPersonnelles service = new ServiceDonneesPersonnelles(
            () -> Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void apercu_liste_les_tournois_termines_depuis_plus_de_n_ans_sans_rien_supprimer() {
        Map<String, Tournoi> tournois = new LinkedHashMap<>();
        tournois.put("vieux", new Tournoi("Vieux", "Club", LocalDate.of(2023, 11, 12)));
        tournois.put("limite", new Tournoi("Limite", "Club", LocalDate.of(2023, 11, 13)));
        tournois.put("recent", new Tournoi("Récent", "Club", LocalDate.of(2025, 5, 1)));
        tournois.put("tresVieux", new Tournoi("Très vieux", "Club", LocalDate.of(2020, 1, 1)));
        List<ServiceDonneesPersonnelles.TournoiAPurger<String>> a = service.apercuPurge(tournois, 3);
        assertEquals(List.of("tresVieux", "vieux"), a.stream().map(ServiceDonneesPersonnelles.TournoiAPurger::cle).toList());
        assertEquals(4, tournois.size(), "rien n'est supprimé");
        assertEquals(1, service.apercuPurge(tournois, 5).size(), "durée configurable");
        assertThrows(IllegalArgumentException.class, () -> service.apercuPurge(tournois, 0));
    }

    @Test
    void tournoi_sur_deux_jours_juge_sur_son_dernier_jour() {
        Tournoi t = new Tournoi("Deux jours", "Club", List.of(LocalDate.of(2023, 11, 11), LocalDate.of(2023, 11, 14)));
        assertFalse(service.estAPurger(t, 3));
    }

    @Test
    void effacement_anonymise_nom_prenom_et_sms_mais_garde_licence_et_donnees_sportives() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 6, 1));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(8).build());
        Tableau b = new Tableau("B", ConfigurationTableau.builder().placesMax(8).build());
        t.ajouterTableau(a);
        t.ajouterTableau(b);
        Joueur cible = new Joueur("9990001", "DUPONT", "Marie", Sexe.FEMME, "Club", 1234);
        Joueur autre = new Joueur("9990002", "MARTIN", "Paul", Sexe.HOMME, "Club", 900);
        a.inscrire(cible);
        a.inscrire(autre);
        b.inscrire(cible);
        ConvocationsSms sms = t.getConvocationsSms();
        sms.enregistrerContact(new ContactSms("9990001", "+33600000000", LocalDateTime.of(2026, 5, 1, 10, 0)));
        sms.enregistrerContact(new ContactSms("9990002", "+33611111111", LocalDateTime.of(2026, 5, 1, 10, 0)));
        sms.journaliser(new MessageSms(LocalDateTime.of(2026, 6, 1, 9, 0), TypeConvocation.values()[0], "9990001",
                "+33600000000", "Marie DUPONT, table 3", StatutEnvoiSms.values()[0], ""));

        ServiceDonneesPersonnelles.BilanAnonymisation bilan = service.anonymiser(t, " 9990001 ");

        assertTrue(bilan.aModifie());
        assertEquals(1, bilan.identitesEffacees());
        assertEquals(2, bilan.elementsSmsEffaces(), "numéro et message");
        assertEquals(ServiceDonneesPersonnelles.NOM_ANONYME, cible.getNom());
        assertEquals(ServiceDonneesPersonnelles.PRENOM_ANONYME, cible.getPrenom());
        assertEquals("9990001", cible.getNumeroLicence(), "licence gardée (obligation fédérale)");
        assertEquals(1234, cible.getPoints());
        assertNull(sms.contact("9990001"));
        assertEquals("", sms.getJournal().get(0).destinataire());
        assertEquals(ConvocationsSms.TEXTE_EFFACE, sms.getJournal().get(0).texte());
        assertEquals("MARTIN", autre.getNom(), "les autres joueurs ne changent pas");
        assertEquals("+33611111111", sms.contact("9990002").numero());

        assertFalse(service.anonymiser(t, "9990001").aModifie(), "déjà effacé : rien à refaire");
    }
}
