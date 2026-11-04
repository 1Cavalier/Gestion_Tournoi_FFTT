package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie le cycle de vie applicatif orchestré par {@link ServiceFichierTournoi} :
 * démarrage, création, enregistrement, récents, ouverture, autosave, et le
 * protocole de décision de reprise sur incident.
 */
class ServiceFichierTournoiTest {

    private final FormatTournoi format = RegistreFormats.formatParDefaut();

    private Path dossierNeuf() throws Exception {
        return Files.createTempDirectory("pm-donnees");
    }

    private Tournoi tournoi(String nom) {
        return new Tournoi(nom, "Club", List.of(LocalDate.of(2026, 6, 1)));
    }

    private ServiceFichierTournoi service(Path dossier, ServiceFichierTournoi.Ecouteur e) {
        return new ServiceFichierTournoi(dossier, Duration.ofMinutes(15), e);
    }

    @Test
    void demarrage_cree_un_depot_vide_au_premier_lancement() throws Exception {
        try (ServiceFichierTournoi svc = service(dossierNeuf(), new ServiceFichierTournoi.Ecouteur() {})) {
            DepotApplication app = svc.demarrer();
            assertNotNull(app);
            assertEquals(0, app.nombreDeClubs());
            assertTrue(svc.recents().isEmpty());
        }
    }

