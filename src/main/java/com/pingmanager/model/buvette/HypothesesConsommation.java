package com.pingmanager.model.buvette;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hypothèses de consommation de la buvette (B046), toutes <b>à ajuster</b> par le
 * club : ce ne sont pas des statistiques, seulement des ordres de grandeur de
 * départ. Valeurs entières pour éviter les nombres à virgule flottante.
 *
 * @param accompagnantsPourcent part des accompagnants (30 = 30 accompagnants pour 100 joueurs)
 * @param presenceDixiemes      durée de présence moyenne d'une personne, en dixièmes de
 *                              demi-journée (15 = 1,5 demi-journée)
 * @param scenarioBasPourcent   affluence du scénario bas (75 = 75 % du scénario moyen)
 * @param scenarioHautPourcent  affluence du scénario haut (125 = 125 %)
 * @param consommations         par produit : consommation par personne et par
 *                              demi-journée, en centièmes (35 = 0,35 café)
 */
public record HypothesesConsommation(int accompagnantsPourcent, int presenceDixiemes, int scenarioBasPourcent,
                                     int scenarioHautPourcent, Map<String, Integer> consommations) {

    /** Valeurs de repli si la ressource de suggestions est absente. */
    public static final HypothesesConsommation PAR_DEFAUT = new HypothesesConsommation(30, 15, 75, 125, Map.of());

    public HypothesesConsommation {
        if (accompagnantsPourcent < 0 || accompagnantsPourcent > 500) {
            throw new IllegalArgumentException("accompagnants entre 0 et 500 %");
        }
        if (presenceDixiemes < 1 || presenceDixiemes > 80) {
            throw new IllegalArgumentException("présence entre 0,1 et 8 demi-journées");
        }
        if (scenarioBasPourcent < 1 || scenarioBasPourcent > 100 || scenarioHautPourcent < 100
                || scenarioHautPourcent > 500) {
            throw new IllegalArgumentException("scénarios : bas entre 1 et 100 %, haut entre 100 et 500 %");
        }
        Map<String, Integer> m = new LinkedHashMap<>();
        if (consommations != null) {
            consommations.forEach((k, v) -> {
                if (k != null && v != null && v >= 0) {
                    m.put(k, v);
                }
            });
        }
        consommations = Collections.unmodifiableMap(m);
    }

    /** Consommation d'un produit, en centièmes (0 si aucune hypothèse). */
    public int consommation(String idProduit) {
        return consommations.getOrDefault(idProduit, 0);
    }
}
