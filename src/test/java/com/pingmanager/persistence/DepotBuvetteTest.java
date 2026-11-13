package com.pingmanager.persistence;

import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.TypeMouvementStock;
import com.pingmanager.persistence.json.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B046 : buvette d'un tournoi dans un fichier voisin ; le fichier du tournoi n'est pas touché. */
class DepotBuvetteTest {

    @TempDir
    Path dossier;

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("soda").nom("Soda").rendement(24).prixAchatCentimes(1200)
                .build());
        return c;
    }

    @Test
    void fichier_voisin_du_fichier_du_tournoi() {
        assertEquals(dossier.resolve("Open_2027.buvette.json"),
                DepotBuvette.fichierPour(dossier.resolve("Open_2027" + DepotTournoi.EXTENSION)));
        assertEquals(dossier.resolve("autre.json.buvette.json"), DepotBuvette.fichierPour(dossier.resolve("autre.json")));
    }

    @Test
    void tournoi_sans_buvette_donne_une_buvette_vide() {
        assertTrue(DepotBuvette.charger(dossier.resolve("t" + DepotTournoi.EXTENSION)).estVide());
    }

    @Test
    void aller_retour_du_stock_des_seuils_et_des_corrections() {
        CatalogueBuvette c = catalogue();
        ServiceStockBuvette s = new ServiceStockBuvette(() -> Instant.parse("2026-11-14T08:00:00Z"));
        BuvetteTournoi b = new BuvetteTournoi();
        s.acheter(b, c, "soda", 2, 2400, LocalDate.of(2026, 11, 13), "Quentin");
        s.inventaire(b, c, "soda", 40, "Quentin");
        s.corriger(b, 1, "Quentin", null);
        s.definirSeuil(b, c, "soda", 12);
        Path tournoi = dossier.resolve("t" + DepotTournoi.EXTENSION);
        DepotBuvette.sauvegarder(b, tournoi);
        assertFalse(Files.exists(tournoi), "le fichier du tournoi n'est ni créé ni modifié");
        BuvetteTournoi relue = DepotBuvette.charger(tournoi);
        assertEquals(b.getMouvements(), relue.getMouvements());
        assertEquals(b.getSeuils(), relue.getSeuils());
    }

    @Test
    void ligne_abimee_ou_type_futur_ecartes_sans_bloquer() {
        String texte = "{\"version\":1,\"kind\":\"buvette\",\"seuils\":{\"soda\":\"beaucoup\",\"eau\":3},"
                + "\"mouvements\":[{\"numero\":1,\"type\":\"ACHAT\",\"ingredient\":\"soda\",\"quantite\":24,"
                + "\"horodatage\":\"2026-11-14T08:00:00Z\",\"auteur\":\"A\"},"
                + "{\"numero\":2,\"type\":\"TELEPORTATION\",\"ingredient\":\"soda\",\"quantite\":1,"
                + "\"horodatage\":\"2026-11-14T08:00:00Z\"},"
                + "{\"numero\":3,\"type\":\"PERTE\",\"ingredient\":\"soda\",\"quantite\":\"x\"}]}";
        BuvetteTournoi b = DepotBuvette.lire(JsonParser.analyser(texte));
        assertEquals(1, b.getMouvements().size());
        assertEquals(List.of("eau"), List.copyOf(b.getSeuils().keySet()));
    }

    @Test
    void ancien_fichier_de_tournoi_relu_a_l_identique_la_buvette_n_y_ecrit_rien() throws Exception {
        Tournoi t = new Tournoi("Open", "Club", List.of(LocalDate.of(2027, 1, 10)));
        Path f = dossier.resolve("Open" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        String avant = Files.readString(f);
        BuvetteTournoi b = DepotBuvette.charger(f);
        new ServiceStockBuvette(Instant::now).stockDepart(b, catalogue(), "soda", 3, "A");
        DepotBuvette.sauvegarder(b, f);
        assertEquals(avant, Files.readString(f));
        assertFalse(Files.readString(f).contains("buvette"));
        assertEquals(TypeMouvementStock.STOCK_DEPART, DepotBuvette.charger(f).getMouvements().get(0).type());
    }

    @Test
    void aller_retour_du_budget_previsionnel() {
        BuvetteTournoi b = new BuvetteTournoi();
        b.setBudget(com.pingmanager.model.buvette.BudgetBuvette.VIDE
                .avecJoueurs(LocalDate.of(2027, 1, 9), 120)
                .avecHypotheses(new com.pingmanager.model.buvette.HypothesesConsommation(20, 20, 80, 120,
                        java.util.Map.of("cafe", 50)))
                .avecQuantitesImposees(java.util.Map.of("the", 12L), "Open 2026")
                .avecDeduireStock(false));
        BuvetteTournoi relue = DepotBuvette.lire(DepotBuvette.ecrire(b));
        assertEquals(b.getBudget(), relue.getBudget());
        assertFalse(DepotBuvette.ecrire(new BuvetteTournoi()).has("budget"), "budget vide : non écrit");
    }

    @Test
    void hypotheses_hors_bornes_reviennent_aux_suggestions() {
        String texte = "{\"version\":1,\"budget\":{\"joueurs\":{\"2027-01-09\":50,\"pas-une-date\":3},"
                + "\"hypotheses\":{\"accompagnants\":9999}}}";
        BuvetteTournoi b = DepotBuvette.lire(JsonParser.analyser(texte));
        assertEquals(java.util.Map.of(LocalDate.of(2027, 1, 9), 50), b.getBudget().joueursSaisis());
        org.junit.jupiter.api.Assertions.assertNull(b.getBudget().hypotheses());
        assertTrue(b.getBudget().deduireStock());
    }

    @Test
    void version_plus_recente_refusee() {
        org.junit.jupiter.api.Assertions.assertThrows(com.pingmanager.persistence.json.JsonException.class,
                () -> DepotBuvette.lire(JsonParser.analyser("{\"version\":99}")));
    }

    @Test
    void aller_retour_des_ventes_et_annulations() {
        CatalogueBuvette c = catalogue();
        c.enregistrerProduit(com.pingmanager.model.buvette.Produit.builder().id("soda").nom("Soda")
                .prixVenteCentimes(150).recette(List.of(new com.pingmanager.model.buvette.LigneRecette("soda", 1)))
                .build());
        com.pingmanager.core.buvette.ServiceVentesBuvette caisse = new com.pingmanager.core.buvette.ServiceVentesBuvette(
                () -> Instant.parse("2027-01-09T09:00:00Z"));
        BuvetteTournoi b = new BuvetteTournoi();
        caisse.vendre(b, c, "soda", "Léa", "Caisse 1");
        caisse.vendre(b, c, "soda", "Léa", "Caisse 1");
        caisse.annulerDerniere(b, c, null, "Léa", "Caisse 1");
        BuvetteTournoi relue = DepotBuvette.lire(DepotBuvette.ecrire(b));
        assertEquals(b.getVentes(), relue.getVentes());
        assertEquals(b.getMouvements(), relue.getMouvements());
        assertTrue(relue.getVentes().get(2).estAnnulation());
    }
}
