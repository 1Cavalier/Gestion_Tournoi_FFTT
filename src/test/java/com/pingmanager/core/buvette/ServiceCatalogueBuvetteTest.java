package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceCatalogueBuvetteTest {

    private final ServiceCatalogueBuvette service = new ServiceCatalogueBuvette();

    private static Produit produit(String id, String nom, CategorieProduit c, long prix) {
        return Produit.builder().id(id).nom(nom).categorie(c).unite(UniteVente.PIECE).prixVenteCentimes(prix).build();
    }

    private static SuggestionsBuvette suggestions() {
        return new SuggestionsBuvette(1, null, List.of(
                produit("cafe", "Café", CategorieProduit.BOISSONS_CHAUDES, 100),
                produit("eau", "Eau", CategorieProduit.BOISSONS_FROIDES, 100)));
    }

    @Test
    void saisie_d_un_produit_nouveau_avec_prix_en_euros() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        ResultatProduitBuvette r = service.saisir(c, null, "Croque-monsieur", CategorieProduit.SANDWICHS,
                UniteVente.PIECE, "2,50");
        assertEquals(ResultatProduitBuvette.Motif.ENREGISTRE, r.motif());
        assertEquals(250, c.produit("croque-monsieur").orElseThrow().getPrixVenteCentimes());
    }

    @Test
    void saisie_refusee_sans_nom_ou_avec_un_prix_illisible() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        assertEquals(ResultatProduitBuvette.Motif.NOM_MANQUANT,
                service.saisir(c, null, " ", CategorieProduit.SNACKS, UniteVente.PIECE, "1").motif());
        assertEquals(ResultatProduitBuvette.Motif.PRIX_INVALIDE,
                service.saisir(c, null, "Chips", CategorieProduit.SNACKS, UniteVente.PIECE, "un euro").motif());
        assertTrue(c.getProduits().isEmpty());
    }

    @Test
    void modification_garde_l_identifiant_et_refuse_un_nom_deja_pris() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        service.enregistrer(c, produit("cafe", "Café", CategorieProduit.BOISSONS_CHAUDES, 100));
        service.enregistrer(c, produit("the", "Thé", CategorieProduit.BOISSONS_CHAUDES, 100));
        assertEquals(ResultatProduitBuvette.Motif.ENREGISTRE,
                service.saisir(c, "cafe", "Café allongé", CategorieProduit.BOISSONS_CHAUDES, UniteVente.GOBELET,
                        "1,20").motif());
        assertEquals("Café allongé", c.produit("cafe").orElseThrow().getNom());
        assertEquals(ResultatProduitBuvette.Motif.NOM_EN_DOUBLE,
                service.saisir(c, "the", "CAFE ALLONGE", CategorieProduit.BOISSONS_CHAUDES, UniteVente.GOBELET,
                        "1").motif());
    }

    @Test
    void alcool_refuse_tant_que_la_categorie_n_est_pas_activee() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        Produit biere = produit("biere", "Bière", CategorieProduit.ALCOOLISEES, 250);
        assertEquals(ResultatProduitBuvette.Motif.ALCOOL_NON_ACTIVE, service.enregistrer(c, biere).motif());
        service.activerAlcool(c, true);
        assertEquals(ResultatProduitBuvette.Motif.ENREGISTRE, service.enregistrer(c, biere).motif());
    }

    @Test
    void retrait_supprime_un_produit_jamais_vendu_mais_desactive_un_produit_vendu() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        service.enregistrer(c, produit("cafe", "Café", CategorieProduit.BOISSONS_CHAUDES, 100));
        service.enregistrer(c, produit("the", "Thé", CategorieProduit.BOISSONS_CHAUDES, 100));
        c.marquerVendu("cafe");
        assertEquals(ResultatProduitBuvette.Motif.DESACTIVE_DEJA_VENDU, service.retirer(c, "cafe").motif());
        assertFalse(c.produit("cafe").orElseThrow().isActif());
        assertEquals(ResultatProduitBuvette.Motif.SUPPRIME, service.retirer(c, "the").motif());
        assertTrue(c.produit("the").isEmpty());
        assertEquals(ResultatProduitBuvette.Motif.INCONNU, service.retirer(c, "the").motif());
    }

    @Test
    void desactivation_puis_reactivation() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        service.enregistrer(c, produit("cafe", "Café", CategorieProduit.BOISSONS_CHAUDES, 100));
        service.changerActif(c, "cafe", false);
        assertFalse(c.produit("cafe").orElseThrow().isActif());
        service.changerActif(c, "cafe", true);
        assertTrue(c.produit("cafe").orElseThrow().isActif());
    }

    @Test
    void suggestions_ajoutees_seulement_apres_clic_et_jamais_en_double() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        SuggestionsBuvette s = suggestions();
        assertTrue(c.getProduits().isEmpty(), "rien n'est ajouté sans clic");
        assertEquals(2, service.suggestionsDisponibles(c, s).size());
        assertEquals(ResultatProduitBuvette.Motif.ENREGISTRE, service.ajouterSuggestion(c, s, "cafe").motif());
        assertEquals(ResultatProduitBuvette.Motif.DEJA_PRESENT, service.ajouterSuggestion(c, s, "cafe").motif());
        assertEquals(ResultatProduitBuvette.Motif.INCONNU, service.ajouterSuggestion(c, s, "biere").motif());
        assertEquals(1, service.ajouterToutesSuggestions(c, s));
        assertEquals(0, service.suggestionsDisponibles(c, s).size());
    }

    @Test
    void suggestion_deja_presente_sous_un_autre_identifiant_n_est_plus_proposee() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        service.enregistrer(c, produit("mon-cafe", "café", CategorieProduit.BOISSONS_CHAUDES, 80));
        assertEquals(List.of("eau"), service.suggestionsDisponibles(c, suggestions()).stream()
                .map(Produit::getId).toList());
    }
}
