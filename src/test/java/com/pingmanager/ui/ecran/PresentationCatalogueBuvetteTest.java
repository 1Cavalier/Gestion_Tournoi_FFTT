package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.ServiceCatalogueBuvette;
import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationCatalogueBuvetteTest {

    private static Produit p(String id, String nom, CategorieProduit c, long prix) {
        return Produit.builder().id(id).nom(nom).categorie(c).unite(UniteVente.GOBELET).prixVenteCentimes(prix).build();
    }

    private final SuggestionsBuvette suggestions = new SuggestionsBuvette(1, "Hypothèses.", List.of(
            p("cafe", "Café", CategorieProduit.BOISSONS_CHAUDES, 100),
            p("eau", "Eau", CategorieProduit.BOISSONS_FROIDES, 100)));

    @Test
    void catalogue_vide_invite_a_partir_des_suggestions() {
        PresentationCatalogueBuvette pr = new PresentationCatalogueBuvette(new CatalogueBuvette("1"), suggestions,
                new ServiceCatalogueBuvette());
        assertTrue(pr.vide());
        assertEquals("2", pr.tuiles().get(3).valeur());
        assertEquals("suggestions à examiner", pr.tuiles().get(3).libelle());
        assertTrue(pr.mentionSuggestions().startsWith("Prix indicatifs à valider par le club"));
        assertTrue(pr.confirmationToutAjouter().startsWith("Ajouter les 2 produits suggérés"));
    }

    @Test
    void produits_groupes_par_categorie_avec_prix_et_action_de_retrait() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerProduit(p("eau", "Eau", CategorieProduit.BOISSONS_FROIDES, 100));
        c.enregistrerProduit(p("cafe", "Café", CategorieProduit.BOISSONS_CHAUDES, 120));
        c.enregistrerProduit(p("the", "Thé", CategorieProduit.BOISSONS_CHAUDES, 100).modifier().actif(false).build());
        c.marquerVendu("cafe");
        PresentationCatalogueBuvette pr = new PresentationCatalogueBuvette(c, suggestions,
                new ServiceCatalogueBuvette());
        List<PresentationCatalogueBuvette.Groupe> g = pr.groupes();
        assertEquals(List.of("Boissons froides", "Boissons chaudes"), g.stream().map(x -> x.titre()).toList());
        PresentationCatalogueBuvette.LigneProduit cafe = g.get(1).lignes().get(0);
        assertEquals("Gobelet · 1,20 € · recette à renseigner · déjà vendu", cafe.detail());
        assertEquals("Désactiver", cafe.actionRetrait());
        assertEquals("Retirer", g.get(1).lignes().get(1).actionRetrait());
        assertFalse(g.get(1).lignes().get(1).actif());
        assertEquals("2", pr.tuiles().get(0).valeur());
        assertEquals("produit désactivé", pr.tuiles().get(1).libelle());
        assertTrue(pr.suggestions().isEmpty(), "café et eau sont déjà là");
    }

    @Test
    void categorie_alcool_proposee_seulement_apres_activation() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        PresentationCatalogueBuvette pr = new PresentationCatalogueBuvette(c, suggestions,
                new ServiceCatalogueBuvette());
        assertFalse(pr.categoriesSaisissables().contains(CategorieProduit.ALCOOLISEES));
        assertTrue(pr.actionAlcool().startsWith("Activer"));
        c.setAlcoolActive(true);
        assertTrue(pr.categoriesSaisissables().contains(CategorieProduit.ALCOOLISEES));
        assertTrue(pr.actionAlcool().startsWith("Désactiver"));
    }

    @Test
    void recette_expliquee_et_synthese_avec_marge_et_taux() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(com.pingmanager.model.buvette.Ingredient.builder().id("baguette").nom("Baguette")
                .uniteAchat("baguette").unitePortion("sandwichs").prixAchatCentimes(110).rendement(2, 3).build());
        c.enregistrerProduit(Produit.builder().id("s").nom("Sandwich").categorie(CategorieProduit.SANDWICHS)
                .unite(UniteVente.PIECE).prixVenteCentimes(300)
                .recette(List.of(new com.pingmanager.model.buvette.LigneRecette("baguette", 1))).build());
        PresentationCatalogueBuvette pr = new PresentationCatalogueBuvette(c, suggestions,
                new ServiceCatalogueBuvette());
        PresentationCatalogueBuvette.Recette r = pr.recette("s").orElseThrow();
        assertEquals("Recette : Sandwich", r.titre());
        assertEquals("Baguette : 1,10 € ÷ 2 sandwichs = 0,55 €", r.lignes().get(0).explication());
        assertEquals("Coût de revient 0,55 € (de 0,37 € à 0,55 € selon le rendement) · marge 2,45 € · "
                + "taux de marge 445 % · taux de marque 82 %", r.synthese());
        assertEquals("Pièce · 3,00 € · coût 0,55 € · marge 2,45 €", pr.groupes().get(0).lignes().get(0).detail());
        assertEquals("Baguette à 1,10 € · 2 à 3 sandwichs · 0,55 € la portion", pr.ingredients().get(0).detail());
        assertEquals("Désactiver", pr.ingredients().get(0).actionRetrait(),
                "l'ingrédient sert dans une recette : il se désactive");
        assertTrue(pr.recette("absent").isEmpty());
    }
}
