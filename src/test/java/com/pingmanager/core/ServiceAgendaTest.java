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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agenda du compte : dates personnelles, et ajout / retrait automatiques d'un
 * tournoi à l'inscription et à la désinscription complète (cahier des charges v0, §1 et §3).
 */
class ServiceAgendaTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 1, 17);
    private static final LocalDateTime PENDANT = LocalDateTime.of(2026, 12, 1, 10, 0);

    private final AtomicInteger compteur = new AtomicInteger();
    private final ServiceAgenda agendas = new ServiceAgenda(() -> "d" + compteur.incrementAndGet());
    private final ServiceInscriptionEnLigne enLigne;

    ServiceAgendaTest() {
        RegistreClubsMemoire clubs = new RegistreClubsMemoire();
        clubs.ajouter(new Club("07450115", "Orléans", "45", "Orléans", "c@d.fr"));
        enLigne = new ServiceInscriptionEnLigne(ServiceInscription.sansContrainteSuccession(),
                new EligibiliteTournoi(clubs), agendas);
    }

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Tournoi d'Orléans", "TT Orléans", JOUR);
        t.setNumeroAffiliationClub("07450115");
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
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

    // ------------------------------------------------------------------
    // Dates personnelles
    // ------------------------------------------------------------------

    @Test
    void date_personnelle_ajoutee_modifiee_puis_retiree() {
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        ResultatAgenda r = agendas.ajouterDate(agenda, "  Entraînement  ", JOUR, LocalTime.of(18, 30), "Salle 2");
        assertEquals(ResultatAgenda.Motif.AJOUTE, r.getMotif());
        EvenementAgenda e = r.getEvenement();
        assertEquals("d1", e.getId());
        assertEquals("Entraînement", e.getTitre());
        assertFalse(e.estAutomatique());

        ResultatAgenda m = agendas.modifierDate(agenda, "d1", "Stage", JOUR, JOUR.plusDays(2), null, null);
        assertEquals(ResultatAgenda.Motif.MODIFIE, m.getMotif());
        assertEquals("d1", m.getEvenement().getId(), "l'identifiant (UID) est conservé");
        assertTrue(m.getEvenement().estSurPlusieursJours());
        assertNull(m.getEvenement().getHeure());

        assertEquals(ResultatAgenda.Motif.RETIRE, agendas.retirerDate(agenda, "d1").getMotif());
        assertEquals(ResultatAgenda.Motif.INTROUVABLE, agendas.retirerDate(agenda, "d1").getMotif());
        assertTrue(agenda.estVide());
    }

    @Test
    void saisie_incomplete_refusee_avec_motif_precis() {
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        assertEquals(ResultatAgenda.Motif.TITRE_MANQUANT, agendas.ajouterDate(agenda, " ", JOUR, null, null).getMotif());
        assertEquals(ResultatAgenda.Motif.DATE_MANQUANTE, agendas.ajouterDate(agenda, "X", null, null, null).getMotif());
        assertEquals(ResultatAgenda.Motif.TITRE_TROP_LONG,
                agendas.ajouterDate(agenda, "x".repeat(ServiceAgenda.LONGUEUR_MAX_TITRE + 1), JOUR, null, null).getMotif());
        assertEquals(ResultatAgenda.Motif.NOTE_TROP_LONGUE,
                agendas.ajouterDate(agenda, "X", JOUR, null, "n".repeat(ServiceAgenda.LONGUEUR_MAX_NOTE + 1)).getMotif());
        assertEquals(ResultatAgenda.Motif.FIN_AVANT_DEBUT,
                agendas.ajouterDate(agenda, "X", JOUR, JOUR.minusDays(1), null, null).getMotif());
        assertTrue(agenda.estVide());
        assertEquals(ResultatAgenda.Motif.INTROUVABLE,
                agendas.modifierDate(agenda, "absent", "X", JOUR, null, null, null).getMotif());
    }

    // ------------------------------------------------------------------
    // Tournois : ajout et retrait automatiques
    // ------------------------------------------------------------------

    @Test
    void inscription_ajoute_automatiquement_le_tournoi_a_l_agenda() {
        Tournoi t = tournoi();
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        var b = enLigne.inscrire(t, fiche("1"), t.getTableaux(), PENDANT, agenda);
        assertTrue(b.estValide());

        EvenementAgenda e = agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).orElseThrow();
        assertTrue(e.estAutomatique());
        assertEquals("Tournoi d'Orléans", e.getTitre());
        assertEquals(JOUR, e.getDate());
        assertEquals(LocalTime.of(8, 30), e.getHeure(), "premier pointage parmi les tableaux du joueur");
        assertEquals("1 rue du Stade, 45000 Orléans", e.getLieu());
        assertTrue(e.getNote().contains("Tableaux : A, B"), e.getNote());
        assertEquals(1, agenda.taille());
    }

    @Test
    void inscription_refusee_ne_touche_pas_l_agenda() {
        Tournoi t = tournoi();
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        var b = enLigne.inscrire(t, fiche("1"), t.getTableaux(), LocalDateTime.of(2027, 1, 16, 0, 0), agenda);
        assertFalse(b.estValide());
        assertTrue(agenda.estVide());
    }

    @Test
    void liste_d_attente_garde_le_tournoi_dans_l_agenda() {
        Tournoi t = tournoi();
        Tableau a = t.getTableaux().get(0);
        enLigne.inscrire(t, fiche("9"), List.of(a), PENDANT); // occupe l'unique place
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT, agenda);
        EvenementAgenda e = agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).orElseThrow();
        assertTrue(e.getNote().contains("Liste d'attente : A"), e.getNote());
    }

    @Test
    void desinscription_partielle_met_a_jour_puis_complete_retire_le_tournoi() {
        Tournoi t = tournoi();
        Tableau a = t.getTableaux().get(0);
        Tableau b = t.getTableaux().get(1);
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        agendas.ajouterDate(agenda, "Entraînement", JOUR.minusDays(3), null, null);
        enLigne.inscrire(t, fiche("1"), t.getTableaux(), PENDANT, agenda);
        String id = agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).orElseThrow().getId();

        var partielle = enLigne.desinscrire(t, fiche("1"), List.of(a), agenda);
        assertTrue(partielle.estValide());
        assertFalse(partielle.desinscriptionComplete());
        assertEquals(ResultatAgenda.Motif.MODIFIE, partielle.agenda().getMotif());
        EvenementAgenda apres = agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).orElseThrow();
        assertEquals(id, apres.getId(), "même UID après mise à jour");
        assertEquals(LocalTime.of(12, 30), apres.getHeure());
        assertTrue(apres.getNote().startsWith("Tableau : B"), apres.getNote());

        var complete = enLigne.desinscrire(t, fiche("1"), List.of(b), agenda);
        assertTrue(complete.desinscriptionComplete());
        assertEquals(ResultatAgenda.Motif.RETIRE, complete.agenda().getMotif());
        assertTrue(agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).isEmpty());
        assertEquals(1, agenda.taille(), "la date personnelle reste");
    }

    @Test
    void desinscription_repeche_le_premier_de_la_liste_d_attente() {
        Tournoi t = tournoi();
        Tableau a = t.getTableaux().get(0);
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT);
        enLigne.inscrire(t, fiche("2"), List.of(a), PENDANT);
        var bilan = enLigne.desinscrire(t, fiche("1"), List.of(a), null);
        assertTrue(bilan.estValide());
        assertNull(bilan.agenda());
        assertEquals("2", bilan.repeches().get(a).getNumeroLicence());
    }

    @Test
    void desinscription_refusee_sans_rien_modifier() {
        Tournoi t = tournoi();
        Tableau a = t.getTableaux().get(0);
        Tableau b = t.getTableaux().get(1);
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        enLigne.inscrire(t, fiche("1"), List.of(a), PENDANT, agenda);

        assertNotNull(enLigne.desinscrire(t, null, List.of(a), agenda).refus());
        assertNotNull(enLigne.desinscrire(t, fiche("1"), List.of(), agenda).refus());
        assertTrue(enLigne.desinscrire(t, fiche("1"), List.of(a, b), agenda).refus().contains("tableau B"),
                "tout ou rien : pas inscrit en B");
        assertEquals(1, a.getInscrits().size());

        a.ouvrirPointage();
        var r = enLigne.desinscrire(t, fiche("1"), List.of(a), agenda);
        assertTrue(r.refus().contains("juge-arbitre"), r.refus());
        assertEquals(1, a.getInscrits().size());
        assertTrue(agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).isPresent());
    }

    @Test
    void date_de_tournoi_non_modifiable_ni_retirable_a_la_main() {
        Tournoi t = tournoi();
        AgendaCompte agenda = new AgendaCompte("a@b.fr");
        enLigne.inscrire(t, fiche("1"), t.getTableaux(), PENDANT, agenda);
        String id = agenda.dateDuTournoi(ServiceAgenda.cleTournoi(t)).orElseThrow().getId();
        assertEquals(ResultatAgenda.Motif.DATE_AUTOMATIQUE, agendas.retirerDate(agenda, id).getMotif());
        assertEquals(ResultatAgenda.Motif.DATE_AUTOMATIQUE,
                agendas.modifierDate(agenda, id, "X", JOUR, null, null, null).getMotif());
        assertEquals(1, agenda.taille());
    }

    @Test
    void synchroniser_deux_fois_ne_change_rien_et_tournoi_sur_deux_jours_en_journees_entieres() {
        Tournoi t = new Tournoi("National B", "Brunoy", List.of(LocalDate.of(2027, 4, 10), LocalDate.of(2027, 4, 11)));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(4).toutesSeries().build());
        a.setHoraires(new HorairesTableau(LocalDateTime.of(2027, 4, 10, 8, 0), null, null));
        t.ajouterTableau(a);
        a.inscrire(JoueurDepuisLicence.creer(fiche("1"), t));
        AgendaCompte agenda = new AgendaCompte("a@b.fr");

        assertEquals(ResultatAgenda.Motif.AJOUTE, agendas.synchroniserTournoi(agenda, t, "1").getMotif());
        assertEquals(ResultatAgenda.Motif.INCHANGE, agendas.synchroniserTournoi(agenda, t, "1").getMotif());
        EvenementAgenda e = agenda.evenements().get(0);
        assertEquals(LocalDate.of(2027, 4, 11), e.getDateFin());
        assertNull(e.getHeure(), "plusieurs jours : journées entières");
    }

    @Test
    void cle_du_tournoi_par_homologation_sinon_nom_date_et_club() {
        Tournoi t = tournoi();
        assertEquals("tournoi:Tournoi d'Orléans|2027-01-17|TT Orléans", ServiceAgenda.cleTournoi(t));
        assertEquals(ServiceAgenda.identifiantTournoi("k"), ServiceAgenda.identifiantTournoi("k"),
                "identifiant stable d'une session à l'autre");
    }
}
