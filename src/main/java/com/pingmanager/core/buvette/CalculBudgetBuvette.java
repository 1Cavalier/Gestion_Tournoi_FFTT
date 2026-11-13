package com.pingmanager.core.buvette;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BudgetBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Budget prévisionnel de la buvette d'un tournoi (B046).
 *
 * <p>Affluence : joueurs prévus (saisis par jour, sinon joueurs distincts inscrits
 * aux tableaux de ce jour) + part des accompagnants = personnes ; × durée de
 * présence moyenne (en demi-journées) = présences. Chaque produit : présences ×
 * consommation par personne et par demi-journée (hypothèse à ajuster), arrondi à
 * l'unité supérieure. Liste de courses : portions nécessaires selon les recettes,
 * moins le stock disponible, converties en unités d'achat au rendement minimal en
 * tenant compte de la perte (arrondi supérieur). Trois scénarios d'affluence
 * (bas, moyen, haut). Tous les montants en centimes.</p>
 */
public final class CalculBudgetBuvette {

    /**
     * Joueurs distincts (par licence) inscrits aux tableaux de chaque jour ; un tableau
     * sans jour connu compte pour le premier jour.
     */
    public static Map<LocalDate, Integer> joueursEstimes(Tournoi tournoi) {
        Map<LocalDate, Set<String>> parJour = new LinkedHashMap<>();
        tournoi.getJours().forEach(j -> parJour.put(j, new HashSet<>()));
        for (Tableau t : tournoi.getTableaux()) {
            LocalDate jour = t.getHoraires() == null ? null : t.getHoraires().getJour();
            if (jour == null || !parJour.containsKey(jour)) {
                jour = tournoi.getDate();
            }
            for (Joueur j : t.getInscrits()) {
                String cle = j.getNumeroLicence() == null || j.getNumeroLicence().isBlank()
                        ? "sans-licence-" + System.identityHashCode(j) : j.getNumeroLicence();
                parJour.get(jour).add(cle);
            }
        }
        Map<LocalDate, Integer> res = new LinkedHashMap<>();
        parJour.forEach((j, s) -> res.put(j, s.size()));
        return res;
    }

    /** Hypothèses retenues : celles du budget si le club les a ajustées, sinon les suggestions. */
    public static HypothesesConsommation hypothesesRetenues(BudgetBuvette budget, HypothesesConsommation suggestions) {
        if (budget.hypotheses() == null) {
            return suggestions;
        }
        Map<String, Integer> conso = new LinkedHashMap<>(suggestions.consommations());
        conso.putAll(budget.hypotheses().consommations());
        HypothesesConsommation h = budget.hypotheses();
        return new HypothesesConsommation(h.accompagnantsPourcent(), h.presenceDixiemes(), h.scenarioBasPourcent(),
                h.scenarioHautPourcent(), conso);
    }

    /** Calcule le budget prévisionnel du tournoi. */
    public BudgetCalcule calculer(Tournoi tournoi, BuvetteTournoi buvette, CatalogueBuvette catalogue,
                                  HypothesesConsommation suggestions) {
        BudgetBuvette budget = buvette.getBudget();
        HypothesesConsommation h = hypothesesRetenues(budget, suggestions);
        Map<LocalDate, Integer> estimes = joueursEstimes(tournoi);
        List<BudgetCalcule.JourBudget> jours = new ArrayList<>();
        int joueurs = 0;
        for (LocalDate j : tournoi.getJours()) {
            Integer saisi = budget.joueursSaisis().get(j);
            int n = saisi != null ? saisi : estimes.getOrDefault(j, 0);
            jours.add(new BudgetCalcule.JourBudget(j, n, saisi != null));
            joueurs += n;
        }
        BigDecimal personnes = BigDecimal.valueOf((long) joueurs * (100 + h.accompagnantsPourcent()))
                .divide(BigDecimal.valueOf(100));
        BigDecimal presences = personnes.multiply(BigDecimal.valueOf(h.presenceDixiemes()))
                .divide(BigDecimal.TEN);
        String affluence = joueurs + (joueurs > 1 ? " joueurs" : " joueur")
                + (jours.size() > 1 ? " sur " + jours.size() + " jours" : "")
                + " + " + h.accompagnantsPourcent() + " % d'accompagnants = " + NombresBuvette.texte(personnes)
                + " personnes ; × " + NombresBuvette.ecrire(h.presenceDixiemes(), 1)
                + " demi-journée de présence = " + NombresBuvette.texte(presences) + " présences";
        Map<String, Long> stock = budget.deduireStock() ? ServiceStockBuvette.restants(buvette) : Map.of();
        List<BudgetCalcule.Scenario> scenarios = List.of(
                scenario("Bas", h.scenarioBasPourcent(), presences, h, budget, catalogue, stock),
                scenario("Moyen", 100, presences, h, budget, catalogue, stock),
                scenario("Haut", h.scenarioHautPourcent(), presences, h, budget, catalogue, stock));
        return new BudgetCalcule(jours, affluence, h, scenarios);
    }

