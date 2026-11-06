package com.pingmanager.persistence;

import com.pingmanager.core.ServicePlanSalle;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.OrientationTable;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRepere;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Persistance du plan de salle (B024) : salles vides, étages négatifs, fichier abîmé. */
class PlanSalleMapperLimitesTest {

    private static Tournoi relire(Tournoi t) throws Exception {
        Path f = Files.createTempFile("salles", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        return DepotTournoi.charger(f).getTournoi();
    }

    @Test
    void salle_sans_table_etage_negatif_et_reservations_survivent_a_l_aller_retour() throws Exception {
        Tournoi t = new Tournoi("T", "C", LocalDate.of(2026, 11, 14));
        PlanSalle plein = PlanSalle.vide(10, 6)
                .ajouter(new TablePlan(7, 4, 2, OrientationTable.VERTICALE, "Dames")).getPlan()
                .ajouter(new ReperePlan(TypeRepere.AUTRE, "Buvette", 0, 0, 3, 1)).getPlan();
        t.setSalles(List.of(
                new SalleTournoi("Vide", 0, PlanSalle.vide(1, 1), Set.of(), Set.of()),
                new SalleTournoi("Cave", -1, plein, Set.of("Dames", "Open"), Set.of("Open"))));
        Tournoi relu = relire(t);
        assertEquals(t.getSalles(), relu.getSalles());
        assertEquals(-1, relu.getSalles().get(1).getEtage());
        assertEquals(Set.of("Dames", "Open"), relu.getSalles().get(1).getTableauxReserves());
        assertEquals(t.getSalles().get(1).getPlan(), relu.getSalles().get(1).getPlan());
        // les réservations dérivées du plan sont identiques avant/après
        ServicePlanSalle s = new ServicePlanSalle();
        assertEquals(s.reservations(t).tableauxAdmis(7), s.reservations(relu).tableauxAdmis(7));
    }

    @Test
    void ancien_fichier_sans_salles_se_relit_et_se_reecrit_sans_salles() throws Exception {
        Tournoi t = new Tournoi("T", "C", LocalDate.of(2026, 11, 14));
        Path f = Files.createTempFile("ancien", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        assertFalse(Files.readString(f).contains("\"salles\""));
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertFalse(relu.aUnPlanDeSalle());
        assertTrue(relu.getSalles().isEmpty());
        assertTrue(new ServicePlanSalle().reservations(relu).estVide());
    }

    @Test
    void salles_vides_donne_aucun_champ_et_relecture_sans_plan() throws Exception {
        Tournoi t = new Tournoi("T", "C", LocalDate.of(2026, 11, 14));
        t.setSalles(List.of(new SalleTournoi("A", PlanSalle.vide(2, 2))));
        t.setSalles(List.of());
        Path f = Files.createTempFile("vide", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        assertFalse(DepotTournoi.charger(f).getTournoi().aUnPlanDeSalle());
    }

    @Test
    void fichier_dont_le_plan_deborde_ou_double_un_numero_s_ouvre_en_ecartant_l_excedent() throws Exception {
        Tournoi t = new Tournoi("T", "C", LocalDate.of(2026, 11, 14));
        PlanSalle p = PlanSalle.vide(8, 2).ajouter(new TablePlan(1, 0, 0)).getPlan()
                .ajouter(new TablePlan(2, 2, 0)).getPlan().ajouter(new TablePlan(3, 4, 0)).getPlan();
        t.setSalles(List.of(new SalleTournoi("A", p)));
        Path f = Files.createTempFile("abime", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        String json = Files.readString(f);
        // Fabrique un doublon de numéro : la table 3 devient 2.
        String modifie = json.replaceAll("\"numero\"\\s*:\\s*3", "\"numero\": 2");
        assertNotEquals(json, modifie, "le champ numero doit exister dans le JSON");
        Files.writeString(f, modifie);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertEquals(2, relu.getSalles().get(0).getPlan().getTables().size());
    }
}
