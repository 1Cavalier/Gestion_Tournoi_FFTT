package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.ResultatVenteBuvette;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationVentesBuvetteTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate SAMEDI = LocalDate.of(2027, 1, 9);
    private final ServiceVentesBuvette service = new ServiceVentesBuvette(() -> Instant.parse("2027-01-09T09:30:00Z"));

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("soda").nom("Soda").rendement(24).prixAchatCentimes(1200)
                .build());
        c.enregistrerProduit(Produit.builder().id("soda").nom("Soda").categorie(CategorieProduit.BOISSONS_FROIDES)
                .prixVenteCentimes(150).recette(List.of(new LigneRecette("soda", 1))).build());
        c.enregistrerProduit(Produit.builder().id("cafe").nom("Café").categorie(CategorieProduit.BOISSONS_CHAUDES)
                .prixVenteCentimes(100).build());
        c.enregistrerProduit(Produit.builder().id("old").nom("Ancien").prixVenteCentimes(100).actif(false).build());
        return c;
    }

    @Test
    void boutons_des_produits_actifs_avec_compteur_du_jour() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.vendre(b, c, "soda", "Léa", null);
        service.vendre(b, c, "soda", "Léa", null);
        PresentationVentesBuvette p = new PresentationVentesBuvette(b, c, SAMEDI, PARIS);
        List<PresentationVentesBuvette.Groupe> g = p.groupes();
        assertEquals(List.of("Boissons froides", "Boissons chaudes"), g.stream().map(x -> x.titre()).toList());
        assertEquals(new PresentationVentesBuvette.BoutonProduit("soda", "Soda", "1,50 €", "× 2"), g.get(0).boutons().get(0));
        assertEquals("", g.get(1).boutons().get(0).compteur());
        assertEquals("3,00 €", p.tuiles().get(0).valeur());
        assertEquals("articles vendus aujourd'hui", p.tuiles().get(1).libelle());
        assertEquals("Caisse 1", p.tuiles().get(3).valeur());
        assertFalse(p.sansProduit());
    }

    @Test
    void message_apres_vente_avec_total_du_jour_et_alerte_de_stock() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        PresentationVentesBuvette p = new PresentationVentesBuvette(b, c, SAMEDI, PARIS);
        ResultatVenteBuvette r = service.vendre(b, c, "soda", "Léa", null);
        assertEquals("Soda : 1,50 €. Total du jour 1,50 €. Attention, stock épuisé : Soda (vérifiez l'inventaire).",
                p.message(r));
        ResultatVenteBuvette a = service.annulerDerniere(b, c, null, "Léa", null);
        assertEquals("Vente n° 1 annulée (Soda). Total du jour 0,00 €.", p.message(a));
        assertEquals("Aucune vente à annuler.", p.message(service.annulerDerniere(b, c, null, "Léa", null)));
    }

    @Test
    void dernieres_ventes_la_plus_recente_en_tete_avec_annulation() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = new BuvetteTournoi();
        service.vendre(b, c, "cafe", "Léa", null);
        service.annulerDerniere(b, c, null, "Léa", null);
        List<PresentationVentesBuvette.LigneVente> l = new PresentationVentesBuvette(b, c, SAMEDI, PARIS).dernieres();
        assertEquals("Annulation de la vente n° 1 · Café", l.get(0).titre());
        assertTrue(l.get(0).annulation());
        assertEquals("n° 1 · Café · 1,00 €", l.get(1).titre());
        assertTrue(l.get(1).annulee());
        assertEquals("10:30 · Léa · Caisse 1", l.get(1).detail());
    }

    @Test
    void caisse_fermee_sans_produit_actif() {
        PresentationVentesBuvette p = new PresentationVentesBuvette(new BuvetteTournoi(), new CatalogueBuvette("1"),
                SAMEDI, PARIS);
        assertTrue(p.sansProduit());
        assertTrue(p.messageSansProduit().contains("Catalogue"));
    }
}