    @Test
    void enregistrer_sous_met_a_jour_le_fichier_courant_et_les_recents() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("open" + DepotTournoi.EXTENSION);

        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {})) {
            svc.demarrer();
            Tournoi t = tournoi("Open de Juin");
            RegistreDisciplinaire d = new RegistreDisciplinaire();
            svc.nouveau(t, d);
            assertTrue(svc.fichierCourant().isEmpty());

            svc.enregistrerSous(t, d, cible);
            assertEquals(cible.toAbsolutePath(), svc.fichierCourant().orElseThrow());
            assertTrue(Files.exists(cible));

            List<ProjetTournoi> recents = svc.recents();
            assertEquals(1, recents.size());
            assertEquals("Open de Juin", recents.get(0).getNom());
            assertEquals(cible.toAbsolutePath(), recents.get(0).getFichier());
        }
    }

    @Test
    void recents_persistes_entre_deux_demarrages() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("t" + DepotTournoi.EXTENSION);

        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {})) {
            svc.demarrer();
            Tournoi t = tournoi("Tournoi Persistant");
            RegistreDisciplinaire d = new RegistreDisciplinaire();
            svc.nouveau(t, d);
            svc.enregistrerSous(t, d, cible);
        }
        // Nouveau service, même dossier : les récents doivent être là.
        try (ServiceFichierTournoi svc2 = service(dossier, new ServiceFichierTournoi.Ecouteur() {})) {
            svc2.demarrer();
            assertEquals(1, svc2.recents().size());
            assertEquals("Tournoi Persistant", svc2.recents().get(0).getNom());
        }
    }

    @Test
    void ouvrir_sans_autosave_charge_le_document() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("doc" + DepotTournoi.EXTENSION);
        // Préparer un document sur disque.
        Tournoi t = tournoi("À Rouvrir");
        DepotTournoi.sauvegarder(t, new RegistreDisciplinaire(), cible);

        AtomicBoolean repriseProposee = new AtomicBoolean(false);
        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {
            @Override public void repriseDetectee(ServiceFichierTournoi.RepriseDecision d) {
                repriseProposee.set(true);
                d.ignorer();
            }
        })) {
            svc.demarrer();
            SessionSalle session = svc.ouvrir(cible);
            assertEquals("À Rouvrir", session.getTournoi().getNom());
            assertFalse(repriseProposee.get(), "aucune reprise ne doit être proposée sans autosave");
            assertEquals(cible.toAbsolutePath(), svc.fichierCourant().orElseThrow());
        }
    }

    @Test
    void reprise_detectee_et_choix_reprendre() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("crash" + DepotTournoi.EXTENSION);

        // Document « ancien » sur disque.
        Tournoi ancien = tournoi("Version Document");
        DepotTournoi.sauvegarder(ancien, new RegistreDisciplinaire(), cible);

        // Autosave « plus récent » non fermé proprement, avec un nom différent.
        SessionSalle sessionCrash = new SessionSalle(
                tournoi("Version Autosave"), new RegistreDisciplinaire(), null, null);
        DepotSession.sauvegarder(sessionCrash, ServiceAutosave.autosavePour(cible));

        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {
            @Override public void repriseDetectee(ServiceFichierTournoi.RepriseDecision d) {
                d.reprendre();
            }
        })) {
            svc.demarrer();
            SessionSalle session = svc.ouvrir(cible);
            assertEquals("Version Autosave", session.getTournoi().getNom(),
                    "en choisissant de reprendre, on doit obtenir l'état de l'autosave");
        }
    }

    @Test
    void reprise_detectee_et_choix_ignorer() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("crash2" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(tournoi("Version Document"), new RegistreDisciplinaire(), cible);
        DepotSession.sauvegarder(
                new SessionSalle(tournoi("Version Autosave"), new RegistreDisciplinaire(), null, null),
                ServiceAutosave.autosavePour(cible));

        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {
            @Override public void repriseDetectee(ServiceFichierTournoi.RepriseDecision d) {
                d.ignorer();
            }
        })) {
            svc.demarrer();
            SessionSalle session = svc.ouvrir(cible);
            assertEquals("Version Document", session.getTournoi().getNom(),
                    "en ignorant la reprise, on doit obtenir le document sur disque");
        }
    }

    @Test
    void reprise_sans_decision_applique_le_defaut_sur() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("crash3" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(tournoi("Version Document"), new RegistreDisciplinaire(), cible);
        DepotSession.sauvegarder(
                new SessionSalle(tournoi("Version Autosave"), new RegistreDisciplinaire(), null, null),
                ServiceAutosave.autosavePour(cible));

        // Écouteur par défaut : ne tranche pas explicitement.
        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {})) {
            svc.demarrer();
            SessionSalle session = svc.ouvrir(cible);
            assertEquals("Version Document", session.getTournoi().getNom(),
                    "sans décision, le défaut sûr est d'ignorer l'autosave");
        }
    }

    @Test
    void autosave_exige_un_fichier_puis_bascule_au_enregistrer_sous() throws Exception {
        Path dossier = dossierNeuf();
        Path cible1 = Files.createTempDirectory("pm-doc").resolve("a" + DepotTournoi.EXTENSION);
        Path cible2 = Files.createTempDirectory("pm-doc2").resolve("b" + DepotTournoi.EXTENSION);

        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {})) {
            svc.demarrer();
            Tournoi t = construireTournoiAvecOrdo();
            RegistreDisciplinaire d = new RegistreDisciplinaire();
            SessionSalle[] boite = { sessionAutour(t, d) };

            svc.nouveau(t, d);
            // Avant tout enregistrement, activer l'autosave doit échouer.
            assertThrows(IllegalStateException.class, () -> svc.activerAutosave(() -> boite[0]));

            svc.enregistrerSous(t, d, cible1);
            svc.activerAutosave(() -> boite[0]);
            assertTrue(svc.autosaveActif());
            assertTrue(Files.exists(ServiceAutosave.autosavePour(cible1)),
                    "l'autosave doit écrire un premier instantané à côté du document");

            // Enregistrer-sous vers un autre fichier : l'autosave doit suivre.
            svc.enregistrerSous(t, d, cible2);
            assertTrue(svc.autosaveActif());
            assertTrue(Files.exists(ServiceAutosave.autosavePour(cible2)),
                    "après enregistrer-sous, l'autosave doit viser le nouveau fichier");

            svc.fermerDocument();
            assertFalse(svc.autosaveActif());
        }
    }

    @Test
    void documents_ouvert_et_ferme_notifies() throws Exception {
        Path dossier = dossierNeuf();
        Path cible = Files.createTempDirectory("pm-doc").resolve("notif" + DepotTournoi.EXTENSION);

        AtomicBoolean ouvertVu = new AtomicBoolean(false);
        AtomicBoolean fermeVu = new AtomicBoolean(false);
        try (ServiceFichierTournoi svc = service(dossier, new ServiceFichierTournoi.Ecouteur() {
            @Override public void documentOuvert(Optional<Path> f) { ouvertVu.set(true); }
            @Override public void documentFerme() { fermeVu.set(true); }
        })) {
            svc.demarrer();
            Tournoi t = tournoi("Notif");
            RegistreDisciplinaire d = new RegistreDisciplinaire();
            svc.nouveau(t, d);
            assertTrue(ouvertVu.get());
            svc.enregistrerSous(t, d, cible);
            svc.fermerDocument();
            assertTrue(fermeVu.get());
        }
    }

    // --- Helpers pour une session avec ordonnancement ---

    private Tournoi construireTournoiAvecOrdo() {
        Tournoi t = tournoi("Avec Ordo");
        Tableau tab = new Tableau("A", ConfigurationTableau.depuisFormat(format).build());
        t.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (int i = 1; i <= 4; i++) {
            Joueur j = new Joueur("J" + i, "N" + i, "P" + i, Sexe.HOMME, "C", 900 + i);
            tab.inscrire(j);
            tab.pointer(j);
        }
        return t;
    }

    private SessionSalle sessionAutour(Tournoi t, RegistreDisciplinaire d) {
        Ordonnanceur ord = new Ordonnanceur(4, new PoidsOrdonnancement());
        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(20), 0.3, 3);
        return new SessionSalle(t, d, ord, est);
    }
}
