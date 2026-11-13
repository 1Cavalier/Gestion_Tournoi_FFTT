package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.HypothesesConsommation;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;

/**
 * Budget prévisionnel calculé de la buvette d'un tournoi (B046) : affluence,
 * hypothèses retenues et trois scénarios (bas, moyen, haut). Chaque quantité est
 * accompagnée de la phrase qui l'explique.
 *
 * @param jours                joueurs prévus par jour (saisis ou estimés)
 * @param explicationAffluence « 120 joueurs + 30 % d'accompagnants = 156 personnes… »
 * @param hypotheses           hypothèses retenues
 * @param scenarios            bas, moyen, haut (dans cet ordre)
 */
public record BudgetCalcule(List<JourBudget> jours, String explicationAffluence, HypothesesConsommation hypotheses,
                            List<Scenario> scenarios) {

    public BudgetCalcule {
        jours = List.copyOf(jours);
        scenarios = List.copyOf(scenarios);
    }

    /** Joueurs d'un jour : nombre et origine (saisi à la main ou estimé des inscriptions). */
    public record JourBudget(LocalDate jour, int joueurs, boolean saisi) {
    }

    /** Un produit prévu. */
    public record LigneProduit(String idProduit, String nom, long quantite, long recetteCentimes, String explication,
                               boolean imposee, boolean sansRecette) {
    }

    /** Une ligne de la liste de courses. */
    public record LigneCourse(String idIngredient, String nom, long portionsNecessaires, long stockDisponible,
                              long portionsAAcheter, long unites, String uniteAchat, long coutCentimes,
                              String explication) {
    }

    /** Un scénario d'affluence. */
    public record Scenario(String nom, int pourcent, List<LigneProduit> produits, List<LigneCourse> courses,
                           long recetteCentimes, long coutAchatsCentimes) {

        public Scenario {
            produits = List.copyOf(produits);
            courses = List.copyOf(courses);
        }

        public long margeCentimes() {
            return recetteCentimes - coutAchatsCentimes;
        }

        /**
         * Seuil de rentabilité : part de la recette attendue qu'il faut vendre pour
         * rembourser les achats, en pour cent arrondi au-dessus ; vide sans recette.
         */
        public OptionalLong seuilRentabilitePourcent() {
            if (recetteCentimes <= 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of((coutAchatsCentimes * 100 + recetteCentimes - 1) / recetteCentimes);
        }
    }

    public Scenario bas() {
        return scenarios.get(0);
    }

    public Scenario moyen() {
        return scenarios.get(1);
    }

    public Scenario haut() {
        return scenarios.get(2);
    }

    public int joueurs() {
        return jours.stream().mapToInt(JourBudget::joueurs).sum();
    }
}