    private static BudgetCalcule.Scenario scenario(String nom, int pourcent, BigDecimal presences,
                                                   HypothesesConsommation h, BudgetBuvette budget,
                                                   CatalogueBuvette catalogue, Map<String, Long> stock) {
        List<BudgetCalcule.LigneProduit> produits = new ArrayList<>();
        Map<String, Long> portions = new LinkedHashMap<>();
        long recette = 0;
        String facteur = pourcent == 100 ? "" : " × " + pourcent + " %";
        for (Produit p : catalogue.getProduits()) {
            if (!p.isActif()) {
                continue;
            }
            Long imposee = budget.quantitesImposees().get(p.getId());
            long quantite;
            String explication;
            if (imposee != null) {
                quantite = plafond(BigDecimal.valueOf(imposee).multiply(BigDecimal.valueOf(pourcent))
                        .divide(BigDecimal.valueOf(100)));
                explication = p.getNom() + " : " + imposee + " repris de « " + budget.origineQuantites() + " »"
                        + facteur + " = " + quantite;
            } else {
                int conso = h.consommation(p.getId());
                if (conso == 0) {
                    continue;
                }
                quantite = plafond(presences.multiply(BigDecimal.valueOf(pourcent)).multiply(BigDecimal.valueOf(conso))
                        .divide(BigDecimal.valueOf(10_000)));
                explication = p.getNom() + " : " + NombresBuvette.texte(presences) + " présences" + facteur + " × "
                        + NombresBuvette.ecrire(conso, 2) + " = " + quantite;
            }
            long r = quantite * p.getPrixVenteCentimes();
            recette += r;
            produits.add(new BudgetCalcule.LigneProduit(p.getId(), p.getNom(), quantite, r, explication,
                    imposee != null, p.getRecette().isEmpty()));
            for (LigneRecette l : p.getRecette()) {
                portions.merge(l.idIngredient(), quantite * l.quantite(), Long::sum);
            }
        }
        List<BudgetCalcule.LigneCourse> courses = new ArrayList<>();
        long cout = 0;
        for (Map.Entry<String, Long> e : portions.entrySet()) {
            Ingredient i = catalogue.ingredient(e.getKey()).orElse(null);
            if (i == null || e.getValue() == 0) {
                continue;
            }
            long dispo = Math.max(0, stock.getOrDefault(i.getId(), 0L));
            long aAcheter = Math.max(0, e.getValue() - dispo);
            // unités = portions ÷ (rendement minimal × (1 − perte)), arrondi au-dessus
            long unites = aAcheter == 0 ? 0 : plafond(BigDecimal.valueOf(aAcheter * 100)
                    .divide(BigDecimal.valueOf((long) i.getRendementMin() * (100 - i.getPertePourcent())), 6,
                            RoundingMode.HALF_UP));
            long c = unites * i.getPrixAchatCentimes();
            cout += c;
            StringBuilder ex = new StringBuilder(i.getNom()).append(" : ").append(e.getValue()).append(' ')
                    .append(i.getUnitePortion());
            if (dispo > 0) {
                ex.append(" − ").append(dispo).append(" en stock = ").append(aAcheter);
            }
            ex.append(" ÷ ").append(i.getRendementMin());
            if (i.getPertePourcent() > 0) {
                ex.append(" (perte ").append(i.getPertePourcent()).append(" %)");
            }
            ex.append(" = ").append(unites).append(" × ").append(i.getUniteAchat()).append(" à ")
                    .append(Centimes.euros(i.getPrixAchatCentimes())).append(" = ").append(Centimes.euros(c));
            courses.add(new BudgetCalcule.LigneCourse(i.getId(), i.getNom(), e.getValue(), dispo, aAcheter, unites,
                    i.getUniteAchat(), c, ex.toString()));
        }
        return new BudgetCalcule.Scenario(nom, pourcent, produits, courses, recette, cout);
    }

    private static long plafond(BigDecimal v) {
        return v.setScale(0, RoundingMode.CEILING).longValueExact();
    }
}
