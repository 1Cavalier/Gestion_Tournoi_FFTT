package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;

import java.util.List;

/** Stock d'exemple des tests de la buvette : café, gobelet, touillette. */
final class StockExemple {

    private StockExemple() {
    }

    /**
     * Café : 8,00 € le paquet de 125 doses (6,4 c la dose), 200 en stock, seuil 40.
     * Gobelet : 4,50 € les 100 (4,5 c), 150 en stock, seuil 50.
     * Touillette : 3,00 € les 1 000 (0,3 c), 500 en stock, seuil 100.
     * Un café = une dose + un gobelet + une touillette.
     */
    static StockBuvette cafe() {
        StockBuvette s = new StockBuvette("08910012");
        s.enregistrer(ArticleBuvette.builder().id("gobelet").nom("Gobelet").famille(FamilleArticle.CONTENANT)
                .unite("gobelet").lot(450, 100).stock(150).seuilAlerte(50).build());
        s.enregistrer(ArticleBuvette.builder().id("touillette").nom("Touillette").famille(FamilleArticle.CONTENANT)
                .unite("touillette").lot(300, 1000).stock(500).seuilAlerte(100).build());
        s.enregistrer(ArticleBuvette.builder().id("cafe").nom("Café").famille(FamilleArticle.PRODUIT)
                .unite("dose").lot(800, 125).stock(200).seuilAlerte(40)
                .contenants(List.of(new LienContenant("gobelet", 1), new LienContenant("touillette", 1))).build());
        return s;
    }
}
