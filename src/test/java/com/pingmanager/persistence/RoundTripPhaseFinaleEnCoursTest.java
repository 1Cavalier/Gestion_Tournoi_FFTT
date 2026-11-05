package com.pingmanager.persistence;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Une phase finale à moitié jouée (scores, forfait, demi-finale jouée) survit
 * à l'enregistrement : après relecture, on reprend la saisie là où on l'avait
 * laissée et la progression continue (vue « Phase finale »). Le format JSON
 * n'est pas modifié. Format P3Q2 du registre : pas de petite finale, donc deux
 * 3es ex aequo.
 */
class RoundTripPhaseFinaleEnCoursTest {

    private final ServicePhaseFinale service = new ServicePhaseFinale();

    private static List<Manche> victoireA(Match m) {
        return java.util.Collections.nCopies(m.getNombreManchesGagnantes(), new Manche(11, 7));
    }

    private Tournoi tournoiEnPhaseFinale() {
        Tournoi t = new Tournoi("Open RT", "Club", List.of(LocalDate.of(2027, 4, 10)));
        Tableau tableau = new Tableau("Série A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= 12; i++) {
            Joueur j = new Joueur("L" + i, "Nom" + i, "Prenom" + i, Sexe.HOMME, "Club" + (i % 5), 600 + 15 * i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 3L);
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
            }
        }
        assertTrue(service.construire(tableau, 77L).estConstruit());

        // Quarts : trois scores et un forfait ; une demi-finale jouée.
        List<Match> quarts = tableau.getPhaseFinale().getPremierTour().getMatchs();
        for (int i = 0; i < 3; i++) {
            assertTrue(service.saisirScore(tableau, quarts.get(i), victoireA(quarts.get(i))).estValide());
        }
        assertTrue(service.declarerIssue(tableau, quarts.get(3), IssueMatch.FORFAIT, quarts.get(3).getJoueurA()).estValide());
        Match demi = tableau.getPhaseFinale().getTours().get(1).getMatchs().get(0);
        assertTrue(service.saisirScore(tableau, demi, victoireA(demi)).estValide());
        return t;
    }

    @Test
    void phase_finale_en_cours_relue_puis_terminee_apres_rechargement() throws Exception {
        Tournoi original = tournoiEnPhaseFinale();
        Path fichier = Files.createTempFile("phase_finale", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(original, new RegistreDisciplinaire(), fichier);

        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
        Tableau tableau = relu.getTableaux().get(0);
        TableauElimination elim = tableau.getPhaseFinale();
        assertEquals(DiagnosticPhaseFinale.Etat.EN_COURS, service.diagnostiquer(tableau).getEtat());

        // Forfait relu : l'adversaire du fautif est qualifié.
        Match quart4 = elim.getPremierTour().getMatchs().get(3);
        assertEquals(IssueMatch.FORFAIT, quart4.getIssue());
        assertEquals(quart4.getJoueurB(), quart4.getVainqueur());

        // La demi jouée a propagé son vainqueur en finale.
        Match demi1 = elim.getTours().get(1).getMatchs().get(0);
        assertEquals(StatutMatch.TERMINE, demi1.getStatut());
        Match finale = ServicePhaseFinale.matchFinale(elim);
        assertEquals(demi1.getVainqueur(), finale.getJoueurA());
        assertNull(ServicePhaseFinale.petiteFinale(elim));

        // On reprend la saisie : seconde demi, puis finale.
        Match demi2 = elim.getTours().get(1).getMatchs().get(1);
        assertEquals(StatutMatch.A_JOUER, demi2.getStatut());
        assertTrue(service.saisirScore(tableau, demi2, victoireA(demi2)).estValide());
        assertEquals(demi2.getVainqueur(), finale.getJoueurB());
        assertTrue(service.saisirScore(tableau, finale, victoireA(finale)).estValide());

        PodiumPhaseFinale podium = ServicePhaseFinale.podium(elim);
        assertEquals(demi1.getVainqueur(), podium.premier());
        assertEquals(demi2.getVainqueur(), podium.deuxieme());
        assertEquals(List.of(demi1.getPerdant(), demi2.getPerdant()), podium.troisiemes());
        assertTrue(podium.troisiemesExAequo());
        assertEquals(DiagnosticPhaseFinale.Etat.TERMINEE, service.diagnostiquer(tableau).getEtat());
    }
}
