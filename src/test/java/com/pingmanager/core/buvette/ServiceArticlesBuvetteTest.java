package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceArticlesBuvetteTest {

    private final ServiceArticlesBuvette service = new ServiceArticlesBuvette();

    private static SaisieArticle saisie(String nom, FamilleArticle f, String prix, String lot, String stock,
                                        String seuil, Map<String, Integer> contenants) {
        return new SaisieArticle(null, nom, f, "", prix, lot, stock, seuil, contenants);
    }

    @Test
    void creer_un_produit_lie_a_ses_contenants() {
        StockBuvette s = new StockBuvette("08910012");
        assertTrue(service.enregistrer(s, saisie("Gobelet", FamilleArticle.CONTENANT, "4,50", "100", "150", "50",
                null)).accepte());
        assertTrue(service.enregistrer(s, saisie("Touillette", FamilleArticle.CONTENANT, "3", "1000", "500", "100",
                null)).accepte());
        Map<String, Integer> liens = new LinkedHashMap<>();
        liens.put("touillette", 1);
        liens.put("gobelet", 1);
        ResultatArticleBuvette r = service.enregistrer(s, new SaisieArticle(null, "Café", FamilleArticle.PRODUIT,
                "dose", "8,00", "125", "200", "40", liens));
        assertTrue(r.accepte());
        assertEquals("Article ajouté : Café.", r.explication());
        ArticleBuvette cafe = r.article();
        assertEquals("cafe", cafe.getId());
        assertEquals("dose", cafe.getUnite());
        assertEquals(800, cafe.getPrixLotCentimes());
        assertEquals(125, cafe.getUnitesParLot());
        assertEquals(200, cafe.getStock());
        assertEquals(40, cafe.getSeuilAlerte());
        assertEquals(List.of(new LienContenant("gobelet", 1), new LienContenant("touillette", 1)),
                cafe.getContenants(), "dans l'ordre des contenants du stock");
        assertEquals("0,112 €", Centimes.eurosPrecis(CalculCoutBuvette.portion(s, cafe).orElseThrow().complet()));
    }

    @Test
    void champs_vides_valeurs_par_defaut() {
        StockBuvette s = new StockBuvette("1");
        ArticleBuvette a = service.enregistrer(s, saisie("  Gâteau   maison ", null, "", "", "", "", null)).article();
        assertEquals("Gâteau maison", a.getNom());
        assertEquals(FamilleArticle.PRODUIT, a.getFamille());
        assertEquals("unité", a.getUnite());
        assertEquals(0, a.getPrixLotCentimes());
        assertEquals(1, a.getUnitesParLot());
        assertEquals(0, a.getStock());
        assertEquals(0, a.getSeuilAlerte());
    }

    @Test
    void saisies_invalides_refusees_avec_un_motif_precis() {
        StockBuvette s = StockExemple.cafe();
        assertEquals(ResultatArticleBuvette.Motif.NOM_OBLIGATOIRE,
                service.enregistrer(s, saisie(" ", null, "", "", "", "", null)).motif());
        assertEquals(ResultatArticleBuvette.Motif.NOM_EN_DOUBLE,
                service.enregistrer(s, saisie("CAFE", null, "", "", "", "", null)).motif());
        assertEquals(ResultatArticleBuvette.Motif.PRIX_INVALIDE,
                service.enregistrer(s, saisie("Thé", null, "-1", "", "", "", null)).motif());
        assertEquals(ResultatArticleBuvette.Motif.LOT_INVALIDE,
                service.enregistrer(s, saisie("Thé", null, "", "0", "", "", null)).motif());
        assertEquals(ResultatArticleBuvette.Motif.STOCK_INVALIDE,
                service.enregistrer(s, saisie("Thé", null, "", "", "-3", "", null)).motif());
        assertEquals(ResultatArticleBuvette.Motif.SEUIL_INVALIDE,
                service.enregistrer(s, saisie("Thé", null, "", "", "", "1,5", null)).motif());
        assertEquals(ResultatArticleBuvette.Motif.CONTENANT_INVALIDE,
                service.enregistrer(s, saisie("Thé", null, "", "", "", "", Map.of("cafe", 1))).motif(),
                "un produit n'est pas un contenant");
        assertEquals(ResultatArticleBuvette.Motif.CONTENANT_INVALIDE,
                service.enregistrer(s, saisie("Thé", null, "", "", "", "", Map.of("gobelet", 0))).motif());
        assertEquals(3, s.getArticles().size());
    }

    @Test
    void un_contenant_n_a_pas_de_contenants() {
        StockBuvette s = StockExemple.cafe();
        ArticleBuvette a = service.enregistrer(s, saisie("Assiette", FamilleArticle.CONTENANT, "", "", "", "",
                Map.of("gobelet", 1))).article();
        assertTrue(a.getContenants().isEmpty());
    }

    @Test
    void modifier_garde_le_stock_qui_ne_change_que_par_les_boutons() {
        StockBuvette s = StockExemple.cafe();
        ResultatArticleBuvette r = service.enregistrer(s, new SaisieArticle("cafe", "Café", FamilleArticle.PRODUIT,
                "dose", "9,00", "125", "999", "30", Map.of("gobelet", 1)));
        assertTrue(r.accepte());
        assertEquals(ResultatArticleBuvette.Motif.MODIFIE, r.motif());
        ArticleBuvette cafe = s.article("cafe").orElseThrow();
        assertEquals(200, cafe.getStock());
        assertEquals(30, cafe.getSeuilAlerte());
        assertEquals(900, cafe.getPrixLotCentimes());
        assertEquals(List.of(new LienContenant("gobelet", 1)), cafe.getContenants());
        assertEquals(List.of("gobelet", "touillette", "cafe"), s.getArticles().stream().map(ArticleBuvette::getId).toList(),
                "l'article garde sa place");
    }

    @Test
    void retirer_un_contenant_utilise_refuse_puis_accepte_une_fois_delie() {
        StockBuvette s = StockExemple.cafe();
        ResultatArticleBuvette refus = service.retirer(s, "gobelet");
        assertFalse(refus.accepte());
        assertEquals("Ce contenant sert encore à un produit : Café.", refus.explication());
        assertEquals(ResultatArticleBuvette.Motif.RETIRE, service.retirer(s, "cafe").motif());
        assertTrue(service.retirer(s, "gobelet").accepte());
        assertEquals(ResultatArticleBuvette.Motif.INCONNU, service.retirer(s, "gobelet").motif());
    }

    @Test
    void un_contenant_utilise_ne_devient_pas_un_produit() {
        StockBuvette s = StockExemple.cafe();
        assertEquals(ResultatArticleBuvette.Motif.FAMILLE_VERROUILLEE, service.enregistrer(s, new SaisieArticle(
                "gobelet", "Gobelet", FamilleArticle.PRODUIT, "", "", "", "", "", null)).motif());
    }
}
