package com.pingmanager.persistence;

import com.pingmanager.core.buvette.CatalogueBase;
import com.pingmanager.core.buvette.CommandeComptoir;
import com.pingmanager.core.buvette.ServiceArticlesBuvette;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Articles du club, réglage par tournoi et ventes de la buvette (B026) : aller-retour JSON. */
class VentesBuvetteRoundTripTest {

    @TempDir
    Path dossier;

    private static void remplir(StockBuvette s) {
        new ServiceArticlesBuvette().ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("cafe", "eau"));
        s.enregistrer(s.article("eau").orElseThrow().avecStock(10));
        s.enregistrer(s.article("cafe").orElseThrow().avecStock(50));
        s.enregistrer(s.article("gobelet-chaud").orElseThrow().avecStock(100));
        s.enregistrer(s.article("touillette").orElseThrow().avecStock(100)); // sucre laissé à 0
        ServiceVentesBuvette v = new ServiceVentesBuvette(() -> Instant.parse("2026-11-21T09:00:00Z"));
        v.regler(s, "brunoy-2026", "Tournoi de Brunoy", ModeBuvette.STOCK_ET_VENTES);
        v.regler(s, "brunoy-2025", "Ancien tournoi", ModeBuvette.STOCK_SEUL); // vide : non écrit
        CommandeComptoir c = new CommandeComptoir();
        c.ajouter("cafe");
        c.ajouter("eau");
        v.vendre(s, "brunoy-2026", c, MoyenPaiement.CARTE, "Claire"); // sucre manquant : à recompter
        v.vendre(s, "brunoy-2026", c, MoyenPaiement.ESPECES, "Claire");
        v.annulerDerniere(s, "brunoy-2026", "Paul");
        new ServiceArticlesBuvette().archiver(s, "eau");
    }

    @Test
    void articles_du_club_reglage_et_ventes_relus_a_l_identique() {
        DepotApplication depot = new DepotApplication();
        remplir(depot.stockBuvette("08910012"));
        Path f = dossier.resolve("app.json");
        depot.sauvegarder(f);

        StockBuvette relu = DepotApplication.charger(f).stockBuvette("08910012");
        StockBuvette attendu = new StockBuvette("08910012");
        remplir(attendu);
        assertEquals(attendu.getArticles(), relu.getArticles());
        assertEquals(attendu.getJournal(), relu.getJournal());
        assertEquals(attendu.getARecompter(), relu.getARecompter());
        assertTrue(relu.estARecompter("sucre"));

        ArticleBuvette cafe = relu.article("cafe").orElseThrow();
        assertEquals("cafe", cafe.getIdCatalogue());
        assertEquals(100, cafe.getPrixVenteCentimes());
        assertEquals(RayonBuvette.BOISSONS_CHAUDES, cafe.getRayon());
        assertTrue(relu.article("eau").orElseThrow().estArchive());

        assertEquals(1, relu.getTournois().size());
        BuvetteTournoi t = relu.tournoi("brunoy-2026").orElseThrow();
        assertEquals("Tournoi de Brunoy", t.getNomTournoi());
        assertEquals(ModeBuvette.STOCK_ET_VENTES, t.getMode());
        assertEquals(attendu.tournoi("brunoy-2026").orElseThrow().getVentes(), t.getVentes());
        assertEquals(2, t.getVentes().size());
        assertFalse(t.getVentes().get(0).estAnnulee());
        assertEquals(java.util.List.of("Sucre en dosette"), t.getVentes().get(0).aRecompter());
        assertTrue(t.getVentes().get(1).estAnnulee());
        assertEquals("Paul", t.getVentes().get(1).annulation().auteur());
        assertEquals(3, t.prochainNumero());
    }

    @Test
    void fichier_d_avant_les_ventes_relu_mode_stock_seulement_sans_article_du_catalogue() throws Exception {
        String ancien = "{\"version\":1,\"stocksBuvette\":[{\"club\":\"08910012\",\"articles\":["
                + "{\"id\":\"cafe\",\"nom\":\"Café\",\"famille\":\"PRODUIT\",\"unite\":\"dose\",\"prixLot\":800,"
                + "\"unitesParLot\":125,\"stock\":200,\"seuil\":40},"
                + "{\"id\":\"gobelet\",\"nom\":\"Gobelet\",\"famille\":\"CONTENANT\",\"stock\":10}],"
                + "\"journal\":[]}]}";
        Path f = dossier.resolve("ancien.json");
        Files.writeString(f, ancien, StandardCharsets.UTF_8);

        StockBuvette s = DepotApplication.charger(f).stockBuvette("08910012");
        ArticleBuvette cafe = s.article("cafe").orElseThrow();
        assertEquals(0, cafe.getPrixVenteCentimes());
        assertFalse(cafe.estArchive());
        assertEquals("", cafe.getIdCatalogue());
        assertEquals(RayonBuvette.AUTRE, cafe.getRayon());
        assertEquals(RayonBuvette.CONTENANTS, s.article("gobelet").orElseThrow().getRayon());
        assertTrue(s.getTournois().isEmpty());
        assertTrue(s.getARecompter().isEmpty());
        assertEquals(ModeBuvette.STOCK_SEUL, ServiceVentesBuvette.mode(s, "n-importe-quel-tournoi"));
    }

    @Test
    void fichier_d_application_sans_buvette_donne_des_articles_du_club_vides() throws Exception {
        Path f = dossier.resolve("vide.json");
        Files.writeString(f, "{\"version\":1}", StandardCharsets.UTF_8);
        StockBuvette s = DepotApplication.charger(f).stockBuvette("08910012");
        assertTrue(s.estVide());
        assertTrue(s.getTournois().isEmpty());
    }

    @Test
    void vente_abimee_ou_d_une_origine_future_ecartee_les_autres_relues() throws Exception {
        String json = "{\"version\":1,\"stocksBuvette\":[{\"club\":\"08910012\",\"articles\":[],\"journal\":[],"
                + "\"aRecompter\":[\"inconnu\"],"
                + "\"tournois\":[{\"ref\":\"t1\",\"nom\":\"T1\",\"mode\":\"MODE_FUTUR\",\"ventes\":["
                + "{\"n\":1,\"t\":\"2026-11-21T09:00:00Z\",\"moyen\":\"CHEQUE\",\"origine\":\"COMPTOIR\","
                + "\"lignes\":[{\"article\":\"cafe\",\"nom\":\"Café\",\"q\":2,\"prix\":100,\"cout\":\"11.2\"}]},"
                + "{\"n\":2,\"t\":\"2026-11-21T09:01:00Z\",\"origine\":\"COMMANDE_EN_LIGNE\",\"lignes\":"
                + "[{\"article\":\"cafe\",\"nom\":\"Café\",\"q\":1,\"prix\":100,\"cout\":\"11.2\"}]},"
                + "{\"n\":3,\"t\":\"pas une date\",\"lignes\":[]},"
                + "{\"n\":1,\"t\":\"2026-11-21T09:02:00Z\",\"lignes\":"
                + "[{\"article\":\"cafe\",\"nom\":\"Café\",\"q\":1,\"prix\":100}]}]},"
                + "{\"pas\":\"de référence\"}]}]}";
        Path f = dossier.resolve("abime.json");
        Files.writeString(f, json, StandardCharsets.UTF_8);

        StockBuvette s = DepotApplication.charger(f).stockBuvette("08910012");
        assertTrue(s.getARecompter().isEmpty(), "article inconnu : signalement écarté");
        assertEquals(1, s.getTournois().size());
        BuvetteTournoi t = s.tournoi("t1").orElseThrow();
        assertEquals(ModeBuvette.STOCK_SEUL, t.getMode());
        assertEquals(1, t.getVentes().size(), "seule la vente n° 1 lisible est gardée (doublon écarté)");
        assertEquals(MoyenPaiement.AUTRE, t.getVentes().get(0).moyen());
        assertEquals(200, t.getVentes().get(0).totalCentimes());
    }
}
