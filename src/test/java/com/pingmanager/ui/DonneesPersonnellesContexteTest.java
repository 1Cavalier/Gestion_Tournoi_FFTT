package com.pingmanager.ui;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.ServiceDonneesPersonnelles;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.persistence.EcritureSure;
import com.pingmanager.persistence.JournalDonneesPersonnelles;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 21 (RGPD) : purge en aperçu puis sur confirmation, effacement d'un joueur, journal sans donnée personnelle. */
class DonneesPersonnellesContexteTest {

    @TempDir
    Path dossier;

    private Path deposer(String nom, String club, LocalDate date, Joueur... joueurs) throws Exception {
        Tournoi t = new Tournoi(nom, "Club", date);
        t.setNumeroAffiliationClub(club);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(
                com.pingmanager.model.format.RegistreFormats.formatParDefaut()).placesMax(8).build());
        t.ajouterTableau(a);
        for (Joueur j : joueurs) {
            a.inscrire(j);
        }
        Path f = Files.createDirectories(dossier.resolve("tournois")).resolve(nom + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, new RegistreDisciplinaire(), f);
        DepotTournoi.sauvegarder(t, new RegistreDisciplinaire(), f); // crée aussi la copie .bak
        return f;
    }

    @Test
    void purge_en_apercu_ne_supprime_rien_puis_confirmation_explicite_exigee() throws Exception {
        ContexteApplication ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Path vieux = deposer("Vieux_Brunoy", DonneesDemo.CLUB_BRUNOY, LocalDate.of(2020, 4, 1));
        Path autreClub = deposer("Vieux_Paris", DonneesDemo.CLUB_PARIS_15, LocalDate.of(2020, 4, 1));
        Path recent = deposer("Recent_Brunoy", DonneesDemo.CLUB_BRUNOY, LocalDate.now().minusMonths(2));

        List<ServiceDonneesPersonnelles.TournoiAPurger<Path>> apercu = ctx.apercuPurge(3);
        assertEquals(List.of(vieux.toAbsolutePath()), apercu.stream().map(ServiceDonneesPersonnelles.TournoiAPurger::cle).toList(),
                "seulement les vieux tournois de ses clubs");
        assertTrue(Files.exists(vieux), "l'aperçu ne supprime rien");

        ContexteApplication.BilanPurge sansConfirmation = ctx.purgerTournois(apercu, 3, false);
        assertFalse(sansConfirmation.confirmee());
        assertTrue(Files.exists(vieux));

        ContexteApplication.BilanPurge b = ctx.purgerTournois(apercu, 3, true);
        assertEquals(List.of("Vieux_Brunoy"), b.supprimes());
        assertFalse(Files.exists(vieux));
        assertFalse(Files.exists(EcritureSure.secours(vieux)), "copie de secours supprimée aussi");
        assertTrue(Files.exists(autreClub) && Files.exists(recent));

        List<JournalDonneesPersonnelles.Entree> journal = ctx.journalRgpd().lire();
        assertEquals("PURGE", journal.get(journal.size() - 1).action());
    }

    @Test
    void effacement_d_un_joueur_dans_les_tournois_de_ses_clubs_termines_ou_non_journal_sans_donnee_personnelle()
            throws Exception {
        ContexteApplication ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Joueur j = new Joueur("9990077", "DURAND", "Lucie", Sexe.FEMME, "Brunoy", 800);
        Path termine = deposer("Termine", DonneesDemo.CLUB_BRUNOY, LocalDate.of(2025, 4, 1), j);
        Path aVenir = deposer("AVenir", DonneesDemo.CLUB_BRUNOY, LocalDate.now().plusMonths(3),
                new Joueur("9990077", "DURAND", "Lucie", Sexe.FEMME, "Brunoy", 800));
        Path autreClub = deposer("Paris", DonneesDemo.CLUB_PARIS_15, LocalDate.now().plusMonths(3),
                new Joueur("9990077", "DURAND", "Lucie", Sexe.FEMME, "Brunoy", 800));
        ctx.prononciations().definirJoueur("9990077", "lysi", "dyʁɑ̃");

        assertEquals(0, ctx.effacerJoueur("9990077", false).tournoisModifies(), "confirmation exigée");
        assertTrue(Files.readString(termine).contains("DURAND"));

        ContexteApplication.BilanEffacement b = ctx.effacerJoueur("9990077", true);
        assertEquals(2, b.tournoisModifies());
        assertTrue(b.prononciationRetiree());
        for (Path f : List.of(termine, aVenir)) {
            String contenu = Files.readString(f);
            assertFalse(contenu.contains("DURAND") || contenu.contains("Lucie"), f.toString());
            assertTrue(contenu.contains("9990077"), "licence gardée");
            Joueur relu = DepotTournoi.charger(f).getTournoi().getTableaux().get(0).getInscrits().get(0);
            assertEquals(ServiceDonneesPersonnelles.NOM_ANONYME, relu.getNom(), "format relu sans changement");
        }
        assertTrue(Files.readString(autreClub).contains("DURAND"), "un autre club n'est pas touché");

        String journal = Files.readString(ctx.journalRgpd().fichier());
        assertTrue(journal.contains("EFFACEMENT_JOUEUR"));
        assertFalse(journal.contains("9990077") || journal.contains("DURAND") || journal.contains("Lucie"),
                "journal sans donnée personnelle");
    }
}
