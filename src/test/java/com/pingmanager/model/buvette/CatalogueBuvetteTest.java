package com.pingmanager.model.buvette;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueBuvetteTest {

    private static Produit cafe() {
        return Produit.builder().id("cafe").nom("Café").categorie(CategorieProduit.BOISSONS_CHAUDES)
                .unite(UniteVente.GOBELET).prixVenteCentimes(100).build();
    }

    @Test
    void identifiant_libre_derive_du_nom_sans_accents_puis_numerote() {
        CatalogueBuvette c = new CatalogueBuvette("08910012");
        assertEquals("croque-monsieur", c.idLibre("Croque-Monsieur"));
        assertEquals("the-glace", c.idLibre("  Thé glacé "));
        c.enregistrerProduit(cafe());
        assertEquals("cafe-2", c.idLibre("Café"));
        assertEquals("produit", c.idLibre("!!!"));
    }

    @Test
    void produit_deja_vendu_ne_se_supprime_pas() {
        CatalogueBuvette c = new CatalogueBuvette("08910012");
        c.enregistrerProduit(cafe());
        c.marquerVendu("cafe");
        assertThrows(IllegalStateException.class, () -> c.retirerProduit("cafe"));
        assertTrue(c.produit("cafe").isPresent());
    }

    @Test
    void catalogue_neuf_est_vide_et_l_alcool_desactive() {
        CatalogueBuvette c = new CatalogueBuvette(" 08910012 ");
        assertTrue(c.estVide());
        assertFalse(c.isAlcoolActive());
        assertEquals("08910012", c.getNumeroClub());
    }

    @Test
    void produit_refuse_un_prix_negatif_ou_un_nom_vide() {
        assertThrows(IllegalArgumentException.class, () -> cafe().modifier().prixVenteCentimes(-1).build());
        assertThrows(IllegalArgumentException.class, () -> cafe().modifier().nom(" ").build());
    }

    @Test
    void relecture_tolerante_des_enumerations() {
        assertEquals(CategorieProduit.AUTRE, CategorieProduit.lire("INCONNUE"));
        assertEquals(UniteVente.PORTION, UniteVente.lire(null));
        assertEquals(CategorieProduit.SNACKS, CategorieProduit.lire("SNACKS"));
    }
}
