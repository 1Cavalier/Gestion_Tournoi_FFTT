package com.pingmanager.model.buvette;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Réglages du budget prévisionnel de la buvette d'un tournoi (B046) : ce que le
 * club a saisi ou ajusté. Tout ce qui n'est pas saisi retombe sur les
 * suggestions (hypothèses) et sur les inscriptions du tournoi. Objet immuable.
 *
 * @param joueursSaisis     joueurs prévus saisis à la main, par jour (absent : estimés)
 * @param hypotheses        hypothèses ajustées, ou {@code null} pour garder les suggestions
 * @param quantitesImposees quantités par produit reprises d'un ancien tournoi (remplacent le calcul)
 * @param origineQuantites  nom du tournoi d'où viennent ces quantités (vide sinon)
 * @param deduireStock      vrai : le stock disponible est déduit de la liste de courses
 */
public record BudgetBuvette(Map<LocalDate, Integer> joueursSaisis, HypothesesConsommation hypotheses,
                            Map<String, Long> quantitesImposees, String origineQuantites, boolean deduireStock) {

    /** Budget vide : tout est estimé. */
    public static final BudgetBuvette VIDE = new BudgetBuvette(Map.of(), null, Map.of(), "", true);

    public BudgetBuvette {
        joueursSaisis = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNullElse(joueursSaisis,
                Map.of())));
        quantitesImposees = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNullElse(
                quantitesImposees, Map.of())));
        origineQuantites = origineQuantites == null ? "" : origineQuantites.trim();
    }

    public boolean estVide() {
        return equals(VIDE);
    }

    public BudgetBuvette avecJoueurs(LocalDate jour, Integer joueurs) {
        Map<LocalDate, Integer> m = new LinkedHashMap<>(joueursSaisis);
        if (joueurs == null) {
            m.remove(jour);
        } else {
            m.put(jour, joueurs);
        }
        return new BudgetBuvette(m, hypotheses, quantitesImposees, origineQuantites, deduireStock);
    }

    public BudgetBuvette avecHypotheses(HypothesesConsommation h) {
        return new BudgetBuvette(joueursSaisis, h, quantitesImposees, origineQuantites, deduireStock);
    }

    public BudgetBuvette avecQuantitesImposees(Map<String, Long> quantites, String origine) {
        return new BudgetBuvette(joueursSaisis, hypotheses, quantites, origine, deduireStock);
    }

    public BudgetBuvette avecDeduireStock(boolean v) {
        return new BudgetBuvette(joueursSaisis, hypotheses, quantitesImposees, origineQuantites, v);
    }
}
