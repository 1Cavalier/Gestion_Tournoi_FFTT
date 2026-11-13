package com.pingmanager.core.buvette;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BudgetBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculBudgetBuvetteTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 1, 9);
    private static final LocalDate DIMANCHE = LocalDate.of(2027, 1, 10);
    /** 30 % d'accompagnants, 1,5 demi-journée, scénarios 75 % / 125 % ; café 0,35, sandwich 0,10. */
    private static final HypothesesConsommation H = new HypothesesConsommation(30, 15, 75, 125,
            Map.of("cafe", 35, "sandwich", 10));
    private final CalculBudgetBuvette calcul = new CalculBudgetBuvette();

    private static CatalogueBuvette catalogue() {
        CatalogueBuvette c = new CatalogueBuvette("1");
        c.enregistrerIngredient(Ingredient.builder().id("cafe-moulu").nom("Café moulu").uniteAchat("paquet de 1 kg")
                .unitePortion("doses").prixAchatCentimes(1200).rendement(120, 140).build());
        c.enregistrerIngredient(Ingredient.builder().id("baguette").nom("Baguette").uniteAchat("baguette")
                .unitePortion("sandwichs").prixAchatCentimes(110).rendement(2, 3).pertePourcent(10).build());
        c.enregistrerProduit(Produit.builder().id("cafe").nom("Café").categorie(CategorieProduit.BOISSONS_CHAUDES)
                .unite(UniteVente.GOBELET).prixVenteCentimes(100)
                .recette(List.of(new LigneRecette("cafe-moulu", 1))).build());
        c.enregistrerProduit(Produit.builder().id("sandwich").nom("Sandwich").categorie(CategorieProduit.SANDWICHS)
                .unite(UniteVente.PIECE).prixVenteCentimes(300)
                .recette(List.of(new LigneRecette("baguette", 1))).build());
        c.enregistrerProduit(Produit.builder().id("the").nom("Thé").prixVenteCentimes(100).build()); // sans hypothèse
        return c;
    }

    private static Tournoi tournoi(LocalDate... jours) {
        return new Tournoi("Open", "Club", List.of(jours));
    }

    private static BuvetteTournoi avecJoueurs(Map<LocalDate, Integer> joueurs) {
        BuvetteTournoi b = new BuvetteTournoi();
        BudgetBuvette budget = BudgetBuvette.VIDE;
        for (Map.Entry<LocalDate, Integer> e : joueurs.entrySet()) {
            budget = budget.avecJoueurs(e.getKey(), e.getValue());
        }
        b.setBudget(budget);
        return b;
    }

    @Test
    void tournoi_d_un_jour_cent_vingt_joueurs_quantites_courses_et_marge() {
        BudgetCalcule r = calcul.calculer(tournoi(SAMEDI), avecJoueurs(Map.of(SAMEDI, 120)), catalogue(), H);
        assertEquals("120 joueurs + 30 % d'accompagnants = 156 personnes ; × 1,5 demi-journée de présence "
                + "= 234 présences", r.explicationAffluence());
        BudgetCalcule.Scenario m = r.moyen();
        assertEquals(2, m.produits().size(), "le thé sans hypothèse n'est pas prévu");
        BudgetCalcule.LigneProduit cafe = m.produits().get(0);
        assertEquals(82, cafe.quantite(), "234 × 0,35 = 81,9 → 82");
        assertEquals("Café : 234 présences × 0,35 = 82", cafe.explication());
        assertEquals(24, m.produits().get(1).quantite(), "234 × 0,10 = 23,4 → 24");
        assertEquals(82 * 100 + 24 * 300, m.recetteCentimes());
        BudgetCalcule.LigneCourse moulu = m.courses().get(0);
        assertEquals(1, moulu.unites(), "82 doses ÷ 120 → 1 paquet");
        BudgetCalcule.LigneCourse baguette = m.courses().get(1);
        assertEquals(14, baguette.unites(), "24 ÷ (2 × 90 %) = 13,3 → 14 baguettes");
        assertEquals("Baguette : 24 sandwichs ÷ 2 (perte 10 %) = 14 × baguette à 1,10 € = 15,40 €",
                baguette.explication());
        assertEquals(1200 + 14 * 110, m.coutAchatsCentimes());
        assertEquals(15_400 - 2_740, m.margeCentimes());
        assertEquals(OptionalLong.of(18), m.seuilRentabilitePourcent(), "2 740 / 15 400 = 17,8 % → 18 %");
    }

    @Test
    void scenarios_bas_et_haut_appliquent_le_pourcentage_d_affluence() {
        BudgetCalcule r = calcul.calculer(tournoi(SAMEDI), avecJoueurs(Map.of(SAMEDI, 120)), catalogue(), H);
        assertEquals(List.of("Bas", "Moyen", "Haut"), r.scenarios().stream().map(BudgetCalcule.Scenario::nom).toList());
        assertEquals(62, r.bas().produits().get(0).quantite(), "234 × 75 % × 0,35 = 61,4 → 62");
        assertEquals("Café : 234 présences × 75 % × 0,35 = 62", r.bas().produits().get(0).explication());
        assertEquals(103, r.haut().produits().get(0).quantite(), "234 × 125 % × 0,35 = 102,4 → 103");
        assertTrue(r.bas().recetteCentimes() < r.moyen().recetteCentimes());
        assertTrue(r.haut().recetteCentimes() > r.moyen().recetteCentimes());
    }

    @Test
    void tournoi_de_deux_jours_additionne_les_joueurs_de_chaque_jour() {
        BudgetCalcule r = calcul.calculer(tournoi(SAMEDI, DIMANCHE),
                avecJoueurs(Map.of(SAMEDI, 120, DIMANCHE, 80)), catalogue(), H);
        assertEquals(200, r.joueurs());
        assertEquals("200 joueurs sur 2 jours + 30 % d'accompagnants = 260 personnes ; × 1,5 demi-journée de "
                + "présence = 390 présences", r.explicationAffluence());
        assertEquals(137, r.moyen().produits().get(0).quantite(), "390 × 0,35 = 136,5 → 137");
        assertEquals(2, r.moyen().courses().get(0).unites(), "137 doses ÷ 120 → 2 paquets");
    }

    @Test
    void joueurs_estimes_d_apres_les_inscriptions_de_chaque_jour() {
        Tournoi t = tournoi(SAMEDI, DIMANCHE);
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(50).toutesSeries().build());
        a.setHoraires(new HorairesTableau(null, null, SAMEDI.atTime(9, 0)));
        Tableau b = new Tableau("B", ConfigurationTableau.builder().placesMax(50).toutesSeries().build());
        b.setHoraires(new HorairesTableau(null, null, DIMANCHE.atTime(9, 0)));
        Tableau c = new Tableau("C", ConfigurationTableau.builder().placesMax(50).toutesSeries().build());
        t.ajouterTableau(a);
        t.ajouterTableau(b);
        t.ajouterTableau(c);
        Joueur j1 = new Joueur("1", "A", "a", Sexe.values()[0], "Club", 500);
        Joueur j2 = new Joueur("2", "B", "b", Sexe.values()[0], "Club", 500);
        a.inscrire(j1);
        a.inscrire(j2);
        c.inscrire(j1); // même jour (sans jour connu → premier jour) : compté une fois
        b.inscrire(j1);
        assertEquals(Map.of(SAMEDI, 2, DIMANCHE, 1), CalculBudgetBuvette.joueursEstimes(t));
        BudgetCalcule r = calcul.calculer(t, avecJoueurs(Map.of(DIMANCHE, 40)), catalogue(), H);
        assertEquals(List.of(new BudgetCalcule.JourBudget(SAMEDI, 2, false),
                new BudgetCalcule.JourBudget(DIMANCHE, 40, true)), r.jours());
    }

    @Test
    void stock_disponible_deduit_de_la_liste_de_courses_sur_demande() {
        CatalogueBuvette c = catalogue();
        BuvetteTournoi b = avecJoueurs(Map.of(SAMEDI, 120));
        new ServiceStockBuvette(() -> Instant.EPOCH).stockDepart(b, c, "baguette", 10, "A");
        BudgetCalcule.LigneCourse baguette = calcul.calculer(tournoi(SAMEDI), b, c, H).moyen().courses().get(1);
        assertEquals(10, baguette.stockDisponible());
        assertEquals(14, baguette.portionsAAcheter());
        assertEquals(8, baguette.unites(), "14 ÷ (2 × 90 %) = 7,8 → 8");
        b.setBudget(b.getBudget().avecDeduireStock(false));
        assertEquals(14, calcul.calculer(tournoi(SAMEDI), b, c, H).moyen().courses().get(1).unites());
    }

    @Test
    void quantites_reprises_d_un_ancien_tournoi_remplacent_le_calcul() {
        BuvetteTournoi b = avecJoueurs(Map.of(SAMEDI, 120));
        b.setBudget(b.getBudget().avecQuantitesImposees(Map.of("cafe", 50L, "the", 10L), "Open 2026"));
        BudgetCalcule r = calcul.calculer(tournoi(SAMEDI), b, catalogue(), H);
        BudgetCalcule.LigneProduit cafe = r.moyen().produits().get(0);
        assertEquals(50, cafe.quantite());
        assertTrue(cafe.imposee());
        assertEquals("Café : 50 repris de « Open 2026 » = 50", cafe.explication());
        assertEquals(3, r.moyen().produits().size(), "le thé repris apparaît malgré l'absence d'hypothèse");
        assertTrue(r.moyen().produits().get(2).sansRecette());
        assertEquals(38, r.bas().produits().get(0).quantite(), "50 × 75 % = 37,5 → 38");
    }

    @Test
    void hypotheses_ajustees_completent_les_suggestions() {
        BuvetteTournoi b = avecJoueurs(Map.of(SAMEDI, 100));
        b.setBudget(b.getBudget().avecHypotheses(new HypothesesConsommation(0, 10, 50, 150, Map.of("cafe", 50))));
        BudgetCalcule r = calcul.calculer(tournoi(SAMEDI), b, catalogue(), H);
        assertEquals(50, r.moyen().produits().get(0).quantite(), "100 personnes × 1 × 0,50");
        assertEquals(10, r.moyen().produits().get(1).quantite(), "sandwich : suggestion 0,10 gardée");
        assertEquals(50, r.hypotheses().scenarioBasPourcent());
    }

    @Test
    void sans_joueur_aucune_vente_et_seuil_inconnu() {
        BudgetCalcule r = calcul.calculer(tournoi(SAMEDI), new BuvetteTournoi(), catalogue(), H);
        assertEquals(0, r.moyen().recetteCentimes());
        assertEquals(OptionalLong.empty(), r.moyen().seuilRentabilitePourcent());
        assertTrue(r.moyen().courses().isEmpty());
    }
}
