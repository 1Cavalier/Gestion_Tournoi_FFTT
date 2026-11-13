package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.TypeMouvementStock;
import com.pingmanager.model.buvette.VenteBuvette;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceVentesBuvetteTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private final AtomicReference<Instant> maintenant = new AtomicReference<>(Instant.parse("2027-01-09T09:00:00Z"));
    private final ServiceVentesBuvette service = new ServiceVentesBuvette(maintenant::get);

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("baguette").nom("Baguette").prixAchatCentimes(110)
                .rendement(3).build());
        c.enregistrerIngredient(Ingredient.builder().id("jambon").nom("Jambon").prixAchatCentimes(260)
                .rendement(4).build());
        c.enregistrerProduit(Produit.builder().id("jb").nom("Jambon-beurre").prixVenteCentimes(300)
                .recette(List.of(new LigneRecette("baguette", 1), new LigneRecette("jambon", 1))).build());
        c.enregistrerProduit(Produit.builder().id("cafe").nom("Café").prixVenteCentimes(100).build());
        c.enregistrerProduit(Produit.builder().id("vieux").nom("Vieux").prixVenteCentimes(100).actif(false).build());
        return c;
    }

    @Test
    void une_vente_deduit_le_stock_selon_la_recette_et_garde_prix_et_cout() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        new ServiceStockBuvette(maintenant::get).stockDepart(b, c, "baguette", 6, "A");
        ResultatVenteBuvette r = service.vendre(b, c, "jb", "Léa", ServiceVentesBuvette.POSTE_PAR_DEFAUT);
        assertEquals(ResultatVenteBuvette.Motif.VENDUE, r.motif());
        VenteBuvette v = r.vente();
        assertEquals(1, v.numero());
        assertEquals(300, v.prixCentimes());
        assertEquals(102, v.coutCentimes(), "36,67 + 65 = 101,67 → 102");
        assertEquals("Caisse 1", v.poste());
        assertEquals(5, ServiceStockBuvette.stock(b, "baguette"));
        assertEquals(-1, ServiceStockBuvette.stock(b, "jambon"));
        assertEquals(List.of("Jambon"), r.stockEpuise(), "la vente est gardée, l'alerte est donnée");
        assertTrue(c.estVendu("jb"), "un produit vendu ne se supprime plus");
        MouvementStock m = b.getMouvements().get(1);
        assertEquals(TypeMouvementStock.VENTE, m.type());
        assertEquals(1, m.numeroLie());
    }

    @Test
    void produit_inconnu_ou_desactive_refuse() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        assertEquals(ResultatVenteBuvette.Motif.PRODUIT_INCONNU, service.vendre(b, c, "x", "A", null).motif());
        assertEquals(ResultatVenteBuvette.Motif.PRODUIT_INACTIF, service.vendre(b, c, "vieux", "A", null).motif());
        assertTrue(b.getVentes().isEmpty());
    }

    @Test
    void annulation_de_la_derniere_vente_remet_le_stock_sans_rien_effacer() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.vendre(b, c, "jb", "A", null);
        service.vendre(b, c, "cafe", "A", null);
        ResultatVenteBuvette r = service.annulerDerniere(b, c, "jb", "A", null);
        assertEquals(ResultatVenteBuvette.Motif.ANNULEE, r.motif());
        assertEquals(1, r.vente().numeroAnnule());
        assertEquals(-300, r.vente().prixCentimes());
        assertEquals(3, b.getVentes().size(), "la vente reste, l'annulation s'ajoute");
        assertEquals(0, ServiceStockBuvette.stock(b, "baguette"));
        assertEquals(0, ServiceStockBuvette.stock(b, "jambon"));
        assertTrue(ServiceStockBuvette.estCorrige(b, 1));
        assertEquals(ResultatVenteBuvette.Motif.RIEN_A_ANNULER,
                service.annulerDerniere(b, c, "jb", "A", null).motif(), "déjà annulée");
        assertEquals("cafe", service.annulerDerniere(b, c, null, "A", null).vente().idProduit());
        assertEquals(ResultatVenteBuvette.Motif.RIEN_A_ANNULER, service.annulerDerniere(b, c, null, "A", null).motif());
        assertNull(service.annulerDerniere(b, c, null, "A", null).vente());
    }

    @Test
    void totaux_du_jour_et_du_tournoi_annulations_deduites() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.vendre(b, c, "cafe", "A", null);
        service.vendre(b, c, "cafe", "A", null);
        service.vendre(b, c, "jb", "A", null);
        service.annulerDerniere(b, c, "cafe", "A", null);
        maintenant.set(Instant.parse("2027-01-10T09:00:00Z"));
        service.vendre(b, c, "cafe", "A", null);
        SyntheseVentes samedi = ServiceVentesBuvette.synthese(b, LocalDate.of(2027, 1, 9), PARIS);
        assertEquals(2, samedi.articles());
        assertEquals(400, samedi.recetteCentimes());
        assertEquals(1, samedi.quantite("cafe"));
        assertFalse(samedi.coutComplet(), "le café n'a pas de recette");
        SyntheseVentes tout = ServiceVentesBuvette.synthese(b, null, PARIS);
        assertEquals(500, tout.recetteCentimes());
        assertEquals(2, tout.quantite("cafe"));
        assertEquals(102, tout.coutCentimes());
        assertEquals("cafe", tout.plusVendus(1).get(0).getKey());
    }
}
