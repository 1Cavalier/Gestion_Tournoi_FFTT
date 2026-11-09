package com.pingmanager.ui.infra;

import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.ui.ContexteApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournoiDemoBrunoyJourJTest {

    static final Instant MAINTENANT = Instant.parse("2027-04-10T10:00:00Z");

    @TempDir
    Path dossier;

    static FicheLicence quentin() {
        return DonneesDemo.licences().trouverParLicence(DonneesDemo.LICENCE_QUENTIN).orElseThrow();
    }

    static Tournoi demo() {
        return TournoiDemoBrunoyJourJ.construire(DonneesDemo.JOUEURS_FICTIFS, quentin(), MAINTENANT);
    }

    static List<Tableau> samedi(Tournoi t) {
        return TournoiDemoBrunoyJourJ.tableauxDuJour(t, t.getJours().first());
    }

    static List<Match> enCours(Tableau tab) {
        List<Match> res = new ArrayList<>();
        for (Poule p : tab.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (m.getStatut() == StatutMatch.EN_COURS) {
                    res.add(m);
                }
            }
        }
        return res;
    }

    @Test
    void copie_du_national_b_avec_les_memes_tableaux_et_horaires() {
        Tournoi copie = demo();
        Tournoi nb = TournoiDemoBrunoy.construire();
        assertEquals(TournoiDemoBrunoyJourJ.NOM, copie.getNom());
        assertNotEquals(nb.getNom(), copie.getNom());
        assertEquals(StatutTournoi.PUBLIE, copie.getStatut());
        assertEquals(nb.getTableaux().size(), copie.getTableaux().size());
        for (int i = 0; i < nb.getTableaux().size(); i++) {
            Tableau a = nb.getTableaux().get(i);
            Tableau b = copie.getTableaux().get(i);
            assertEquals(a.getNom(), b.getNom());
            assertEquals(a.getHoraires().getDebutMatchs(), b.getHoraires().getDebutMatchs());
            assertEquals(a.getConfiguration().getPlacesMax(), b.getConfiguration().getPlacesMax());
        }
        assertEquals(nb.getJours(), copie.getJours());
    }

    @Test
    void presque_tous_les_tableaux_remplis() {
        Tournoi t = demo();
        int inscrits = 0;
        int places = 0;
        for (Tableau tab : t.getTableaux()) {
            int max = tab.getConfiguration().getPlacesMax();
            assertTrue(tab.getInscrits().size() >= max * 9 / 10,
                    tab.getNom() + " : " + tab.getInscrits().size() + " / " + max);
            inscrits += tab.getInscrits().size();
            places += max;
        }
        assertTrue(inscrits >= places * 95 / 100, inscrits + " / " + places);
        assertTrue(t.getTableaux().stream().filter(x -> !x.getFileAttente().isEmpty()).count() >= 3,
                "quelques tableaux avec une liste d'attente");
    }

    @Test
    void samedi_en_cours_etape_par_etape() {
        Tournoi t = demo();
        List<Tableau> sa = samedi(t);
        assertTrue(sa.size() >= 6);

        // 1. Tableau final tiré, premier tour joué, un match en cours.
        Tableau finale = sa.get(0);
        assertEquals(StatutTableau.PHASE_FINALE, finale.getStatut());
        assertEquals(Long.valueOf(TournoiDemoBrunoyJourJ.GRAINE_TABLEAU_FINAL), finale.getGraineTableauFinal());
        List<Tour> tours = ServicePhaseFinale.toursPrincipaux(finale.getPhaseFinale());
        assertTrue(tours.get(0).getMatchs().stream().noneMatch(m -> m.getStatut() == StatutMatch.A_JOUER
                && !m.getJoueurA().estADeterminer() && !m.getJoueurB().estADeterminer()), "premier tour joué");
        Match enCoursFinale = tours.get(1).getMatchs().stream()
                .filter(m -> m.getStatut() == StatutMatch.EN_COURS).findFirst().orElseThrow();
        assertEquals(Integer.valueOf(TournoiDemoBrunoyJourJ.TABLE_TABLEAU_FINAL), enCoursFinale.getNumeroTable());

        // 2. Poules toutes jouées : prêt pour le tirage du tableau final.
        assertEquals(StatutTableau.LANCE, sa.get(1).getStatut());
        assertTrue(sa.get(1).poulesTerminees());

        // 3. Poules en cours, trois parties en salle.
        Tableau poules = sa.get(2);
        assertEquals(StatutTableau.LANCE, poules.getStatut());
        assertEquals(Long.valueOf(TournoiDemoBrunoyJourJ.GRAINE_POULES), poules.getGrainePoules());
        assertFalse(poules.poulesTerminees());
        assertEquals(TournoiDemoBrunoyJourJ.TABLES_POULES,
                enCours(poules).stream().map(Match::getNumeroTable).sorted().toList());

        // 4. Pointage clôturé ; 5. pointage ouvert.
        assertEquals(StatutTableau.POINTAGE, sa.get(3).getStatut());
        assertTrue(sa.get(3).pointageEstCloture());
        assertEquals(StatutTableau.POINTAGE, sa.get(4).getStatut());
        assertFalse(sa.get(4).pointageEstCloture());

        // Le reste du samedi et tout le dimanche : encore en inscription.
        for (Tableau tab : t.getTableaux()) {
            int i = sa.indexOf(tab);
            if (i < 0 || i > 4) {
                assertEquals(StatutTableau.INSCRIPTION, tab.getStatut(), tab.getNom());
            }
        }
    }

    @Test
    void compte_de_demo_inscrit_aux_2e_et_4e_tableaux_du_samedi() {
        Tournoi t = demo();
        List<Tableau> sa = samedi(t);
        String licence = DonneesDemo.LICENCE_QUENTIN;
        assertTrue(sa.get(1).getInscrits().stream().anyMatch(j -> licence.equals(j.getNumeroLicence())));
        assertTrue(sa.get(3).getInscrits().stream().anyMatch(j -> licence.equals(j.getNumeroLicence())));
    }

    @Test
    void faux_numero_et_personnes_fictives_seulement_dans_la_copie() {
        Tournoi copie = demo();
        assertEquals(TournoisDemo.HOMOLOGATION_BRUNOY_DEMO, copie.getNumeroHomologation());
        assertTrue(copie.getNumeroHomologation().contains("fictif"));
        assertEquals(List.of(TournoiDemoBrunoyJourJ.JA_FICTIF, TournoiDemoBrunoyJourJ.ADJOINT_FICTIF),
                copie.getJugesArbitres());
        assertFalse(copie.getCorrespondantEmail().contains("gmail"));
        assertFalse(copie.getPaiement().getLien().contains("cttb-brunoy"));
        String reglement = new com.pingmanager.core.reglement.GenerateurReglement().generer(copie).articles()
                .stream().flatMap(a -> a.blocs().stream()).map(b -> b.texteBrut())
                .reduce("", (x, y) -> x + "\n" + y);
        for (String reel : List.of("LOOBUYCK", "PRIOU", "BOULIER", "sboulier", "cttb-brunoy")) {
            assertFalse(reglement.contains(reel), "personne ou lien réel dans la copie : " + reel);
        }

        // Le vrai National B, lui, reste en brouillon, avec ses vraies personnes.
        Tournoi nb = TournoiDemoBrunoy.construire();
        assertEquals(StatutTournoi.BROUILLON, nb.getStatut());
        assertTrue(nb.getNumeroHomologation() == null || nb.getNumeroHomologation().isBlank());
        assertEquals("LOOBUYCK", nb.getJugeArbitrePrincipal().orElseThrow().getNom());
        assertTrue(nb.getTableaux().stream().allMatch(x -> x.getStatut() == StatutTableau.INSCRIPTION
                && x.getInscrits().isEmpty()));
    }

    @Test
    void enregistre_puis_relu_a_l_identique() throws Exception {
        Tournoi t = demo();
        Path f = dossier.resolve("copie" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        List<Tableau> a = samedi(t);
        List<Tableau> b = samedi(relu);
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).getStatut(), b.get(i).getStatut(), a.get(i).getNom());
            assertEquals(a.get(i).getInscrits().size(), b.get(i).getInscrits().size());
        }
        assertEquals(TournoiDemoBrunoyJourJ.TABLES_POULES,
                enCours(b.get(2)).stream().map(Match::getNumeroTable).sorted().toList());
    }

    @Test
    void au_demarrage_seuls_le_national_b_et_sa_copie_restent() throws Exception {
        // Un ancien tournoi de démo et un tournoi créé à la main, avant la démo v16.
        Path tournois = Files.createDirectories(dossier.resolve("tournois"));
        Path ancienneDemo = tournois.resolve("Demo_Regional_Creteil_2026-demo" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(TournoiDemoBrunoy.construire(), ancienneDemo);
        Path aLaMain = tournois.resolve("Mon_essai" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(TournoiDemoBrunoy.construire(), aLaMain);

        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_QUENTIN, "Soumet123/").isPresent());

        assertFalse(Files.exists(ancienneDemo), "ancienne démo supprimée");
        assertFalse(Files.exists(aLaMain));
        assertTrue(Files.exists(tournois.resolve("archives").resolve(aLaMain.getFileName())),
                "tournoi créé à la main archivé, pas supprimé");
        List<Path> fichiers = ctx.fichiersTournois();
        assertEquals(2, fichiers.size(), fichiers.toString());
        Path copie = ctx.fichierTournoiJourJDemo().toAbsolutePath();
        assertTrue(fichiers.contains(copie));
        assertTrue(fichiers.contains(ctx.fichierTournoiDemo().toAbsolutePath()));
        assertEquals(StatutTournoi.BROUILLON, ctx.chargerTournoi(ctx.fichierTournoiDemo()).getStatut(),
                "le vrai National B, en brouillon");
        assertTrue(ctx.tournoisRecents().stream().anyMatch(p -> p.getFichier().equals(copie)), "dans les récents");
        Tournoi t = ctx.chargerTournoi(copie);
        assertEquals(TournoiDemoBrunoyJourJ.NOM, t.getNom());
        assertTrue(ctx.estTournoiDuCompte(t));
    }
}
