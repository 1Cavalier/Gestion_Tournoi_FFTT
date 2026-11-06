package com.pingmanager.persistence;

import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** B035 : l'historique des refus et le statut survivent à DepotTournoi. */
class RefusHomologationPersistanceTest {

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi(() -> Instant.parse("2026-11-06T08:00:00Z"));

    private Tournoi tournoi() {
        Tournoi t = new Tournoi("Open", "Brunoy CTT", LocalDate.of(2026, 3, 8));
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Paul", "p@x.fr", "1234567", GradeJugeArbitre.JA3));
        t.setNiveau(NiveauTournoi.PROMOTIONNEL);
        t.setPeriode(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8));
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        Tableau a = new Tableau("Dames", ConfigurationTableau.depuisFormat(com.pingmanager.model.format.RegistreFormats.formatParDefaut()).sexeRequis(Sexe.FEMME).build());
        a.setType("Toutes séries");
        a.setHoraires(new HorairesTableau(null, null, LocalDateTime.of(2026, 3, 8, 9, 0)));
        t.ajouterTableau(a);
        return t;
    }

    private Tournoi aller_retour(Tournoi t, Path dir) throws Exception {
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        return DepotTournoi.charger(f).getTournoi();
    }

    @Test
    void refus_multiples_et_motif_a_accents_survivent(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "Dates : « conflit » \"Critérium\"\nligne 2");
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "Second refus");
        Tournoi r = aller_retour(t, dir);
        assertEquals(StatutTournoi.BROUILLON, r.getStatut());
        assertEquals(t.getRefusHomologation(), r.getRefusHomologation());
        assertEquals("Second refus", r.getDernierRefusHomologation().orElseThrow().motif());
    }

    @Test
    void statut_demande_survit_et_reste_verrouille(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        cycle.demanderHomologation(t);
        Tournoi r = aller_retour(t, dir);
        assertEquals(StatutTournoi.DEMANDE_HOMOLOGATION, r.getStatut());
        assertTrue(r.champsHomologationVerrouilles());
    }

    @Test
    void refus_puis_homologation_survit(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "x");
        cycle.demanderHomologation(t);
        cycle.homologuer(t, "2026/9");
        Tournoi r = aller_retour(t, dir);
        assertEquals(StatutTournoi.HOMOLOGUE, r.getStatut());
        assertEquals(1, r.getRefusHomologation().size());
    }

    @Test
    void ancien_fichier_sans_le_champ_se_relit(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "x");
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        String json = Files.readString(f);
        assertTrue(json.contains("refusHomologation"));
        Files.writeString(f, json.replace("\"refusHomologation\"", "\"inconnuAutre\""));
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        assertTrue(r.getRefusHomologation().isEmpty());
        assertEquals("Open", r.getNom());
    }

    @Test
    void sans_refus_le_champ_n_est_pas_ecrit_et_la_relecture_est_vide(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        assertFalse(Files.readString(f).contains("refusHomologation"));
        assertTrue(DepotTournoi.charger(f).getTournoi().getRefusHomologation().isEmpty());
    }

    @Test
    void entree_de_refus_corrompue_est_ignoree(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        cycle.demanderHomologation(t);
        cycle.refuserHomologation(t, "motifOK");
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        Files.writeString(f, Files.readString(f).replace("\"motifOK\"", "\"  \""));
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        assertTrue(r.getRefusHomologation().isEmpty());
    }
}
