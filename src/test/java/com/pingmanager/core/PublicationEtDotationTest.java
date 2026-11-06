package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du branchement de la grille P dans la transition {@code publier} et du
 * calcul automatique de la dotation globale.
 */
class PublicationEtDotationTest {

    private final ServiceCycleTournoi service = new ServiceCycleTournoi();

    private Tableau tableau(String nom, boolean dames, GrilleRecompenses grille) {
        ConfigurationTableau.Builder b = ConfigurationTableau.builder()
                .format(RegistreFormats.formatParDefaut())
                .placesMax(24);
        if (dames) b.sexeRequis(Sexe.FEMME);
        if (grille != null) b.recompenses(grille);
        Tableau t = new Tableau(nom, b.build());
        t.setType("Toutes séries");
        HorairesTableau h = new HorairesTableau();
        h.setDebutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0));
        t.setHoraires(h);
        return t;
    }

    private Tournoi tournoiHomologue(GrilleRecompenses grilleDames) {
        Tournoi t = new Tournoi("Open", "08911132", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue X", "91800", "Brunoy"));
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(12);
        t.setReglementDocument(new DocumentReference("Règlement", "/r.pdf"));
        t.setInfosReglement(InfosReglement.builder()
                .responsableNom("R").responsableEmail("r@x.fr")
                .salleNom("G").salleVille("B").nombreDeTables(12).marqueBalles("ABS")
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 1, 12, 0)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 1, 12, 0))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 8, 30))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build());
        t.ajouterTableau(tableau("Messieurs", false, null));
        t.ajouterTableau(tableau("Dames", true, grilleDames));
        // Amener en HOMOLOGUE.
        assertTrue(service.homologuer(t, "2026-91-042").estEffectuee());
        return t;
    }

    // --- Publication branchée sur P ------------------------------------------

    @Test
    void publier_refuse_si_grille_P_incomplete() {
        // Tournoi homologable mais SANS les exigences P (pas de niveau, tables, PDF…).
        Tournoi t = new Tournoi("Open", "C", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("N", "P", "e@x.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("N", "P", "e@x.fr", "1", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("v", "91800", "B"));
        Tableau dames = tableau("Dames", true, null);
        t.ajouterTableau(dames);
        service.homologuer(t, "NUM");

        ResultatTransition res = service.publier(t);
        assertEquals(ResultatTransition.Motif.DOSSIER_INCOMPLET, res.getMotif());
        assertFalse(res.getManquants().isEmpty());
        assertEquals(StatutTournoi.HOMOLOGUE, t.getStatut(), "reste homologué si refus");
    }

    @Test
    void publier_reussit_si_grille_P_complete() {
        Tournoi t = tournoiHomologue(null);
        ResultatTransition res = service.publier(t);
        assertTrue(res.estEffectuee());
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
    }

    // --- Calcul de la dotation globale ---------------------------------------

    @Test
    void dotation_globale_nulle_sans_recompenses() {
        Tournoi t = tournoiHomologue(null);
        assertEquals(0.0, service.calculerDotationGlobale(t), 0.001);
    }

    @Test
    void dotation_globale_somme_les_recompenses_argent() {
        // 1ʳᵉ place : 100 €, 2ᵉ-3ᵉ : 50 € chacune -> 100 + 2*50 = 200 €
        GrilleRecompenses grille = GrilleRecompenses.builder()
                .place(1, Recompense.argent(100))
                .plage(2, 3, Recompense.argent(50))
                .build();
        Tournoi t = tournoiHomologue(grille);
        assertEquals(200.0, service.calculerDotationGlobale(t), 0.001);
    }

    @Test
    void publier_renseigne_la_dotation_calculee() {
        GrilleRecompenses grille = GrilleRecompenses.builder()
                .place(1, Recompense.argent(80))
                .build();
        Tournoi t = tournoiHomologue(grille);
        service.publier(t);
        assertEquals(80.0, t.getDotationGlobale(), 0.001);
    }
}
