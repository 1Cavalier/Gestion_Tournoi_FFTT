package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.BudgetCalcule;
import com.pingmanager.core.buvette.CalculBudgetBuvette;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BudgetBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationBudgetBuvetteTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 1, 9);
    private static final LocalDate DIMANCHE = LocalDate.of(2027, 1, 10);

    private static PresentationBudgetBuvette presentation(Map<LocalDate, Integer> joueurs, LocalDate... jours) {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("soda").nom("Soda").uniteAchat("carton de 24")
                .unitePortion("canettes").prixAchatCentimes(1200).rendement(24).build());
        c.enregistrerProduit(Produit.builder().id("soda").nom("Soda").prixVenteCentimes(150)
                .recette(List.of(new LigneRecette("soda", 1))).build());
        BuvetteTournoi b = new BuvetteTournoi();
        BudgetBuvette budget = BudgetBuvette.VIDE;
        for (Map.Entry<LocalDate, Integer> e : joueurs.entrySet()) {
            budget = budget.avecJoueurs(e.getKey(), e.getValue());
        }
        b.setBudget(budget);
        BudgetCalcule calcul = new CalculBudgetBuvette().calculer(new Tournoi("Open d'hiver", "Club", List.of(jours)),
                b, c, new HypothesesConsommation(0, 10, 75, 125, Map.of("soda", 50)));
        return new PresentationBudgetBuvette(calcul, "Open d'hiver");
    }

    @Test
    void budget_d_un_jour_tuiles_phrases_et_courses() {
        PresentationBudgetBuvette p = presentation(Map.of(SAMEDI, 100), SAMEDI);
        assertEquals("75,00 €", p.tuiles().get(0).valeur(), "50 canettes × 1,50 €");
        assertEquals("36,00 €", p.tuiles().get(1).valeur(), "3 cartons");
        assertEquals("39,00 €", p.tuiles().get(2).valeur());
        assertEquals("48 %", p.tuiles().get(3).valeur());
        assertEquals("100 joueurs + 0 % d'accompagnants = 100 personnes ; × 1 demi-journée de présence "
                + "= 100 présences.", p.affluence());
        assertEquals("Les achats (36,00 €) sont remboursés dès 36,00 € de ventes, soit 48 % de la recette attendue.",
                p.phraseSeuil("Moyen"));
        assertEquals("Soda : 50 · 75,00 €", p.produits("Moyen").get(0).titre());
        assertEquals("Soda : 100 présences × 0,5 = 50", p.produits("Moyen").get(0).detail());
        assertEquals("Soda : 3 × carton de 24 · 36,00 €", p.courses("Moyen").get(0).titre());
        assertEquals("Soda : 100 présences × 75 % × 0,5 = 38", p.produits("Bas").get(0).detail());
        assertEquals(3, p.scenarios().size());
        assertEquals("Bas (75 %)", p.scenarios().get(0).nom());
        assertEquals("0,5", p.consommation("soda"));
        assertEquals("1", p.presence());
        assertTrue(p.mentionHypotheses().startsWith("Hypothèse à ajuster"));
        assertFalse(p.sansProduit());
    }

    @Test
    void budget_de_deux_jours_et_jours_estimes_ou_saisis() {
        PresentationBudgetBuvette p = presentation(Map.of(DIMANCHE, 60), SAMEDI, DIMANCHE);
        assertEquals(2, p.jours().size());
        assertEquals("Samedi 9 janvier 2027", p.jours().get(0).libelle());
        assertEquals("0", p.jours().get(0).joueurs());
        assertEquals("estimé d'après les inscriptions", p.jours().get(0).origine());
        assertEquals("saisi à la main", p.jours().get(1).origine());
        assertTrue(p.affluence().startsWith("60 joueurs sur 2 jours"));
    }

    @Test
    void exports_csv_et_pdf_de_la_liste_de_courses() {
        PresentationBudgetBuvette p = presentation(Map.of(SAMEDI, 100), SAMEDI);
        assertTrue(p.csvCourses("Haut").contains("Scénario haut"));
        List<List<String>> lignes = p.lignesPdfCourses("Moyen");
        assertEquals(List.of("Soda", "3 × carton de 24", "50 portions", "36,00 €"), lignes.get(0));
        assertEquals("Total", lignes.get(lignes.size() - 1).get(0));
        assertEquals("courses-buvette-open-d-hiver-moyen.csv", p.nomFichier("Moyen", "csv"));
    }

    @Test
    void sans_joueur_ni_vente_phrase_d_aide() {
        PresentationBudgetBuvette p = presentation(Map.of(), SAMEDI);
        assertEquals("Aucune vente prévue : renseignez les joueurs et les consommations.", p.phraseSeuil("Moyen"));
        assertEquals("—", p.tuiles().get(3).valeur());
    }
}
