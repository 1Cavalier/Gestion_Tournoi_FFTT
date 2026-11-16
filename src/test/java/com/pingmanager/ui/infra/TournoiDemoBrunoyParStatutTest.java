package com.pingmanager.ui.infra;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.ServiceTournoiEnCours;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** B047 : la démo ne contient que le National B de Brunoy, une copie par statut. */
class TournoiDemoBrunoyParStatutTest {

    @TempDir
    Path dossier;

    private static boolean sansNumero(Tournoi t) {
        return t.getNumeroHomologation() == null || t.getNumeroHomologation().isBlank();
    }

    @Test
    void copie_en_demande_dossier_complet_sans_numero_ni_inscrit() {
        Tournoi t = TournoiDemoBrunoyParStatut.demandeHomologation();
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, t.getStatut());
        assertEquals("Tournoi National B de Brunoy 2027 — demande d'homologation", t.getNom());
        assertTrue(sansNumero(t), "le numéro n'est délivré qu'à l'homologation");
        assertEquals(List.of(), new ServiceCycleTournoi().elementsManquantsPourHomologation(t), "dossier complet");
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(t));
    }

    @Test
    void copie_homologuee_numero_fictif_sans_inscrit_avant_publication() {
        Tournoi t = TournoiDemoBrunoyParStatut.homologue();
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
        assertEquals("Tournoi National B de Brunoy 2027 — homologation validée", t.getNom());
        assertEquals(TournoisDemo.HOMOLOGATION_BRUNOY_DEMO, t.getNumeroHomologation());
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(t), "inscriptions ouvertes à la publication seulement");
    }

    @Test
    void copie_publiee_remplie_de_joueurs_fictifs_aucun_tableau_commence() {
        Tournoi t = TournoiDemoBrunoyParStatut.publie(DonneesDemo.JOUEURS_FICTIFS);
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
        assertEquals("Tournoi National B de Brunoy 2027 — publié", t.getNom());
        assertEquals(TournoisDemo.HOMOLOGATION_BRUNOY_DEMO, t.getNumeroHomologation());
        assertTrue(RemplissageTableaux.nombreDeParticipants(t) > 200);
        assertTrue(t.getTableaux().stream().allMatch(tab -> !tab.getInscrits().isEmpty()), "chaque tableau a des inscrits");
        assertTrue(t.getTableaux().stream().anyMatch(tab -> !tab.getFileAttente().isEmpty()), "des listes d'attente");
        assertTrue(t.getTableaux().stream().allMatch(tab -> tab.getStatut() == StatutTableau.INSCRIPTION));
        assertTrue(t.getTableaux().stream().flatMap(tab -> tab.getInscrits().stream())
                .allMatch(j -> j.getNumeroLicence().startsWith("99")), "joueurs fictifs seulement");
    }

    @Test
    void les_copies_sont_le_meme_tournoi_que_le_brouillon_sans_personne_reelle() {
        Tournoi original = TournoiDemoBrunoy.construire();
        List<Tournoi> copies = List.of(TournoiDemoBrunoyParStatut.demandeHomologation(),
                TournoiDemoBrunoyParStatut.homologue(),
                TournoiDemoBrunoyParStatut.publie(DonneesDemo.JOUEURS_FICTIFS));
        List<String> tableaux = original.getTableaux().stream().map(Tableau::getNom).toList();
        for (Tournoi c : copies) {
            assertEquals(original.getJours(), c.getJours(), c.getNom());
            assertEquals(tableaux, c.getTableaux().stream().map(Tableau::getNom).toList(), c.getNom());
            assertEquals(original.getClubOrganisateur(), c.getClubOrganisateur());
            assertEquals(original.getNumeroAffiliationClub(), c.getNumeroAffiliationClub());
            assertTrue(c.getNom().startsWith(TournoiDemoBrunoy.NOM + " — "), c.getNom());
            assertEquals(List.of(TournoiDemoBrunoyJourJ.JA_FICTIF, TournoiDemoBrunoyJourJ.ADJOINT_FICTIF),
                    c.getJugesArbitres(), c.getNom());
            assertTrue(c.getPaiement().getLien().contains("exemple-demo"), c.getNom());
        }
        assertEquals(StatutTournoi.BROUILLON, original.getStatut(), "l'original reste en brouillon");
    }

    @Test
    void la_demo_contient_cinq_tournois_un_par_statut_tous_de_brunoy() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());

        assertEquals(5, ctx.fichiersTournois().size(), ctx.fichiersTournois().toString());
        Tournoi brouillon = ctx.chargerTournoi(ctx.fichierTournoiDemo());
        Tournoi demande = ctx.chargerTournoi(ctx.fichierTournoiDemandeDemo());
        Tournoi homologue = ctx.chargerTournoi(ctx.fichierTournoiHomologueDemo());
        Tournoi publie = ctx.chargerTournoi(ctx.fichierTournoiPublieDemo());
        Tournoi jourJ = ctx.chargerTournoi(ctx.fichierTournoiJourJDemo());

        assertEquals(StatutTournoi.BROUILLON, brouillon.getStatut());
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, demande.getStatut());
        assertEquals(StatutTournoi.HOMOLOGUE, homologue.getStatut());
        assertEquals(StatutTournoi.PUBLIE, publie.getStatut());
        assertEquals(StatutTournoi.PUBLIE, jourJ.getStatut());
        assertTrue(ServiceTournoiEnCours.aUnTableauCommenceNonTermine(jourJ), "la copie jour J se joue");
        assertFalse(ServiceTournoiEnCours.aUnTableauCommenceNonTermine(publie));

        // Les quatre statuts du cycle de vie sont tous représentés.
        Set<StatutTournoi> statuts = new LinkedHashSet<>();
        Set<String> noms = new LinkedHashSet<>();
        for (ContexteApplication.TournoiFichier tf : ctx.tousLesTournois()) {
            statuts.add(tf.tournoi().getStatut());
            noms.add(tf.tournoi().getNom());
            assertTrue(ctx.estTournoiDuCompte(tf.tournoi()), tf.tournoi().getNom());
            assertTrue(tf.tournoi().getNom().startsWith(TournoiDemoBrunoy.NOM), tf.tournoi().getNom());
        }
        assertEquals(Set.of(StatutTournoi.BROUILLON, StatutTournoi.DEMANDE_HOMOLOGATION, StatutTournoi.HOMOLOGUE,
                StatutTournoi.PUBLIE), statuts);
        assertEquals(Set.of(TournoiDemoBrunoy.NOM, TournoiDemoBrunoyParStatut.NOM_DEMANDE,
                TournoiDemoBrunoyParStatut.NOM_HOMOLOGUE, TournoiDemoBrunoyParStatut.NOM_PUBLIE,
                TournoiDemoBrunoyJourJ.NOM), noms, "cinq noms distincts, un par état");

        // Rechargées depuis le disque : numéros et inscrits conservés.
        assertTrue(sansNumero(brouillon));
        assertTrue(sansNumero(demande));
        assertEquals(TournoisDemo.HOMOLOGATION_BRUNOY_DEMO, homologue.getNumeroHomologation());
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(brouillon));
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(demande));
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(homologue));
        assertTrue(RemplissageTableaux.nombreDeParticipants(publie) > 200);
    }

    @Test
    void en_demo_seule_la_copie_jour_j_est_le_tournoi_en_cours_quelle_que_soit_la_date() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), true);
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());
        List<Path> enCours = new ArrayList<>();
        ctx.tournoisEnCours().forEach(tf -> enCours.add(tf.fichier().toAbsolutePath()));
        // Le tournoi se joue en avril 2027 : la copie jour J est affichée dès maintenant (démo) ;
        // les copies homologuée et publiée suivent la règle générale (J-7).
        assertTrue(enCours.contains(ctx.fichierTournoiJourJDemo().toAbsolutePath()), enCours.toString());
        assertFalse(enCours.contains(ctx.fichierTournoiDemo().toAbsolutePath()));
        assertFalse(enCours.contains(ctx.fichierTournoiDemandeDemo().toAbsolutePath()));
        assertEquals(1, enCours.size(), "avril 2027 est à plus de sept jours : " + enCours);
        assertEquals(ctx.fichierTournoiJourJDemo().toAbsolutePath(), enCours.get(0));

        // Au lancement habituel aussi (sans -Dpingmanager.demo=true) : la copie jour J est affichée.
        ContexteApplication habituel = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(),
                false);
        assertTrue(habituel.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());
        assertEquals(1, habituel.tournoisEnCours().size());
        assertEquals(TournoiDemoBrunoyJourJ.NOM, habituel.tournoisEnCours().get(0).tournoi().getNom());
    }

    @Test
    void les_copies_ne_sont_pas_recreees_si_elles_existent_deja() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());
        ContexteApplication ctx2 = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertEquals(5, ctx2.fichiersTournois().size(), "pas de doublon au redémarrage");
    }
}
