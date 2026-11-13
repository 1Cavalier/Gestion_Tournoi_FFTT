package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.Club;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Vérification indépendante de l'agenda : désinscription, liste d'attente, doublons, export .ics. */
class AgendaVerificationTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 1, 17);
    private static final LocalDateTime PENDANT = LocalDateTime.of(2026, 12, 1, 10, 0);
    private final ServiceAgenda agendas = ServiceAgenda.parDefaut();
    private final ServiceInscriptionEnLigne enLigne;

    AgendaVerificationTest() {
        RegistreClubsMemoire clubs = new RegistreClubsMemoire();
        clubs.ajouter(new Club("07450115", "Orléans", "45", "Orléans", "c@d.fr"));
        enLigne = new ServiceInscriptionEnLigne(ServiceInscription.sansContrainteSuccession(),
                new EligibiliteTournoi(clubs), agendas);
    }

    private static Tournoi tournoi(String homologation) {
        Tournoi t = new Tournoi("Tournoi d'Orléans", "TT Orléans", JOUR);
        t.setNumeroAffiliationClub("07450115");
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
        t.setNumeroHomologation(homologation);
        t.setAdresse(AdresseTournoi.de("1 rue du Stade", "45000", "Orléans"));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(1).tailleMaxListeAttente(2)
                .pointMax(1000).tarif(new TarifInscription(8, 10)).build());
        a.setHoraires(new HorairesTableau(JOUR.atTime(8, 30), null, JOUR.atTime(9, 0)));
        Tableau b = new Tableau("B", ConfigurationTableau.builder().placesMax(10).toutesSeries()
                .tarif(new TarifInscription(10, 12)).build());
        b.setHoraires(new HorairesTableau(JOUR.atTime(12, 30), null, JOUR.atTime(13, 0)));
        t.ajouterTableau(a);
        t.ajouterTableau(b);
        t.setInfosReglement(InfosReglement.builder().ouvertureInscriptions(LocalDateTime.of(2026, 11, 1, 0, 0))
                .clotureInscriptions(LocalDateTime.of(2027, 1, 15, 20, 0)).build());
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private static FicheLicence fiche(String licence) {
        return new FicheLicence.Builder(licence, "NOM" + licence, "Prénom").sexe(Sexe.HOMME)
                .club("07450115", "Orléans").points(923).dateNaissance(LocalDate.of(2002, 3, 15))
                .typeLicence("Compétition").build();
    }

    @Test
    void desinscription_sans_agenda_fonctionne_et_agenda_null_tolere() {
        Tournoi t = tournoi("H1");
        enLigne.inscrire(t, fiche("1"), t.getTableaux(), PENDANT, null);
        var b = enLigne.desinscrire(t, fiche("1"), t.getTableaux(), null);
        assertTrue(b.estValide());
        assertTrue(b.desinscriptionComplete());
        assertNull(b.agenda());
    }

    @Test
    void desinscription_licence_non_inscrite_ou_fiche_nulle_refusee() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag = new AgendaCompte("a@b.fr");
        assertFalse(enLigne.desinscrire(t, null, t.getTableaux(), ag).estValide());
        assertFalse(enLigne.desinscrire(t, fiche("9"), t.getTableaux(), ag).estValide());
        assertFalse(enLigne.desinscrire(t, fiche("9"), List.of(), ag).estValide());
        assertFalse(enLigne.desinscrire(t, fiche("9"), null, ag).estValide());
    }

    @Test
    void desinscription_tout_ou_rien_si_un_seul_tableau_invalide() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag = new AgendaCompte("a@b.fr");
        Tableau a = t.getTableaux().get(0);
        Tableau b = t.getTableaux().get(1);
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT, ag);
        var r = enLigne.desinscrire(t, fiche("1"), List.of(a, b), ag);
        assertFalse(r.estValide());
        assertEquals(1, a.getInscrits().size(), "rien n'a bougé");
        assertEquals(1, ag.taille());
    }

    @Test
    void desinscription_avec_tableau_en_double_ne_plante_pas() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag = new AgendaCompte("a@b.fr");
        Tableau a = t.getTableaux().get(0);
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT, ag);
        var r = assertDoesNotThrow(() -> enLigne.desinscrire(t, fiche("1"), List.of(a, a), ag));
        assertTrue(ag.estVide(), "désinscrit complètement : retiré de l'agenda");
        assertNotNull(r);
    }

    @Test
    void repeche_depuis_l_attente_garde_son_propre_agenda_et_celui_du_partant_est_retire() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag1 = new AgendaCompte("1@b.fr");
        AgendaCompte ag2 = new AgendaCompte("2@b.fr");
        Tableau a = t.getTableaux().get(0);
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT, ag1);
        enLigne.inscrire(t, fiche("2"), List.of(a), PENDANT, ag2);
        assertEquals(1, a.getFileAttente().size());
        assertTrue(ag2.evenements().get(0).getNote().contains("Liste d'attente"));
        var r = enLigne.desinscrire(t, fiche("1"), List.of(a), ag1);
        assertEquals(ResultatAgenda.Motif.RETIRE, r.agenda().getMotif());
        assertTrue(ag1.estVide());
        assertEquals(1, r.repeches().size());
        // l'agenda de la personne repêchée se resynchronise sur demande
        agendas.synchroniserTournoi(ag2, t, "2");
        assertFalse(ag2.evenements().get(0).getNote().contains("Liste d'attente"));
    }

    @Test
    void sortie_de_la_liste_d_attente_seule_retire_le_tournoi_de_l_agenda() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag2 = new AgendaCompte("2@b.fr");
        Tableau a = t.getTableaux().get(0);
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT, new AgendaCompte("1@b.fr"));
        enLigne.inscrire(t, fiche("2"), List.of(a), PENDANT, ag2);
        var r = enLigne.desinscrire(t, fiche("2"), List.of(a), ag2);
        assertTrue(r.estValide());
        assertTrue(r.desinscriptionComplete());
        assertTrue(ag2.estVide());
        assertEquals(1, a.getInscrits().size());
    }

    @Test
    void reinscription_apres_desinscription_reutilise_le_meme_identifiant() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag = new AgendaCompte("a@b.fr");
        enLigne.inscrire(t, fiche("1"), List.of(t.getTableaux().get(1)), PENDANT, ag);
        String id = ag.evenements().get(0).getId();
        enLigne.desinscrire(t, fiche("1"), List.of(t.getTableaux().get(1)), ag);
        enLigne.inscrire(t, fiche("1"), List.of(t.getTableaux().get(1)), PENDANT, ag);
        assertEquals(1, ag.taille());
        assertEquals(id, ag.evenements().get(0).getId());
    }

    @Test
    void deux_tournois_homologues_differents_coexistent_et_meme_tournoi_inscrit_deux_fois_ne_double_pas() {
        Tournoi t1 = tournoi("H1");
        Tournoi t2 = tournoi("H2");
        AgendaCompte ag = new AgendaCompte("a@b.fr");
        enLigne.inscrire(t1, fiche("1"), List.of(t1.getTableaux().get(1)), PENDANT, ag);
        enLigne.inscrire(t2, fiche("1"), List.of(t2.getTableaux().get(1)), PENDANT, ag);
        enLigne.inscrire(t1, fiche("1"), List.of(t1.getTableaux().get(0)), PENDANT, ag);
        assertEquals(2, ag.taille());
        enLigne.desinscrire(t1, fiche("1"), t1.getTableaux(), ag);
        assertEquals(1, ag.taille());
    }

    @Test
    void synchroniser_avec_licence_nulle_retire_ou_ne_change_rien() {
        Tournoi t = tournoi("H1");
        AgendaCompte ag = new AgendaCompte("a@b.fr");
        assertEquals(ResultatAgenda.Motif.INCHANGE, agendas.synchroniserTournoi(ag, t, null).getMotif());
        assertTrue(ag.estVide());
    }

    // ------------------------------------------------------------------ .ics

    private static String ics(EvenementAgenda... evts) {
        AgendaCompte a = new AgendaCompte("a@b.fr");
        for (EvenementAgenda e : evts) {
            a.enregistrer(e);
        }
        return new ExportIcs(() -> Instant.parse("2027-03-04T05:06:07Z")).exporter(a);
    }

    @Test
    void ics_dtstamp_en_utc_et_journee_entiere_fin_exclusive_a_la_fin_de_mois_et_d_annee() {
        String s = ics(EvenementAgenda.builder().id("x").titre("Fin d'année").date(LocalDate.of(2027, 12, 30))
                .dateFin(LocalDate.of(2027, 12, 31)).construire());
        assertTrue(s.contains("DTSTAMP:20270304T050607Z\r\n"));
        assertTrue(s.contains("DTSTART;VALUE=DATE:20271230\r\n"));
        assertTrue(s.contains("DTEND;VALUE=DATE:20280101\r\n"), s);
    }

    @Test
    void ics_evenement_d_un_jour_sans_heure_dtend_est_le_lendemain() {
        String s = ics(EvenementAgenda.builder().id("x").titre("T").date(LocalDate.of(2028, 2, 28)).construire());
        assertTrue(s.contains("DTEND;VALUE=DATE:20280229\r\n"), s);
    }

    @Test
    void ics_evenement_avec_heure_multi_jours_a_une_fin_apres_le_dernier_jour() {
        String s = ics(EvenementAgenda.builder().id("x").titre("T").date(LocalDate.of(2027, 1, 1))
                .dateFin(LocalDate.of(2027, 1, 2)).heure(LocalTime.of(9, 5)).construire());
        assertTrue(s.contains("DTSTART:20270101T090500\r\n"));
        assertTrue(s.contains("DTEND:20270103T000000\r\n"), s);
    }

    @Test
    void ics_pliage_chaque_ligne_physique_au_plus_75_octets_avec_caracteres_de_2_3_et_4_octets() {
        for (String motif : new String[]{"é", "€", "🏓", "a", "\\", "é€🏓,"}) {
            String titre = motif.repeat(80);
            String s = ics(EvenementAgenda.builder().id("x").titre(titre).note(titre).lieu(titre)
                    .date(JOUR).construire());
            for (String ligne : s.split("\r\n", -1)) {
                assertTrue(ligne.getBytes(StandardCharsets.UTF_8).length <= 75, "ligne trop longue : " + ligne);
            }
            // dépliage = retrait de CRLF + espace : doit redonner le titre échappé
            String deplie = s.replace("\r\n ", "");
            assertTrue(deplie.contains("SUMMARY:" + ExportIcs.echapper(titre) + "\r\n"));
            assertFalse(s.contains("�"));
        }
    }

    @Test
    void ics_echappement_cas_limites() {
        assertEquals("", ExportIcs.echapper(null));
        assertEquals("", ExportIcs.echapper(""));
        assertEquals("a\\nb\\nc\\nd", ExportIcs.echapper("a\r\nb\rc\nd"));
        assertEquals("\\\\" + "\\;" + "\\,", ExportIcs.echapper("\\;,"));
        assertEquals("\\\\n", ExportIcs.echapper("\\n"), "un backslash suivi de n littéral reste distinct d'un saut de ligne");
    }

    @Test
    void ics_titre_et_note_avec_caracteres_speciaux_ne_creent_pas_de_ligne_parasite() {
        String s = ics(EvenementAgenda.builder().id("x").titre("A\r\nATTENDEE:evil").note("l1\nl2;l3,l4\\")
                .date(JOUR).construire());
        assertFalse(s.contains("\r\nATTENDEE"), s);
        assertTrue(s.contains("DESCRIPTION:l1\\nl2\\;l3\\,l4\\\\\r\n"), s);
    }

    @Test
    void ics_dernier_octet_est_crlf_et_aucun_lf_isole() {
        String s = ics(EvenementAgenda.builder().id("x").titre("T").note("a\nb").date(JOUR).construire());
        assertTrue(s.endsWith("END:VCALENDAR\r\n"));
        assertFalse(s.replace("\r\n", "").contains("\n"));
    }

    @Test
    void ics_plier_ligne_vide_et_exactement_75_octets() {
        assertEquals("", ExportIcs.plier(""));
        String exact = "a".repeat(75);
        assertEquals(exact, ExportIcs.plier(exact));
        assertEquals("a".repeat(75) + "\r\n a", ExportIcs.plier("a".repeat(76)));
    }

    @Test
    void ics_horloge_nulle_refusee_et_agenda_nul_refuse() {
        assertThrows(NullPointerException.class, () -> new ExportIcs(null));
        assertThrows(NullPointerException.class, () -> new ExportIcs(Instant::now).exporter(null));
    }
}
