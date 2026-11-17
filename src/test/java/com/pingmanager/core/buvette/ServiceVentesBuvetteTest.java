package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.SortieVente;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.model.buvette.VenteBuvette;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ventes au comptoir et bilan de la buvette (B026). Chiffres vérifiés à la main :
 * <ul>
 *   <li>Café vendu 1,00 € ; coût d'une portion = dose 6,4 c + gobelet 4,5 c +
 *       touillette 0,3 c = <b>11,2 c</b>.</li>
 *   <li>Soda vendu 1,50 € ; 9,60 € les 24 canettes = <b>40 c</b>, sans contenant.</li>
 * </ul>
 */
class ServiceVentesBuvetteTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final String TOURNOI = "tournoi-brunoy-2026";

    private Instant maintenant = Instant.parse("2026-11-21T09:00:00Z");
    private final ServiceVentesBuvette service = new ServiceVentesBuvette(() -> maintenant);
    private StockBuvette stock;

    @BeforeEach
    void preparer() {
        stock = StockExemple.cafe();
        stock.enregistrer(stock.article("cafe").orElseThrow().modifier().prixVenteCentimes(100).build());
        stock.enregistrer(ArticleBuvette.builder().id("soda").nom("Soda").unite("canette").lot(960, 24).stock(24)
                .seuilAlerte(6).prixVenteCentimes(150).build());
        service.regler(stock, TOURNOI, "Tournoi de Brunoy", ModeBuvette.STOCK_ET_VENTES);
    }

    private ResultatVente vendre(Map<String, Integer> quantites, MoyenPaiement moyen) {
        CommandeComptoir c = new CommandeComptoir();
        quantites.forEach((id, q) -> {
            for (int i = 0; i < q; i++) {
                c.ajouter(id);
            }
        });
        return service.vendre(stock, TOURNOI, c, moyen, "Claire");
    }

    private long stockDe(String id) {
        return stock.article(id).orElseThrow().getStock();
    }

    @Test
    void une_vente_sort_le_produit_et_ses_contenants() {
        ResultatVente r = vendre(Map.of("cafe", 2), MoyenPaiement.ESPECES);
        assertTrue(r.accepte(), r.explication());
        assertEquals(198, stockDe("cafe"));
        assertEquals(148, stockDe("gobelet"));
        assertEquals(498, stockDe("touillette"));
        VenteBuvette v = r.vente();
        assertEquals(1, v.numero());
        assertEquals(200, v.totalCentimes());
        assertEquals(0, new BigDecimal("22.4").compareTo(v.coutTotal()));
        assertEquals(Instant.parse("2026-11-21T09:00:00Z"), v.horodatage());
        assertEquals("Vente n° 1 enregistrée : 2 articles, 2,00 € (espèces).", r.explication());
        for (MouvementStock m : stock.getJournal()) {
            assertEquals(OrigineMouvement.VENTE, m.origine());
            assertEquals(TOURNOI + "#1", m.reference());
        }
    }

    @Test
    void annuler_la_vente_remet_le_produit_et_les_contenants_et_la_garde_dans_l_historique() {
        vendre(Map.of("cafe", 2), MoyenPaiement.ESPECES);
        maintenant = Instant.parse("2026-11-21T09:05:00Z");
        ResultatVente r = service.annulerDerniere(stock, TOURNOI, "Paul");
        assertTrue(r.accepte(), r.explication());
        assertEquals(200, stockDe("cafe"));
        assertEquals(150, stockDe("gobelet"));
        assertEquals(500, stockDe("touillette"));
        BuvetteTournoi t = stock.tournoi(TOURNOI).orElseThrow();
        assertEquals(1, t.getVentes().size());
        VenteBuvette v = t.getVentes().get(0);
        assertTrue(v.estAnnulee());
        assertEquals("Paul", v.annulation().auteur());
        assertEquals(Instant.parse("2026-11-21T09:05:00Z"), v.annulation().horodatage());
        assertEquals(OrigineMouvement.ANNULATION_VENTE, stock.getJournal().get(stock.getJournal().size() - 1).origine());
        assertEquals(ResultatVente.Motif.DEJA_ANNULEE, service.annuler(stock, TOURNOI, 1, "Paul").motif());
        assertEquals(ResultatVente.Motif.AUCUNE_VENTE, service.annulerDerniere(stock, TOURNOI, "Paul").motif());
    }

    @Test
    void annuler_une_vente_plus_ancienne_depuis_l_historique() {
        vendre(Map.of("soda", 1), MoyenPaiement.CARTE);
        vendre(Map.of("soda", 2), MoyenPaiement.ESPECES);
        assertEquals(21, stockDe("soda"));
        assertTrue(service.annuler(stock, TOURNOI, 1, "Paul").accepte());
        assertEquals(22, stockDe("soda"));
        // La dernière vente valable est toujours la n° 2.
        assertEquals(2, stock.tournoi(TOURNOI).orElseThrow().derniereVenteValable().orElseThrow().numero());
        assertEquals(3, stock.tournoi(TOURNOI).orElseThrow().prochainNumero());
    }

    @Test
    void le_prix_est_fige_dans_la_vente() {
        vendre(Map.of("cafe", 1), MoyenPaiement.ESPECES);
        stock.enregistrer(stock.article("cafe").orElseThrow().modifier().prixVenteCentimes(150).nom("Expresso")
                .lot(2000, 125).build());
        VenteBuvette v = stock.tournoi(TOURNOI).orElseThrow().getVentes().get(0);
        assertEquals(100, v.lignes().get(0).prixUnitaireCentimes());
        assertEquals("Café", v.lignes().get(0).nom());
        BilanBuvette b = CalculBilanBuvette.bilan(stock.tournoi(TOURNOI).orElseThrow().getVentes());
        assertEquals(100, b.recetteCentimes());
        assertEquals(11, b.coutCentimes()); // 11,2 c figés, arrondis à 11
    }

    @Test
    void stock_insuffisant_la_vente_passe_avec_avertissement_et_article_a_recompter() {
        stock.enregistrer(stock.article("gobelet").orElseThrow().avecStock(1));
        ResultatVente r = vendre(Map.of("cafe", 2), MoyenPaiement.ESPECES);
        assertTrue(r.accepte());
        assertTrue(r.avecAvertissement());
        assertEquals(List.of("Gobelet"), r.aRecompter());
        assertTrue(r.explication().contains("Stock insuffisant pour Gobelet"), r.explication());
        assertEquals(0, stockDe("gobelet"));
        assertTrue(stock.estARecompter("gobelet"));
        assertFalse(stock.estARecompter("cafe"));
        assertTrue(r.vente().sorties().contains(new SortieVente("gobelet", 1)));

        // L'annulation remet ce qui a vraiment été sorti : 1 gobelet, pas 2.
        service.annulerDerniere(stock, TOURNOI, "Claire");
        assertEquals(1, stockDe("gobelet"));
        assertEquals(200, stockDe("cafe"));

        // Le recomptage lève le signalement.
        new ServiceStockBuvette(() -> maintenant).recompter(stock, "gobelet", 80, "Claire");
        assertFalse(stock.estARecompter("gobelet"));
    }

    @Test
    void en_mode_stock_seulement_aucune_vente_n_est_enregistree() {
        service.regler(stock, TOURNOI, "Tournoi de Brunoy", ModeBuvette.STOCK_SEUL);
        ResultatVente r = vendre(Map.of("cafe", 1), MoyenPaiement.ESPECES);
        assertEquals(ResultatVente.Motif.MODE_STOCK_SEUL, r.motif());
        assertEquals(200, stockDe("cafe"));
        assertEquals(ModeBuvette.STOCK_SEUL, ServiceVentesBuvette.mode(stock, "autre-tournoi"));
        assertEquals(ResultatVente.Motif.MODE_STOCK_SEUL,
                service.vendre(stock, "autre-tournoi", Map.of("cafe", 1), MoyenPaiement.ESPECES,
                        com.pingmanager.model.buvette.OrigineVente.COMPTOIR, "", "x").motif());
    }

    @Test
    void refus_sans_rien_sortir_commande_vide_article_archive_contenant_ou_prix_manquant() {
        assertEquals(ResultatVente.Motif.COMMANDE_VIDE, vendre(Map.of(), MoyenPaiement.ESPECES).motif());
        assertEquals(ResultatVente.Motif.PAS_UN_PRODUIT,
                vendre(Map.of("gobelet", 1), MoyenPaiement.ESPECES).motif());
        stock.enregistrer(stock.article("soda").orElseThrow().modifier().prixVenteCentimes(0).build());
        ResultatVente sansPrix = service.vendre(stock, TOURNOI, new java.util.LinkedHashMap<>(Map.of("cafe", 1,
                "soda", 1)), MoyenPaiement.ESPECES, com.pingmanager.model.buvette.OrigineVente.COMPTOIR, "", "x");
        assertEquals(ResultatVente.Motif.PRIX_MANQUANT, sansPrix.motif());
        assertEquals("Fixez d'abord le prix de vente dans la fiche de l'article : Soda.", sansPrix.explication());
        assertEquals(200, stockDe("cafe")); // rien n'est sorti : toute la commande est vérifiée avant
        new ServiceArticlesBuvette().archiver(stock, "cafe");
        assertEquals(ResultatVente.Motif.ARTICLE_ARCHIVE, vendre(Map.of("cafe", 1), MoyenPaiement.ESPECES).motif());
        assertTrue(stock.tournoi(TOURNOI).orElseThrow().getVentes().isEmpty());
    }

    @Test
    void comptoir_propose_les_produits_en_vente_ranges_par_rayon() {
        stock.enregistrer(ArticleBuvette.builder().id("eau").nom("Eau").rayon(com.pingmanager.model.buvette
                .RayonBuvette.BOISSONS_FROIDES).prixVenteCentimes(100).build());
        stock.enregistrer(ArticleBuvette.builder().id("the").nom("Thé").rayon(com.pingmanager.model.buvette
                .RayonBuvette.BOISSONS_CHAUDES).prixVenteCentimes(100).build());
        List<String> ids = ServiceVentesBuvette.articlesEnVente(stock).stream().map(ArticleBuvette::getId).toList();
        // Café et soda sans rayon : « Autres produits », en dernier.
        assertEquals(List.of("the", "eau", "cafe", "soda"), ids);
    }

    /**
     * Bilan vérifié à la main.
     * <pre>
     * Jour 1 (21/11) : n° 1 = 2 cafés + 1 soda, espèces : 3,50 € ; coût 2 × 11,2 + 40 = 62,4 c
     *                  n° 3 = 3 sodas, espèces : 4,50 €, ANNULÉE (ne compte pas)
     * Jour 2 (22/11) : n° 2 = 1 café, carte : 1,00 € ; coût 11,2 c
     * Tournoi : 2 ventes, 4 articles, recette 4,50 €, coût 73,6 c → 0,74 €, marge 3,76 €
     *   café : 3 portions, 3,00 €, coût 33,6 c → 0,34 €, marge 2,66 €
     *   soda : 1 canette, 1,50 €, coût 0,40 €, marge 1,10 €
     *   espèces : 1 vente, 3,50 € ; carte : 1 vente, 1,00 € ; autre : 0
     * Jour 1 : 1 vente, 3,50 €, coût 62,4 c → 0,62 €, marge 2,88 € (1 annulée)
     * Jour 2 : 1 vente, 1,00 €, coût 0,11 €, marge 0,89 €
     * </pre>
     */
    @Test
    void bilan_du_tournoi_et_par_journee_verifie_a_la_main() {
        CommandeComptoir c = new CommandeComptoir();
        c.ajouter("cafe");
        c.ajouter("soda");
        c.ajouter("cafe");
        maintenant = Instant.parse("2026-11-21T09:00:00Z");
        assertTrue(service.vendre(stock, TOURNOI, c, MoyenPaiement.ESPECES, "Claire").accepte());
        maintenant = Instant.parse("2026-11-22T10:00:00Z");
        vendre(Map.of("cafe", 1), MoyenPaiement.CARTE);
        maintenant = Instant.parse("2026-11-21T15:00:00Z");
        vendre(Map.of("soda", 3), MoyenPaiement.ESPECES);
        service.annuler(stock, TOURNOI, 3, "Claire");

        List<VenteBuvette> ventes = stock.tournoi(TOURNOI).orElseThrow().getVentes();
        BilanBuvette b = CalculBilanBuvette.bilan(ventes);
        assertNull(b.jour());
        assertEquals(2, b.nombreVentes());
        assertEquals(1, b.nombreAnnulees());
        assertEquals(4, b.nombreArticles());
        assertEquals(450, b.recetteCentimes());
        assertEquals(74, b.coutCentimes());
        assertEquals(376, b.margeCentimes());
        assertEquals(225, b.panierMoyenCentimes());
        assertEquals(List.of(new BilanBuvette.LigneArticle("cafe", "Café", 3, 300, 34),
                new BilanBuvette.LigneArticle("soda", "Soda", 1, 150, 40)), b.parArticle());
        assertEquals(266, b.parArticle().get(0).margeCentimes());
        assertEquals(110, b.parArticle().get(1).margeCentimes());
        assertEquals(List.of(new BilanBuvette.LigneMoyen(MoyenPaiement.ESPECES, 1, 350),
                new BilanBuvette.LigneMoyen(MoyenPaiement.CARTE, 1, 100),
                new BilanBuvette.LigneMoyen(MoyenPaiement.AUTRE, 0, 0)), b.parMoyen());

        assertEquals(List.of(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 22)),
                CalculBilanBuvette.jours(ventes, PARIS));
        BilanBuvette j1 = CalculBilanBuvette.bilanDuJour(ventes, LocalDate.of(2026, 11, 21), PARIS);
        assertEquals(1, j1.nombreVentes());
        assertEquals(1, j1.nombreAnnulees());
        assertEquals(350, j1.recetteCentimes());
        assertEquals(62, j1.coutCentimes());
        assertEquals(288, j1.margeCentimes());
        BilanBuvette j2 = CalculBilanBuvette.bilanDuJour(ventes, LocalDate.of(2026, 11, 22), PARIS);
        assertEquals(1, j2.nombreVentes());
        assertEquals(100, j2.recetteCentimes());
        assertEquals(11, j2.coutCentimes());
        assertEquals(89, j2.margeCentimes());

        // Stock : 3 cafés et 3 gobelets sortis (la vente de sodas annulée est remise).
        assertEquals(197, stockDe("cafe"));
        assertEquals(147, stockDe("gobelet"));
        assertEquals(23, stockDe("soda"));
    }

    @Test
    void bilan_sans_vente_tout_a_zero() {
        BilanBuvette b = CalculBilanBuvette.bilan(List.of());
        assertEquals(0, b.nombreVentes());
        assertEquals(0, b.recetteCentimes());
        assertEquals(0, b.panierMoyenCentimes());
        assertTrue(b.parArticle().isEmpty());
        assertEquals(3, b.parMoyen().size());
    }

    @Test
    void commande_comptoir_ajouter_retirer_vider() {
        CommandeComptoir c = new CommandeComptoir();
        c.ajouter("cafe");
        c.ajouter("soda");
        c.ajouter("cafe");
        assertEquals(Map.of("cafe", 2, "soda", 1), c.quantites());
        assertEquals(List.of("cafe", "soda"), List.copyOf(c.quantites().keySet()));
        c.retirerUn("soda");
        assertEquals(0, c.quantite("soda"));
        c.retirerUn("inconnu");
        c.vider();
        assertTrue(c.estVide());
    }
}
