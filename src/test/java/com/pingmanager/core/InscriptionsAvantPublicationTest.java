package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B052 : tant qu'un tournoi n'est pas publié, ses inscriptions ne sont pas ouvertes.
 * Aucune inscription (service, sélection, sur place) ni aucun import de joueurs en
 * brouillon, en demande d'homologation ou homologué ; tout est accepté une fois publié.
 */
class InscriptionsAvantPublicationTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 4, 10);
    private static final List<StatutTournoi> AVANT_PUBLICATION =
            List.of(StatutTournoi.BROUILLON, StatutTournoi.DEMANDE_HOMOLOGATION, StatutTournoi.HOMOLOGUE);

    private static Tournoi tournoiAuStatut(StatutTournoi statut) {
        Tournoi t = new Tournoi("Open", "Club", JOUR);
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(8).tailleMaxListeAttente(2).build());
        t.ajouterTableau(a);
        if (statut == StatutTournoi.BROUILLON) {
            return t;
        }
        t.changerStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        if (statut == StatutTournoi.DEMANDE_HOMOLOGATION) {
            return t;
        }
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        if (statut == StatutTournoi.HOMOLOGUE) {
            return t;
        }
        t.changerStatut(statut);
        return t;
    }

    private static Joueur joueur(String licence) {
        return new Joueur(licence, "Nom" + licence, "Prenom", Sexe.HOMME, "Club", 800);
    }

    private static ImportJoueurs.Plan planUnJoueur(Tournoi t) {
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Tableau"),
                List.of("UN", "Un", "800", "C1", "A")));
        return ImportJoueurs.planifier(t, a, new ImportJoueurs.Options(null, false));
    }

    @Test
    void inscription_refusee_avec_motif_pour_chaque_statut_non_publie() {
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        for (StatutTournoi s : AVANT_PUBLICATION) {
            Tournoi t = tournoiAuStatut(s);
            Tableau a = t.getTableaux().get(0);
            ResultatInscriptionTournoi r = service.inscrire(t, joueur("1"), a);
            assertEquals(ResultatInscriptionTournoi.Motif.INSCRIPTIONS_NON_OUVERTES, r.getMotif(), s.name());
            assertFalse(r.estAccepte(), s.name());
            assertEquals(PolitiqueOuverture.MOTIF_AVANT_PUBLICATION,
                    MotifsInscription.texte(r, t, joueur("1"), a), s.name());
            assertTrue(a.getInscrits().isEmpty() && a.getFileAttente().isEmpty(), "rien d'inscrit : " + s);
        }
    }

    @Test
    void selection_et_simulation_refusees_avant_publication() {
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        for (StatutTournoi s : AVANT_PUBLICATION) {
            Tournoi t = tournoiAuStatut(s);
            Tableau a = t.getTableaux().get(0);
            Map<Tableau, ResultatInscriptionTournoi> r = service.inscrireSelection(t, joueur("1"), List.of(a));
            assertEquals(ResultatInscriptionTournoi.Motif.INSCRIPTIONS_NON_OUVERTES, r.get(a).getMotif(), s.name());
            assertEquals(0, RemplissageTableaux.nombreDeParticipants(t), s.name());
        }
    }

    @Test
    void inscription_sur_place_refusee_avant_publication() {
        ServiceInscriptionSurPlace surPlace = new ServiceInscriptionSurPlace();
        for (StatutTournoi s : AVANT_PUBLICATION) {
            Tournoi t = tournoiAuStatut(s);
            ServiceInscriptionSurPlace.Resultat r = surPlace.inscrire(t, joueur("1"), t.getTableaux().get(0), true);
            assertFalse(r.accepte(), s.name());
            assertEquals(PolitiqueOuverture.MOTIF_AVANT_PUBLICATION, r.motif(), s.name());
            assertEquals(0, RemplissageTableaux.nombreDeParticipants(t), s.name());
        }
    }

    @Test
    void import_de_joueurs_refuse_avant_publication_sans_rien_modifier() {
        for (StatutTournoi s : AVANT_PUBLICATION) {
            Tournoi t = tournoiAuStatut(s);
            ImportJoueurs.Plan p = planUnJoueur(t);
            assertEquals(PolitiqueOuverture.MOTIF_AVANT_PUBLICATION, ImportJoueurs.motifRefus(t), s.name());
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> ImportJoueurs.appliquer(t, p, Map.of()), s.name());
            assertEquals(PolitiqueOuverture.MOTIF_AVANT_PUBLICATION, e.getMessage());
            assertEquals(0, RemplissageTableaux.nombreDeParticipants(t), s.name());
            assertEquals(1, t.getTableaux().size(), s.name());
        }
    }

    @Test
    void tout_est_accepte_une_fois_publie() {
        Tournoi t = tournoiAuStatut(StatutTournoi.PUBLIE);
        Tableau a = t.getTableaux().get(0);
        assertTrue(PolitiqueOuverture.accepteJoueurs(t));
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT,
                ServiceInscription.sansContrainteSuccession().inscrire(t, joueur("1"), a).getMotif());
        assertTrue(new ServiceInscriptionSurPlace().inscrire(t, joueur("2"), a, true).accepte());
        assertNull(ImportJoueurs.motifRefus(t));
        ImportJoueurs.Bilan b = ImportJoueurs.appliquer(t, planUnJoueur(t), Map.of());
        assertEquals(1, b.inscrits());
        assertEquals(3, a.getInscrits().size());
        assertTrue(PolitiqueOuverture.constatJoueursAvantPublication(t).isEmpty());
    }

    @Test
    void tournoi_annule_reste_ferme() {
        Tournoi t = tournoiAuStatut(StatutTournoi.ANNULE);
        assertEquals(ResultatInscriptionTournoi.Motif.TOURNOI_FERME, ServiceInscription.sansContrainteSuccession()
                .inscrire(t, joueur("1"), t.getTableaux().get(0)).getMotif());
        assertNotNull(ImportJoueurs.motifRefus(t));
    }

    @Test
    void tournoi_importe_garde_son_import_des_le_brouillon_en_attente_de_decision() {
        // Hors périmètre de B052 (README section 6) : comportement inchangé.
        Tournoi t = tournoiAuStatut(StatutTournoi.BROUILLON);
        t.setModePreparation(ModePreparation.IMPORT);
        assertNull(ImportJoueurs.motifRefus(t));
        assertEquals(1, ImportJoueurs.appliquer(t, planUnJoueur(t), Map.of()).inscrits());
        assertTrue(PolitiqueOuverture.constatJoueursAvantPublication(t).isEmpty(), "pas d'alerte : cas prévu");
    }

    @Test
    void ancien_fichier_non_publie_avec_joueurs_est_signale_sans_rien_supprimer() {
        Tournoi t = tournoiAuStatut(StatutTournoi.HOMOLOGUE);
        Tableau a = t.getTableaux().get(0);
        a.inscrire(joueur("1")); // état d'un fichier écrit avant la règle
        a.inscrire(joueur("2"));
        String constat = PolitiqueOuverture.constatJoueursAvantPublication(t).orElseThrow();
        assertTrue(constat.contains("2 joueurs"), constat);
        assertTrue(constat.contains("conservés"), constat);
        assertEquals(2, a.getInscrits().size(), "aucune donnée supprimée");
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIPTIONS_NON_OUVERTES,
                ServiceInscription.sansContrainteSuccession().inscrire(t, joueur("3"), a).getMotif());
        assertTrue(PolitiqueOuverture.constatJoueursAvantPublication(tournoiAuStatut(StatutTournoi.HOMOLOGUE))
                .isEmpty(), "rien à signaler sans joueur");
    }
}
