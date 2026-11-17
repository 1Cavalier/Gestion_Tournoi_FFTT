package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleCatalogue;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.RayonBuvette;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueBaseTest {

    @Test
    void catalogue_livre_une_trentaine_d_articles_dans_chaque_rayon() {
        CatalogueBuvette c = CatalogueBase.catalogue();
        assertTrue(c.taille() >= 30, "articles : " + c.taille());
        for (RayonBuvette r : List.of(RayonBuvette.BOISSONS_CHAUDES, RayonBuvette.BOISSONS_FROIDES, RayonBuvette.SALE,
                RayonBuvette.SUCRE, RayonBuvette.CONTENANTS)) {
            assertTrue(c.rayon(r).size() >= 3, r + " : " + c.rayon(r).size());
        }
    }

    @Test
    void cafe_une_dose_un_gobelet_une_touillette_un_sucre() {
        ArticleCatalogue cafe = CatalogueBase.catalogue().article("cafe").orElseThrow();
        assertEquals("Café", cafe.nom());
        assertEquals("dose", cafe.unite());
        assertEquals(RayonBuvette.BOISSONS_CHAUDES, cafe.rayon());
        assertEquals(List.of(new LienContenant("gobelet-chaud", 1), new LienContenant("touillette", 1),
                new LienContenant("sucre", 1)), cafe.composition());
        assertEquals(100, cafe.prixVenteCentimes());
        assertEquals(1200, cafe.prixLotCentimes());
        assertEquals(125, cafe.unitesParLot());
    }

    @Test
    void chaque_produit_a_un_prix_de_vente_et_chaque_contenant_aucune_composition() {
        for (ArticleCatalogue a : CatalogueBase.catalogue().getArticles()) {
            if (a.famille() == FamilleArticle.PRODUIT) {
                assertTrue(a.prixVenteCentimes() > 0, a.id());
                assertTrue(a.prixLotCentimes() > 0, a.id());
            } else {
                assertTrue(a.composition().isEmpty(), a.id());
                assertEquals(0, a.prixVenteCentimes(), a.id());
            }
        }
    }

    @Test
    void ligne_illisible_signalee_avec_son_numero() {
        String csv = "# en-tête\ncafe;BOISSONS_CHAUDES;Café;dose;douze;125;1,00;40;\n";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CatalogueBase.lire(new StringReader(csv)));
        assertTrue(e.getMessage().contains("ligne 2"), e.getMessage());
    }

    @Test
    void composition_vers_un_article_absent_refusee() {
        String csv = "cafe;BOISSONS_CHAUDES;Café;dose;12,00;125;1,00;40;gobelet*1\n";
        assertThrows(IllegalArgumentException.class, () -> CatalogueBase.lire(new StringReader(csv)));
    }

    @Test
    void identifiant_en_double_refuse() {
        String csv = "eau;BOISSONS_FROIDES;Eau;bouteille;3,00;24;1,00;24;\n"
                + "eau;BOISSONS_FROIDES;Eau plate;bouteille;3,00;24;1,00;24;\n";
        assertThrows(IllegalArgumentException.class, () -> CatalogueBase.lire(new StringReader(csv)));
    }

    @Test
    void lignes_vides_et_commentaires_ignores_quantite_par_defaut_un() throws Exception {
        String csv = "\n# x\ngobelet;CONTENANTS;Gobelet;gobelet;4,90;100;;50;\n\n"
                + "the;BOISSONS_CHAUDES;Thé;sachet;3,00;50;1,00;20;gobelet\n";
        CatalogueBuvette c = CatalogueBase.lire(new StringReader(csv));
        assertEquals(2, c.taille());
        assertEquals(List.of(new LienContenant("gobelet", 1)), c.article("the").orElseThrow().composition());
    }
}
