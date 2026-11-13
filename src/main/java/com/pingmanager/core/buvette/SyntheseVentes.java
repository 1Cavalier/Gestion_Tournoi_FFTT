package com.pingmanager.core.buvette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Totaux des ventes de la buvette (B046), annulations déduites.
 *
 * @param articles        nombre d'articles vendus
 * @param recetteCentimes montant encaissé
 * @param coutCentimes    coût de revient des articles vendus (recettes connues)
 * @param coutComplet     faux si un article vendu n'avait pas de coût connu
 * @param quantites       quantité nette par produit
 * @param noms            nom de chaque produit au moment de la vente
 */
public record SyntheseVentes(long articles, long recetteCentimes, long coutCentimes, boolean coutComplet,
                             Map<String, Long> quantites, Map<String, String> noms) {

    public SyntheseVentes {
        quantites = Collections.unmodifiableMap(new LinkedHashMap<>(quantites));
        noms = Collections.unmodifiableMap(new LinkedHashMap<>(noms));
    }

    public long quantite(String idProduit) {
        return quantites.getOrDefault(idProduit, 0L);
    }

    /** Produits du plus vendu au moins vendu (quantité nette positive), au plus {@code n}. */
    public List<Map.Entry<String, Long>> plusVendus(int n) {
        List<Map.Entry<String, Long>> l = new ArrayList<>();
        quantites.entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(n).forEach(l::add);
        return l;
    }
}
