package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.CategorieDuree;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.MatchOrdonnancable;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import com.pingmanager.ordonnancement.PropositionLancement;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie la persistance de l'état vivant d'une session en salle (ordonnanceur
 * et estimateur de durées) et le mécanisme de reprise sur incident : un autosave
 * sans fermeture propre est détecté au démarrage, et le rechargement rétablit
 * l'occupation des tables, les mêmes instances de matchs que le tournoi, et
 * l'apprentissage des durées.
 */
class RepriseSessionTest {

    private final FormatTournoi format = RegistreFormats.formatParDefaut();

    private Joueur j(String licence, int points) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence, Sexe.HOMME, "Club", points);
    }

    private SessionSalle construireSessionEnCours() {
        Tournoi t = new Tournoi("Open Session", "Club", List.of(LocalDate.of(2026, 5, 2)));
        t.setNombreDeTables(6);
        Tableau tab = new Tableau("A", ConfigurationTableau.depuisFormat(format).build());
        t.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (int i = 1; i <= 8; i++) {
            Joueur joueur = j("J" + i, 900 + i);
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau().lancer(tab, 1L);

        PoidsOrdonnancement poids = new PoidsOrdonnancement();
        poids.setPoidsAttente(2.5);
        poids.setPoidsTableauCommence(1.75); // B025
        Ordonnanceur ord = new Ordonnanceur(6, poids);
        for (Poule p : tab.getPoules()) {
            for (Match m : p.getMatchs()) {
                MatchOrdonnancable mo = new MatchOrdonnancable(m, "A", "Tableau A", PhaseMatch.POULE);
                mo.setPretDepuis(LocalDateTime.of(2026, 5, 2, 9, 0));
                ord.ajouterMatch(mo);
            }
        }
        // Un match lancé sur la table 3 (partie en cours).
        MatchOrdonnancable moEnCours = ord.tousLesMatchs().get(0);
        ord.lancer(new PropositionLancement(moEnCours, 3, 10.0, "test"),
                LocalDateTime.of(2026, 5, 2, 9, 5));

        EstimateurDuree est = new EstimateurDuree(Duration.ofMinutes(20), 0.4, 2);
        CategorieDuree cat = new CategorieDuree(PhaseMatch.POULE, 4);
        est.definirValeurDepart(cat, Duration.ofMinutes(18));
        est.observer(cat, Duration.ofMinutes(22));
        est.observer(cat, Duration.ofMinutes(20));
        est.observer(cat, Duration.ofMinutes(24));

        return new SessionSalle(t, new RegistreDisciplinaire(), ord, est);
    }

    @Test
    void autosave_sans_fermeture_propre_est_detecte_et_rechargeable() throws Exception {
        SessionSalle session = construireSessionEnCours();
        Path document = Files.createTempFile("session_doc", ".ptt.json");
        Path autosave = ServiceAutosave.autosavePour(document);

        // Simuler un incident : autosave écrit, pas de marqueur de fermeture propre.
        ServiceAutosave svc = new ServiceAutosave(autosave, () -> session, Duration.ofMinutes(15));
        svc.sauvegarderMaintenant();
        assertTrue(Files.exists(autosave));

        ServiceAutosave.Reprise reprise = ServiceAutosave.detecterReprise(document);
        assertNotNull(reprise, "un incident doit être détecté");

        SessionSalle s2 = reprise.charger();
        assertEquals("Open Session", s2.getTournoi().getNom());
        assertTrue(s2.aOrdonnancement());
        assertEquals(6, s2.getOrdonnanceur().getNombreDeTables());
        assertEquals(2.5, s2.getOrdonnanceur().getPoids().getPoidsAttente(), 0.001);
        assertEquals(1.75, s2.getOrdonnanceur().getPoids().getPoidsTableauCommence(), 0.001);
    }

    @Test
    void occupation_de_table_et_meme_instance_de_match() throws Exception {
        SessionSalle session = construireSessionEnCours();
        Path document = Files.createTempFile("session_occ", ".ptt.json");
        Path autosave = ServiceAutosave.autosavePour(document);
        DepotSession.sauvegarder(session, autosave);

        SessionSalle s2 = DepotSession.charger(autosave);

        MatchOrdonnancable surTable3 = s2.getOrdonnanceur().matchSurTable(3);
        assertNotNull(surTable3, "la table 3 doit être ré-occupée");
        assertEquals(StatutMatch.EN_COURS, surTable3.getMatch().getStatut());

        // Le match de l'ordonnanceur est la même instance que celui du tournoi.
        Match ref = surTable3.getMatch();
        boolean memeInstance = false;
        for (Poule p : s2.getTournoi().getTableaux().get(0).getPoules()) {
            for (Match m : p.getMatchs()) {
                if (m == ref) {
                    memeInstance = true;
                }
            }
        }
        assertTrue(memeInstance,
                "le match sous l'ordonnanceur doit être la même instance que dans le tournoi");
    }

    @Test
    void apprentissage_des_durees_preserve() throws Exception {
        SessionSalle session = construireSessionEnCours();
        CategorieDuree cat = new CategorieDuree(PhaseMatch.POULE, 4);
        Duration estimAvant = session.getEstimateur().estimer(cat);
        int obsAvant = session.getEstimateur().observationsPour(cat);

        Path autosave = Files.createTempFile("session_est", ".autosave.json");
        DepotSession.sauvegarder(session, autosave);
        EstimateurDuree est2 = DepotSession.charger(autosave).getEstimateur();

        assertNotNull(est2);
        assertEquals(obsAvant, est2.observationsPour(cat));
        assertEquals(estimAvant, est2.estimer(cat));
        assertEquals(Duration.ofMinutes(20), est2.getValeurParDefautGlobale());
    }

    @Test
    void fermeture_propre_desactive_la_reprise() throws Exception {
        SessionSalle session = construireSessionEnCours();
        Path document = Files.createTempFile("session_clean", ".ptt.json");
        Path autosave = ServiceAutosave.autosavePour(document);

        try (ServiceAutosave svc = new ServiceAutosave(autosave, () -> session, Duration.ofMinutes(15))) {
            svc.demarrer();
        } // close() pose le marqueur de fermeture propre

        assertNull(ServiceAutosave.detecterReprise(document),
                "après une fermeture propre, aucune reprise ne doit être proposée");
    }
}
