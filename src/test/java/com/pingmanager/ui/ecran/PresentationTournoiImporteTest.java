package com.pingmanager.ui.ecran;

import com.pingmanager.core.ImportJoueurs;
import com.pingmanager.core.ImportTableaux;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationTournoiImporteTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 4, 10);

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Open importé", "Club", JOUR);
        t.setPeriode(JOUR, JOUR);
        return t;
    }

    /** Importe deux tableaux (A avec horaire, B sans) et deux joueurs. */
    private static void importer(Tournoi t, String heureB) {
        ImportTableaux.Analyse defs = ImportTableaux.analyser(List.of(
                List.of("Nom du tableau *", "Points min", "Points max", "Public", "Jour", "Heure", "Places"),
                List.of("A", "", "799", "Mixte", "", "9h", "48"),
                List.of("B", "800", "1299", "Mixte", "", heureB, "48")));
        ImportJoueurs.Analyse j = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Tableau 1"),
                List.of("UN", "Un", "700", "C1", "A"),
                List.of("DEUX", "Deux", "900", "C2", "B")));
        ImportJoueurs.Plan p = ImportJoueurs.planifier(t, j, new ImportJoueurs.Options(null, true), defs.definitions());
        ImportJoueurs.appliquer(t, p, Map.of());
    }

    @Test
    void tournoi_neuf_aucune_etape_faite_et_tout_reste_a_faire() {
        PresentationTournoiImporte p = new PresentationTournoiImporte(tournoi());
        assertEquals(0, p.etapesFaites());
        assertFalse(p.pret());
        assertEquals(List.of("Importez d'abord le classeur des tableaux et des joueurs."), p.pointsAVerifier());
        assertEquals("Reste à déposer le règlement, puis importer les tableaux et les joueurs.", p.phraseJourJ());
    }

    @Test
    void import_avec_un_tableau_sans_horaire_reste_a_verifier() {
        Tournoi t = tournoi();
        importer(t, "");
        PresentationTournoiImporte p = new PresentationTournoiImporte(t);
        assertTrue(p.importFait());
        assertFalse(p.verificationFaite());
        assertEquals(List.of("B"), p.tableauxSansHoraire());
        assertEquals(List.of("Le tableau « B » n'a pas d'horaire."), p.pointsAVerifier());
        assertEquals(1, p.etapesFaites());
        assertEquals("Reste à déposer le règlement, puis vérifier l'import.", p.phraseJourJ());
    }

    @Test
    void reglement_depose_et_import_complet_donnent_un_tournoi_pret() {
        Tournoi t = tournoi();
        importer(t, "14h");
        t.setReglementDocument(new DocumentReference("Règlement", "/tmp/reglement.pdf"));
        PresentationTournoiImporte p = new PresentationTournoiImporte(t);
        assertTrue(p.reglementDepose());
        assertTrue(p.verificationFaite());
        assertTrue(p.pointsAVerifier().isEmpty());
        assertEquals(3, p.etapesFaites());
        assertTrue(p.pret());
        assertTrue(p.phraseJourJ().startsWith("Tout est prêt"));
    }
}
