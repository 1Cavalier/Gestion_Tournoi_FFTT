package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculCoutBuvetteTest {

    private static void egal(String attendu, BigDecimal obtenu) {
        assertEquals(0, new BigDecimal(attendu).compareTo(obtenu), "attendu " + attendu + ", obtenu " + obtenu);
    }

    @Test
    void cout_unitaire_prix_du_lot_divise_par_les_unites() {
        StockBuvette s = StockExemple.cafe();
        egal("6.4", CalculCoutBuvette.coutUnitaire(s.article("cafe").orElseThrow()));
        egal("4.5", CalculCoutBuvette.coutUnitaire(s.article("gobelet").orElseThrow()));
        egal("0.3", CalculCoutBuvette.coutUnitaire(s.article("touillette").orElseThrow()));
    }

    @Test
    void cafe_cout_brut_emballage_et_complet_separes() {
        StockBuvette s = StockExemple.cafe();
        CoutPortion c = CalculCoutBuvette.portion(s, s.article("cafe").orElseThrow()).orElseThrow();
        egal("6.4", c.brut());       // la dose de café
        egal("4.8", c.emballage());  // gobelet 4,5 c + touillette 0,3 c
        egal("11.2", c.complet());   // 0,112 € la portion
        assertEquals("0,064 €", Centimes.eurosPrecis(c.brut()));
        assertEquals("0,048 €", Centimes.eurosPrecis(c.emballage()));
        assertEquals("0,112 €", Centimes.eurosPrecis(c.complet()));
    }

    @Test
    void quantite_par_portion_multiplie_le_cout_du_contenant() {
        StockBuvette s = StockExemple.cafe();
        s.enregistrer(ArticleBuvette.builder().id("serviette").nom("Serviette").famille(FamilleArticle.CONTENANT)
                .lot(200, 100).build());
        s.enregistrer(ArticleBuvette.builder().id("sandwich").nom("Sandwich").lot(250, 1)
                .contenants(List.of(new LienContenant("serviette", 2))).build());
        CoutPortion c = CalculCoutBuvette.portion(s, s.article("sandwich").orElseThrow()).orElseThrow();
        egal("250", c.brut());
        egal("4", c.emballage());
        egal("254", c.complet());
        assertEquals("2,54 €", Centimes.eurosPrecis(c.complet()));
    }

    @Test
    void produit_sans_contenant_emballage_nul() {
        StockBuvette s = new StockBuvette("1");
        s.enregistrer(ArticleBuvette.builder().id("canette").nom("Canette").lot(1200, 24).build());
        CoutPortion c = CalculCoutBuvette.portion(s, s.article("canette").orElseThrow()).orElseThrow();
        egal("50", c.brut());
        egal("0", c.emballage());
        egal("50", c.complet());
    }

    @Test
    void lot_non_divisible_sans_arrondi_intermediaire() {
        StockBuvette s = new StockBuvette("1");
        s.enregistrer(ArticleBuvette.builder().id("g").nom("Gobelet").famille(FamilleArticle.CONTENANT)
                .lot(100, 3).build());
        s.enregistrer(ArticleBuvette.builder().id("p").nom("Thé").lot(0, 1)
                .contenants(List.of(new LienContenant("g", 3))).build());
        // 3 × (100 / 3) = 100 c à un millionième de centime près
        BigDecimal e = CalculCoutBuvette.portion(s, s.article("p").orElseThrow()).orElseThrow().emballage();
        assertTrue(e.subtract(BigDecimal.valueOf(100)).abs().compareTo(new BigDecimal("0.00001")) < 0);
        assertEquals("1,00 €", Centimes.eurosPrecis(e));
    }

    @Test
    void lien_vers_un_contenant_disparu_ignore_et_contenant_sans_cout_de_portion() {
        StockBuvette s = StockExemple.cafe();
        s.retirer("touillette");
        CoutPortion c = CalculCoutBuvette.portion(s, s.article("cafe").orElseThrow()).orElseThrow();
        egal("4.5", c.emballage());
        assertTrue(CalculCoutBuvette.portion(s, s.article("gobelet").orElseThrow()).isEmpty());
    }
}
