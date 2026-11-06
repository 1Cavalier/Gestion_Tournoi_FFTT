package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du câblage entre briques : habilitation compte↔club↔tournoi
 * ({@link ServiceTournois}) et couplage statut du tournoi ↔ ouverture des
 * inscriptions ({@link PolitiqueOuverture}).
 */
class CablagePreTournoiTest {

    private static final String CLUB = "08911132";
    private static final String AUTRE_CLUB = "07500001";

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();
    private final ServiceTournois tournois = new ServiceTournois(cycle);

    private AccesClub accesPour(String club) {
        return new AccesClub(club, "orga@club.fr", "Boulier", "Serge");
    }

    private Tableau tableauMixte(String nom) {
        return typeEtDate(new Tableau(nom, ConfigurationTableau.builder()
                .format(com.pingmanager.model.format.RegistreFormats.formatParDefaut())
                .placesMax(24).build()));
    }

    private Tableau tableauDames(String nom) {
        return typeEtDate(new Tableau(nom, ConfigurationTableau.builder().sexeRequis(Sexe.FEMME)
                .format(com.pingmanager.model.format.RegistreFormats.formatParDefaut())
                .placesMax(24).build()));
    }

    private Tableau typeEtDate(Tableau t) {
        t.setType("Toutes séries");
        HorairesTableau h = new HorairesTableau();
        h.setDebutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0));
        t.setHoraires(h);
        return t;
    }

    private com.pingmanager.model.InfosReglement reglementComplet() {
        return com.pingmanager.model.InfosReglement.builder()
                .responsableNom("Boulier Serge").responsableEmail("serge@club.fr")
                .salleNom("Gymnase").salleVille("Brunoy")
                .nombreDeTables(12).marqueBalles("ABS 40+")
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 1, 23, 59)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 1, 23, 59))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 8, 30))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build();
    }

    /** Tournoi prêt à homologuer, créé via le service habilité. */
    private Tournoi tournoiPret(AccesClub acces) {
        Tournoi t = tournois.creerTournoi(acces, "Open de Brunoy", LocalDate.of(2026, 3, 8));
        t.setInfosReglement(reglementComplet().toBuilder()
                .politiqueBalles(com.pingmanager.model.PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .build());
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(12);
        t.setReglementDocument(new com.pingmanager.model.DocumentReference("Règlement", "/r.pdf"));
        t.ajouterTableau(tableauMixte("A"));
        t.ajouterTableau(tableauDames("Dames"));
        return t;
    }

    // --- Habilitation : création ----------------------------------------------

    @Test
    void creation_rattache_le_tournoi_au_club_de_l_acces() {
        Tournoi t = tournois.creerTournoi(accesPour(CLUB), "Open", LocalDate.now());
        assertEquals(CLUB, t.getClubOrganisateur());
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
    }

    @Test
    void creation_multi_jours() {
        Tournoi t = tournois.creerTournoi(accesPour(CLUB), "Open",
                java.util.List.of(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 8)));
        assertEquals(2, t.nombreDeJours());
        assertEquals(CLUB, t.getClubOrganisateur());
    }

    // --- Habilitation : transitions -------------------------------------------

    @Test
    void homologuer_autorise_pour_le_bon_club() {
        AccesClub acces = accesPour(CLUB);
        Tournoi t = tournoiPret(acces);
        ResultatTransition res = tournois.homologuer(acces, t, "2026-91-042");
        assertTrue(res.estEffectuee());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut());
    }

    @Test
    void homologuer_refuse_pour_un_autre_club() {
        AccesClub acces = accesPour(CLUB);
        Tournoi t = tournoiPret(acces);
        AccesClub intrus = accesPour(AUTRE_CLUB);
        assertThrows(SecurityException.class, () -> tournois.homologuer(intrus, t, "NUM"));
        assertEquals(StatutTournoi.BROUILLON, t.getStatut(), "le tournoi n'a pas bougé");
    }

    @Test
    void publier_et_annuler_controlent_aussi_l_habilitation() {
        AccesClub acces = accesPour(CLUB);
        Tournoi t = tournoiPret(acces);
        tournois.homologuer(acces, t, "NUM");
        AccesClub intrus = accesPour(AUTRE_CLUB);
        assertThrows(SecurityException.class, () -> tournois.publier(intrus, t));
        assertThrows(SecurityException.class, () -> tournois.annuler(intrus, t));
        // Le bon accès, lui, fonctionne.
        assertTrue(tournois.publier(acces, t).estEffectuee());
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
    }

    @Test
    void est_habilite_reflete_le_club() {
        Tournoi t = tournois.creerTournoi(accesPour(CLUB), "Open", LocalDate.now());
        assertTrue(tournois.estHabilite(accesPour(CLUB), t));
        assertFalse(tournois.estHabilite(accesPour(AUTRE_CLUB), t));
    }

    // --- Statut ↔ inscriptions : mode par défaut (REFUSER_SI_ANNULE) ----------

    @Test
    void inscription_refusee_si_tournoi_annule() {
        AccesClub acces = accesPour(CLUB);
        Tournoi t = tournoiPret(acces);
        Tableau cible = t.getTableaux().get(0);
        Joueur j = new Joueur("L1", "Nom", "Prenom", Sexe.HOMME, "Club", 800);

        // En brouillon : accepté (rétrocompatible).
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT,
                service.inscrire(t, j, cible).getMotif());

        // Après annulation : refusé.
        tournois.annuler(acces, t);
        Joueur j2 = new Joueur("L2", "Nom2", "Prenom2", Sexe.HOMME, "Club", 800);
        assertEquals(ResultatInscriptionTournoi.Motif.TOURNOI_FERME,
                service.inscrire(t, j2, cible).getMotif());
    }

    // --- Statut ↔ inscriptions : mode strict (EXIGER_PUBLIE) ------------------

    @Test
    void mode_strict_refuse_hors_publie_et_accepte_publie() {
        AccesClub acces = accesPour(CLUB);
        Tournoi t = tournoiPret(acces);
        Tableau cible = t.getTableaux().get(0);
        Joueur j = new Joueur("L1", "Nom", "Prenom", Sexe.HOMME, "Club", 800);

        ServiceInscription strict = new ServiceInscription(
                com.pingmanager.ordonnancement.ValidationInscription.desactivee(),
                PolitiqueOuverture.EXIGER_PUBLIE);

        // Brouillon : refusé en mode strict.
        assertEquals(ResultatInscriptionTournoi.Motif.TOURNOI_FERME,
                strict.inscrire(t, j, cible).getMotif());

        // Une fois publié : accepté.
        tournois.homologuer(acces, t, "NUM");
        tournois.publier(acces, t);
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT,
                strict.inscrire(t, j, cible).getMotif());
    }

    // --- Parcours complet de bout en bout -------------------------------------

    @Test
    void parcours_complet_habilite_creation_a_inscription() {
        AccesClub acces = accesPour(CLUB);
        Tournoi t = tournoiPret(acces);

        assertTrue(tournois.homologuer(acces, t, "2026-91-042").estEffectuee());
        assertTrue(tournois.publier(acces, t).estEffectuee());
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());

        ServiceInscription strict = new ServiceInscription(
                com.pingmanager.ordonnancement.ValidationInscription.desactivee(),
                PolitiqueOuverture.EXIGER_PUBLIE);
        Joueur j = new Joueur("L1", "Nom", "Prenom", Sexe.HOMME, "Club", 900);
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT,
                strict.inscrire(t, j, t.getTableaux().get(0)).getMotif());
    }
}
