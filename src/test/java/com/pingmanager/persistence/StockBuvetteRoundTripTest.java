package com.pingmanager.persistence;

import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.NatureMouvement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockBuvetteRoundTripTest {

    @TempDir
    Path dossier;

    private static void remplir(StockBuvette s) {
        s.enregistrer(ArticleBuvette.builder().id("gobelet").nom("Gobelet").famille(FamilleArticle.CONTENANT)
                .unite("gobelet").lot(450, 100).stock(150).seuilAlerte(50).build());
        s.enregistrer(ArticleBuvette.builder().id("cafe").nom("Café").unite("dose").lot(800, 125).stock(200)
                .seuilAlerte(40).contenants(List.of(new LienContenant("gobelet", 2))).build());
        new ServiceStockBuvette(() -> Instant.parse("2026-11-14T09:30:00Z")).sortir(s, "cafe", 3, "Claire");
    }

    @Test
    void articles_liens_et_journal_relus_a_l_identique() {
        DepotApplication depot = new DepotApplication();
        remplir(depot.stockBuvette("08910012"));
        Path f = dossier.resolve("app.json");
        depot.sauvegarder(f);

        StockBuvette relu = DepotApplication.charger(f).stockBuvette("08910012");
        StockBuvette attendu = new StockBuvette("08910012");
        remplir(attendu);
        assertEquals(attendu.getArticles(), relu.getArticles());
        assertEquals(attendu.getJournal(), relu.getJournal());
        assertEquals(197, relu.article("cafe").orElseThrow().getStock());
        assertEquals(144, relu.article("gobelet").orElseThrow().getStock());
        assertEquals(2, relu.prochaineOperation());
    }

    @Test
    void stock_vide_non_ecrit_ancien_fichier_inchange() throws Exception {
        DepotApplication depot = new DepotApplication();
        depot.stockBuvette("08910012");
        Path f = dossier.resolve("app.json");
        depot.sauvegarder(f);
        assertFalse(Files.readString(f).contains("stocksBuvette"));
        assertTrue(DepotApplication.charger(f).stockBuvette("08910012").estVide());
    }

    @Test
    void ancien_catalogue_b046_garde_tel_quel_et_fichier_relu() throws Exception {
        String ancien = "{\"version\":1,\"buvettes\":[{\"club\":\"08910012\",\"alcoolActive\":false,"
                + "\"vendus\":[\"cafe\"],\"produits\":[{\"id\":\"cafe\",\"nom\":\"Café\",\"categorie\":"
                + "\"BOISSONS_CHAUDES\",\"unite\":\"GOBELET\",\"prix\":100,\"actif\":true}],"
                + "\"ingredients\":[{\"id\":\"baguette\",\"nom\":\"Baguette\",\"prixAchat\":110,"
                + "\"rendementMin\":2,\"rendementMax\":3}]}]}";
        Path f = dossier.resolve("ancien.json");
        Files.writeString(f, ancien, StandardCharsets.UTF_8);

        DepotApplication depot = DepotApplication.charger(f);
        assertTrue(depot.stockBuvette("08910012").estVide(), "l'ancien catalogue n'est pas converti");
        remplir(depot.stockBuvette("08910012"));
        Path g = dossier.resolve("reecrit.json");
        depot.sauvegarder(g);
        String reecrit = Files.readString(g);
        assertTrue(reecrit.contains("\"rendementMax\""), "l'ancienne section est recopiée, rien n'est perdu");
        assertTrue(reecrit.contains("\"vendus\""));
        assertEquals(2, DepotApplication.charger(g).stockBuvette("08910012").getArticles().size());
    }

    @Test
    void lignes_illisibles_ou_d_une_version_plus_recente_ecartees() throws Exception {
        String json = "{\"version\":1,\"stocksBuvette\":[{\"club\":\"08910012\",\"articles\":["
                + "{\"id\":\"cafe\",\"nom\":\"Café\",\"famille\":\"INCONNUE\",\"stock\":-5,\"unitesParLot\":0,"
                + "\"contenants\":[{\"contenant\":\"g\",\"quantite\":0},{\"contenant\":\"g\"}]},"
                + "{\"nom\":\"sans identifiant\"}],"
                + "\"journal\":[{\"op\":1,\"t\":\"2026-11-14T09:30:00Z\",\"article\":\"cafe\",\"nature\":\"SORTIE\","
                + "\"variation\":-1,\"stockApres\":4,\"origine\":\"COMMANDE\",\"reference\":\"C-1\"},"
                + "{\"op\":2,\"t\":\"pas une date\",\"article\":\"cafe\",\"nature\":\"SORTIE\",\"variation\":-1,"
                + "\"stockApres\":3,\"origine\":\"SAISIE_MANUELLE\"},"
                + "{\"op\":3,\"t\":\"2026-11-14T09:31:00Z\",\"article\":\"cafe\",\"nature\":\"ENTREE\","
                + "\"variation\":2,\"stockApres\":5,\"origine\":\"SAISIE_MANUELLE\"}]},"
                + "{\"pas\":\"de club\"}]}";
        Path f = dossier.resolve("abime.json");
        Files.writeString(f, json, StandardCharsets.UTF_8);

        StockBuvette s = DepotApplication.charger(f).stockBuvette("08910012");
        assertEquals(1, s.getArticles().size());
        ArticleBuvette cafe = s.article("cafe").orElseThrow();
        assertEquals(FamilleArticle.PRODUIT, cafe.getFamille());
        assertEquals(0, cafe.getStock());
        assertEquals(1, cafe.getUnitesParLot());
        assertEquals(List.of(new LienContenant("g", 1)), cafe.getContenants());
        assertEquals(1, s.getJournal().size());
        MouvementStock m = s.getJournal().get(0);
        assertEquals(NatureMouvement.ENTREE, m.nature());
        assertEquals(OrigineMouvement.SAISIE_MANUELLE, m.origine());
        assertEquals("inconnu", m.auteur());
        assertEquals(4, s.prochaineOperation());
    }
}
