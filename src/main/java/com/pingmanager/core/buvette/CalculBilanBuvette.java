package com.pingmanager.core.buvette;

import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.HypothesesConsommation;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.TypeMouvementStock;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bilan de la buvette d'un tournoi (B046), prévu contre réel. Le prévu est le
 * scénario moyen du budget, achats bruts (sans déduire le stock) ; le réel vient de la caisse (ventes nettes), des
 * achats payés et du journal de stock (pertes, dons, stock restant).
 */
public final class CalculBilanBuvette {

    private final CalculBudgetBuvette budget = new CalculBudgetBuvette();

    public BilanBuvette bilan(Tournoi tournoi, BuvetteTournoi buvette, CatalogueBuvette catalogue,
                              HypothesesConsommation suggestions, ZoneId zone) {
        // Prévu : budget brut, sans déduire le stock (celui de fin de tournoi fausserait les achats prévus).
        BuvetteTournoi seulBudget = new BuvetteTournoi();
        seulBudget.setBudget(buvette.getBudget().avecDeduireStock(false));
        BudgetCalcule.Scenario prevu = budget.calculer(tournoi, seulBudget, catalogue, suggestions).moyen();
        SyntheseVentes ventes = ServiceVentesBuvette.synthese(buvette, null, zone);
        long achats = 0;
        long pertes = 0;
        BigDecimal valeurPertes = BigDecimal.ZERO;
        for (MouvementStock m : buvette.getMouvements()) {
            achats += m.prixPayeCentimes();
            if (m.type() == TypeMouvementStock.PERTE || m.type() == TypeMouvementStock.DON
                    || (m.type() == TypeMouvementStock.CORRECTION && estCorrectionDePerte(buvette, m))) {
                pertes -= m.quantite();
                Ingredient i = catalogue.ingredient(m.idIngredient()).orElse(null);
                if (i != null) {
                    valeurPertes = valeurPertes.add(CalculCoutBuvette.coutPortion(i, true)
                            .multiply(BigDecimal.valueOf(-m.quantite())));
                }
            }
        }
        BigDecimal restant = BigDecimal.ZERO;
        for (Map.Entry<String, Long> e : ServiceStockBuvette.restants(buvette).entrySet()) {
            Ingredient i = catalogue.ingredient(e.getKey()).orElse(null);
            if (i != null && e.getValue() > 0) {
                restant = restant.add(CalculCoutBuvette.coutPortion(i, true).multiply(BigDecimal.valueOf(e.getValue())));
            }
        }
        List<Map.Entry<String, Long>> top = new ArrayList<>();
        for (Map.Entry<String, Long> e : ventes.plusVendus(5)) {
            top.add(new AbstractMap.SimpleImmutableEntry<>(ventes.noms().getOrDefault(e.getKey(), e.getKey()),
                    e.getValue()));
        }
        Map<String, Long> parProduit = new java.util.LinkedHashMap<>();
        ventes.quantites().forEach((id, q) -> {
            if (q > 0) {
                parProduit.put(id, q);
            }
        });
        return new BilanBuvette(tournoi.getNom(), tournoi.getDate(), Formats.saison(tournoi.getDate()),
                prevu.recetteCentimes(), prevu.coutAchatsCentimes(), ventes.recetteCentimes(), achats,
                ventes.coutCentimes(), ventes.articles(), top, pertes, Centimes.arrondir(valeurPertes),
                Centimes.arrondir(restant), parProduit);
    }

    private static boolean estCorrectionDePerte(BuvetteTournoi buvette, MouvementStock correction) {
        return buvette.getMouvements().stream().anyMatch(m -> m.numero() == correction.numeroLie()
                && (m.type() == TypeMouvementStock.PERTE || m.type() == TypeMouvementStock.DON));
    }
}
