package com.pingmanager.persistence;

import com.pingmanager.core.Horloge;
import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Une poule lancée en bloc (table, heure de lancement, matchs déjà joués) se
 * sauvegarde, se recharge et survit à la reprise sur incident ; un ancien
 * fichier où des matchs de poule ont été lancés un par un s'ouvre et se termine.
 */
class PouleLanceeRoundTripTest {

    @TempDir
    Path dossier;

    private Instant instant = Instant.parse("2026-11-14T09:00:00Z");
    private final Horloge horloge = () -> instant;
    private final ServiceSalle salle = new ServiceSalle(horloge);
    private static final List<Manche> TROIS_ZERO = List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5));

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    /** Tournoi de 4 tables, 9 joueurs (3 poules de 3) ; la poule 1 lancée en table 3, un résultat saisi. */
    private Tournoi enCours() {
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
        Poule p = a.getPoules().get(0);
        assertTrue(salle.lancerPoule(t, p, 3).estReussi());
        instant = instant.plusSeconds(60 * 14);
        assertTrue(new SaisieResultatMatch(horloge).enregistrerScore(a, p.getMatchs().get(0), TROIS_ZERO)
                .estEnregistre());
        return t;
    }

    private static void memePoule(Poule avant, Poule apres) {
        assertTrue(apres.estLanceeEnBloc());
        assertEquals(avant.getNumeroTable(), apres.getNumeroTable());
        assertEquals(avant.getInstantLancement(), apres.getInstantLancement());
        for (int i = 0; i < avant.getMatchs().size(); i++) {
            Match x = avant.getMatchs().get(i);
            Match y = apres.getMatchs().get(i);
            assertEquals(x.getStatut(), y.getStatut(), "match " + (i + 1));
            assertEquals(x.getManches(), y.getManches());
            assertEquals(x.getInstantLancement(), y.getInstantLancement());
        }
    }

    @Test
    void poule_en_cours_survit_a_l_enregistrement_et_a_la_reouverture() {
        Tournoi t = enCours();
        Poule avant = t.getTableaux().get(0).getPoules().get(0);
        Path f = dossier.resolve("t" + DepotTournoi.EXTENSION);

        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();

        Poule apres = relu.getTableaux().get(0).getPoules().get(0);
        memePoule(avant, apres);
        TableauDeBordSalle bord = salle.tableauDeBord(relu);
        assertTrue(bord.table(3).estPoule(), "la table 3 est toujours occupée par la poule");
        assertTrue(bord.getAnomalies().isEmpty());
        // Ses joueurs restent occupés, même celui qui est entre deux matchs.
        Poule autre = relu.getTableaux().get(0).getPoules().get(1);
        assertTrue(salle.lancerPoule(relu, autre, 1).estReussi());
        // Et la poule relue se termine normalement.
        SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);
        Tableau a = relu.getTableaux().get(0);
        for (Match m : apres.getMatchs()) {
            if (m.getStatut() != StatutMatch.TERMINE) {
                assertTrue(saisie.enregistrerScore(a, m, TROIS_ZERO).estEnregistre());
            }
        }
        assertTrue(salle.tableauDeBord(relu).table(3).estLibre());
    }

    @Test
    void poule_en_cours_survit_a_la_reprise_sur_incident() {
        Tournoi t = enCours();
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        Path f = dossier.resolve("autosave.json");

        DepotSession.sauvegarder(new SessionSalle(t, new RegistreDisciplinaire(), bord.getOrdonnanceur(), null), f);
        SessionSalle reprise = DepotSession.charger(f);

        Poule apres = reprise.getTournoi().getTableaux().get(0).getPoules().get(0);
        memePoule(t.getTableaux().get(0).getPoules().get(0), apres);
        // L'ordonnanceur repris garde la table 3 et tous les joueurs de la poule occupés.
        assertNotNull(reprise.getOrdonnanceur().pouleSurTable(3));
        assertFalse(reprise.getOrdonnanceur().tablesLibres().contains(3));
        for (Joueur joueur : apres.getJoueurs()) {
            assertTrue(reprise.getOrdonnanceur().joueursOccupes().contains(joueur.getNumeroLicence()));
        }
        assertTrue(salle.tableauDeBord(reprise.getTournoi()).table(3).estPoule());
    }

    @Test
    void poule_jamais_lancee_en_bloc_n_ecrit_rien_de_plus() {
        Tournoi t = enCours();
        Path f = dossier.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertFalse(relu.getTableaux().get(0).getPoules().get(2).estLanceeEnBloc());
    }

    @Test
    void ancien_fichier_avec_des_matchs_de_poule_lances_un_par_un_s_ouvre_et_se_termine() throws Exception {
        // Ancien fichier : chaque poule a son premier match lancé seul (tables 1 à 3), sans lancement de poule.
        Tournoi t = new Tournoi("Ancien", "Club", LocalDate.of(2026, 11, 14));
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
        LocalDateTime debut = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        for (int k = 0; k < 3; k++) {
            Match m = a.getPoules().get(k).getMatchs().get(0);
            m.setStatut(StatutMatch.EN_COURS);
            m.setNumeroTable(k + 1);
            m.setInstantLancement(debut);
        }
        Path f = dossier.resolve("ancien" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);

        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        Tableau r = relu.getTableaux().get(0);
        TableauDeBordSalle bord = salle.tableauDeBord(relu);
        assertTrue(bord.getAnomalies().isEmpty());
        assertEquals(3, bord.nombreTablesOccupees(), "chaque match lancé seul garde sa table");

        // Chaque poule se termine : son match en cours, puis le reste en bloc.
        SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);
        for (int k = 0; k < 3; k++) {
            Poule p = r.getPoules().get(k);
            Match enCours = p.getMatchs().get(0);
            assertTrue(saisie.enregistrerScore(r, enCours, TROIS_ZERO).estEnregistre());
            assertEquals(ResultatAffectationTable.Motif.TABLE_LIBEREE,
                    salle.libererApresResultat(relu, enCours).getMotif());
            assertTrue(salle.lancerPoule(relu, p, k + 1).estReussi(), "poule " + p.getNom());
            for (Match m : p.getMatchs()) {
                if (m.getStatut() != StatutMatch.TERMINE) {
                    assertTrue(saisie.enregistrerScore(r, m, TROIS_ZERO).estEnregistre());
                }
            }
            assertTrue(p.estTerminee());
        }
        assertEquals(4, salle.tableauDeBord(relu).nombreTablesLibres());
    }
}
