package com.pingmanager.persistence;

import com.pingmanager.core.Horloge;
import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.ResultatPropositionSalle;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L'état de la salle (matchs en cours, numéro de table, heure de début) survit
 * à une fermeture : par le document du tournoi, enregistré à chaque action, et
 * par l'autosave de session avec sa reprise après incident. Le format JSON
 * n'a pas changé : ces champs existaient déjà sur chaque match.
 */
class SalleRoundTripTest {

    @TempDir
    Path dossier;

    private Instant instant = Instant.parse("2026-11-14T09:00:00Z");
    private final Horloge horloge = () -> instant;
    private final ServiceSalle salle = new ServiceSalle(horloge);

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    /** Tournoi de 4 tables, tableau de 9 joueurs (3 poules de 3), trois parties lancées. */
    private Tournoi tournoiEnSalle() {
        Tournoi t = new Tournoi("Open Salle", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(4);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (int i = 1; i <= 9; i++) {
            a.inscrire(j("L" + i));
            a.pointer(j("L" + i));
        }
        new ServicePointage(t).cloturerPointage(a);
        new ServiceLancementTableau(t).lancer(a, 3L);
        ResultatPropositionSalle r = salle.proposer(t);
        assertEquals(3, r.getLancements().size(), "une partie par poule");
        instant = instant.plusSeconds(60 * 12);
        return t;
    }

    /** Résumé comparable d'une salle : « table : licenceA-licenceB depuis N min ». */
    private static List<String> resume(TableauDeBordSalle bord) {
        List<String> r = new ArrayList<>();
        for (TableauDeBordSalle.CarteTable c : bord.getTables()) {
            if (c.estLibre()) {
                r.add(c.numero() + " : libre");
            } else {
                Match m = c.match().match();
                r.add(c.numero() + " : " + m.getJoueurA().getNumeroLicence() + "-" + m.getJoueurB().getNumeroLicence()
                        + " (" + c.match().tour() + ") depuis " + c.ecoule().toMinutes() + " min");
            }
        }
        for (TableauDeBordSalle.MatchSalle p : bord.getPrets()) {
            r.add("prêt " + p.match().getJoueurA().getNumeroLicence() + "-" + p.match().getJoueurB().getNumeroLicence());
        }
        for (TableauDeBordSalle.MatchBloque b : bord.getBloques()) {
            r.add("bloqué " + b.match().match().getJoueurA().getNumeroLicence() + " : " + b.motif());
        }
        return r;
    }

    @Test
    void salle_identique_apres_enregistrement_et_relecture_du_tournoi() {
        Tournoi t = tournoiEnSalle();
        List<String> avant = resume(salle.tableauDeBord(t));
        Path fichier = dossier.resolve("salle" + DepotTournoi.EXTENSION);

        DepotTournoi.sauvegarder(t, new RegistreDisciplinaire(), fichier);
        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();

        TableauDeBordSalle apres = salle.tableauDeBord(relu);
        assertEquals(avant, resume(apres));
        assertEquals(3, apres.nombreTablesOccupees());
        assertEquals(Duration.ofMinutes(12), apres.table(1).ecoule());
        assertTrue(apres.getAnomalies().isEmpty());
    }

    @Test
    void salle_identique_apres_reprise_de_l_autosave_de_session() throws Exception {
        Tournoi t = tournoiEnSalle();
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        List<String> avant = resume(bord);
        Path document = dossier.resolve("open" + DepotTournoi.EXTENSION);
        SessionSalle session = new SessionSalle(t, new RegistreDisciplinaire(), bord.getOrdonnanceur(), null);

        // Incident : l'autosave est écrit, mais la session n'est pas fermée proprement.
        ServiceAutosave autosave = new ServiceAutosave(ServiceAutosave.autosavePour(document), () -> session,
                Duration.ofMinutes(15));
        autosave.sauvegarderMaintenant();
        ServiceAutosave.Reprise reprise = ServiceAutosave.detecterReprise(document);
        assertNotNull(reprise);

        SessionSalle reprisee = reprise.charger();

        assertEquals(avant, resume(salle.tableauDeBord(reprisee.getTournoi())));
        assertEquals(3, 4 - reprisee.getOrdonnanceur().tablesLibres().size(),
                "l'ordonnanceur de la session retrouve les tables occupées");
        for (int table = 1; table <= 3; table++) {
            Match m = reprisee.getOrdonnanceur().matchSurTable(table).getMatch();
            assertEquals(StatutMatch.EN_COURS, m.getStatut());
            assertEquals(Integer.valueOf(table), m.getNumeroTable());
            assertNotNull(m.getInstantLancement());
        }
    }

    @Test
    void session_relue_meme_avec_un_match_en_cours_sur_une_table_inexistante() throws Exception {
        Tournoi t = tournoiEnSalle();
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        // Fichier incohérent : un match libre marqué en cours sur la table 9 (le tournoi n'en a que 4).
        Match perdu = bord.getBloques().get(0).match().match();
        perdu.setStatut(StatutMatch.EN_COURS);
        perdu.setNumeroTable(9);
        Path fichier = dossier.resolve("incoherent.autosave.json");
        DepotSession.sauvegarder(new SessionSalle(t, new RegistreDisciplinaire(), bord.getOrdonnanceur(), null),
                fichier);

        SessionSalle relue = DepotSession.charger(fichier);

        assertEquals(1, relue.getOrdonnanceur().tablesLibres().size());
        TableauDeBordSalle apres = salle.tableauDeBord(relue.getTournoi());
        assertEquals(1, apres.getAnomalies().size(), "le match sur la table 9 est à régulariser");
        assertTrue(salle.annulerAffectation(relue.getTournoi(), apres.getAnomalies().get(0).match()).estReussi());
    }

    @Test
    void ancien_fichier_sans_match_en_cours_donne_une_salle_libre() throws Exception {
        Tournoi t = tournoiEnSalle();
        Path fichier = dossier.resolve("ancien" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, new RegistreDisciplinaire(), fichier);
        // Fichier d'une version antérieure : ni statut « en cours », ni table, ni heure de début.
        String texte = Files.readString(fichier)
                .replace("\"EN_COURS\"", "\"A_JOUER\"")
                .replaceAll("\"numeroTable\"\\s*:\\s*\\d+\\s*,?", "")
                .replaceAll("\"instantLancement\"\\s*:\\s*\"[^\"]*\"\\s*,?", "");
        Files.writeString(fichier, texte);

        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
        TableauDeBordSalle bord = salle.tableauDeBord(relu);

        assertEquals(4, bord.nombreTablesLibres());
        assertEquals(9, bord.getPrets().size());
    }
}
