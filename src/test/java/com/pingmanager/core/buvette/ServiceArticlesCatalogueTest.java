package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.ArticleCatalogue;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Articles du club copiés du catalogue de base, créés de zéro, archivés (B026). */
class ServiceArticlesCatalogueTest {

    private final ServiceArticlesBuvette service = new ServiceArticlesBuvette();

    @Test
    void cocher_le_cafe_ajoute_le_cafe_et_ses_trois_contenants_stock_a_zero() {
        StockBuvette s = new StockBuvette("08910012");
        ResultatAjoutCatalogue r = service.ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("cafe"));
        assertTrue(r.accepte());
        assertEquals(4, s.getArticles().size());
        ArticleBuvette cafe = s.article("cafe").orElseThrow();
        assertEquals("cafe", cafe.getIdCatalogue());
        assertEquals(100, cafe.getPrixVenteCentimes());
        assertEquals(0, cafe.getStock());
        assertEquals(40, cafe.getSeuilAlerte());
        assertEquals(RayonBuvette.BOISSONS_CHAUDES, cafe.getRayon());
        assertEquals(List.of(new LienContenant("gobelet-chaud", 1), new LienContenant("touillette", 1),
                new LienContenant("sucre", 1)), cafe.getContenants());
        assertEquals(FamilleArticle.CONTENANT, s.article("sucre").orElseThrow().getFamille());
        assertTrue(r.explication().startsWith("4 articles ajoutés"), r.explication());
    }

    @Test
    void modifier_prix_et_composition_d_un_article_du_club_ne_change_pas_le_catalogue() {
        StockBuvette s = new StockBuvette("08910012");
        service.ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("cafe"));
        ArticleCatalogue avant = CatalogueBase.catalogue().article("cafe").orElseThrow();

        // Le club vend son café 1,20 €, sans sucre, et le renomme.
        ResultatArticleBuvette r = service.enregistrer(s, new SaisieArticle("cafe", "Café du club",
                FamilleArticle.PRODUIT, "dose", "15,00", "125", "", "30",
                Map.of("gobelet-chaud", 1, "touillette", 1), "1,20", null));
        assertTrue(r.accepte(), r.explication());

        ArticleBuvette cafe = s.article("cafe").orElseThrow();
        assertEquals("Café du club", cafe.getNom());
        assertEquals(120, cafe.getPrixVenteCentimes());
        assertEquals(1500, cafe.getPrixLotCentimes());
        assertEquals(2, cafe.getContenants().size());
        assertEquals(RayonBuvette.BOISSONS_CHAUDES, cafe.getRayon());
        assertEquals("cafe", cafe.getIdCatalogue());

        ArticleCatalogue apres = CatalogueBase.catalogue().article("cafe").orElseThrow();
        assertEquals(avant, apres);
        assertEquals("Café", apres.nom());
        assertEquals(100, apres.prixVenteCentimes());
        assertEquals(1200, apres.prixLotCentimes());
        assertEquals(3, apres.composition().size());
    }

    @Test
    void cocher_deux_fois_n_ajoute_rien_et_les_contenants_sont_partages() {
        StockBuvette s = new StockBuvette("08910012");
        service.ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("cafe"));
        ResultatAjoutCatalogue r = service.ajouterDepuisCatalogue(s, CatalogueBase.catalogue(),
                Set.of("cafe", "the"));
        assertEquals(List.of("Thé"), r.ajoutes());
        assertEquals(List.of("Café"), r.dejaPresents());
        assertEquals(5, s.getArticles().size()); // le thé réutilise gobelet, touillette et sucre
        assertEquals(s.article("cafe").orElseThrow().getContenants(), s.article("the").orElseThrow().getContenants());
    }

    @Test
    void contenant_deja_saisi_a_la_main_sous_le_meme_nom_est_reutilise() {
        StockBuvette s = new StockBuvette("08910012");
        service.enregistrer(s, new SaisieArticle(null, "Touillette en bois", FamilleArticle.CONTENANT, "touillette",
                "3,00", "1000", "200", "50", Map.of()));
        service.ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("chocolat-chaud"));
        assertEquals(3, s.getArticles().size());
        assertTrue(s.article("chocolat-chaud").orElseThrow().getContenants()
                .contains(new LienContenant("touillette-en-bois", 1)));
        assertEquals(200, s.article("touillette-en-bois").orElseThrow().getStock());
    }

    @Test
    void rien_coche_message_d_aide() {
        ResultatAjoutCatalogue r = service.ajouterDepuisCatalogue(new StockBuvette("1"), CatalogueBase.catalogue(),
                Set.of());
        assertFalse(r.accepte());
        assertEquals("Cochez au moins un article du catalogue.", r.explication());
    }

    @Test
    void article_cree_de_zero_avec_prix_de_vente_et_rayon() {
        StockBuvette s = new StockBuvette("08910012");
        service.enregistrer(s, new SaisieArticle(null, "Barquette", FamilleArticle.CONTENANT, "barquette", "4,00",
                "100", "50", "10", Map.of()));
        ResultatArticleBuvette r = service.enregistrer(s, new SaisieArticle(null, "Frites maison",
                FamilleArticle.PRODUIT, "cornet", "5,00", "20", "40", "10", Map.of("barquette", 1), "2,00",
                RayonBuvette.SALE));
        assertTrue(r.accepte(), r.explication());
        ArticleBuvette frites = s.article("frites-maison").orElseThrow();
        assertEquals("", frites.getIdCatalogue());
        assertEquals(200, frites.getPrixVenteCentimes());
        assertEquals(RayonBuvette.SALE, frites.getRayon());
        assertEquals(40, frites.getStock());
        assertTrue(CatalogueBase.catalogue().getArticles().stream().noneMatch(a -> a.nom().equals("Frites maison")));
    }

    @Test
    void prix_de_vente_illisible_refuse() {
        ResultatArticleBuvette r = service.enregistrer(new StockBuvette("1"), new SaisieArticle(null, "Eau",
                FamilleArticle.PRODUIT, "bouteille", "3", "24", "", "", Map.of(), "un euro", null));
        assertEquals(ResultatArticleBuvette.Motif.PRIX_VENTE_INVALIDE, r.motif());
    }

    @Test
    void saisie_sans_prix_de_vente_garde_le_prix_et_le_rayon_existants() {
        StockBuvette s = new StockBuvette("1");
        service.ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("eau"));
        service.enregistrer(s, new SaisieArticle("eau", "Eau 50 cl", FamilleArticle.PRODUIT, "bouteille", "3,00",
                "24", "", "24", Map.of()));
        assertEquals(100, s.article("eau").orElseThrow().getPrixVenteCentimes());
        assertEquals(RayonBuvette.BOISSONS_FROIDES, s.article("eau").orElseThrow().getRayon());
    }

    @Test
    void archiver_retire_de_la_vente_sans_perdre_l_historique_puis_remettre_en_vente() {
        StockBuvette s = StockExemple.cafe();
        s.enregistrer(s.article("cafe").orElseThrow().modifier().prixVenteCentimes(100).build());
        ServiceVentesBuvette ventes = new ServiceVentesBuvette(() -> Instant.parse("2026-11-21T09:00:00Z"));
        ventes.regler(s, "brunoy", "Tournoi de Brunoy", ModeBuvette.STOCK_ET_VENTES);
        CommandeComptoir c = new CommandeComptoir();
        c.ajouter("cafe");
        ventes.vendre(s, "brunoy", c, MoyenPaiement.ESPECES, "Claire");

        assertEquals(ResultatArticleBuvette.Motif.A_UN_HISTORIQUE, service.supprimer(s, "cafe").motif());
        ResultatArticleBuvette r = service.archiver(s, "cafe");
        assertTrue(r.accepte(), r.explication());
        assertTrue(s.article("cafe").orElseThrow().estArchive());
        assertTrue(ServiceVentesBuvette.articlesEnVente(s).isEmpty());
        assertEquals(1, s.tournoi("brunoy").orElseThrow().getVentes().size());
        assertEquals(1, s.articlesArchives().size());

        assertTrue(service.remettreEnVente(s, "cafe").accepte());
        assertFalse(s.article("cafe").orElseThrow().estArchive());
        assertEquals(ResultatArticleBuvette.Motif.NON_ARCHIVE, service.remettreEnVente(s, "cafe").motif());
    }

    @Test
    void archiver_un_contenant_encore_utilise_par_un_produit_en_vente_est_refuse() {
        StockBuvette s = StockExemple.cafe();
        assertEquals(ResultatArticleBuvette.Motif.CONTENANT_UTILISE, service.archiver(s, "gobelet").motif());
        service.archiver(s, "cafe");
        assertTrue(service.archiver(s, "gobelet").accepte());
        // Remettre le café en vente remet aussi son gobelet.
        service.remettreEnVente(s, "cafe");
        assertFalse(s.article("gobelet").orElseThrow().estArchive());
    }

    @Test
    void article_sans_historique_peut_etre_supprime() {
        StockBuvette s = new StockBuvette("1");
        service.enregistrer(s, new SaisieArticle(null, "Erreur", FamilleArticle.PRODUIT, "", "", "", "", "",
                Map.of()));
        assertEquals(ResultatArticleBuvette.Motif.RETIRE, service.supprimer(s, "erreur").motif());
        assertTrue(s.getArticles().isEmpty());
    }

    @Test
    void nom_d_un_article_archive_signale_en_double() {
        StockBuvette s = StockExemple.cafe();
        service.archiver(s, "cafe");
        ResultatArticleBuvette r = service.enregistrer(s, new SaisieArticle(null, "café", FamilleArticle.PRODUIT,
                "", "", "", "", "", Map.of()));
        assertEquals(ResultatArticleBuvette.Motif.NOM_EN_DOUBLE, r.motif());
        assertTrue(r.explication().contains("retiré de la vente"), r.explication());
    }
}
