package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la complétude à trois niveaux (étape 2) : ce qui est requis pour
 * l'HOMOLOGATION (grille H) et ce qui l'est en plus pour la PUBLICATION (grille P).
 */
class CompletudeTroisNiveauxTest {

    private final ServiceCycleTournoi service = new ServiceCycleTournoi();

    // --- Montage --------------------------------------------------------------

    private Tableau tableauDamesComplet() {
        Tableau t = new Tableau("Dames", ConfigurationTableau.builder()
                .sexeRequis(Sexe.FEMME)
                .format(RegistreFormats.formatParDefaut())   // format défini (requis P)
                .placesMax(24)                          // capacité (requis P)
                .build());
        t.setType("Toutes séries");                     // type SPID (requis H)
        HorairesTableau h = new HorairesTableau();
        h.setDebutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0)); // date (requis H)
        t.setHoraires(h);
        return t;
    }

    private InfosReglement reglementPourPublication() {
        return InfosReglement.builder()
                .responsableNom("R").responsableEmail("r@x.fr")
                .salleNom("Gymnase").salleVille("Brunoy")
                .nombreDeTables(10).marqueBalles("ABS 40+")
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .ouvertureInscriptions((LocalDateTime.of(2026, 3, 1, 12, 0)).minusDays(30))

                .clotureInscriptions(LocalDateTime.of(2026, 3, 1, 12, 0))
                .cloturePointage(LocalDateTime.of(2026, 3, 8, 8, 30))
                .debutMatchs(LocalDateTime.of(2026, 3, 8, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build();
    }

    /** Tournoi satisfaisant la grille H (dossier de demande complet). */
    private Tournoi tournoiHomologable() {
        Tournoi t = new Tournoi("Open", "08911132", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre(
                "Durand", "Paul", "paul@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue X", "91800", "Brunoy"));
        t.ajouterTableau(tableauDamesComplet());
        return t;
    }

    // --- Homologation (grille H) ----------------------------------------------

    @Test
    void homologable_quand_grille_H_complete() {
        assertTrue(service.elementsManquantsPourHomologation(tournoiHomologable()).isEmpty());
    }

    @Test
    void H_exige_le_correspondant() {
        Tournoi t = tournoiHomologable();
        t.setCorrespondant(null, null, null, null, null);
        var m = service.elementsManquantsPourHomologation(t);
        assertTrue(m.stream().anyMatch(x -> x.contains("correspondant : nom")));
        assertTrue(m.stream().anyMatch(x -> x.contains("correspondant : email")));
    }

    @Test
    void H_exige_un_juge_arbitre() {
        Tournoi t = new Tournoi("Open", "C", LocalDate.now());
        assertTrue(service.elementsManquantsPourHomologation(t).stream()
                .anyMatch(x -> x.contains("juge-arbitre")));
    }

    @Test
    void H_exige_la_categorie() {
        // La catégorie (IX.102) remplace l'ancien type et fixe l'échelon (IX.103).
        Tournoi t = tournoiHomologable();
        t.setNiveau(null);
        assertTrue(service.elementsManquantsPourHomologation(t).stream()
                .anyMatch(x -> x.contains("catégorie du tournoi")));
    }

    @Test
    void le_type_n_intervient_plus() {
        Tournoi t = tournoiHomologable();
        t.setTypeTournoi(null);
        assertTrue(service.elementsManquantsPourHomologation(t).isEmpty());
    }

    @Test
    void H_exige_adresse_complete() {
        Tournoi t = tournoiHomologable();
        t.setAdresse(AdresseTournoi.partielle("5 rue X", null, null, null, null));
        assertTrue(service.elementsManquantsPourHomologation(t).stream()
                .anyMatch(x -> x.contains("adresse")));
    }

    @Test
    void H_exige_tableau_feminin_et_type_et_date() {
        // Un tableau mixte sans type ni date : plusieurs manques cumulés.
        Tournoi t = new Tournoi("Open", "C", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("N", "P", "e@x.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("N", "P", "e@x.fr", "1", GradeJugeArbitre.JA3));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("v", "91800", "B"));
        t.ajouterTableau(new Tableau("A", ConfigurationTableau.builder().build())); // mixte, sans type/date
        var m = service.elementsManquantsPourHomologation(t);
        assertTrue(m.stream().anyMatch(x -> x.contains("dames")), "IX.104");
        assertTrue(m.stream().anyMatch(x -> x.contains(": type")));
        assertTrue(m.stream().anyMatch(x -> x.contains("début des matchs")));
    }

    // --- Publication (grille P) -----------------------------------------------

    @Test
    void publiable_quand_grille_P_complete() {
        Tournoi t = tournoiHomologable();
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(10);
        t.setNumeroHomologation("2026-91-042");
        t.setReglementDocument(new com.pingmanager.model.DocumentReference("Règlement", "/r.pdf"));
        t.setInfosReglement(reglementPourPublication());
        assertTrue(service.elementsManquantsPourPublication(t).isEmpty(),
                "manquants inattendus : " + service.elementsManquantsPourPublication(t));
    }

    @Test
    void P_exige_tables_numero_et_reglement_pdf() {
        Tournoi t = tournoiHomologable(); // rien des exigences P
        var m = service.elementsManquantsPourPublication(t);
        assertFalse(m.contains("niveau du tournoi"), "la catégorie est exigée dès l'homologation");
        assertTrue(m.contains("nombre de tables"));
        assertTrue(m.contains("numéro d'homologation"));
        assertTrue(m.contains("document de règlement (PDF)"));
    }

    @Test
    void P_exige_politique_balles_et_clotures() {
        Tournoi t = tournoiHomologable();
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(10);
        t.setNumeroHomologation("NUM");
        t.setReglementDocument(new com.pingmanager.model.DocumentReference("R", "/r.pdf"));
        // règlement présent mais sans politique balles ni clôtures
        t.setInfosReglement(InfosReglement.builder()
                .responsableNom("R").responsableEmail("r@x.fr")
                .salleNom("G").salleVille("B").nombreDeTables(10).marqueBalles("ABS")
                .build());
        var m = service.elementsManquantsPourPublication(t);
        assertTrue(m.contains("politique des balles"));
        assertTrue(m.contains("date de clôture des inscriptions"));
        // Le début des matchs se règle par tableau : plus exigé au niveau du tournoi.
        assertFalse(m.contains("date de début des matchs"));
    }

    @Test
    void P_exige_chaque_tableau_complet() {
        Tournoi t = tournoiHomologable();
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(10);
        t.setNumeroHomologation("NUM");
        t.setReglementDocument(new com.pingmanager.model.DocumentReference("R", "/r.pdf"));
        t.setInfosReglement(reglementPourPublication());
        // Ajout d'un tableau sans format ni capacité.
        t.ajouterTableau(new Tableau("Messieurs", ConfigurationTableau.builder().build()));
        var m = service.elementsManquantsPourPublication(t);
        assertTrue(m.stream().anyMatch(x -> x.contains("Messieurs") && x.contains("format")));
        assertTrue(m.stream().anyMatch(x -> x.contains("Messieurs") && x.contains("capacité")));
    }
}
