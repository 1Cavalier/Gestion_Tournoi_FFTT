package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.Carton;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.CouleurCarton;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeTournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que sauvegarder puis recharger un tournoi complet préserve son état
 * de bout en bout : métadonnées, configuration, inscrits et paiements, poules et
 * scores, phase finale (avec le graphe de matchs), discipline, et déduplication
 * des joueurs. Contrôle aussi l'idempotence du cycle écriture → lecture.
 */
class RoundTripTournoiTest {

    private final FormatTournoi format = RegistreFormats.formatParDefaut();

    private Joueur j(String licence, int points) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence, Sexe.HOMME, "Club", points);
    }

    private Tournoi construireTournoiComplet(RegistreDisciplinaire discipline) {
        Tournoi t = new Tournoi("Open de Test", "PingClub",
                List.of(LocalDate.of(2026, 3, 14), LocalDate.of(2026, 3, 15)));
        t.setNombreDeTables(8);
        t.setNiveau(NiveauTournoi.REGIONAL);
        t.setTypeTournoi(TypeTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 14), LocalDate.of(2026, 3, 15));
        t.setAdresse(AdresseTournoi.de("12 rue du Sport", "44000", "Nantes"));
        t.setCorrespondant("Martin", "Claire", "claire@pingclub.fr", "0600000000", null);
        t.setDotationGlobale(1500.0);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Paul", "paul@x.fr",
                "JA123", GradeJugeArbitre.JA3));

        ConfigurationTableau config = ConfigurationTableau.depuisFormat(format)
                .placesMax(20).tailleMaxListeAttente(4)
                .tarif(new TarifInscription(8, 10))
                .tranche(500, 1500)
                .build();
        Tableau tableau = new Tableau("Tableau A", config);
        tableau.setType("Messieurs série 2");
        t.ajouterTableau(tableau);

        tableau.ouvrirPointage();
        for (int i = 1; i <= 12; i++) {
            Joueur joueur = j("J" + i, 800 + i * 20);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
            if (i % 2 == 0) {
                tableau.marquerPaye(joueur);
            }
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);

        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(List.of(new Manche(11, 5), new Manche(11, 7), new Manche(11, 9)));
            }
        }
        new ServiceLancementTableau().construirePhaseFinale(tableau, 2L);

        if (discipline != null) {
            Joueur premier = tableau.getInscrits().get(0);
            discipline.enregistrer(new Carton(premier, CouleurCarton.JAUNE, 0, false, "retard",
                    LocalDateTime.of(2026, 3, 14, 10, 30)));
        }
        return t;
    }

    @Test
    void round_trip_preserve_tout_l_etat() throws Exception {
        RegistreDisciplinaire discipline = new RegistreDisciplinaire();
        Tournoi original = construireTournoiComplet(discipline);

        Path fichier = Files.createTempFile("tournoi_test", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(original, discipline, fichier);

        TournoiMapper.DocumentCharge charge = DepotTournoi.charger(fichier);
        Tournoi t2 = charge.getTournoi();
        RegistreDisciplinaire d2 = charge.getDiscipline();

        // Métadonnées.
        assertEquals("Open de Test", t2.getNom());
        assertEquals("PingClub", t2.getClubOrganisateur());
        assertEquals(2, t2.getJours().size());
        assertEquals(NiveauTournoi.REGIONAL, t2.getNiveau());
        assertEquals(TypeTournoi.PROMOTIONNEL, t2.getTypeTournoi());
        assertEquals("Nantes", t2.getAdresse().getVille());
        assertEquals("claire@pingclub.fr", t2.getCorrespondantEmail());
        assertEquals(1500.0, t2.getDotationGlobale(), 0.001);
        assertEquals(8, t2.getNombreDeTables());
        assertEquals(1, t2.getJugesArbitres().size());
        assertEquals(StatutTournoi.BROUILLON, t2.getStatut());

        // Tableau + configuration.
        assertEquals(1, t2.getTableaux().size());
        Tableau tab2 = t2.getTableaux().get(0);
        assertEquals("Tableau A", tab2.getNom());
        assertEquals("Messieurs série 2", tab2.getType());
        assertEquals(StatutTableau.PHASE_FINALE, tab2.getStatut());
        assertEquals(20, tab2.getConfiguration().getPlacesMax());
        assertEquals(10, tab2.getConfiguration().getTarif().getMontantSurPlace(), 0.001);
        assertEquals(format.identifiant(), tab2.getConfiguration().getFormat().identifiant());
        assertEquals(12, tab2.getInscrits().size());

        // Paiements par tableau préservés (les pairs ont payé).
        int payes = 0;
        for (Joueur joueur : tab2.getInscrits()) {
            if (tab2.aPaye(joueur)) {
                payes++;
            }
        }
        assertEquals(6, payes);

        // Poules et scores.
        assertEquals(4, tab2.getPoules().size());
        assertTrue(tab2.poulesTerminees());
        Match m0 = tab2.getPoules().get(0).getMatchs().get(0);
        assertEquals(11, m0.getManches().get(0).pointsJoueurA());
        assertEquals(5, m0.getManches().get(0).pointsJoueurB());
        assertEquals(StatutMatch.TERMINE, m0.getStatut());

        // Phase finale + graphe de matchs.
        assertTrue(tab2.aPhaseFinale());
        TableauElimination elim2 = tab2.getPhaseFinale();
        assertEquals(8, elim2.getCadre());
        assertEquals(8, elim2.getNombreJoueurs());
        assertNotNull(elim2.getFinale());

        // Discipline.
        assertEquals(1, d2.tousLesCartons().size());
    }

    @Test
    void joueurs_dedupliques_meme_instance_partout() throws Exception {
        Tournoi original = construireTournoiComplet(null);
        Path fichier = Files.createTempFile("tournoi_dedup", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(original, fichier);

        Tableau tab = DepotTournoi.charger(fichier).getTournoi().getTableaux().get(0);
        Joueur ins0 = tab.getInscrits().get(0);

        boolean memeInstanceDansPoule = false;
        for (Poule p : tab.getPoules()) {
            for (Joueur joueur : p.getJoueurs()) {
                if (joueur.getNumeroLicence().equals(ins0.getNumeroLicence())) {
                    memeInstanceDansPoule = (joueur == ins0);
                }
            }
        }
        assertTrue(memeInstanceDansPoule,
                "un joueur inscrit et le même joueur en poule doivent être la même instance");
    }

    @Test
    void lien_de_progression_retabli_dans_la_phase_finale() throws Exception {
        Tournoi original = construireTournoiComplet(null);
        Path fichier = Files.createTempFile("tournoi_graphe", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(original, fichier);

        TableauElimination elim = DepotTournoi.charger(fichier)
                .getTournoi().getTableaux().get(0).getPhaseFinale();

        assertTrue(elim.getTours().size() >= 2);
        Tour finale = elim.getFinale();
        Match matchFinal = finale.getMatchs().get(0);
        Match src = matchFinal.getSourceA() != null ? matchFinal.getSourceA() : matchFinal.getSourceB();
        assertNotNull(src, "le match de finale doit avoir au moins un match source");
        Tour demi = elim.getTours().get(elim.getTours().size() - 2);
        assertTrue(demi.getMatchs().contains(src),
                "la source du match de finale doit être un match du tour précédent");
    }

    @Test
    void round_trip_idempotent() throws Exception {
        RegistreDisciplinaire discipline = new RegistreDisciplinaire();
        Tournoi original = construireTournoiComplet(discipline);

        Path f1 = Files.createTempFile("idem1", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(original, discipline, f1);
        TournoiMapper.DocumentCharge charge = DepotTournoi.charger(f1);

        Path f2 = Files.createTempFile("idem2", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(charge.getTournoi(), charge.getDiscipline(), f2);

        assertEquals(Files.readString(f1), Files.readString(f2),
                "recharger puis re-sauvegarder doit produire un fichier identique");
    }
}
