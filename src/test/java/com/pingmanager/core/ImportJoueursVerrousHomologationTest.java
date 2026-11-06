package com.pingmanager.core;

import com.pingmanager.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** B035 : l'import « créer les tableaux » ne doit pas contourner le verrou LISTE_TABLEAUX. */
class ImportJoueursVerrousHomologationTest {

    private static Tournoi tournoiEnDemande() {
        Tournoi t = new Tournoi("Open", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Paul", "p@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        Tableau d = new Tableau("Dames", ConfigurationTableau.builder().sexeRequis(Sexe.FEMME).build());
        d.setType("Toutes séries");
        d.setHoraires(new HorairesTableau(null, null, LocalDateTime.of(2026, 3, 8, 9, 0)));
        t.ajouterTableau(d);
        assertTrue(new ServiceCycleTournoi().demanderHomologation(t).estEffectuee());
        return t;
    }

    @Test
    void import_ne_cree_pas_de_tableau_dans_un_tournoi_en_demande_d_homologation() {
        Tournoi t = tournoiEnDemande();
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Tableau"),
                List.of("UN", "Un", "900", "C1", "Nouveau tableau")));
        ImportJoueurs.Plan p = ImportJoueurs.planifier(t, a, new ImportJoueurs.Options(null, true));

        assertTrue(p.tableaux().isEmpty());
        assertEquals(1, p.sansTableau().size());
        assertTrue(p.sansTableau().get(0).message().contains("homologation"), p.sansTableau().get(0).message());

        ImportJoueurs.Bilan b = ImportJoueurs.appliquer(t, p, Map.of());
        assertEquals(0, b.tableauxCrees());
        assertEquals(1, t.getTableaux().size(), "la liste des tableaux est verrouillée dès la demande");
    }

    @Test
    void import_inscrit_toujours_sur_un_tableau_existant_malgre_le_verrou() {
        Tournoi t = tournoiEnDemande();
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Tableau", "Sexe"),
                List.of("DEUX", "Deux", "900", "C1", "Dames", "F")));
        ImportJoueurs.Plan p = ImportJoueurs.planifier(t, a, new ImportJoueurs.Options(null, true));
        ImportJoueurs.Bilan b = ImportJoueurs.appliquer(t, p, Map.of());

        assertEquals(1, b.inscrits());
        assertEquals(1, t.getTableaux().get(0).getInscrits().size());
    }

    @Test
    void plan_etabli_avant_la_demande_ne_cree_pas_de_tableau_apres() {
        Tournoi t = tournoiEnDemande();
        new ServiceCycleTournoi().retirerDemande(t);
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Tableau"),
                List.of("UN", "Un", "900", "C1", "Nouveau tableau")));
        ImportJoueurs.Plan p = ImportJoueurs.planifier(t, a, new ImportJoueurs.Options(null, true));
        assertEquals(1, p.tableaux().size());
        assertTrue(new ServiceCycleTournoi().demanderHomologation(t).estEffectuee());

        ImportJoueurs.Bilan b = ImportJoueurs.appliquer(t, p, Map.of());
        assertEquals(0, b.tableauxCrees());
        assertEquals(1, t.getTableaux().size());
    }
}
